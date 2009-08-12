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

import org.opencastproject.media.analysis.AudioStreamMetadata;
import org.opencastproject.media.analysis.MediaAnalyzer;
import org.opencastproject.media.analysis.MediaAnalyzerException;
import org.opencastproject.media.analysis.MediaAnalyzerFactory;
import org.opencastproject.media.analysis.MediaContainerMetadata;
import org.opencastproject.media.analysis.VideoStreamMetadata;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.media.mediapackage.MediaPackageReferenceImpl;
import org.opencastproject.media.mediapackage.Track;
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

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;

/**
 * Abstract base class for the various track builders.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @author Christoph E. Driessen <ced@neopoly.de>
 * @version $Id: AbstractTrackBuilderPlugin.java 2914 2009-07-22 14:58:07Z ced $
 */
public abstract class AbstractTrackBuilderPlugin extends AbstractElementBuilderPlugin {

  /**
   * The media analyzer
   */
  private MediaAnalyzer mediaAnalyzer = null;

  /**
   * The media information
   */
  protected MediaContainerMetadata mediaInfo = null;

  /**
   * The analyzed file
   */
  protected File analyzedFile = null;

  /**
   * the logging facility provided by log4j
   */
  private final static Logger log_ = LoggerFactory.getLogger(AbstractTrackBuilderPlugin.class.getName());

  /**
   * Creates a new instance of an abstract track builder plugin.
   * 
   * @throws IllegalStateException
   *           in case of not being able to initialize
   */
  protected AbstractTrackBuilderPlugin() throws IllegalStateException {
    try {
      MediaAnalyzerFactory analyzerFactory = MediaAnalyzerFactory.newInstance();
      mediaAnalyzer = analyzerFactory.newMediaAnalyzer();
    } catch (Throwable t) {
      throw new IllegalStateException("Unable to create media analyzer: " + t.getMessage());
    }
  }

  /**
   * Create a new track implementation.
   * 
   * @param trackPath
   *          the track file
   * @param mimeType
   *          the mime type
   * @param checksum
   *          the checksum
   * @return the track implementation
   */
  protected abstract TrackImpl newTrack(File trackPath, MimeType mimeType, Checksum checksum)
          throws NoSuchAlgorithmException, IOException, UnknownFileTypeException;

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElementBuilderPlugin#newElement(org.opencastproject.media.mediapackage.MediaPackageElement.Type
   *      ,org.opencastproject.media.mediapackage.MediaPackageElementFlavor)
   */
  public MediaPackageElement newElement(MediaPackageElement.Type type, MediaPackageElementFlavor flavor)
          throws IOException {
    throw new IllegalStateException("Unable to create track from scratch");
  }

  public MediaPackageElement elementFromManifest(Node elementNode, File packageRoot, boolean verify)
          throws MediaPackageException {
    String trackId = null;
    String trackPath = null;
    String mimeType = null;
    String reference = null;
    try {
      // id
      trackId = (String) xpath.evaluate("@id", elementNode, XPathConstants.STRING);

      // flavor
      String flavor = xpath.evaluate("@type", elementNode);

      // mime type
      mimeType = (String) xpath.evaluate("MimeType/text()", elementNode, XPathConstants.STRING);
      mimeType = mimeType.trim();

      // file
      trackPath = (String) xpath.evaluate("File/text()", elementNode, XPathConstants.STRING);
      trackPath = PathSupport.concat(packageRoot.getAbsolutePath(), trackPath.trim());

      // reference
      reference = (String) xpath.evaluate("@ref", elementNode, XPathConstants.STRING);

      // checksum
      String checksumValue = (String) xpath.evaluate("Checksum/text()", elementNode, XPathConstants.STRING);
      String checksumType = (String) xpath.evaluate("Checksum/@type", elementNode, XPathConstants.STRING);
      Checksum checksum = Checksum.create(checksumType.trim(), checksumValue.trim());

      // verify the track
      if (verify) {
        log_.debug("Verifying integrity of '" + flavor + "'" + trackPath);
        verifyFileIntegrity(new File(trackPath), checksum);
      }

      //
      // Build the track

      TrackImpl track = newTrack(new File(trackPath), MimeTypes.parseMimeType(mimeType), checksum);
      if (trackId != null && !trackId.equals(""))
        track.setIdentifier(trackId);

      // Add reference
      if (reference != null && !reference.equals(""))
        track.referTo(MediaPackageReferenceImpl.fromString(reference));

      // description
      String description = (String) xpath.evaluate("Description/text()", elementNode, XPathConstants.STRING);
      if (description != null && !description.trim().equals(""))
        track.setElementDescription(description.trim());

      // duration
      try {
        String strDuration = (String) xpath.evaluate("Duration/text()", elementNode, XPathConstants.STRING);
        long duration = Long.parseLong(strDuration.trim());
        if (duration <= 0)
          throw new MediaPackageException("Invalid duration for track " + trackPath + " found in manifest: " + duration);
        track.setDuration(duration);
      } catch (NumberFormatException e) {
        throw new MediaPackageException("Duration of track " + trackPath + " is malformatted");
      }

      // audio settings
      Node audioSettingsNode = (Node) xpath.evaluate("Audio", elementNode, XPathConstants.NODE);
      if (audioSettingsNode != null && audioSettingsNode.hasChildNodes()) {
        try {
          AudioStreamImpl as = AudioStreamImpl.fromManifest(createStreamID(track), audioSettingsNode, xpath);
          track.addStream(as);
        } catch (IllegalStateException e) {
          throw new MediaPackageException("Illegal state encountered while reading audio settings from " + trackPath
                  + ": " + e.getMessage());
        } catch (XPathException e) {
          throw new MediaPackageException("Error while parsing audio settings from " + trackPath + ": "
                  + e.getMessage());
        }
      }

      // video settings
      Node videoSettingsNode = (Node) xpath.evaluate("Video", elementNode, XPathConstants.NODE);
      if (videoSettingsNode != null && videoSettingsNode.hasChildNodes()) {
        try {
          VideoStreamImpl vs = VideoStreamImpl.fromManifest(createStreamID(track), videoSettingsNode, xpath);
          track.addStream(vs);
        } catch (IllegalStateException e) {
          throw new MediaPackageException("Illegal state encountered while reading video settings from " + trackPath
                  + ": " + e.getMessage());
        } catch (XPathException e) {
          throw new MediaPackageException("Error while parsing video settings from " + trackPath + ": "
                  + e.getMessage());
        }
      }

      return track;
    } catch (XPathExpressionException e) {
      throw new MediaPackageException("Error while reading track information from manifest: " + e.getMessage());
    } catch (NoSuchAlgorithmException e) {
      throw new MediaPackageException("Unsupported digest algorithm: " + e.getMessage());
    } catch (UnknownFileTypeException e) {
      throw new ConfigurationException("MimeType " + mimeType + " is not supported: " + e.getMessage());
    } catch (IOException e) {
      throw new MediaPackageException("Error while reading presenter track " + trackPath + ": " + e.getMessage());
    }
  }

  private String createStreamID(Track track) {
    return "stream-" + (track.getStreams().length + 1);
  }

  /**
   * Returns the {@link org.opencastproject.media.analysis.MediaContainerMetadata} for this file or <code>null</code> if
   * the file is not a media track.
   * 
   * @param file
   *          the media file
   * @return the media info
   * @throws MediaAnalyzerException
   *           if the media could not be analyzed
   */
  protected MediaContainerMetadata getMediaMetadata(File file) throws MediaAnalyzerException {
    if (mediaInfo == null || !file.equals(analyzedFile)) {
      try {
        mediaInfo = mediaAnalyzer.analyze(file);
        analyzedFile = file;
      } catch (MediaAnalyzerException e) {
        log_.warn("Track " + file + " could not be analyzed: " + e.getMessage());
        throw e;
      }
    }
    return mediaInfo;
  }

  /**
   * Adds the media info extracted by the {@link org.opencastproject.media.analysis.MediaAnalyzer}.
   * 
   * @throws org.opencastproject.media.analysis.MediaAnalyzerException
   *           in case of an error
   */
  protected void addMediaInfo(TrackImpl track) throws MediaAnalyzerException {
    if (mediaInfo == null)
      throw new MediaAnalyzerException("Media analyzer returned no results for " + track);

    // Set the track duration
    if (mediaInfo.getDuration() == null)
      throw new MediaAnalyzerException("Media analyzer was unable to determine track duration for " + track);
    track.setDuration(mediaInfo.getDuration());

    // Video
    for (VideoStreamMetadata metadata : mediaInfo.getVideoStreamMetadata()) {
      track.addStream(from(createStreamID(track), metadata));
    }

    // Audio
    for (AudioStreamMetadata metadata : mediaInfo.getAudioStreamMetadata()) {
      track.addStream(from(createStreamID(track), metadata));
    }
  }

  private VideoStreamImpl from(String streamID, VideoStreamMetadata m) {
    VideoStreamImpl vs = new VideoStreamImpl(streamID);
    vs.setBitRate(m.getBitRate());
    vs.setCaptureDevice(m.getCaptureDevice());
    vs.setCaptureDeviceVendor(m.getCaptureDeviceVendor());
    vs.setCaptureDeviceVersion(m.getCaptureDeviceVersion());
    vs.setEncoderLibraryVendor(m.getEncoderLibraryVendor());
    vs.setFormat(m.getFormat());
    vs.setFormatVersion(m.getFormatVersion());
    vs.setFrameHeight(m.getFrameHeight());
    vs.setFrameRate(m.getFrameRate());
    vs.setFrameWidth(m.getFrameWidth());
    vs.setScanOrder(m.getScanOrder());
    vs.setScanType(m.getScanType());
    return vs;
  }

  private AudioStreamImpl from(String streamID, AudioStreamMetadata m) {
    AudioStreamImpl as = new AudioStreamImpl(streamID);
    as.setBitRate(m.getBitRate());
    as.setCaptureDevice(m.getCaptureDevice());
    as.setCaptureDeviceVendor(m.getCaptureDeviceVendor());
    as.setCaptureDeviceVersion(m.getCaptureDeviceVersion());
    as.setEncoderLibraryVendor(m.getEncoderLibraryVendor());
    as.setChannels(m.getChannels());
    as.setFormat(m.getFormat());
    as.setFormatVersion(m.getFormatVersion());
    as.setResolution(m.getResolution());
    as.setSamplingRate(m.getSamplingRate());
    return as;
  }
}