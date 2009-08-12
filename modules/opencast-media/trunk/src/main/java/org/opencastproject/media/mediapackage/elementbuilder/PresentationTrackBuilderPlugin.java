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

import org.opencastproject.media.analysis.MediaAnalyzerException;
import org.opencastproject.media.analysis.MediaContainerMetadata;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.media.mediapackage.MediaPackageElements;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.UnknownFileTypeException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPathExpressionException;

/**
 * This implementation of the {@link MediaPackageElementBuilderPlugin} recognises presentation tracks and provides the
 * functionality of reading it on behalf of the media package.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: PresentationTrackBuilderPlugin.java 2905 2009-07-15 16:16:05Z ced $
 */
public class PresentationTrackBuilderPlugin extends AbstractTrackBuilderPlugin {

  /**
   * The mime type identifier
   */
  // TODO: Make this configurable
  private static String[] MIME_TYPES = { MimeTypes.MJPEG2000.toString(), MimeTypes.MPEG4.toString(),
          MimeTypes.MPEG4_AAC.toString(), MimeTypes.DV.toString() };

  /** Prefix for presentation track filenames */
  private String FILENAME_PREFIX = "presentation";

  /** Presentation track mime type */
  private static MimeType[] mimeTypes = null;

  /** the logging facility provided by log4j */
  private final static Logger log_ = LoggerFactory.getLogger(PresentationTrackBuilderPlugin.class.getName());

  public PresentationTrackBuilderPlugin() throws IllegalStateException {
    setPriority(1);
  }

  /**
   * This method does the setup of mime types that is required for the plugin.
   * 
   * @see org.opencastproject.media.mediapackage.elementbuilder.AbstractElementBuilderPlugin#setup()
   */
  @Override
  public void setup() throws Exception {
    super.setup();
    if (mimeTypes == null) {
      List<MimeType> types = new ArrayList<MimeType>();
      for (String m : MIME_TYPES) {
        try {
          MimeType mimeType = MimeTypes.parseMimeType(m);
          mimeType.setFlavor(MediaPackageElements.PRESENTATION_TRACK.getSubtype(),
                  MediaPackageElements.PRESENTATION_TRACK.getDescription());
          types.add(mimeType);
          log_.debug("Enabling presentation tracks of type " + m);
        } catch (Exception e) {
          log_.warn("Unable to create presentation tracks of type mimetype " + m);
        }
      }
      mimeTypes = types.toArray(new MimeType[types.size()]);
    }
  }

  @Override
  protected TrackImpl newTrack(File trackPath, MimeType mimeType, Checksum checksum) throws NoSuchAlgorithmException,
          IOException, UnknownFileTypeException {
    return new TrackImpl(MediaPackageElements.PRESENTATION_TRACK, mimeType, trackPath, checksum);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElementBuilderPlugin#accept(org.opencastproject.media.mediapackage.MediaPackageElement.Type
   *      , org.opencastproject.media.mediapackage.MediaPackageElementFlavor)
   */
  public boolean accept(MediaPackageElement.Type type, MediaPackageElementFlavor flavor) {
    return type.equals(MediaPackageElement.Type.Track) && flavor.equals(MediaPackageElements.PRESENTATION_TRACK);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElementBuilderPlugin#accept(org.w3c.dom.Node)
   */
  public boolean accept(Node elementNode) {
    try {
      String name = elementNode.getNodeName();
      String flavor = xpath.evaluate("@type", elementNode);
      return name.equals(MediaPackageElement.Type.Track.toString())
              && MediaPackageElements.PRESENTATION_TRACK.eq(flavor);
    } catch (XPathExpressionException e) {
      return false;
    }
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElementBuilderPlugin#accept(File,
   *      org.opencastproject.media.mediapackage.MediaPackageElement.Type , MediaPackageElementFlavor)
   */
  public boolean accept(File file, MediaPackageElement.Type type, MediaPackageElementFlavor flavor) throws IOException {
    if (type == null || flavor == null)
      return false;
    else if ((!type.equals(MediaPackageElement.Type.Track) || !flavor.equals(MediaPackageElements.PRESENTATION_TRACK))
            && !checkFilenamePrefix(file, FILENAME_PREFIX))
      return false;

    // TODO Make sure we accept only files that we want in the system

    // Finally, see if we can analyze the track file and find a video track
    MediaContainerMetadata metadata = getMediaMetadata(file);
    return metadata != null && metadata.hasVideoStreamMetadata();
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElementBuilderPlugin#elementFromFile(File)
   */
  public MediaPackageElement elementFromFile(File file) throws MediaPackageException {
    try {
      log_.trace("Creating presentation track from " + file);
      TrackImpl track = new TrackImpl(MediaPackageElements.PRESENTATION_TRACK, file);
      addMediaInfo(track);
      return track;
    } catch (IOException e) {
      throw new MediaPackageException("Error reading presentation track from " + file + " : " + e.getMessage());
    } catch (UnknownFileTypeException e) {
      throw new MediaPackageException("Presentation track " + file + " has an unknown file type: " + e.getMessage());
    } catch (NoSuchAlgorithmException e) {
      throw new MediaPackageException("Presentation track " + file + " cannot be checksummed: " + e.getMessage());
    } catch (MediaAnalyzerException e) {
      throw new MediaPackageException("Error extracting media information from " + file + ": " + e.getMessage());
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