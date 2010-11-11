package org.opencastproject.capture.pipeline.bins.sources;

import java.util.Properties;

import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Pad;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;


public class V4lsrcBin extends VideoDeviceBin {

  Element v4lsrc;
  
  public V4lsrcBin(CaptureDevice captureDevice, Properties properties) throws Exception {
    super(captureDevice, properties);
  }
  
  @Override
  protected void createElements(){
    super.createElements();
    v4lsrc = ElementFactory.make("v4lsrc", null);
  }
  
  @Override
  protected void addElementsToBin() {
    bin.addMany(v4lsrc, queue, videorate, fpsfilter);
  }

  @Override
  protected void linkElements() throws Exception {
    /*if (!v4lsrc.link(queue))
      throw new UnableToLinkGStreamerElementsException(captureDevice, v4lsrc, queue);
    else if (!queue.link(videorate))
      throw new UnableToLinkGStreamerElementsException(captureDevice, queue, videorate);
    else if (!videorate.link(fpsfilter))
      throw new UnableToLinkGStreamerElementsException(captureDevice, videorate, fpsfilter);*/
    
    if (!v4lsrc.link(videorate))
      throw new UnableToLinkGStreamerElementsException(captureDevice, v4lsrc, videorate);
    else if (!videorate.link(fpsfilter))
      throw new UnableToLinkGStreamerElementsException(captureDevice, videorate, fpsfilter);
  }

  @Override
  public Pad getSrcPad() {
    return fpsfilter.getStaticPad("src");
  } 
}
