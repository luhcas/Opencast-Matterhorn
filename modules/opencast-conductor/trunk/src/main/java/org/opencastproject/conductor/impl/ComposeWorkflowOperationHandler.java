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
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.media.mediapackage.UnsupportedElementException;
import org.opencastproject.media.mediapackage.selector.AudioElementSelector;
import org.opencastproject.media.mediapackage.selector.AudioVisualElementSelector;
import org.opencastproject.media.mediapackage.selector.VideoElementSelector;
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

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * The workflow definition for handling "compose" operations
 */
public class ComposeWorkflowOperationHandler implements WorkflowOperationHandler {
  
  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ComposeWorkflowOperationHandler.class);

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
    logger.debug("Running compose workflow operation on workflow {}", workflowInstance.getId());

    // Encode the media package
    MediaPackage resultingMediaPackage = null;
    try {
      resultingMediaPackage = encode(workflowInstance.getCurrentMediaPackage(), workflowInstance.getCurrentOperation());
    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    }

    logger.debug("Compose operation completed");

    return WorkflowBuilder.getInstance().buildWorkflowOperationResult(resultingMediaPackage, null, false);
  }

  /**
   * Encode tracks from MediaPackage using profiles stored in properties and updates current MediaPackage.
   * 
   * @param src The source media package
   * @param properties
   * @return
   * @throws EncoderException
   * @throws MediaPackageException
   * @throws UnsupportedElementException
   * @throws ExecutionException
   * @throws InterruptedException
   */
  private MediaPackage encode(MediaPackage src, WorkflowOperationInstance operation) throws EncoderException,
          MediaPackageException, UnsupportedElementException, InterruptedException, ExecutionException {
    MediaPackage mediaPackage = MediaPackageUtil.clone(src);
    // Read the configuration properties
    String sourceVideoFlavor = StringUtils.trimToNull(operation.getConfiguration("source-video-flavor"));
    String sourceAudioFlavor = StringUtils.trimToNull(operation.getConfiguration("source-audio-flavor"));
    String targetTrackTags = StringUtils.trimToNull(operation.getConfiguration("target-tags"));
    String targetTrackFlavor = StringUtils.trimToNull(operation.getConfiguration("target-flavor"));
    String encodingProfile = StringUtils.trimToNull(operation.getConfiguration("encoding-profile"));

    // Select the tracks based on the flavors
    
    Collection<Track> tracks = null;
    if (sourceAudioFlavor != null && sourceVideoFlavor != null) {
      AudioVisualElementSelector avselector = new AudioVisualElementSelector();
      avselector.setVideoFlavor(sourceVideoFlavor);
      avselector.setAudioFlavor(sourceAudioFlavor);
      tracks = avselector.select(mediaPackage);
    } else if (sourceAudioFlavor != null) {
      AudioElementSelector aselector = new AudioElementSelector();
      aselector.setAudioFlavor(sourceAudioFlavor);
      tracks = aselector.select(mediaPackage);
    } else if(sourceVideoFlavor != null) {
      VideoElementSelector vselector = new VideoElementSelector();
      vselector.setVideoFlavor(sourceVideoFlavor);
    } else {
      throw new IllegalStateException("either source audio flavor or source video flavor or both must be specified");
    }
    
    String videoSourceTrackId = null;
    String audioSourceTrackId = null;
    
    if (tracks.size() == 0) {
      logger.info("Mediapackage {} has no suitable tracks to encode based on flavor {}", mediaPackage, sourceVideoFlavor);
      return mediaPackage;
    } else {
      for (Track t : tracks) {
        if (t.hasVideo()) {
          videoSourceTrackId = t.getIdentifier();
        } else if (t.hasAudio()) {
          audioSourceTrackId = t.getIdentifier();
        }
      }
    }
    
    // TODO profile retrieval, matching for media type (Audio, Visual, AudioVisual, EnhancedAudio, Image,
    // ImageSequence, Cover)
    // String[] profiles = ((String)properties.get("encode")).split(" ");
    EncodingProfile[] profileList = composerService.listProfiles();
    for (EncodingProfile profile : profileList) {
      if (profile.getIdentifier().equals(encodingProfile)) {
        Future<Track> futureTrack = composerService.encode(mediaPackage, videoSourceTrackId, audioSourceTrackId, profile.getIdentifier());
        // is there anything we can be doing while we wait for the track to be composed?
        Track composedTrack = futureTrack.get();
        if (composedTrack == null)
          throw new RuntimeException("unable to retrieve composed track");

        // Add the flavor, either from the operation configuration or from the composer
        if (targetTrackFlavor != null)
          composedTrack.setFlavor(MediaPackageElementFlavor.parseFlavor(targetTrackFlavor));
        logger.debug("Composed track has flavor '{}'", composedTrack.getFlavor());

        // Set the mimetype
        if (profile.getMimeType() != null)
          composedTrack.setMimeType(MimeTypes.parseMimeType(profile.getMimeType()));
        
        // Add tags
        if (targetTrackTags != null) {
          for (String tag : targetTrackTags.split("\\W")) {
            if(StringUtils.trimToNull(tag) == null) continue;
            logger.debug("Tagging composed track with '{}'", tag);
            composedTrack.addTag(tag);
          }
        }

        // store new tracks to mediaPackage
        // FIXME derived media comes from multiple sources, so how do we choose which is the "parent" of the derived
        // media?
        String parentId = videoSourceTrackId == null ? audioSourceTrackId : videoSourceTrackId; 
        mediaPackage.addDerived(composedTrack, mediaPackage.getElementById(parentId));
        break;
      }
    }

    return mediaPackage;
  }

}
