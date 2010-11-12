package org.opencastproject.capture.pipeline.bins.sources;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

import org.gstreamer.Gst;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.opencastproject.capture.pipeline.SourceDeviceName;
import org.opencastproject.capture.pipeline.bins.BinTestHelpers;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceBinTest;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomAudioSrcBinTest {

  /** Capture Device Properties created for unit testing **/
  CaptureDevice captureDevice = null;
    
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
   
  @Before @Ignore
  public void setup() throws ConfigurationException, IOException, URISyntaxException {
    if (!gstreamerInstalled)
      return;
  }

  @AfterClass
  public static void tearDownGst() {
    if (gstreamerInstalled) {
      Gst.deinit();
    }
  }
  
  @After
  public void tearDown() {
    if (!gstreamerInstalled)
      return;
    captureDevice = null;
  }
  
  /** Salient encoder properties are codec and bitrate **/
  /** Salient muxer properties are codec and container **/
  private Properties createProperties(String customSource){
    Properties captureDeviceProperties = BinTestHelpers.createCaptureDeviceProperties(customSource, null, null, null, null, null, null, null, null);
    return captureDeviceProperties;
  }
  
  @Test
  public void nullSettingForCustomSourceResultsInException() {
    if (!gstreamerInstalled) return;
    Properties captureDeviceProperties = createProperties(null);
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.CUSTOM_AUDIO_SRC, "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties);
    @SuppressWarnings("unused")
    CustomAudioSrcBin customAudioSrcBin = createCustomAudioSrcBinWantException(captureDeviceProperties);
  }

  @Test
  public void garbageSettingForCustomSourceResultsInException() {
    if (!gstreamerInstalled) return;
    Properties captureDeviceProperties = createProperties("This is not really a source");
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.CUSTOM_AUDIO_SRC, "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties);
    @SuppressWarnings("unused")
    CustomAudioSrcBin customAudioSrcBin = createCustomAudioSrcBinWantException(captureDeviceProperties);
  }
  
  @Test
  public void singleItemInStringResultsInCorrectPipeline() {
    if (!gstreamerInstalled) return;
    Properties captureDeviceProperties = createProperties("fakesrc");
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.CUSTOM_AUDIO_SRC, "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties);
    @SuppressWarnings("unused")
    CustomAudioSrcBin customAudioSrcBin = createCustomAudioSrcBinDontWantException(captureDeviceProperties);
  }
  
  @Test
  public void multiItemInStringResultsInCorrectPipeline() {
    if (!gstreamerInstalled) return;
    Properties captureDeviceProperties = createProperties("fakesrc ! queue");
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.CUSTOM_AUDIO_SRC, "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties);
    @SuppressWarnings("unused")
    CustomAudioSrcBin customAudioSrcBin = createCustomAudioSrcBinDontWantException(captureDeviceProperties);
  }
  
  private CustomAudioSrcBin createCustomAudioSrcBinDontWantException(Properties captureDeviceProperties) {
    CustomAudioSrcBin customAudioSrcBin = null;
    try {
      customAudioSrcBin = createCustomAudioSrcBin(captureDeviceProperties);
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail();
    }
    return customAudioSrcBin;
  }
  
  private CustomAudioSrcBin createCustomAudioSrcBinWantException(Properties captureDeviceProperties) {
    CustomAudioSrcBin customAudioSrcBin = null;
    try {
      customAudioSrcBin = createCustomAudioSrcBin(captureDeviceProperties);
      Assert.fail();
    } catch (Exception e) {
      
    }
    return customAudioSrcBin;
  }

  private CustomAudioSrcBin createCustomAudioSrcBin(Properties captureDeviceProperties) throws Exception {
    CustomAudioSrcBin customAudioSrcBin;
    customAudioSrcBin = new CustomAudioSrcBin(captureDevice, captureDeviceProperties);
    return customAudioSrcBin;
  }
}
