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
package org.opencastproject.capture.pipeline;

import static org.easymock.EasyMock.createMock;

import org.opencastproject.capture.api.CaptureAgent;
import org.opencastproject.capture.api.CaptureParameters;
import org.opencastproject.capture.impl.ConfigurationManager;
import org.opencastproject.util.XProperties;
import org.osgi.service.cm.ConfigurationException;

import org.gstreamer.Gst;
import org.gstreamer.Pipeline;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

/**
 * TODO: Clarify how gstreamer testing should be done.
 */
public class PipelineFactoryTest {

  private static final Logger logger = LoggerFactory.getLogger(PipelineFactoryTest.class);

  private static ArrayList<String> devices;

  private Pipeline testPipeline;
  private static boolean gstreamerInstalled;

  @BeforeClass
  public static void setup() {
    try {
      Gst.init();
    } catch (Throwable t) {
      logger.warn("Skipping agent tests due to unsatisifed gstreamer installation");
      gstreamerInstalled = false;
    }

    devices = new ArrayList<String>();
    // determine devices to test from command line parameters
    if (System.getProperty("testHauppauge") != null) {
      String hauppaugeDevice = System.getProperty("testHauppauge");
      if (new File(hauppaugeDevice).exists()) {
        devices.add(hauppaugeDevice);
        logger.info("Testing Hauppauge card at: " + hauppaugeDevice);
      } else {
        logger.error("File does not exist: " + hauppaugeDevice);
        Assert.fail();
      }
    }
    if (System.getProperty("testEpiphan") != null) {
      String epiphanDevice = System.getProperty("testEpiphan");
      if (new File(epiphanDevice).exists()) {
        devices.add(epiphanDevice);
        logger.info("Testing Epiphan card at: " + epiphanDevice);
      } else {
        logger.error("File does not exist: " + epiphanDevice);
        Assert.fail();
      }
    }
    if (System.getProperty("testBt878") != null) {
      String bt878Device = System.getProperty("testBt878");
      if (new File(bt878Device).exists()) {
        devices.add(bt878Device);
        logger.info("Testing BT878 card at: " + bt878Device);
      } else {
        logger.error("File does not exist: " + bt878Device);
        Assert.fail();
      }
    }
    if (System.getProperty("testAlsa") != null) {
      String alsaDevice = System.getProperty("testAlsa");
      devices.add(alsaDevice);
      logger.info("Testing ALSA source: " + alsaDevice);
    }
  }

  @AfterClass
  public static void tearDown() {
    if (gstreamerInstalled) {
      Gst.deinit();
    }
    devices = null;
  }

  @Test
  public void testDevices() {
    // if we have something to test
    if (!devices.isEmpty()) {

      // setup capture properties
      Properties props = new Properties();
      String deviceNames = "";
      for (int i = 0; i < devices.size(); i++) {
        props.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + i + CaptureParameters.CAPTURE_DEVICE_SOURCE,
                devices.get(i));
        props.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + i + CaptureParameters.CAPTURE_DEVICE_DEST, i
                + ".out");
        deviceNames += i + ",";
      }
      props.setProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, deviceNames);
      testPipeline = PipelineFactory.create(props, false, null);
      Assert.assertEquals(testPipeline.getSources().size(), devices.size());
    }
  }

  @Test
  public void testNullProperties() {
    boolean success = false;
    try {
      PipelineFactory.create(null, false, null);
    } catch (NullPointerException e) {
      success = true;
    }
    Assert.assertTrue(success);
  }

  @Test
  public void testEmptyProperties() {
    Properties p = new Properties();
    Assert.assertNull(PipelineFactory.create(p, false, null));
  }

  @Test
  public void initDevicesCreatesDevicesSuccessfullyWithCorrectTypes() {

    if (!gstreamerInstalled)
      return;

    ConfigurationManager config = new ConfigurationManager();

    Properties p = null;
    try {
      p = loadProperties("config/capture.properties");
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }
    p.put("org.opencastproject.storage.dir", new File(System.getProperty("java.io.tmpdir"), "capture-agent-test")
            .getAbsolutePath());
    p.put("org.opencastproject.server.url", "http://localhost:8080");
    p.put(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_POLLING_INTERVAL, -1);
    p.put("M2_REPO", getClass().getClassLoader().getResource("m2_repo").getFile());
    try {
      config.updated(p);
    } catch (ConfigurationException e) {
      e.printStackTrace();
      Assert.fail();
    }
    CaptureAgent captureAgentMock = createMock(CaptureAgent.class);
    PipelineFactory.create(config.getAllProperties(), false, captureAgentMock);
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

  /*
   * @Test public void testWithInvalidFileSource() { Properties properties = new Properties();
   * properties.setProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, "INVALID");
   * properties.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "INVALID" +
   * CaptureParameters.CAPTURE_DEVICE_SOURCE, "capture/invalid.mpg");
   * properties.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "INVALID" + CaptureParameters.CAPTURE_DEVICE_DEST,
   * "invalid_out.mpg");
   * 
   * Pipeline pipeline = PipelineFactory.create(properties); Assert.assertNull(pipeline); }
   * 
   * @Test public void testWithFileSource() { URL testScreen =
   * getClass().getClassLoader().getResource("capture/screen.mpg"); URL testPresenter =
   * getClass().getClassLoader().getResource("capture/camera.mpg"); URL testAudio =
   * getClass().getClassLoader().getResource("capture/audio.mp3"); Properties properties = new Properties();
   * properties.setProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, "SCREEN,PRESENTER,AUDIO");
   * properties.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "PRESENTER" +
   * CaptureParameters.CAPTURE_DEVICE_SOURCE, testPresenter.getPath());
   * properties.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "PRESENTER" +
   * CaptureParameters.CAPTURE_DEVICE_DEST, "camera_out.mpg");
   * properties.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "SCREEN" +
   * CaptureParameters.CAPTURE_DEVICE_SOURCE, testScreen.getPath());
   * properties.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "SCREEN" + CaptureParameters.CAPTURE_DEVICE_DEST,
   * "screen_out.mpg"); properties.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "AUDIO" +
   * CaptureParameters.CAPTURE_DEVICE_SOURCE, testAudio.getPath());
   * properties.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "AUDIO" + CaptureParameters.CAPTURE_DEVICE_DEST,
   * "audio_out.mp3");
   * 
   * Pipeline pipeline = PipelineFactory.create(properties); Assert.assertNotNull(pipeline); }
   * 
   * @Test public void testAddPipeline() { DeviceName[] deviceList = { DeviceName.EPIPHAN_VGA2USB, DeviceName.ALSASRC,
   * DeviceName.BLUECHERRY_PROVIDEO, DeviceName.FILE, DeviceName.HAUPPAUGE_WINTV }; String source = "source"; String
   * dest = "destination"; boolean ret; for (DeviceName device : deviceList) { CaptureDevice captureDevice = new
   * CaptureDevice(source, device, dest); Pipeline pipeline = new Pipeline(); ret =
   * PipelineFactory.addPipeline(captureDevice, pipeline); Assert.assertTrue(ret); } }
   */
}
