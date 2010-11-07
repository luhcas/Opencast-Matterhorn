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
package org.opencastproject.analysis.text;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.lang.StringUtils;
import org.opencastproject.analysis.api.MediaAnalysisException;
import org.opencastproject.analysis.api.MediaAnalysisServiceSupport;
import org.opencastproject.analysis.text.ocropus.OcropusLine;
import org.opencastproject.analysis.text.ocropus.OcropusTextAnalyzer;
import org.opencastproject.analysis.text.ocropus.OcropusTextFrame;
import org.opencastproject.dictionary.api.DictionaryService;
import org.opencastproject.dictionary.api.DictionaryService.DICT_TOKEN;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElements;
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
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workspace.api.Workspace;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Media analysis service that takes takes an image and returns text as extracted from that image.
 */
public class TextAnalyzer extends MediaAnalysisServiceSupport {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(TextAnalyzer.class);

  /** Receipt type */
  public static final String JOB_TYPE = "org.opencastproject.analysis.text";

  /** Resulting collection in the working file repository */
  public static final String COLLECTION_ID = "ocrtext";

  /** The configuration key for setting the number of worker threads */
  public static final String CONFIG_THREADS = "org.opencastproject.textanalyzer.threads";

  /** The default worker thread pool size to use if no configuration is specified */
  public static final int DEFAULT_THREADS = 1;

  /** Reference to the receipt service */
  private ServiceRegistry remoteServiceManager = null;

  /** The workspace to ue when retrieving remote media files */
  private Workspace workspace = null;

  /** The mpeg-7 service */
  protected Mpeg7CatalogService mpeg7CatalogService;

  /** The dictionary service */
  protected DictionaryService dictionaryService;

  /** The executor service used to queue and run jobs */
  protected ExecutorService executor = null;

  /** Path to the ocropus binary */
  private String ocropusbinary = OcropusTextAnalyzer.OCROPUS_BINARY_DEFAULT;

  /**
   * Creates a new text analzer.
   */
  public TextAnalyzer() {
    super(MediaPackageElements.TEXTS);
  }

  protected void activate(ComponentContext cc) {
    
    // Set the number of concurrent threads
    int threads = DEFAULT_THREADS;
    String threadsConfig = StringUtils.trimToNull(cc.getBundleContext().getProperty(CONFIG_THREADS));
    if (threadsConfig != null) {
      try {
        threads = Integer.parseInt(threadsConfig);
      } catch (NumberFormatException e) {
        logger.warn("Download distribution threads configuration is malformed: '{}'", threadsConfig);
      }
    }
    executor = Executors.newFixedThreadPool(threads);

    if (cc.getBundleContext().getProperty("org.opencastproject.textanalyzer.ocrocmd") != null)
      ocropusbinary = (String) cc.getBundleContext().getProperty("org.opencastproject.textanalyzer.ocrocmd");
  }

  /**
   * Starts text extraction on the image and returns a receipt containing the final result in the form of an
   * Mpeg7Catalog.
   * 
   * @param element
   *          the element to analyze
   * @param block
   *          <code>true</code> to make this operation synchronous
   * @return a receipt containing the resulting mpeg-7 catalog
   * @throws MediaAnalysisException
   */
  @Override
  public Job analyze(final MediaPackageElement element, boolean block) throws MediaAnalysisException {
    final Job job;
    try {
      job = remoteServiceManager.createJob(JOB_TYPE);
    } catch (ServiceUnavailableException e) {
      throw new MediaAnalysisException("No service of type '" + JOB_TYPE + "' available", e);
    } catch (ServiceRegistryException e) {
      throw new MediaAnalysisException("Unable to create job", e);
    }

    final Attachment attachment = (Attachment) element;
    final URI imageUrl = attachment.getURI();

    Callable<Void> command = new Callable<Void>() {
      /**
       * {@inheritDoc}
       * @see java.util.concurrent.Callable#call()
       */
      @SuppressWarnings("unchecked")
      @Override
      public Void call() throws MediaAnalysisException {
        try {
          job.setStatus(Status.RUNNING);
          updateJob(job);

          Mpeg7CatalogImpl mpeg7 = Mpeg7CatalogImpl.newInstance();

          logger.info("Starting text extraction from {}", imageUrl);

          File imageFile;
          try {
            imageFile = workspace.get(imageUrl);
          } catch (NotFoundException e) {
            throw new MediaAnalysisException("Image " + imageUrl + " not found in workspace", e);
          } catch (IOException e) {
            throw new MediaAnalysisException("Unable to access " + imageUrl + " in workspace", e);
          }
          VideoText[] videoTexts = analyze(imageFile, element.getIdentifier());

          // Create a temporal decomposition
          MediaTime mediaTime = new MediaTimeImpl(0, 0);
          Video avContent = mpeg7.addVideoContent(element.getIdentifier(), mediaTime, null);
          TemporalDecomposition<VideoSegment> temporalDecomposition = (TemporalDecomposition<VideoSegment>) avContent
                  .getTemporalDecomposition();

          // Add a segment
          VideoSegment videoSegment = temporalDecomposition.createSegment("segment-0");
          videoSegment.setMediaTime(mediaTime);

          // Add the video text to the spacio temporal decomposition of the segment
          SpatioTemporalDecomposition spatioTemporalDecomposition = videoSegment.createSpatioTemporalDecomposition(
                  true, false);
          for (VideoText videoText : videoTexts) {
            spatioTemporalDecomposition.addVideoText(videoText);
          }

          logger.info("Text extraction of {} finished, {} lines found", attachment.getURI(), videoTexts.length);

          URI uri;
          try {
            uri = workspace.putInCollection(COLLECTION_ID, job.getId() + ".xml",
                    mpeg7CatalogService.serialize(mpeg7));
          } catch (IOException e) {
            throw new MediaAnalysisException("Unable to put mpeg7 into the workspace", e);
          }
          Catalog catalog = (Catalog) MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
                  .newElement(Catalog.TYPE, MediaPackageElements.TEXTS);
          catalog.setURI(uri);

          job.setElement(catalog);
          job.setStatus(Status.FINISHED);
          updateJob(job);

          logger.info("Finished text extraction of {}", imageUrl);
          return null;
        } catch(Exception e) {
          try {
            job.setStatus(Status.FAILED);
            updateJob(job);
          } catch (Exception failureToFail) {
            logger.warn("Unable to update job to failed state", failureToFail);
          }
          if (e instanceof MediaAnalysisException) {
            throw (MediaAnalysisException) e;
          } else {
            throw new MediaAnalysisException(e);
          }
        }
      }
    };

    Future<?> future = executor.submit(command);
    if (block) {
      try {
        future.get();
      } catch (Exception e) {
        try {
          job.setStatus(Status.FAILED);
          updateJob(job);
        } catch (Exception failureToFail) {
          logger.warn("Unable to update job to failed state", failureToFail);
        }
        if (e instanceof MediaAnalysisException) {
          throw (MediaAnalysisException) e;
        } else {
          throw new MediaAnalysisException(e);
        }
      }
    }
    
    return job;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JobProducer#getJob(java.lang.String)
   */
  public Job getJob(String id) throws NotFoundException, ServiceRegistryException {
    return remoteServiceManager.getJob(id);
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
   * @see org.opencastproject.job.api.JobProducer#countJobs(org.opencastproject.job.api.Job.Status,
   *      java.lang.String)
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
  protected VideoText[] analyze(File imageFile, String id) throws MediaAnalysisException {
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
   * {@link MediaAnalysisException}.
   * 
   * @param job
   *          the job to update
   * @throws MediaAnalysisException
   *           the exception that is being thrown
   */
  private void updateJob(Job job) throws MediaAnalysisException {
    try {
      remoteServiceManager.updateJob(job);
    } catch (NotFoundException notFound) {
      throw new MediaAnalysisException("Unable to find job " + job, notFound);
    } catch (ServiceUnavailableException e) {
      throw new MediaAnalysisException("No service of type '" + JOB_TYPE + "' available", e);
    } catch (ServiceRegistryException serviceRegException) {
      throw new MediaAnalysisException("Unable to update job '" + job + "' in service registry", serviceRegException);
    }
  }

}
