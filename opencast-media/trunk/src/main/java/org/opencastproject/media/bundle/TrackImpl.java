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

package org.opencastproject.media.bundle;

import org.opencastproject.util.Checksum;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.UnknownFileTypeException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * This class is the base implementation for a media track, which itself is part
 * of a bundle, representing e. g. the speaker video or the slide presentation
 * movie.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public class TrackImpl extends AbstractBundleElement implements Track,
    AudioVisualTrack {

  /** Serial version UID */
  private static final long serialVersionUID = -1092781733885994038L;

  /** The duration in milliseconds */
  protected long duration = -1L;

  /** The track's audio settings */
  protected AudioSettings audioSettings = null;

  /** The track's video settings */
  protected VideoSettings videoSettings = null;

  /**
   * Creates a new track object.
   * 
   * @param flavor
   *          the track flavor
   * @param file
   *          the file
   * @param checksum
   *          the file's checksum
   * @throws IOException
   *           if the track file cannot be accessed
   * @throws UnknownFileTypeException
   *           if the file is of an unknown file type
   * @throws NoSuchAlgorithmException
   *           if the track's checksum cannot be computed
   */
  protected TrackImpl(BundleElementFlavor flavor, File file, Checksum checksum)
      throws IOException, UnknownFileTypeException, NoSuchAlgorithmException {
    super(Type.Track, flavor, file, checksum);
  }

  /**
   * Creates a new track object.
   * 
   * @param flavor
   *          the track flavor
   * @param mimeType
   *          the file's mime type
   * @param file
   *          the file
   * @param checksum
   *          the file's checksum
   * @throws IOException
   *           if the track file cannot be accessed
   * @throws UnknownFileTypeException
   *           if the file is of an unknown file type
   */
  protected TrackImpl(BundleElementFlavor flavor, MimeType mimeType, File file,
      Checksum checksum) throws IOException, UnknownFileTypeException {
    super(Type.Track, flavor, mimeType, file, checksum);
  }

  /**
   * Creates a new track object.
   * 
   * @param flavor
   *          the track flavor
   * @param mimeType
   *          the file's mime type
   * @param file
   *          the file
   * @throws IOException
   *           if the track file cannot be accessed
   * @throws NoSuchAlgorithmException
   *           if the track's checksum cannot be computed
   */
  protected TrackImpl(BundleElementFlavor flavor, MimeType mimeType, File file)
      throws IOException, NoSuchAlgorithmException {
    super(Type.Track, flavor, mimeType, file);
  }

  /**
   * Creates a new track object.
   * 
   * @param id
   *          the track identifier within the bundle
   * @param flavor
   *          the track flavor
   * @param mimeType
   *          the file's mime type
   * @param file
   *          the file
   * @param checksum
   *          the file's checksum
   * @throws IOException
   *           if the track file cannot be accessed
   */
  protected TrackImpl(String id, BundleElementFlavor flavor, MimeType mimeType,
      File file, Checksum checksum) throws IOException {
    super(id, Type.Track, flavor, mimeType, file, checksum);
  }

  /**
   * Creates a new track object for the given file and track type.
   * 
   * @param flavor
   *          the track flavor
   * @param track
   *          the file
   * @throws IOException
   *           if the track file cannot be accessed
   * @throws UnknownFileTypeException
   *           if the file is of an unknown file type
   * @throws NoSuchAlgorithmException
   *           if the track's checksum cannot be computed
   */
  protected TrackImpl(BundleElementFlavor flavor, File track)
      throws IOException, UnknownFileTypeException, NoSuchAlgorithmException {
    super(Type.Track, flavor, track);
  }

  /**
   * Sets the track's duration in milliseconds.
   * 
   * @param duration
   *          the duration
   */
  public void setDuration(long duration) {
    this.duration = duration;
  }

  /**
   * @see org.opencastproject.media.bundle.Track#getDuration()
   */
  public long getDuration() {
    return duration;
  }

  /**
   * @see org.opencastproject.media.bundle.Track#hasAudio()
   */
  public boolean hasAudio() {
    return audioSettings != null;
  }

  /**
   * @see org.opencastproject.media.bundle.Track#hasVideo()
   */
  public boolean hasVideo() {
    return videoSettings != null;
  }

  /**
   * Returns the track's audio settings.
   * 
   * @return the audio settings
   */
  public AudioSettings getAudioSettings() {
    return audioSettings;
  }

  /**
   * Sets the track's audio settings.
   * 
   * @param settings
   *          the audio settings
   */
  public void setAudioSettings(AudioSettings settings) {
    audioSettings = settings;
  }

  /**
   * Returns the track's video settings.
   * 
   * @return the video settings
   */
  public VideoSettings getVideoSettings() {
    return videoSettings;
  }

  /**
   * Sets the track's video settings.
   * 
   * @param settings
   *          the video settings
   */
  public void setVideoSettings(VideoSettings settings) {
    videoSettings = settings;
  }

  /**
   * @see java.lang.Object#clone()
   */
  @Override
  public Object clone() throws CloneNotSupportedException {
    TrackImpl t = null;
    try {
      t = new TrackImpl(flavor, mimeType, new File(path, fileName), checksum);
      t.duration = duration;
      t.audioSettings = (AudioSettings) audioSettings.clone();
      t.videoSettings = (VideoSettings) videoSettings.clone();
    } catch (Exception e) {
      throw new IllegalStateException("Illegal state while cloning track: " + t);
    }
    return super.clone();
  }

  /**
   * @see org.opencastproject.media.bundle.AbstractBundleElement#toManifest(org.w3c.dom.Document)
   */
  @Override
  public Node toManifest(Document document) {
    Node node = super.toManifest(document);

    // File
    Element fileNode = document.createElement("File");
    String trackPath = PathSupport.concat(getFile().getParentFile().getName(),
        fileName);
    fileNode.appendChild(document.createTextNode(trackPath));
    node.appendChild(fileNode);

    // duration
    Node durationNode = document.createElement("Duration");
    durationNode.appendChild(document.createTextNode(Long.toString(duration)));
    node.appendChild(durationNode);

    if (audioSettings != null)
      node.appendChild(audioSettings.toManifest(document));
    if (videoSettings != null)
      node.appendChild(videoSettings.toManifest(document));
    return node;
  }

  /**
   * This implementation returns the track's mime type.
   * 
   * @see org.opencastproject.media.bundle.Track#getDescription()
   */
  public String getDescription() {
    StringBuffer buf = new StringBuffer("");
    boolean details = false;
    if (hasVideo()) {
      details = true;
      buf.append(videoSettings);
    }
    if (hasAudio()) {
      String audioCodec = audioSettings.toString();
      if (!hasVideo() || !audioCodec.equals(videoSettings.toString())) {
        if (details)
          buf.append(" / ");
        details = true;
        buf.append(audioCodec);
      }
    }
    if (!details) {
      buf.append(getMimeType());
    }
    return buf.toString().toLowerCase();
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuffer b = new StringBuffer();
    if (audioSettings != null)
      b.append("audio");
    if (videoSettings != null)
      b.append("visual");
    b.append(" track '");
    b.append(getIdentifier());
    b.append("'");
    return b.toString();
  }

}