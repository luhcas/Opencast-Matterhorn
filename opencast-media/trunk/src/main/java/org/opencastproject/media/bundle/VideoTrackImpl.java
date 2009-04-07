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

import org.opencastproject.util.Checksum;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.UnknownFileTypeException;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * Implementation of a video track.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public class VideoTrackImpl extends TrackImpl implements VideoTrack {

  /** Serial version UID */
  private static final long serialVersionUID = 1215537070849312626L;

  /**
   * Creates a new video track.
   * 
   * @param flavor
   *          the flavor
   * @param track
   *          the track file
   * @throws IOException
   *           if the file cannot be accessed
   * @throws UnknownFileTypeException
   *           if the track is of an unknown file format
   * @throws NoSuchAlgorithmException
   *           if the md5 checksum cannot be computed
   */
  protected VideoTrackImpl(BundleElementFlavor flavor, File track)
      throws IOException, UnknownFileTypeException, NoSuchAlgorithmException {
    super(flavor, track);
  }

  /**
   * Creates a new video track.
   * 
   * @param flavor
   *          the flavor
   * @param mimeType
   *          the track's mime type
   * @param track
   *          the track file
   * @param checksum
   *          the file checksum
   * @throws IOException
   *           if the file cannot be accessed
   * @throws UnknownFileTypeException
   *           if the track is of an unknown file format
   * @throws NoSuchAlgorithmException
   *           if the md5 checksum cannot be computed
   */
  protected VideoTrackImpl(BundleElementFlavor flavor, MimeType mimeType,
      File track, Checksum checksum) throws IOException,
      UnknownFileTypeException, NoSuchAlgorithmException {
    super(flavor, mimeType, track, checksum);
  }

  /**
   * Reads a track from the specified file and returns it encapsulated in a
   * {@link VideoTrack} object.
   * 
   * @param file
   *          the track file
   * @return the track object
   * @throws IOException
   *           if reading the track file fails
   * @throws UnknownFileTypeException
   *           if the track file is of an unknown file type
   * @throws NoSuchAlgorithmException
   *           if the md5 checksum cannot be computed
   */
  public static VideoTrackImpl fromFile(File file) throws IOException,
      UnknownFileTypeException, NoSuchAlgorithmException {
    return new VideoTrackImpl(VideoTrack.FLAVOR, file);
  }

  /**
   * Reads a track from the specified file and returns it encapsulated in a
   * {@link VideoTrack} object.
   * 
   * @param file
   *          the track file
   * @param mimeType
   *          the track's mime type
   * @param checksum
   *          the file checksum
   * @return the track object
   * @throws IOException
   *           if reading the track file fails
   * @throws UnknownFileTypeException
   *           if the track file is of an unknown file type
   * @throws NoSuchAlgorithmException
   *           if the md5 checksum cannot be computed
   */
  public static VideoTrackImpl fromFile(File file, MimeType mimeType,
      Checksum checksum) throws IOException, UnknownFileTypeException,
      NoSuchAlgorithmException {
    return new VideoTrackImpl(VideoTrack.FLAVOR, mimeType, file, checksum);
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer("video (");
    boolean details = false;
    if (hasVideo()) {
      details = true;
      buf.append(getVideoSettings().getMetadata().getFormat());
    }
    if (hasAudio() && getAudioSettings().getMetadata().getFormat() != null) {
      String audioCodec = getAudioSettings().getMetadata().getFormat();
      if (!hasVideo()
          || !audioCodec.equals(getVideoSettings().getMetadata().getFormat())) {
        if (details)
          buf.append("/");
        details = true;
        buf.append(audioCodec);
      }
    }
    if (!details) {
      buf.append(getMimeType());
    }
    buf.append(")");
    return buf.toString().toLowerCase();
  }

  /**
   * @see org.opencastproject.media.bundle.AudioTrack#getAudioSettings()
   */
  @Override
  public AudioSettings getAudioSettings() {
    return super.getAudioSettings();
  }

  /**
   * @see org.opencastproject.media.bundle.VideoTrack#getVideoSettings()
   */
  @Override
  public VideoSettings getVideoSettings() {
    return super.getVideoSettings();
  }

}