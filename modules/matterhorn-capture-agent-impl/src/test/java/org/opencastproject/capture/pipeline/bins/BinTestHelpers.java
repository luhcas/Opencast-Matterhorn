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

import java.util.Properties;

import org.opencastproject.capture.api.CaptureParameters;
import org.opencastproject.capture.pipeline.bins.producers.ProducerType;

public class BinTestHelpers {
  public static final String V4L_LOCATION = "/dev/vga";
  public static final String V4L2_LOCATION = "/dev/video2";
  public static final String HAUPPAGE_LOCATION = "/dev/video0";
  
 
  private static String operatingSystemName = null;
  
  public static String getOsName()
  {
    if(operatingSystemName == null) { 
      operatingSystemName = System.getProperty("os.name");
    }
    return operatingSystemName;
  }

  public static boolean isWindows(){
    return getOsName().startsWith("Windows");
  }

  public static boolean isLinux() {
    return getOsName().startsWith("Linux");
  }
  

  
  public static Properties createCaptureDeviceProperties(String customSource, String codec, String bitrate, 
          String quantizer, String container, String bufferCount, String bufferBytes, String bufferTime, 
          String framerate){
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
  
  public static CaptureDevice createCaptureDevice(String sourceLocation, ProducerType sourceDeviceName, 
          String friendlyName, String outputLocation, Properties captureDeviceProperties) {
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
