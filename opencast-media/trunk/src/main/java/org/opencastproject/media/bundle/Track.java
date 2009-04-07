/**
 *  Copyright 2009 Opencast Project (http://www.opencastproject.org)
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

package org.opencastproject.media.bundle;

/**
 * This interface describes methods and fields for audio and video tracks as
 * part of a bundle.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public interface Track extends BundleElement {

  /**
   * Returns the track duration in milliseconds.
   * 
   * @return the track duration
   */
  long getDuration();

  /**
   * Returns <code>true</code> if this track contains video information.
   * <p>
   * For example, an ISO Motion JPEG 2000 file will return <code>true</code>
   * here, while an MP3 won't.
   * 
   * @return <code>true</code> if this track contains video
   */
  boolean hasVideo();

  /**
   * Returns <code>true</code> if this track contains audio information.
   * <p>
   * For example, an MP3 file will return <code>true</code> here, while an ISO
   * Motion JPEG 2000 won't.
   * 
   * @return <code>true</code> if this track contains audio
   */
  boolean hasAudio();

  /**
   * Returns the track's description with details about framerate, codecs etc.
   * 
   * @return the track description.
   */
  String getDescription();

}