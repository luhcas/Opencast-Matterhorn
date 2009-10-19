/**
 *  Copyright 2009 The Regents of the University of California
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
package org.opencastproject.capture.pipeline;

/**
 * Simple representation of the location, name, capture type and output file of a capture device.
 */
public class CaptureDevice {

  /** String representation of absolute path to device file */
  private String location;

  /** Device name */
  private DeviceName name;

  /** Name of the file to save the stream to */
  private String outputPath;

  /**
   * Create a representation of a capture device for the PipelineFactory
   * 
   * @param loc
   *          The location of the device on the system
   * @param ct
   *          The CaptureType of the device
   * @param name
   *          The name of the device
   */
  public CaptureDevice(String loc, DeviceName name, String output) {
    this.location = loc;
    this.name = name;
    this.outputPath = output;

  }

  public DeviceName getName() {
    return name;
  }

  public String getLocation() {
    return location;
  }

  public String getOutputPath() {
    return outputPath;
  }

  public String toString() {
    return "[" + name + ", " + location + ": " + outputPath + "]";
  }

}
