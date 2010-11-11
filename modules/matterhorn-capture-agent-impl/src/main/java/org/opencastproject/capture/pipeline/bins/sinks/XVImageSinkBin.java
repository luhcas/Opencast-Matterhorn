package org.opencastproject.capture.pipeline.bins.sinks;

import java.util.Properties;

import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;

public class XVImageSinkBin extends SinkBin {
  Element xvimagesink;
  
  public XVImageSinkBin(CaptureDevice captureDevice, Properties properties) throws Exception {
    super(captureDevice, properties);
  }

  @Override
  protected void createElements() {
    xvimagesink = ElementFactory.make("xvimagesink", null);
  }
  
  @Override
  protected void addElementsToBin(){
    bin.add(xvimagesink);
  }

  @Override
  protected void linkElements() {
  }

  @Override
  protected void setElementProperties() {
    
  }

  @Override
  public Element getSrc() {
    return xvimagesink;
  }
}
