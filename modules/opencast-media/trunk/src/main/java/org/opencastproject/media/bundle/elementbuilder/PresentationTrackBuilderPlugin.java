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

package org.opencastproject.media.bundle.elementbuilder;

import org.opencastproject.media.analysis.MediaAnalyzerException;
import org.opencastproject.media.analysis.MediaContainerMetadata;
import org.opencastproject.media.bundle.AudioSettings;
import org.opencastproject.media.bundle.AudioSettingsImpl;
import org.opencastproject.media.bundle.BundleElement;
import org.opencastproject.media.bundle.BundleElementBuilderPlugin;
import org.opencastproject.media.bundle.BundleElementFlavor;
import org.opencastproject.media.bundle.BundleException;
import org.opencastproject.media.bundle.BundleReferenceImpl;
import org.opencastproject.media.bundle.PresentationTrack;
import org.opencastproject.media.bundle.PresentationTrackImpl;
import org.opencastproject.media.bundle.VideoSettings;
import org.opencastproject.media.bundle.VideoSettingsImpl;
import org.opencastproject.media.bundle.BundleElement.Type;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.UnknownFileTypeException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;

/**
 * This implementation of the {@link BundleElementBuilderPlugin} recognises
 * presentation tracks and provides the functionality of reading it on behalf of
 * the bundle.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: PresentationTrackBuilderPlugin.java 102 2009-04-03 20:05:36Z
 *          wunden $
 */
public class PresentationTrackBuilderPlugin extends AbstractTrackBuilderPlugin
    implements BundleElementBuilderPlugin {

  /** The mime type identifier */
  // TODO: Make this configurable
  private static String[] MIME_TYPES = { MimeTypes.MJPEG2000.toString(),
      MimeTypes.MPEG4.toString(), MimeTypes.MPEG4_AAC.toString(),
      MimeTypes.DV.toString() };

  /** Presentation track mime type */
  private static MimeType[] mimeTypes = null;

  /** the logging facility provided by log4j */
  private final static Logger log_ = LoggerFactory
      .getLogger(PresentationTrackBuilderPlugin.class);

  public PresentationTrackBuilderPlugin() throws IllegalStateException {
    setPriority(1);
  }

  /**
   * This method does the setup of mime types that is required for the plugin.
   * 
   * @see org.opencastproject.media.bundle.elementbuilder.AbstractElementBuilderPlugin#setup()
   */
  @Override
  public void setup() throws Exception {
    super.setup();
    if (mimeTypes == null) {
      List<MimeType> types = new ArrayList<MimeType>();
      for (String m : MIME_TYPES) {
        try {
          MimeType mimeType = MimeTypes.parseMimeType(m);
          mimeType.setFlavor(PresentationTrack.FLAVOR.getSubtype(),
              PresentationTrack.FLAVOR_DESCRIPTION);
          types.add(mimeType);
          log_.debug("Enabling presentation tracks of type " + m);
        } catch (Exception e) {
          log_.warn("Unable to create presentation tracks of type mimetype "
              + m);
        }
      }
      mimeTypes = types.toArray(new MimeType[types.size()]);
    }
  }

  /**
   * @see org.opencastproject.media.bundle.BundleElementBuilderPlugin#accept(org.opencastproject.media.bundle.BundleElement.Type,
   *      org.opencastproject.media.bundle.BundleElementFlavor)
   */
  public boolean accept(Type type, BundleElementFlavor flavor) {
    return type.equals(Type.Track) && flavor.equals(PresentationTrack.FLAVOR);
  }

  /**
   * @see org.opencastproject.media.bundle.BundleElementBuilderPlugin#accept(org.w3c.dom.Node)
   */
  public boolean accept(Node elementNode) {
    try {
      String name = elementNode.getNodeName();
      String flavor = xpath.evaluate("@type", elementNode);
      return name.equals(BundleElement.Type.Track.toString())
          && flavor.equals(PresentationTrack.FLAVOR.toString());
    } catch (XPathExpressionException e) {
      return false;
    }
  }

  /**
   * @see org.opencastproject.media.bundle.BundleElementBuilderPlugin#accept(File,
   *      Type, BundleElementFlavor)
   */
  public boolean accept(File file, BundleElement.Type type,
      BundleElementFlavor flavor) throws IOException {
    if (type == null || flavor == null)
      return false;
    else if ((!type.equals(BundleElement.Type.Track) || !flavor
        .equals(PresentationTrack.FLAVOR))
        && !checkFilenamePrefix(file, PresentationTrack.FILENAME_PREFIX))
      return false;

    // TODO Make sure we accept only files that we want in the system

    // Finally, see if we can analyze the track file and find a video track
    MediaContainerMetadata metadata = getMediaMetadata(file);
    return metadata != null && metadata.hasVideoStreamMetadata();
  }

  /**
   * @see org.opencastproject.media.bundle.BundleElementBuilderPlugin#elementFromManifest(org.w3c.dom.Node,
   *      java.io.File, boolean)
   */
  public BundleElement elementFromManifest(Node elementNode, File bundleRoot,
      boolean verify) throws BundleException {
    String trackId = null;
    String trackPath = null;
    String mimeType = null;
    String reference = null;
    try {
      // id
      trackId = (String) xpath.evaluate("@id", elementNode,
          XPathConstants.STRING);

      // mime type
      mimeType = (String) xpath.evaluate("MimeType/text()", elementNode,
          XPathConstants.STRING);
      mimeType = mimeType.trim();

      // file
      trackPath = (String) xpath.evaluate("File/text()", elementNode,
          XPathConstants.STRING);
      trackPath = PathSupport.concat(bundleRoot.getAbsolutePath(), trackPath
          .trim());

      // reference
      reference = (String) xpath.evaluate("@ref", elementNode,
          XPathConstants.STRING);

      // checksum
      String checksumValue = (String) xpath.evaluate("Checksum/text()",
          elementNode, XPathConstants.STRING);
      String checksumType = (String) xpath.evaluate("Checksum/@type",
          elementNode, XPathConstants.STRING);
      Checksum checksum = Checksum.create(checksumType.trim(), checksumValue
          .trim());

      // verify the track
      if (verify) {
        log_.debug("Verifying integrity of presentation track " + trackPath);
        verifyFileIntegrity(new File(trackPath), checksum);
      }

      //
      // Build the track

      PresentationTrackImpl track = PresentationTrackImpl.fromFile(new File(
          trackPath), MimeTypes.parseMimeType(mimeType), checksum);
      if (trackId != null && !trackId.equals(""))
        track.setIdentifier(trackId);

      // Add reference
      if (reference != null && !reference.equals(""))
        track.referTo(BundleReferenceImpl.fromString(reference));

      // description
      String description = (String) xpath.evaluate("Description/text()",
          elementNode, XPathConstants.STRING);
      if (description != null && !description.trim().equals(""))
        track.setElementDescription(description.trim());

      // duration
      try {
        String strDuration = (String) xpath.evaluate("Duration/text()",
            elementNode, XPathConstants.STRING);
        long duration = Long.parseLong(strDuration.trim());
        if (duration <= 0)
          throw new BundleException("Invalid duration for track " + trackPath
              + " found in manifest: " + duration);
        track.setDuration(duration);
      } catch (NumberFormatException e) {
        throw new BundleException("Duration of track " + trackPath
            + " is malformatted");
      }

      // audio settings
      Node audioSettingsNode = (Node) xpath.evaluate("Audio", elementNode,
          XPathConstants.NODE);
      if (audioSettingsNode != null && audioSettingsNode.hasChildNodes()) {
        try {
          AudioSettings settings = AudioSettingsImpl.fromManifest(
              audioSettingsNode, xpath);
          track.setAudioSettings(settings);
        } catch (IllegalStateException e) {
          throw new BundleException(
              "Illegal state encountered while reading audio settings from "
                  + trackPath + ": " + e.getMessage());
        } catch (XPathException e) {
          throw new BundleException("Error while parsing audio settings from "
              + trackPath + ": " + e.getMessage());
        }
      }

      // video settings
      Node videoSettingsNode = (Node) xpath.evaluate("Video", elementNode,
          XPathConstants.NODE);
      if (videoSettingsNode != null && videoSettingsNode.hasChildNodes()) {
        try {
          VideoSettings settings = VideoSettingsImpl.fromManifest(
              videoSettingsNode, xpath);
          track.setVideoSettings(settings);
        } catch (IllegalStateException e) {
          throw new BundleException(
              "Illegal state encountered while reading video settings from "
                  + trackPath + ": " + e.getMessage());
        } catch (XPathException e) {
          throw new BundleException("Error while parsing video settings from "
              + trackPath + ": " + e.getMessage());
        }
      }

      return track;
    } catch (XPathExpressionException e) {
      throw new BundleException(
          "Error while reading track information from manifest: "
              + e.getMessage());
    } catch (NoSuchAlgorithmException e) {
      throw new ConfigurationException(
          "Unable to calculate checksum for presenation track " + trackPath
              + ": " + e.getMessage());
    } catch (UnknownFileTypeException e) {
      throw new ConfigurationException("MimeType " + mimeType
          + " is not supported: " + e.getMessage());
    } catch (IOException e) {
      throw new BundleException("Error while reading presentation track "
          + trackPath + ": " + e.getMessage());
    }
  }

  /**
   * @see org.opencastproject.media.bundle.BundleElementBuilderPlugin#elementFromFile(File)
   */
  public BundleElement elementFromFile(File file) throws BundleException {
    try {
      log_.trace("Creating presentation track from " + file);
      PresentationTrackImpl track = PresentationTrackImpl.fromFile(file);
      addMediaInfo(track);
      return track;
    } catch (IOException e) {
      throw new BundleException("Error reading presentation track from " + file
          + " : " + e.getMessage());
    } catch (UnknownFileTypeException e) {
      throw new BundleException("Presentation track " + file
          + " has an unknown file type: " + e.getMessage());
    } catch (NoSuchAlgorithmException e) {
      throw new BundleException("Presentation track " + file
          + " cannot be checksummed: " + e.getMessage());
    } catch (MediaAnalyzerException e) {
      throw new BundleException("Error extracting media information from "
          + file + ": " + e.getMessage());
    }
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "Presentation Track Builder Plugin";
  }

}