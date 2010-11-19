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
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceNullPointerException;
import org.opencastproject.capture.pipeline.bins.GStreamerElementFactory;
import org.opencastproject.capture.pipeline.bins.GStreamerElements;
import org.opencastproject.capture.pipeline.bins.GStreamerProperties;
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateGhostPadsForBinException;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;
import org.opencastproject.capture.pipeline.bins.UnableToSetElementPropertyBecauseElementWasNullException;



public abstract class VideoSrcBin extends SrcBin {
  // Filter used to make a change in the FPS in a video output
  Element fpsfilter;
  Element videorate;
  
  public VideoSrcBin(CaptureDevice captureDevice, Properties properties) throws UnableToLinkGStreamerElementsException,
          UnableToCreateGhostPadsForBinException, UnableToSetElementPropertyBecauseElementWasNullException,
          CaptureDeviceNullPointerException, UnableToCreateElementException {
    super(captureDevice, properties);
  }
  
  /*
   * The main difference between this and a general source bin is that we will need a caps filter to support being able
   * to change the FPS of the output media and a videorate Element to fix the output media's timestamp in case of an fps
   * change. 
   */
  @Override
  protected void createElements() throws UnableToCreateElementException {
    super.createElements();
    createVideoRate();
    createFramerateCaps();
  }
 
  /* Creates a videorate GST Element that adjusts the timestamps in case of a FPS change in the output file.*/
  private void createVideoRate() throws UnableToCreateElementException {
    videorate = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.VIDEORATE, null);
  }
  
  private void createFramerateCaps() throws UnableToCreateElementException {
    fpsfilter = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.CAPSFILTER, null);
  }
  
  @Override
  protected void setElementProperties(){
    setFramerateCapsProperties();
  }
  
  
  private void setFramerateCapsProperties() {
    Caps fpsCaps;
    if (captureDeviceProperties.framerate != null) {
      fpsCaps = new Caps(GStreamerProperties.VIDEO_X_RAW_YUV + ", " + GStreamerProperties.FRAMERATE + "="
              + captureDeviceProperties.framerate + "/1");
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
