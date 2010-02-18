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
  public void badAgentData() {
    service.setAgentCapabilities(null, capabilities);
    Assert.assertEquals(0, service.getKnownAgents().size());
    service.setAgentCapabilities("", capabilities);
    Assert.assertEquals(0, service.getKnownAgents().size());
    service.setAgentState("something", null);
    Assert.assertEquals(0, service.getKnownAgents().size());
  }

  private void verifyAgent(String name, String state) {
    Agent agent = service.getAgentState(name);
    
    if (agent != null) {
      Assert.assertEquals(name, agent.getName());
      Assert.assertEquals(state, agent.getState());
    } else if (state != null)
      Assert.fail();
  }

  @Test
  public void oneAgent() {
    service.setAgentCapabilities("agent1", capabilities);
    Assert.assertEquals(1, service.getKnownAgents().size());

    verifyAgent("notAgent1", null);
    verifyAgent("agent1", AgentState.IDLE);

    service.setAgentState("agent1", AgentState.CAPTURING);
    Assert.assertEquals(1, service.getKnownAgents().size());

    verifyAgent("notAgent1",null);
    verifyAgent("agent1", AgentState.CAPTURING);
  }

  @Test
  public void removeAgent() {
    service.setAgentCapabilities("agent1", capabilities);
    Assert.assertEquals(1, service.getKnownAgents().size());
    service.setAgentCapabilities("agent2", capabilities);
    service.setAgentState("agent2", AgentState.UPLOADING);

    verifyAgent("notAnAgent", null);
    verifyAgent("agent1", AgentState.IDLE);
    verifyAgent("agent2", AgentState.UPLOADING);

    service.removeAgent("agent1");
    Assert.assertEquals(1, service.getKnownAgents().size());
    verifyAgent("notAnAgent", null);
    verifyAgent("agent1", null);
    verifyAgent("agent2", AgentState.UPLOADING);
  }

  @Test
  public void nonExistantRecording() {
    Recording recording = service.getRecordingState("doesNotExist");
    Assert.assertEquals("doesNotExist", recording.getID());
    Assert.assertEquals(RecordingState.UNKNOWN, recording.getState());
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
    Assert.assertEquals(id, recording.getID());
    Assert.assertEquals(state, recording.getState());
  }

  @Test
  public void oneRecording() {
    service.setRecordingState("Recording1", RecordingState.UPLOAD_FINISHED);
    Assert.assertEquals(1, service.getKnownRecordings().size());

    verifyRecording("notRecording1", RecordingState.UNKNOWN);
    verifyRecording("Recording1", RecordingState.UPLOAD_FINISHED);

    service.setRecordingState("Recording1", RecordingState.CAPTURING);
    Assert.assertEquals(1, service.getKnownRecordings().size());

    verifyRecording("notRecording1", RecordingState.UNKNOWN);
    verifyRecording("Recording1", RecordingState.CAPTURING);
  }

  @Test
  public void removeRecording() {
    service.setRecordingState("Recording1", RecordingState.CAPTURING);
    Assert.assertEquals(1, service.getKnownRecordings().size());
    service.setRecordingState("Recording2", RecordingState.UPLOADING);
    Assert.assertEquals(2, service.getKnownRecordings().size());

    verifyRecording("notAnRecording", RecordingState.UNKNOWN);
    verifyRecording("Recording1", RecordingState.CAPTURING);
    verifyRecording("Recording2", RecordingState.UPLOADING);

    service.removeRecording("Recording1");
    Assert.assertEquals(1, service.getKnownRecordings().size());
    verifyRecording("notAnRecording", RecordingState.UNKNOWN);
    verifyRecording("Recording1", RecordingState.UNKNOWN);
    verifyRecording("Recording2", RecordingState.UPLOADING);
  }
}
