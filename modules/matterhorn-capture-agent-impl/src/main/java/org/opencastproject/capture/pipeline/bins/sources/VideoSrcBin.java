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
import org.gstreamer.ElementFactory;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;



public abstract class VideoSrcBin extends SrcBin {
  // Filter used to make a change in the FPS in a video output
  Element fpsfilter;
  Element videorate;
  
  public VideoSrcBin(CaptureDevice captureDevice, Properties properties) throws Exception{
    super(captureDevice, properties);
  }
  
  /* All Video Capture Devices will need to support changes in FPS and some more logic for the muxer if 
   * they are using H264. 
   * @see org.opencastproject.capture.pipeline.bins.CaptureDeviceBin#createElements()
   */
  @Override
  protected void createElements() {
    super.createElements();
    createVideoRate();
    createFramerateCaps();
  }
 
  /** Creates a videorate gst Element that correctly adjusts the timestamps in case of a FPS change in the output file. **/
  private void createVideoRate(){
    videorate = ElementFactory.make("videorate", null);
  }
  
  private void createFramerateCaps() {
    fpsfilter = ElementFactory.make("capsfilter", null);
  }
  
  @Override
  protected void setElementProperties(){
    setFramerateCapsProperties();
  }
  
  
  private void setFramerateCapsProperties() {
    Caps fpsCaps;
    if (captureDeviceProperties.framerate != null) {
      fpsCaps = new Caps("video/x-raw-yuv, framerate=" + captureDeviceProperties.framerate + "/1");
      logger.debug("{} fps: {}", captureDevice.getName(), captureDeviceProperties.framerate);
    }
    else{
      fpsCaps = Caps.anyCaps();
    }
    fpsfilter.setCaps(fpsCaps);
  }
  
  @Override
  public boolean isVideoDevice(){
    return true;
  }
  
}
