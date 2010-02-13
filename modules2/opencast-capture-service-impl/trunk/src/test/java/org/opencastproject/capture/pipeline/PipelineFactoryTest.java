package org.opencastproject.capture.pipeline;

import static org.junit.Assert.assertEquals;

import org.opencastproject.capture.impl.CaptureParameters;

import org.gstreamer.Pipeline;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Properties;

/**
 * TODO: Clarify how gstreamer testing should be done.
 */
public class PipelineFactoryTest {
  
  private static final Logger logger = LoggerFactory.getLogger(PipelineFactoryTest.class);

  private ArrayList<String> devices;
  
  private Pipeline testPipeline;
  
  @Before
  public void setup() {
    devices = new ArrayList<String>();
    // determine devices to test from command line parameters
    if (System.getProperty("testHauppauge") != null) {
      String hauppaugeDevice = System.getProperty("testHauppauge");
      if (new File(hauppaugeDevice).exists()) {
        devices.add(hauppaugeDevice);
        logger.info("Testing Hauppauge card at: " + hauppaugeDevice);
      }
      else {
        logger.error("File does not exist: " + hauppaugeDevice);
        Assert.fail(); 
      }
    }
    if (System.getProperty("testEpiphan") != null) {
      String epiphanDevice = System.getProperty("testEpiphan");
      if (new File(epiphanDevice).exists()) {
        devices.add(epiphanDevice);
        logger.info("Testing Epiphan card at: " + epiphanDevice);
      }
      else {
        logger.error("File does not exist: " + epiphanDevice);
        Assert.fail(); 
      }
    }
    if (System.getProperty("testBt878") != null) {
      String bt878Device = System.getProperty("testBt878");
      if (new File(bt878Device).exists()) {
        devices.add(bt878Device);
        logger.info("Testing BT878 card at: "+ bt878Device);
      }
      else {
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
  
  @After
  public void tearDown() {
    
  }
  
  @Test
  public void testPipelineFactory() {
    // if we have something to test
    if (!devices.isEmpty()) {
      
      // setup capture properties
      Properties props = new Properties();
      String deviceNames = "";
      for (int i = 0; i < devices.size(); i++) {
        props.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + i + CaptureParameters.CAPTURE_DEVICE_SOURCE, devices.get(i));
        props.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + i + CaptureParameters.CAPTURE_DEVICE_DEST, i + ".out");
        deviceNames += i + ",";
      }
      props.setProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, deviceNames);
      testPipeline = PipelineFactory.create(props);
      assertEquals(testPipeline.getSources().size(), devices.size());
      
    }
    
    
    
  }
  
}
