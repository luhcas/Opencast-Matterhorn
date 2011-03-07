package org.opencastproject.series.api;

public class SeriesException extends Exception {

  /**
   * The UID for java serialization
   */
  private static final long serialVersionUID = 3988901471834949666L;

  /**
   * Build a new series exception with a message and an original cause.
   * 
   * @param message
   *          the error message
   * @param cause
   *          the original exception causing this series exception to be thrown
   */
  public SeriesException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Build a new series exception from the original cause.
   * 
   * @param cause
   *          the original exception causing this series exception to be thrown
   */
  public SeriesException(Throwable cause) {
    super(cause);
  }

  /**
   * Build a new series exception with a message
   * 
   * @param message
   *          the error message
   */
  public SeriesException(String message) {
    super(message);
  }
}
