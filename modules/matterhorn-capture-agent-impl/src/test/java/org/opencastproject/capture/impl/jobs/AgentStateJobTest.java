package org.opencastproject.capture.impl.jobs;

import org.opencastproject.capture.api.AgentRecording;
import org.opencastproject.capture.api.CaptureParameters;
import org.opencastproject.capture.api.StateService;
import org.opencastproject.capture.impl.ConfigurationManager;
import org.opencastproject.capture.impl.RecordingImpl;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.util.ConfigurationException;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpPost;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Ignore()
public class AgentStateJobTest {
  AgentStateJob job = null;

  @Before
  public void setup() throws IllegalArgumentException, ConfigurationException, IOException, MediaPackageException {
    ConfigurationManager config = new ConfigurationManager();
    config.setItem(CaptureParameters.AGENT_STATE_REMOTE_ENDPOINT_URL, "http://localhost:8080/");
    config.setItem(CaptureParameters.RECORDING_STATE_REMOTE_ENDPOINT_URL, "http://localhost:8080/");
    config.setItem(CaptureParameters.AGENT_NAME, "testAgent");

    StateService state = EasyMock.createMock(StateService.class);

    EasyMock.expect(state.getAgentName()).andReturn("testAgent");
    EasyMock.expectLastCall().anyTimes();

    EasyMock.expect(state.getAgentState()).andReturn("idle");
    EasyMock.expectLastCall().anyTimes();

    Map<String, AgentRecording> recordingMap = new HashMap<String, AgentRecording>();
    recordingMap.put("test", new RecordingImpl(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew(), new Properties()));
    EasyMock.expect(state.getKnownRecordings()).andReturn(recordingMap);
    EasyMock.expectLastCall().anyTimes();

    StatusLine statusline = EasyMock.createMock(StatusLine.class);
    EasyMock.expect(statusline.getStatusCode()).andReturn(200);
    EasyMock.expectLastCall().anyTimes();

    HttpResponse response = EasyMock.createMock(HttpResponse.class);
    EasyMock.expect(response.getStatusLine()).andReturn(statusline);
    EasyMock.expectLastCall().anyTimes();

    TrustedHttpClient client = EasyMock.createMock(TrustedHttpClient.class);
    EasyMock.expect(client.execute(EasyMock.isA(HttpPost.class))).andReturn(response);
    EasyMock.expectLastCall().anyTimes();

    EasyMock.replay(state);
    EasyMock.replay(client);
    
    job = new AgentStateJob();
    job.setConfigManager(config);
    job.setStateService(state);
    job.setTrustedClient(client);
  }

  @Test
  public void test() {
    job.sendAgentState();
    job.sendRecordingState();
  }
}