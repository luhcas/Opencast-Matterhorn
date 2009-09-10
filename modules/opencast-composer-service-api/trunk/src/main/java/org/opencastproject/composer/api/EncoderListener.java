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

import org.opencastproject.media.mediapackage.Track;

/**
 * Interface for encoder listener.
 */
public interface EncoderListener {

  /**
   * Tells the listener that the given track has been encoded into the
   * {@link EncodingProfile.ethz.replay.core.api.common.media.DistributionFormat} <code>format</code>.
   * 
   * @param engine
   *          the encoding engine
   * @param track
   *          the track that was encoded
   * @param profile
   *          the encoding profile
   */
  void trackEncoded(EncoderEngine engine, Track track, EncodingProfile profile);

  /**
   * Tellst the listener that the given track could not be encoded into
   * {@link EncodingProfile.ethz.replay.core.api.common.media.DistributionFormat} <code>format</code>.
   * 
   * @param engine
   *          the encoding engine
   * @param track
   *          the track that was encoded
   * @param profile
   *          the encoding profile
   * @param cause
   *          the failure reason
   */
  void trackEncodingFailed(EncoderEngine engine, Track track, EncodingProfile profile, Throwable cause);

  /**
   * Tells the listener about encoding progress while the track is being encoded into
   * {@link EncodingProfile.ethz.replay.core.api.common.media.DistributionFormat} <code>format</code>. The value ranges
   * between <code>0</code> (started) and <code>100</code> (finished).
   * 
   * @param engine
   *          the encoding engine
   * @param track
   *          the track that was encoded
   * @param profile
   *          the encoding profile
   * @param progress
   *          the encoding progress
   */
  void trackEncodingProgressed(EncoderEngine engine, Track track, EncodingProfile profile, int progress);

}