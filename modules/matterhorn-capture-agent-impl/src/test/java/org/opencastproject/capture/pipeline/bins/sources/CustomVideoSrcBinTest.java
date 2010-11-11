package org.opencastproject.capture.pipeline.bins.sources;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

import org.gstreamer.Gst;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opencastproject.capture.pipeline.SourceDeviceName;
import org.opencastproject.capture.pipeline.bins.BinTestHelpers;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceBinTest;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomVideoSrcBinTest {

  /** Capture Device Properties created for unit testing **/
  CaptureDevice captureDevice = null;
  
  /** Properties specifically designed for unit testing */
  //private static Properties properties = null;
  
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
    Properties captureDeviceProperties = createProperties(null);
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.VIDEOTESTSRC, "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties);
    @SuppressWarnings("unused")
    CustomVideoSrcBin customVideoSrcBin = createCustomVideoSrcBinWantException(captureDeviceProperties);
  }

  @Test
  public void garbageSettingForCustomSourceResultsInException() {
    Properties captureDeviceProperties = createProperties("This is not really a source");
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.VIDEOTESTSRC, "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties);
    @SuppressWarnings("unused")
    CustomVideoSrcBin customVideoSrcBin = createCustomVideoSrcBinWantException(captureDeviceProperties);
  }
  
  @Test
  public void singleItemInStringResultsInCorrectPipeline() {
    Properties captureDeviceProperties = createProperties("fakesrc");
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.VIDEOTESTSRC, "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties);
    @SuppressWarnings("unused")
    CustomVideoSrcBin customVideoSrcBin = createCustomVideoSrcBinDontWantException(captureDeviceProperties);
  }
  
  @Test
  public void multiItemInStringResultsInCorrectPipeline() {
    Properties captureDeviceProperties = createProperties("fakesrc ! queue");
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.VIDEOTESTSRC, "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties);
    @SuppressWarnings("unused")
    CustomVideoSrcBin customVideoSrcBin = createCustomVideoSrcBinDontWantException(captureDeviceProperties);
  }
  
  private CustomVideoSrcBin createCustomVideoSrcBinDontWantException(Properties captureDeviceProperties) {
    CustomVideoSrcBin customVideoSrcBin = null;
    try {
      customVideoSrcBin = createCustomVideoSrcBin(captureDeviceProperties);
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail();
    }
    return customVideoSrcBin;
  }
  
  private CustomVideoSrcBin createCustomVideoSrcBinWantException(Properties captureDeviceProperties) {
    CustomVideoSrcBin customVideoSrcBin = null;
    try {
      customVideoSrcBin = createCustomVideoSrcBin(captureDeviceProperties);
      Assert.fail();
    } catch (Exception e) {
      
    }
    return customVideoSrcBin;
  }

  private CustomVideoSrcBin createCustomVideoSrcBin(Properties captureDeviceProperties) throws Exception {
    CustomVideoSrcBin customVideoSrcBin;
    customVideoSrcBin = new CustomVideoSrcBin(captureDevice, captureDeviceProperties);
    return customVideoSrcBin;
  }
}
