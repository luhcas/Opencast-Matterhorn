package org.opencastproject.capture.admin.impl;

import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.capture.admin.api.AgentState;
import org.opencastproject.capture.admin.api.Recording;
import org.opencastproject.capture.admin.api.RecordingState;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowSetImpl;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.eclipse.persistence.jpa.PersistenceProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class CaptureAgentStateServiceImplTest {
  private CaptureAgentStateServiceImpl service = null;
  private Properties capabilities;
  private ComboPooledDataSource pooledDataSource = null;
  private long timestamp = -1L;

  @Before
  public void setup() throws Exception {
    timestamp = System.currentTimeMillis();
    setupService();

    capabilities = new Properties();
    capabilities.setProperty("CAMERA", "/dev/video0");
    capabilities.setProperty("SCREEN", "/dev/video1");
    capabilities.setProperty("AUDIO", "hw:0");
  }

  private void setupService() throws Exception {
    pooledDataSource = new ComboPooledDataSource();
    pooledDataSource.setDriverClass("org.h2.Driver");
    pooledDataSource.setJdbcUrl("jdbc:h2:./target/db" + timestamp + ";LOCK_MODE=1;MVCC=TRUE");
    pooledDataSource.setUser("sa");
    pooledDataSource.setPassword("sa");

    // Collect the persistence properties
    Map<String, Object> props = new HashMap<String, Object>();
    props.put("javax.persistence.nonJtaDataSource", pooledDataSource);
    props.put("eclipselink.ddl-generation", "create-tables");
    props.put("eclipselink.ddl-generation.output-mode", "database");

    service = new CaptureAgentStateServiceImpl();
    service.setPersistenceProvider(new PersistenceProvider());
    service.setPersistenceProperties(props);
    service.activate(null);

    WorkflowService workflowService = EasyMock.createNiceMock(WorkflowService.class);
    EasyMock.expect(workflowService.getWorkflowInstances((WorkflowQuery) EasyMock.anyObject()))
            .andReturn(new WorkflowSetImpl()).anyTimes();
    EasyMock.replay(workflowService);
    service.setWorkflowService(workflowService);

    Assert.assertNotNull(service);
  }

  @After
  public void teardown() {
    service = null;
    pooledDataSource.close();
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
    verifyAgent("agent1", AgentState.IDLE, new Properties());

    service.setAgentState("agent1", AgentState.CAPTURING);
    Assert.assertEquals(1, service.getKnownAgents().size());

    verifyAgent("notAgent1", null, null);
    verifyAgent("agent1", AgentState.CAPTURING, new Properties());
  }

  @Test
  public void oneAgentCapabilities() {
    service.setAgentCapabilities("agent1", capabilities);
    Assert.assertEquals(1, service.getKnownAgents().size());

    verifyAgent("notAgent1", null, null);
    verifyAgent("agent1", AgentState.UNKNOWN, capabilities);

    service.setAgentState("agent1", AgentState.IDLE);
    Assert.assertEquals(1, service.getKnownAgents().size());

    verifyAgent("notAgent1", null, null);
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
  public void stickyAgents() throws Exception {
    Assert.assertEquals(0, service.getKnownAgents().size());

    Properties cap1 = new Properties();
    cap1.put("key", "value");
    Properties cap2 = new Properties();
    cap2.put("foo", "bar");
    Properties cap3 = new Properties();
    cap3.put("bam", "bam");

    // Setup the two agents and persist them
    service.setAgentState("sticky1", AgentState.IDLE);
    service.setAgentCapabilities("sticky1", cap1);
    service.setAgentState("sticky2", AgentState.CAPTURING);
    service.setAgentCapabilities("sticky2", cap2);
    service.setAgentState("sticky3", AgentState.UPLOADING);
    service.setAgentCapabilities("sticky3", cap3);

    // Make sure they're set right
    Assert.assertEquals(cap1, service.getAgentCapabilities("sticky1"));
    Assert.assertEquals(AgentState.IDLE, service.getAgentState("sticky1").getState());
    Assert.assertEquals(cap2, service.getAgentCapabilities("sticky2"));
    Assert.assertEquals(AgentState.CAPTURING, service.getAgentState("sticky2").getState());
    Assert.assertEquals(cap3, service.getAgentCapabilities("sticky3"));
    Assert.assertEquals(AgentState.UPLOADING, service.getAgentState("sticky3").getState());
    Assert.assertNull(service.getAgentCapabilities("sticky4"));
    Assert.assertNull(service.getAgentState("sticky4"));

    // Shut down the service completely
    service.deactivate();
    service = null;

    // Restart the service with the same configuration as before
    setupService();

    Assert.assertEquals(3, service.getKnownAgents().size());

    // The agents should still be there
    Assert.assertEquals(cap1, service.getAgentCapabilities("sticky1"));
    Assert.assertEquals(AgentState.IDLE, service.getAgentState("sticky1").getState());
    Assert.assertEquals(cap2, service.getAgentCapabilities("sticky2"));
    Assert.assertEquals(AgentState.CAPTURING, service.getAgentState("sticky2").getState());
    Assert.assertEquals(cap3, service.getAgentCapabilities("sticky3"));
    Assert.assertEquals(AgentState.UPLOADING, service.getAgentState("sticky3").getState());
    Assert.assertNull(service.getAgentCapabilities("sticky4"));
    Assert.assertNull(service.getAgentState("sticky4"));
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

    Assert.assertTrue(service.removeRecording("Recording1"));
    Assert.assertFalse(service.removeRecording("asdfasdf"));
    Assert.assertEquals(1, service.getKnownRecordings().size());
    verifyRecording("notAnRecording", null);
    verifyRecording("Recording1", null);
    verifyRecording("Recording2", RecordingState.UPLOADING);
  }
}
