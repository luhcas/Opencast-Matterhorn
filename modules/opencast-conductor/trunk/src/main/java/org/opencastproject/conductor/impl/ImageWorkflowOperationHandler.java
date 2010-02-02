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
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.media.mediapackage.UnsupportedElementException;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * The workflow definition for handling "image" operations
 */
public class ImageWorkflowOperationHandler implements WorkflowOperationHandler {
  
  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ImageWorkflowOperationHandler.class);

  /** The composer service */
  private ComposerService composerService = null;

  /**
   * Callback for the OSGi declarative services configuration.
   * 
   * @param composerService
   *          the composer service
   */
  protected void setComposerService(ComposerService composerService) {
    this.composerService = composerService;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#run(org.opencastproject.workflow.api.WorkflowInstance)
   */
  public WorkflowOperationResult run(final WorkflowInstance workflowInstance) throws WorkflowOperationException {
    logger.debug("Running image workflow operation on {}", workflowInstance);

    MediaPackage src = (MediaPackage)workflowInstance.getCurrentMediaPackage().clone();

    // Create the image
    MediaPackage resultingMediaPackage = null;
    try {
      resultingMediaPackage = image(src, workflowInstance.getCurrentOperation());
    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    }

    logger.debug("Image operation completed");

    return WorkflowBuilder.getInstance().buildWorkflowOperationResult(resultingMediaPackage, null, false);
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
  private MediaPackage image(MediaPackage mediaPackage, WorkflowOperationInstance operation) throws EncoderException,
          MediaPackageException, UnsupportedElementException, InterruptedException, ExecutionException {

    // Read the configuration properties
    String sourceVideoFlavor = StringUtils.trimToNull(operation.getConfiguration("source-flavor"));
    String sourceTags = StringUtils.trimToNull(operation.getConfiguration("source-tags"));
    String targetImageTags = StringUtils.trimToNull(operation.getConfiguration("target-tags"));
    String targetImageFlavor = StringUtils.trimToNull(operation.getConfiguration("target-flavor"));
    String encodingProfile = StringUtils.trimToNull(operation.getConfiguration("encoding-profile"));
    String timeConfiguration = StringUtils.trimToNull(operation.getConfiguration("time"));

    Set<String> sourceTagSet = null;
    if(StringUtils.trimToNull(sourceTags) != null) {
      sourceTagSet = new HashSet<String>();
      sourceTagSet.addAll(Arrays.asList(sourceTags.split("\\W")));
    }

    // Select the tracks based on the flavors
    Set<Track> videoTracks = new HashSet<Track>();
    for(Track track : mediaPackage.getTracks()) {
      if(sourceVideoFlavor == null || (track.getFlavor() != null && sourceVideoFlavor.equals(track.getFlavor().toString()))) {
        if(sourceTags == null) {
          videoTracks.add(track);
          continue;
        } else {
          for(String tag : track.getTags()) {
            if(sourceTagSet.contains(tag)) {
              videoTracks.add(track);
              continue;
            }
          }
        }
      }
    }
    
    if (videoTracks.size() == 0) {
      logger.debug("Mediapackage {} has no suitable tracks to extract images based on tags {} and flavor {}",
              new Object[] {mediaPackage, sourceTags, sourceVideoFlavor});
      return mediaPackage;
    } else {
      for (Track t : videoTracks) {
        if (t.hasVideo()) {
          // take the minimum of the specified time and the video track duration
          long time = Math.min(Long.parseLong(timeConfiguration), t.getDuration());
          
          for (EncodingProfile profile : composerService.listProfiles()) {
            if (profile.getIdentifier().equals(encodingProfile)) {
              Future<Attachment> futureAttachment = composerService.image(mediaPackage, t.getIdentifier(), profile.getIdentifier(), time);
              // is there anything we can be doing while we wait for the track to be composed?
              Attachment composedImage = futureAttachment.get();
              if (composedImage == null)
                throw new RuntimeException("unable to compose image");

              // Add the flavor, either from the operation configuration or from the composer
              if (targetImageFlavor != null)
                composedImage.setFlavor(MediaPackageElementFlavor.parseFlavor(targetImageFlavor));
              logger.debug("image has flavor '{}'", composedImage.getFlavor());

              // Set the mimetype
              if (profile.getMimeType() != null)
                composedImage.setMimeType(MimeTypes.parseMimeType(profile.getMimeType()));
              
              // Add tags
              if (targetImageTags != null) {
                for (String tag : targetImageTags.split("\\W")) {
                  logger.trace("Tagging image with '{}'", tag);
                  if(StringUtils.trimToNull(tag) != null) composedImage.addTag(tag);
                }
              }
              // store new image in the mediaPackage
              mediaPackage.addDerived(composedImage, t);
            }
          }
        }
      }
    }
    return mediaPackage;
  }

}
