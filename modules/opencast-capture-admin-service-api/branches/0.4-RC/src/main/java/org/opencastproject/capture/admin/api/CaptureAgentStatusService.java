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
package org.opencastproject.capture.admin.api;

import java.util.Map;

/**
 * API for the capture-admin service (MH-1336, MH-1394, MH-1457, MH-1475 and MH-1476)
 */
public interface CaptureAgentStatusService {

  /**
   * Returns the last known state of a given agent
   * @param agentName The name of the agent
   * @return The agent with a value for both its last checkin time and its last known state
   */
  public Agent getAgentState(String agentName);

  /**
   * Sets a given agent's state.  Note that this will create the agent if it does not already exist.
   * @param agenName The name of the agent
   * @param state The current state of the agent
   */
  public void setAgentState(String agentName, String state);

  /**
   * Remove an agent from the system, if the agent exists
   * @param agentName The name of the agent
   */
  public void removeAgent(String agentName);

  /**
   * Returns the list of known agents
   * @return A map of agent-state pairs
   */
  public Map<String, Agent> getKnownAgents();

  /**
   * Gets the state of a recording, if it exists
   * @param id The id of the recording
   * @return The state of the recording, or null if it does not exist
   */
  public Recording getRecordingState(String id);

  /**
   * Updates the state of a recording with the given state, if it exists
   * @param id The id of the recording in the system
   * @param state The state to set for that recording
   */
  public void setRecordingState(String id, String state);

  /**
   * Removes a recording from the system, if the recording exists
   * @param id The id of the recording to remove
   */
  public void removeRecording(String id);
  
  /**
   * Gets the state of all recordings in the system
   * @return A map of recording-state pairs
   */
  public Map<String,Recording> getKnownRecordings();
}
