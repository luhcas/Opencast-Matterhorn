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
import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.media.mediapackage.MediaPackage;
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
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Default implementation of the composer service api.
 */
public class ComposerServiceImpl implements ComposerService {

  /** The logging instance */
  private static final Logger log_ = LoggerFactory.getLogger(ComposerServiceImpl.class);

  /** Encoding profile manager */
  private EncodingProfileManager profileManager = null;

  /** Reference to the media inspection service */
  private MediaInspectionService inspectionService = null;

  /** Reference to the workspace service */
  private Workspace workspace = null;

  /**  */
  ExecutorService executor = null;

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
   * Activator that will make sure the encoding profiles are loaded.
   */
  protected void activate(Map<String, String> map) {
    try {
      profileManager = new EncodingProfileManager();
      executor = Executors.newFixedThreadPool(4);
    } catch (ConfigurationException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#encode(org.opencastproject.media.mediapackage.MediaPackage,
   *      java.lang.String, java.lang.String)
   */
  public Future<Track> encode(MediaPackage mediaPackage, String sourceTrackId, String targetTrackId, String profileId)
          throws EncoderException {
    return encode(mediaPackage, sourceTrackId, sourceTrackId, targetTrackId, profileId);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#encode(org.opencastproject.media.mediapackage.MediaPackage,
   *      java.lang.String, java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public Future<Track> encode(final MediaPackage mediaPackage, final String sourceVideoTrackId,
          final String sourceAudioTrackId, final String targetTrackId, final String profileId) throws EncoderException {
    Callable<Track> callable = new Callable<Track>() {
      public Track call() {
        log_.info("encoding track {} for media package {} using source audio track {} and source video track {}",
                new String[] { targetTrackId, mediaPackage.getIdentifier().toString(), sourceAudioTrackId,
                        sourceVideoTrackId });

        // Get the tracks and make sure they exist
        Track audioTrack = mediaPackage.getTrack(sourceAudioTrackId);
        File audioFile = null;
        if (audioTrack != null)
          audioFile = workspace.get(audioTrack.getURI());

        File videoFile = null;
        Track videoTrack = mediaPackage.getTrack(sourceVideoTrackId);
        if (videoTrack != null)
          videoFile = workspace.get(videoTrack.getURI());

        // Create the engine
        EncoderEngine engine = EncoderEngineFactory.newInstance().newEngineByProfile(profileId);
        EncodingProfile profile = profileManager.getProfile(profileId);
        if (profile == null) {
          throw new RuntimeException("Profile '" + profileId + " is unkown");
        }

        // Do the work
        File encodingOutput;
        try {
          encodingOutput = engine.encode(audioFile, videoFile, profile);
        } catch (EncoderException e) {
          throw new RuntimeException(e);
        }

        // Put the file in the workspace
        URI returnURL = null;
        InputStream in = null;
        try {
          in = new FileInputStream(encodingOutput);
          returnURL = workspace
                  .put(mediaPackage.getIdentifier().compact(), targetTrackId, encodingOutput.getName(), in);
          log_.debug("Copied the encoded file to the workspace at {}", returnURL);
          // encodingOutput.delete();
          // log_.info("Deleted the local copy of the encoded file at {}", encodingOutput.getAbsolutePath());
        } catch (Exception e) {
          log_.error("unable to put the encoded file into the workspace");
          e.printStackTrace();
        } finally {
          IOUtils.closeQuietly(in);
        }

        // Have the encoded track inspected and return the result
        Track inspectedTrack = inspectionService.inspect(returnURL);
        inspectedTrack.setIdentifier(targetTrackId);
        return inspectedTrack;
      }
    };
    return executor.submit(callable);
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
}
