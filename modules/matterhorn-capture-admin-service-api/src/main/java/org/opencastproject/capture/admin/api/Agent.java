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

import java.util.Properties;

/**
 * An in-memory construct to represent the state of a capture agent, and when it was last heard from.
 */
// FIXME: This should be cleaned to conform a real API style and move the implementation to other file
public class Agent {

  /**
   * The name of the agent.
   */
  protected String name;

  /**
   * The state of the agent.  This should be defined from the constants in {@link org.opencastproject.capture.admin.api.AgentState}.
   * @see AgentState
   */
  protected String state;

  /**
   * The time at which the agent last checked in with this service.
   * Note that this is an absolute timestamp (ie, milliseconds since 1970) rather than a relative timestamp (ie, it's been 3000 ms since it last checked in). 
   */
  protected Long lastHeardFrom;

  /**
   * The capabilities the agent has
   * Capabilities are the devices this agent can record from, with a friendly name associated
   * to determine their nature (e.g. PRESENTER --> dev/video0)
   */
  protected Properties capabilities;

  /**
   * Builds a representation of the agent.
   *
   * @param agentName The name of the agent.
   * @param agentState The state of the agent.  This should be defined from the constants in {@link org.opencastproject.capture.admin.api.AgentState}.
   * @see AgentState
   */
  public Agent(String agentName, String agentState, Properties capabilities) {
    name = agentName;
    this.setState(agentState);
    // TODO: agents with no capabilities are allowed? (i.e. with capabilities == null)
    this.capabilities = capabilities;
  }


  /**
   * Gets the name of the agent.
   *
   * @return The name of the agent.
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the state of the agent, and updates the time it was last heard from.
   *
   * @param newState The new state of the agent.  This should defined from the constants in {@link org.opencastproject.capture.admin.api.AgentState}.  This can be equal to the current one if the goal is to update the timestamp.
   * @see AgentState
   */
  public void setState(String newState) {
    state = newState;
    lastHeardFrom = System.currentTimeMillis();
  }

  /**
   * Gets the state of the agent.
   *
   * @return The state of the agent.  This should be defined from the constants in {@link org.opencastproject.capture.admin.api.AgentState}.
   * @see AgentState
   */
  public String getState() {
    return state;
  }

  /**
   * Gets the time at which the agent last checked in.
   *
   * @return The number of milliseconds since 1970 when the agent last checked in.
   */
  public Long getLastHeardFrom() {
    return lastHeardFrom;
  }

  public Properties getCapabilities() {
    return capabilities;
  }

  public void setCapabilities(Properties capabilities) {
    this.capabilities = capabilities;
  }
}
