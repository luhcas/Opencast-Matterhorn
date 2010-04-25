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
package org.opencastproject.composer.impl;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncoderEngine;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.impl.ffmpeg.FFmpegEncoderEngine;
import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.media.mediapackage.Attachment;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.media.mediapackage.identifier.IdBuilder;
import org.opencastproject.media.mediapackage.identifier.IdBuilderFactory;
import org.opencastproject.receipt.api.Receipt;
import org.opencastproject.receipt.api.ReceiptService;
import org.opencastproject.receipt.api.Receipt.Status;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.workspace.api.NotFoundException;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Default implementation of the composer service api.
 */
public class ComposerServiceImpl implements ComposerService {

  /** The logging instance */
  private static final Logger logger = LoggerFactory.getLogger(ComposerServiceImpl.class);

  /** Encoding profile manager */
  private EncodingProfileManager profileManager = null;

  /** Reference to the media inspection service */
  private MediaInspectionService inspectionService = null;

  /** Reference to the workspace service */
  private Workspace workspace = null;

  /** Reference to the database service */
  private ReceiptService receiptService;

  /** Id builder used to create ids for encoded tracks */
  private final IdBuilder idBuilder = IdBuilderFactory.newInstance().newIdBuilder();

  /** Thread pool */
  ExecutorService executor = null;
  
  private Map<String, Object> encoderEngineConfig = new ConcurrentHashMap<String, Object>();
  public static final String CONFIG_FFMPEG_PATH = "composer.ffmpegpath";

  /**
   * Sets the media inspection service
   * 
   * @param mediaInspectionService
   *          an instance of the media inspection service
   */
  public void setMediaInspectionService(MediaInspectionService mediaInspectionService) {
    this.inspectionService = mediaInspectionService;
  }

  /**
   * Sets the workspace
   * 
   * @param workspace
   *          an instance of the workspace
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * Sets the receipt service
   * @param receiptService
   */
  public void setReceiptService(ReceiptService receiptService) {
    this.receiptService = receiptService;
  }

  /**
   * Activator that will make sure the encoding profiles are loaded.
   */
  @SuppressWarnings("unchecked")
  protected void activate(Map map) {
    // set up threading
    try {
      profileManager = new EncodingProfileManager();
      executor = Executors.newFixedThreadPool(4);
    } catch (ConfigurationException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    // Configure ffmpeg
    String path = (String) map.get(CONFIG_FFMPEG_PATH);
    if (path == null) {
      // DEFAULT - https://issues.opencastproject.org/jira/browse/MH-2158
      logger.info("DEFAULT " + CONFIG_FFMPEG_PATH + ": " + FFmpegEncoderEngine.FFMPEG_BINARY_DEFAULT);
    } else {
      // use CONFIG
      encoderEngineConfig.put(FFmpegEncoderEngine.CONFIG_FFMPEG_BINARY, path);
      logger.info("CONFIG " + CONFIG_FFMPEG_PATH + ": " + path);
    }
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#encode(java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public Receipt encode(MediaPackage mediaPackage, String sourceTrackId, String profileId) throws EncoderException,
          MediaPackageException {
    return encode(mediaPackage, sourceTrackId, sourceTrackId, profileId, false);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#encode(java.lang.String, java.lang.String, java.lang.String,
   *      org.opencastproject.composer.api.ReceiptHandler)
   */
  @Override
  public Receipt encode(MediaPackage mediaPackage, String sourceTrackId, String profileId, boolean block)
          throws EncoderException, MediaPackageException {
    return encode(mediaPackage, sourceTrackId, sourceTrackId, profileId, block);
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#encode(java.lang.String, java.lang.String, java.lang.String,
   *      java.lang.String)
   */
  @Override
  public Receipt encode(MediaPackage mediaPackage, String sourceVideoTrackId, String sourceAudioTrackId,
          String profileId) throws EncoderException, MediaPackageException {
    return encode(mediaPackage, sourceVideoTrackId, sourceAudioTrackId, profileId, false);
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#encode(java.lang.String, java.lang.String, java.lang.String,
   *      java.lang.String, boolean)
   */
  @Override
  public Receipt encode(final MediaPackage mp, final String sourceVideoTrackId, final String sourceAudioTrackId,
          final String profileId, final boolean block) throws EncoderException, MediaPackageException {

    final String targetTrackId = idBuilder.createNew().toString();
    final Receipt composerReceipt = receiptService.createReceipt(RECEIPT_TYPE);

    // Get the tracks and make sure they exist
    Track audioTrack = mp.getTrack(sourceAudioTrackId);
    final File audioFile;
    if (audioTrack == null) {
      audioFile = null;
    } else {
      try {
        audioFile = workspace.get(audioTrack.getURI());
      } catch (NotFoundException e) {
        composerReceipt.setStatus(Status.FAILED);
        receiptService.updateReceipt(composerReceipt);
        throw new MediaPackageException("unable to access audio track " + audioTrack);
      }
    }

    Track videoTrack = mp.getTrack(sourceVideoTrackId);
    final File videoFile;
    if (videoTrack == null) {
      videoFile = null;
    } else {
      try {
        videoFile = workspace.get(videoTrack.getURI());
      } catch (NotFoundException e) {
        composerReceipt.setStatus(Status.FAILED);
        receiptService.updateReceipt(composerReceipt);
        throw new MediaPackageException("unable to access video track " + videoTrack);
      }
    }

    // Create the engine
    EncoderEngineFactory factory = EncoderEngineFactory.newInstance();
    final EncoderEngine engine = factory.newEngineByProfile(profileId);
    final EncodingProfile profile = profileManager.getProfile(profileId);
    if (profile == null) {
      composerReceipt.setStatus(Status.FAILED);
      receiptService.updateReceipt(composerReceipt);
      throw new RuntimeException("Profile '" + profileId + " is unkown");
    }

    Runnable runnable = new Runnable() {
      public void run() {
        logger.info("encoding track {} for media package {} using source audio track {} and source video track {}",
                new String[] { targetTrackId, mp.getIdentifier().toString(), sourceAudioTrackId, sourceVideoTrackId });
        composerReceipt.setStatus(Status.RUNNING);
        receiptService.updateReceipt(composerReceipt);

        // Do the work
        File encodingOutput;
        try {
          encodingOutput = engine.encode(audioFile, videoFile, profile, null);
        } catch (EncoderException e) {
          composerReceipt.setStatus(Status.FAILED);
          receiptService.updateReceipt(composerReceipt);
          throw new RuntimeException(e);
        }

        // Put the file in the workspace
        URI returnURL = null;
        InputStream in = null;
        try {
          in = new FileInputStream(encodingOutput);
          returnURL = workspace.put(mp.getIdentifier().compact(), targetTrackId, encodingOutput.getName(), in);
          logger.debug("Copied the encoded file to the workspace at {}", returnURL);
          encodingOutput.delete();
          logger.info("Deleted the local copy of the encoded file at {}", encodingOutput.getAbsolutePath());
        } catch (Exception e) {
          composerReceipt.setStatus(Status.FAILED);
          receiptService.updateReceipt(composerReceipt);
          logger.error("unable to put the encoded file into the workspace");
          throw new RuntimeException(e);
        } finally {
          IOUtils.closeQuietly(in);
        }
        if (encodingOutput != null)
          encodingOutput.delete(); // clean up the encoding output, since the file is now safely stored in the file repo

        // Have the encoded track inspected and return the result
        Receipt inspectionReceipt = inspectionService.inspect(returnURL, true);
        Track inspectedTrack = (Track)inspectionReceipt.getElement();
        inspectedTrack.setIdentifier(targetTrackId);

        composerReceipt.setElement(inspectedTrack);
        composerReceipt.setStatus(Status.FINISHED);
        receiptService.updateReceipt(composerReceipt);
      }
    };
    Future<?> future = executor.submit(runnable);
    if (block) {
      try {
        future.get();
      } catch (Exception e) {
        composerReceipt.setStatus(Status.FAILED);
        receiptService.updateReceipt(composerReceipt);
        throw new EncoderException(engine, e);
      }
    }
    return composerReceipt;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#listProfiles()
   */
  public EncodingProfile[] listProfiles() {
    Collection<EncodingProfile> profiles = profileManager.getProfiles().values();
    return profiles.toArray(new EncodingProfile[profiles.size()]);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#getProfile(java.lang.String)
   */
  @Override
  public EncodingProfile getProfile(String profileId) {
    return profileManager.getProfiles().get(profileId);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#image(org.opencastproject.media.mediapackage.MediaPackage,
   *      java.lang.String, java.lang.String, long)
   */
  @Override
  public Receipt image(MediaPackage mediaPackage, String sourceVideoTrackId, String profileId, long time)
          throws EncoderException, MediaPackageException {
    return image(mediaPackage, sourceVideoTrackId, profileId, time, false);
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#image(org.opencastproject.media.mediapackage.MediaPackage,
   *      java.lang.String, java.lang.String, long, boolean)
   */
  public Receipt image(final MediaPackage mediaPackage, final String sourceVideoTrackId, final String profileId,
          final long time, boolean block) throws EncoderException, MediaPackageException {
    final String targetAttachmentId = "attachment-" + (mediaPackage.getAttachments().length + 1);

    // Create the engine
    final EncoderEngine engine = EncoderEngineFactory.newInstance().newEngineByProfile(profileId);

    final EncodingProfile profile = profileManager.getProfile(profileId);
    if (profile == null) {
      throw new RuntimeException("Profile '" + profileId + " is unkown");
    }

    // Get the video track and make sure it exists
    Track videoTrack = mediaPackage.getTrack(sourceVideoTrackId);
    if (videoTrack != null && !videoTrack.hasVideo()) {
      throw new RuntimeException("can not extract an image without a video stream");
    }
    if (videoTrack == null) {
      throw new RuntimeException("videoTrack cannot be null");
    }
    if (time < 0 || time > videoTrack.getDuration()) {
      throw new IllegalArgumentException("Can not extract an image at time " + Long.valueOf(time)
              + " from a video track with duration " + Long.valueOf(videoTrack.getDuration()));
    }
    final File videoFile;
    try {
      videoFile = workspace.get(videoTrack.getURI());
    } catch (NotFoundException e) {
      throw new MediaPackageException("unable to access video track " + videoTrack, e);
    }
    final Receipt receipt = receiptService.createReceipt(RECEIPT_TYPE);

    Runnable runnable = new Runnable() {
      @Override
      public void run() {
        logger.info("creating an image for media package {} using video track {}", new String[] {
                mediaPackage.getIdentifier().toString(), sourceVideoTrackId });

        receipt.setStatus(Status.RUNNING);
        receiptService.updateReceipt(receipt);

        Map<String, String> properties = new HashMap<String, String>();
        String timeAsString = Long.toString(time);
        properties.put("time", timeAsString);
        // Do the work
        File encodingOutput = null;
        try {
          encodingOutput = engine.encode(videoFile, profile, properties);
        } catch (EncoderException e) {
          throw new RuntimeException(e);
        }

        if (encodingOutput == null || !encodingOutput.isFile())
          throw new RuntimeException("Encoding output doesn't exist: " + encodingOutput);

        // Put the file in the workspace
        URI returnURL = null;
        InputStream in = null;
        try {
          in = new FileInputStream(encodingOutput);
          returnURL = workspace.put(mediaPackage.getIdentifier().compact(), targetAttachmentId, encodingOutput
                  .getName(), in);
          logger.debug("Copied the encoded file to the workspace at {}", returnURL);
        } catch (Exception e) {
          receipt.setStatus(Status.FAILED);
          receiptService.updateReceipt(receipt);
          throw new RuntimeException("unable to put the encoded file into the workspace", e);
        } finally {
          IOUtils.closeQuietly(in);
        }
        if (encodingOutput != null)
          encodingOutput.delete(); // clean up the encoding output, since the file is now safely stored in the file repo
        Attachment attachment = (Attachment) MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
                .elementFromURI(returnURL, Attachment.TYPE, null);

        receipt.setElement(attachment);
        receipt.setStatus(Status.FINISHED);
        receiptService.updateReceipt(receipt);
      }
    };
    Future<?> future = executor.submit(runnable);
    if (block) {
      try {
        future.get();
      } catch (ExecutionException e) {
        throw new EncoderException(engine, e);
      } catch (InterruptedException e) {
        throw new EncoderException(engine, e);
      }
    }
    return receipt;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#getReceipt(java.lang.String)
   */
  public Receipt getReceipt(String id) {
    return receiptService.getReceipt(id);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#countJobs()
   */
  @Override
  public long countJobs(Status status) {
    return receiptService.count(RECEIPT_TYPE, status);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#countJobs(java.lang.String, java.lang.String)
   */
  @Override
  public long countJobs(Status status, String host) {
    return receiptService.count(RECEIPT_TYPE, status, host);
  }
}
