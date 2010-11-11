package org.opencastproject.capture.pipeline.bins;

import java.util.Properties;

import org.gstreamer.Element;
import org.gstreamer.ElementFactory;

public class FileBin extends PartialBin {
  Element filesrc;
  Element filesink;
  
  /**
   * Adds a pipeline for a media file that just copies it to a new location
   * 
   * @param captureDevice
   *          The {@code CaptureDevice} with source and output information
   * @param pipeline
   *          The {@code Pipeline} bin to add it to
   * @return True, if successful
   * @throws Exception 
   */
  public FileBin(CaptureDevice captureDevice, Properties properties) throws Exception {
    super(captureDevice, properties);
  }
  
  @Override
  protected void createElements(){
    filesrc = ElementFactory.make("filesrc", null);
    filesink = ElementFactory.make("filesink", null);
  }
  
  @Override
  protected void setElementProperties(){
    filesrc.set("location", captureDevice.getLocation());
    filesink.set("location", captureDevice.getOutputPath());
  }
  
  @Override
  protected void addElementsToBin(){
    bin.addMany(filesrc, filesink);
  }
  
  @Override
  protected void linkElements() throws Exception {
    if (!filesrc.link(filesink))
      throw new UnableToLinkGStreamerElementsException(captureDevice, filesrc, filesink);
  }

  @Override
  protected void createGhostPads() throws Exception {
    
  }
}
