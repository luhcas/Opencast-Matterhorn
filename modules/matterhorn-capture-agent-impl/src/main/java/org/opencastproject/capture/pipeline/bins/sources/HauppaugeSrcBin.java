package org.opencastproject.capture.pipeline.bins.sources;

import java.util.Properties;

import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Pad;
import org.gstreamer.PadLinkReturn;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;

public class HauppaugeSrcBin extends FileSrcBin {
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
   * @param pipeline
   *          The {@code Pipeline} bin to add it to
   * @return True, if successful
   * @throws Exception 
   */
  public HauppaugeSrcBin(CaptureDevice captureDevice, Properties properties) throws Exception {
    super(captureDevice, properties);
  }

  @Override
  protected void createElements(){
    super.createElements();
    filesrc = ElementFactory.make("filesrc", null);
    queue = ElementFactory.make("queue", null);
    mpegpsdemux = ElementFactory.make("mpegpsdemux", null);
    mpegvideoparse = ElementFactory.make("mpegvideoparse", null);
    decoder = ElementFactory.make("mpeg2dec", null);
  }
  
  @Override
  protected void setElementProperties(){
    super.setElementProperties();
    filesrc.set("location", captureDevice.getLocation());
    // mpegpsdemux source pad is only available sometimes, therefore we need to add a listener to accept dynamic pads
    mpegpsdemux.connect(new Element.PAD_ADDED() {
      public void padAdded(Element arg0, Pad newPad) {
        if(newPad.getName().contains("video")){
          PadLinkReturn padLinkReturn = newPad.link(mpegvideoparse.getStaticPad("sink"));
          if(padLinkReturn != PadLinkReturn.OK){
            try {
              throw new UnableToLinkGStreamerElementsException(captureDevice, mpegpsdemux, mpegvideoparse);
            } catch (UnableToLinkGStreamerElementsException e) {
              logger.error(e.getMessage() + " because PadLinkReturn was " + padLinkReturn.toString() + " on Pad " + newPad.getName());
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
      return fpsfilter.getStaticPad("src");
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
