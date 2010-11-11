package org.opencastproject.capture.pipeline.bins;

import org.gstreamer.Element;

public class UnableToLinkGStreamerElementsException extends Exception {
  private static final long serialVersionUID = 159994156186562753L;
  String message; 
  
  public UnableToLinkGStreamerElementsException(CaptureDevice captureDevice, Element firstElement, Element secondElement){
    message = captureDevice.getFriendlyName() + " of type " + captureDevice.getName() + " could not link " + firstElement.getName() + " to " + secondElement.getName();
  }
  
  @Override
  public String getMessage(){
    return message;
  }
}
