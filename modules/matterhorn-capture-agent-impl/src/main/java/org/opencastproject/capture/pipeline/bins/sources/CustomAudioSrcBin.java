package org.opencastproject.capture.pipeline.bins.sources;

import java.util.Properties;

import org.gstreamer.Bin;
import org.gstreamer.Pad;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;

public class CustomAudioSrcBin extends SrcBin {

  private static final boolean LINK_UNUSED_GHOST_PADS = true;
  
  public CustomAudioSrcBin(CaptureDevice captureDevice, Properties properties) throws Exception {
    super(captureDevice, properties);
  }

  @Override
  protected Pad getSrcPad() {
    if(bin.getSinks().size() >= 1){
      return bin.getSinks().get(0).getStaticPad("src");
    }
    else{
      return null;
    }
  }

  @Override
  protected void createElements(){
    bin = Bin.launch(captureDeviceProperties.customSource, LINK_UNUSED_GHOST_PADS);
  }
  
  @Override
  protected void addElementsToBin() {

  }

  @Override
  protected void createGhostPads() throws Exception {
    
  }
  
  @Override
  protected void linkElements() throws Exception {

  }

  @Override
  protected void setElementProperties() throws Exception {

  }

  @Override
  public boolean isAudioDevice(){
    return true;
  }
  
}
