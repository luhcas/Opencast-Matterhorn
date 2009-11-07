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
package org.opencastproject.inspection.impl;

import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.media.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageElements;
import org.opencastproject.media.mediapackage.Stream;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.media.mediapackage.MediaPackageElement.Type;
import org.opencastproject.media.mediapackage.track.AudioStreamImpl;
import org.opencastproject.media.mediapackage.track.TrackImpl;
import org.opencastproject.media.mediapackage.track.VideoStreamImpl;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.ChecksumType;
import org.opencastproject.workspace.api.Workspace;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

/**
 * Inspects media via the 3rd party MediaInfo tool by default, and can be configured to use other media analyzers.
 */
public class MediaInspectionServiceImpl implements MediaInspectionService, ManagedService {
  private static final Logger logger = LoggerFactory.getLogger(MediaInspectionServiceImpl.class);

  Workspace workspace;

  public void setWorkspace(Workspace workspace) {
    logger.debug("setting " + workspace);
    this.workspace = workspace;
  }

  public void unsetWorkspace(Workspace workspace) {
    logger.debug("unsetting " + workspace);
  }

  public Track inspect(URL url) {
    logger.debug("inspect(" + url + ") called, using workspace " + workspace);

    // Get the file from the URL
    File file = workspace.get(url);

    MediaContainerMetadata metadata = null;
    try {
      MediaAnalyzerFactory analyzerFactory = MediaAnalyzerFactory.newInstance();
      MediaAnalyzer mediaAnalyzer = analyzerFactory.newMediaAnalyzer();
      metadata = mediaAnalyzer.analyze(file);
    } catch (Throwable t) {
      throw new IllegalStateException("Unable to create media analyzer", t);
    }

    if (metadata == null) {
      logger.warn("Unable to acquire media metadata for " + url);
      return null;
    } else {
      MediaPackageElementBuilder elementBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
      TrackImpl track;
      try {
        MediaPackageElement element = elementBuilder.elementFromURL(url, Type.Track,
                MediaPackageElements.INDEFINITE_TRACK);
        track = (TrackImpl) element;
        if(metadata.getDuration() != null) track.setDuration(metadata.getDuration());
        track.setChecksum(Checksum.create(ChecksumType.DEFAULT_TYPE, file));
        List<AudioStreamMetadata> audioList = metadata.getAudioStreamMetadata();
        if (audioList != null && !audioList.isEmpty()) {
          for (int i = 0; i < audioList.size(); i++) {
            AudioStreamImpl audio = new AudioStreamImpl("audio-" + (i + 1));
            AudioStreamMetadata a = audioList.get(i);
            audio.setBitRate(a.getBitRate());
            audio.setChannels(a.getChannels());
            audio.setFormat(a.getFormat());
            audio.setFormatVersion(a.getFormatVersion());
            audio.setResolution(a.getResolution());
            audio.setSamplingRate(a.getSamplingRate());
            track.addStream(audio);
          }
        }
        List<VideoStreamMetadata> videoList = metadata.getVideoStreamMetadata();
        if (videoList != null && !videoList.isEmpty()) {
          for (int i = 0; i < audioList.size(); i++) {
            VideoStreamImpl video = new VideoStreamImpl("video-" + (i + 1));
            VideoStreamMetadata v = videoList.get(i);
            video.setBitRate(v.getBitRate());
            video.setFormat(v.getFormat());
            video.setFormatVersion(v.getFormatVersion());
            video.setFrameHeight(v.getFrameHeight());
            video.setFrameRate(v.getFrameRate());
            video.setFrameWidth(v.getFrameWidth());
            video.setScanOrder(v.getScanOrder());
            video.setScanType(v.getScanType());
            track.addStream(video);
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      } // FIXME: how should we determine flavor?
      return track;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.inspection.api.MediaInspectionService#enrich(Track, Boolean)
   */
  public Track enrich(Track originalTrack, Boolean override) {
    URL originalTrackUrl = originalTrack.getURL();
    logger.debug("enrych(" + originalTrackUrl + ") called");

    // Get the file from the URL
    File file = workspace.get(originalTrackUrl);

    MediaContainerMetadata metadata = null;
    try {
      MediaAnalyzerFactory analyzerFactory = MediaAnalyzerFactory.newInstance();
      MediaAnalyzer mediaAnalyzer = analyzerFactory.newMediaAnalyzer();
      metadata = mediaAnalyzer.analyze(file);
    } catch (Throwable t) {
      throw new IllegalStateException("Unable to create media analyzer", t);
    }

    if (metadata == null) {
      logger.warn("Unable to acquire media metadata for " + originalTrackUrl);
      return null;
    } else {
      MediaPackageElementBuilder elementBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
      TrackImpl track;
      try {
        MediaPackageElement element = elementBuilder.elementFromURL(originalTrackUrl, Type.Track,
                MediaPackageElements.INDEFINITE_TRACK);
        // init the new track with old
        track = (TrackImpl) element;
        track.setChecksum(originalTrack.getChecksum());
        track.setDuration(originalTrack.getDuration());
        track.setElementDescription(originalTrack.getElementDescription());
        track.setFlavor(originalTrack.getFlavor());
        track.setIdentifier(originalTrack.getIdentifier());
        track.setMimeType(originalTrack.getMimeType());
        track.setReference(originalTrack.getReference());
        track.setSize(originalTrack.getSize());
        track.setURL(originalTrackUrl);
        // enrich the new track with basic info
        if (track.getDuration() == -1L || override)
          track.setDuration(metadata.getDuration());
        if (track.getChecksum() == null || override)
          track.setChecksum(Checksum.create(ChecksumType.DEFAULT_TYPE, file));

        // find all streams
        Dictionary<String, Stream> streamsId2Stream = new Hashtable<String, Stream>();
        for (Stream stream : originalTrack.getStreams()) {
          streamsId2Stream.put(stream.getIdentifier(), stream);
        }

        // audio list
        List<AudioStreamMetadata> audioList = metadata.getAudioStreamMetadata();
        if (audioList != null && !audioList.isEmpty()) {
          for (int i = 0; i < audioList.size(); i++) {
            AudioStreamImpl audio = new AudioStreamImpl("audio-" + (i + 1));
            AudioStreamMetadata a = audioList.get(i);
            audio.setBitRate(a.getBitRate());
            audio.setChannels(a.getChannels());
            audio.setFormat(a.getFormat());
            audio.setFormatVersion(a.getFormatVersion());
            audio.setResolution(a.getResolution());
            audio.setSamplingRate(a.getSamplingRate());
            // TODO: retain the original audio metadata
            track.addStream(audio);
          }
        }
        // video list
        List<VideoStreamMetadata> videoList = metadata.getVideoStreamMetadata();
        if (videoList != null && !videoList.isEmpty()) {
          for (int i = 0; i < audioList.size(); i++) {
            VideoStreamImpl video = new VideoStreamImpl("video-" + (i + 1));
            VideoStreamMetadata v = videoList.get(i);
            video.setBitRate(v.getBitRate());
            video.setFormat(v.getFormat());
            video.setFormatVersion(v.getFormatVersion());
            video.setFrameHeight(v.getFrameHeight());
            video.setFrameRate(v.getFrameRate());
            video.setFrameWidth(v.getFrameWidth());
            video.setScanOrder(v.getScanOrder());
            video.setScanType(v.getScanType());
            // TODO: retain the original video metadata
            track.addStream(video);
          }
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      return track;
    }
  }

  @SuppressWarnings("unchecked")
  public void updated(Dictionary properties) throws ConfigurationException {
    logger.info("Updating configuration on " + this.getClass().getName());
    // TODO Update the local path to the mediainfo binary
  }

}
