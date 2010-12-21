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
package org.opencastproject.capture.pipeline.bins.producers;

import java.io.File;
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
 * Png-sub-bin to use in {@link EpiphanVGA2USBV4LProducer}.
 * Creates a bin witch convert a png image to video stream.
 * AppSink is the last Element where data can grabed from.
 */
public class EpiphanVGA2USBV4LSubPngBin extends EpiphanVGA2USBV4LSubAbstractBin {

  /** CaptureDevice */
  CaptureDevice captureDevice;

  /** Fallback image path (png) */
  String imagePath = null;

  /** Caps */
  String caps = null;

  /** Bin elements */
  Element src, pngdec, colorspace, scale, caps_filter;
  /** AppSink, the last element. */
  AppSink sink = null;

  /**
   * Constructor. Creates a image sub-bin.
   * @param captureDevice CaptureDevice
   * @param caps Caps
   * @param imagePath path to png file
   * @throws UnableToCreateElementException
   *        If the required GStreamer Modules are not installed to create
   *        all of the Elements or image path is not exist, this Exception will be thrown.
   * @throws UnableToLinkGStreamerElementsException
   *        If our elements fail to link together we will throw an exception.
   */
  public EpiphanVGA2USBV4LSubPngBin(CaptureDevice captureDevice, String caps, String imagePath)
          throws UnableToCreateElementException, UnableToLinkGStreamerElementsException {

    this.captureDevice = captureDevice;
    this.caps = caps;

    this.imagePath = imagePath;
    if (imagePath == null || !new File(imagePath).isFile())
      throw new UnableToCreateElementException(captureDevice.getFriendlyName(), GStreamerElements.MULTIFILESRC);

    createElements();
    setElementProperties();
    linkElements();
    
    bin.debugToDotFile(Pipeline.DEBUG_GRAPH_SHOW_ALL, bin.getName(), false);
  }

  /**
   * Create elements and add these to bin.
   * @throws UnableToCreateElementException
   *           If any of the Elements fail to be created because the GStreamer module
   *           for the Element isn't present then this Exception will be thrown.
   */
  private void createElements() throws UnableToCreateElementException {
    src = GStreamerElementFactory.getInstance().createElement(
            captureDevice.getFriendlyName(), GStreamerElements.MULTIFILESRC, null);
    pngdec = GStreamerElementFactory.getInstance().createElement(
            captureDevice.getFriendlyName(), GStreamerElements.PNGDEC, null);
    colorspace = GStreamerElementFactory.getInstance().createElement(
            captureDevice.getFriendlyName(), GStreamerElements.FFMPEGCOLORSPACE, null);
    scale = GStreamerElementFactory.getInstance().createElement(
            captureDevice.getFriendlyName(), GStreamerElements.FFVIDEOSCALE, null);
    caps_filter = GStreamerElementFactory.getInstance().createElement(
            captureDevice.getFriendlyName(), GStreamerElements.CAPSFILTER, null);
    sink = (AppSink) GStreamerElementFactory.getInstance().createElement(
            captureDevice.getFriendlyName(), GStreamerElements.APPSINK, null);
    bin.addMany(src, pngdec, colorspace, scale, caps_filter, sink);
  }

  /**
   * Set bin element properties.
   */
  private void setElementProperties() {
    src.set(GStreamerProperties.LOCATION, imagePath);
    src.setCaps(Caps.fromString("image/png, framerate=(fraction)25/1"));
    caps_filter.setCaps(Caps.fromString(caps));
    sink.set(GStreamerProperties.EMIT_SIGNALS, "false");
    sink.set(GStreamerProperties.DROP, "true");
    sink.set(GStreamerProperties.MAX_BUFFERS, "1");
    sink.setCaps(Caps.fromString(caps));
  }

  /**
   * Link elements together.
   * @throws UnableToLinkGStreamerElementsException
   *        If Elements can not be linked together.
   */
  private void linkElements() throws UnableToLinkGStreamerElementsException {
    if (!src.link(pngdec)) {
      removeElements();
      throw new UnableToLinkGStreamerElementsException(captureDevice, src, pngdec);
    }
    if (!pngdec.link(colorspace)) {
      removeElements();
      throw new UnableToLinkGStreamerElementsException(captureDevice, pngdec, colorspace);
    }
    if (!colorspace.link(scale)) {
      removeElements();
      throw new UnableToLinkGStreamerElementsException(captureDevice, colorspace, scale);
    }
    if (!scale.link(caps_filter)) {
      removeElements();
      throw new UnableToLinkGStreamerElementsException(captureDevice, scale, caps_filter);
    }
    if (!caps_filter.link(sink)) {
      removeElements();
      throw new UnableToLinkGStreamerElementsException(captureDevice, caps_filter, sink);
    }
  }

  /**
   * Remove all elements from bin.
   */
  protected void removeElements() {
    bin.removeMany(src, pngdec, colorspace, scale, caps_filter, sink);
  }

  /**
   * @inheritDocs
   * @see EpiphanVGA2USBV4LSubBin#getSink()
   */
  @Override
  public AppSink getSink() {
    return sink;
  }
}
