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

import org.opencastproject.media.mediapackage.MediaPackage;

import java.util.Properties;

/**
 * OSGi service for starting capture (MH-730)
 */
public interface CaptureAgent {
  /**
   * Starting a simple capture.
   */
  String startCapture();

  // FIXME: What happens if there is an ongoing recording? Looking at the implementation, we can see that <code>null</code>
  // is returned, which should be documented. Even better would be do throw a checked exception, since this looks like
  // an illegal state to us.
  // FIXME: The @return javadoc is missing.
  /**
   * Starting a simple capture.
   * 
   * @param mediaPackage 
   */
  String startCapture(MediaPackage mediaPackage);

  // FIXME: What are these properties?  How do I know the valid keys?
  // FIXME: See comments on #startCapture(MediaPackage)
  /**
   * Starting a simple capture.
   * 
   * @param configuration HashMap<String, String> for properties.
   */
  String startCapture(Properties configuration);

  //TODO: provide @see link for configuration properties, or at least an a href link to a configurations file in the repo
  // FIXME: See comments on #startCapture(MediaPackage)
  /**
   * Starting a simple capture.
   * 
   * @param mediaPackage 
   * @param configuration HashMap<String, String> for properties.
   */
  String startCapture(MediaPackage mediaPackage, Properties configuration);
  
  // FIXME: Javadocs are talking about a string instead of boolean
  // FIXME: What does "false" as a return value mean? Was the capture not stopped, or was there no capture running?
  // Even better would be to return void, with checked exceptions indicating various problems.
  /**
   * Stops the capture
   * @return A string indicating the success or fail of the action
   */
  boolean stopCapture();

  // FIXME: document why the recordingId is necessary.  There doesn't seem to be a need for this parameter, since there
  // should only be one recording at a time.
  /**
   * Stops the capture
   * This version takes in a recording ID and only stops the recording if that ID matches the current recording's ID.
   * @param recordingID The ID of the recording you wish to stop
   * @return A string indicating the success or fail of the action
   */
  boolean stopCapture(String recordingID);

  // FIXME: This method should return an instance of the class (or better: enum) AgentState rather than a string
  /**
   * Gets the machine's current state.
   * 
   * @return A state (should be defined in AgentState)
   * @see org.opencastproject.capture.admin.api.AgentState
   */
  String getAgentState();

  // FIXME: In this interface, we would expect to find information about the agent's capabilities as well. 
  //String[] getAgentCapabilities();
  
}

