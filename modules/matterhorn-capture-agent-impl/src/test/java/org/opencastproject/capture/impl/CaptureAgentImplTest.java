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
import org.opencastproject.capture.api.CaptureParameters;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * Test the implementation of the Capture Agent, which uses gstreamer to 
 * generate pipelines that capture the media. 
 */
public class CaptureAgentImplTest {
  
  private static Logger logger = LoggerFactory.getLogger(CaptureAgentImplTest.class);
  
  /** The single instance of CaptureAgentImpl needed */
  private static CaptureAgentImpl agent = null;

  /** The configuration manager for these tests */
  private static ConfigurationManager config = null;

  private static SchedulerImpl sched = null;
  
  /** Properties specifically designed for unit testing */
  private static Properties properties = null;
  
  /** Define a recording ID for the test */
  private final static String recordingID = "UnitTest1";
  
  @AfterClass
  public static void afterclass() {
    FileUtils.deleteQuietly(new File(System.getProperty("java.io.tmpdir"), "capture-agent-test"));
  }

  @Before
  public void setup() throws ConfigurationException, IOException {
    //Create the configuration manager
    config = new ConfigurationManager();
    InputStream s = getClass().getClassLoader().getResourceAsStream("config/capture.properties");
    if (s == null) {
      throw new RuntimeException("Unable to load capture properties for test!");
    }
    Properties p = new Properties();
    p.load(s);
    p.put("org.opencastproject.storage.dir", new File(System.getProperty("java.io.tmpdir"), "capture-agent-test").getAbsolutePath());
    p.put("org.opencastproject.server.url", "http://localhost:8080");
    config.updated(p);

    sched = new SchedulerImpl();
    sched.setConfigService(config);
    Properties schedulerProps = new Properties();
    s = getClass().getClassLoader().getResourceAsStream("config/scheduler.properties");
    if (s == null) {
      throw new RuntimeException("Unable to load configuration file for scheduler!");
    }
    schedulerProps.load(s);
    sched.updated(schedulerProps);
    
    // creates agent, initially idle
    agent = new CaptureAgentImpl();
    agent.setConfigService(config);
    sched.setCaptureAgent(agent);

    Assert.assertNull(agent.getAgentState());
    agent.activate(null);
    Assert.assertEquals(agent.getAgentState(), AgentState.IDLE);
    
    // setup testing properties
    properties = new Properties();
    properties.setProperty(CaptureParameters.RECORDING_ID, recordingID);
  }
  
  @After
  public void tearDown() {
    agent = null;
    properties = null;
  }
  
  @Test
  public void testCaptureAgentImpl() throws Exception {
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
    
    // the appropriate files exists, so the capture can be stopped. The agent's state should return to idle
    // and a stopped capture file should exists.
    Assert.assertTrue(agent.stopCapture(recordingID));
    Assert.assertEquals(AgentState.IDLE, agent.getAgentState());
    File captureStopped = new File(outputdir.getAbsolutePath(), CaptureParameters.CAPTURE_STOPPED_FILE_NAME);
    Assert.assertTrue(captureStopped.exists());
    
    Thread.sleep(1000);

    // test creation of the manifest file
    File manifestFile = new File(outputdir.getAbsolutePath(), CaptureParameters.MANIFEST_NAME);
    Assert.assertTrue(manifestFile.exists());

    Thread.sleep(1000);

    // test zipping media
    File zippedMedia = new File(outputdir.getAbsolutePath(), CaptureParameters.ZIP_NAME);
    Assert.assertTrue(zippedMedia.exists());
    
    // clean up the files created
    FileUtils.deleteQuietly(outputdir);
  }
}
