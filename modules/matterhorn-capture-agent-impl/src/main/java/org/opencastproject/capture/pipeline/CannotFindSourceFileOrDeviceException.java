package org.opencastproject.capture.pipeline;

public class CannotFindSourceFileOrDeviceException extends Exception {
  private static final long serialVersionUID = -3758743529545374867L;
  public CannotFindSourceFileOrDeviceException(String message){
    super(message);
  }
}
