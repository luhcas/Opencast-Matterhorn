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
package org.opencastproject.composer.api;

import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.receipt.api.Receipt;
import org.opencastproject.receipt.api.Receipt.Status;

/**
 * Encodes media and (optionally) periodically alerts a statusService endpoint of the status of this encoding job.
 */
public interface ComposerService {
  final String RECEIPT_TYPE = "org.opencastproject.composer";
    
  /**
   * Encode one track, using that track's audio and video streams.
   * 
   * @param mediaPackage
   *          The media package containing the source track
   * @param sourceTrackId
   *          The ID of the source track within the media package
   * @param profileId
   *          The profile to use for encoding
   * @return The receipt for this encoding job. The receipt can be used with {@link ComposerService#getReceipt(String)}
   *         to obtain the status of an encoding job.
   * @throws EncoderException
   * @throws MediaPackageException
   */
  Receipt encode(MediaPackage mediaPackage, String sourceTrackId, String profileId) throws EncoderException,
          MediaPackageException;

  /**
   * Encode one track, using that track's audio and video streams.
   * 
   * @param mediaPackage
   *          The media package containing the source track
   * @param sourceTrackId
   *          The ID of the source track within the media package
   * @param profileId
   *          The profile to use for encoding
   * @param block
   *          Whether this method should block the calling thread (true) or return asynchronously (false)
   * @return The receipt for this encoding job
   * @throws EncoderException
   * @throws MediaPackageException
   */
  Receipt encode(MediaPackage mediaPackage, String sourceTrackId, String profileId, boolean block)
          throws EncoderException, MediaPackageException;

  /**
   * Encode the video stream from one track and the audio stream from another, into a new {@link Track}.
   * 
   * @param mediaPackage
   *          The media package containing the source track
   * @param sourceVideoTrackId
   *          The ID of the source video track within the media package
   * @param sourceAudioTrackId
   *          The ID of the source audio track within the media package
   * @param profileId
   *          The profile to use for encoding
   * @return The receipt for this encoding job
   * @throws EncoderException
   * @throws MediaPackageException
   */
  Receipt encode(MediaPackage mediaPackage, String sourceVideoTrackId, String sourceAudioTrackId, String profileId)
          throws EncoderException, MediaPackageException;

  /**
   * Encode the video stream from one track and the audio stream from another, into a new {@link Track}.
   * 
   * @param mediaPackage
   *          The media package containing the source track
   * @param sourceVideoTrackId
   *          The ID of the source video track within the media package
   * @param sourceAudioTrackId
   *          The ID of the source audio track within the media package
   * @param profileId
   *          The profile to use for encoding
   * @param block
   *          Whether this method should block the calling thread (true) or return asynchronously (false)
   * @return The receipt for this encoding job
   * @throws EncoderException
   * @throws MediaPackageException
   */
  Receipt encode(MediaPackage mediaPackage, String sourceVideoTrackId, String sourceAudioTrackId, String profileId,
          boolean block) throws EncoderException, MediaPackageException;

  /**
   * Extracts an image from the media package element identified by <code>sourceVideoTrackId</code>. The image is taken
   * at the timepoint <code>time</code> seconds into the movie.
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
  Receipt image(MediaPackage mediaPackage, String sourceVideoTrackId, String profileId, long time)
          throws EncoderException, MediaPackageException;

  /**
   * Extracts an image from the media package element identified by <code>sourceVideoTrackId</code>. The image is taken
   * at the timepoint <code>time</code> seconds into the movie.
   * 
   * @param mediaPackage
   *          the media package
   * @param sourceVideoTrackId
   *          element identifier of the source video track
   * @param profileId
   *          identifier of the encoding profile
   * @param time
   *          number of seconds into the video
   * @param block
   *          Whether this method should block the calling thread (true) or return asynchronously (false)
   * @return the extracted image as an attachment
   * @throws EncoderException
   */
  Receipt image(MediaPackage mediaPackage, String sourceVideoTrackId, String profileId, long time, boolean block)
          throws EncoderException, MediaPackageException;

  /**
   * @return All registered {@link EncodingProfile}s.
   */
  EncodingProfile[] listProfiles();

  /**
   * Gets a profile by its ID
   * 
   * @param profileId
   *          The profile ID
   * @return The encoding profile, or null if no profile is registered with that ID
   */
  EncodingProfile getProfile(String profileId);

  /**
   * Get a {@link Receipt} of the submitted encoding jobs.
   * 
   * @param id
   *          The id of a Receipt
   * @return The Receipt with this identifier, or null if no receipt exists with this identifier
   */
  Receipt getReceipt(String id);

  /**
   * Get the number of encoding jobs in a current status on all nodes.
   * 
   * @return Number of jobs in this state
   */
  long countJobs(Status status);

  /**
   * Get the number of encoding jobs in a current status on a specific node.
   * 
   * @return Number of running jobs
   */
  long countJobs(Status status, String host);

}
