package org.opencastproject.capture.admin.impl;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.capture.admin.api.AgentState;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.capture.admin.api.Recording;
import org.opencastproject.capture.admin.api.RecordingState;
import org.opencastproject.capture.admin.impl.CaptureAgentStateServiceImpl;

public class CaptureAgentStateServiceImplTest {
  private CaptureAgentStateService service = null;

  @Before
  public void setup() {
    service = new CaptureAgentStateServiceImpl();
    Assert.assertNotNull(service);
  }

  @After
  public void teardown() {
    service = null;
  }

  @Test
  public void nonExistantAgent() {
    Agent agent = service.getAgentState("doesNotExist");
    Assert.assertEquals("doesNotExist", agent.getName());
    Assert.assertEquals(AgentState.UNKNOWN, agent.getState());
  }

  @Test
  public void noAgents() {
    Assert.assertEquals(0, service.getKnownAgents().size());
  }

  private void verifyAgent(String name, String state) {
    Agent agent = service.getAgentState(name);
    Assert.assertEquals(name, agent.getName());
    Assert.assertEquals(state, agent.getState());
  }

  @Test
  public void oneAgent() {
    service.setAgentState("agent1", AgentState.IDLE);
    Assert.assertEquals(1, service.getKnownAgents().size());

    verifyAgent("notAgent1", AgentState.UNKNOWN);
    verifyAgent("agent1", AgentState.IDLE);

    service.setAgentState("agent1", AgentState.CAPTURING);
    Assert.assertEquals(1, service.getKnownAgents().size());

    verifyAgent("notAgent1", AgentState.UNKNOWN);
    verifyAgent("agent1", AgentState.CAPTURING);
  }

  @Test
  public void removeAgent() {
    service.setAgentState("agent1", AgentState.IDLE);
    Assert.assertEquals(1, service.getKnownAgents().size());
    service.setAgentState("agent2", AgentState.UPLOADING);
    Assert.assertEquals(2, service.getKnownAgents().size());

    verifyAgent("notAnAgent", AgentState.UNKNOWN);
    verifyAgent("agent1", AgentState.IDLE);
    verifyAgent("agent2", AgentState.UPLOADING);

    service.removeAgent("agent1");
    Assert.assertEquals(1, service.getKnownAgents().size());
    verifyAgent("notAnAgent", AgentState.UNKNOWN);
    verifyAgent("agent1", AgentState.UNKNOWN);
    verifyAgent("agent2", AgentState.UPLOADING);
  }

  @Test
  public void nonExistantRecording() {
    Recording Recording = service.getRecordingState("doesNotExist");
    Assert.assertEquals("doesNotExist", Recording.getID());
    Assert.assertEquals(RecordingState.UNKNOWN, Recording.getState());
  }

  @Test
  public void noRecordings() {
    Assert.assertEquals(0, service.getKnownRecordings().size());
  }

  private void verifyRecording(String id, String state) {
    Recording Recording = service.getRecordingState(id);
    Assert.assertEquals(id, Recording.getID());
    Assert.assertEquals(state, Recording.getState());
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
