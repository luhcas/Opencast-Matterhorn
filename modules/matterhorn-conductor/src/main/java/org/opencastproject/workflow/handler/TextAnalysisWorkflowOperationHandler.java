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

import org.opencastproject.analysis.api.MediaAnalysisService;
import org.opencastproject.media.mediapackage.Attachment;
import org.opencastproject.media.mediapackage.Catalog;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.MediaPackageElements;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.media.mediapackage.MediaPackageReference;
import org.opencastproject.media.mediapackage.MediaPackageReferenceImpl;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.media.mediapackage.UnsupportedElementException;
import org.opencastproject.metadata.mpeg7.MediaDuration;
import org.opencastproject.metadata.mpeg7.MediaRelTimePointImpl;
import org.opencastproject.metadata.mpeg7.MediaTime;
import org.opencastproject.metadata.mpeg7.MediaTimeImpl;
import org.opencastproject.metadata.mpeg7.MediaTimePoint;
import org.opencastproject.metadata.mpeg7.Mpeg7Catalog;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogImpl;
import org.opencastproject.metadata.mpeg7.Segment;
import org.opencastproject.metadata.mpeg7.SpatioTemporalDecomposition;
import org.opencastproject.metadata.mpeg7.SpatioTemporalLocator;
import org.opencastproject.metadata.mpeg7.SpatioTemporalLocatorImpl;
import org.opencastproject.metadata.mpeg7.TemporalDecomposition;
import org.opencastproject.metadata.mpeg7.Video;
import org.opencastproject.metadata.mpeg7.VideoSegment;
import org.opencastproject.metadata.mpeg7.VideoText;
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

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

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
    CONFIG_OPTIONS.put("source-flavor", "The flavor of the input tracks");
    CONFIG_OPTIONS.put("source-tags", "The required tags that must exist on the segments catalog");
    CONFIG_OPTIONS.put("target-tags", "The tags to apply to the resulting mpeg-7 catalog");
  }

  /** The local workspace */
  private Workspace workspace = null;

  /** The text analysis service */
  private MediaAnalysisService analysisService = null;

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
  protected void setTextAnalyzer(MediaAnalysisService analysisService) {
    this.analysisService = analysisService;
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

    String sourceFlavor = StringUtils.trimToNull(operation.getConfiguration("source-flavor"));

    // Prepare the set of source tags
    String sourceTags = StringUtils.trimToNull(operation.getConfiguration("source-tags"));
    Set<String> sourceTagSet = null;
    if (sourceTags != null) {
      sourceTagSet = new HashSet<String>();
      sourceTagSet.addAll(Arrays.asList(sourceTags.split("\\W")));
    }

    // Prepare the set of target tages
    String targetTags = StringUtils.trimToNull(operation.getConfiguration("target-tags"));
    Set<String> targetTagSet = null;
    if (targetTags != null) {
      targetTagSet = new HashSet<String>();
      targetTagSet.addAll(Arrays.asList(targetTags.split("\\W")));
    }
    
    // Select the catalogs according to the tags
    Set<Mpeg7Catalog> catalogs = new HashSet<Mpeg7Catalog>();
    for (Catalog c : mediaPackage.getCatalogs(MediaPackageElements.SEGMENTS_FLAVOR)) {
      if (sourceFlavor != null) {
        if (c.getReference() == null)
          continue;
        Track t = mediaPackage.getTrack(c.getReference().getIdentifier());
        if (t == null || !t.getFlavor().matches(MediaPackageElementFlavor.parseFlavor(sourceFlavor)))
          continue;
      }
      
      // Make sure the catalog features at least one of the required tags
      if (!c.containsTag(sourceTagSet))
        continue;

      Mpeg7CatalogImpl mpeg7 = new Mpeg7CatalogImpl(c);
      mpeg7.setTrustedHttpClient(trustedHttpClient);

      // Make sure there is video content
      if (mpeg7.videoContent() == null || !mpeg7.videoContent().hasNext()) {
        logger.debug("Mpeg-7 segments catalog {} does not contain any video content", mpeg7);
        continue;
      }

      // Make sure there is a temporal decomposition
      Video videoContent = mpeg7.videoContent().next();
      TemporalDecomposition<? extends Segment> decomposition = videoContent.getTemporalDecomposition();
      if (decomposition == null || !decomposition.hasSegments()) {
        logger.debug("Mpeg-7 catalog {} does not contain a temporal decomposition", mpeg7);
        continue;
      }

      mpeg7.setFlavor(MediaPackageElements.TEXTS_FLAVOR);
      catalogs.add(mpeg7);
    }

    // Was there at least one matching catalog
    if (catalogs.size() == 0) {
      logger.debug("Mediapackage {} has no suitable mpeg-7 catalogs based on tags {} to to run text analysis",
              mediaPackage, sourceTags);
      return mediaPackage;
    }

    // Loop over all existing segment catalogs
    for (Mpeg7Catalog segmentCatalog : catalogs) {
      
      logger.info("Analyzing mpeg-7 segments catalog {} for text", segmentCatalog);

      // Create a copy that will contain the segments enriched with the video text elements
      Mpeg7CatalogImpl textCatalog = new Mpeg7CatalogImpl(segmentCatalog);
      textCatalog.setTrustedHttpClient(trustedHttpClient);
      
      // Load the temporal decomposition (segments)
      Video videoContent = textCatalog.videoContent().next();
      TemporalDecomposition<? extends Segment> decomposition = videoContent.getTemporalDecomposition();
      Iterator<? extends Segment> segmentIterator = decomposition.segments();

      // For every segment, try to find the still image and run text analysis on it
      while (segmentIterator.hasNext()) {
        Segment segment = segmentIterator.next();
        if (!(segment instanceof VideoSegment))
          continue;

        VideoSegment videoSegment = (VideoSegment) segment;
        MediaTimePoint segmentTimePoint = videoSegment.getMediaTime().getMediaTimePoint();
        MediaDuration segmentDuration = videoSegment.getMediaTime().getMediaDuration();

        // Choose a time
        MediaPackageReference catalogRef = textCatalog.getReference();
        MediaPackageReference reference = null;
        if (catalogRef == null)
          reference = new MediaPackageReferenceImpl();
        else
          reference = new MediaPackageReferenceImpl(catalogRef.getType(), catalogRef.getIdentifier());
        reference.setProperty("time", segmentTimePoint.toString());
        Attachment[] images = mediaPackage.getAttachments(reference, true);

        // Do we have a slide preview?
        if (images == null || images.length == 0) {
          logger.info("No slide preview found for segment {} of mpeg-7 catalog {}", segmentTimePoint, textCatalog);
          continue;
        } else if (images.length > 1) {
          logger.info("More than one slide preview found for segment {} of mpeg-7 catalog {}, using the first one",
                  segmentTimePoint, textCatalog);
        }

        // If there is a corresponding spaciotemporal decomposition, remove all the videotext elements

        Receipt receipt = analysisService.analyze(images[0], true);
        Mpeg7Catalog videoTextCatalog = (Mpeg7Catalog) receipt.getElement();
        if (videoTextCatalog == null)
          throw new RuntimeException("Text analysis service did not return a result");

        // Add the spatiotemporal decompositions from the new catalog to the existing video segments
        Iterator<Video> videoTextContents = videoTextCatalog.videoContent();
        if (videoTextContents == null || !videoTextContents.hasNext()) {
          logger.debug("Text analysis was not able to extract any text from {}", images[0]);
          break;
        }

        try {
          Video textVideoContent = videoTextContents.next();
          VideoSegment textVideoSegment = (VideoSegment) textVideoContent.getTemporalDecomposition().segments().next();
          VideoText[] videoTexts = textVideoSegment.getSpatioTemporalDecomposition().getVideoText();
          SpatioTemporalDecomposition std = videoSegment.createSpatioTemporalDecomposition(true, false);
          for (VideoText videoText : videoTexts) {
            MediaTime mediaTime = new MediaTimeImpl(new MediaRelTimePointImpl(0), segmentDuration);
            SpatioTemporalLocator locator = new SpatioTemporalLocatorImpl(mediaTime);
            videoText.setSpatioTemporalLocator(locator);
            std.addVideoText(videoText);
          }
        } catch (Exception e) {
          logger.warn("The mpeg-7 structure returned by the text analyzer is not what is expected", e);
          continue;
        }

      }

      // Add flavor and target tags
      textCatalog.setFlavor(MediaPackageElements.TEXTS_FLAVOR);
      for (String tag : targetTagSet)
        textCatalog.addTag(tag);

      // Store the catalog in the workspace
      store(mediaPackage, textCatalog);
    }

    logger.debug("Text analysis completed");
    return mediaPackage;
  }

  /**
   * Adds the mpeg-7 catalog to the media package and stores it in the workspace.
   * 
   * @param mediaPackage
   *          the media package
   * @param mpeg7
   *          the new mpeg-7 catalog
   * @return the resulting URI
   */
  protected URI store(MediaPackage mediaPackage, Mpeg7Catalog mpeg7) {
    InputStream in = null;
    try {
      Transformer tf = TransformerFactory.newInstance().newTransformer();
      DOMSource xmlSource = new DOMSource(mpeg7.toXml());
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      tf.transform(xmlSource, new StreamResult(out));
      in = new ByteArrayInputStream(out.toByteArray());
    } catch (Exception e) {
      logger.error("Error serializing mpeg-7catalog", e);
      throw new RuntimeException(e);
    }

    // Add the catalog to the media package
    mpeg7.setIdentifier(null);
    mediaPackage.add(mpeg7);

    // Put the result into the workspace
    String mediaPackageId = mediaPackage.getIdentifier().toString();
    String filename = "slidetext.xml";
    URI workspaceURI = workspace.put(mediaPackageId, mpeg7.getIdentifier(), filename, in);
    mpeg7.setURI(workspaceURI);
    return workspaceURI;
  }

}
