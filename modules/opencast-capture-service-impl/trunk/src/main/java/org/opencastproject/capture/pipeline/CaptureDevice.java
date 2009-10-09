package org.opencastproject.capture.pipeline;

/**
 * Simple representation of the location, name, capture type and output file
 * of a capture device. 
 */
public class CaptureDevice {


  /** String representation of absolute path to device file */
  private String location;

  /** Device name */
  private DeviceName name;

  /** Name of the file to save the stream to */
  private String outputPath;

  /**
   * Create a representation of a capture device for the PipelineFactory
   * @param loc The location of the device on the system
   * @param ct  The CaptureType of the device
   * @param name  The name of the device
   */
  public CaptureDevice(String loc, DeviceName name, String output) {
    this.location = loc;
    this.name = name;
    this.outputPath = output;

  }

  public DeviceName getName() {
    return name;
  }

  public String getLocation() {
    return location;
  }

  public String getOutputPath() {
    return outputPath;
  }

  public String toString() {
    return "[" + name + ", " + location  + ": " + outputPath + "]";
  }

}
