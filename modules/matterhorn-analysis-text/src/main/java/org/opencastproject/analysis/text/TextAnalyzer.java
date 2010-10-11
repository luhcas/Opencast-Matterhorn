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

import org.opencastproject.analysis.api.MediaAnalysisException;
import org.opencastproject.analysis.api.MediaAnalysisServiceSupport;
import org.opencastproject.analysis.text.ocropus.OcropusLine;
import org.opencastproject.analysis.text.ocropus.OcropusTextAnalyzer;
import org.opencastproject.analysis.text.ocropus.OcropusTextFrame;
import org.opencastproject.dictionary.api.DictionaryService;
import org.opencastproject.dictionary.api.DictionaryService.DICT_TOKEN;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageReferenceImpl;
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
import org.opencastproject.remote.api.Job;
import org.opencastproject.remote.api.RemoteServiceManager;
import org.opencastproject.remote.api.Job.Status;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Media analysis service that takes takes an image and returns text as extracted from that image.
 */
public class TextAnalyzer extends MediaAnalysisServiceSupport {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(TextAnalyzer.class);

  /** Receipt type */
  public static final String RECEIPT_TYPE = "org.opencastproject.analysis.text";

  /** Resulting collection in the working file repository */
  public static final String COLLECTION_ID = "ocrtext";

  /** The configuration key for setting the number of worker threads */
  public static final String CONFIG_THREADS = "textanalyzer.threads";

  /** The default worker thread pool size to use if no configuration is specified */
  public static final int DEFAULT_THREADS = 1;

  /** Reference to the receipt service */
  private RemoteServiceManager remoteServiceManager = null;

  /** The workspace to ue when retrieving remote media files */
  private Workspace workspace = null;

  /** The mpeg-7 service */
  protected Mpeg7CatalogService mpeg7CatalogService;

  /** The dictionary service */
  protected DictionaryService dictionaryService;

  /** The executor service used to queue and run jobs */
  private ExecutorService executor = null;

  /** Path to the ocropus binary */
  private String ocropusbinary = OcropusTextAnalyzer.OCROPUS_BINARY_DEFAULT;

  /**
   * Creates a new text analzer.
   */
  public TextAnalyzer() {
    super(MediaPackageElements.TEXTS);
  }

  protected void activate(ComponentContext cc) {
    // set up threading
    int threads = -1;
    String configredThreads = (String) cc.getBundleContext().getProperty(CONFIG_THREADS);
    // try to parse the value as a number. If it fails to parse, there is a config problem so we throw an exception.
    if (configredThreads == null) {
      threads = DEFAULT_THREADS;
    } else {
      threads = Integer.parseInt(configredThreads);
    }
    if (threads < 1) {
      throw new IllegalStateException("The text analyzer needs one or more threads to function.");
    }
    setExecutorThreads(threads);

    if (cc.getBundleContext().getProperty("textanalyzer.ocrocmd") != null)
      ocropusbinary = (String) cc.getBundleContext().getProperty("textanalyzer.ocrocmd");
  }

  /**
   * Separating this from the activate method so it's easier to test
   */
  void setExecutorThreads(int threads) {
    executor = Executors.newFixedThreadPool(threads);
    logger.info("Thread pool size = {}", threads);
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
    final RemoteServiceManager rs = remoteServiceManager;
    final Job receipt = rs.createJob(RECEIPT_TYPE);

    final Attachment attachment = (Attachment) element;
    final URI imageUrl = attachment.getURI();

    Runnable command = new Runnable() {
      @SuppressWarnings("unchecked")
      public void run() {
        receipt.setStatus(Status.RUNNING);
        rs.updateJob(receipt);

        Mpeg7CatalogImpl mpeg7 = Mpeg7CatalogImpl.newInstance();

        try {

          logger.info("Starting text extraction from {}", imageUrl);

          File imageFile = workspace.get(imageUrl);
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

          URI uri = workspace.putInCollection(COLLECTION_ID, receipt.getId() + ".xml", mpeg7CatalogService
                  .serialize(mpeg7));
          Catalog catalog = (Catalog) MediaPackageElementBuilderFactory.newInstance().newElementBuilder().newElement(
                  Catalog.TYPE, MediaPackageElements.TEXTS);
          catalog.setURI(uri);
          catalog.setReference(new MediaPackageReferenceImpl(element));

          receipt.setElement(catalog);
          receipt.setStatus(Status.FINISHED);
          rs.updateJob(receipt);

          logger.info("Finished text extraction of {}", imageUrl);

        } catch (MediaAnalysisException e) {
          receipt.setStatus(Status.FAILED);
          rs.updateJob(receipt);
          throw e;
        } catch (Exception e) {
          receipt.setStatus(Status.FAILED);
          rs.updateJob(receipt);
          throw new MediaAnalysisException(e);
        }
      }
    };

    Future<?> future = executor.submit(command);
    if (block) {
      try {
        future.get();
      } catch (Exception e) {
        receipt.setStatus(Status.FAILED);
        remoteServiceManager.updateJob(receipt);
        throw new MediaAnalysisException(e);
      }
    }
    return receipt;
  }

  /**
   * Returns the receipt.
   * 
   * @param id
   *          the receipt identifier
   * @return the receipt
   */
  public Job getReceipt(String id) {
    return remoteServiceManager.getJob(id);
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
  protected VideoText[] analyze(File imageFile, String id) throws IOException {
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
  public void setRemoteServiceManager(RemoteServiceManager remoteServiceManager) {
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
}
