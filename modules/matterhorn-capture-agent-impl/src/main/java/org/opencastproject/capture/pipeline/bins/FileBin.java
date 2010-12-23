/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.capture.pipeline.bins;

import org.gstreamer.Element;

import java.io.File;
import java.util.Properties;

/**
 * TODO: Comment me!
 */
public class FileBin extends PartialBin {
  
  private Element filesrc;
  private Element filesink;

  /**
   * Adds a pipeline for a media file that just copies it to a new location
   * 
   * @param captureDevice
   *          The {@code CaptureDevice} with source and output information
   * @param pipeline
   *          The {@code Pipeline} bin to add it to
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If either filesrc or filesink is null when trying to set the location and output path this is thrown.
   * @throws UnableToCreateGhostPadsForBinException
   *           Shouldn't be thrown by this class.
   * @throws UnableToLinkGStreamerElementsException
   *           If the filesrc cannot be linked to the filesink then this Exception is thrown.
   * @throws CaptureDeviceNullPointerException
   *           The parameter captureDevice is required for the location and output path.
   * @throws UnableToCreateElementException
   *           If the filesink or filesrc cannot be created this Exception is thrown.
   */
  public FileBin(CaptureDevice captureDevice, Properties properties) throws UnableToLinkGStreamerElementsException,
          UnableToCreateGhostPadsForBinException, UnableToSetElementPropertyBecauseElementWasNullException,
          CaptureDeviceNullPointerException, UnableToCreateElementException {
    super(captureDevice, properties);
  }

  /**
   * Creates the filesrc and filesink.
   * 
   * @throws UnableToCreateElementException
   *           Thrown if either filesrc or filesink cannot be created.
   * **/
  @Override
  protected void createElements() throws UnableToCreateElementException {
    filesrc = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.FILESRC, null);
    filesink = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.FILESINK, null);
  }

  /**
   * Sets the location of the filesrc and the output path of the filesink.
   * 
   * @throws UnableToCreateElementException
   *           Thrown if either the filesrc or filesink is null.
   * @throws IllegalArgumentException
   *           Thrown if cannot read from filesrc location or write to filesink outputPath
   **/
  @Override
  protected synchronized void setElementProperties() throws UnableToSetElementPropertyBecauseElementWasNullException {
    if (filesrc == null) {
      throw new UnableToSetElementPropertyBecauseElementWasNullException(filesrc, captureDevice.getLocation());
    } else if (filesink == null) {
      throw new UnableToSetElementPropertyBecauseElementWasNullException(filesink, captureDevice.getOutputPath());
    }
    if (!new File(captureDevice.getLocation()).canRead()) {
      throw new IllegalArgumentException(captureDevice.getFriendlyName() + " cannot read from "
              + captureDevice.getLocation());
    }
    if (!new File(captureDevice.getOutputPath()).canWrite()) {
      throw new IllegalArgumentException(captureDevice.getFriendlyName() + " cannot write to "
              + captureDevice.getOutputPath());
    }
    filesrc.set(GStreamerProperties.LOCATION, captureDevice.getLocation());
    filesink.set(GStreamerProperties.LOCATION, captureDevice.getOutputPath());
  }

  /** Add filesrc and filesink to the Bin. **/
  @Override
  protected void addElementsToBin() {
    bin.addMany(filesrc, filesink);
  }

  /**
   * Link filesrc to filesink.
   * 
   * @throws UnableToLinkGStreamerElementsException
   *           Thrown if unable to link filesrc to filesink.
   * **/
  @Override
  protected void linkElements() throws UnableToLinkGStreamerElementsException {
    if (!filesrc.link(filesink))
      throw new UnableToLinkGStreamerElementsException(captureDevice, filesrc, filesink);
  }

  /** An empty createGhostPads prevents the default createGhostPads from running. **/
  @Override
  protected void createGhostPads() {

  }
}
