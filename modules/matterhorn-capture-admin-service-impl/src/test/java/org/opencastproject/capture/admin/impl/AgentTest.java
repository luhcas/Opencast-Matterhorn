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
import org.opencastproject.capture.admin.api.AgentState;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AgentTest {
  private Agent agent = null;
  private Long time = 0L;

  @Before
  public void setUp() {
    agent = new AgentImpl("test", AgentState.IDLE, "http://localhost/", null);
    Assert.assertNotNull(agent);
    time = agent.getLastHeardFrom();
  }

  @After
  public void tearDown() {
    agent = null;
    time = 0L;
  }

  @Test
  public void correctInformation() {
    Assert.assertEquals("test", agent.getName());
    Assert.assertEquals(AgentState.IDLE, agent.getState());
    Assert.assertEquals("http://localhost/", agent.getUrl());
  }
  
  @Test
  public void changedInformation() throws InterruptedException {
    Assert.assertEquals("test", agent.getName());
    Assert.assertEquals(AgentState.IDLE, agent.getState());
    Assert.assertEquals(time, agent.getLastHeardFrom());

    Thread.sleep(1);
    agent.setState(AgentState.CAPTURING);

    Assert.assertEquals("test", agent.getName());
    Assert.assertEquals(AgentState.CAPTURING, agent.getState());
    Thread.sleep(1);
    if (agent.getLastHeardFrom() <= time || agent.getLastHeardFrom() > System.currentTimeMillis()) {
      Assert.fail("Invalid checkin time");
    }
  }  
}
