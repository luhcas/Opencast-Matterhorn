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

import java.util.Map;

import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.capture.admin.api.Recording;

/**
 * OSGi service for querying the capture device's current state (MH-58)
 */
public interface StateService {

  /**
   * Gets the internal Agent used to store this agent's status
   * @return The Agent which represents this capture agent
   */
  public Agent getAgent();

  /**
   * Sets the state of the agent.  Note that this should not change the *actual* state of the agent, only update the StatusService's record of its state.
   * @param state The state of the agent.  Should be defined in AgentState.
   * @see org.opencastproject.capture.api.AgentState
   */
  public void setAgentState(String state);

  /**
   * Gets the state of the agent
   * @return The state of the agent (should be defined in AgentState)
   * @see org.opencastproject.capture.api.AgentState
   */
  public String getAgentState();

  /**
   * Gets the state of all recordings in the system
   * @return A map of recording-state pairs
   */
  public Map<String,Recording> getKnownRecordings();

  /**
   * Sets the recording's current state
   * 
   * @param recordingID The ID of the recording.
   * @param state The state for the recording.  Defined in RecordingState.
   * @see org.opencastproject.capture.api.RecordingState
   */
  public void setRecordingState(String recordingID, String state);
  
  /**
   * Gets the state of a recording
   * 
   * @param recordingID The ID of the recording in question
   * @return A state (should be defined in RecordingState)
   * @see org.opencastproject.capture.api.RecordingState
   */
  public Recording getRecordingState(String recordingID);
}

