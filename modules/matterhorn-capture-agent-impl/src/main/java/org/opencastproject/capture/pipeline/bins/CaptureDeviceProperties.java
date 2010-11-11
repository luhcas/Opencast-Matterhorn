package org.opencastproject.capture.pipeline.bins;

import java.util.Properties;

import org.opencastproject.capture.api.CaptureParameters;

public class CaptureDeviceProperties {
  public String customSource;
  public String codec;
  public String container;
  public String bitrate;
  public String framerate;
  public String bufferCount;
  public String bufferBytes;
  public String bufferTime;
  public boolean confidence;
  
  public CaptureDeviceProperties(CaptureDevice captureDevice, Properties properties){
    customSource = captureDevice.properties.getProperty("customSource");
    codec = captureDevice.properties.getProperty("codec");
    container = captureDevice.properties.getProperty("container");
    bitrate = captureDevice.properties.getProperty("bitrate");
    framerate = captureDevice.properties.getProperty("framerate");
    bufferCount = captureDevice.properties.getProperty("bufferCount");
    bufferBytes = captureDevice.properties.getProperty("bufferBytes");
    bufferTime = captureDevice.properties.getProperty("bufferTime");
    if(properties != null && properties.getProperty(CaptureParameters.CAPTURE_CONFIDENCE_ENABLE) != null){
      confidence = Boolean.valueOf(properties.getProperty(CaptureParameters.CAPTURE_CONFIDENCE_ENABLE));
    }
    else{
      confidence = false;
    }
  }
}
