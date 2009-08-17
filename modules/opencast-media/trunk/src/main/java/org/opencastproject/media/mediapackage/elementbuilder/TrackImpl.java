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

package org.opencastproject.media.mediapackage.elementbuilder;

import org.opencastproject.media.mediapackage.AbstractMediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.Stream;
import org.opencastproject.media.mediapackage.Track;
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
class TrackImpl extends AbstractMediaPackageElement implements Track {

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
  TrackImpl(MediaPackageElementFlavor flavor, MimeType mimeType, File file, Checksum checksum) throws IOException,
          UnknownFileTypeException {
    super(Type.Track, flavor, mimeType, file, checksum);
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
  TrackImpl(MediaPackageElementFlavor flavor, File track) throws IOException, UnknownFileTypeException,
          NoSuchAlgorithmException {
    super(Type.Track, flavor, track);
  }

  /**
   * Sets the track's duration in milliseconds.
   * 
   * @param duration
   *          the duration
   */
  protected void setDuration(long duration) {
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
  protected void addStream(AbstractStreamImpl stream) {
    streams.add(stream);
  }

  /**
   * @see org.opencastproject.media.mediapackage.AbstractMediaPackageElement#toManifest(org.w3c.dom.Document)
   */
  @Override
  public Node toManifest(Document document) {
    Node node = super.toManifest(document);

    // File
    Element fileNode = document.createElement("url");
    String trackPath = PathSupport.concat(getFile().getParentFile().getName(), fileName);
    fileNode.appendChild(document.createTextNode(trackPath));
    node.appendChild(fileNode);

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