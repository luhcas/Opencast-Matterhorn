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

import org.gstreamer.Bus.EOS;
import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.GstObject;
import org.gstreamer.Pipeline;
import org.gstreamer.State;
import org.gstreamer.elements.AppSink;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.GStreamerElementFactory;
import org.opencastproject.capture.pipeline.bins.GStreamerElements;
import org.opencastproject.capture.pipeline.bins.GStreamerProperties;
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;

/**
 * EpiphanSubDeviceBin extends EpiphanSubAbstractBin.
 * Represent a SubBin of a Epiphan VGA2USB device.
 */
public class EpiphanSubDeviceBin extends EpiphanSubAbstractBin {

  /** CaptureDevice */
  CaptureDevice captureDevice;
  /** Caps */
  String caps = null;

  /** Pipeline elements */
  Element src, colorspace, videoscale, capsfilter;
  /** AppSink, the last element. */
  AppSink sink;

  /** True if no VGA signal. Will be set automaticly, do not set it manually! */
  static boolean broken = false;

  /**
   * Constructor. Creates Epiphan VGA2USB Device sub-pipeline.
   * @param captureDevice CaptureDevice
   * @param caps Caps
   * @trows UnableToCreateElementException
   * @throws UnableToLinkGStreamerElementsException
   */
  public EpiphanSubDeviceBin(CaptureDevice captureDevice, String caps) 
          throws UnableToCreateElementException, UnableToLinkGStreamerElementsException {
    
    super("epiphan_pipeline");
    this.captureDevice = captureDevice;
    this.caps = caps;
    

    createElements();
    setElementProperties();
    linkElements();
    setEOSListener();
    pipeline.debugToDotFile(Pipeline.DEBUG_GRAPH_SHOW_ALL, pipeline.getName(), false);
  }

  /**
   * @inheritDocs
   * @see EpiphanSubBin#getSink() 
   */
  @Override
  public AppSink getSink() {
    if (!isBroken())
      return sink;
    else return null;
  }

  /**
   * Returns Caps.
   * @return Caps.
   */
  public String getCaps() {
    return caps;
  }

  /**
   * Creates pipeline elements and add these to bin.
   * @throws UnableToCreateElementException
   */
  protected void createElements() throws UnableToCreateElementException {
    src = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(), GStreamerElements.V4LSRC, captureDevice.getLocation());
    colorspace = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(), GStreamerElements.FFMPEGCOLORSPACE, "ffmpegcolorspace");
    videoscale = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(), GStreamerElements.FFVIDEOSCALE, "ffvideoscale");
    capsfilter = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(), GStreamerElements.CAPSFILTER, "v4l_caps");
    sink = (AppSink) GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(), GStreamerElements.APPSINK, "epiphan_appsink");
    
    pipeline.addMany(src, colorspace, videoscale, capsfilter, sink);
  }

  /**
   * Set pipeline element properties.
   */
  protected void setElementProperties() {
    src.set(GStreamerProperties.DEVICE, captureDevice.getLocation());
    src.set(GStreamerProperties.DO_TIMESTAP, "false");
    sink.set(GStreamerProperties.EMIT_SIGNALS, "false");
    sink.set(GStreamerProperties.DROP, "true");
    sink.set(GStreamerProperties.MAX_BUFFERS, "1");
    if (caps != null && !caps.isEmpty()) {
      capsfilter.setCaps(Caps.fromString(caps));
      sink.setCaps(Caps.fromString(caps));
    }
  }

  /**
   * Link pipeline elements.
   * @throws UnableToLinkGStreamerElementsException
   */
  protected void linkElements() throws UnableToLinkGStreamerElementsException {

    if (!src.link(colorspace)) {
      removeElements();
      throw new UnableToLinkGStreamerElementsException(captureDevice, src, colorspace);
    }
    if (!colorspace.link(videoscale)) {
      removeElements();
      throw new UnableToLinkGStreamerElementsException(captureDevice, colorspace, videoscale);
    }
    if (!videoscale.link(capsfilter)) {
      removeElements();
      throw new UnableToLinkGStreamerElementsException(captureDevice, videoscale, capsfilter);
    }
    if (!capsfilter.link(sink)) {
      removeElements();
      throw new UnableToLinkGStreamerElementsException(captureDevice, capsfilter, sink);
    }
  }

  /**
   * Remove all elements from pipeline.
   */
  protected void removeElements() {
    pipeline.removeMany(src, colorspace, videoscale, capsfilter, sink);
  }

  /**
   * Start pipeline and manage vga signal broken state.
   * @param time time to check, if pipeline is playing, -1 skip checks.
   * @return true, if pipeline is playing.
   */
  @Override
  public boolean start(long time) {
    if (setState(State.PLAYING, time)) {
      broken = false;
      return true;
    } else {
      stop();
      broken = true;
      return false;
    }
  }

  /**
   * Return broken state.
   * @return true if vga signal is broken.
   */
  protected synchronized boolean isBroken() {
      return broken;
  }

  /**
   * Set pipeline EOS Listener to shut down pipeline and set broken state.
   */
  protected void setEOSListener() {
    pipeline.getBus().connect(new EOS() {

      @Override
      public void endOfStream(GstObject source) {
        broken = true;
        pipeline.setState(State.NULL);
      }
    });
  }
}
