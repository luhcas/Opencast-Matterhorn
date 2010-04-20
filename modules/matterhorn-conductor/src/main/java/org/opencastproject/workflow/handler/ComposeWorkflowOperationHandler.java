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
import org.opencastproject.composer.api.Receipt;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.media.mediapackage.MediaPackageReferenceImpl;
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

/**
 * The workflow definition for handling "compose" operations
 */
public class ComposeWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ComposeWorkflowOperationHandler.class);

  /** The composer service */
  private ComposerService composerService = null;

  /** Name of the audio-only (strip video) profile */
  public static final String AUDIO_ONLY_PROFILE = "audio-only.http";

  /** Name of the audio-only (strip audio) profile */
  public static final String VIDEO_ONLY_PROFILE = "video-only.http";

  /**
   * Callback for the OSGi declarative services configuration.
   * 
   * @param composerService
   *          the local composer service
   */
  protected void setComposerService(ComposerService composerService) {
    this.composerService = composerService;
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
      resultingMediaPackage = encode(workflowInstance.getMediaPackage(), workflowInstance.getCurrentOperation());
    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    }

    logger.debug("Compose operation completed");

    return WorkflowBuilder.getInstance().buildWorkflowOperationResult(resultingMediaPackage, Action.CONTINUE);
  }

  /**
   * Encode tracks from MediaPackage using profiles stored in properties and updates current MediaPackage.
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
  private MediaPackage encode(MediaPackage src, WorkflowOperationInstance operation) throws EncoderException,
          MediaPackageException, UnsupportedElementException, InterruptedException, ExecutionException {
    MediaPackage mediaPackage = (MediaPackage) src.clone();
    // Read the configuration properties
    String sourceVideoFlavor = StringUtils.trimToNull(operation.getConfiguration("source-video-flavor"));
    String sourceAudioFlavor = StringUtils.trimToNull(operation.getConfiguration("source-audio-flavor"));
    String targetTrackTags = StringUtils.trimToNull(operation.getConfiguration("target-tags"));
    String targetTrackFlavor = StringUtils.trimToNull(operation.getConfiguration("target-flavor"));
    String encodingProfileName = StringUtils.trimToNull(operation.getConfiguration("encoding-profile"));

    if (sourceAudioFlavor == null && sourceVideoFlavor == null)
      throw new IllegalStateException("either source audio flavor or source video flavor or both must be specified");

    // Find the encoding profile
    EncodingProfile profile = null;
    for (EncodingProfile p : composerService.listProfiles()) {
      if (p.getIdentifier().equals(encodingProfileName)) {
        profile = p;
        break;
      }
    }
    if (profile == null) {
      throw new IllegalStateException("Encoding profile '" + encodingProfileName + "' was not found");
    }

    // Depending on the input type of the profile and the configured flavors and
    // tags,
    // make sure we have the required tracks:
    AudioVisualElementSelector avSelector = new AudioVisualElementSelector();
    avSelector.setVideoFlavor(sourceVideoFlavor);
    avSelector.setAudioFlavor(sourceAudioFlavor);      
    switch (profile.getApplicableMediaType()) {
      case AudioVisual:
        avSelector.setRequireVideoTrack(true);
        avSelector.setRequireAudioTrack(true);
        break;
      case Visual:
        avSelector.setRequireVideoTrack(true);
        break;
      case Audio:
        avSelector.setRequireAudioTrack(true);
        break;
    }
    Collection<Track> tracks = avSelector.select(mediaPackage);

    String sourceVideoTrackId = null;
    String sourceAudioTrackId = null;

    // Did we get the set of tracks that we need?
    if (tracks.size() == 0) {
      logger.debug("Skipping encoding of media package to '{}': no suitable input tracks found", profile);
      return mediaPackage;
    } else {
      if (avSelector.getAudioTrack() != null)
        sourceAudioTrackId = avSelector.getAudioTrack().getIdentifier();
      if (avSelector.getVideoTrack() != null)
        sourceVideoTrackId = avSelector.getVideoTrack().getIdentifier();
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

    // If muxing (audio + video from two different files) is happening, then we need to make sure that those files
    // contain the relevant streams only. Therefore, we encode to audio-only and video-only first.
    if (sourceAudioTrackId != null && sourceVideoTrackId != null) {
      Track audioTrack = mediaPackage.getTrack(sourceAudioTrackId);
      if (audioTrack.hasVideo()) {
        Track strippedTrack = getStrippedTrack(mediaPackage, audioTrack, AUDIO_ONLY_PROFILE);
        mediaPackage.add(strippedTrack);
        sourceAudioTrackId = strippedTrack.getIdentifier();
      }
      Track videoTrack = mediaPackage.getTrack(sourceVideoTrackId);
      if (videoTrack.hasAudio()) {
        Track strippedTrack = getStrippedTrack(mediaPackage, videoTrack, VIDEO_ONLY_PROFILE);
        mediaPackage.add(strippedTrack);
        sourceVideoTrackId = strippedTrack.getIdentifier();
      }
    }

    // choose composer service with least running jobs
    // listAllComposerServices();
    // ComposerService cs = allComposerServices[0];
    // for (ComposerService c : allComposerServices) {
    // if (c.countJobs() < cs.countJobs()) {
    // cs = c;
    // }
    // }
    // logger.debug("Media will be encoded on {}", cs.toString());

    // Start encoding and wait for the result
    final Receipt receipt = composerService.encode(mediaPackage, sourceVideoTrackId, sourceAudioTrackId, profile
            .getIdentifier(), true);
    Track composedTrack = (Track) receipt.getElement();
    updateTrack(composedTrack, operation, profile);

    // store new tracks to mediaPackage
    // FIXME derived media comes from multiple sources, so how do we choose
    // which is the "parent" of the derived
    // media?
    String parentId = sourceVideoTrackId == null ? sourceAudioTrackId : sourceVideoTrackId;
    mediaPackage.addDerived(composedTrack, mediaPackage.getElementById(parentId));

    // Add the flavor
    MediaPackageElementFlavor targetFlavor = null;
    if (targetTrackFlavor != null) {
      targetFlavor = MediaPackageElementFlavor.parseFlavor(targetTrackFlavor);
    } else {
      if (sourceVideoFlavor != null)
        targetFlavor = MediaPackageElementFlavor.parseFlavor(sourceVideoFlavor);
      else if (sourceAudioFlavor != null)
        targetFlavor = MediaPackageElementFlavor.parseFlavor(sourceAudioFlavor);
    }
    composedTrack.setFlavor(targetFlavor);

    // Add the tags
    if (targetTrackTags != null) {
      String[] tags = targetTrackTags.split("\\W");
      if (tags.length > 0)
        for (String tag : tags)
          composedTrack.addTag(tag);
    }
    return mediaPackage;
  }

  // Update the newly composed track with metadata
  private void updateTrack(Track composedTrack, WorkflowOperationInstance operation, EncodingProfile profile) {
    // Read the configuration properties
    String targetTrackTags = StringUtils.trimToNull(operation.getConfiguration("target-tags"));
    String targetTrackFlavor = StringUtils.trimToNull(operation.getConfiguration("target-flavor"));

    if (composedTrack == null)
      throw new RuntimeException("unable to retrieve composed track");

    // Add the flavor, either from the operation configuration or from the
    // composer
    if (targetTrackFlavor != null)
      composedTrack.setFlavor(MediaPackageElementFlavor.parseFlavor(targetTrackFlavor));
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
  }

  /**
   * Create a stripped down version of the track, using the specified encoding profile. The result is a track (with
   * either stripped video or audio), a reference to the original track as well as a flavor created from the original
   * track and including the encoding profile, as in <code>source/presentation+audioonly</code>.
   * 
   * @param mediaPackage
   *          the media package
   * @param trackId
   *          the track identifier
   * @param profile
   *          the encoding profile
   * @return the encoded track
   * @throws MediaPackageException
   *           if accessing the media package fails
   * @throws EncoderException
   *           if encoding fails
   */
  private Track getStrippedTrack(MediaPackage mediaPackage, Track track, String profile) throws MediaPackageException,
          EncoderException {
    MediaPackageElementFlavor strippedFlavor = null;

    // Create a derived flavor (original+audionly)
    if (track.getFlavor() != null) {
      String profileSuffix = profile.replaceAll("\\.[\\W]*$", "").replaceAll("\\W", "");
      String subtype = track.getFlavor().getSubtype() + "+" + profileSuffix;
      strippedFlavor = new MediaPackageElementFlavor(track.getFlavor().getType(), subtype);
    }

    // See if such a track is already part of the mediapackage
    Track strippedTrack = null;
    if (strippedFlavor != null) {
      Track[] candidates = mediaPackage.getTracks(strippedFlavor, new MediaPackageReferenceImpl(track));
      if (candidates.length == 1)
        strippedTrack = candidates[0];
    }

    // If no such track was found, create it
    if (strippedTrack == null) {
      logger.info("Creating stripped version of '{}' using encoding profile '{}'", track, profile);
      final Receipt receipt = composerService.encode(mediaPackage, track.getIdentifier(), null, profile, true);
      strippedTrack = (Track) receipt.getElement();
      strippedTrack.setFlavor(strippedFlavor);
      strippedTrack.referTo(track);
    }
    return strippedTrack;
  }

}
