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
