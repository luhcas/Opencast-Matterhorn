package org.opencastproject.capture.pipeline.bins;

import java.util.Properties;

import org.opencastproject.capture.api.CaptureParameters;
import org.opencastproject.capture.pipeline.SourceDeviceName;

public class BinTestHelpers {
  public static final String V4L_LOCATION = "/dev/vga";
  public static final String V4L2_LOCATION = "/dev/video0";
  public static final String HAUPPAGE_LOCATION = "/dev/video1";
  
  public static Properties createCaptureDeviceProperties(String customSource, String codec, String bitrate, String quantizer, String container, String bufferCount, String bufferBytes, String bufferTime, String framerate){
    Properties properties = new Properties();
   
    if (customSource != null)
      properties.setProperty("customSource", customSource);
    if (codec != null)
      properties.setProperty("codec", codec);
    if (bitrate != null)
      properties.setProperty("bitrate", bitrate);
    if (quantizer != null)
      properties.setProperty("quantizer", quantizer);
    if (container != null)
      properties.setProperty("container", container);
    if (bufferCount != null)
      properties.setProperty("bufferCount", bufferCount);
    if (bufferBytes != null)
      properties.setProperty("bufferBytes", bufferBytes);
    if (bufferTime != null)
      properties.setProperty("bufferTime", bufferTime);
    if (framerate != null)
      properties.setProperty("framerate", framerate);
    return properties;
  }
  
  public static CaptureDevice createCaptureDevice(String sourceLocation, SourceDeviceName sourceDeviceName, String friendlyName, String outputLocation, Properties captureDeviceProperties) {
    CaptureDevice captureDevice = new CaptureDevice(sourceLocation, sourceDeviceName, friendlyName, outputLocation);
    captureDevice.properties = captureDeviceProperties;
    return captureDevice;
  }
  
  public static Properties createConfidenceMonitoringProperties(){
    // setup testing properties
    Properties properties = new Properties();
    properties.setProperty(CaptureParameters.CAPTURE_CONFIDENCE_VIDEO_LOCATION, "/tmp/testpipe/confidence");
    properties.setProperty(CaptureParameters.CAPTURE_CONFIDENCE_ENABLE, "false");
    properties.setProperty(CaptureParameters.CAPTURE_CONFIDENCE_VIDEO_LOCATION, "/tmp/testpipe/confidence");
    return properties;
  }
}
