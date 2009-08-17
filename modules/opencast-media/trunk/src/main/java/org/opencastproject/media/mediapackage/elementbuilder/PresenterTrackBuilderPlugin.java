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
import org.opencastproject.media.mediapackage.MediaPackageElements;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.UnknownFileTypeException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import javax.xml.xpath.XPathExpressionException;

/**
 * This implementation of the {@link MediaPackageElementBuilderPlugin} recognises movie tracks and provides the
 * functionality of reading it on behalf of the media package.
 * <p/>
 * The test currently depends on the filename and mimetype only.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: PresenterTrackBuilderPlugin.java 2905 2009-07-15 16:16:05Z ced $
 */
public class PresenterTrackBuilderPlugin extends AbstractTrackBuilderPlugin {

  /** the logging facility provided by log4j */
  private final static Logger log_ = LoggerFactory.getLogger(PresenterTrackBuilderPlugin.class.getName());

  private static final String FILENAME_PREFIX = "presenter";

  public PresenterTrackBuilderPlugin() throws IllegalStateException {
    setPriority(1);
  }

  @Override
  protected TrackImpl newTrack(File trackPath, MimeType mimeType, Checksum checksum) throws NoSuchAlgorithmException,
          IOException, UnknownFileTypeException {
    return new TrackImpl(MediaPackageElements.PRESENTER_TRACK, mimeType, trackPath, checksum);
  }

  public boolean accept(MediaPackageElement.Type type, MediaPackageElementFlavor flavor) {
    return type.equals(MediaPackageElement.Type.Track) && flavor.equals(MediaPackageElements.PRESENTER_TRACK);
  }

  public boolean accept(Node elementNode) {
    try {
      String name = elementNode.getNodeName();
      String flavor = xpath.evaluate("@type", elementNode);
      return name.equalsIgnoreCase(MediaPackageElement.Type.Track.toString()) && MediaPackageElements.PRESENTER_TRACK.eq(flavor);
    } catch (XPathExpressionException e) {
      return false;
    }
  }

  public boolean accept(File file, MediaPackageElement.Type type, MediaPackageElementFlavor flavor) throws IOException {
    if (type == null || flavor == null)
      return false;
    else if ((!type.equals(MediaPackageElement.Type.Track) || !flavor.equals(MediaPackageElements.PRESENTER_TRACK))
            && !checkFilenamePrefix(file, FILENAME_PREFIX))
      return false;

    // TODO Make sure we accept only files that we want in the system

    // Finally, see if we can analyze the track file and find a video track
    MediaContainerMetadata metadata = getMediaMetadata(file);
    return metadata != null && metadata.hasVideoStreamMetadata();
  }

  public MediaPackageElement elementFromFile(File file) throws MediaPackageException {
    try {
      log_.trace("Creating presenter track from " + file);
      TrackImpl track = new TrackImpl(MediaPackageElements.PRESENTER_TRACK, file);
      addMediaInfo(track);
      return track;
    } catch (IOException e) {
      throw new MediaPackageException("Error reading presenter track from " + file + " : " + e.getMessage());
    } catch (UnknownFileTypeException e) {
      throw new MediaPackageException("Presenter track " + file + " has an unknown file type: " + e.getMessage());
    } catch (NoSuchAlgorithmException e) {
      throw new MediaPackageException("Presenter track " + file + " cannot be checksummed: " + e.getMessage());
    } catch (MediaAnalyzerException e) {
      throw new MediaPackageException("Error extracting media information from " + file + ": " + e.getMessage());
    }
  }

  @Override
  public String toString() {
    return "Presenter Track Builder Plugin";
  }
}