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
package org.opencastproject.capture.pipeline.bins.sinks;

import java.util.Properties;

import org.opencastproject.capture.pipeline.SinkDeviceName;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;

public class SinkFactory {
  
  private static SinkFactory factory;
  
  public static SinkFactory getInstance(){
    if(factory == null){
      factory = new SinkFactory();
    }
    return factory;
  }
  
  private SinkFactory(){
    
  }
  
  public SinkBin parseSinkBin(String bin){
    return null;
  }
  
  public SinkBin getSink(SinkDeviceName sinkDeviceName, CaptureDevice captureDevice, Properties properties)
          throws Exception {
    if (sinkDeviceName == SinkDeviceName.AUDIO_FILE_SINK)
      return new AudioFileSinkBin(captureDevice, properties);
    else if(sinkDeviceName == SinkDeviceName.XVIMAGESINK)
      return new XVImageSinkBin(captureDevice, properties);
    else if(sinkDeviceName == SinkDeviceName.VIDEO_FILE_SINK)
      return new VideoFileSinkBin(captureDevice, properties);
    else{
      Exception e = new Exception("No valid SinkBin found for device " + sinkDeviceName);
      throw e;
    }
  }
}
