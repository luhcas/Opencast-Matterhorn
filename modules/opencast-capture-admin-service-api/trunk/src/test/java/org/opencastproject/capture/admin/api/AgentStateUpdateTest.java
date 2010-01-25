package org.opencastproject.capture.admin.api;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.capture.admin.api.AgentState;

public class AgentStateUpdateTest {
  private Agent agent = null;
  private AgentStateUpdate asu = null;

  @Before
  public void setup() throws InterruptedException {
    agent = new Agent("test", AgentState.IDLE);
    Assert.assertNotNull(agent);
    Thread.sleep(1);
    asu = new AgentStateUpdate(agent);
    Assert.assertNotNull(asu);
  }

  @After
  public void teardown() {
    agent = null;
    asu = null;
  }

  @Test
  public void correctInformation() {
    Assert.assertEquals("test", asu.name);
    Assert.assertEquals(AgentState.IDLE, asu.state);
    if (asu.time_delta <= 1) {
      Assert.fail("Invalid update time in agent state update");
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

public class AgentStateUpdateTest {
  private Agent agent = null;
  private AgentStateUpdate asu = null;

  @Before
  public void setup() throws InterruptedException {
    agent = new Agent("test", AgentState.IDLE);
    Assert.assertNotNull(agent);
    Thread.sleep(1);
    asu = new AgentStateUpdate(agent);
    Assert.assertNotNull(asu);
  }

  @After
  public void teardown() {
    agent = null;
    asu = null;
  }

  @Test
  public void correctInformation() {
    Assert.assertEquals("test", asu.name);
    Assert.assertEquals(AgentState.IDLE, asu.state);
    if (asu.time_delta <= 1) {
      Assert.fail("Invalid update time in agent state update");
    }
  }
}
