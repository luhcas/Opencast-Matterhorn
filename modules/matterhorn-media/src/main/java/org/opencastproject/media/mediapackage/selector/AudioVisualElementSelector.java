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
package org.opencastproject.media.mediapackage.selector;

import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.Track;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * This <code>MediaPackageElementSelector</code> selects a combination of tracks
 * from a <code>MediaPackage</code> that contain audio and video stream.
 */
public class AudioVisualElementSelector extends
        AbstractMediaPackageElementSelector<Track> {

  /** Explicit video flavor */
  protected MediaPackageElementFlavor videoFlavor = null;

  /** Explicit audio flavor */
  protected MediaPackageElementFlavor audioFlavor = null;

  /** Make this selector require an audio track */
  protected boolean requireAudio = true;

  /** Make this selector require a video track */
  protected boolean requireVideo = true;

  /**
   * Creates a new selector.
   */
  public AudioVisualElementSelector() {
  }

  /**
   * Creates a new selector that will restrict the result of
   * <code>select()</code> to the given flavor.
   * 
   * @param flavor
   *          the flavor
   */
  public AudioVisualElementSelector(String flavor) {
    this(MediaPackageElementFlavor.parseFlavor(flavor));
  }

  /**
   * Creates a new selector that will restrict the result of
   * <code>select()</code> to the given flavor.
   * 
   * @param flavor
   *          the flavor
   */
  public AudioVisualElementSelector(MediaPackageElementFlavor flavor) {
    addFlavor(flavor);
  }

  /**
   * Specifies an explicit audio flavor.
   * 
   * @param flavor
   *          the flavor
   */
  public void setAudioFlavor(String flavor) {
    if (flavor == null) {
      audioFlavor = null;
      return;
    }
    setAudioFlavor(MediaPackageElementFlavor.parseFlavor(flavor));
  }

  /**
   * Specifies an explicit audio flavor.
   * 
   * @param flavor
   *          the flavor
   */
  public void setAudioFlavor(MediaPackageElementFlavor flavor) {
    if (flavor != null)
      addFlavor(flavor);
    audioFlavor = flavor;
  }

  /**
   * If set to <code>true</code>, this selector requires an audio track to be
   * part of the result set, or a video track containing at least one audio
   * stream.
   * 
   * @param require
   *          <code>true</code> to require an audio track
   */
  public void setRequireAudioTrack(boolean require) {
    requireAudio = require;
  }

  /**
   * If set to <code>true</code>, this selector requires a video track to be
   * part of the result set.
   * 
   * @param require
   *          <code>true</code> to require a video track
   */
  public void setRequireVideoTrack(boolean require) {
    requireVideo = require;
  }

  /**
   * Returns the explicit audio flavor or <code>null</code> if none was
   * specified.
   * 
   * @return the audio flavor
   */
  public MediaPackageElementFlavor getAudioFlavor() {
    return audioFlavor;
  }

  /**
   * Specifies an explicit video flavor.
   * 
   * @param flavor
   *          the flavor
   */
  public void setVideoFlavor(String flavor) {
    if (flavor == null) {
      videoFlavor = null;
    } else {
      setVideoFlavor(MediaPackageElementFlavor.parseFlavor(flavor));
    }
  }

  /**
   * Specifies an explicit video flavor.
   * 
   * @param flavor
   *          the flavor
   */
  public void setVideoFlavor(MediaPackageElementFlavor flavor) {
    if (flavor != null)
      addFlavor(flavor);
    videoFlavor = flavor;
  }

  /**
   * Returns the explicit video flavor or <code>null</code> if none was
   * specified.
   * 
   * @return the video flavor
   */
  public MediaPackageElementFlavor getVideoFlavor() {
    return videoFlavor;
  }

  /**
   * Returns a track or a number of tracks from the media package that together
   * contain audio and video. If no such combination can be found, e. g. there
   * is no audio or video at all, an empty array is returned.
   * 
   * @see org.opencastproject.media.mediapackage.selector.AbstractMediaPackageElementSelector#select(org.opencastproject.media.mediapackage.MediaPackage)
   */
  @Override
  public Collection<Track> select(MediaPackage mediaPackage) {
    // instead of relying on the broken superclass, we'll inspect every track
    // Collection<Track> candidates = super.select(mediaPackage);
    Set<Track> result = new HashSet<Track>();

    MediaPackageElementFlavor intermediaryAudioFlavor = audioFlavor;
    MediaPackageElementFlavor intermediaryVideoFlavor = videoFlavor;
    boolean intermediary = false;

    boolean foundAudio = false;
    boolean foundVideo = false;
    
    // special handling for */* flavors
    if ("*".equals(audioFlavor.getType()) || "*".equals(audioFlavor.getSubtype())) {
      intermediaryAudioFlavor = videoFlavor;
      intermediary = true;
    } else if ("*".equals(audioFlavor.getType()) || "*".equals(audioFlavor.getSubtype())) {
      intermediaryVideoFlavor = audioFlavor;
      intermediary = true;
    }

    // Try to look up the best match first. If it fails, try again with wildcard flavors (if defined)
    result = select(mediaPackage, intermediaryAudioFlavor, intermediaryVideoFlavor);
    if (intermediary && (!foundAudio && requireAudio) || (!foundVideo && requireVideo)) {
      result = select(mediaPackage, audioFlavor, videoFlavor);
    }

    if ((!foundAudio && requireAudio) || (!foundVideo && requireVideo))
      result.clear();

    // We were lucky, a combination was found!
    return result;
  }

  /**
   * Returns a track or a number of tracks from the media package that together
   * contain audio and video. If no such combination can be found, e. g. there
   * is no audio or video at all, an empty array is returned.
   * 
   * @see org.opencastproject.media.mediapackage.selector.AbstractMediaPackageElementSelector#select(org.opencastproject.media.mediapackage.MediaPackage)
   */
  public Set<Track> select(MediaPackage mediaPackage, MediaPackageElementFlavor audioFlavor, MediaPackageElementFlavor videoFlavor) {
    Collection<Track> candidates = Arrays.asList(mediaPackage.getTracks());
    Set<Track> result = new HashSet<Track>();

    boolean foundAudio = false;
    boolean foundVideo = false;

    // Try to look for the perfect match: a track containing audio and video
    for (Track t : candidates) {
      if (audioFlavor == null && videoFlavor == null) {
        foundAudio |= t.hasAudio();
        foundVideo |= t.hasVideo();
        result.add(t);
      } else {
        if (audioFlavor != null && t.hasAudio() && audioFlavor.equals(t.getFlavor())) {
          foundAudio = true;
          result.add(t);
        }
        if (videoFlavor != null && t.hasVideo() && videoFlavor.equals(t.getFlavor())) {
          foundVideo = true;
          result.add(t);
        }
      }
    }

    if ((!foundAudio && requireAudio) || (!foundVideo && requireVideo))
      result.clear();

    // We were lucky, a combination was found!
    return result;
  }

}
