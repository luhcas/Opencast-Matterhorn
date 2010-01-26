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
import org.opencastproject.media.mediapackage.AbstractMediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.media.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.Stream;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.media.mediapackage.UnsupportedElementException;
import org.opencastproject.media.mediapackage.MediaPackageElement.Type;
import org.opencastproject.media.mediapackage.track.AudioStreamImpl;
import org.opencastproject.media.mediapackage.track.TrackImpl;
import org.opencastproject.media.mediapackage.track.VideoStreamImpl;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.ChecksumType;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.UnknownFileTypeException;
import org.opencastproject.workspace.api.Workspace;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Inspects media via the 3rd party MediaInfo tool by default, and can be configured to use other media analyzers.
 */
public class MediaInspectionServiceImpl implements MediaInspectionService, ManagedService {
  public static final String CONFIG_ANALYZER_CLASS = "inspection.analyzerclass";
  public static final String CONFIG_ANALYZER_MEDIAINFOPATH = "inspection.mediainfopath";

  private static final Logger logger = LoggerFactory.getLogger(MediaInspectionServiceImpl.class);

  Workspace workspace;
  Map<String, Object> analyzerConfig = new ConcurrentHashMap<String, Object>();

  public void setWorkspace(Workspace workspace) {
    logger.debug("setting " + workspace);
    this.workspace = workspace;
  }

  public void unsetWorkspace(Workspace workspace) {
    logger.debug("unsetting " + workspace);
  }

  @SuppressWarnings("unchecked")
  public void updated(Dictionary properties) throws ConfigurationException {
    logger.info("TODO Updating configuration on {}", this.getClass().getName());
    // TODO this is doing nothing
  }

  public void activate(ComponentContext cc) {
    if (cc != null) {
      if (cc.getBundleContext().getProperty(CONFIG_ANALYZER_CLASS) != null) {
        // use analyzerclass from CONFIG
        MediaAnalyzerFactory.analyzerClassName = cc.getBundleContext().getProperty(CONFIG_ANALYZER_CLASS);
        logger.info("CONFIG "+CONFIG_ANALYZER_CLASS+": " + MediaAnalyzerFactory.analyzerClassName);
      }
      if (cc.getBundleContext().getProperty(CONFIG_ANALYZER_MEDIAINFOPATH) != null) {
        // use binary path from CONFIG
        String path = cc.getBundleContext().getProperty(CONFIG_ANALYZER_MEDIAINFOPATH);
        analyzerConfig.put(MediaInfoAnalyzer.CONFIG_MEDIAINFO_BINARY, path);
        logger.info("CONFIG "+CONFIG_ANALYZER_MEDIAINFOPATH+": " + path);
      }
    }
  }

  public Track inspect(URI uri) {
    logger.debug("inspect(" + uri + ") called, using workspace " + workspace);

    // Get the file from the URL
    File file = workspace.get(uri);

    MediaContainerMetadata metadata = null;
    try {
      MediaAnalyzerFactory analyzerFactory = MediaAnalyzerFactory.newInstance();
      MediaAnalyzer mediaAnalyzer = analyzerFactory.newMediaAnalyzer();
      mediaAnalyzer.setConfig(analyzerConfig);
      metadata = mediaAnalyzer.analyze(file);
    } catch (Throwable t) {
      throw new IllegalStateException("Unable to create media analyzer", t);
    }

    if (metadata == null) {
      logger.warn("Unable to acquire media metadata for " + uri);
      return null;  // TODO: does the contract for this service define what to do if the file isn't valid media?
    } else {
      MediaPackageElementBuilder elementBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
      TrackImpl track;
        MediaPackageElement element;
        try {
          element = elementBuilder.elementFromURI(uri, Type.Track, null);
        } catch (UnsupportedElementException e) {
          throw new RuntimeException(e);
        }
        track = (TrackImpl) element;
        if (metadata.getDuration() != null)
          track.setDuration(metadata.getDuration());
        try {
          track.setChecksum(Checksum.create(ChecksumType.DEFAULT_TYPE, file));
        } catch (NoSuchAlgorithmException e) {
          throw new RuntimeException(e);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        try {
          track.setMimeType(MimeTypes.fromURL(file.toURI().toURL()));
        } catch (Exception e) {
          logger.info("unable to find mimetype for {}", file.getAbsolutePath());
        }
        List<AudioStreamMetadata> audioList = metadata.getAudioStreamMetadata();
        if (audioList != null && !audioList.isEmpty()) {
          for (int i = 0; i < audioList.size(); i++) {
            AudioStreamImpl audio = new AudioStreamImpl("audio-" + (i + 1));
            AudioStreamMetadata a = audioList.get(i);
            audio.setBitRate(a.getBitRate());
            audio.setChannels(a.getChannels());
            audio.setFormat(a.getFormat());
            audio.setFormatVersion(a.getFormatVersion());
            audio.setBitDepth(a.getResolution());
            audio.setSamplingRate(a.getSamplingRate());
            track.addStream(audio);
          }
        }
        List<VideoStreamMetadata> videoList = metadata.getVideoStreamMetadata();
        if (videoList != null && !videoList.isEmpty()) {
          for (int i = 0; i < videoList.size(); i++) {
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
      return track;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.inspection.api.MediaInspectionService#enrich(Track, Boolean)
   */
  public Track enrich(Track originalTrack, Boolean override) {
    URI originalTrackUrl = originalTrack.getURI();
    MediaPackageElementFlavor flavor = originalTrack.getFlavor();
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
      TrackImpl track = null;
      try {
        track = (TrackImpl) MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
          .elementFromURI(originalTrackUrl, Type.Track, flavor);
      } catch (UnsupportedElementException e) {
        throw new RuntimeException(e);
      }
      // init the new track with old
      track.setChecksum(originalTrack.getChecksum());
      track.setDuration(originalTrack.getDuration());
      track.setElementDescription(originalTrack.getElementDescription());
      track.setFlavor(flavor);
      track.setIdentifier(originalTrack.getIdentifier());
      track.setMimeType(originalTrack.getMimeType());
      track.setReference(originalTrack.getReference());
      track.setSize(originalTrack.getSize());
      track.setURI(originalTrackUrl);
      // enrich the new track with basic info
      if (track.getDuration() == -1L || override)
        track.setDuration(metadata.getDuration());
      if (track.getChecksum() == null || override)
        try {
          track.setChecksum(Checksum.create(ChecksumType.DEFAULT_TYPE, file));
        } catch (NoSuchAlgorithmException e) {
          throw new RuntimeException(e);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        
      // Add the mime type if it's not already present
      if(track.getMimeType() == null) {
        try {
          track.setMimeType(MimeTypes.fromURL(track.getURI().toURL()));
        } catch (MalformedURLException e) {
          logger.warn("Track {} has a malformed URL, {}", track.getIdentifier(), track.getURI());
        } catch (UnknownFileTypeException e) {
          logger.debug("Unable to detect the mimetype for track {} at {}", track.getIdentifier(), track.getURI());
        }
      }
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
          audio.setBitDepth(a.getResolution());
          audio.setSamplingRate(a.getSamplingRate());
          // TODO: retain the original audio metadata
          track.addStream(audio);
        }
      }
      // video list
      List<VideoStreamMetadata> videoList = metadata.getVideoStreamMetadata();
      if (videoList != null && !videoList.isEmpty()) {
        for (int i = 0; i < videoList.size(); i++) {
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
      return track;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.inspection.api.MediaInspectionService#enrich(AbstractMediaPackageElement, Boolean)
   */
  public AbstractMediaPackageElement enrich(AbstractMediaPackageElement element, Boolean override) {
    File file = workspace.get(element.getURI());
    if (element.getChecksum() == null || override)
      try {
        element.setChecksum(Checksum.create(ChecksumType.DEFAULT_TYPE, file));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    return element;
  }

}
