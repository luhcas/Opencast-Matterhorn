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
package org.opencastproject.capture.pipeline.bins.sources;

import java.util.Properties;

import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.Pad;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceNullPointerException;
import org.opencastproject.capture.pipeline.bins.GStreamerElementFactory;
import org.opencastproject.capture.pipeline.bins.GStreamerElements;
import org.opencastproject.capture.pipeline.bins.GStreamerProperties;
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateGhostPadsForBinException;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;
import org.opencastproject.capture.pipeline.bins.UnableToSetElementPropertyBecauseElementWasNullException;


public class DV1394SrcBin extends VideoSrcBin {
  
  private static final String VIDEO_X_DV = "video/x-dv";
  private Element dv1394src;
  private Element demux;
  private Element decoder;
  private Element ffmpegcolorspace;
  
  /**
   * Adds a pipeline specifically designed to captured from a DV Camera attached by firewire to the main pipeline
   * 
   * @deprecated  This function has not been maintained in a long time and has many problems.  
   * If you need DV support let the list know.
   * @param captureDevice
   *          DV Camera attached to firewire {@code CaptureDevice} to create pipeline around
   * @param properties
   *          The {@code Properties} of the confidence monitoring.
   * @throws UnableToSetElementPropertyBecauseElementWasNullException 
   * @throws UnableToCreateGhostPadsForBinException 
   * @throws UnableToLinkGStreamerElementsException 
   * @throws CaptureDeviceNullPointerException 
   * @throws UnableToCreateElementException 
   */
  public DV1394SrcBin(CaptureDevice captureDevice, Properties properties)
          throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException,
          UnableToCreateElementException {
    super(captureDevice, properties);
  }
  
  @Override
  protected void createElements() throws UnableToCreateElementException{
    super.createElements();
    dv1394src = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.DV1394SRC, null);
    /* set up dv stream decoding */
    demux = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.DVDEMUX, null);
    decoder = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.DVDEC, null);
    ffmpegcolorspace = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.FFMPEGCOLORSPACE, null);
  }
  @Override
  protected void setElementProperties(){
    super.setElementProperties();
    /* handle demuxer's sometimes pads. Filter for just video. */
    demux.connect(new Element.PAD_ADDED() {
      public void padAdded(Element element, Pad pad) {
        logger.info("Element: {}, Pad: {}", element.getName(), pad.getName());
        Element.linkPadsFiltered(demux, GStreamerProperties.VIDEO, decoder, GStreamerProperties.SINK, 
                Caps.fromString(VIDEO_X_DV));
      }
    });
  }
  
  @Override
  protected void addElementsToBin(){
    bin.addMany(dv1394src, demux, decoder, videorate, fpsfilter, ffmpegcolorspace);
    
    demux.connect(new Element.PAD_ADDED() {
      public void padAdded(Element element, Pad pad) {
        pad.link(decoder.getStaticPad(GStreamerProperties.SINK));
      }
    });
  }
  
  @Override
  protected void linkElements() throws UnableToLinkGStreamerElementsException{
    if (!dv1394src.link(queue)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, dv1394src, queue);
    } 
    else if (!queue.link(demux)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, queue, demux);
    } 
    else if (!decoder.link(videorate)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, decoder, videorate);
    } 
    else if (!videorate.link(fpsfilter)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, videorate, fpsfilter);
    }
    else if (!fpsfilter.link(ffmpegcolorspace)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, fpsfilter, ffmpegcolorspace);
    }
  }

  @Override
  public Pad getSrcPad() {
    return decoder.getStaticPad(GStreamerProperties.SRC);
  }
}
