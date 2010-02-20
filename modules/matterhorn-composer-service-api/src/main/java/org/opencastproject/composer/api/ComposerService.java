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

import org.opencastproject.media.mediapackage.Attachment;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageException;

import java.util.concurrent.Future;

/**
 * Encodes media and (optionally) periodically alerts a statusService endpoint
 * of the status of this encoding job.
 */
public interface ComposerService {

  /**
   * Encode one track, using that track's audio and video streams.
   * 
   * @param mediaPackage
   *          The media package containing the source track
   * @param sourceTrackId
   *          The ID of the source track within the media package
   * @param profileId
   *          The profile to use for encoding
   * @return The track that results from the encoding
   * @throws EncoderException
   * @throws MediaPackageException
   */
  String encode(String mediaPackage, String sourceTrackId, String profileId) throws EncoderException,
          MediaPackageException;

  /**
   * Encode the video stream from one track and the audio stream from another,
   * into a new {@link Track}.
   * 
   * @param mediaPackage
   *          The media package containing the source track
   * @param sourceVideoTrackId
   *          The ID of the source video track within the media package
   * @param sourceAudioTrackId
   *          The ID of the source audio track within the media package
   * @param profileId
   *          The profile to use for encoding
   * @return The track that results from the encoding
   * @throws EncoderException
   * @throws MediaPackageException
   */
  String encode(String mediaPackage, String sourceVideoTrackId, String sourceAudioTrackId, String profileId) throws EncoderException, MediaPackageException;

  /**
   * Extracts an image from the media package element identified by
   * <code>sourceVideoTrackId</code>. The image is taken at the timepoint
   * <code>time</code> seconds into the movie.
   * 
   * @param mediaPackage
   *          the media package
   * @param sourceVideoTrackId
   *          element identifier of the source video track
   * @param profileId
   *          identifier of the encoding profile
   * @param time
   *          number of seconds into the video
   * @return the extracted image as an attachment
   * @throws EncoderException
   */
  Future<Attachment> image(MediaPackage mediaPackage, String sourceVideoTrackId, String profileId, long time)
          throws EncoderException;

  /**
   * @return All registered {@link EncodingProfile}s.
   */
  EncodingProfile[] listProfiles();
  
  /**
   * Get a {@link Receipt} of the submitted encoding jobs. 
   * @param id The id of a Receipt as returned by the encode method
   * @return Serialized Receipt
   * @throws Exception
   */
  String getReceipt(String id) throws Exception;

  /**
   * Get a number of encoding jobs currently running on the service.
   * 
   * @return Number or current running jobs
   */
  int getNumRunningJobs();
}
