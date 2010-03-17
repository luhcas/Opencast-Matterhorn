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
package org.opencastproject.capture.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opencastproject.capture.admin.api.AgentState;
import org.opencastproject.capture.admin.api.Recording;
import org.opencastproject.capture.admin.api.RecordingState;
import org.osgi.service.cm.ConfigurationException;

public class StateServiceImplTest {
  private StateServiceImpl service = null;
  private ConfigurationManager cfg = null;

  @Before
  public void setup() {
    service = new StateServiceImpl();
    Assert.assertNotNull(service);
    service.unsetConfigService();
    cfg = new ConfigurationManager();
    Assert.assertNotNull(cfg);
    cfg.setItem(CaptureParameters.AGENT_STATE_REMOTE_POLLING_INTERVAL, "1");
    cfg.setItem(CaptureParameters.AGENT_STATE_REMOTE_ENDPOINT_URL, "http://localhost");
    service.setConfigService(cfg);
  }

  @After
  public void teardown() {
    service.deactivate();
    service = null;
  }

  //Note:  This test is meant to test that the code handles weird cases in the polling, *not* the functionality itself 
  @Test
  public void testValidPolling() throws ConfigurationException {
    InputStream s = getClass().getClassLoader().getResourceAsStream("config/scheduler.properties");
    if (s == null) {
      throw new RuntimeException("Unable to load configuration file for scheduler!");
    }

    Properties props = new Properties();
    try {
      props.load(s);
    } catch (IOException e) {
      throw new RuntimeException("Unable to read configuration data for scheduler!");
    }

    service.updated(props);

  }

  //Note:  This test is meant to test that the code handles weird cases in the polling, *not* the functionality itself 
  @Test
  public void testInvalidPolling() throws ConfigurationException {
    Properties props = new Properties();
    service.updated(props);

    try {
      service.updated(null);
    } catch (ConfigurationException e) {
      //Good, this is expected
      return;
    }
    Assert.fail();
  }

  //Note:  This test combines its subtests to verify that the code to handle the update functionality is working.
  @Test
  public void testPollingChanges() throws ConfigurationException {
    testValidPolling();
    testInvalidPolling();
  }

  @Test
  public void testUnpreparedImpl() {
    Assert.assertNull(service.getAgentState());
    Assert.assertNull(service.getAgent());
    service.setAgentState("TEST");
    Assert.assertNull(service.getAgentState());
    service.setRecordingState(null, "won't work");
    service.setRecordingState("somethign", null);
    service.setRecordingState("works", "working");
    Assert.assertNull(service.getKnownRecordings());
    Assert.assertNull(service.getRecordingState("works"));

    service.activate(null);
    Assert.assertEquals(AgentState.UNKNOWN, service.getAgentState());
    service.setAgentState(AgentState.CAPTURING);
    Assert.assertEquals(AgentState.CAPTURING, service.getAgentState());
  }

  @Test
  public void testRecordings() {
    Assert.assertNull(service.getKnownRecordings());
    service.activate(null);
    Assert.assertNotNull(service.getKnownRecordings());
    Assert.assertEquals(0, service.getKnownRecordings().size());

    Assert.assertNull(service.getRecordingState("abc"));
    Assert.assertNull(service.getRecordingState("123"));
    Assert.assertNull(service.getRecordingState("doesnotexist"));
    service.setRecordingState("abc", RecordingState.CAPTURING);
    service.setRecordingState("123", RecordingState.UPLOADING);
    Assert.assertEquals(2, service.getKnownRecordings().size());
    verifyRecording(service.getRecordingState("abc"), "abc", RecordingState.CAPTURING);
    verifyRecording(service.getRecordingState("123"), "123", RecordingState.UPLOADING);
    Assert.assertNull(service.getRecordingState("doesnotexist"));
  }

  @Test
  public void testInvalidRecording() {
    Assert.assertNull(service.getKnownRecordings());
    service.activate(null);
    Assert.assertNotNull(service.getKnownRecordings());
    Assert.assertEquals(0, service.getKnownRecordings().size());

    service.setRecordingState(null, "won't work");
    service.setRecordingState("something", null);
    Assert.assertEquals(0, service.getKnownRecordings().size());
  }

  private void verifyRecording(Recording r, String id, String state) {
    Assert.assertEquals(id, r.id);
    Assert.assertEquals(state, r.state);
  }
}
