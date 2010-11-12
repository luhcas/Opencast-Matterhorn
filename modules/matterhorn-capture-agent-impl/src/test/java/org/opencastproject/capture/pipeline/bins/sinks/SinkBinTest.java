package org.opencastproject.capture.pipeline.bins.sinks;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Properties;

import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Gst;
import org.gstreamer.Pad;
import org.junit.After;
import org.junit.AfterClass;
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

public class SinkBinTest {

  /** Capture Device Properties created for unit testing **/
  CaptureDevice captureDevice = null;
  
  
  /** True to run the tests */
  private static boolean gstreamerInstalled = true;
  
  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(CaptureDeviceBinTest.class);

  private String maxSizeBuffersDefault;

  private String maxSizeBytesDefault;

  private String maxSizeTimeDefault;

  @BeforeClass
  public static void testGst() {
    try {
      Gst.init();
    } catch (Throwable t) {
      logger.warn("Skipping agent tests due to unsatisifed gstreamer installation");
      gstreamerInstalled = false;
    }
  }
   
  @AfterClass
  public static void tearDownGst() {
    if (gstreamerInstalled) {
      Gst.deinit();
    }
  }
  
  @Before
  public void setup() throws ConfigurationException, IOException, URISyntaxException {
    if (!gstreamerInstalled)
      return;
    Element testQueue = ElementFactory.make("queue", null);
    maxSizeBuffersDefault = testQueue.getPropertyDefaultValue("max-size-buffers").toString();
    maxSizeBytesDefault = testQueue.getPropertyDefaultValue("max-size-bytes").toString();
    maxSizeTimeDefault = testQueue.get("max-size-time").toString();
    
  }

  @After
  public void tearDown() {
    if (!gstreamerInstalled)
      return;
    captureDevice = null;
  }
  
  /** Salient queue properties to test are bufferCount, bufferBytes and bufferTime. **/
  private Properties createQueueProperties(String bufferCount, String bufferBytes, String bufferTime){
    Properties captureDeviceProperties = BinTestHelpers.createCaptureDeviceProperties(null, null, null, null, null, bufferCount, bufferBytes, bufferTime, null);
    return captureDeviceProperties;
  }

  @Test
  public void testSetQueuePropertiesAllNull() {
    if (!gstreamerInstalled) return;
    Properties captureDeviceProperties = createQueueProperties(null, null, null);
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.VIDEOTESTSRC, "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties);
    SinkBin sinkBin = createSinkBinDontWantException(captureDeviceProperties);
    checkQueueProperties(sinkBin, maxSizeBuffersDefault, maxSizeBytesDefault, maxSizeTimeDefault); 
  }

  /** Test setting the max buffer size  of the queue to different values **/ 
  @Test
  public void testSetQueuePropertieMaxSizeBuffersToBelowMinimum() {
    if (!gstreamerInstalled) return;
    Properties captureDeviceProperties = createQueueProperties("-1", null, null);
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.VIDEOTESTSRC, "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties); 
    SinkBin sinkBin = createSinkBinDontWantException(captureDeviceProperties);
    checkQueueProperties(sinkBin, "-1", maxSizeBytesDefault, maxSizeTimeDefault); 
  }
  
  @Test
  public void testSetQueuePropertieMaxSizeBuffersToMinimum() {
    if (!gstreamerInstalled) return;
    Properties captureDeviceProperties = createQueueProperties("0", null, null);
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.VIDEOTESTSRC, "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties); 
    SinkBin sinkBin = createSinkBinDontWantException(captureDeviceProperties);
    checkQueueProperties(sinkBin, "0", maxSizeBytesDefault, maxSizeTimeDefault); 
  }
  
  @Test
  public void testSetQueuePropertieMaxSizeBuffersToNormalValue() {
    if (!gstreamerInstalled) return;
    Properties captureDeviceProperties = createQueueProperties("250", null, null);
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.VIDEOTESTSRC, "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties); 
    SinkBin sinkBin = createSinkBinDontWantException(captureDeviceProperties);
    checkQueueProperties(sinkBin, "250", maxSizeBytesDefault, maxSizeTimeDefault); 
  }
  
  @Test
  public void testSetQueuePropertieMaxSizeBuffersToMaximumValue() {
    if (!gstreamerInstalled) return;
    Properties captureDeviceProperties = createQueueProperties("" + Integer.MAX_VALUE, null, null);
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.VIDEOTESTSRC, "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties); 
    SinkBin sinkBin = createSinkBinDontWantException(captureDeviceProperties);
    checkQueueProperties(sinkBin, "" + Integer.MAX_VALUE, maxSizeBytesDefault, maxSizeTimeDefault); 
  }
  
  @Test
  public void testSetQueuePropertieMaxSizeBuffersToGarbageValue() {
    if (!gstreamerInstalled) return;
    Properties captureDeviceProperties = createQueueProperties("Invalid String", null, null);
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.VIDEOTESTSRC, "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties); 
    @SuppressWarnings("unused")
    SinkBin sinkBin = createSinkBinWantException(captureDeviceProperties);
  }
  
  /** Test setting the max bytes size of the queue to different values **/
  @Test
  public void testSetQueuePropertieMaxSizeBytesToBelowMinimum() {
    if (!gstreamerInstalled) return;
    Properties captureDeviceProperties = createQueueProperties(null, "-1", null);
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.VIDEOTESTSRC, "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties); 
    SinkBin sinkBin = createSinkBinDontWantException(captureDeviceProperties);
    checkQueueProperties(sinkBin, maxSizeBuffersDefault, "-1", maxSizeTimeDefault); 
    
  }
  
  @Test
  public void testSetQueuePropertieMaxSizeBytesToMinimum() {
    if (!gstreamerInstalled) return;
    Properties captureDeviceProperties = createQueueProperties(null, "0", null);
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.VIDEOTESTSRC, "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties); 
    SinkBin sinkBin = createSinkBinDontWantException(captureDeviceProperties);
    checkQueueProperties(sinkBin, maxSizeBuffersDefault, "0", maxSizeTimeDefault); 
  }
  
  @Test
  public void testSetQueuePropertieMaxSizeBytesToNormalValue() {
    if (!gstreamerInstalled) return;
    Properties captureDeviceProperties = createQueueProperties(null, "12485760", null);
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.VIDEOTESTSRC, "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties); 
    SinkBin sinkBin = createSinkBinDontWantException(captureDeviceProperties);
    checkQueueProperties(sinkBin, maxSizeBuffersDefault, "12485760", maxSizeTimeDefault); 
    
  }
  
  @Test
  public void testSetQueuePropertieMaxSizeBytesToMaximumValue() {
    if (!gstreamerInstalled) return;
    Properties captureDeviceProperties = createQueueProperties(null, "" + Integer.MAX_VALUE, null);
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.VIDEOTESTSRC, "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties); 
    SinkBin sinkBin = createSinkBinDontWantException(captureDeviceProperties);
    checkQueueProperties(sinkBin, maxSizeBuffersDefault, "" + Integer.MAX_VALUE, maxSizeTimeDefault); 
    
  }
  
  @Test
  public void testSetQueuePropertieMaxSizeBytesToGarbageValue() {
    if (!gstreamerInstalled) return;
    Properties captureDeviceProperties = createQueueProperties(null, "Invalid String", null);
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.VIDEOTESTSRC, "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties); 
    @SuppressWarnings("unused")
    SinkBin sinkBin = createSinkBinWantException(captureDeviceProperties);
  }
 
  /** Test setting the max time size of the queue to different values **/
  @Test
  public void testSetQueuePropertieMaxSizeTimeToBelowMinimum() {
    if (!gstreamerInstalled) return;
    Properties captureDeviceProperties = createQueueProperties(null, null, "-1");
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.VIDEOTESTSRC, "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties); 
    SinkBin sinkBin = createSinkBinDontWantException(captureDeviceProperties);
    checkQueueProperties(sinkBin, maxSizeBuffersDefault, maxSizeBytesDefault,"-1"); 
    
  }
  
  @Test
  public void testSetQueuePropertieMaxSizeTimeToMinimum() {
    if (!gstreamerInstalled) return;
    Properties captureDeviceProperties = createQueueProperties(null, null, "0");
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.VIDEOTESTSRC, "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties); 
    SinkBin sinkBin = createSinkBinDontWantException(captureDeviceProperties);
    checkQueueProperties(sinkBin, maxSizeBuffersDefault, maxSizeBytesDefault, "0"); 
  }
  
  @Test
  public void testSetQueuePropertieMaxSizeTimeToNormalValue() {
    if (!gstreamerInstalled) return;
    Properties captureDeviceProperties = createQueueProperties(null, null, "1000000");
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.VIDEOTESTSRC, "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties); 
    SinkBin sinkBin = createSinkBinDontWantException(captureDeviceProperties);
    checkQueueProperties(sinkBin, maxSizeBuffersDefault, maxSizeBytesDefault, "1000000"); 
    
  }
  
  @Test
  public void testSetQueuePropertieMaxSizeTimeToMaximumValue() {
    if (!gstreamerInstalled) return;
    Properties captureDeviceProperties = createQueueProperties(null, null, "" + Integer.MAX_VALUE);
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.VIDEOTESTSRC, "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties); 
    SinkBin sinkBin = createSinkBinDontWantException(captureDeviceProperties);
    checkQueueProperties(sinkBin, maxSizeBuffersDefault, maxSizeBytesDefault, "" + Integer.MAX_VALUE); 
    
  }
  
  @Test
  public void testSetQueuePropertieMaxSizeTimeToGarbageValue() {
    if (!gstreamerInstalled) return;
    Properties captureDeviceProperties = createQueueProperties(null, "Invalid String", null);
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.VIDEOTESTSRC, "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties); 
    @SuppressWarnings("unused")
    SinkBin sinkBin = createSinkBinWantException(captureDeviceProperties);
  }
  
  @Test
  public void testGhostPadIsCreated() {
    if (!gstreamerInstalled) return;
    Properties captureDeviceProperties = createQueueProperties(null, null, null);
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.VIDEOTESTSRC, "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties); 
    SinkBin sinkBin = createSinkBinDontWantException(captureDeviceProperties);
    List<Pad> binPads = sinkBin.getBin().getPads();
    Assert.assertEquals(1, binPads.size());
    Assert.assertEquals(SinkBin.GHOST_PAD_NAME, binPads.get(0).getName());
  }
  
  private void checkQueueProperties(SinkBin sinkBin, String expectedMaxSizeBuffer, String expectedMaxSizeBytes, String expectedMaxSizeTime) {
    Assert.assertEquals(expectedMaxSizeBuffer, sinkBin.queue.get("max-size-buffers").toString());
    Assert.assertEquals(expectedMaxSizeBytes, sinkBin.queue.get("max-size-bytes").toString());
    Assert.assertEquals(expectedMaxSizeTime, sinkBin.queue.get("max-size-time").toString());
  }

  private SinkBin createSinkBinDontWantException(Properties captureDeviceProperties) {
    SinkBin sinkBin = null;
    try {
      sinkBin = createSinkBin(captureDeviceProperties);
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail();
    }
    return sinkBin;
  }
  
  private SinkBin createSinkBinWantException(Properties captureDeviceProperties) {
    SinkBin sinkBin = null;
    try {
      sinkBin = createSinkBin(captureDeviceProperties);
      Assert.fail();
    } catch (Exception e) {
      
    }
    return sinkBin;
  }

  private SinkBin createSinkBin(Properties captureDeviceProperties) throws Exception {
    SinkBin sinkBin;
    sinkBin = new SinkBin(captureDevice, captureDeviceProperties) {
      
      @Override
      protected void linkElements() throws Exception {
        
      }
      
      @Override
      protected void addElementsToBin() {
        
      }
      
      @Override
      public Element getSrc() {
        return queue;
      }
    };
    return sinkBin;
  }
}
