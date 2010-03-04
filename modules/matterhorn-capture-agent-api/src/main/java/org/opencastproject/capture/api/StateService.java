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
 * Service for querying the capture agent's current state (MH-58).
 */
public interface StateService {

  /**
   * Gets the internal Agent used to store this agent's state.
   * @return The Agent which represents this capture agent.
   * @see org.opencastproject.capture.admin.api.AgentState
   */
  public Agent getAgent();

  //TODO: explain why we are passing strings instead of something more type safe
  /**
   * Sets the state of the agent.  Note that this should not change the *actual* state of the agent, only update the StateService's record of its state.
   * @param state The state of the agent.  Should be defined in AgentState.
   * @see org.opencastproject.capture.admin.api.AgentState
   */
  public void setAgentState(String state);

  /**
   * Gets the state of the agent.
   * @return The state of the agent.  Should be defined in AgentState.  May be null in cases where the service implementation is not ready yet.
   * @see org.opencastproject.capture.admin.api.AgentState
   */
  public String getAgentState();

  //TODO: indicate the format of the string (is it an item defined in AgentState?  Maybe this whole interface needs an introduction as to why strings are being passed?)
  /**
   * Gets a map of recording ID and Recording pairs containing all of the recordings the system is aware of.
   * @return A map of recording-state pairs.  May be null if the implementation is not active yet.
   */
  public Map<String,Recording> getKnownRecordings();

  /**
   * Sets the recording's current state.
   * 
   * @param recordingID The ID of the recording.
   * @param state The state for the recording.  Defined in RecordingState.
   * @see org.opencastproject.capture.admin.api.RecordingState
   */
  public void setRecordingState(String recordingID, String state);
  
  /**
   * Gets the state of a recording.
   * 
   * @param recordingID The ID of the recording in question.
   * @return A state defined in RecordingState.  May return null if the implementation is not active.
   * @see org.opencastproject.capture.admin.api.RecordingState
   */
  public Recording getRecordingState(String recordingID);
}

