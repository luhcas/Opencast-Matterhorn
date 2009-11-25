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
import java.net.URL;
import java.util.Collection;
import java.util.Map;

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
  public Track encode(MediaPackage mediaPackage, String sourceTrackId, String targetTrackId, String profileId) throws EncoderException {
    StringBuilder message = new StringBuilder();
    message.append("encoding job started on:\nMedia package=");
    message.append(mediaPackage.getIdentifier());
    message.append("\nTrack ID=");
    message.append(sourceTrackId);

    // Get the track and make sure it's there
    Track track = mediaPackage.getTrack(sourceTrackId);
    if (track == null)
      throw new RuntimeException("Unable to encode non-existing track " + sourceTrackId);
    File workspaceVersion = workspace.get(track.getURL());

    // Create the engine
    EncoderEngine engine = EncoderEngineFactory.newInstance().newEngineByProfile(profileId);
    EncodingProfile profile = profileManager.getProfile(profileId);
    if (profile == null)
      throw new RuntimeException("Profile '" + profileId + " is unkown");
    
    // Do the work
    File encodingOutput = engine.encode(workspaceVersion, profile);

    // Put the file in the workspace
    URL returnURL = null;
    InputStream in = null;
    try {
      in = new FileInputStream(encodingOutput);
      returnURL = workspace.put(mediaPackage.getIdentifier().compact(), targetTrackId, encodingOutput.getName(), in);
      log_.info("Copied the encoded file to the workspace at " + returnURL);
//      encodingOutput.delete();
//      log_.info("Deleted the local copy of the encoded file at " + encodingOutput.getAbsolutePath());
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
