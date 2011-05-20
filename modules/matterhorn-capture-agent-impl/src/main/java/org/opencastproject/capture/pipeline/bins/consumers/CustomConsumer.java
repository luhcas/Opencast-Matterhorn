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

package org.opencastproject.capture.pipeline.bins.consumers;

import org.opencastproject.capture.impl.XProperties;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceNullPointerException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateGhostPadsForBinException;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;
import org.opencastproject.capture.pipeline.bins.UnableToSetElementPropertyBecauseElementWasNullException;

import org.apache.commons.lang.StringUtils;
import org.gstreamer.Bin;
import org.gstreamer.Element;
import org.gstreamer.GstException;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomConsumer extends ConsumerBin {

  public static final String FRIENDLY_NAME = "friendlyName";
  public static final String OUTPUT_PATH = "outputPath";
  public static final String LOCATION = "location";
  public static final String TYPE = "type";
  
  /** Determines whether to create ghostpads for the bin or just leave the pads as they are. **/
  private static final boolean LINK_UNUSED_GHOST_PADS = true;
  
  /**
   * CustomConsumer allows the user to specify at run time a custom gstreamer pipeline that will act as the consumer for
   * the Capture Device.
   * 
   * @throws UnableToLinkGStreamerElementsException
   *           Not actually thrown by this class, just inherited.
   * @throws UnableToCreateGhostPadsForBinException
   *           Not actually thrown by this class, just inherited.
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           Not actually thrown by this class, just inherited.
   * @throws CaptureDeviceNullPointerException
   *           The captureDevice parameter is required to create this custom Producer so an Exception is thrown when it
   *           is null.
   * @throws UnableToCreateElementException
   *           Thrown if the pipeline specified to be used in customConsumer string in the properties file is null or
   *           causes a gstreamer exception.
   **/
  public CustomConsumer(CaptureDevice captureDevice, Properties properties)
          throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException,
          UnableToCreateElementException {
    super(captureDevice, properties);

  }

  /**
   * This goes through the customConsumer property provided by the user and checks for any properties that they may wish
   * substituted by ConfigurationManager properties such as ${capture.filesystem.cache.capture.url} would be replaced by
   * the actual location of the capture cache.
   * 
   * @param customString
   *          The customConsumer= string from the ConfigurationManager properties.
   * @return The customString with all of the ${property} it was able to substitute.
   */
  public String replacePropertiesInCustomConsumerString(String customString) {
    if (customString != null) {
      XProperties allProperties = getAllCustomStringSubstitutions();
      // Find all properties defined by ${someProperty} looking specifically for 1$ followed by 1{ then 1 or more not }
      // and finally 1 }
      String regEx = "\\$\\{[^\\}]+\\}";
      String workingString = new String(customString);
      Pattern pattern = Pattern.compile(regEx);
      Matcher matcher = pattern.matcher(workingString);
      while (matcher.find()) {
        String propertyKey = matcher.group();
        // Strip off the ${} from the actual property key.
        String strippedPropertyKey = propertyKey.substring(2, propertyKey.length() - 1);
        if (strippedPropertyKey != null && !StringUtils.isEmpty(strippedPropertyKey) && allProperties.get(strippedPropertyKey) != null) {
          // Get the property from the XProperties collection
          String result = allProperties.get(strippedPropertyKey).toString();
          // Replace the key with the value.
          workingString = workingString.replace(propertyKey, result);
        }
      }
      return workingString;
    } else {
      return null;
    }
  }

  /**
   * Adds the friendly name, output path, location of the capture device and type to the properties collection.
   * 
   * @return Returns an XProperties that contains all of the XProperties from ConfigurationManager and the mentioned
   *         properties above.
   **/
  private XProperties getAllCustomStringSubstitutions() {
    XProperties allProperties = new XProperties();
    allProperties.putAll(properties);
    allProperties.put(FRIENDLY_NAME, captureDevice.getFriendlyName());
    allProperties.put(OUTPUT_PATH, captureDevice.getOutputPath());
    allProperties.put(LOCATION, captureDevice.getLocation());
    allProperties.put(TYPE, captureDevice.getName());
    return allProperties;
  }

  @Override
  public Element getSrc() {
    return getBin();
  }

  /**
   * Creates the Bin for this class using the GStreamer Java Bin.launch command. Users can specify an Consumer in
   * this way using a gst-launch like syntax (e.g. "fakesrc ! fakesink")
   * @throws UnableToCreateElementException 
   **/
  @Override
  protected void createElements() throws UnableToCreateElementException {
    super.createElements();
    String customConsumer = replacePropertiesInCustomConsumerString(captureDeviceProperties.getCustomConsumer());
    logger.info("Custom Consumer is going to use Pipeline: \"" + customConsumer + "\"");
    if (captureDeviceProperties.getCustomConsumer() == null) {
      throw new UnableToCreateElementException(captureDevice.getFriendlyName(), "Custom Consumer because it was null.");
    }
    try {
      bin = Bin.launch(customConsumer, LINK_UNUSED_GHOST_PADS);
    } catch (GstException exception) {
      throw new UnableToCreateElementException(captureDevice.getFriendlyName(), "Custom Consumer had exception " + exception.getMessage());
    }
    
    
  }

  /** Need an empty method for createGhostPads because the Bin.launch will create the ghost pads all on its own. **/
  @Override
  protected void createGhostPads() throws UnableToCreateGhostPadsForBinException {
  }
}
