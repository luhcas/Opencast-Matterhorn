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
package org.opencastproject.analysis.api;

import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.Track;

/**
 * Convencience implementation that can be used to easily build media analysis service implementations.
 */
public abstract class MediaAnalysisServiceSupport implements MediaAnalysisService {

  /** The catalog flavor that is produced by this implementation */
  protected MediaPackageElementFlavor resultingFlavor = null;

  /** The flavors that are required by this media analysis */
  protected MediaPackageElementFlavor[] requiredFlavors = new MediaPackageElementFlavor[] {};

  /** Flag to indicate audio track as a requirement */
  protected boolean requiresAudio = false;

  /** Flag to indicate video track as a requirement */
  protected boolean requiresVideo = false;

  /**
   * Creates a new media analysis support object with no resulting flavor and no requirements.
   */
  protected MediaAnalysisServiceSupport() {
    this(null, new MediaPackageElementFlavor[] {});
  }

  /**
   * Creates a new media analysis support object with the given resulting flavor and requirements.
   * 
   * @param resultingFlavor
   *          the resulting catalog flavor
   * @param requiredFlavors
   *          the required catalog flavors
   */
  protected MediaAnalysisServiceSupport(MediaPackageElementFlavor resultingFlavor,
          MediaPackageElementFlavor[] requiredFlavors) {
    this.resultingFlavor = resultingFlavor;
    if (requiredFlavors != null)
      this.requiredFlavors = requiredFlavors;
  }

  /**
   * Creates a new media analysis support object with the given resulting flavor and no requirements.
   * 
   * @param resultingFlavor
   *          the resulting catalog flavor
   */
  protected MediaAnalysisServiceSupport(MediaPackageElementFlavor resultingFlavor) {
    this(resultingFlavor, new MediaPackageElementFlavor[] {});
  }

  /**
   * Creates a new media analysis support object with the given set of required flavors.
   * 
   * @param requiredFlavors
   *          the required catalog flavors
   */
  protected MediaAnalysisServiceSupport(MediaPackageElementFlavor[] requiredFlavors) {
    this(null, requiredFlavors);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.analysis.api.MediaAnalysisService#produces()
   */
  @Override
  public MediaPackageElementFlavor produces() {
    return resultingFlavor;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.analysis.api.MediaAnalysisService#requires()
   */
  @Override
  public MediaPackageElementFlavor[] requires() {
    return requiredFlavors;
  }

  /**
   * Sets the requirement for audio.
   * 
   * @param requireAudio
   *          <code>true</code> to require audio
   */
  public void requireAudio(boolean requireAudio) {
    this.requiresAudio = requireAudio;
  }

  /**
   * Sets the requirement for video.
   * 
   * @param requireVideo
   *          <code>true</code> to require video
   */
  public void requireVideo(boolean requireVideo) {
    this.requiresVideo = requireVideo;
  }

  /**
   * Returns <code>true</code> if the requirements that this media analysis service defines are met.
   * 
   * @return <code>true</code> if all requirements are met
   * @throws IllegalArgumetnException
   *           if the element is <code>null</code>
   */
  protected boolean isSupported(MediaPackageElement element) throws IllegalArgumentException {
    if (element == null)
      throw new IllegalArgumentException("Media package cannot be null");

    // Test for audio/video support in case this is required
    if (requiresAudio || requiresVideo) {
      if (!MediaPackageElement.Type.Track.equals(element.getElementType()))
        return false;
      Track t = (Track) element;
      if ((requiresAudio && !t.hasAudio()) || (requiresVideo && !t.hasVideo()))
        return false;
    }

    // Test flavor requirements
    if (requires().length == 0)
      return true;
    for (MediaPackageElementFlavor flavor : requires()) {
      if (flavor.equals(element.getFlavor())) {
        return true;
      }
    }
    
    return false;
  }

}
