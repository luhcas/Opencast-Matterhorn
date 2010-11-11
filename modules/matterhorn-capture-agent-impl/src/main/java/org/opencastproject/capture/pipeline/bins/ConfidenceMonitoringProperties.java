package org.opencastproject.capture.pipeline.bins;

import java.io.File;
import java.util.Properties;

import org.opencastproject.capture.api.CaptureParameters;

public class ConfidenceMonitoringProperties {
  public String imageloc;
  public String device;
  public int interval;
  public int monitoringLength;
  
  public ConfidenceMonitoringProperties(CaptureDevice captureDevice, Properties properties){
    if(properties != null){
      imageloc = properties.getProperty(CaptureParameters.CAPTURE_CONFIDENCE_VIDEO_LOCATION);
      device = new File(captureDevice.getOutputPath()).getName();
      interval = Integer.parseInt(properties.getProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + captureDevice.getFriendlyName() + CaptureParameters.CAPTURE_DEVICE_CONFIDENCE_INTERVAL, "5"));
      monitoringLength = Integer.parseInt(properties.getProperty(CaptureParameters.CAPTURE_CONFIDENCE_AUDIO_LENGTH, "60"));
    }
  }
}
