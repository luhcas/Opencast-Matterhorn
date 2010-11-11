package org.opencastproject.capture.pipeline.bins.sources;

import java.util.Properties;

import org.opencastproject.capture.api.CaptureAgent;
import org.opencastproject.capture.pipeline.SourceDeviceName;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;

public class SourceFactory {
  
  private static SourceFactory factory;
  
  public static SourceFactory getInstance(){
    if(factory == null){
      factory = new SourceFactory();
    }
    return factory;
  }
  
  private SourceFactory(){
    
  }
  
  public SrcBin parseSourceBin(String bin){
    return null;
  }
  
  public SrcBin getSource(CaptureDevice captureDevice, Properties properties, CaptureAgent captureAgent) throws Exception {
    if (captureDevice.getName() == SourceDeviceName.EPIPHAN_VGA2USB)
      return new EpiphanVGA2USBV4LSrcBin(captureDevice, properties, captureAgent);
    else if (captureDevice.getName() == SourceDeviceName.HAUPPAUGE_WINTV)
      return new HauppaugeSrcBin(captureDevice, properties);
    else if (captureDevice.getName() == SourceDeviceName.FILE_DEVICE)
      return new FileSrcBin(captureDevice, properties);
    else if(captureDevice.getName() == SourceDeviceName.BLUECHERRY_PROVIDEO)
      return new BlueCherrySrcBin(captureDevice, properties);
    else if (captureDevice.getName() == SourceDeviceName.ALSASRC)
      return new AlsaSrcBin(captureDevice, properties);
    else if (captureDevice.getName() == SourceDeviceName.PULSESRC)
      return new PulseAudioSrcBin(captureDevice, properties);
    else if (captureDevice.getName() == SourceDeviceName.AUDIOTESTSRC)
      return new AudioTestSrcBin(captureDevice, properties);
    else if(captureDevice.getName() == SourceDeviceName.DV_1394)
      return new DV1394SrcBin(captureDevice, properties);
    else if(captureDevice.getName() == SourceDeviceName.VIDEOTESTSRC)
      return new VideoTestSrc(captureDevice, properties);
    else if(captureDevice.getName() == SourceDeviceName.V4LSRC)
      return new V4lsrcBin(captureDevice, properties);
    else if(captureDevice.getName() == SourceDeviceName.V4L2SRC)
      return new V4l2srcDeviceBin(captureDevice, properties);
    else if(captureDevice.getName() == SourceDeviceName.CUSTOM_VIDEO_SRC)
      return new CustomVideoSrcBin(captureDevice, properties); 
    else if(captureDevice.getName() == SourceDeviceName.CUSTOM_AUDIO_SRC)
      return new CustomAudioSrcBin(captureDevice, properties); 
    
    return null;
  }
}
