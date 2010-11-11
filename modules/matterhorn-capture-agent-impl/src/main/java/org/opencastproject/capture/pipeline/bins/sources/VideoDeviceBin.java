package org.opencastproject.capture.pipeline.bins.sources;

import java.util.Properties;

import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;



public abstract class VideoDeviceBin extends SrcBin {
  // Filter used to make a change in the FPS in a video output
  Element fpsfilter;
  Element videorate;
  
  public VideoDeviceBin(CaptureDevice captureDevice, Properties properties) throws Exception{
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
