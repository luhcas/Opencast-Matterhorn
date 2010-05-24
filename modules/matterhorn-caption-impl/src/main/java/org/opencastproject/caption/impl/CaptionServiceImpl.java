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
package org.opencastproject.caption.impl;

import org.opencastproject.caption.api.CaptionCollection;
import org.opencastproject.caption.api.CaptionConverter;
import org.opencastproject.caption.api.CaptionService;
import org.opencastproject.caption.api.IllegalCaptionFormatException;
import org.opencastproject.caption.api.UnsupportedCaptionFormatException;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of {@link CaptionService}. Uses {@link ComponentContext} to get all registered
 * {@link CaptionConverter}s. When trying to determine input format of all {@link CaptionConverter}s is traversed and
 * first regex match of specific format will be set as input format. If format is specified, only those
 * {@link CaptionConverter}s are searched that has <code>caption.format</code> property set to specified name. If none
 * is found or input cannot be determined {@link UnsupportedCaptionException} is thrown.
 * 
 */
public class CaptionServiceImpl implements CaptionService {

  // private static final String NEWLINE = System.getProperty("line.separator");
  private static final Logger logger = LoggerFactory.getLogger(CaptionServiceImpl.class);

  protected ComponentContext componentContext = null;

  // TODO add reference to working file repository if we will add files as well

  /**
   * Activate this service implementation via the OSGI service component runtime
   */
  public void activate(ComponentContext componentContext) {
    this.componentContext = componentContext;
  }

  /**
   * {@inheritDoc}
   * 
   * @throws IOException
   * @throws IllegalCaptionFormatException
   * @see org.opencastproject.caption.api.CaptionService#convert(java.io.InputStream, java.lang.String,
   *      java.lang.String)
   */
  @Override
  public String convert(String input, String inputFormat, String outputType) throws UnsupportedCaptionFormatException,
          IllegalCaptionFormatException {
    if (inputFormat == null || outputType == null) {
      // or just spit warning and find format?
      throw new UnsupportedCaptionFormatException("null");
    }

    // logger.info("Reading stream...");
    // String input = parseInputStream(is);

    logger.debug("Atempting to convert from {} to {}...", inputFormat, outputType);
    CaptionCollection collection;
    collection = importCaptions(input, inputFormat);
    logger.debug("Parsing to collection succeeded.");
    String output = exportCaptions(collection, outputType);
    logger.debug("Conversion succeeded.");
    return output;
  }

  /**
   * {@inheritDoc}
   * 
   * @throws IOException
   * @throws IllegalCaptionFormatException
   * 
   * @see org.opencastproject.caption.api.CaptionService#convert(java.io.InputStream, java.lang.String)
   */
  @Override
  public String convert(String input, String outputType) throws UnsupportedCaptionFormatException,
          IllegalCaptionFormatException {
    if (outputType == null) {
      throw new UnsupportedCaptionFormatException("null");
    }

    // logger.info("Reading stream...");
    // String input = parseInputStream(is);

    logger.debug("Atempting to convert to {}...", outputType);
    CaptionCollection collection;
    // directly import captions
    collection = importCaptions(input);
    logger.debug("Parsing to collection succeeded.");
    String output = exportCaptions(collection, outputType);
    logger.debug("Converting succeeded.");
    return output;
  }

  /**
   * Returns all registered {@link CaptionFormat}s.
   */
  protected HashMap<String, CaptionConverter> getAvailableCaptionConverters() {
    HashMap<String, CaptionConverter> captionConverters = new HashMap<String, CaptionConverter>();
    ServiceReference[] refs = null;
    try {
      refs = componentContext.getBundleContext().getServiceReferences(CaptionConverter.class.getName(), null);
    } catch (InvalidSyntaxException e) {
      // should not happen since it is called with null argument
    }

    if (refs != null) {
      for (ServiceReference ref : refs) {
        CaptionConverter converter = (CaptionConverter) componentContext.getBundleContext().getService(ref);
        String format = (String) ref.getProperty("caption.format");
        if (captionConverters.containsKey(format)) {
          logger.warn("Caption converter with format {} has already been registered. Ignoring second definition.",
                  format);
        } else {
          captionConverters.put((String) ref.getProperty("caption.format"), converter);
        }
      }
    }

    return captionConverters;
  }

  /**
   * Returns specific {@link CaptionConverter}. Registry is searched based on formatName, so in order for
   * {@link CaptionConverter} to be found, it has to have <code>caption.format</code> property set with
   * {@link CaptionConverter} format. If none is found, null is returned, if more than one is found then the first
   * reference is returned.
   * 
   * @param formatName
   *          name of the caption format
   * @return {@link CaptionConverter} or null if none is found
   */
  protected CaptionConverter getCaptionConverter(String formatName) {
    ServiceReference[] ref = null;
    try {
      ref = componentContext.getBundleContext().getServiceReferences(CaptionConverter.class.getName(),
              "(caption.format=" + formatName + ")");
    } catch (InvalidSyntaxException e) {
      throw new RuntimeException(e);
    }
    if (ref == null) {
      logger.warn("No caption format available for {}.", formatName);
      return null;
    }
    if (ref.length > 1)
      logger.warn("Multiple references for caption format {}! Returning first service reference.", formatName);
    CaptionConverter converter = (CaptionConverter) componentContext.getBundleContext().getService(ref[0]);
    return converter;
  }

  /**
   * Traverses through the {@link CaptionConverter}'s hash map to match input with every format's signature. Returns
   * first {@link CaptionConverter} whose signature is matched or null if there is no match.
   * 
   * @param input
   *          String representation of captions
   * @return corresponding {@link CaptionConverter} or <code>null</code> if format is not recognized
   */
  private CaptionConverter autoDetectCaptionFormat(String input) {

    HashMap<String, CaptionConverter> availableConverters = getAvailableCaptionConverters();

    for (CaptionConverter format : availableConverters.values()) {
      Pattern pattern = Pattern.compile(format.getIdPattern());
      Matcher matcher = pattern.matcher(input);
      if (matcher.find()) {
        // return first matching format
        return format;
      }
    }
    return null;
  }

  /**
   * Imports captions and returns {@link CaptionCollection}. If format cannot be determined from captions,
   * {@link UnsupportedCaptionFormatException} is thrown. If there is an exception while parsing caption string,
   * {@link IllegalCaptionFormatException} is thrown.
   * 
   * @param input
   *          String representation of captions
   * @return {@link CaptionCollection} of captions
   * @throws UnsupportedCaptionFormatException
   *           if input format cannot be determined
   * @throws IllegalCaptionFormatException
   *           if exception occurs while parsing captions
   */
  private CaptionCollection importCaptions(String input) throws UnsupportedCaptionFormatException,
          IllegalCaptionFormatException {
    logger.debug("Atempting to recognise input format...");
    CaptionConverter converter = autoDetectCaptionFormat(input);
    if (converter == null) {
      throw new UnsupportedCaptionFormatException("null");
    }
    logger.debug("Input format recognised as {}.", converter.getName());
    return converter.importCaption(input);
  }

  /**
   * Imports captions with specified format. Throws {@link UnsupportedCaptionFormatException} if format is not supported
   * or {@link IllegalCaptionFormatException} if captions fail regex check with format id pattern.
   * 
   * @param input
   *          String representation of captions
   * @param inputFormat
   *          captions format
   * @return {@link CaptionCollection} of captions
   * @throws UnsupportedCaptionFormatException
   *           if format is not supported
   * @throws IllegalCaptionFormatException
   *           if captions fail regex check for specified format
   */
  private CaptionCollection importCaptions(String input, String inputFormat) throws UnsupportedCaptionFormatException,
          IllegalCaptionFormatException {
    // get input format
    CaptionConverter converter = getCaptionConverter(inputFormat);
    if (converter == null) {
      logger.error("No available caption format found for {}.", inputFormat);
      throw new UnsupportedCaptionFormatException(inputFormat);
    }
    // check for pattern matching between format and input
    if (!Pattern.compile(converter.getIdPattern()).matcher(input).find()) {
      logger.error("Captions do not match format pattern {}.", converter.getIdPattern());
      throw new IllegalCaptionFormatException("Input does not match format pattern for " + converter.getName());
    }
    return converter.importCaption(input);
  }

  /**
   * Exports {@link CaptionCollection} to specified format. Throws {@link UnsupportedCaptionFormatException} if format
   * is not supported.
   * 
   * @param collection
   *          {@link CaptionCollection} to be exported
   * @param outputFormat
   *          format of exported collection
   * @return String representation of exported collection
   * @throws UnsupportedCaptionFormatException
   *           if format is not supported
   */
  private String exportCaptions(CaptionCollection collection, String outputFormat)
          throws UnsupportedCaptionFormatException {
    CaptionConverter converter = getCaptionConverter(outputFormat);
    if (converter == null) {
      logger.error("No available caption format found for {}.", outputFormat);
      throw new UnsupportedCaptionFormatException("Unknown caption format: " + outputFormat);
    }
    return converter.exportCaption(collection);
  }

  // private String parseInputStream(InputStream is) throws IOException {
  // if (is != null) {
  // initialize StringBuffer
  // StringBuffer buffer = new StringBuffer();
  // String line;
  // BufferedReader reader = new BufferedReader(new InputStreamReader(is));
  // try {
  // while ((line = reader.readLine()) != null) {
  // buffer.append(line).append(NEWLINE);
  // }
  // reader.close();
  // } catch (IOException e) {
  // throw e;
  // } finally {
  // reader.close();
  // }
  // return buffer.toString();
  // } else {
  // return "";
  // }
  // }
}
