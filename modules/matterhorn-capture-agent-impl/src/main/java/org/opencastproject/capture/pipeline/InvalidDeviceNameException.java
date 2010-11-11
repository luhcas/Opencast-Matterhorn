package org.opencastproject.capture.pipeline;

public class InvalidDeviceNameException extends Exception {
  private static final long serialVersionUID = 4821621152347933277L;
  public InvalidDeviceNameException(String message){
    super(message);
  }
}
