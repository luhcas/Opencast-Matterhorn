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
package org.opencastproject.capture.api;

/**
 * OSGi service for querying the capture device's current state (MH-58)
 */
public interface StatusService {
  /**
   * Gets the machine's current encoding status
   * 
   * @return The capture machine's current state
   */
  public String getState();

  /**
   * Sets the machine's current encoding status
   * 
   * @param state The state to change the machine into
   */
  public void setState(String state);
}

