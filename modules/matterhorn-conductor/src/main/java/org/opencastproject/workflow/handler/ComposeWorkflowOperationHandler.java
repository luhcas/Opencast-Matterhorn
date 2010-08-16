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
import org.opencastproject.composer.api.EncodingProfile.MediaType;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.remote.api.Receipt;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

/**
 * The workflow definition for handling "compose" operations
 */
public class ComposeWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ComposeWorkflowOperationHandler.class);

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put("source-flavor", "The \"flavor\" of the track to use as a video source input");
    CONFIG_OPTIONS.put("encoding-profile", "The encoding profile to use");
    CONFIG_OPTIONS.put("target-flavor", "The flavor to apply to the encoded file");
    CONFIG_OPTIONS.put("target-tags", "The tags to apply to the encoded file");
    CONFIG_OPTIONS.put("audio-only", "Set to 'true' to process tracks containing only audio streams");
    CONFIG_OPTIONS.put("video-only", "Set to 'true' to process tracks containing only video streams");
  }

  /** The composer service */
  private ComposerService composerService = null;

  /** The local workspace */
  private Workspace workspace = null;

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
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#getConfigurationOptions()
   */
  @Override
  public SortedMap<String, String> getConfigurationOptions() {
    return CONFIG_OPTIONS;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance)
   */
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance) throws WorkflowOperationException {
    logger.debug("Running compose workflow operation on workflow {}", workflowInstance.getId());

    try {
      return encode(workflowInstance.getMediaPackage(), workflowInstance.getCurrentOperation());
    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    }
  }

  /**
   * Encode tracks from MediaPackage using profiles stored in properties and updates current MediaPackage.
   * 
   * @param src
   *          The source media package
   * @param operation
   *          the current workflow operation
   * @return the operation result containing the updated media package
   * @throws EncoderException
   *           if encoding fails
   * @throws WorkflowOperationException
   *           if errors occur during processing
   * @throws IOException
   *           if the workspace operations fail
   * @throws NotFoundException
   *           if the workspace doesn't contain the requested file
   */
  private WorkflowOperationResult encode(MediaPackage src, WorkflowOperationInstance operation) throws EncoderException,
          IOException, NotFoundException, WorkflowOperationException {
    MediaPackage mediaPackage = (MediaPackage) src.clone();
    // Read the configuration properties
    String sourceFlavor = StringUtils.trimToNull(operation.getConfiguration("source-flavor"));
    String encodingProfileName = StringUtils.trimToNull(operation.getConfiguration("encoding-profile"));
    String audioOnlyConfig = StringUtils.trimToNull(operation.getConfiguration("audio-only"));
    String videoOnlyConfig = StringUtils.trimToNull(operation.getConfiguration("video-only"));

    if (sourceFlavor == null)
      throw new IllegalStateException("Source flavor must be specified");

    // Find the encoding profile
    EncodingProfile profile = composerService.getProfile(encodingProfileName);
    if (profile == null) {
      throw new IllegalStateException("Encoding profile '" + encodingProfileName + "' was not found");
    }

    // Audio / Video only?
    boolean audioOnly = audioOnlyConfig != null && Boolean.parseBoolean(audioOnlyConfig);
    boolean videoOnly = videoOnlyConfig != null && Boolean.parseBoolean(videoOnlyConfig);

    // Depending on the input type of the profile and the configured flavors and
    // tags, make sure we have the required tracks:
    Track[] tracks = mediaPackage.getTracks(MediaPackageElementFlavor.parseFlavor(sourceFlavor));

    // Did we get the set of tracks that we need?
    if (tracks.length == 0) {
      logger.info("Skipping encoding of media package to '{}': no suitable input tracks found", profile);
      return WorkflowBuilder.getInstance().buildWorkflowOperationResult(mediaPackage, Action.CONTINUE);
    }

    // Encode all found tracks
    long totalTimeInQueue = 0;
    for (Track t : tracks) {

      // Skip audio/video only mismatches
      if (audioOnly && t.hasVideo()) {
        logger.info("Skipping encoding of '{}', since it contains a video stream", t);
        continue;
      } else if (videoOnly && t.hasAudio()) {
        logger.info("Skipping encoding of '{}', since it containsa an audio stream", t);
        continue;
      }

      // Check if the track supports the output type of the profile
      MediaType outputType = profile.getOutputType();
      if (outputType.equals(MediaType.Audio) && !t.hasAudio()) {
        logger.info("Skipping encoding of '{}', since it lacks an audio stream", t);
        continue;
      } else if (outputType.equals(MediaType.Visual) && !t.hasVideo()) {
        logger.info("Skipping encoding of '{}', since it lacks a video stream", t);
        continue;
      }

      logger.info("Encoding track {} using encoding profile '{}'", t, profile);

      // Start encoding and wait for the result
      final Receipt receipt = composerService.encode(t, profile.getIdentifier(), true);
      if (receipt == null || receipt.getStatus().equals(Receipt.Status.FAILED)) {
        throw new WorkflowOperationException("Encoding failed");
      }
      Track composedTrack = (Track) receipt.getElement();

      // add this receipt's queue time to the total
      long timeInQueue = receipt.getDateStarted().getTime() - receipt.getDateCreated().getTime();
      totalTimeInQueue+=timeInQueue;

      updateTrackMetadata(composedTrack, operation, profile);

      // store new tracks to mediaPackage
      mediaPackage.addDerived(composedTrack, t);
      String fileName = getFileNameFromElements(t, composedTrack);
      composedTrack.setURI(workspace.moveTo(composedTrack.getURI(), mediaPackage.getIdentifier().toString(),
              composedTrack.getIdentifier(), fileName));
    }

    WorkflowOperationResult result = WorkflowBuilder.getInstance().buildWorkflowOperationResult(mediaPackage, Action.CONTINUE, totalTimeInQueue);
    logger.debug("Compose operation completed");
    return result;
  }

  // Update the newly composed track with metadata
  private void updateTrackMetadata(Track composedTrack, WorkflowOperationInstance operation, EncodingProfile profile) {
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
    List<String> targetTags = asList(targetTrackTags);
    for (String tag : targetTags) {
      logger.trace("Tagging composed track with '{}'", tag);
      composedTrack.addTag(tag);
    }
  }

}
