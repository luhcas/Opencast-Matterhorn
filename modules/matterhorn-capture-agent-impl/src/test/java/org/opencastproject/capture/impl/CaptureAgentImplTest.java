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
 * Test the implementation of the Capture Agent, which uses gstreamer to generate pipelines that capture the media.
 */
public class CaptureAgentImplTest {

  /** The single instance of CaptureAgentImpl needed */
  private CaptureAgentImpl agent = null;

  /** The configuration manager for these tests */
  private ConfigurationManager config = null;

  /** Properties specifically designed for unit testing */
  private Properties properties = null;

  /** Define a recording ID for the test */
  private static final String recordingID = "UnitTest1";

  /** True to run the tests */
  private static boolean gstreamerInstalled = true;

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(CaptureAgentImplTest.class);

  /** Waits for a particular state to occur or times out waiting. **/
  private WaitForState waiter;

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
  public void setUp() throws ConfigurationException, IOException, URISyntaxException {
    if (!gstreamerInstalled)
      return;
    // Create the configuration manager
    config = new ConfigurationManager();

    Properties p = loadProperties("config/capture.properties");
    p.put("org.opencastproject.storage.dir",
            new File("./target", "capture-agent-test").getAbsolutePath());
    p.put("org.opencastproject.server.url", "http://localhost:8080");
    p.put(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_POLLING_INTERVAL, -1);
    p.put("M2_REPO", getClass().getClassLoader().getResource("m2_repo").getFile());
    config.updated(p);
    // creates agent, initially idle
    agent = new CaptureAgentImpl();
    agent.setConfigService(config);

    Assert.assertNull(agent.getAgentState());
    agent.activate(null);
    Assert.assertEquals(AgentState.IDLE, agent.getAgentState());
    agent.updated(p);
    // setup testing properties
    properties = new Properties();
    properties.setProperty(CaptureParameters.RECORDING_ID, recordingID);
    properties.setProperty(CaptureParameters.RECORDING_END, "something");
    waiter = new WaitForState();
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
    Assert.assertFalse("".equals(devnames[0]));

    for (String devname : devnames) {
      File outputfile = new File(outputdir, config.getItem(CaptureParameters.CAPTURE_DEVICE_PREFIX + devname
              + CaptureParameters.CAPTURE_DEVICE_DEST));
      Assert.assertTrue(outputfile.exists());
    }

    // the appropriate files exists, so the capture can be stopped. The agent's state should return to idle.
    Assert.assertTrue(agent.stopCapture(recordingID, true));
    Assert.assertEquals(AgentState.IDLE, agent.getAgentState());
    Assert.assertEquals(RecordingState.CAPTURE_FINISHED,
            agent.loadRecording(new File(agent.getKnownRecordings().get(id).getBaseDir(), id + ".recording"))
                    .getState());

    Thread.sleep(2000);

    // test creation of the manifest file
    File manifestFile = new File(outputdir.getAbsolutePath(), CaptureParameters.MANIFEST_NAME);
    Assert.assertTrue(manifestFile.exists());

    Thread.sleep(2000);

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
      Thread.sleep(2000);
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

    // Put a recording into the agent, then kill it mid-capture
    Assert.assertEquals(0, agent.getKnownRecordings().size());
    String id = agent.startCapture(properties);
    Assert.assertEquals(1, agent.getKnownRecordings().size());
    Thread.sleep(20000);
    agent.deactivate();
    agent = null;
    // Bring the agent back up and check to make sure it reloads the recording
    agent = new CaptureAgentImpl();
    agent.setConfigService(config);
    agent.activate(null);
    Assert.assertEquals(0, agent.getKnownRecordings().size());
    agent.updated(loadProperties("config/scheduler.properties"));
    agent.getSchedulerImpl().setCaptureAgent(agent);
    agent.getSchedulerImpl().stopScheduler();
    agent.createRecordingLoadTask(1);
    System.out.println("Waiting 5 seconds to make sure the scheduler has time to load...");
    waiter.sleepWait(new CheckState() {
      @Override
      public boolean check() {
        if (agent != null && agent.getKnownRecordings() != null) {
          return agent.getKnownRecordings().size() == 1;
        } else {
          return false;
        }
      }
    });
    Assert.assertEquals(1, agent.getKnownRecordings().size());
    Assert.assertEquals(id, agent.getKnownRecordings().get(id).getID());
  }

  @Test
  public void testRecordingLoadMethod() throws IOException, ConfigurationException {
    if (!gstreamerInstalled)
      return;
    agent.deactivate();
    agent = null;

    // Create the agent and verify some of the error handling logic
    agent = new CaptureAgentImpl();

    Assert.assertEquals(0, agent.getKnownRecordings().size());
    agent.loadRecordingsFromDisk();
    Assert.assertEquals(0, agent.getKnownRecordings().size());

    agent.setConfigService(config);

    Assert.assertEquals(0, agent.getKnownRecordings().size());
    agent.loadRecordingsFromDisk();
    Assert.assertEquals(0, agent.getKnownRecordings().size());
    
    agent.activate(null);

    // More error handling tests
    String backup = config.getItem(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL);
    config.setItem(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL, null);
    agent.loadRecordingsFromDisk();
    Assert.assertEquals(0, agent.getKnownRecordings().size());
    config.setItem(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL,
            getClass().getClassLoader().getResource("config/capture.properties").getFile());
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
    agent.activate(null);
    agent.updated(loadProperties("config/scheduler.properties"));
    Assert.assertEquals(0, agent.getKnownRecordings().size());
    agent.getSchedulerImpl().stopScheduler();
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

  @Test
  public void captureAgentImplWillWaitForConfigurationManagerUpdate() throws IOException, ConfigurationException,
          InterruptedException {
    if (!gstreamerInstalled)
      return;
    // Create the configuration manager
    config = new ConfigurationManager();
    Properties p = setupConfigurationManagerProperties();
    agent = new CaptureAgentImpl();
    agent.setConfigService(config);
    waiter = new WaitForState();
    waiter.sleepWait(new CheckState() {
      @Override
      public boolean check() {
        if (agent != null) {
          return !agent.isRefreshed() && !agent.isUpdated();
        } else {
          return false;
        }
      }
    });    
    Assert.assertFalse("The configuration manager is just created it shouldn't be updated yet.", agent.isRefreshed());
    Assert.assertFalse("The agent is just created it shouldn't be updated either", agent.isUpdated());
    agent.updated(properties);
    waiter = new WaitForState();
    waiter.sleepWait(new CheckState() {
      @Override
      public boolean check() {
        if (agent != null) {
          return !agent.isRefreshed() && agent.isUpdated();
        } else {
          return false;
        }
      }
    });    
    Assert.assertFalse("The config manager hasn't been updated so should not be refreshed.", agent.isRefreshed());
    Assert.assertTrue("The agent has been updated, so updated should be true.", agent.isUpdated());
    config.updated(p);
    waiter = new WaitForState();
    waiter.sleepWait(new CheckState() {
      @Override
      public boolean check() {
        if (agent != null) {
          return agent.isUpdated() && agent.isRefreshed() && (agent.getSchedulerImpl() != null);
        } else {
          return false;
        }
      }
    });
    Assert.assertTrue("The config manager is now updated so refreshed should be true.", agent.isRefreshed());
    Assert.assertTrue("The agent should still be updated.", agent.isUpdated());
    Assert.assertNotNull("If the properties are set, a SchedulerImpl should be created.", agent.getSchedulerImpl());
  }

  @Test
  public void configurationManagerRefreshWillWaitForCaptureAgentUpdate() throws IOException, ConfigurationException,
          InterruptedException {
    if (!gstreamerInstalled)
      return;
    // Create the configuration manager
    config = new ConfigurationManager();
    Properties p = setupConfigurationManagerProperties();
    agent = new CaptureAgentImpl();
    agent.setConfigService(config);
    waiter = new WaitForState();
    waiter.sleepWait(new CheckState() {
      @Override
      public boolean check() {
        if (agent != null) {
          return !agent.isRefreshed() && !agent.isUpdated();
        } else {
          return false;
        }
      }
    });    
    Assert.assertFalse("The configuration manager is just created it shouldn't be updated yet.", agent.isRefreshed());
    Assert.assertFalse("The agent is just created it shouldn't be updated either", agent.isUpdated());
    config.updated(p);
    waiter = new WaitForState();
    waiter.sleepWait(new CheckState() {
      @Override
      public boolean check() {
        if (agent != null) {
          return agent.isRefreshed() && !agent.isUpdated();
        } else {
          return false;
        }
      }
    });
    Assert.assertTrue("The config manager is now updated so refreshed should be true.", agent.isRefreshed());
    Assert.assertFalse("The agent should still not be updated.", agent.isUpdated());
    agent.updated(properties);
    waiter = new WaitForState();
    waiter.sleepWait(new CheckState() {
      @Override
      public boolean check() {
        if (agent != null) {
          return agent.isRefreshed() && agent.isUpdated() && agent.getSchedulerImpl() != null;
        } else {
          return false;
        }
      }
    });
    Assert.assertTrue("The config manager is now updated so refreshed should be true.", agent.isRefreshed());
    Assert.assertTrue("The agent should still be updated.", agent.isUpdated());
    Assert.assertNotNull("If the properties are set, a SchedulerImpl should be created.", agent.getSchedulerImpl());
  }
  
  @Test
  public void configurationManagerComingUpCompletelyBeforeCaptureAgentImplOkay() throws IOException,
          ConfigurationException, InterruptedException {
    if (!gstreamerInstalled)
      return;
    // Create the configuration manager
    config = new ConfigurationManager();
    Properties p = setupConfigurationManagerProperties();
    config.updated(p);
    waiter = new WaitForState();
    waiter.sleepWait(new CheckState() {
      @Override
      public boolean check() {
        if (config != null) {
          return config.isInitialized();
        } else {
          return false;
        }
      }
    });
    Assert.assertTrue(config.isInitialized());
    
    agent = new CaptureAgentImpl();
    agent.setConfigService(config);
    waiter = new WaitForState();
    waiter.sleepWait(new CheckState() {
      @Override
      public boolean check() {
        if (agent != null) {
          return agent.isRefreshed();
        } else {
          return false;
        }
      }
    });
    Assert.assertTrue("The configuration manager is fully up, so it should refresh the agent.", agent.isRefreshed());
    Assert.assertFalse("The agent is just created it shouldn't be updated either", agent.isUpdated());
    agent.updated(properties);
    waiter = new WaitForState();
    waiter.sleepWait(new CheckState() {
      @Override
      public boolean check() {
        if (agent != null) {
          return agent.isRefreshed() && agent.isUpdated() && agent.getSchedulerImpl() != null;
        } else {
          return false;
        }
      }
    });
    Assert.assertTrue("The config manager is still updated so refreshed should be true.", agent.isRefreshed());
    Assert.assertTrue("The agent should be updated.", agent.isUpdated());
    Assert.assertNotNull("If the properties are set, a SchedulerImpl should be created.", agent.getSchedulerImpl());
  }
  
  @Test
  public void captureAgentImplComingUpFullyBeforeConfigurationManagerOkay() throws IOException,
          ConfigurationException, InterruptedException {
    if (!gstreamerInstalled)
      return;
    // Create the configuration manager
    config = new ConfigurationManager();
    agent = new CaptureAgentImpl();
    agent.updated(properties);
    waiter = new WaitForState();
    waiter.sleepWait(new CheckState() {
      @Override
      public boolean check() {
        if (agent != null) {
          return !agent.isRefreshed() && agent.isUpdated();
        } else {
          return false;
        }
      }
    });
    Assert.assertFalse("The config manager should still be waiting for an update.", agent.isRefreshed());
    Assert.assertTrue("The agent should be updated.", agent.isUpdated());
    Properties p = setupConfigurationManagerProperties();
    config.updated(p);
    waiter = new WaitForState();
    waiter.sleepWait(new CheckState() {
      @Override
      public boolean check() {
        if (config != null) {
          return config.isInitialized();
        } else {
          return false;
        }
      }
    });
    Assert.assertTrue(config.isInitialized());
    agent.setConfigService(config);
    waiter = new WaitForState();
    waiter.sleepWait(new CheckState() {
      @Override
      public boolean check() {
        if (agent != null) {
          return agent.isRefreshed() && agent.isUpdated();
        } else {
          return false;
        }
      }
    });
    Assert.assertTrue("The config manager should be updated.", agent.isRefreshed());
    Assert.assertTrue("The agent should be updated.", agent.isUpdated());
    Assert.assertNotNull("If the properties are set, a SchedulerImpl should be created.", agent.getSchedulerImpl());
  }
  
  private Properties setupConfigurationManagerProperties() throws IOException {
    Properties p = loadProperties("config/capture.properties");
    p.put("org.opencastproject.storage.dir",
            new File("./target/", "capture-agent-test").getAbsolutePath());
    p.put("org.opencastproject.server.url", "http://localhost:8080");
    p.put(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_POLLING_INTERVAL, -1);
    p.put("M2_REPO", getClass().getClassLoader().getResource("m2_repo").getFile());
    return p;
  }
  
}
