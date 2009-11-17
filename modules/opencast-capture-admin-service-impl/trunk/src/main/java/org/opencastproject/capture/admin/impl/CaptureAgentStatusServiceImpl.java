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
import org.opencastproject.capture.admin.api.CaptureAgentStatusService;
import org.opencastproject.capture.admin.api.Recording;
import org.opencastproject.capture.api.AgentState;
import org.opencastproject.capture.api.RecordingState;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FIXME -- Add javadocs
 */
public class CaptureAgentStatusServiceImpl implements CaptureAgentStatusService, ManagedService {
  private static final Logger logger = LoggerFactory.getLogger(CaptureAgentStatusServiceImpl.class);

  private HashMap<String, Agent> agents;
  private HashMap<String, Recording> recordings;

  public CaptureAgentStatusServiceImpl() {
    if (agents == null) {
      agents = new HashMap<String, Agent>();
    }
    if (recordings == null) {
      recordings = new HashMap<String, Recording>();
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.admin.api.CaptureAgentStatusService#getAgentState(java.lang.String)
   */
  public Agent getAgentState(String agentName) {
    Agent req = agents.get(agentName);
    //If that agent doesn't exist, return an unknown agent, else return the known agent
    if (req == null) {
      Agent a = new Agent(agentName, AgentState.UNKNOWN);
      return a;
    } else {
      return req;
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.admin.api.CaptureAgentStatusService#setAgentState(java.lang.String, java.lang.String)
   */
  public void setAgentState(String agentName, String state) {
    Agent req = agents.get(agentName);
    //if the agent is known set the state
    if (req != null) {
      req.setState(state);
    } else {
      Agent a = new Agent(agentName, state);
      agents.put(agentName, a);
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.admin.api.CaptureAgentStatusService#removeAgent(java.lang.String)
   */
  public void removeAgent(String agentName) {
    agents.remove(agentName);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.admin.api.CaptureAgentStatusService#getKnownAgents()
   */
  public Map<String, Agent> getKnownAgents() {
    return agents;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.admin.api.CaptureAgentStatusService#getRecordingState(java.lang.String)
   */
  public Recording getRecordingState(String id) {
    Recording req = recordings.get(id);
    //If that agent doesn't exist, return an unknown agent, else return the known agent
    if (req == null) {
      Recording r = new Recording(id, RecordingState.UNKNOWN);
      return r;
    } else {
      return req;
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.admin.api.CaptureAgentStatusService#setRecordingState(java.lang.String, java.lang.String)
   */
  public void setRecordingState(String id, String state) {
    Recording req = recordings.get(id);
    if (req != null) {
      req.setState(state);
    } else {
      Recording r = new Recording(id, state);
      recordings.put(id, r);
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.admin.api.CaptureAgentStatusService#removeRecording(java.lang.String)
   */
  public void removeRecording(String id) {
    recordings.remove(id);
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.admin.api.CaptureAgentStatusService#getKnownRecordings()
   */
  public Map<String,Recording> getKnownRecordings() {
    return recordings;
  }


  @SuppressWarnings("unchecked")
  public void updated(Dictionary props) throws ConfigurationException {
    // Update any configuration properties here
  }
}
