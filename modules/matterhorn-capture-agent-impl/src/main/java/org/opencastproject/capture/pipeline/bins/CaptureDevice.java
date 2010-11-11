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

import org.opencastproject.capture.pipeline.SourceDeviceName;

/**
 * Simple representation of the location, friendly name, capture type and output file of a capture device.
 */
public class CaptureDevice {

  /** String representation of absolute path to device file */
  private String location;

  /** Device name */
  private SourceDeviceName sourceDeviceName;
  
  /** Friendly name defined in properties file */
  private String friendlyName;

  /** Name of the file to save the stream to */
  private String outputPath;
  
  /** A list of properties set for this device */
  public Properties properties;
  
  /**
   * Create a representation of a capture device for the PipelineFactory
   * 
   * @param location
   *          The location of the device on the system
   * @param sourceDeviceName
   *          The {@code DeviceName} object of the device
   * @param friendlyName
   *          The user friendly name of the device
   * @param outputPath
   *          The output path
   */
  public CaptureDevice(String location, SourceDeviceName sourceDeviceName, String friendlyName, String outputPath) {
    this.location = location;
    this.sourceDeviceName = sourceDeviceName;
    this.outputPath = outputPath;
    this.friendlyName = friendlyName;
    
    properties = new Properties();

  }

  public SourceDeviceName getName() {
    return sourceDeviceName;
  }

  public String getLocation() {
    return location;
  }

  public String getOutputPath() {
    return outputPath;
  }

  public String toString() {
    return "[" + sourceDeviceName + ", " + location + ": " + outputPath + "]";
  }
  
  public String getFriendlyName() {
    return friendlyName;
  }

}
