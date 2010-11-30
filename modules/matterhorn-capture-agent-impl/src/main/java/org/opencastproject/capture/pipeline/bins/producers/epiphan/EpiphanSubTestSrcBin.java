/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.capture.pipeline.bins.producers.epiphan;

import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.Pipeline;
import org.gstreamer.elements.AppSink;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.GStreamerElementFactory;
import org.opencastproject.capture.pipeline.bins.GStreamerElements;
import org.opencastproject.capture.pipeline.bins.GStreamerProperties;
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;

/**
 * EpiphanSubTestSrcBin extends EpiphanSubAbstractBin.
 * Creates a VideoTestSrc sub-pipeline.
 */
public class EpiphanSubTestSrcBin extends EpiphanSubAbstractBin {
  
  /** CaptureDevice */
  CaptureDevice captureDevice;

  /** Caps */
  String caps = null;

  /** Pipeline elements */
  Element src, caps_filter;
  /** AppSink, the last element. */
  AppSink sink = null;

  /**
   * Constructor. Creates a VideoTestSrc sub-pipeline.
   * @param captureDevice CaptureDevice
   * @param caps Caps
   * @throws UnableToCreateElementException
   * @throws UnableToLinkGStreamerElementsException
   */
  public EpiphanSubTestSrcBin(CaptureDevice captureDevice, String caps) 
          throws UnableToCreateElementException, UnableToLinkGStreamerElementsException {
    super("videotestsrc_pipeline");
    this.captureDevice = captureDevice;
    this.caps = caps;
    
    createElements();
    setElementProperties();
    linkElements();
    
    pipeline.debugToDotFile(Pipeline.DEBUG_GRAPH_SHOW_ALL, pipeline.getName(), false);
  }

  /**
   * Creates pipeline elements.
   * @throws UnableToCreateElementException
   */
  protected void createElements() throws UnableToCreateElementException {
    src = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(), GStreamerElements.VIDEOTESTSRC, "videotestsrc");
    caps_filter = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(), GStreamerElements.CAPSFILTER, "videotestsrc_capsfilter");
    sink = (AppSink) GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(), GStreamerElements.APPSINK, "videotestsrc_appsink");
    pipeline.addMany(src, caps_filter, sink);
  }

  /**
   * Set pipeline element properties.
   */
  protected void setElementProperties() {
    src.set(GStreamerProperties.PATTERN, "0");
    src.set(GStreamerProperties.IS_LIVE, "true");
    src.set(GStreamerProperties.DO_TIMESTAP, "false");
    caps_filter.setCaps(Caps.fromString(caps));
    sink.set(GStreamerProperties.EMIT_SIGNALS, "false");
    sink.set(GStreamerProperties.DROP, "true");
    sink.set(GStreamerProperties.MAX_BUFFERS, "1");
    sink.setCaps(Caps.fromString(caps));
  }

  /**
   * Link pipeline elements.
   * @throws UnableToLinkGStreamerElementsException
   */
  protected void linkElements() throws UnableToLinkGStreamerElementsException {
    if (!src.link(caps_filter)) {
      removeElements();
      throw new UnableToLinkGStreamerElementsException(captureDevice, src, caps_filter);
    }
    
    if (!caps_filter.link(sink)) {
      removeElements();
      throw new UnableToLinkGStreamerElementsException(captureDevice, caps_filter, sink);
    }
  }

  /**
   * Remove all elements from pipeline.
   */
  protected void removeElements() {
    pipeline.removeMany(src, caps_filter, sink);
  }

  /**
   * @inhetirDocs
   * @see EpiphanSubBin#getSink() 
   */
  @Override
  public AppSink getSink() {
    return sink;
  }
}
