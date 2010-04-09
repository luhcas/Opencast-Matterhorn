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
package org.opencastproject.capture.api;

import java.util.List;

/** 
 * OSGi service for video confidence monitoring
 */
public interface VideoMonitor {

  /**
   * Return the JPEG image monitor associated with the device
   * 
   * @param friendlyName Friendly name of device to get source from
   * @return a byte array in jpeg form
   */
  byte[] grabFrame(String friendlyName);
  
  /**
   * Provide access to the devices on the capture box
   * 
   * @return the list of friendly device names associated with the capture agent
   */
  List<String> getFriendlyNames();
  
}
