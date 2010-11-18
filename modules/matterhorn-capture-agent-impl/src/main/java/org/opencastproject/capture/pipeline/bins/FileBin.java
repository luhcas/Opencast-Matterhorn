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
   * @throws UnableToSetElementPropertyBecauseElementWasNullException  
   * @throws UnableToCreateGhostPadsForBinException 
   * @throws UnableToLinkGStreamerElementsException 
   * @throws CaptureDeviceNullPointerException 
   */
  public FileBin(CaptureDevice captureDevice, Properties properties) throws UnableToLinkGStreamerElementsException,
          UnableToCreateGhostPadsForBinException, UnableToSetElementPropertyBecauseElementWasNullException,
          CaptureDeviceNullPointerException {
    super(captureDevice, properties);
  }
  
  @Override
  protected void createElements(){
    filesrc = ElementFactory.make(GStreamerElements.FILESRC, null);
    filesink = ElementFactory.make(GStreamerElements.FILESINK, null);
  }
  
  @Override
  protected void setElementProperties(){
    filesrc.set(GStreamerProperties.LOCATION, captureDevice.getLocation());
    filesink.set(GStreamerProperties.LOCATION, captureDevice.getOutputPath());
  }
  
  @Override
  protected void addElementsToBin(){
    bin.addMany(filesrc, filesink);
  }
  
  @Override
  protected void linkElements() throws UnableToLinkGStreamerElementsException {
    if (!filesrc.link(filesink))
      throw new UnableToLinkGStreamerElementsException(captureDevice, filesrc, filesink);
  }

  @Override
  protected void createGhostPads() {
    
  }
}
