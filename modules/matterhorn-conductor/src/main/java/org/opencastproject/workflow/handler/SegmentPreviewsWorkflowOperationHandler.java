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

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
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
import org.opencastproject.metadata.mpeg7.ContentSegment;
import org.opencastproject.metadata.mpeg7.MediaTimePoint;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogImpl;
import org.opencastproject.metadata.mpeg7.TemporalDecomposition;
import org.opencastproject.metadata.mpeg7.Video;
import org.opencastproject.receipt.api.Receipt;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

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
 * The workflow definition for creating segment preview images from an segment mpeg-7 catalog.
 */
public class SegmentPreviewsWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(SegmentPreviewsWorkflowOperationHandler.class);

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put("source-flavor", "The \"flavor\" of the track to use as a video source input");
    CONFIG_OPTIONS.put("source-tags", "The required tags that must exist on the track for the track to be used as a video source");
    CONFIG_OPTIONS.put("encoding-profile", "The encoding profile to use for generating the image");
    CONFIG_OPTIONS.put("target-flavor", "The flavor to apply to the extracted images");
    CONFIG_OPTIONS.put("target-tags", "The tags to apply to the extracted images");
  }
  
  /** The composer service */
  private ComposerService composerService = null;

  /** The trusted http client, used to load mpeg7 catalogs */
  private TrustedHttpClient trustedHttpClient = null;

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#getConfigurationOptions()
   */
  @Override
  public SortedMap<String, String> getConfigurationOptions() {
    return CONFIG_OPTIONS;
  }

  /**
   * Callback for the OSGi declarative services configuration.
   * 
   * @param composerService
   *          the composer service
   */
  protected void setComposerService(ComposerService composerService) {
    this.composerService = composerService;
  }
  
  protected void setTrustedHttpClient(TrustedHttpClient trustedHttpClient) {
    this.trustedHttpClient = trustedHttpClient;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance)
   */
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance) throws WorkflowOperationException {
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
      resultingMediaPackage = createPreviews(src, workflowInstance.getCurrentOperation());
    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    }

    logger.debug("Segments preview operation completed");

    return WorkflowBuilder.getInstance().buildWorkflowOperationResult(resultingMediaPackage, Action.CONTINUE);
  }

  /**
   * Encode tracks from MediaPackage using profiles stored in properties and updates current MediaPackage.
   * 
   * @param mediaPackage
   * @param properties
   * @return
   * @throws EncoderException
   * @throws MediaPackageException
   * @throws UnsupportedElementException
   * @throws ExecutionException
   * @throws InterruptedException
   */
  private MediaPackage createPreviews(final MediaPackage mediaPackage, WorkflowOperationInstance operation)
          throws EncoderException, MediaPackageException, UnsupportedElementException, InterruptedException,
          ExecutionException {

    // Read the configuration properties
    String sourceVideoFlavor = StringUtils.trimToNull(operation.getConfiguration("source-flavor"));
    String sourceTags = StringUtils.trimToNull(operation.getConfiguration("source-tags"));
    String targetImageTags = StringUtils.trimToNull(operation.getConfiguration("target-tags"));
    String targetImageFlavor = StringUtils.trimToNull(operation.getConfiguration("target-flavor"));
    String encodingProfileName = StringUtils.trimToNull(operation.getConfiguration("encoding-profile"));

    // Find the encoding profile
    EncodingProfile profile = composerService.getProfile(encodingProfileName);
    if (profile == null)
      throw new IllegalStateException("Encoding profile '" + encodingProfileName + "' was not found");

    Set<String> sourceTagSet = null;
    if (StringUtils.trimToNull(sourceTags) != null) {
      sourceTagSet = new HashSet<String>();
      sourceTagSet.addAll(Arrays.asList(sourceTags.split("\\W")));
    }

    // Select the tracks based on the flavors
    Set<Track> videoTracks = new HashSet<Track>();
    for (Track track : mediaPackage.getTracks()) {
      if (sourceVideoFlavor == null
              || (track.getFlavor() != null && sourceVideoFlavor.equals(track.getFlavor().toString()))) {
        if (!track.hasVideo())
          continue;
        if (sourceTags == null) {
          videoTracks.add(track);
          continue;
        } else {
          for (String tag : track.getTags()) {
            if (sourceTagSet.contains(tag)) {
              videoTracks.add(track);
              continue;
            }
          }
        }
      }
    }

    if (videoTracks.size() == 0) {
      logger.debug("Mediapackage {} has no suitable tracks to extract images based on tags {} and flavor {}",
              new Object[] { mediaPackage, sourceTags, sourceVideoFlavor });
      return mediaPackage;
    } else {
      for (Track t : videoTracks) {
        
        // Try to load the segments catalog
        MediaPackageReference trackReference = new MediaPackageReferenceImpl(t);
        Catalog[] segmentCatalogs = mediaPackage.getCatalogs(MediaPackageElements.SEGMENTS_FLAVOR, trackReference);
        Mpeg7CatalogImpl mpeg7 = null;
        if (segmentCatalogs.length > 0) {
          mpeg7 = new Mpeg7CatalogImpl(segmentCatalogs[0]);
          mpeg7.setTrustedHttpClient(trustedHttpClient);
          if (segmentCatalogs.length > 1)
            logger.warn("More than one segments catalog found for track {}. Resuming with the first one ({})", t, mpeg7);
        } else {
          logger.debug("No segments catalog found for track {}", t);
          continue;
        }

        // Check the catalog's consistency
        if (mpeg7.videoContent() == null || mpeg7.videoContent().next() == null) {
          logger.info("Segments catalog {} contains no video content", mpeg7);
          continue;
        }
        
        Video videoContent = mpeg7.videoContent().next();
        TemporalDecomposition<? extends ContentSegment> decomposition = videoContent.getTemporalDecomposition();

        // Are there any segments?
        if (decomposition == null || !decomposition.hasSegments()) {
          logger.info("Segments catalog {} contains no video content", mpeg7);
          continue;
        }
          
        // Create the preview images according to the mpeg7 segments
        if (t.hasVideo() && mpeg7 != null) {

          Iterator<? extends ContentSegment> segmentIterator = decomposition.segments();

          while (segmentIterator.hasNext()) {
            ContentSegment segment = segmentIterator.next();
            MediaTimePoint tp = segment.getMediaTime().getMediaTimePoint();

            // Choose a time
            long time = tp.getTimeInMilliseconds()/1000;
  
            Receipt receipt = composerService.image(mediaPackage, t.getIdentifier(), profile.getIdentifier(), time, true);
            Attachment composedImage = (Attachment) receipt.getElement();
            if (composedImage == null)
              throw new RuntimeException("Unable to compose image");
  
            // Add the flavor, either from the operation configuration or from the composer
            if (targetImageFlavor != null) {
              composedImage.setFlavor(MediaPackageElementFlavor.parseFlavor(targetImageFlavor));
              logger.debug("Preview image has flavor '{}'", composedImage.getFlavor());
            }
  
            // Set the mimetype
            if (profile.getMimeType() != null)
              composedImage.setMimeType(MimeTypes.parseMimeType(profile.getMimeType()));
  
            // Add tags
            if (targetImageTags != null) {
              for (String tag : targetImageTags.split("\\W")) {
                logger.trace("Tagging image with '{}'", tag);
                if (StringUtils.trimToNull(tag) != null)
                  composedImage.addTag(tag);
              }
            }

            // Refer to the original track including a timestamp
            MediaPackageReferenceImpl ref = new MediaPackageReferenceImpl(t);
            ref.setProperty("time", tp.toString());
            composedImage.setReference(ref);
            
            // store new image in the mediaPackage
            mediaPackage.add(composedImage);
          }
        }
      }
    }

    return mediaPackage;
  }

}
