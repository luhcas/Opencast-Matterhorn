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

import org.opencastproject.media.analysis.AudioStreamMetadata;
import org.opencastproject.media.analysis.MediaAnalyzer;
import org.opencastproject.media.analysis.MediaAnalyzerException;
import org.opencastproject.media.analysis.MediaAnalyzerFactory;
import org.opencastproject.media.analysis.MediaContainerMetadata;
import org.opencastproject.media.analysis.VideoStreamMetadata;
import org.opencastproject.media.bundle.AudioSettingsImpl;
import org.opencastproject.media.bundle.BundleElement;
import org.opencastproject.media.bundle.BundleElementFlavor;
import org.opencastproject.media.bundle.TrackImpl;
import org.opencastproject.media.bundle.VideoSettingsImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Abstract base class for the various track builders.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: AbstractTrackBuilderPlugin.java 102 2009-04-03 20:05:36Z wunden
 *          $
 */
public abstract class AbstractTrackBuilderPlugin extends
    AbstractElementBuilderPlugin {

  /** The media analyzer */
  private MediaAnalyzer mediaAnalyzer = null;

  /** The media information */
  protected MediaContainerMetadata mediaInfo = null;

  /** The analyzed file */
  protected File analyzedFile = null;

  /** the logging facility provided by log4j */
  private final static Logger log_ = LoggerFactory
      .getLogger(AbstractTrackBuilderPlugin.class);

  /**
   * Creates a new instance of an abstract track builder plugin.
   * 
   * @throws IllegalStateException
   */
  protected AbstractTrackBuilderPlugin() throws IllegalStateException {
    try {
      MediaAnalyzerFactory analyzerFactory = MediaAnalyzerFactory.newInstance();
      mediaAnalyzer = analyzerFactory.newMediaAnalyzer();
    } catch (Throwable t) {
      throw new IllegalStateException("Unable to create media analyzer: "
          + t.getMessage());
    }
  }

  /**
   * @see org.opencastproject.media.bundle.BundleElementBuilderPlugin#newElement(org.opencastproject.media.bundle.BundleElement.Type,org.opencastproject.media.bundle.BundleElementFlavor)
   */
  public BundleElement newElement(BundleElement.Type type,
      BundleElementFlavor flavor) throws IOException {
    throw new IllegalStateException("Unable to create track from scratch");
  }

  /**
   * Returns the
   * {@link ch.ethz.replay.core.api.common.media.analysis.MediaContainerMetadata}
   * for this file or <code>null</code> if the file is not a media track.
   * 
   * @param file
   *          the media file
   * @return the media info
   * @throws MediaAnalyzerException
   *           if the media could not be analyzed
   */
  protected MediaContainerMetadata getMediaMetadata(File file)
      throws MediaAnalyzerException {
    if (mediaInfo == null || !file.equals(analyzedFile)) {
      try {
        mediaInfo = mediaAnalyzer.analyze(file);
        analyzedFile = file;
      } catch (MediaAnalyzerException e) {
        log_
            .warn("Track " + file + " could not be analyzed: " + e.getMessage());
        throw e;
      }
    }
    return mediaInfo;
  }

  /**
   * Adds the media info extracted by the
   * {@link ch.ethz.replay.core.api.common.media.analysis.MediaAnalyzer}.
   */
  protected void addMediaInfo(TrackImpl track) throws MediaAnalyzerException {
    if (mediaInfo == null)
      throw new MediaAnalyzerException(
          "Media analyzer returned no results for " + track);

    // Set the track duration
    if (mediaInfo.getDuration() == null)
      throw new MediaAnalyzerException(
          "Media analyzer was unable to determine track duration for " + track);
    track.setDuration(mediaInfo.getDuration());

    // Video settings
    if (mediaInfo.hasVideoStreamMetadata()) {
      VideoSettingsImpl videoSettings = new VideoSettingsImpl();
      List<VideoStreamMetadata> metadata = mediaInfo.getVideoStreamMetadata();
      if (metadata.size() > 1)
        log_.warn("Analysis reveals that " + track.getFile().getAbsolutePath()
            + " contains " + metadata.size()
            + " video streams. Because the VideoSettings "
            + "can handle only one stream, only the first one will be used.");
      videoSettings.setMetadata(metadata.get(0));
      track.setVideoSettings(videoSettings);
    }

    // Audio settings
    if (mediaInfo.hasAudioStreamMetadata()) {
      AudioSettingsImpl audioSettings = new AudioSettingsImpl();
      List<AudioStreamMetadata> metadata = mediaInfo.getAudioStreamMetadata();
      if (metadata.size() > 1)
        log_.warn("Analysis reveals that " + track.getFile().getAbsolutePath()
            + " contains " + metadata.size()
            + " audio streams. Because the AudioSettings "
            + "can handle only one stream, only the first one will be used.");
      audioSettings.setMetadata(metadata.get(0));
      track.setAudioSettings(audioSettings);
    }
  }
}