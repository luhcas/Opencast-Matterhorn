package org.opencastproject.capture.pipeline.bins.sources;

import java.util.Properties;

import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Pad;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;

public class PulseAudioSrcBin extends AudioSrcBin {

  Element pulseAudioSrc;
  
  public PulseAudioSrcBin(CaptureDevice captureDevice, Properties properties) throws Exception {
    super(captureDevice, properties);
  }

  @Override
  protected void createElements(){
    pulseAudioSrc = ElementFactory.make("pulsesrc", null);
  }
  
  @Override
  public Pad getSrcPad() {
    return pulseAudioSrc.getStaticPad("src");
  }

  @Override
  protected void addElementsToBin() {
    bin.add(pulseAudioSrc);
  }

  @Override
  protected void linkElements() throws Exception {

  }

  @Override
  protected void setElementProperties() {

  }

  @Override
  public boolean isVideoDevice(){
    return false;
  }
  
  @Override
  public boolean isAudioDevice(){
    return true;
  }
  
}
