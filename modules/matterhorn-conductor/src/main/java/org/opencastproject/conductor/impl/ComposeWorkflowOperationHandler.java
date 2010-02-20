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
import org.opencastproject.media.mediapackage.selector.AudioVisualElementSelector;
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

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * The workflow definition for handling "compose" operations
 */
public class ComposeWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory
          .getLogger(ComposeWorkflowOperationHandler.class);

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
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance)
   */
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance)
          throws WorkflowOperationException {
    logger.debug("Running compose workflow operation on workflow {}",
            workflowInstance.getId());

    // Encode the media package
    MediaPackage resultingMediaPackage = null;
    try {
      resultingMediaPackage = encode(workflowInstance.getMediaPackage(),
              workflowInstance.getCurrentOperation());
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
  private MediaPackage encode(MediaPackage src,
          WorkflowOperationInstance operation) throws EncoderException,
          MediaPackageException, UnsupportedElementException,
          InterruptedException, ExecutionException {
    MediaPackage mediaPackage = (MediaPackage)src.clone();
    // Read the configuration properties
    String sourceVideoFlavor = StringUtils.trimToNull(operation
            .getConfiguration("source-video-flavor"));
    String sourceAudioFlavor = StringUtils.trimToNull(operation
            .getConfiguration("source-audio-flavor"));
    String targetTrackTags = StringUtils.trimToNull(operation
            .getConfiguration("target-tags"));
    String targetTrackFlavor = StringUtils.trimToNull(operation
            .getConfiguration("target-flavor"));
    String encodingProfileName = StringUtils.trimToNull(operation
            .getConfiguration("encoding-profile"));

    if (sourceAudioFlavor == null && sourceVideoFlavor == null)
      throw new IllegalStateException(
              "either source audio flavor or source video flavor or both must be specified");

    // Find the encoding profile
    EncodingProfile profile = null;
    for (EncodingProfile p : composerService.listProfiles()) {
      if (p.getIdentifier().equals(encodingProfileName)) {
        profile = p;
        break;
      }
    }
    if (profile == null) {
      throw new IllegalStateException("Encoding profile '"
              + encodingProfileName + "' was not found");
    }

    // Depending on the input type of the profile and the configured flavors and
    // tags,
    // make sure we have the required tracks:
    AudioVisualElementSelector avSelector = new AudioVisualElementSelector();
    avSelector.setVideoFlavor(sourceVideoFlavor);
    avSelector.setAudioFlavor(sourceAudioFlavor);
    switch (profile.getApplicableMediaType()) {
    case Stream:
      avSelector.setRequireVideoTrack(false);
      avSelector.setRequireAudioTrack(false);
      break;
    case AudioVisual:
      avSelector.setRequireVideoTrack(true);
      avSelector.setRequireAudioTrack(true);
      break;
    case Visual:
      avSelector.setRequireVideoTrack(true);
      avSelector.setRequireAudioTrack(false);
      break;
    case Audio:
      avSelector.setRequireVideoTrack(false);
      avSelector.setRequireAudioTrack(true);
      break;
    default:
      logger.warn("Don't know if the current track is applicable to encoding profile '"
                      + profile
                      + "' based on type "
                      + profile.getApplicableMediaType());
      return mediaPackage;
    }
    Collection<Track> tracks = avSelector.select(mediaPackage);

    String sourceVideoTrackId = null;
    String sourceAudioTrackId = null;

    // Did we get the set of tracks that we need?
    if (tracks.size() == 0) {
      logger.debug("Skipping encoding of media package to '{}': no suitable input tracks found", profile);
      return mediaPackage;
    } else {
      for (Track t : tracks) {
        if (sourceVideoTrackId == null && t.hasVideo()) {
          sourceVideoTrackId = t.getIdentifier();
        }
        if (sourceAudioTrackId == null && t.hasAudio()) {
          sourceAudioTrackId = t.getIdentifier();
        }
      }
    }

    // Don't pass the same track as audio *and* video
    if (sourceVideoTrackId != null && sourceVideoTrackId.equals(sourceAudioTrackId)) {
      switch (profile.getOutputType()) {
        case Audio:
          sourceVideoTrackId = null;
          break;
        default:
          sourceAudioTrackId = null;
          break;
      }
    }

    // Make sure we get audio and video for audio and video flavors
    if (sourceVideoFlavor != null && sourceVideoTrackId == null) {
      logger.debug("Skipping encoding of media package to '{}': no video with flavor {} found", profile, sourceVideoFlavor);
      return mediaPackage;
    } else if (sourceAudioFlavor != null && sourceAudioTrackId == null) {
      logger.debug("Skipping encoding of media package to '{}': no audio with flavor {} found", profile, sourceAudioFlavor);
      return mediaPackage;
    }

    // Start encoding and wait for the result
    Future<Track> futureTrack = composerService.encode(mediaPackage,
            sourceVideoTrackId, sourceAudioTrackId, profile.getIdentifier());
    Track composedTrack = futureTrack.get();
    if (composedTrack == null)
      throw new RuntimeException("unable to retrieve composed track");

    // Add the flavor, either from the operation configuration or from the
    // composer
    if (targetTrackFlavor != null)
      composedTrack.setFlavor(MediaPackageElementFlavor
              .parseFlavor(targetTrackFlavor));
    logger.debug("Composed track has flavor '{}'", composedTrack.getFlavor());

    // Set the mimetype
    if (profile.getMimeType() != null)
      composedTrack.setMimeType(MimeTypes.parseMimeType(profile.getMimeType()));

    // Add tags
    if (targetTrackTags != null) {
      for (String tag : targetTrackTags.split("\\W")) {
        if (StringUtils.trimToNull(tag) == null)
          continue;
        logger.trace("Tagging composed track with '{}'", tag);
        composedTrack.addTag(tag);
      }
    }

    // store new tracks to mediaPackage
    // FIXME derived media comes from multiple sources, so how do we choose
    // which is the "parent" of the derived
    // media?
    String parentId = sourceVideoTrackId == null ? sourceAudioTrackId
            : sourceVideoTrackId;
    mediaPackage.addDerived(composedTrack, mediaPackage
            .getElementById(parentId));

    return mediaPackage;
  }
}
