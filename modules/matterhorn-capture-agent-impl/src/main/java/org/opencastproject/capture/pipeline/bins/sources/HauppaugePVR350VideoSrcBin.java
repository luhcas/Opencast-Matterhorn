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

import org.gstreamer.Element;
import org.gstreamer.Pad;
import org.gstreamer.PadLinkReturn;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceNullPointerException;
import org.opencastproject.capture.pipeline.bins.GStreamerElementFactory;
import org.opencastproject.capture.pipeline.bins.GStreamerElements;
import org.opencastproject.capture.pipeline.bins.GStreamerProperties;
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateGhostPadsForBinException;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;
import org.opencastproject.capture.pipeline.bins.UnableToSetElementPropertyBecauseElementWasNullException;

public class HauppaugePVR350VideoSrcBin extends FileSrcBin {
  Element filesrc;
  Element mpegpsdemux;
  Element decoder;
  Element mpegvideoparse;
  private Element queue;
  
  /**
   * Adds a pipeline specifically designed to captured from the Hauppauge WinTv cards to the main pipeline
   * 
   * @param captureDevice
   *          The Hauppauge {@code CaptureDevice} to create pipeline around
   * @param properties
   *          The confidence monitoring properties. 
   * @throws UnableToSetElementPropertyBecauseElementWasNullException 
   * @throws UnableToCreateGhostPadsForBinException 
   * @throws UnableToLinkGStreamerElementsException 
   * @throws CaptureDeviceNullPointerException 
   * @throws UnableToCreateElementException 
   * @throws Exception 
   */
  public HauppaugePVR350VideoSrcBin(CaptureDevice captureDevice, Properties properties)
          throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException,
          UnableToCreateElementException {
    super(captureDevice, properties);
  }

  @Override
  protected void createElements() throws UnableToCreateElementException{
    super.createElements();
    filesrc = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.FILESRC, null);
    queue = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.QUEUE, null);
    mpegpsdemux = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.MPEGPSDEMUX, null);
    mpegvideoparse = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.MPEGVIDEOPARSE, null);
    decoder = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.MPEG2DEC, null);
  }
  
  @Override
  protected void setElementProperties(){
    super.setElementProperties();
    setFileSrcProperties();
    setMPEGPSDemuxProperties();
  }

  private void setFileSrcProperties() {
    filesrc.set(GStreamerProperties.LOCATION, captureDevice.getLocation());
  }
  
  private void setMPEGPSDemuxProperties() {
    // mpegpsdemux source pad is only available sometimes, therefore we need to add a listener to accept dynamic pads
    mpegpsdemux.connect(new Element.PAD_ADDED() {
      public void padAdded(Element arg0, Pad newPad) {
        if(newPad.getName().contains(GStreamerProperties.VIDEO)){
          PadLinkReturn padLinkReturn = newPad.link(mpegvideoparse.getStaticPad(GStreamerProperties.SINK));
          if(padLinkReturn != PadLinkReturn.OK){
            try {
              throw new UnableToLinkGStreamerElementsException(captureDevice, mpegpsdemux, mpegvideoparse);
            } catch (UnableToLinkGStreamerElementsException e) {
              logger.error(e.getMessage() + " because PadLinkReturn was " + padLinkReturn.toString() + " on Pad "
                      + newPad.getName());
            }
          }
        }
      }
    });
  }
  
  @Override
  protected void addElementsToBin(){
    bin.addMany(filesrc, queue, mpegpsdemux, mpegvideoparse, decoder, videorate, fpsfilter);
  }

  @Override
  public Pad getSrcPad() {
      return fpsfilter.getStaticPad(GStreamerProperties.SRC);
  }

  @Override
  protected void linkElements() throws UnableToLinkGStreamerElementsException {
    if (!filesrc.link(queue)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, filesrc, queue);
    } else if (!queue.link(mpegpsdemux))
      throw new UnableToLinkGStreamerElementsException(captureDevice, queue, mpegpsdemux);
    else if (!mpegvideoparse.link(decoder))
      throw new UnableToLinkGStreamerElementsException(captureDevice, mpegvideoparse, decoder);
    else if (!decoder.link(videorate))
      throw new UnableToLinkGStreamerElementsException(captureDevice, decoder, videorate);
    else if (!videorate.link(fpsfilter))
      throw new UnableToLinkGStreamerElementsException(captureDevice, videorate, fpsfilter);
  }
}
