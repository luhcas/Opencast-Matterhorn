package org.opencastproject.capture.pipeline.bins.sources;

import java.util.Properties;

import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Pad;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;


public class DV1394SrcBin extends VideoDeviceBin {
  Element dv1394src;
  Element demux;
  Element decoder;
  Element ffmpegcolorspace;
  
  /**
   * Adds a pipeline specifically designed to captured from a DV Camera attached by firewire to the main pipeline
   * 
   * @deprecated  This function has not been maintained in a long time and has many problems.  If you need DV support let the list know.
   * @param captureDevice
   *          DV Camera attached to firewire {@code CaptureDevice} to create pipeline around
   * @param properties
   *          The {@code Properties} of the confidence monitoring.
   * @throws Exception - If something doesn't link together correctly we throw an exception. 
   */
  public DV1394SrcBin(CaptureDevice captureDevice, Properties properties) throws Exception {
    super(captureDevice, properties);
  }
  
  @Override
  protected void createElements(){
    super.createElements();
    dv1394src = ElementFactory.make("dv1394src", null);
    /* set up dv stream decoding */
    demux = ElementFactory.make("dvdemux", null);
    decoder   = ElementFactory.make("dvdec", null);
    ffmpegcolorspace = ElementFactory.make("ffmpegcolorspace", null);
  }
  @Override
  protected void setElementProperties(){
    super.setElementProperties();
    /* handle demuxer's sometimes pads. Filter for just video. */
    demux.connect(new Element.PAD_ADDED() {
      public void padAdded(Element element, Pad pad) {
        logger.info("Element: {}, Pad: {}", element.getName(), pad.getName());
        Element.linkPadsFiltered(demux, "video", decoder, "sink", Caps.fromString("video/x-dv"));
      }
    });
  }
  
  @Override
  protected void addElementsToBin(){
    bin.addMany(dv1394src, demux, decoder, videorate, fpsfilter, ffmpegcolorspace);
    
    demux.connect(new Element.PAD_ADDED() {
      public void padAdded(Element element, Pad pad) {
        pad.link(decoder.getStaticPad("sink"));
      }
    });
  }
  
  @Override
  protected void linkElements() throws Exception{
    if (!dv1394src.link(queue)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, dv1394src, queue);
    } 
    else if (!queue.link(demux)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, queue, demux);
    } 
    else if (!decoder.link(videorate)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, decoder, videorate);
    } 
    else if (!videorate.link(fpsfilter)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, videorate, fpsfilter);
    }
    else if (!fpsfilter.link(ffmpegcolorspace)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, fpsfilter, ffmpegcolorspace);
    }
  }

  @Override
  public Pad getSrcPad() {
    return decoder.getStaticPad("src");
  }
}
