package org.opencastproject.capture.pipeline.bins.sources;

import java.util.Properties;

import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Pad;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;

public class BlueCherrySrcBin extends V4l2srcDeviceBin{
  Element v4l2src;
  /**
   * Adds a pipeline specifically designed to captured from the Bluecherry Provideo cards to the main pipeline
   * 
   * @param captureDevice
   *          The Bluecherry {@code CaptureDevice} to create pipeline around
   * @param properties
   *          The {@code Properties} of the confidence monitoring.
   * @return True, if successful
   * @throws Exception - When something in the bin doesn't link together correctly.
   */
  public BlueCherrySrcBin(CaptureDevice captureDevice, Properties properties) throws Exception {
    super(captureDevice, properties);
  }

  /** Create the v4l2src Element **/
  @Override
  protected void createElements(){
    super.createElements();
    v4l2src = ElementFactory.make("v4l2src", null);
  }
  
  /** Add the v4l2src, queue, videorate corrector and fpsfilter to the source bin. **/
  @Override
  protected void addElementsToBin() {
    bin.addMany(v4l2src, queue, videorate, fpsfilter);
  }

  /** Link together all of the {@code Element}s in this bin so that we can use 
   * them as a source.
   * @throws Exception - When something in the bin doesn't link together correctly we throw an exception. **/ 
  @Override
  protected void linkElements() throws Exception {
    if (!v4l2src.link(queue)){
      throw new UnableToLinkGStreamerElementsException(captureDevice, v4l2src, queue);
    }
    else if (!queue.link(videorate)){
      throw new UnableToLinkGStreamerElementsException(captureDevice, queue, videorate);
    }
    else if (!videorate.link(fpsfilter)){
      throw new UnableToLinkGStreamerElementsException(captureDevice, videorate, fpsfilter);
    }
  }

  /** Returns the fpsfilter element as the sink for this source bin since it is the last part of this source. **/ 
  @Override
  public Pad getSrcPad() {
    return fpsfilter.getStaticPad("src");
  }
}
