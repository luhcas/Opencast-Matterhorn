package org.opencastproject.capture.pipeline.bins.sources;

import java.util.Properties;

import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Pad;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;


public class V4l2srcDeviceBin extends VideoDeviceBin{

  Element v4l2src;

  public V4l2srcDeviceBin(CaptureDevice captureDevice, Properties properties) throws Exception {
    super(captureDevice, properties);
  }
  
  @Override
  protected void createElements(){
    super.createElements();
    v4l2src = ElementFactory.make("v4l2src", null);
  }
  
  @Override
  protected void addElementsToBin() {
    bin.addMany(v4l2src, queue, videorate, fpsfilter);
  }

  @Override
  protected void linkElements() throws Exception {
    /*if (!v4l2src.link(queue))
      throw new Exception(CaptureDeviceBin.formatBinError(captureDevice, v4l2src, queue));
    else if (!queue.link(videorate))
      throw new Exception(CaptureDeviceBin.formatBinError(captureDevice, queue, videorate));
    else if (!videorate.link(fpsfilter))
      throw new Exception(CaptureDeviceBin.formatBinError(captureDevice, videorate, fpsfilter));*/
    if (!v4l2src.link(videorate))
      throw new UnableToLinkGStreamerElementsException(captureDevice, v4l2src, videorate);
    else if (!videorate.link(fpsfilter))
      throw new UnableToLinkGStreamerElementsException(captureDevice, videorate, fpsfilter);
  }

  @Override
  public Pad getSrcPad() {
    return fpsfilter.getStaticPad("src");
  } 
}
