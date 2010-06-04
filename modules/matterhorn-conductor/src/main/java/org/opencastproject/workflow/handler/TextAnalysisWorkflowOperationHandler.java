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
package org.opencastproject.workflow.handler;

import org.opencastproject.analysis.text.TextAnalyzer;
import org.opencastproject.media.mediapackage.Attachment;
import org.opencastproject.media.mediapackage.Catalog;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageElements;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.media.mediapackage.MediaPackageReference;
import org.opencastproject.media.mediapackage.MediaPackageReferenceImpl;
import org.opencastproject.media.mediapackage.UnsupportedElementException;
import org.opencastproject.metadata.mpeg7.MediaTimePoint;
import org.opencastproject.metadata.mpeg7.Mpeg7Catalog;
import org.opencastproject.metadata.mpeg7.Segment;
import org.opencastproject.metadata.mpeg7.TemporalDecomposition;
import org.opencastproject.metadata.mpeg7.Video;
import org.opencastproject.remote.api.Receipt;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

/**
 * The <code>TextAnalysisOperationHandler</code> will take an <code>MPEG-7</code> catalog, look for video segments and
 * run a text analysis on the associated still images. The resulting <code>VideoText</code> elements will then be added
 * to the segments.
 */
public class TextAnalysisWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(SegmentPreviewsWorkflowOperationHandler.class);

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put("source-tags", "The required tags that must exist on the catalog");
  }

  /** The local workspace */
  private Workspace workspace = null;

  /** The text analysis service */
  private TextAnalyzer textAnalysisService = null;

  /** The trusted http client, used to load mpeg7 catalogs */
  private TrustedHttpClient trustedHttpClient = null;

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#getConfigurationOptions()
   */
  @Override
  public SortedMap<String, String> getConfigurationOptions() {
    return CONFIG_OPTIONS;
  }

  /**
   * Callback for the OSGi declarative services configuration that will set the text analysis service.
   * 
   * @param textAnalyzer
   *          the text analysis service
   */
  protected void setTextAnalyzer(TextAnalyzer textAnalyzer) {
    this.textAnalysisService = textAnalyzer;
  }

  /**
   * Callback from the OSGi environment that will set the trusted http client.
   * 
   * @param trustedHttpClient
   *          the http client
   */
  protected void setTrustedHttpClient(TrustedHttpClient trustedHttpClient) {
    this.trustedHttpClient = trustedHttpClient;
  }

  /**
   * Callback for declarative services configuration that will introduce us to the local workspace service.
   * Implementation assumes that the reference is configured as being static.
   * 
   * @param workspace
   *          an instance of the workspace
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance) throws WorkflowOperationException {
    logger.debug("Running segments preview workflow operation on {}", workflowInstance);

    // Check if there is an mpeg-7 catalog containing video segments
    MediaPackage src = (MediaPackage) workflowInstance.getMediaPackage().clone();
    Catalog[] segmentCatalogs = src.getCatalogs(MediaPackageElements.SEGMENTS_FLAVOR);
    if (segmentCatalogs.length == 0) {
      logger.info("Media package {} does not contain segment information", src);
      return WorkflowBuilder.getInstance().buildWorkflowOperationResult(Action.CONTINUE);
    }

    // Create the image
    MediaPackage resultingMediaPackage = null;
    try {
      resultingMediaPackage = extractVideoText(src, workflowInstance.getCurrentOperation());
    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    }

    logger.debug("Segments preview operation completed");

    return WorkflowBuilder.getInstance().buildWorkflowOperationResult(resultingMediaPackage, Action.CONTINUE);
  }

  /**
   * Runs the text analysis service on each of the video segments found.
   * 
   * @param mediaPackage
   *          the original mediapackage
   * @param operation
   *          the workflow operation
   * @throws MediaPackageException
   * @throws UnsupportedElementException
   * @throws ExecutionException
   * @throws InterruptedException
   */
  private MediaPackage extractVideoText(final MediaPackage mediaPackage, WorkflowOperationInstance operation)
          throws MediaPackageException, UnsupportedElementException, InterruptedException, ExecutionException {

    // Read the configuration properties
    String sourceTags = StringUtils.trimToNull(operation.getConfiguration("source-tags"));

    Set<String> sourceTagSet = null;
    if (StringUtils.trimToNull(sourceTags) != null) {
      sourceTagSet = new HashSet<String>();
      sourceTagSet.addAll(Arrays.asList(sourceTags.split("\\W")));
    }

    // Select the catalogs according to the tags
    Set<Mpeg7Catalog> catalogs = new HashSet<Mpeg7Catalog>();
    for (String tag : sourceTagSet) {
      for (Catalog c : mediaPackage.getCatalogs(MediaPackageElements.SEGMENTS_FLAVOR)) {
        if (c.containsTag(tag) && c instanceof Mpeg7Catalog) {
          Mpeg7Catalog mpeg7 = (Mpeg7Catalog)c;
          
          // Make sure there is video content
          if (mpeg7.videoContent() == null || mpeg7.videoContent().next() == null) {
            logger.info("Mpeg-7 catalog {} does not contain any video content", mpeg7);
            continue;
          }
          
          // Make sure there is a temporal decomposition
          Video videoContent = mpeg7.videoContent().next();
          TemporalDecomposition<? extends Segment> decomposition = videoContent.getTemporalDecomposition();
          if (decomposition == null || !decomposition.hasSegments()) {
            logger.info("Mpeg-7 catalog {} does not contain a temporal decomposition", mpeg7);
            continue;
          }

          catalogs.add((Mpeg7Catalog)c);
        }
      }
    }
    
    // Was there at least one matching catalog
    if (catalogs.size() == 0) {
      logger.debug("Mediapackage {} has no suitable mpeg-7 catalogs based on tags {} to to run text analysis", mediaPackage, sourceTags);
      return mediaPackage;
    }

    // Loop over all catalogs
    for (Mpeg7Catalog mpeg7 : catalogs) {

      logger.info("Inspecting segments of mpeg-7 catalog {}", mpeg7);

      // Load the temporal decomposition (segments)
      Video videoContent = mpeg7.videoContent().next();
      TemporalDecomposition<? extends Segment> decomposition = videoContent.getTemporalDecomposition();
      Iterator<? extends Segment> segmentIterator = decomposition.segments();

      // For every segment, try to find the still image and run text analysis on it
      while (segmentIterator.hasNext()) {
        Segment segment = segmentIterator.next();
        MediaTimePoint tp = segment.getMediaTime().getMediaTimePoint();

        // Choose a time
        MediaPackageReference reference = new MediaPackageReferenceImpl();
        reference.setProperty("time", tp.toString());
        Attachment[] images = mediaPackage.getAttachments(reference);
        
        // Do we have a slide preview?
        if (images == null || images.length == 0) {
          logger.info("No slide preview found for segment {} of mpeg-7 catalog {}", tp, mpeg7);
          continue;
        } else if (images.length > 1) {
          logger.info("More than one slide preview found for segment {} of mpeg-7 catalog {}, using the first one", tp, mpeg7);
        }
        
        // If there is a corresponding spaciotemporal decomposition, remove all the videotext elements
        // TODO: Remove existing videotext elements

        Receipt receipt = textAnalysisService.analyze(images[0], true);
        Mpeg7Catalog videoTextCatalog = (Mpeg7Catalog)receipt.getElement();
        if (videoTextCatalog == null)
          throw new RuntimeException("Text analysis service did not return a result");

        // Add the videotext elements from the analysis result
        // TODO: Add videotext elements to mpeg7 catalog
        
      }
      
      // TODO: update mpeg-7 catalog
    }

    return mediaPackage;
  }

}
