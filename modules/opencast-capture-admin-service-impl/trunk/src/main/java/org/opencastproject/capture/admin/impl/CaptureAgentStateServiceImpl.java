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
package org.opencastproject.capture.admin.impl;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.capture.admin.api.AgentState;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.capture.admin.api.Recording;
import org.opencastproject.capture.admin.api.RecordingState;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FIXME -- Add javadocs
 */
public class CaptureAgentStateServiceImpl implements CaptureAgentStateService, ManagedService {
  private static final Logger logger = LoggerFactory.getLogger(CaptureAgentStateServiceImpl.class);

  private HashMap<String, Agent> agents;
  private HashMap<String, Recording> recordings;

  public CaptureAgentStateServiceImpl() {
    logger.info("CaptureAgentStateServiceImpl starting.");
    agents = new HashMap<String, Agent>();
    recordings = new HashMap<String, Recording>();
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#getAgentState(java.lang.String)
   */
  public Agent getAgentState(String agentName) {
    Agent req = agents.get(agentName);
    //If that agent doesn't exist, return an unknown agent, else return the known agent
    if (req == null) {
      logger.debug("Agent {} does not exist in the system.", agentName);
      Agent a = new Agent(agentName, AgentState.UNKNOWN);
      return a;
    } else {
      logger.debug("Agent {} found, returning state.", agentName);
      return req;
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#setAgentState(java.lang.String, java.lang.String)
   */
  public void setAgentState(String agentName, String state) {
    Agent req = agents.get(agentName);
    //if the agent is known set the state
    if (req != null) {
      logger.debug("Setting Agent {} to state {}.", agentName, state);
      req.setState(state);
    } else {
      if (agentName == null || agentName.equals("")) {
        logger.debug("Unable to set agent state, agent name is blank or null");
        return;
      } else if (state == null) {
        logger.debug("Unable to set agent state, no state specified");
        return;
      }
      logger.debug("Creating Agent {} with state {}.", agentName, state);
      Agent a = new Agent(agentName, state);
      agents.put(agentName, a);
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#removeAgent(java.lang.String)
   */
  public void removeAgent(String agentName) {
    logger.debug("Removing Agent {}.", agentName);
    agents.remove(agentName);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#getKnownAgents()
   */
  public Map<String, Agent> getKnownAgents() {
    return agents;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#getAgentCapabilities()
   */
  public Map<String, String> getAgentCapabilities() {
    //TODO:  Fill this in for MH-1336
    return null;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#getRecordingState(java.lang.String)
   */
  public Recording getRecordingState(String id) {
    Recording req = recordings.get(id);
    //If that agent doesn't exist, return an unknown agent, else return the known agent
    if (req == null) {
      logger.debug("Recording {} does not exist in the system.", id);
      Recording r = new Recording(id, RecordingState.UNKNOWN);
      return r;
    } else {
      logger.debug("Recording {} found, returning state.", id);
      return req;
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#setRecordingState(java.lang.String, java.lang.String)
   */
  public void setRecordingState(String id, String state) {
    Recording req = recordings.get(id);
    if (req != null) {
      logger.debug("Setting Recording {} to state {}.", id, state);
      req.setState(state);
    } else {
      if (id == null || id.equals("")) {
        logger.debug("Unable to set agent state, agent name is blank or null");
        return;
      }
      logger.debug("Creating Recording {} with state {}.", id, state);
      Recording r = new Recording(id, state);
      recordings.put(id, r);
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#removeRecording(java.lang.String)
   */
  public void removeRecording(String id) {
    logger.debug("Removing Recording {}.", id);
    recordings.remove(id);
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#getKnownRecordings()
   */
  public Map<String,Recording> getKnownRecordings() {
    return recordings;
  }

  public void updated(Dictionary props) throws ConfigurationException {
    // Update any configuration properties here
  }
}
