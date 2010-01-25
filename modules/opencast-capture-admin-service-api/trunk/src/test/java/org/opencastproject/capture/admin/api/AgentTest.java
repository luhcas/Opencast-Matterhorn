package org.opencastproject.capture.admin.api;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.capture.admin.api.AgentState;

public class AgentTest {
  private Agent agent = null;
  private Long time = 0L;

  @Before
  public void setup() {
    agent = new Agent("test", AgentState.IDLE);
    Assert.assertNotNull(agent);
    time = agent.getLastCheckinTime();
  }

  @After
  public void teardown() {
    agent = null;
    time = 0L;
  }

  @Test
  public void correctInformation() {
    Assert.assertEquals("test", agent.getName());
    Assert.assertEquals(AgentState.IDLE, agent.getState());
  }
  
  @Test
  public void changedInformation() throws InterruptedException {
    Assert.assertEquals("test", agent.getName());
    Assert.assertEquals(AgentState.IDLE, agent.getState());
    Assert.assertEquals(time, agent.getLastCheckinTime());

    Thread.sleep(1);
    agent.setState(AgentState.CAPTURING);

    Assert.assertEquals("test", agent.getName());
    Assert.assertEquals(AgentState.CAPTURING, agent.getState());
    Thread.sleep(1);
    if (agent.getLastCheckinTime() <= time || agent.getLastCheckinTime() >= System.currentTimeMillis()) {
      Assert.fail("Invalid checkin time");
    }
  }  
}
package org.opencastproject.capture.admin.api;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.capture.admin.api.AgentState;

public class AgentTest {
  private Agent agent = null;
  private Long time = 0L;

  @Before
  public void setup() {
    agent = new Agent("test", AgentState.IDLE);
    Assert.assertNotNull(agent);
    time = agent.getLastCheckinTime();
  }

  @After
  public void teardown() {
    agent = null;
    time = 0L;
  }

  @Test
  public void correctInformation() {
    Assert.assertEquals("test", agent.getName());
    Assert.assertEquals(AgentState.IDLE, agent.getState());
  }
  
  @Test
  public void changedInformation() throws InterruptedException {
    Assert.assertEquals("test", agent.getName());
    Assert.assertEquals(AgentState.IDLE, agent.getState());
    Assert.assertEquals(time, agent.getLastCheckinTime());

    Thread.sleep(1);
    agent.setState(AgentState.CAPTURING);

    Assert.assertEquals("test", agent.getName());
    Assert.assertEquals(AgentState.CAPTURING, agent.getState());
    Thread.sleep(1);
    if (agent.getLastCheckinTime() <= time || agent.getLastCheckinTime() >= System.currentTimeMillis()) {
      Assert.fail("Invalid checkin time");
    }
  }  
}
