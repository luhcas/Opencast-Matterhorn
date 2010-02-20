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
package org.opencastproject.composer.impl;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncoderEngine;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.impl.endpoint.ComposerServiceDao;
import org.opencastproject.composer.impl.endpoint.Receipt;
import org.opencastproject.composer.impl.endpoint.ReceiptBuilder;
import org.opencastproject.composer.impl.endpoint.Receipt.STATUS;
import org.opencastproject.composer.impl.ffmpeg.FFmpegEncoderEngine;
import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.media.mediapackage.Attachment;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilder;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.util.ConfigurationException;
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
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
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
  private ComposerServiceDao dao;

  private MediaPackageBuilder builder;

  /**  */
  ExecutorService executor = null;

  private Map<String, Object> encoderEngineConfig = new ConcurrentHashMap<String, Object>();
  public static final String CONFIG_FFMPEG_PATH = "composer.ffmpegpath";
  protected Map<String, Future<Track>> futuresMap = null;
  protected Thread pollingThread = null;
  protected boolean poll;

  /**
   * Callback for declarative services configuration that will introduce us to the media inspection service.
   * Implementation assumes that the reference is configured as being static.
   * 
   * @param mediaInspectionService
   *          an instance of the media inspection service
   */
  public void setMediaInspectionService(MediaInspectionService mediaInspectionService) {
    this.inspectionService = mediaInspectionService;
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
   * Callback for declarative services configuration that will introduce us to the database service. Implementation
   * assumes that the reference is configured as being static.
   * 
   * @param workspace
   *          an instance of the workspace
   */
  public void setDao(ComposerServiceDao dao) {
    this.dao = dao;
  }

  /**
   * Activator that will make sure the encoding profiles are loaded.
   */
  @SuppressWarnings("unchecked")
  protected void activate(Map map) {

    // set up builder
    builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();

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

    // Set up polling to persist the state of running encoding jobs
    futuresMap = new ConcurrentHashMap<String, Future<Track>>();
    poll = true;
    pollingThread = new Thread(new Runnable() {
      public void run() {
        while (poll) {
          logger.debug("polling for completed encoding tasks");
          for (Iterator<Entry<String, Future<Track>>> entryIter = futuresMap.entrySet().iterator(); entryIter.hasNext();) {
            Entry<String, Future<Track>> entry = entryIter.next();
            Future<Track> futureTrack = entry.getValue();
            String id = entry.getKey();
            logger.debug("found receipt {} while polling", id);
            if (futureTrack.isDone()) {
              logger.debug("encoding task with receipt {} is done", id);
              // update the database
              Receipt receipt = dao.getReceipt(id);
              if (receipt == null)
                throw new RuntimeException("Could not find the receipt for encoding job " + id);
              try {
                Track t = futureTrack.get();
                if (t == null) {
                  // this was a failed encoding job
                  receipt.setStatus(STATUS.FAILED.toString());
                } else {
                  receipt.setStatus(STATUS.FINISHED.toString());
                  receipt.setTrack(t);
                }
                dao.updateReceipt(receipt);
                entryIter.remove();
              } catch (Exception e) {
                e.printStackTrace();
                logger.error(e.getMessage());
                receipt.setStatus(STATUS.FAILED.toString());
                dao.updateReceipt(receipt);
              }
            } else {
              logger.debug("encoding task for receipt {} is still running", id);
            }
          }
          try {
            Thread.sleep(10 * 1000); // check again in 10 seconds
          } catch (InterruptedException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
          }
        }
      }
    });
    pollingThread.start();

  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#encode(java.lang.String, java.lang.String, java.lang.String)
   */
  public String encode(String mediaPackage, String sourceTrackId, String profileId) throws EncoderException,
          MediaPackageException {
    return encode(mediaPackage, sourceTrackId, sourceTrackId, profileId);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#encode(org.opencastproject.media.mediapackage.MediaPackage,
   *      java.lang.String, java.lang.String, java.lang.String, java.lang.String)
   */
  public String encode(final String mediaPackage, final String sourceVideoTrackId, final String sourceAudioTrackId,
          final String profileId) throws EncoderException, MediaPackageException {

    final MediaPackage mp = builder.loadFromXml(mediaPackage);
    final String targetTrackId = "track-" + (mp.getTracks().length + 1);
    Callable<Track> callable = new Callable<Track>() {
      public Track call() {
        logger.info("encoding track {} for media package {} using source audio track {} and source video track {}",
                new String[] { targetTrackId, mp.getIdentifier().toString(), sourceAudioTrackId, sourceVideoTrackId });

        // Get the tracks and make sure they exist
        Track audioTrack = mp.getTrack(sourceAudioTrackId);
        File audioFile = null;
        if (audioTrack != null)
          audioFile = workspace.get(audioTrack.getURI());

        File videoFile = null;
        Track videoTrack = mp.getTrack(sourceVideoTrackId);
        if (videoTrack != null)
          videoFile = workspace.get(videoTrack.getURI());

        // Create the engine
        EncoderEngineFactory factory = EncoderEngineFactory.newInstance();
        EncoderEngine engine = factory.newEngineByProfile(profileId);
        EncodingProfile profile = profileManager.getProfile(profileId);
        if (profile == null) {
          throw new RuntimeException("Profile '" + profileId + " is unkown");
        }

        // Do the work
        File encodingOutput;
        try {
          encodingOutput = engine.encode(audioFile, videoFile, profile, null);
        } catch (EncoderException e) {
          throw new RuntimeException(e);
        }

        // Put the file in the workspace
        URI returnURL = null;
        InputStream in = null;
        try {
          in = new FileInputStream(encodingOutput);
          returnURL = workspace.put(mp.getIdentifier().compact(), targetTrackId, encodingOutput.getName(), in);
          logger.debug("Copied the encoded file to the workspace at {}", returnURL);
          // encodingOutput.delete();
          // logger.info("Deleted the local copy of the encoded file at {}", encodingOutput.getAbsolutePath());
        } catch (Exception e) {
          logger.error("unable to put the encoded file into the workspace");
          e.printStackTrace();
        } finally {
          IOUtils.closeQuietly(in);
        }
        if (encodingOutput != null)
          encodingOutput.delete(); // clean up the encoding output, since the file is now safely stored in the file repo

        // Have the encoded track inspected and return the result
        Track inspectedTrack = inspectionService.inspect(returnURL);
        inspectedTrack.setIdentifier(targetTrackId);
        return inspectedTrack;
      }
    };
    Future<Track> ft = executor.submit(callable);

    // Prepair receit to return
    Receipt receipt = dao.createReceipt();
    receipt.setStatus(STATUS.RUNNING.toString());
    logger.debug("created receipt {}", receipt.getId());
    futuresMap.put(receipt.getId(), ft);
    return receipt.getId();
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
   * @see org.opencastproject.composer.api.ComposerService#image(org.opencastproject.media.mediapackage.MediaPackage,
   *      java.lang.String, long)
   */
  public Future<Attachment> image(final MediaPackage mediaPackage, final String sourceVideoTrackId,
          final String profileId, final long time) throws EncoderException {
    final String targetAttachmentId = "attachment-" + (mediaPackage.getAttachments().length + 1);
    Callable<Attachment> callable = new Callable<Attachment>() {
      public Attachment call() {
        logger.info("creating an image for media package {} using video track {}", new String[] {
                mediaPackage.getIdentifier().toString(), sourceVideoTrackId });

        // Get the video track and make sure it exists
        File videoFile = null;
        Track videoTrack = mediaPackage.getTrack(sourceVideoTrackId);
        if (videoTrack != null) {
          videoFile = workspace.get(videoTrack.getURI());
          if (!videoTrack.hasVideo()) {
            throw new RuntimeException("can not extract an image without a video stream");
          }
        } else {
          throw new RuntimeException("videoTrack cannot be null");
        }

        // Create the engine
        EncoderEngine engine = EncoderEngineFactory.newInstance().newEngineByProfile(profileId);

        EncodingProfile profile = profileManager.getProfile(profileId);
        if (profile == null) {
          throw new RuntimeException("Profile '" + profileId + " is unkown");
        }
        if (time < 0 || time > videoTrack.getDuration()) {
          throw new IllegalArgumentException("Can not extract an image at time " + Long.valueOf(time)
                  + " from a video track with duration " + Long.valueOf(videoTrack.getDuration()));
        }

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

          throw new RuntimeException("unable to put the encoded file into the workspace", e);
        } finally {
          IOUtils.closeQuietly(in);
        }
        if (encodingOutput != null)
          encodingOutput.delete(); // clean up the encoding output, since the file is now safely stored in the file repo
        return (Attachment) MediaPackageElementBuilderFactory.newInstance().newElementBuilder().elementFromURI(
                returnURL, Attachment.TYPE, null);
      }
    };
    return executor.submit(callable);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#getReceipt(java.lang.String)
   */
  public String getReceipt(String id) throws Exception {
    Receipt r = dao.getReceipt(id);
    return ReceiptBuilder.getInstance().toXml(r);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#getNumRunningJobs()
   */
  @Override
  public int getNumRunningJobs() {
    return futuresMap.size();
  }
}
