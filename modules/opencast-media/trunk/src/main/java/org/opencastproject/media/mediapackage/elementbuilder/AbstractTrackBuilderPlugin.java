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

import java.io.IOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;

import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.media.mediapackage.MediaPackageReferenceImpl;
import org.opencastproject.media.mediapackage.MediaPackageSerializer;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.media.mediapackage.track.AudioStreamImpl;
import org.opencastproject.media.mediapackage.track.TrackImpl;
import org.opencastproject.media.mediapackage.track.VideoStreamImpl;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.UnknownFileTypeException;
import org.w3c.dom.Node;

/**
 * Abstract base class for the various track builders.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @author Christoph E. Driessen <ced@neopoly.de>
 * @version $Id: AbstractTrackBuilderPlugin.java 2914 2009-07-22 14:58:07Z ced $
 */
public abstract class AbstractTrackBuilderPlugin extends AbstractElementBuilderPlugin {

  /**
   * Creates a new instance of an abstract track builder plugin.
   * 
   * @throws IllegalStateException
   *           in case of not being able to initialize
   */
  protected AbstractTrackBuilderPlugin() throws IllegalStateException { }

  /**
   * Creates a track object from the given url. The method is called when the plugin reads the track information from
   * the manifest.
   * 
   * @param id
   *          the track id
   * @param url
   *          the track location
   * @return
   */
  protected abstract TrackImpl trackFromManifest(String id, URL url);

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElementBuilderPlugin#newElement(org.opencastproject.media.mediapackage.MediaPackageElement.Type
   *      ,org.opencastproject.media.mediapackage.MediaPackageElementFlavor)
   */
  public MediaPackageElement newElement(MediaPackageElement.Type type, MediaPackageElementFlavor flavor)
          throws IOException {
    throw new IllegalStateException("Unable to create track from scratch");
  }

  /**
   * @see org.opencastproject.media.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin#elementFromManifest(org.w3c.dom.Node, org.opencastproject.media.mediapackage.MediaPackageSerializer)
   */
  public MediaPackageElement elementFromManifest(Node elementNode, MediaPackageSerializer serializer) throws MediaPackageException {

    String id = null;
    MimeType mimeType = null;
    String reference = null;
    URL url = null;
    long size = -1;
    Checksum checksum = null;

    try {
      // id
      id = (String) xpath.evaluate("@id", elementNode, XPathConstants.STRING);

      // url
      url = serializer.resolve(xpath.evaluate("url/text()", elementNode).trim());

      // reference
      reference = (String) xpath.evaluate("@ref", elementNode, XPathConstants.STRING);

      // size
      try {
        size = Long.parseLong(xpath.evaluate("size/text()", elementNode).trim());
      } catch (Exception e) {
        // size may not be present
      }

      // checksum
      String checksumValue = (String) xpath.evaluate("checksum/text()", elementNode, XPathConstants.STRING);
      String checksumType = (String) xpath.evaluate("checksum/@type", elementNode, XPathConstants.STRING);
      if (checksumValue != null && checksumType != null)
        checksum = Checksum.create(checksumType.trim(), checksumValue.trim());

      // mimetype
      String mimeTypeValue = (String) xpath.evaluate("mimetype/text()", elementNode, XPathConstants.STRING);
      if (mimeTypeValue != null)
        mimeType = MimeTypes.parseMimeType(mimeTypeValue);

      //
      // Build the track

      TrackImpl track = trackFromManifest(id, url);
      if (id != null && !id.equals(""))
        track.setIdentifier(id);

      // Add url
      track.setURL(url);

      // Add reference
      if (reference != null && !reference.equals(""))
        track.referTo(MediaPackageReferenceImpl.fromString(reference));

      // Set size
      if (size > 0)
        track.setSize(size);

      // Set checksum
      if (checksum != null)
        track.setChecksum(checksum);

      // Set mimetpye
      if (mimeType != null)
        track.setMimeType(mimeType);

      // description
      String description = (String) xpath.evaluate("description/text()", elementNode, XPathConstants.STRING);
      if (description != null && !description.trim().equals(""))
        track.setElementDescription(description.trim());

      // duration
      try {
        String strDuration = (String) xpath.evaluate("duration/text()", elementNode, XPathConstants.STRING);
        long duration = Long.parseLong(strDuration.trim());
        if (duration <= 0)
          throw new MediaPackageException("Invalid duration for track " + url + " found in manifest: " + duration);
        track.setDuration(duration);
      } catch (NumberFormatException e) {
        throw new MediaPackageException("Duration of track " + url + " is malformatted");
      }

      // audio settings
      Node audioSettingsNode = (Node) xpath.evaluate("audio", elementNode, XPathConstants.NODE);
      if (audioSettingsNode != null && audioSettingsNode.hasChildNodes()) {
        try {
          AudioStreamImpl as = AudioStreamImpl.fromManifest(createStreamID(track), audioSettingsNode, xpath);
          track.addStream(as);
        } catch (IllegalStateException e) {
          throw new MediaPackageException("Illegal state encountered while reading audio settings from " + url + ": "
                  + e.getMessage());
        } catch (XPathException e) {
          throw new MediaPackageException("Error while parsing audio settings from " + url + ": " + e.getMessage());
        }
      }

      // video settings
      Node videoSettingsNode = (Node) xpath.evaluate("video", elementNode, XPathConstants.NODE);
      if (videoSettingsNode != null && videoSettingsNode.hasChildNodes()) {
        try {
          VideoStreamImpl vs = VideoStreamImpl.fromManifest(createStreamID(track), videoSettingsNode, xpath);
          track.addStream(vs);
        } catch (IllegalStateException e) {
          throw new MediaPackageException("Illegal state encountered while reading video settings from " + url + ": "
                  + e.getMessage());
        } catch (XPathException e) {
          throw new MediaPackageException("Error while parsing video settings from " + url + ": " + e.getMessage());
        }
      }

      return track;
    } catch (XPathExpressionException e) {
      throw new MediaPackageException("Error while reading track information from manifest: " + e.getMessage());
    } catch (NoSuchAlgorithmException e) {
      throw new MediaPackageException("Unsupported digest algorithm: " + e.getMessage());
    } catch (IOException e) {
      throw new MediaPackageException("Error while reading presenter track " + url + ": " + e.getMessage());
    } catch (UnknownFileTypeException e) {
      throw new MediaPackageException("Track " + url + " is of unknown mime type: " + e.getMessage());
    }
  }

  private String createStreamID(Track track) {
    return "stream-" + (track.getStreams().length + 1);
  }

}