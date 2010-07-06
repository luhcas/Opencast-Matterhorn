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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;

/**
 * Implementation of {@link CaptionService}. Uses {@link ComponentContext} to get all registered
 * {@link CaptionConverter}s. Converters are searched based on <code>caption.format</code> property. If there is no
 * match for specified input or output format {@link UnsupportedCaptionFormatException} is thrown.
 * 
 */
public class CaptionServiceImpl implements CaptionService {

  /** Logging utility */
  private static final Logger logger = LoggerFactory.getLogger(CaptionServiceImpl.class);

  /** Component context needed for retrieving Converter Engines */
  protected ComponentContext componentContext = null;

  /**
   * Activate this service implementation via the OSGI service component runtime
   */
  public void activate(ComponentContext componentContext) {
    this.componentContext = componentContext;
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.opencastproject.caption.api.CaptionService#convert(java.io.InputStream, java.lang.String,
   *      java.io.OutputStream, java.lang.String, java.lang.String)
   */
  @Override
  public void convert(InputStream input, String inputFormat, OutputStream output, String outputFormat, String language)
          throws UnsupportedCaptionFormatException, IllegalCaptionFormatException, IOException {
    if (inputFormat == null || outputFormat == null) {
      // or just spit warning and find format?
      throw new UnsupportedCaptionFormatException("<null>");
    }

    // TODO sanitize language

    logger.debug("Atempting to convert from {} to {}...", inputFormat, outputFormat);
    CaptionCollection collection;
    // FIXME perform language check
    collection = importCaptions(input, inputFormat, language);
    logger.debug("Parsing to collection succeeded.");
    exportCaptions(collection, output, outputFormat, language);
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.opencastproject.caption.api.CaptionService#getLanguageList(java.io.InputStream, java.lang.String)
   */
  @Override
  public List<String> getLanguageList(InputStream input, String format) throws UnsupportedCaptionFormatException,
          IllegalCaptionFormatException {

    if (format == null) {
      throw new UnsupportedCaptionFormatException("<null>");
    }
    CaptionConverter converter = getCaptionConverter(format);
    if (converter == null) {
      throw new UnsupportedCaptionFormatException(format);
    }
    return converter.getLanguageList(input);
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
   * Imports captions using registered converter engine and specified language.
   * 
   * @param input
   *          stream from which captions are imported
   * @param inputFormat
   *          format of imported captions
   * @param language
   *          (optional) captions' language
   * @return {@link CaptionCollection} of parsed captions
   * @throws UnsupportedCaptionFormatException
   *           if there is no registered engine for given format
   * @throws IllegalCaptionFormatException
   *           if parser encounters exception
   */
  private CaptionCollection importCaptions(InputStream input, String inputFormat, String language)
          throws UnsupportedCaptionFormatException, IllegalCaptionFormatException {
    // get input format
    CaptionConverter converter = getCaptionConverter(inputFormat);
    if (converter == null) {
      logger.error("No available caption format found for {}.", inputFormat);
      throw new UnsupportedCaptionFormatException(inputFormat);
    }
    // TODO check if collection is empty
    return converter.importCaption(input, language);
  }

  /**
   * Exports {@link CaptionCollection} to specified format. Throws {@link UnsupportedCaptionFormatException} if format
   * is not supported.
   * 
   * @param collection
   *          {@link CaptionCollection} to be exported
   * @param output
   *          where to export caption collection
   * @param outputFormat
   *          format of exported collection
   * @param language
   *          (optional) captions' language
   * @throws UnsupportedCaptionFormatException
   *           if there is no registered engine for given format
   * @throws IOException
   *           if exception occurs while writing to output stream
   */
  private void exportCaptions(CaptionCollection collection, OutputStream output, String outputFormat, String language)
          throws UnsupportedCaptionFormatException, IOException {
    CaptionConverter converter = getCaptionConverter(outputFormat);
    if (converter == null) {
      logger.error("No available caption format found for {}.", outputFormat);
      throw new UnsupportedCaptionFormatException(outputFormat);
    }

    converter.exportCaption(output, collection, language);
  }
}
