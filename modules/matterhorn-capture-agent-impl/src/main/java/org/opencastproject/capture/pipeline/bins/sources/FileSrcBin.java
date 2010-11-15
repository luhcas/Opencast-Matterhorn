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
import org.gstreamer.ElementFactory;
import org.gstreamer.Pad;
import org.gstreamer.PadLinkReturn;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;

public class FileSrcBin extends VideoSrcBin{
  private Element filesrc;  
  private Element decodebin;
  
  public FileSrcBin(CaptureDevice captureDevice, Properties properties) throws Exception {
      super(captureDevice, properties);
    }
    
    @Override
    protected void createElements(){
      super.createElements();
      filesrc = ElementFactory.make("filesrc", null);
      queue = ElementFactory.make("queue", null);
      decodebin = ElementFactory.make("decodebin", null);
    }
    
    @Override
    protected void setElementProperties(){
      super.setElementProperties();
      filesrc.set("location", captureDevice.getLocation());
      
   // decodebin source pad is only available sometimes, therefore we need to add a listener to accept dynamic pads
      decodebin.connect(new Element.PAD_ADDED() {
        public void padAdded(Element arg0, Pad newPad) {
          if(newPad.getName().contains("video")){
            PadLinkReturn padLinkReturn = newPad.link(videorate.getStaticPad("sink"));
            if(padLinkReturn != PadLinkReturn.OK){
              try {
                throw new UnableToLinkGStreamerElementsException(captureDevice, decodebin, videorate);
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
      bin.addMany(filesrc, queue, decodebin, videorate, fpsfilter);
    }

    @Override
    public Pad getSrcPad() {
        return fpsfilter.getStaticPad("src");
    }

    @Override
    protected void linkElements() throws UnableToLinkGStreamerElementsException {
      if (!filesrc.link(queue)) {
        throw new UnableToLinkGStreamerElementsException(captureDevice, filesrc, queue);
      } else if (!queue.link(decodebin))
        throw new UnableToLinkGStreamerElementsException(captureDevice, queue, decodebin);
      else if (!videorate.link(fpsfilter))
        throw new UnableToLinkGStreamerElementsException(captureDevice, videorate, fpsfilter);
    }
}
