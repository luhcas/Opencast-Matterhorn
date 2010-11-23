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
package org.opencastproject.capture.pipeline.bins;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import org.opencastproject.capture.admin.api.AgentState;
import org.opencastproject.capture.api.CaptureAgent;
import org.opencastproject.capture.api.CaptureParameters;
import org.opencastproject.capture.pipeline.bins.producers.ProducerType;

import org.apache.commons.io.FileUtils;
import org.gstreamer.Gst;
import org.gstreamer.Pipeline;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

@Ignore
public class CaptureDeviceBinTest {

  private static final int PIPELINE_TEARDOWN_TIME = 500;

  private static final int SLEEP_TIME = 1500;

  CaptureAgent captureAgentMock;

  /** Capture Device Properties created for unit testing **/
  CaptureDevice captureDevice = null;

  /** Properties specifically designed for unit testing */
  private static Properties properties = null;

  /** True to run the tests */
  private static boolean gstreamerInstalled = true;

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(CaptureDeviceBinTest.class);

  private static String tempDirectory = System.getProperty("java.io.tmpdir") + "/CaptureDeviceBinTest";

  private Properties captureDeviceProperties;

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

    new File(tempDirectory).mkdir();

    captureAgentMock = createMock(CaptureAgent.class);

    // setup testing properties
    properties = new Properties();
    properties.setProperty(CaptureParameters.CAPTURE_CONFIDENCE_VIDEO_LOCATION, "/tmp/testpipe/confidence");
    properties.setProperty(CaptureParameters.CAPTURE_CONFIDENCE_ENABLE, "false");
    properties.setProperty(CaptureParameters.CAPTURE_CONFIDENCE_VIDEO_LOCATION, "/tmp/testpipe/confidence");

    captureDeviceProperties = BinTestHelpers.createCaptureDeviceProperties(null, null, null, null, null, null, null,
            null, null);
  }

  @After
  public void tearDown() {
    if (!gstreamerInstalled)
      return;

    properties = null;
    captureDevice = null;
  }

  @AfterClass
  public static void afterClass() {
    FileUtils.deleteQuietly(new File(tempDirectory));
  }

  private CaptureDeviceBin createCaptureDevice() {
    CaptureDeviceBin captureDeviceBin = null;
    try {
      captureDeviceBin = new CaptureDeviceBin(captureDevice, properties, captureAgentMock);
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail();
    }
    return captureDeviceBin;
  }

  private void startPipeLine(CaptureDeviceBin captureDeviceBin) {
    try {
      Pipeline pipeline = new Pipeline();
      pipeline.add(captureDeviceBin.getBin());
      pipeline.play();
      Thread.sleep(SLEEP_TIME);
      pipeline.stop();
      Thread.sleep(PIPELINE_TEARDOWN_TIME);
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail();
    }
  }

  private void runDeviceTest(String deviceLocation, ProducerType deviceName, String friendlyDeviceName,
          String outputLocation) {
    if (!gstreamerInstalled)
      return;
    captureDevice = BinTestHelpers.createCaptureDevice(deviceLocation, deviceName, friendlyDeviceName, outputLocation,
            captureDeviceProperties);
    expect(captureAgentMock.getAgentState()).andReturn(AgentState.CAPTURING);
    replay(captureAgentMock);
    CaptureDeviceBin captureDeviceBin = createCaptureDevice();
    if (captureDeviceBin != null) {
      startPipeLine(captureDeviceBin);
    }

    Assert.assertTrue(new File(outputLocation).exists());
  }

  @Test
  public void testVideoTestSrcAndTestSink() throws InterruptedException {
    if (!gstreamerInstalled)
      return;
    runDeviceTest(BinTestHelpers.V4L_LOCATION, ProducerType.VIDEOTESTSRC, "Video Test Source", tempDirectory
            + "/videoTestSource.mpeg");
  }

  @Test
  public void testV4LSrcAndTestSink() {
    if (!gstreamerInstalled)
      return;
    runDeviceTest(BinTestHelpers.V4L_LOCATION, ProducerType.V4LSRC, "V4L Source", tempDirectory + "/v4lSrc.mpeg");
  }

  @Test
  public void testV4L2SrcAndTestSink() {
    if (!gstreamerInstalled)
      return;
    runDeviceTest(BinTestHelpers.V4L2_LOCATION, ProducerType.V4L2SRC, "V4L2-Source", tempDirectory
            + "/v4l2Src.mpeg");
  }

  @Test
  public void testEpiphanSrcAndTestSink() {
    if (!gstreamerInstalled)
      return;
    runDeviceTest(BinTestHelpers.V4L_LOCATION, ProducerType.EPIPHAN_VGA2USB, "Epiphan VGA2USB Source",
            tempDirectory + "/epiphanSrc.mpeg");
  }

  @Test
  public void testBlueCherryAndTestSink() {
    if (!gstreamerInstalled)
      return;
    runDeviceTest(BinTestHelpers.V4L2_LOCATION, ProducerType.BLUECHERRY_PROVIDEO, "Blue Cherry Source",
            tempDirectory + "/blueCherrySrc.mpeg");
  }

  @Test
  public void testAudioTestSrcAndTestSink() {
    if (!gstreamerInstalled)
      return;
    runDeviceTest(BinTestHelpers.V4L2_LOCATION, ProducerType.AUDIOTESTSRC, "Audio Test Source", tempDirectory
            + "/audioTestSrc.mp2");
  }

  @Test
  public void testPulseSrcAndTestSink() {
    if (!gstreamerInstalled)
      return;
    runDeviceTest(BinTestHelpers.V4L2_LOCATION, ProducerType.PULSESRC, "Audio Test Source", tempDirectory
            + "/pulseSrc.mp2");
  }

  @Test
  public void testCustomVideoSrcAndTestSink() {
    if (!gstreamerInstalled)
      return;
    
    captureDeviceProperties.setProperty("customSource", "v4l2src device=" + BinTestHelpers.V4L2_LOCATION);
    runDeviceTest(BinTestHelpers.V4L2_LOCATION, ProducerType.CUSTOM_VIDEO_SRC, "Custom Video Test Source",
            tempDirectory + "/customVideoSrc.mpg");
  }

  @Test
  public void testCustomAudioSrcAndTestSink() {
    if (!gstreamerInstalled)
      return;
    captureDeviceProperties.setProperty("customSource", "pulsesrc");
    runDeviceTest(BinTestHelpers.V4L2_LOCATION, ProducerType.CUSTOM_AUDIO_SRC, "Custom Video Test Source",
            tempDirectory + "/customAudioSrc.mp2");
  }

  @Test
  public void testHauppageSrcNoContainerChangeAndFileBin() {
    if (!gstreamerInstalled)
      return;
    runDeviceTest(BinTestHelpers.HAUPPAGE_LOCATION, ProducerType.HAUPPAUGE_WINTV, "Hauppage Source", tempDirectory
            + "/hauppageNoChangeSrc.mpg");
  }

  @Test
  public void testHauppageSrcContainerChange() {
    if (!gstreamerInstalled)
      return;
    captureDeviceProperties = BinTestHelpers.createCaptureDeviceProperties(null, "ffenc_mpeg2video", null, null,
            "mpegtsmux", null, null, null, null);
    runDeviceTest(BinTestHelpers.HAUPPAGE_LOCATION, ProducerType.HAUPPAUGE_WINTV, "Hauppage Source", tempDirectory
            + "/hauppageSrc.mpg");
  }

  @Test
  public void testFileSrc() {
    if (!gstreamerInstalled)
      return;
    captureDeviceProperties = BinTestHelpers.createCaptureDeviceProperties(null, null, null, null, null, null, null,
            null, null);
    runDeviceTest(BinTestHelpers.HAUPPAGE_LOCATION, ProducerType.HAUPPAUGE_WINTV, "Hauppage Source", tempDirectory
            + "/fileSrc.mpg");
  }
}
