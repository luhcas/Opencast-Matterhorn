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
import org.opencastproject.caption.api.CaptionConverterException;
import org.opencastproject.caption.api.CaptionService;
import org.opencastproject.caption.api.UnsupportedCaptionFormatException;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;

import javax.activation.MimetypesFileTypeMap;

/**
 * Implementation of {@link CaptionService}. Uses {@link ComponentContext} to get all registered
 * {@link CaptionConverter}s. Converters are searched based on <code>caption.format</code> property. If there is no
 * match for specified input or output format {@link UnsupportedCaptionFormatException} is thrown.
 * 
 */
public class CaptionServiceImpl implements CaptionService {

  /** Logging utility */
  private static final Logger logger = LoggerFactory.getLogger(CaptionServiceImpl.class);

  /** The collection name */
  public static final String COLLECTION = "captions";

  /** Reference to workspace */
  private Workspace workspace;

  /** Reference to remote service manager */
  private ServiceRegistry remoteServiceManager;

  /** Component context needed for retrieving Converter Engines */
  protected ComponentContext componentContext = null;

  /**
   * Activate this service implementation via the OSGI service component runtime
   */
  public void activate(ComponentContext componentContext) {
    this.componentContext = componentContext;
  }

  /** Setter for workspace via declarative activation */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /** Setter for remote service manager via declarative activation */
  public void setRemoteServiceManager(ServiceRegistry manager) {
    this.remoteServiceManager = manager;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.caption.api.CaptionService#convert(org.opencastproject.mediapackage.MediaPackageElement,
   *      java.lang.String, java.lang.String)
   */
  @Override
  public Job convert(Catalog input, String inputFormat, String outputFormat)
          throws UnsupportedCaptionFormatException, CaptionConverterException {
    return convert(input, inputFormat, outputFormat, null);
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.opencastproject.caption.api.CaptionService#convert(org.opencastproject.mediapackage.MediaPackageElement,
   *      java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public Job convert(Catalog input, String inputFormat, String outputFormat, String language)
          throws UnsupportedCaptionFormatException, CaptionConverterException {

    Job receipt = remoteServiceManager.createJob(JOB_TYPE);

    if (inputFormat == null || outputFormat == null) {
      receipt.setStatus(Status.FAILED);
      remoteServiceManager.updateJob(receipt);
      throw new UnsupportedCaptionFormatException("<null>");
    }

    // get input file
    File captionsFile;
    try {
      captionsFile = workspace.get(input.getURI());
    } catch (NotFoundException e) {
      receipt.setStatus(Status.FAILED);
      remoteServiceManager.updateJob(receipt);
      throw new CaptionConverterException("Requested media package element " + input + " could not be found.");
    } catch (IOException e) {
      receipt.setStatus(Status.FAILED);
      remoteServiceManager.updateJob(receipt);
      throw new CaptionConverterException("Requested media package element " + input + "could not be accessed.");
    }

    logger.debug("Atempting to convert from {} to {}...", inputFormat, outputFormat);

    CaptionCollection collection;
    try {
      collection = importCaptions(captionsFile, inputFormat, language);
      logger.debug("Parsing to collection succeeded.");
    } catch (UnsupportedCaptionFormatException e) {
      receipt.setStatus(Status.FAILED);
      remoteServiceManager.updateJob(receipt);
      throw new UnsupportedCaptionFormatException(inputFormat);
    } catch (CaptionConverterException e) {
      receipt.setStatus(Status.FAILED);
      remoteServiceManager.updateJob(receipt);
      throw e;
    }

    URI exported;
    try {
      exported = exportCaptions(collection, FilenameUtils.getBaseName(captionsFile.getAbsolutePath()), outputFormat,
              language);
      logger.debug("Exporting captions succeeding.");
    } catch (UnsupportedCaptionFormatException e) {
      receipt.setStatus(Status.FAILED);
      remoteServiceManager.updateJob(receipt);
      throw new UnsupportedCaptionFormatException(outputFormat);
    } catch (IOException e) {
      receipt.setStatus(Status.FAILED);
      remoteServiceManager.updateJob(receipt);
      throw new CaptionConverterException("Could not export caption collection.", e);
    }

    // create catalog and set properties
    MediaPackageElementBuilder elementBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
    Catalog catalog = (Catalog) elementBuilder.elementFromURI(exported, Catalog.TYPE, new MediaPackageElementFlavor(
            "captions", outputFormat));
    String[] mimetype = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(exported.getPath()).split("/");
    catalog.setMimeType(new MimeType(mimetype[0], mimetype[1]));
    catalog.addTag("lang:" + language);

    receipt.setElement(catalog);
    receipt.setStatus(Status.FINISHED);
    remoteServiceManager.updateJob(receipt);

    return receipt;
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.opencastproject.caption.api.CaptionService#getLanguageList(org.opencastproject.mediapackage.MediaPackageElement,
   *      java.lang.String)
   */
  @Override
  public String[] getLanguageList(Catalog input, String format) throws UnsupportedCaptionFormatException,
          CaptionConverterException {

    if (format == null) {
      throw new UnsupportedCaptionFormatException("<null>");
    }
    CaptionConverter converter = getCaptionConverter(format);
    if (converter == null) {
      throw new UnsupportedCaptionFormatException(format);
    }

    File captions;
    try {
      captions = workspace.get(input.getURI());
    } catch (NotFoundException e) {
      throw new CaptionConverterException("Requested media package element " + input + " could not be found.");
    } catch (IOException e) {
      throw new CaptionConverterException("Requested media package element " + input + "could not be accessed.");
    }

    FileInputStream stream;
    try {
      stream = new FileInputStream(captions);
    } catch (FileNotFoundException e) {
      throw new CaptionConverterException("Requested file " + captions + "could not be found.");
    }

    String[] languageList = converter.getLanguageList(stream);

    try {
      stream.close();
    } catch (IOException e) {
      logger.warn("Could not close stream.");
    }

    return languageList == null ? new String[0] : languageList;
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
   *          file containing captions
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
  private CaptionCollection importCaptions(File input, String inputFormat, String language)
          throws UnsupportedCaptionFormatException, CaptionConverterException {
    // get input format
    CaptionConverter converter = getCaptionConverter(inputFormat);
    if (converter == null) {
      logger.error("No available caption format found for {}.", inputFormat);
      throw new UnsupportedCaptionFormatException(inputFormat);
    }

    FileInputStream fileStream;
    try {
      fileStream = new FileInputStream(input);
    } catch (FileNotFoundException e) {
      throw new CaptionConverterException("Could not locate file " + input);
    }
    CaptionCollection collection = converter.importCaption(fileStream, language);
    IOUtils.closeQuietly(fileStream);

    return collection;
  }

  /**
   * Exports {@link CaptionCollection} to specified format. Extension is added to exported file name. Throws
   * {@link UnsupportedCaptionFormatException} if format is not supported.
   * 
   * @param collection
   *          {@link CaptionCollection} to be exported
   * @param outputName
   *          name under which exported captions will be stored
   * @param outputFormat
   *          format of exported collection
   * @param language
   *          (optional) captions' language
   * @throws UnsupportedCaptionFormatException
   *           if there is no registered engine for given format
   * @return location of converted captions
   * @throws IOException
   *           if exception occurs while writing to output stream
   */
  private URI exportCaptions(CaptionCollection collection, String outputName, String outputFormat, String language)
          throws UnsupportedCaptionFormatException, IOException {
    CaptionConverter converter = getCaptionConverter(outputFormat);
    if (converter == null) {
      logger.error("No available caption format found for {}.", outputFormat);
      throw new UnsupportedCaptionFormatException(outputFormat);
    }

    // TODO instead of first writing it all in memory, write it directly to disk
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try {
      converter.exportCaption(outputStream, collection, language);
    } catch (IOException e) {
      // since we're writing to memory, this should not happen
    }
    ByteArrayInputStream in = new ByteArrayInputStream(outputStream.toByteArray());
    return workspace.putInCollection(COLLECTION, outputName + "." + converter.getExtension(), in);
  }
}
