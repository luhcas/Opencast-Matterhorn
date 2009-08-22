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

package org.opencastproject.media.mediapackage.track;

import org.opencastproject.media.mediapackage.AbstractMediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.MediaPackageElements;
import org.opencastproject.media.mediapackage.Stream;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.UnknownFileTypeException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.IOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is the base implementation for a media track, which itself is part of a media package, representing e. g.
 * the speaker video or the slide presentation movie.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @author Christoph E. Driessen <ced@neopoly.de>
 * @version $Id: TrackImpl.java 2905 2009-07-15 16:16:05Z ced $
 */
public class TrackImpl extends AbstractMediaPackageElement implements Track {

  /** Serial version UID */
  private static final long serialVersionUID = -1092781733885994038L;

  /** The duration in milliseconds */
  protected long duration = -1L;

  private List<Stream> streams = new ArrayList<Stream>();

  /**
   * Creates a new track object.
   * 
   * @param flavor
   *          the track flavor
   * @param url
   *          the track location
   * @param checksum
   *          the track checksum
   * @param mimeType
   *          the track mime type
   */
  TrackImpl(MediaPackageElementFlavor flavor, MimeType mimeType, URL url, long size, Checksum checksum) {
    super(Type.Track, flavor, url, size, checksum, mimeType);
  }

  /**
   * Creates a new track object for the given file and track type.
   * 
   * @param flavor
   *          the track flavor
   * @param url
   *          the track location
   * @throws IOException
   *           if the track file cannot be accessed
   * @throws UnknownFileTypeException
   *           if the file is of an unknown file type
   * @throws NoSuchAlgorithmException
   *           if the track's checksum cannot be computed
   */
  TrackImpl(MediaPackageElementFlavor flavor, URL url) {
    super(Type.Track, flavor, url);
  }

  /**
   * Creates a new track from the given url.
   * 
   * @param url
   *          the track location
   * @return the track
   */
  public static TrackImpl fromURL(URL url) {
    return new TrackImpl(MediaPackageElements.INDEFINITE_TRACK, url);
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
   * @see org.opencastproject.media.mediapackage.Track#getDuration()
   */
  public long getDuration() {
    return duration;
  }

  public Stream[] getStreams() {
    return streams.toArray(new Stream[streams.size()]);
  }

  /**
   * Add a stream to the track.
   */
  public void addStream(AbstractStreamImpl stream) {
    streams.add(stream);
  }

  /**
   * @see org.opencastproject.media.mediapackage.AbstractMediaPackageElement#toManifest(org.w3c.dom.Document)
   */
  @Override
  public Node toManifest(Document document) {
    Node node = super.toManifest(document);

    // duration
    Node durationNode = document.createElement("duration");
    durationNode.appendChild(document.createTextNode(Long.toString(duration)));
    node.appendChild(durationNode);

    for (Stream s : streams)
      node.appendChild(s.toManifest(document));
    return node;
  }

  /**
   * This implementation returns the track's mime type.
   * 
   * @see org.opencastproject.media.mediapackage.Track#getDescription()
   */
  public String getDescription() {
    StringBuffer buf = new StringBuffer("");
    /*
     * todo boolean details = false; if (hasVideo()) { details = true; buf.append(videoSettings); } if (hasAudio()) {
     * String audioCodec = audioSettings.toString(); if (!hasVideo() || !audioCodec.equals(videoSettings.toString())) {
     * if (details) buf.append(" / "); details = true; buf.append(audioCodec); } } if (!details) {
     * buf.append(getMimeType()); }
     */
    return buf.toString().toLowerCase();
  }

  /**
   * @see java.lang.Object#clone() todo
   */
  // @Override
  // public Object clone() throws CloneNotSupportedException {
  // TrackImpl t = null;
  // try {
  // t = new TrackImpl(flavor, mimeType, new File(path, fileName), checksum);
  // t.duration = duration;
  // // todo
  // //t.audioSettings = (AudioSettings)audioSettings.clone();
  // //t.videoSettings = (VideoSettings)videoSettings.clone();
  // } catch (Exception e) {
  // throw new IllegalStateException("Illegal state while cloning track: " + t);
  // }
  // return super.clone();
  // }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "track '" + getFlavor() + "'";
  }
}