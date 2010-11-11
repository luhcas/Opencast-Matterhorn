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
  
  public SinkBin getSink(SinkDeviceName sinkDeviceName, CaptureDevice captureDevice, Properties properties) throws Exception {
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
