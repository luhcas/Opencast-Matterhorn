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
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.media.mediapackage.UnsupportedElementException;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowConfiguration;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * The workflow definition for handling "compose" operations
 */
public class ComposeWorkflowOperationHandler implements WorkflowOperationHandler {
  private static final Logger logger = LoggerFactory.getLogger(ComposeWorkflowOperationHandler.class);

  private ComposerService composerService;

  protected void setComposerService(ComposerService composerService) {
    this.composerService = composerService;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#run(org.opencastproject.workflow.api.WorkflowInstance)
   */
  public WorkflowOperationResult run(final WorkflowInstance workflowInstance) throws WorkflowOperationException {
    logger.info("run() compose workflow operation on {}", workflowInstance);
    for(WorkflowConfiguration config : workflowInstance.getConfigurations()) {
      logger.info("config {} = {}", config.getKey(), config.getValue());
    }
    MediaPackage resultingMediaPackage;

    // FIXME change when encode is called (think how to pass properties)
    if (workflowInstance.getConfiguration("encode") != null) {
      try {
        resultingMediaPackage = encode(workflowInstance.getCurrentMediaPackage(), workflowInstance.getCurrentOperation());
      } catch (Exception e) {
        throw new WorkflowOperationException(e);
      }
    } else {
      logger.info("No property for encoding, skipping...");
      resultingMediaPackage = workflowInstance.getCurrentMediaPackage();
    }

    logger.info("run() compose operation completed");

    // TODO Add new media track(s) to the media package
    return WorkflowBuilder.getInstance().buildWorkflowOperationResult(resultingMediaPackage, null, false);
  }

  /**
   * Encode tracks from MediaPackage using profiles stored in properties and updates current
   * MediaPackage.
   * @param mediaPackage 
   * @param properties
   * @return
   * @throws EncoderException
   * @throws MediaPackageException
   * @throws UnsupportedElementException
   * @throws ExecutionException 
   * @throws InterruptedException 
   */
  private MediaPackage encode(MediaPackage mediaPackage, WorkflowOperationInstance operation) throws EncoderException,
          MediaPackageException, UnsupportedElementException, InterruptedException, ExecutionException {
    String videoSourceTrackId = operation.getConfiguration("video-source-track-id");
    String audioSourceTrackId = operation.getConfiguration("audio-source-track-id");
    // If there is no separate audio track, use the audio from the video file
    if(audioSourceTrackId == null) {
      audioSourceTrackId = videoSourceTrackId;
    }
    String targetTrackId = operation.getConfiguration("target-track-id");
    
    Track audioSourceTrack = mediaPackage.getTrack(audioSourceTrackId);
    if (audioSourceTrack == null) {
      logger.info("Source track '{}' was not found in media package and will not be encoded", audioSourceTrackId);
      return mediaPackage;
    }

    Track videoSourceTrack = mediaPackage.getTrack(videoSourceTrackId);
    if (videoSourceTrack == null) {
      logger.info("Source track '{}' was not found in media package and will not be encoded", videoSourceTrackId);
      return mediaPackage;
    }

    // TODO profile retrieval, matching for media type (Audio, Visual, AudioVisual, EnhancedAudio, Image,
    // ImageSequence, Cover)
    // String[] profiles = ((String)properties.get("encode")).split(" ");
    EncodingProfile[] profileList = composerService.listProfiles();
    for (EncodingProfile profile : profileList) {
      if (operation.getConfiguration(profile.getIdentifier()) != null) {
        logger.info("Encoding audio track {} and video track {} using profile {}",
                new String[] {audioSourceTrackId, videoSourceTrackId, profile.getIdentifier()});
        Future<Track> futureComposedTrack = composerService.encode(mediaPackage, videoSourceTrackId, audioSourceTrackId,
                targetTrackId, profile.getIdentifier());
        // is there anything we can be doing while we wait for the track to be composed?
        Track composedTrack = futureComposedTrack.get();
        composedTrack.setFlavor(videoSourceTrack.getFlavor());
        // store new tracks to mediaPackage
        // FIXME derived media comes from multiple sources, so how do we choose which is the "parent" of the derived media?
        mediaPackage.addDerived(composedTrack, videoSourceTrack);
        break;
      }
    }
    return mediaPackage;
  }
}
