package org.opencastproject.capture.admin.impl;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.capture.admin.api.AgentState;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.capture.admin.api.Recording;
import org.opencastproject.capture.admin.api.RecordingState;
import org.opencastproject.capture.admin.impl.CaptureAgentStateServiceImpl;

public class CaptureAgentStateServiceImplTest {
  private CaptureAgentStateService service = null;
  private Properties capabilities;
  
  @Before
  public void setup() {
    service = new CaptureAgentStateServiceImpl();
    Assert.assertNotNull(service);
    capabilities = new Properties();
    capabilities.setProperty("CAMERA", "/dev/video0");
    capabilities.setProperty("SCREEN", "/dev/video1");
    capabilities.setProperty("AUDIO", "hw:0");
  }

  @After
  public void teardown() {
    service = null;
  }

  @Test
  public void nonExistantAgent() {
    Agent agent = service.getAgentState("doesNotExist");
    Assert.assertNull(agent);
  }

  @Test
  public void noAgents() {
    Assert.assertEquals(0, service.getKnownAgents().size());
  }

  @Test
  public void badAgentStates() {
    service.setAgentState(null, "something");
    Assert.assertEquals(0, service.getKnownAgents().size());
    service.setAgentState("", "something");
    Assert.assertEquals(0, service.getKnownAgents().size());
    service.setAgentState("something", null);
    Assert.assertEquals(0, service.getKnownAgents().size());
  }

  @Test
  public void badAgentCapabilities() {
    service.setAgentCapabilities(null, capabilities);
    Assert.assertEquals(0, service.getKnownAgents().size());
    service.setAgentCapabilities("", capabilities);
    Assert.assertEquals(0, service.getKnownAgents().size());
    service.setAgentState("something", null);
    Assert.assertEquals(0, service.getKnownAgents().size());
  }

  private void verifyAgent(String name, String state, Properties caps) {
    Agent agent = service.getAgentState(name);
    
    if (agent != null) {
      Assert.assertEquals(name, agent.getName());
      Assert.assertEquals(state, agent.getState());
      Assert.assertEquals(caps, agent.getCapabilities());
    } else if (state != null)
      Assert.fail();
  }

  @Test
  public void oneAgentState() {
    service.setAgentState("agent1", AgentState.IDLE);
    Assert.assertEquals(1, service.getKnownAgents().size());

    verifyAgent("notAgent1", null, null);
    verifyAgent("agent1", AgentState.IDLE, null);

    service.setAgentState("agent1", AgentState.CAPTURING);
    Assert.assertEquals(1, service.getKnownAgents().size());

    verifyAgent("notAgent1",null, null);
    verifyAgent("agent1", AgentState.CAPTURING, null);
  }
  
  @Test
  public void oneAgentCapabilities() {
    service.setAgentCapabilities("agent1", capabilities);
    Assert.assertEquals(1, service.getKnownAgents().size());

    verifyAgent("notAgent1", null, null);
    verifyAgent("agent1", AgentState.UNKNOWN, capabilities);

    service.setAgentState("agent1", AgentState.IDLE);
    Assert.assertEquals(1, service.getKnownAgents().size());

    verifyAgent("notAgent1",null, null);
    verifyAgent("agent1", AgentState.IDLE, capabilities);
    
    service.setAgentCapabilities("agent1", new Properties());
    Assert.assertEquals(1, service.getKnownAgents().size());
    
    verifyAgent("notAnAgent", null, null);
    verifyAgent("agent1", AgentState.IDLE, new Properties());
  }


  @Test
  public void removeAgent() {
    service.setAgentCapabilities("agent1", capabilities);
    Assert.assertEquals(1, service.getKnownAgents().size());
    service.setAgentCapabilities("agent2", capabilities);
    service.setAgentState("agent2", AgentState.UPLOADING);

    verifyAgent("notAnAgent", null, capabilities);
    verifyAgent("agent1", AgentState.UNKNOWN, capabilities);
    verifyAgent("agent2", AgentState.UPLOADING, capabilities);

    service.removeAgent("agent1");
    Assert.assertEquals(1, service.getKnownAgents().size());
    verifyAgent("notAnAgent", null, capabilities);
    verifyAgent("agent1", null, capabilities);
    verifyAgent("agent2", AgentState.UPLOADING, capabilities);

    service.removeAgent("notAnAgent");
    Assert.assertEquals(1, service.getKnownAgents().size());
    verifyAgent("notAnAgent", null, capabilities);
    verifyAgent("agent1", null, capabilities);
    verifyAgent("agent2", AgentState.UPLOADING, capabilities);
  }

  @Test
  public void agentCapabilities() {
    Assert.assertNull(service.getAgentCapabilities("agent"));
    Assert.assertNull(service.getAgentCapabilities("NotAgent"));
    service.setAgentCapabilities("agent", capabilities);
    Assert.assertEquals(service.getAgentCapabilities("agent"), capabilities);
    Assert.assertNull(service.getAgentCapabilities("NotAgent"));
  }

  @Test
  public void nonExistantRecording() {
    Recording recording = service.getRecordingState("doesNotExist");
    Assert.assertNull(recording);
  }

  @Test
  public void badRecordingData() {
    service.setRecordingState(null, RecordingState.CAPTURING);
    Assert.assertEquals(0, service.getKnownRecordings().size());
    service.setRecordingState("", AgentState.IDLE);
    Assert.assertEquals(0, service.getKnownRecordings().size());
    service.setRecordingState("something", null);
    Assert.assertEquals(0, service.getKnownRecordings().size());
  }


  @Test
  public void noRecordings() {
    Assert.assertEquals(0, service.getKnownRecordings().size());
  }

  private void verifyRecording(String id, String state) {
    Recording recording = service.getRecordingState(id);
    
    if (state != null) {
      Assert.assertEquals(id, recording.getID());
      Assert.assertEquals(state, recording.getState());
    } else
      Assert.assertNull(recording);
  }

  @Test
  public void oneRecording() {
    service.setRecordingState("Recording1", RecordingState.UPLOAD_FINISHED);
    Assert.assertEquals(1, service.getKnownRecordings().size());

    verifyRecording("notRecording1", null);
    verifyRecording("Recording1", RecordingState.UPLOAD_FINISHED);

    service.setRecordingState("Recording1", RecordingState.CAPTURING);
    Assert.assertEquals(1, service.getKnownRecordings().size());

    verifyRecording("notRecording1", null);
    verifyRecording("Recording1", RecordingState.CAPTURING);
  }

  @Test
  public void removeRecording() {
    service.setRecordingState("Recording1", RecordingState.CAPTURING);
    Assert.assertEquals(1, service.getKnownRecordings().size());
    service.setRecordingState("Recording2", RecordingState.UPLOADING);
    Assert.assertEquals(2, service.getKnownRecordings().size());

    verifyRecording("notAnRecording", null);
    verifyRecording("Recording1", RecordingState.CAPTURING);
    verifyRecording("Recording2", RecordingState.UPLOADING);

    service.removeRecording("Recording1");
    Assert.assertEquals(1, service.getKnownRecordings().size());
    verifyRecording("notAnRecording", null);
    verifyRecording("Recording1", null);
    verifyRecording("Recording2", RecordingState.UPLOADING);
  }
}
