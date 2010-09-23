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

import org.opencastproject.capture.admin.api.AgentState;
import org.opencastproject.capture.admin.api.RecordingState;
import org.opencastproject.capture.api.CaptureParameters;
import org.opencastproject.util.XProperties;

import org.apache.commons.io.FileUtils;
import org.gstreamer.Gst;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * Test the implementation of the Capture Agent, which uses gstreamer to 
 * generate pipelines that capture the media. 
 */
public class CaptureAgentImplTest {
  
  /** The single instance of CaptureAgentImpl needed */
  private static CaptureAgentImpl agent = null;

  /** The configuration manager for these tests */
  private static ConfigurationManager config = null;

  private static SchedulerImpl sched = null;
  
  /** Properties specifically designed for unit testing */
  private static Properties properties = null;
  
  /** Define a recording ID for the test */
  private final static String recordingID = "UnitTest1";
  
  /** True to run the tests */
  private static boolean gstreamerInstalled = true;
  
  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(CaptureAgentImplTest.class);

  @BeforeClass
  public static void testGst() {
    try {
      Gst.init();
    } catch (Throwable t) {
      logger.warn("Skipping agent tests due to unsatisifed gstreamer installation");
      gstreamerInstalled = false;
    }
  }
  
  @Before
  public void setup() throws ConfigurationException, IOException, URISyntaxException {
    if (!gstreamerInstalled)
      return;
    
    //Create the configuration manager
    config = new ConfigurationManager();

    Properties p = loadProperties("config/capture.properties");
    p.put("org.opencastproject.storage.dir", new File(System.getProperty("java.io.tmpdir"), "capture-agent-test").getAbsolutePath());
    p.put("org.opencastproject.server.url", "http://localhost:8080");
    p.put(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_POLLING_INTERVAL, -1);
    p.put("M2_REPO", getClass().getClassLoader().getResource("m2_repo").getFile());
    config.updated(p);

    sched = new SchedulerImpl();
    sched.setConfigService(config);
    sched.updated(loadProperties("config/scheduler.properties"));
    
    // creates agent, initially idle
    agent = new CaptureAgentImpl();
    agent.setConfigService(config);
    sched.setCaptureAgent(agent);

    Assert.assertNull(agent.getAgentState());
    agent.activate(null);
    Assert.assertEquals(AgentState.IDLE, agent.getAgentState());
    
    // setup testing properties
    properties = new Properties();
    properties.setProperty(CaptureParameters.RECORDING_ID, recordingID);
    properties.setProperty(CaptureParameters.RECORDING_END, "something");
  }

  @After
  public void tearDown() {
    if (!gstreamerInstalled)
      return;

    agent.deactivate();
    agent = null;
    config.deactivate();
    config = null;
    properties = null;
    sched.shutdown();
    sched = null;
    FileUtils.deleteQuietly(new File(System.getProperty("java.io.tmpdir"), "capture-agent-test"));
  }

  private XProperties loadProperties(String location) throws IOException {
    XProperties props = new XProperties();
    InputStream s = getClass().getClassLoader().getResourceAsStream(location);
    if (s == null) {
      throw new RuntimeException("Unable to load configuration file from " + location);
    }
    props.load(s);
    return props;
  }

  @Test
  public void testCaptureAgentImpl() throws InterruptedException {
    if (!gstreamerInstalled)
      return;

    // start the capture, assert the recording id is correct
    String id = agent.startCapture(properties);
    Assert.assertEquals(recordingID, id);

    File outputdir = new File(config.getItem("capture.filesystem.cache.capture.url"), id);
    
    // even with a mock capture, the state should remain capturing until stopCapture has been called
    Assert.assertEquals(AgentState.CAPTURING, agent.getAgentState());
    
    // affirm the captured media exists in the appropriate location
    String[] devnames = config.getItem(CaptureParameters.CAPTURE_DEVICE_NAMES).split(",");
    Assert.assertTrue(devnames.length >= 1);
    Assert.assertFalse(devnames[0].equals(""));

    for (String devname : devnames) {
      File outputfile = new File(outputdir, config.getItem(CaptureParameters.CAPTURE_DEVICE_PREFIX + devname + CaptureParameters.CAPTURE_DEVICE_DEST));
      Assert.assertTrue(outputfile.exists());
    }

    // the appropriate files exists, so the capture can be stopped. The agent's state should return to idle.
    Assert.assertTrue(agent.stopCapture(recordingID, true));
    Assert.assertEquals(AgentState.IDLE, agent.getAgentState());
    Assert.assertEquals(RecordingState.CAPTURE_FINISHED, agent.loadRecording(new File(agent.getKnownRecordings().get(id).getBaseDir(), id + ".recording")).getState());

    Thread.sleep(1000);

    // test creation of the manifest file
    File manifestFile = new File(outputdir.getAbsolutePath(), CaptureParameters.MANIFEST_NAME);
    Assert.assertTrue(manifestFile.exists());

    Thread.sleep(1000);

    // test zipping media
    File zippedMedia = new File(outputdir.getAbsolutePath(), CaptureParameters.ZIP_NAME);
    Assert.assertTrue(zippedMedia.exists());
  }

  private void buildRecordingState(String id, String state) throws IOException {
    if (!gstreamerInstalled)
      return;

    agent.setRecordingState(id, state);
    Assert.assertEquals(state, agent.getRecordingState(id).getState());
    RecordingImpl rec = (RecordingImpl) agent.getKnownRecordings().get(id);
    String newID = rec.getID().split("-")[0] + "-" + rec.getState();
    rec.id = newID;
    Assert.assertEquals(newID, agent.getRecordingState(id).getID());
    Assert.assertEquals(state, agent.getRecordingState(id).getState());
    agent.serializeRecording(id);
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    RecordingImpl r = agent.loadRecording(new File(rec.getBaseDir(), newID + ".recording"));
    Assert.assertEquals(state, r.getState());
    Assert.assertEquals(newID, r.getID());
  }
  
  @Test
  public void testRecordingLoadJob() throws ConfigurationException, IOException, InterruptedException {
    if (!gstreamerInstalled)
      return;

    //Put a recording into the agent, then kill it mid-capture
    Assert.assertEquals(0, agent.getKnownRecordings().size());
    String id = agent.startCapture(properties);
    Assert.assertEquals(1, agent.getKnownRecordings().size());
    Thread.sleep(1000);
    agent.deactivate();
    agent = null;

    //Bring the agent back up and check to make sure it reloads the recording
    agent = new CaptureAgentImpl();
    agent.setConfigService(config);
    sched.setCaptureAgent(agent);
    sched.stopScheduler();
    agent.activate(null);
    Assert.assertEquals(0, agent.getKnownRecordings().size());
    agent.createRecordingLoadTask(loadProperties("config/scheduler.properties"), 1);
    System.out.println("Waiting 5 seconds to make sure the scheduler has time to load...");
    Thread.sleep(5000);
    Assert.assertEquals(1, agent.getKnownRecordings().size());
    Assert.assertEquals(id, agent.getKnownRecordings().get(id).getID());
  }

  @Test
  public void testRecordingLoadMethod() throws IOException {
    if (!gstreamerInstalled)
      return;

    agent.deactivate();
    agent = null;

    //Create the agent and verify some of the error handling logic
    agent = new CaptureAgentImpl();

    Assert.assertEquals(0, agent.getKnownRecordings().size());
    agent.loadRecordingsFromDisk();
    Assert.assertEquals(0, agent.getKnownRecordings().size());

    agent.setConfigService(config);

    Assert.assertEquals(0, agent.getKnownRecordings().size());
    agent.loadRecordingsFromDisk();
    Assert.assertEquals(0, agent.getKnownRecordings().size());

    sched.setCaptureAgent(agent);
    agent.activate(null);

    //More error handling tests
    String backup = config.getItem(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL);
    config.setItem(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL, null);
    agent.loadRecordingsFromDisk();
    Assert.assertEquals(0, agent.getKnownRecordings().size());
    config.setItem(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL, getClass().getClassLoader().getResource("config/capture.properties").getFile());
    agent.loadRecordingsFromDisk();
    Assert.assertEquals(0, agent.getKnownRecordings().size());
    config.setItem(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL, backup);
    agent.loadRecordingsFromDisk();
    Assert.assertEquals(0, agent.getKnownRecordings().size());

    String id = agent.startCapture(properties);
    Assert.assertEquals(recordingID, id);

    // even with a mock capture, the state should remain capturing until stopCapture has been called
    Assert.assertEquals(AgentState.CAPTURING, agent.getAgentState());
    buildRecordingState(id, RecordingState.CAPTURE_ERROR);
    buildRecordingState(id, RecordingState.CAPTURE_FINISHED);
    buildRecordingState(id, RecordingState.CAPTURING);
    buildRecordingState(id, RecordingState.MANIFEST);
    buildRecordingState(id, RecordingState.MANIFEST_FINISHED);
    buildRecordingState(id, RecordingState.MANIFEST_ERROR);
    buildRecordingState(id, RecordingState.COMPRESSING);
    buildRecordingState(id, RecordingState.COMPRESSING_ERROR);
    buildRecordingState(id, RecordingState.UPLOAD_ERROR);
    buildRecordingState(id, RecordingState.UPLOADING);
    buildRecordingState(id, RecordingState.UPLOAD_FINISHED);
    buildRecordingState(id, RecordingState.UNKNOWN);

    agent.deactivate();
    agent = null;

    agent = new CaptureAgentImpl();
    agent.setConfigService(config);
    sched.setCaptureAgent(agent);
    agent.activate(null);

    Assert.assertEquals(0, agent.getKnownRecordings().size());
    sched.stopScheduler();
    agent.loadRecordingsFromDisk();

    Assert.assertEquals(10, agent.getKnownRecordings().size());
    Assert.assertNotNull(agent.getKnownRecordings().get(id + "-" + RecordingState.CAPTURE_FINISHED));
    Assert.assertNotNull(agent.getKnownRecordings().get(id + "-" + RecordingState.CAPTURING));
    Assert.assertNotNull(agent.getKnownRecordings().get(id + "-" + RecordingState.MANIFEST));
    Assert.assertNotNull(agent.getKnownRecordings().get(id + "-" + RecordingState.MANIFEST_FINISHED));
    Assert.assertNotNull(agent.getKnownRecordings().get(id + "-" + RecordingState.MANIFEST_ERROR));
    Assert.assertNotNull(agent.getKnownRecordings().get(id + "-" + RecordingState.COMPRESSING));
    Assert.assertNotNull(agent.getKnownRecordings().get(id + "-" + RecordingState.COMPRESSING_ERROR));
    Assert.assertNotNull(agent.getKnownRecordings().get(id + "-" + RecordingState.UPLOADING));
    Assert.assertNotNull(agent.getKnownRecordings().get(id + "-" + RecordingState.UPLOAD_ERROR));
    Assert.assertNotNull(agent.getKnownRecordings().get(id + "-" + RecordingState.UPLOADING));

    agent.loadRecordingsFromDisk();
    Assert.assertEquals(10, agent.getKnownRecordings().size());
  }

}
