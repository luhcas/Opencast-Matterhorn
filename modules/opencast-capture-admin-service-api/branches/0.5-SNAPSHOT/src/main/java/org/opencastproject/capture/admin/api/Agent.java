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

/**
 * An in-memory construct to represent the state of a capture agent, and when it was last heard from 
 */
public class Agent {

  /**
   * The name of the agent (eg: agent1)
   */
  protected String name;

  /**
   * The state of the agent.  This should be defined from the constants in AgentState
   * @see org.opencastproject.capture.api.AgentState
   */
  protected String state;

  /**
   * The time at which the agent last checked in with this service.
   * Note that this is an absolute timestamp (ie, milliseconds since 1970) rather than a relative timestamp (ie, it's been 3000 ms since it last checked in) 
   */
  protected Long lastHeardFrom;

  /**
   * Builds a representation of the agent
   * @param agentName The name of the agent
   * @param agentState The state of the agent
   */
  public Agent(String agentName, String agentState) {
    name = agentName;
    this.setState(agentState);
  }

  /**
   * Gets the name of the agent
   * @return The name of the agent
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the state of the agent, and updates the time it was last heard from
   * 
   * @see org.opencastproject.capture.api.AgentState
   * @param newState The new state of the agent.  This can be equal to the current one if the goal is to update the timestamp.
   */
  public void setState(String newState) {
    state = newState;
    lastHeardFrom = System.currentTimeMillis();
  }

  /**
   * Gets the state of the agent
   * 
   * @see org.opencastproject.capture.api.AgentState
   * @return The state of the agent as defined in AgentState
   */
  public String getState() {
    return state;
  }

  /**
   * Gets the time at which the agent last checked in
   * @return The number of milliseconds since 1970 when the agent last checked in
   */
  public Long getLastCheckinTime() {
    return lastHeardFrom;
  }
}
