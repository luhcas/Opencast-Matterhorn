package org.opencastproject.capture.pipeline.bins;

import java.util.Properties;

import org.gstreamer.Bin;
import org.opencastproject.capture.pipeline.PipelineFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PartialBin {
  protected Bin bin = new Bin();
  protected static final Logger logger = LoggerFactory.getLogger(PipelineFactory.class);
  protected CaptureDevice captureDevice;
  protected Properties properties;
  protected CaptureDeviceProperties captureDeviceProperties;
  protected ConfidenceMonitoringProperties confidenceMonitoringProperties;
  
  public PartialBin(CaptureDevice captureDevice, Properties properties) throws Exception{
    this.captureDevice = captureDevice;
    this.properties = properties;
    getCaptureProperties();
    createElements();
    setElementProperties();
    addElementsToBin();
    createGhostPads();
    linkElements();
  }

  /** Renders the capture device properties into usable data objects. 
   * @param confidenceMonitoringProperties 
   * @param captureDevice **/ 
  private void getCaptureProperties() {
    captureDeviceProperties = new CaptureDeviceProperties(captureDevice, properties);
    confidenceMonitoringProperties = new ConfidenceMonitoringProperties(captureDevice, properties);
  }
  
  abstract protected void createElements();
  abstract protected void setElementProperties() throws Exception;
  
  /** Descendents will use this to add all the elements they create to the Bin. **/
  abstract protected void addElementsToBin();
  
  abstract protected void createGhostPads() throws Exception;
  
  /** Descendants will implement this to link all of their elements together in the proper order. 
   * @throws Exception **/
  abstract protected void linkElements() throws Exception;
  
  public Bin getBin(){
    return bin;
  }
  
  public boolean isAudioDevice(){
    return false;
  }
  
  public boolean isVideoDevice(){
    return false;
  }
}
