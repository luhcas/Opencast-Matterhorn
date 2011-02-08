/**
 *  Copyright 2009, 2010 The Regents of the University of California
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

import org.opencastproject.capture.admin.api.Agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * An in-memory construct to represent the state of a capture agent, and when it was last heard from.
 */
@Entity
@Table(name = "CAPTURE_AGENT_STATE")
@NamedQueries({ @NamedQuery(name = "AgentImpl.getAll", query = "SELECT a FROM AgentImpl a") })
public class AgentImpl implements Agent {

  private static final Logger log = LoggerFactory.getLogger(AgentImpl.class);

  /**
   * The name of the agent.
   */
  @Id
  protected String name;

  /**
   * The state of the agent. This should be defined from the constants in AgentState.
   */
  @Column(name = "STATE", nullable = false)
  protected String state;

  /**
   * The URL of the agent. This is determined from the referer header parameter when the agent is registered.
   * 
   */
  @Column(name = "URL")
  protected String url;

  /**
   * The time at which the agent last checked in with this service. Note that this is an absolute timestamp (ie,
   * milliseconds since 1970) rather than a relative timestamp (ie, it's been 3000 ms since it last checked in).
   */
  @Column(name = "LAST_HEARD_FROM", nullable = false)
  protected Long lastHeardFrom;

  /**
   * The capabilities the agent has Capabilities are the devices this agent can record from, with a friendly name
   * associated to determine their nature (e.g. PRESENTER --> dev/video0)
   */
  @Column(name = "CAPABILITIES", nullable = true, length = 65535)
  @Lob
  protected String capabilities;

  //Private var to store the caps as a properties object.
  @Transient
  private Properties caps;

  /**
   * Required 0-arg constructor for JAXB, creates a blank agent.
   */
  public AgentImpl() {
  };

  /**
   * Builds a representation of the agent.
   * 
   * @param agentName
   *          The name of the agent.
   * @param agentState
   *          The state of the agent. This should be defined from the constants in AgentState
   */
  public AgentImpl(String agentName, String agentState, String agentUrl, Properties capabilities) {
    name = agentName;
    this.setState(agentState);
    this.setUrl(agentUrl);
    // Agents with no capabilities are allowed. These can/will be updated after the agent is built if necessary.
    setCapabilities(capabilities);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.admin.api.Agent#getName()
   */
  public String getName() {
    return name;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.admin.api.Agent#setState(java.lang.String)
   */
  public void setState(String newState) {
    state = newState;
    setLastHeardFrom(System.currentTimeMillis());
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.admin.api.Agent#getState()
   */
  public String getState() {
    return state;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.admin.api.Agent#setUrl(java.lang.String)
   */
  public void setUrl(String agentUrl) {
    url = agentUrl;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.admin.api.Agent#getUrl()
   */
  public String getUrl() {
    return url;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.admin.api.Agent#setLastHeardFrom(java.lang.Long)
   */
  public void setLastHeardFrom(Long time) {
    lastHeardFrom = time;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.admin.api.Agent#getLastHeardFrom()
   */
  public Long getLastHeardFrom() {
    return lastHeardFrom;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.admin.api.Agent#getCapabilities()
   */
  public Properties getCapabilities() {
    return caps;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.admin.api.Agent#setCapabilities(java.util.Properties)
   */
  public void setCapabilities(Properties capabilities) {
    if (capabilities == null) {
      return;
    }

    this.caps = capabilities;
    try {
      StringWriter sw = new StringWriter();
      capabilities.store(sw, "");
      this.capabilities = sw.toString();
    } catch (IOException e) {
      log.warn("Unable to store agent " + "'s capabilities to the database, IO exception occurred.", e);
    }
  }

  /**
   * Post load method to load the capabilities from a string to the properties object
   */
  @SuppressWarnings("unused")
  @PostLoad
  private void loadCaps() {
    this.caps = new Properties();
    try {
      caps.load(new StringReader(this.capabilities));
    } catch (IOException e) {
      log.warn("Unable to load agent " + name + "'s capabilities, IO exception occurred.", e);
    }
  }
}
