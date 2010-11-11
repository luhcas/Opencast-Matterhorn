package org.opencastproject.capture.pipeline;

public class UnableToCreateSampleOutputFileException extends Exception {
  private static final long serialVersionUID = -7263863505984641712L;

  public UnableToCreateSampleOutputFileException(String message){
    super(message);
  }
}
