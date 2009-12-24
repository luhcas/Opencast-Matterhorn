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
package org.opencastproject.composer.api;

import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.Track;

import java.util.concurrent.Future;

/**
 * Encodes media and (optionally) periodically alerts a statusService endpoint of the status of this encoding job.
 */
public interface ComposerService {

  /**
   * Encode one track, using that track's audio and video streams.
   * 
   * @param mediaPackage The media package containing the source track
   * @param sourceTrackId The ID of the source track within the media package
   * @param targetTrackId The ID of the track to generate
   * @param profileId The profile to use for encoding
   * @return The track that results from the encoding
   * @throws EncoderException
   */
  public Future<Track> encode(MediaPackage mediaPackage, String sourceTrackId, String targetTrackId, String profileId) throws EncoderException;

  /**
   * Encode the video stream from one track and the audio stream from another, into a new {@link Track}.
   * 
   * @param mediaPackage The media package containing the source track
   * @param sourceTrackId The ID of the source track within the media package
   * @param targetTrackId The ID of the track to generate
   * @param profileId The profile to use for encoding
   * @return The track that results from the encoding
   * @throws EncoderException
   */
  public Future<Track> encode(MediaPackage mediaPackage, String sourceVideoTrackId, String sourceAudioTrackId,
          String targetTrackId, String profileId) throws EncoderException;

  /**
   * @return All registered {@link EncodingProfile}s.
   */
  EncodingProfile[] listProfiles();
  
}
