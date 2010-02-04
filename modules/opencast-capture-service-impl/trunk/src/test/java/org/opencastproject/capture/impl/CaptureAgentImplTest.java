/**
 *  Copyright 2009 The Regents of the University of California
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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.Properties;

/**
 * Test the implementation of the Capture Agent, which uses gstreamer to 
 * generate pipelines that capture the media. 
 */
public class CaptureAgentImplTest {
  
  /** The single instance of CaptureAgentImpl needed */
  private static CaptureAgentImpl agent = null;
  
  /** Properties specifically designed for unit testing */
  private static Properties properties = null;
  
  /** Define a recording ID for the test */
  private final static String recordingID = "UnitTest1";
  
  /** Directory containing the captured media */
  private static URL outputDir = null;
  
  @Before
  public void setup() {
    // creates agent, initially idle
    agent = new CaptureAgentImpl();
    Assert.assertEquals(agent.getAgentState(), AgentState.IDLE);
    
    // setup test media
    URL testScreen = getClass().getClassLoader().getResource("capture/screen.mpg");
    URL testPresenter = getClass().getClassLoader().getResource("capture/camera.mpg");
    URL testAudio = getClass().getClassLoader().getResource("capture/audio.mp3");
    outputDir = getClass().getClassLoader().getResource("ingest");
    
    // setup testing properties
    properties = new Properties();
    properties.setProperty(CaptureParameters.RECORDING_ID, recordingID);
    properties.setProperty(CaptureParameters.RECORDING_ROOT_URL, outputDir.getPath());
    properties.setProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, "SCREEN,PRESENTER,AUDIO");
    properties.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "PRESENTER" + CaptureParameters.CAPTURE_DEVICE_SOURCE, testPresenter.getPath());
    properties.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "PRESENTER" + CaptureParameters.CAPTURE_DEVICE_DEST, "camera_out.mpg");
    properties.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "SCREEN" + CaptureParameters.CAPTURE_DEVICE_SOURCE, testScreen.getPath());
    properties.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "SCREEN" + CaptureParameters.CAPTURE_DEVICE_DEST, "screen_out.mpg");
    properties.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "AUDIO" + CaptureParameters.CAPTURE_DEVICE_SOURCE, testAudio.getPath());
    properties.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "AUDIO" + CaptureParameters.CAPTURE_DEVICE_DEST, "audio_out.mp3");
  }
  
  @After
  public void tearDown() {
    agent = null;
    properties = null;
    outputDir = null;
  }
  
  @Test
  public void testCaptureAgentImpl() {
    // start the capture, assert the recording id is correct
    String id = agent.startCapture(properties);
    Assert.assertEquals(id, recordingID);
    
    // even with a mock capture, the state should remain capturing until stopCapture has been called
    Assert.assertEquals(AgentState.CAPTURING, agent.getAgentState());
    
    // affirm the captured media exists in the appropriate location
    String cameraOut = properties.getProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "PRESENTER" + CaptureParameters.CAPTURE_DEVICE_DEST);
    String screenOut = properties.getProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "SCREEN" + CaptureParameters.CAPTURE_DEVICE_DEST);
    String audioOut = properties.getProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "AUDIO" + CaptureParameters.CAPTURE_DEVICE_DEST);
    File cameraFile = new File(outputDir.getPath(), cameraOut);
    File screenFile = new File(outputDir.getPath(), screenOut);
    File audioFile = new File(outputDir.getPath(), audioOut);
    Assert.assertTrue(cameraFile.exists() && screenFile.exists() && audioFile.exists());
    
    // the appropriate files exists, so the capture can be stopped. The agent's state should return to idle
    // and a stopped capture file should exists.
    Assert.assertTrue(agent.stopCapture(recordingID));
    Assert.assertEquals(AgentState.IDLE, agent.getAgentState());
    File captureStopped = new File(outputDir.getPath(), CaptureParameters.CAPTURE_STOPPED_FILE_NAME);
    Assert.assertTrue(captureStopped.exists());
    
    // test creation of the manifest file
    Assert.assertTrue(agent.createManifest(recordingID));
    File manifestFile = new File(outputDir.getPath(), CaptureParameters.MANIFEST_NAME);
    Assert.assertTrue(manifestFile.exists());
    
    // test zipping media
    File zippedMedia = agent.zipFiles(recordingID);
    Assert.assertTrue(zippedMedia.exists());
    
    // clean up the files created
    cameraFile.deleteOnExit();
    screenFile.deleteOnExit();
    audioFile.deleteOnExit();
    captureStopped.deleteOnExit();
    manifestFile.deleteOnExit();
    zippedMedia.deleteOnExit();
  }
}
