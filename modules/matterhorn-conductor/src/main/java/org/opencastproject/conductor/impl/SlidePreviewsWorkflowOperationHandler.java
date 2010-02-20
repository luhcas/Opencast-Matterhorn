/**
 *  Copyright 2009 The Regents of the University of California
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
package org.opencastproject.conductor.impl;

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
import org.opencastproject.metadata.mpeg7.Mpeg7Catalog;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogService;
import org.opencastproject.metadata.mpeg7.MultimediaContent;
import org.opencastproject.metadata.mpeg7.MultimediaContentType;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * This workflow operation will look for MPEG-7 catalogs and create a preview
 * image for every time segment it finds by calling to the composer service.
 */
public class SlidePreviewsWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(SlidePreviewsWorkflowOperationHandler.class);

  /** The composer service */
  private ComposerService composerService = null;

  /** The mpeg7 service */
  private Mpeg7CatalogService mpeg7Service = null;

  /**
   * Callback for the OSGi declarative services configuration.
   * 
   * @param composerService
   *          the composer service
   */
  protected void setComposerService(ComposerService composerService) {
    this.composerService = composerService;
  }

  protected void setMpeg7CatalogService(Mpeg7CatalogService mpeg7CatalogService) {
    this.mpeg7Service = mpeg7CatalogService;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance)
   */
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance) throws WorkflowOperationException {
    logger.debug("Running compose workflow operation on workflow {}", workflowInstance.getId());

    // Encode the media package
    MediaPackage resultingMediaPackage = null;
    try {
      resultingMediaPackage = createSlidePreview(workflowInstance.getMediaPackage(), workflowInstance
              .getCurrentOperation());
    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    }

    logger.debug("Compose operation completed");

    return WorkflowBuilder.getInstance().buildWorkflowOperationResult(resultingMediaPackage, Action.CONTINUE);
  }

  /**
   * Encode tracks from MediaPackage using profiles stored in properties and
   * updates current MediaPackage.
   * 
   * @param src
   *          The source media package
   * @param properties
   * @return
   * @throws EncoderException
   * @throws MediaPackageException
   * @throws UnsupportedElementException
   * @throws ExecutionException
   * @throws InterruptedException
   */
  private MediaPackage createSlidePreview(MediaPackage src, WorkflowOperationInstance operation)
          throws EncoderException, MediaPackageException, UnsupportedElementException, InterruptedException,
          ExecutionException {
    MediaPackage mediaPackage = (MediaPackage) src.clone();

    // Find the mpeg7 slides catalog
    Catalog mpeg7Catalogs[] = mediaPackage.getCatalogs(Mpeg7Catalog.SLIDES_FLAVOR);
    if (mpeg7Catalogs.length == 0) {
      logger.debug("No slides catalog available.");
      return mediaPackage;
    } else if (mpeg7Catalogs.length > 1) {
      logger.debug("multiple slides catalogs found... using the first catalog to generate images.");
    }

    Mpeg7Catalog mpeg7Catalog = mpeg7Service.load(mpeg7Catalogs[0]);

    // Read the configuration properties
    String sourceVideoFlavor = StringUtils.trimToNull(operation.getConfiguration("source-flavor"));
    String sourceTags = StringUtils.trimToNull(operation.getConfiguration("source-tags"));
    String targetImageTags = StringUtils.trimToNull(operation.getConfiguration("target-tags"));
    String encodingProfileName = StringUtils.trimToNull(operation.getConfiguration("encoding-profile"));

    // Find the encoding profile
    EncodingProfile profile = null;
    for (EncodingProfile p : composerService.listProfiles()) {
      if (p.getIdentifier().equals(encodingProfileName)) {
        profile = p;
        break;
      }
    }
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
    }

    // Check for multimedia content
    if (!mpeg7Catalog.multimediaContent().hasNext()) {
      logger.warn("Mpeg-7 doesn't contain  multimedia content");
      return mediaPackage;
    }

    // Get the content duration by looking at the first content track. This
    // of course assumes that all tracks are equally long.
    Iterator<MultimediaContent<? extends MultimediaContentType>> mmIter = mpeg7Catalog.multimediaContent();

    // We assume that there is just one track per mpeg7 catalog
    if (!mmIter.hasNext()) {
      logger.debug("No track in this mpeg7 catalog");
      return mediaPackage;
    }

    MultimediaContent<?> multimediaContent = mmIter.next();
    // for every multimedia content track
    for (Iterator<?> iterator = multimediaContent.elements(); iterator.hasNext();) {
      MultimediaContentType type = (MultimediaContentType) iterator.next();

      // for every segment in the current multimedia content track

      Iterator<? extends ContentSegment> ctIter = type.getTemporalDecomposition().segments();
      while (ctIter.hasNext()) {
        ContentSegment contentSegment = ctIter.next();

        // get the segments time properties
        MediaTimePoint timepoint = contentSegment.getMediaTime().getMediaTimePoint();
        long time = timepoint.getTimeInMilliseconds();
        long timeInSeconds = time/1000L;
        
        if (time < 0) {
          logger.warn("Segment does not have a time associated with it");
          continue;
        }

        if (time > mediaPackage.getDuration()) {
          logger.warn("The duration of the mpeg-7 catalog does not match the media package duration");
          return mediaPackage;
        }

        // create an image for this timepoint
        for (Track t : videoTracks) {
          if (t.hasVideo()) {
            Future<Attachment> futureAttachment = composerService.image(mediaPackage, t.getIdentifier(), profile
                    .getIdentifier(), timeInSeconds);
            // is there anything we can be doing while we wait for the track to
            // be composed?
            Attachment composedImage = futureAttachment.get();
            if (composedImage == null)
              throw new RuntimeException("unable to compose image");

            MediaPackageElementFlavor flavor = MediaPackageElements.SLIDE_PREVIEW_FLAVOR;
            if (t.getFlavor() != null) {
              String flavorType = MediaPackageElements.SLIDE_PREVIEW_FLAVOR.getType();
              String flavorSubtype = t.getFlavor().getType();
              flavor = new MediaPackageElementFlavor(flavorType, flavorSubtype);
            }
            composedImage.setFlavor(flavor);
            logger.debug("image has flavor '{}'", composedImage.getFlavor());

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
            
            // store new image in the mediaPackage
            MediaPackageReference ref = new MediaPackageReferenceImpl(); // links to this mediapackage
            ref.setProperty("time", Long.toString(timepoint.getTimeInMilliseconds()));
            mediaPackage.add(composedImage);
            composedImage.setReference(ref);
          }
        }
      }
    }
    return mediaPackage;
  }
}
