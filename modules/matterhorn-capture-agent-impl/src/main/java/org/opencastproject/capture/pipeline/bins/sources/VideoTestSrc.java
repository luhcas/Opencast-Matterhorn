package org.opencastproject.capture.pipeline.bins.sources;

import java.util.Properties;

import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Pad;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;

public class VideoTestSrc extends VideoDeviceBin {

  Element videotestsrc;
  
  public VideoTestSrc(CaptureDevice captureDevice, Properties properties) throws Exception {
    super(captureDevice, properties);
  }
  
  @Override
  protected void createElements(){
    super.createElements();
    videotestsrc = ElementFactory.make("videotestsrc", null);
  }
  
  @Override
  protected void addElementsToBin() {
    bin.addMany(videotestsrc, queue, videorate, fpsfilter);
  }

  @Override
  protected void linkElements() throws Exception {
    if (!videotestsrc.link(queue)){
      throw new UnableToLinkGStreamerElementsException(captureDevice, videotestsrc, queue);
    }
    else if (!queue.link(videorate)){
      throw new UnableToLinkGStreamerElementsException(captureDevice, queue, videorate);
    }
    else if (!videorate.link(fpsfilter)){
      throw new UnableToLinkGStreamerElementsException(captureDevice, videorate, fpsfilter);
    }
  }

  @Override
  public Pad getSrcPad() {
    return fpsfilter.getStaticPad("src");
  }
}
