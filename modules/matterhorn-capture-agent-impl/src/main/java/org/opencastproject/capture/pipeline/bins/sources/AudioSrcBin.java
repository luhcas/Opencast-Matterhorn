package org.opencastproject.capture.pipeline.bins.sources;

import java.util.Properties;

import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;

public abstract class AudioSrcBin extends SrcBin{

  /** Super class for all audio sources whether they are test, pulse, alsa or other. **/
  public AudioSrcBin(CaptureDevice captureDevice, Properties properties) throws Exception {
    super(captureDevice, properties);
  }

  /** Audio convert is used to convert any input audio into a format usable by gstreamer. Might not be strictly necessary. **/
  Element audioconvert;
  
  /** Create all the common elements necessary for audio sources including a queue and an audio converter. **/
  protected void createElements(){
    super.createElements();
    createQueue();
    createAudioConverter();
  }
  
  private void createQueue() {
    queue = ElementFactory.make("queue", null);
  }

  private void createAudioConverter() {
    audioconvert = ElementFactory.make("audioconvert", null);
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
