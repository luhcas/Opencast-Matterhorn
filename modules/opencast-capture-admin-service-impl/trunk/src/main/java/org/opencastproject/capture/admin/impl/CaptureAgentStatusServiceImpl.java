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

import org.opencastproject.capture.admin.api.CaptureAgentStatusService;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FIXME -- Add javadocs
 */
public class CaptureAgentStatusServiceImpl implements CaptureAgentStatusService, ManagedService {
  private static final Logger logger = LoggerFactory.getLogger(CaptureAgentStatusServiceImpl.class);

  private static HashMap<String, String> agents;
  private static HashMap<String, String> recordings;

  public CaptureAgentStatusServiceImpl() {
    if (agents == null) {
      agents = new HashMap<String, String>();
    }
    if (recordings == null) {
      recordings = new HashMap<String, String>();
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.admin.api.CaptureAgentStatusService#getAgentState(java.lang.String)
   */
  public String getAgentState(String agentName) {
    return agents.get(agentName);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.admin.api.CaptureAgentStatusService#setAgentState(java.lang.String, java.lang.String)
   */
  public void setAgentState(String agentName, String state) {
    agents.put(agentName, state);
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
  public Map<String, String> getKnownAgents() {
    return agents;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.admin.api.CaptureAgentStatusService#getRecordingState(java.lang.String)
   */
  public String getRecordingState(String id) {
    return recordings.get(id);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.admin.api.CaptureAgentStatusService#setRecordingState(java.lang.String, java.lang.String)
   */
  public void setRecordingState(String id, String state) {
    recordings.put(id, state);
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
   * @see org.opencastproject.capture.admin.api.CaptureAgentStatusService#getAllRecordingStates()
   */
  public Map<String,String> getAllRecordingStates() {
    return recordings;
  }

  @SuppressWarnings("unchecked")
  public void updated(Dictionary props) throws ConfigurationException {
    // Update any configuration properties here
  }
}
