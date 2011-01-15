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
package org.opencastproject.textanalyzer.impl;

import org.opencastproject.dictionary.api.DictionaryService;
import org.opencastproject.dictionary.api.DictionaryService.DICT_TOKEN;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.metadata.mpeg7.MediaTime;
import org.opencastproject.metadata.mpeg7.MediaTimeImpl;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogImpl;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogService;
import org.opencastproject.metadata.mpeg7.SpatioTemporalDecomposition;
import org.opencastproject.metadata.mpeg7.TemporalDecomposition;
import org.opencastproject.metadata.mpeg7.Textual;
import org.opencastproject.metadata.mpeg7.TextualImpl;
import org.opencastproject.metadata.mpeg7.Video;
import org.opencastproject.metadata.mpeg7.VideoSegment;
import org.opencastproject.metadata.mpeg7.VideoText;
import org.opencastproject.metadata.mpeg7.VideoTextImpl;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.serviceregistry.api.ServiceUnavailableException;
import org.opencastproject.textanalyzer.api.TextAnalyzerException;
import org.opencastproject.textanalyzer.api.TextAnalyzerService;
import org.opencastproject.textanalyzer.impl.ocropus.OcropusLine;
import org.opencastproject.textanalyzer.impl.ocropus.OcropusTextAnalyzer;
import org.opencastproject.textanalyzer.impl.ocropus.OcropusTextFrame;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Media analysis service that takes takes an image and returns text as extracted from that image.
 */
public class TextAnalyzerServiceImpl implements TextAnalyzerService, JobProducer {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(TextAnalyzerServiceImpl.class);

  /** List of available operations on jobs */
  private enum Operation {
    Extract
  };

  /** Resulting collection in the working file repository */
  public static final String COLLECTION_ID = "ocrtext";

  /** Reference to the receipt service */
  private ServiceRegistry remoteServiceManager = null;

  /** The workspace to ue when retrieving remote media files */
  private Workspace workspace = null;

  /** The mpeg-7 service */
  protected Mpeg7CatalogService mpeg7CatalogService;

  /** The dictionary service */
  protected DictionaryService dictionaryService;

  /** Path to the ocropus binary */
  private String ocropusbinary = OcropusTextAnalyzer.OCROPUS_BINARY_DEFAULT;

  protected void activate(ComponentContext cc) {
    if (cc.getBundleContext().getProperty("org.opencastproject.textanalyzer.ocrocmd") != null)
      ocropusbinary = (String) cc.getBundleContext().getProperty("org.opencastproject.textanalyzer.ocrocmd");
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.textanalyzer.api.TextAnalyzerService#extract(org.opencastproject.mediapackage.Attachment)
   */
  @Override
  public Job extract(Attachment image) throws TextAnalyzerException, MediaPackageException {
    try {
      return remoteServiceManager.createJob(JOB_TYPE, Operation.Extract.toString(), Arrays.asList(MediaPackageElementParser.getAsXml(image)));
    } catch (ServiceUnavailableException e) {
      throw new TextAnalyzerException("No service of type '" + JOB_TYPE + "' available", e);
    } catch (ServiceRegistryException e) {
      throw new TextAnalyzerException("Unable to create job", e);
    }
  }

  /**
   * Starts text extraction on the image and returns a receipt containing the final result in the form of an
   * Mpeg7Catalog.
   * 
   * @param image
   *          the element to analyze
   * @param block
   *          <code>true</code> to make this operation synchronous
   * @return a receipt containing the resulting mpeg-7 catalog
   * @throws TextAnalyzerException
   */
  @SuppressWarnings("unchecked")
  private Catalog extract(Job job, Attachment image) throws TextAnalyzerException, MediaPackageException {

    final Attachment attachment = (Attachment) image;
    final URI imageUrl = attachment.getURI();

    try {
      job.setStatus(Status.RUNNING);
      updateJob(job);

      Mpeg7CatalogImpl mpeg7 = Mpeg7CatalogImpl.newInstance();

      logger.info("Starting text extraction from {}", imageUrl);

      File imageFile;
      try {
        imageFile = workspace.get(imageUrl);
      } catch (NotFoundException e) {
        throw new TextAnalyzerException("Image " + imageUrl + " not found in workspace", e);
      } catch (IOException e) {
        throw new TextAnalyzerException("Unable to access " + imageUrl + " in workspace", e);
      }
      VideoText[] videoTexts = analyze(imageFile, image.getIdentifier());

      // Create a temporal decomposition
      MediaTime mediaTime = new MediaTimeImpl(0, 0);
      Video avContent = mpeg7.addVideoContent(image.getIdentifier(), mediaTime, null);
      TemporalDecomposition<VideoSegment> temporalDecomposition = (TemporalDecomposition<VideoSegment>) avContent
              .getTemporalDecomposition();

      // Add a segment
      VideoSegment videoSegment = temporalDecomposition.createSegment("segment-0");
      videoSegment.setMediaTime(mediaTime);

      // Add the video text to the spacio temporal decomposition of the segment
      SpatioTemporalDecomposition spatioTemporalDecomposition = videoSegment.createSpatioTemporalDecomposition(true,
              false);
      for (VideoText videoText : videoTexts) {
        spatioTemporalDecomposition.addVideoText(videoText);
      }

      logger.info("Text extraction of {} finished, {} lines found", attachment.getURI(), videoTexts.length);

      URI uri;
      try {
        uri = workspace.putInCollection(COLLECTION_ID, job.getId() + ".xml", mpeg7CatalogService.serialize(mpeg7));
      } catch (IOException e) {
        throw new TextAnalyzerException("Unable to put mpeg7 into the workspace", e);
      }
      Catalog catalog = (Catalog) MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
              .newElement(Catalog.TYPE, MediaPackageElements.TEXTS);
      catalog.setURI(uri);

      job.setPayload(MediaPackageElementParser.getAsXml(catalog));
      job.setStatus(Status.FINISHED);
      updateJob(job);

      logger.info("Finished text extraction of {}", imageUrl);

      return catalog;
    } catch (Exception e) {
      logger.warn("Error extracting text from " + imageUrl, e);
      try {
        job.setStatus(Status.FAILED);
        updateJob(job);
      } catch (Exception failureToFail) {
        logger.warn("Unable to update job to failed state", failureToFail);
      }
      if (e instanceof TextAnalyzerException) {
        throw (TextAnalyzerException) e;
      } else {
        throw new TextAnalyzerException(e);
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JobProducer#startJob(org.opencastproject.job.api.Job, java.lang.String,
   *      java.util.List)
   */
  @Override
  public void startJob(Job job, String operation, List<String> arguments) throws ServiceRegistryException {
    Operation op = null;
    try {
      op = Operation.valueOf(operation);
      switch (op) {
        case Extract:
          Attachment element = (Attachment) MediaPackageElementParser.getFromXml(arguments.get(0));
          extract(job, element);
          break;
        default:
          throw new IllegalStateException("Don't know how to handle operation '" + operation + "'");
      }
    } catch (IllegalArgumentException e) {
      throw new ServiceRegistryException("This service can't handle operations of type '" + op + "'");
    } catch (IndexOutOfBoundsException e) {
      throw new ServiceRegistryException("This argument list for operation '" + op + "' does not meet expectations");
    } catch (Exception e) {
      throw new ServiceRegistryException("Error handling operation '" + op + "'");
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JobProducer#getJob(long)
   */
  public Job getJob(long id) throws NotFoundException, ServiceRegistryException {
    return remoteServiceManager.getJob(id);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.job.api.JobProducer#getJobType()
   */
  @Override
  public String getJobType() {
    return JOB_TYPE;
  }
  
  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JobProducer#countJobs(org.opencastproject.job.api.Job.Status)
   */
  public long countJobs(Status status) throws ServiceRegistryException {
    if (status == null)
      throw new IllegalArgumentException("status must not be null");
    return remoteServiceManager.count(JOB_TYPE, status);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JobProducer#countJobs(org.opencastproject.job.api.Job.Status, java.lang.String)
   */
  public long countJobs(Status status, String host) throws ServiceRegistryException {
    if (status == null)
      throw new IllegalArgumentException("status must not be null");
    if (host == null)
      throw new IllegalArgumentException("host must not be null");
    return remoteServiceManager.count(JOB_TYPE, status, host);
  }

  /**
   * Returns the video text element for the given image.
   * 
   * @param imageFile
   *          the image
   * @param id
   *          the video text id
   * @return the video text found on the image
   * @throws IOException
   *           if accessing the image fails
   */
  protected VideoText[] analyze(File imageFile, String id) throws TextAnalyzerException {
    boolean languagesInstalled;
    if (dictionaryService.getLanguages().length == 0) {
      languagesInstalled = false;
      logger.warn("There are no language packs installed.  All text extracted from video will be considered valid.");
    } else {
      languagesInstalled = true;
    }

    List<VideoText> videoTexts = new ArrayList<VideoText>();
    OcropusTextAnalyzer analyzer = new OcropusTextAnalyzer(ocropusbinary);
    OcropusTextFrame textFrame = analyzer.analyze(imageFile);
    int i = 1;
    for (OcropusLine line : textFrame.getLines()) {
      VideoText videoText = new VideoTextImpl(id + "-" + i++);
      videoText.setBoundary(line.getBoundaries());
      Textual text = null;
      if (languagesInstalled) {
        String[] potentialWords = StringUtils.split(line.getText());
        String[] languages = dictionaryService.detectLanguage(potentialWords);
        if (languages.length == 0) {
          StringBuilder potentialWordsBuilder = new StringBuilder();
          for (int j = 0; j < potentialWords.length; j++) {
            if (potentialWordsBuilder.length() > 0) {
              potentialWordsBuilder.append(" ");
            }
            potentialWordsBuilder.append(potentialWords[j]);
          }
          logger.warn(
                  "Unable to determine the language for these words: '{}'.  Perhaps the language pack(s) are missing.",
                  potentialWordsBuilder.toString());
          text = new TextualImpl(line.getText());
        } else {
          String language = languages[0];
          DICT_TOKEN[] tokens = dictionaryService.cleanText(potentialWords, language);
          StringBuilder cleanLine = new StringBuilder();
          for (int j = 0; j < potentialWords.length; j++) {
            if (tokens[j] == DICT_TOKEN.WORD) {
              if (cleanLine.length() > 0) {
                cleanLine.append(" ");
              }
              cleanLine.append(potentialWords[j]);
            }
          }
          // TODO: Ensure that the language returned by the dictionary is compatible with the MPEG-7 schema
          text = new TextualImpl(cleanLine.toString(), language);
        }
      } else {
        text = new TextualImpl(line.getText());
      }
      videoText.setText(text);
      videoTexts.add(videoText);
    }
    return videoTexts.toArray(new VideoText[videoTexts.size()]);
  }

  /**
   * Sets the receipt service
   * 
   * @param remoteServiceManager
   *          the receipt service
   */
  public void setRemoteServiceManager(ServiceRegistry remoteServiceManager) {
    this.remoteServiceManager = remoteServiceManager;
  }

  /**
   * Sets the workspace
   * 
   * @param workspace
   *          an instance of the workspace
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * Sets the mpeg7CatalogService
   * 
   * @param mpeg7CatalogService
   *          an instance of the mpeg7 catalog service
   */
  public void setMpeg7CatalogService(Mpeg7CatalogService mpeg7CatalogService) {
    this.mpeg7CatalogService = mpeg7CatalogService;
  }

  /**
   * Sets the dictionary service
   * 
   * @param dictionaryService
   *          an instance of the dicitonary service
   */
  public void setDictionaryService(DictionaryService dictionaryService) {
    this.dictionaryService = dictionaryService;
  }

  /**
   * Updates the job in the service registry. The exceptions that are possibly been thrown are wrapped in a
   * {@link TextAnalyzerException}.
   * 
   * @param job
   *          the job to update
   * @throws TextAnalyzerException
   *           the exception that is being thrown
   */
  private void updateJob(Job job) throws TextAnalyzerException {
    try {
      remoteServiceManager.updateJob(job);
    } catch (NotFoundException notFound) {
      throw new TextAnalyzerException("Unable to find job " + job, notFound);
    } catch (ServiceUnavailableException e) {
      throw new TextAnalyzerException("No service of type '" + JOB_TYPE + "' available", e);
    } catch (ServiceRegistryException serviceRegException) {
      throw new TextAnalyzerException("Unable to update job '" + job + "' in service registry", serviceRegException);
    }
  }

}
