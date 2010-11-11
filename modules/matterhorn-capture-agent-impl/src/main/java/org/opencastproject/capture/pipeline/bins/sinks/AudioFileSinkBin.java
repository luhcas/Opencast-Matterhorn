package org.opencastproject.capture.pipeline.bins.sinks;

import java.util.Properties;

import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;

public class AudioFileSinkBin extends SinkBin {

  public static final String DEFAULT_ENCODER = "twolame";
  public static final String DEFAULT_MUXER = "capsfilter";
  
  public AudioFileSinkBin(CaptureDevice captureDevice, Properties properties) throws Exception {
    super(captureDevice, properties);
  }

  @Override
  protected void createElements(){
    super.createElements();
    createEncoder();
    createMuxer();
  }
  
  @Override
  protected void addElementsToBin(){
    bin.addMany(queue, encoder, muxer, filesink); 
  }
  
  protected void createEncoder(){
    if (captureDeviceProperties.codec != null) {
      logger.debug("{} encoder set to: {}", captureDevice.getName(), captureDeviceProperties.codec);
      encoder = ElementFactory.make(captureDeviceProperties.codec, null);
    }
    else {
      encoder = ElementFactory.make(DEFAULT_ENCODER, null);
    }
    
    if (captureDeviceProperties.bitrate != null) {
      logger.debug("{} bitrate set to: {}", captureDevice.getName(), captureDeviceProperties.bitrate);
      encoder.set("bitrate", captureDeviceProperties.bitrate);
    }
  }
  
  private void createMuxer() {
    if (captureDeviceProperties.codec != null) {
      if (captureDeviceProperties.codec.equalsIgnoreCase("faac"))
        muxer = ElementFactory.make("mp4mux", null);
      else
        muxer = ElementFactory.make(DEFAULT_MUXER, null);
    }
    else {
      muxer = ElementFactory.make(DEFAULT_MUXER, null);
    }
    
    if (captureDeviceProperties.container != null) {
      logger.debug("{} muxing to: {}", captureDevice.getName(), captureDeviceProperties.container);
      muxer = ElementFactory.make(captureDeviceProperties.container, null);
    }
  }
  
  @Override
  protected void setElementProperties() throws Exception{
    super.setElementProperties();
    if(!captureDevice.getOutputPath().equals("")){
      filesink.set("location", captureDevice.getOutputPath());
    }
    else{
      throw new Exception("File location must be set, it cannot be an empty String.");
    }
  }
  
  @Override
  protected void linkElements() throws Exception {
    if (!queue.link(encoder))
      throw new UnableToLinkGStreamerElementsException(captureDevice, queue, encoder);
    else if (!encoder.link(muxer))
      throw new UnableToLinkGStreamerElementsException(captureDevice, encoder, muxer);
    else if (!muxer.link(filesink))
      throw new UnableToLinkGStreamerElementsException(captureDevice, muxer, filesink);
  }
  
  @Override
  public Element getSrc(){
    return queue;
  }
}
