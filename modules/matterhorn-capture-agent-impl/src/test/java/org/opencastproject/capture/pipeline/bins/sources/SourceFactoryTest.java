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
package org.opencastproject.capture.pipeline.bins.sources;

import static org.easymock.EasyMock.createMock;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

import org.gstreamer.Gst;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opencastproject.capture.api.CaptureAgent;
import org.opencastproject.capture.api.CaptureParameters;
import org.opencastproject.capture.pipeline.SourceDeviceName;
import org.opencastproject.capture.pipeline.bins.BinTestHelpers;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceBinTest;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourceFactoryTest {

CaptureAgent captureAgentMock;
  
  /** Capture Device Properties created for unit testing **/
  CaptureDevice captureDevice = null;
  
  /** Properties specifically designed for unit testing */
  private static Properties properties = null;
  
  private Properties captureDeviceProperties;
  
  /** True to run the tests */
  private static boolean gstreamerInstalled = true;
  
  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(CaptureDeviceBinTest.class);

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
    
    captureAgentMock = createMock(CaptureAgent.class);
    captureDeviceProperties = BinTestHelpers.createCaptureDeviceProperties(null, null, null, null, null, null, null, 
            null, null);
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.EPIPHAN_VGA2USB, 
            "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties); 
    
    // setup testing properties
    properties = new Properties();
    properties.setProperty(CaptureParameters.CAPTURE_CONFIDENCE_VIDEO_LOCATION, "/tmp/testpipe/confidence");
    properties.setProperty(CaptureParameters.CAPTURE_CONFIDENCE_ENABLE, "false");
    properties.setProperty(CaptureParameters.CAPTURE_CONFIDENCE_VIDEO_LOCATION, "/tmp/testpipe/confidence");
    
    
  }

  @After
  public void tearDown() {
    if (!gstreamerInstalled)
      return;

    properties = null;
    captureDevice = null;
    //FileUtils.deleteQuietly(new File(SysteCm.getProperty("java.io.tmpdir"), "capture-agent-test"));
  }

  
  @Test 
  public void testVideoTestSrc() throws Exception{
    if (!gstreamerInstalled)
      return;
    // Setup properties for an videotestsrc card.
    captureDevice = BinTestHelpers.createCaptureDevice(null, SourceDeviceName.VIDEOTESTSRC, "Video Test Source", 
            "/tmp/testpipe/test.mpeg", captureDeviceProperties);
    SrcBin srcBin = SourceFactory.getInstance().getSource(captureDevice, properties, captureAgentMock);
    // Make sure we got the right object back
    Assert.assertTrue(srcBin instanceof VideoTestSrc);
    checkCorrectnessOfVideoSource(srcBin);
  }
  
  @Test 
  public void testExistingEpiphanSource() throws Exception{
    if (!gstreamerInstalled)
      return;
    // Setup properties for an epiphan card.
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.EPIPHAN_VGA2USB, 
            "Epiphan VGA 2 USB", "/tmp/testpipe/test.mpeg", captureDeviceProperties);
    SrcBin srcBin = SourceFactory.getInstance().getSource(captureDevice, properties, captureAgentMock);
    // Make sure we got the right object back
    Assert.assertTrue(srcBin instanceof EpiphanVGA2USBV4LSrcBin);
    checkCorrectnessOfVideoSource(srcBin);
  }
  
  @Test
  public void testMissingEpiphanSource() throws Exception{
    if (!gstreamerInstalled)
      return;
    // Setup properties for an epiphan card.
    captureDevice = BinTestHelpers.createCaptureDevice("/woot/video0", SourceDeviceName.EPIPHAN_VGA2USB, 
            "Epiphan VGA 2 USB", "/tmp/testpipe/test.mpeg", captureDeviceProperties);
    SrcBin srcBin = SourceFactory.getInstance().getSource(captureDevice, properties, captureAgentMock);
    // Make sure we got the right object back
    Assert.assertTrue(srcBin instanceof EpiphanVGA2USBV4LSrcBin);
    checkCorrectnessOfVideoSource(srcBin);
  }
  
  @Test 
  public void testExistingV4LSource() throws Exception{
    if (!gstreamerInstalled)
      return;
    // Setup properties for an v4lsource
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.V4LSRC, "V4L Source",
            "/tmp/testpipe/test.mpeg", captureDeviceProperties);
    SrcBin srcBin = SourceFactory.getInstance().getSource(captureDevice, properties, captureAgentMock);
    // Make sure we got the right object back
    Assert.assertTrue(srcBin instanceof V4LSrcBin);
    checkCorrectnessOfVideoSource(srcBin);
  }
  
  @Test
  public void testMissingV4LSource() throws Exception{
    if (!gstreamerInstalled)
      return;
    // Setup properties for an v4lsource
    captureDevice = BinTestHelpers.createCaptureDevice("/woot!/video0", SourceDeviceName.V4LSRC, "V4L Source", 
            "/tmp/testpipe/test.mpeg", captureDeviceProperties);
    SrcBin srcBin = SourceFactory.getInstance().getSource(captureDevice, properties, captureAgentMock);
    // Make sure we got the right object back
    Assert.assertTrue(srcBin instanceof V4LSrcBin);
    checkCorrectnessOfVideoSource(srcBin);
  }
  
  @Test
  public void testExistingHauppaugeSource() throws Exception{
    if (!gstreamerInstalled)
      return;
    // Setup properties for an hauppage source.
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.HAUPPAUGE_WINTV, 
            "Hauppage Source", "/tmp/testpipe/test.mpeg", captureDeviceProperties);
    SrcBin srcBin = SourceFactory.getInstance().getSource(captureDevice, properties, captureAgentMock);
    // Make sure we got the right object back
    Assert.assertTrue(srcBin instanceof HauppaugeSrcBin);
    checkCorrectnessOfVideoSource(srcBin);
  }
  
  @Test
  public void testMissingHauppaugeSource() throws Exception{
    if (!gstreamerInstalled)
      return;
    // Setup properties for an hauppage source.
    captureDevice = BinTestHelpers.createCaptureDevice("/woot!/video0", SourceDeviceName.HAUPPAUGE_WINTV, 
            "Hauppage Source", "/tmp/testpipe/test.mpeg", captureDeviceProperties);
    SrcBin srcBin = SourceFactory.getInstance().getSource(captureDevice, properties, captureAgentMock);
    // Make sure we got the right object back
    Assert.assertTrue(srcBin instanceof HauppaugeSrcBin);
    checkCorrectnessOfVideoSource(srcBin);
  }
  
  @Test
  public void testExistingBlueCherrySource() throws Exception{
    if (!gstreamerInstalled)
      return;
    // Setup properties for an bluecherry card.
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.BLUECHERRY_PROVIDEO, 
            "Bluecherry Source", "/tmp/testpipe/test.mpeg", captureDeviceProperties);
    SrcBin srcBin = SourceFactory.getInstance().getSource(captureDevice, properties, captureAgentMock);
    // Make sure we got the right object back
    Assert.assertTrue(srcBin instanceof BlueCherrySrcBin);
    checkCorrectnessOfVideoSource(srcBin);
  }
  
  @Test
  public void testMissingBlueCherrySource() throws Exception{
    if (!gstreamerInstalled)
      return;
    // Setup properties for an bluecherry card.
    captureDevice = BinTestHelpers.createCaptureDevice("/woot!/video0", SourceDeviceName.BLUECHERRY_PROVIDEO, 
            "Bluecherry Source", "/tmp/testpipe/test.mpeg", captureDeviceProperties);
    SrcBin srcBin = SourceFactory.getInstance().getSource(captureDevice, properties, captureAgentMock);
    // Make sure we got the right object back
    Assert.assertTrue(srcBin instanceof BlueCherrySrcBin);
    checkCorrectnessOfVideoSource(srcBin);
  }


  private void checkCorrectnessOfVideoSource(SrcBin srcBin) {
    // Check to make sure the sink exists and is not null.
    Assert.assertTrue(srcBin.getSrcPad()!= null);
    // Check to make sure that this is a video device
    Assert.assertTrue(srcBin.isVideoDevice());
    // Check to make sure that isn't an audio device
    Assert.assertTrue(!srcBin.isAudioDevice());
  }
  
  @Test
  public void testExistingAlsaSource() throws Exception{
    if (!gstreamerInstalled)
      return;
    // Setup properties for an alsa source
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.ALSASRC, "Alsa Source", 
            "/tmp/testpipe/test.mp2", captureDeviceProperties);
    SrcBin srcBin = SourceFactory.getInstance().getSource(captureDevice, properties, captureAgentMock);
    // Make sure we got the right object back
    Assert.assertTrue(srcBin instanceof AlsaSrcBin);
    // Check the actual correctness of the object
    checkCorrectnessOfAudioDevice(srcBin);
  }
  
  @Test
  public void testMissingAlsaSource() throws Exception{
    if (!gstreamerInstalled)
      return;
    // Setup properties for an alsa source
    captureDevice = BinTestHelpers.createCaptureDevice("/woot!/video0", SourceDeviceName.ALSASRC, "Alsa Source",
            "/tmp/testpipe/test.mp2", captureDeviceProperties);
    SrcBin srcBin = SourceFactory.getInstance().getSource(captureDevice, properties, captureAgentMock);
    // Make sure we got the right object back
    Assert.assertTrue(srcBin instanceof AlsaSrcBin);
    // Check the actual correctness of the object
    checkCorrectnessOfAudioDevice(srcBin);
  }

  
  @Test
  public void testExistingV4L2CustomVideoSource() throws Exception{
    if (!gstreamerInstalled)
      return;
    captureDeviceProperties = BinTestHelpers.createCaptureDeviceProperties("v4l2src device=" + 
            BinTestHelpers.V4L2_LOCATION, null, null, null, null, null, null, null, null);
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video1", SourceDeviceName.CUSTOM_VIDEO_SRC, 
            "Custom Video Bin Source", "/tmp/testpipe/test.mpeg", captureDeviceProperties);
    SrcBin srcBin = SourceFactory.getInstance().getSource(captureDevice, properties, captureAgentMock);
    // Make sure we got the right object back
    Assert.assertTrue(srcBin instanceof CustomVideoSrcBin);
    // Check the actual correctness of the object
  }

  @Test
  public void testExistingPulseCustomAudioSource() throws Exception{
    if (!gstreamerInstalled)
      return;
    captureDeviceProperties = BinTestHelpers.createCaptureDeviceProperties("pulsesrc", null, null, null, null, null, 
            null, null, null);
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.CUSTOM_AUDIO_SRC, 
            "Custom Audio Bin Source", "/tmp/testpipe/test.mp2", captureDeviceProperties);
    SrcBin srcBin = SourceFactory.getInstance().getSource(captureDevice, properties, captureAgentMock);
    // Make sure we got the right object back
    Assert.assertTrue(srcBin instanceof CustomAudioSrcBin);
    // Check the actual correctness of the object
  }
  
  @Test
  public void testFileSrcBin() throws Exception{
    if (!gstreamerInstalled)
      return;
    captureDevice = BinTestHelpers.createCaptureDevice(BinTestHelpers.HAUPPAGE_LOCATION, SourceDeviceName.FILE_DEVICE, 
            "File Device Source", "/tmp/testpipe/test.mp2", captureDeviceProperties);
    SrcBin srcBin = SourceFactory.getInstance().getSource(captureDevice, properties, captureAgentMock);
    // Make sure we got the right object back
    Assert.assertTrue(srcBin instanceof FileSrcBin);
    // Check the actual correctness of the object
  }
  
  private void checkCorrectnessOfAudioDevice(SrcBin srcBin) {
    // Check to make sure the sink exists and is not null.
    Assert.assertTrue(srcBin.getSrcPad()!= null);
    // Check to make sure that this is an audio device
    Assert.assertTrue(srcBin.isAudioDevice());
    // Check to make sure that isn't a video device
    Assert.assertTrue(!srcBin.isVideoDevice());
  }
}
