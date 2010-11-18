/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
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
  
  public PartialBin(CaptureDevice captureDevice, Properties properties) throws UnableToLinkGStreamerElementsException,
          UnableToCreateGhostPadsForBinException, UnableToSetElementPropertyBecauseElementWasNullException,
          CaptureDeviceNullPointerException {
    if(captureDevice != null){
      this.captureDevice = captureDevice;
    }
    else{
      throw new CaptureDeviceNullPointerException();
    }
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
  
  protected void createElements(){
    
  }
  
  protected void setElementProperties() throws UnableToSetElementPropertyBecauseElementWasNullException{
    
  }
  
  /** Descendents will use this to add all the elements they create to the Bin. **/
  protected void addElementsToBin(){
    
  }
  
  protected void createGhostPads() throws UnableToCreateGhostPadsForBinException{
    
  }
  
  /** Descendants will implement this to link all of their elements together in the proper order. 
   * @throws Exception **/
  protected void linkElements() throws UnableToLinkGStreamerElementsException{
    
  }
  
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
