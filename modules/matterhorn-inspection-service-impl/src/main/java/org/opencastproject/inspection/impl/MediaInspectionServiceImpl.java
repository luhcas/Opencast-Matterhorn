/**
 *  Copyright 2009, 2010 The Regents of the University of California
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
import org.opencastproject.inspection.impl.api.AudioStreamMetadata;
import org.opencastproject.inspection.impl.api.MediaAnalyzer;
import org.opencastproject.inspection.impl.api.MediaContainerMetadata;
import org.opencastproject.inspection.impl.api.VideoStreamMetadata;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.Stream;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.UnsupportedElementException;
import org.opencastproject.mediapackage.MediaPackageElement.Type;
import org.opencastproject.mediapackage.track.AudioStreamImpl;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.mediapackage.track.VideoStreamImpl;
import org.opencastproject.remote.api.Job;
import org.opencastproject.remote.api.RemoteServiceManager;
import org.opencastproject.remote.api.Job.Status;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.ChecksumType;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UnknownFileTypeException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Inspects media via the 3rd party MediaInfo tool by default, and can be configured to use other media analyzers.
 */
public class MediaInspectionServiceImpl implements MediaInspectionService {
  // FIXME move this to media info analyzer service
  public static final String CONFIG_ANALYZER_MEDIAINFOPATH = "inspection.analyzer.mediainfopath";
  private static final Logger logger = LoggerFactory.getLogger(MediaInspectionServiceImpl.class);

  Workspace workspace;
  RemoteServiceManager remoteServiceManager;
  ExecutorService executor = null;
  String serverUrl = null;
  Map<String, Object> analyzerConfig = new ConcurrentHashMap<String, Object>();

  public void setWorkspace(Workspace workspace) {
    logger.debug("setting " + workspace);
    this.workspace = workspace;
  }

  public void setRemoteServiceManager(RemoteServiceManager remoteServiceManager) {
    this.remoteServiceManager = remoteServiceManager;
  }

  public void activate(ComponentContext cc) {
    if (cc != null) {
      if (cc.getBundleContext().getProperty(CONFIG_ANALYZER_MEDIAINFOPATH) != null) {
        // use binary path from CONFIG
        String path = cc.getBundleContext().getProperty(CONFIG_ANALYZER_MEDIAINFOPATH);
        analyzerConfig.put(MediaInfoAnalyzer.CONFIG_MEDIAINFO_BINARY, path);
        logger.info("CONFIG " + CONFIG_ANALYZER_MEDIAINFOPATH + ": " + path);
      }
    }
    if (cc == null || cc.getBundleContext().getProperty("org.opencastproject.server.url") == null) {
      serverUrl = UrlSupport.DEFAULT_BASE_URL;
    } else {
      serverUrl = cc.getBundleContext().getProperty("org.opencastproject.server.url");
    }
    activate();
  }

  public void activate() {
    executor = Executors.newFixedThreadPool(4);
    remoteServiceManager.registerService(JOB_TYPE, serverUrl);
  }

  public void deactivate() {
    remoteServiceManager.unRegisterService(JOB_TYPE, serverUrl);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.inspection.api.MediaInspectionService#getReceipt(java.lang.String)
   */
  @Override
  public Job getReceipt(String id) {
    return remoteServiceManager.getJob(id);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.inspection.api.MediaInspectionService#inspect(java.net.URI, boolean)
   */
  public Job inspect(final URI uri, final boolean block) {
    logger.debug("inspect(" + uri + ") called, using workspace " + workspace);

    // Construct a receipt for this operation
    final Job job = remoteServiceManager.createJob(JOB_TYPE);
    final RemoteServiceManager rs = remoteServiceManager;
    Callable<Track> command = new Callable<Track>() {
      public Track call() throws Exception {
        // Update the receipt status
        job.setStatus(Status.RUNNING);
        rs.updateJob(job);

        // Get the file from the URL (runtime exception if invalid)
        File file = null;
        try {
          file = workspace.get(uri);
        } catch (NotFoundException e) {
          logger.warn("File " + file + " was not found and can therefore not be inspected");
          job.setStatus(Status.FAILED);
          rs.updateJob(job);
          throw new RuntimeException(e);
        }
        
        // Make sure the file has an extension. Otherwise, tools like ffmpeg will not work.
        // TODO: Try to guess the extension from the container's metadata
        if ("".equals(FilenameUtils.getExtension(file.getName()))) {
          logger.warn("Track " + file + " has no file extension");
          job.setStatus(Status.FAILED);
          rs.updateJob(job);
          throw new UnsupportedElementException("Track " + file + " has no file extension");
        }
        
        MediaContainerMetadata metadata = getFileMetadata(file);
        if (metadata == null) {
          logger.warn("Unable to acquire media metadata for " + uri);
          job.setStatus(Status.FAILED);
          rs.updateJob(job);
          return null; // TODO: does the contract for this service define what to do if the file isn't valid media?
        } else {
          MediaPackageElementBuilder elementBuilder = MediaPackageElementBuilderFactory.newInstance()
                  .newElementBuilder();
          TrackImpl track;
          MediaPackageElement element;
          try {
            element = elementBuilder.elementFromURI(uri, Type.Track, null);
          } catch (UnsupportedElementException e) {
            logger.warn("Unable to create track element from " + file + ": " + e.getMessage());
            job.setStatus(Status.FAILED);
            rs.updateJob(job);
            throw new RuntimeException(e);
          }
          track = (TrackImpl) element;

          // Duration
          if (metadata.getDuration() != null)
            track.setDuration(metadata.getDuration());

          // Checksum
          try {
            track.setChecksum(Checksum.create(ChecksumType.DEFAULT_TYPE, file));
          } catch (NoSuchAlgorithmException e) {
            logger.warn("Unable to create checksum for " + file + ": " + e.getMessage());
            job.setStatus(Status.FAILED);
            rs.updateJob(job);
            throw new RuntimeException(e);
          } catch (IOException e) {
            logger.warn("Unable to read " + file + ": " + e.getMessage());
            job.setStatus(Status.FAILED);
            rs.updateJob(job);
            throw new RuntimeException(e);
          }

          // Mimetype
          try {
            track.setMimeType(MimeTypes.fromURL(file.toURI().toURL()));
          } catch (Exception e) {
            logger.info("Unable to find mimetype for {}", file.getAbsolutePath());
          }

          // Audio metadata
          try {
            addAudioStreamMetadata(track, metadata);
          } catch (Exception e) {
            logger.warn("Unable to extract audio metadata from " + file + ": " + e.getMessage());
            job.setStatus(Status.FAILED);
            rs.updateJob(job);
            throw new RuntimeException(e);
          }

          // Videometadata
          try {
            addVideoStreamMetadata(track, metadata);
          } catch (Exception e) {
            logger.warn("Unable to extract video metadata from " + file + ": " + e.getMessage());
            job.setStatus(Status.FAILED);
            rs.updateJob(job);
            throw new RuntimeException(e);
          }

          job.setElement(track);
          job.setStatus(Status.FINISHED);
          rs.updateJob(job);
          return track;
        }
      }
    };

    Future<Track> future = executor.submit(command);

    if (block) {
      try {
        future.get();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return job;
  }

  protected Callable<MediaPackageElement> getEnrichTrackCommand(final Track originalTrack, final boolean override,
          final Job receipt) {
    final RemoteServiceManager rs = remoteServiceManager;
    return new Callable<MediaPackageElement>() {
      public MediaPackageElement call() throws Exception {
        // Set the receipt state to running
        receipt.setStatus(Status.RUNNING);
        rs.updateJob(receipt);

        URI originalTrackUrl = originalTrack.getURI();
        MediaPackageElementFlavor flavor = originalTrack.getFlavor();
        logger.debug("enrich(" + originalTrackUrl + ") called");

        // Get the file from the URL
        File file = null;
        try {
          file = workspace.get(originalTrackUrl);
        } catch (NotFoundException e) {
          logger.warn("File " + file + " was not found and can therefore not be inspected");
          receipt.setStatus(Status.FAILED);
          rs.updateJob(receipt);
          throw new RuntimeException(e);
        }

        // Make sure the file has an extension. Otherwise, tools like ffmpeg will not work.
        // TODO: Try to guess the extension from the container's metadata
        if ("".equals(FilenameUtils.getExtension(file.getName()))) {
          logger.warn("Track " + file + " has no file extension");
          receipt.setStatus(Status.FAILED);
          rs.updateJob(receipt);
          throw new UnsupportedElementException("Track " + file + " has no file extension");
        }

        MediaContainerMetadata metadata = getFileMetadata(file);
        if (metadata == null) {
          logger.warn("Unable to acquire media metadata for " + originalTrackUrl);
          receipt.setStatus(Status.FAILED);
          rs.updateJob(receipt);
          return null;
        } else if (metadata.getAudioStreamMetadata().size() == 0 && metadata.getVideoStreamMetadata().size() == 0) {
          logger.warn("File at {} does not seem like a a/v media", originalTrackUrl);
          receipt.setStatus(Status.FAILED);
          rs.updateJob(receipt);
          return null;
        } else {
          TrackImpl track = null;
          try {
            track = (TrackImpl) MediaPackageElementBuilderFactory.newInstance().newElementBuilder().elementFromURI(
                    originalTrackUrl, Type.Track, flavor);
          } catch (UnsupportedElementException e) {
            logger.warn("Unable to create track element from " + file + ": " + e.getMessage());
            receipt.setStatus(Status.FAILED);
            rs.updateJob(receipt);
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
          for (String tag : originalTrack.getTags()) {
            track.addTag(tag);
          }

          // enrich the new track with basic info
          if (track.getDuration() == -1L || override)
            track.setDuration(metadata.getDuration());
          if (track.getChecksum() == null || override) {
            try {
              track.setChecksum(Checksum.create(ChecksumType.DEFAULT_TYPE, file));
            } catch (NoSuchAlgorithmException e) {
              logger.warn("Unable to create checksum for " + file + ": " + e.getMessage());
              receipt.setStatus(Status.FAILED);
              rs.updateJob(receipt);
              throw new RuntimeException(e);
            } catch (IOException e) {
              logger.warn("Unable to read " + file + ": " + e.getMessage());
              receipt.setStatus(Status.FAILED);
              rs.updateJob(receipt);
              throw new RuntimeException(e);
            }
          }

          // Add the mime type if it's not already present
          if (track.getMimeType() == null || override) {
            try {
              track.setMimeType(MimeTypes.fromURL(track.getURI().toURL()));
            } catch (MalformedURLException e) {
              logger.warn("Track {} has a malformed URL, {}", track.getIdentifier(), track.getURI());
            } catch (UnknownFileTypeException e) {
              logger.info("Unable to detect the mimetype for track {} at {}", track.getIdentifier(), track.getURI());
            }
          }

          // find all streams
          Dictionary<String, Stream> streamsId2Stream = new Hashtable<String, Stream>();
          for (Stream stream : originalTrack.getStreams()) {
            streamsId2Stream.put(stream.getIdentifier(), stream);
          }

          // audio list
          try {
            addAudioStreamMetadata(track, metadata);
          } catch (Exception e) {
            logger.warn("Unable to extract audio metadata from " + file + ": " + e.getMessage());
            receipt.setStatus(Status.FAILED);
            rs.updateJob(receipt);
            throw new RuntimeException(e);
          }

          // video list
          try {
            addVideoStreamMetadata(track, metadata);
          } catch (Exception e) {
            logger.warn("Unable to extract video metadata from " + file + ": " + e.getMessage());
            receipt.setStatus(Status.FAILED);
            rs.updateJob(receipt);
            throw new RuntimeException(e);
          }

          receipt.setElement(track);
          receipt.setStatus(Status.FINISHED);
          rs.updateJob(receipt);
          logger.info("Successfully inspected track {}", track);
          return track;
        }
      }
    };
  }

  /**
   * Adds the video related metadata to the track.
   * 
   * @param track
   *          the track
   * @param metadata
   *          the container metadata
   */
  protected Track addVideoStreamMetadata(TrackImpl track, MediaContainerMetadata metadata) {
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
  
  /**
   * Adds the audio related metadata to the track.
   * 
   * @param track
   *          the track
   * @param metadata
   *          the container metadata
   */
  protected Track addAudioStreamMetadata(TrackImpl track, MediaContainerMetadata metadata) {
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
    return track;
  }

  protected Callable<MediaPackageElement> getEnrichElementCommand(final MediaPackageElement element,
          final boolean override, final Job receipt) {
    final RemoteServiceManager rs = remoteServiceManager;
    return new Callable<MediaPackageElement>() {
      public MediaPackageElement call() throws Exception {
        receipt.setStatus(Status.RUNNING);
        rs.updateJob(receipt);
        File file;
        try {
          file = workspace.get(element.getURI());
        } catch (NotFoundException e) {
          receipt.setStatus(Status.FAILED);
          rs.updateJob(receipt);
          throw new RuntimeException(e);
        }
        if (element.getChecksum() == null || override) {
          try {
            element.setChecksum(Checksum.create(ChecksumType.DEFAULT_TYPE, file));
          } catch (Exception e) {
            receipt.setStatus(Status.FAILED);
            rs.updateJob(receipt);
            throw new RuntimeException(e);
          }
        }
        if (element.getMimeType() == null || override) {
          try {
            element.setMimeType(MimeTypes.fromURL(file.toURI().toURL()));
          } catch (UnknownFileTypeException e) {
            logger.info("unable to determine the mime type for {}", file.getName());
          }
        }
        receipt.setElement(element);
        receipt.setStatus(Status.FINISHED);
        rs.updateJob(receipt);
        logger.info("Successfully inspected element {}", element);
        return element;
      }
    };
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.inspection.api.MediaInspectionService#enrich(org.opencastproject.mediapackage.MediaPackageElement,
   *      boolean, boolean)
   */
  @Override
  public Job enrich(final MediaPackageElement element, final boolean override, final boolean block) {
    Callable<MediaPackageElement> command;
    final Job job = remoteServiceManager.createJob(JOB_TYPE);
    if (element instanceof Track) {
      final Track originalTrack = (Track) element;
      command = getEnrichTrackCommand(originalTrack, override, job);
    } else {
      command = getEnrichElementCommand(element, override, job);
    }

    Future<MediaPackageElement> future = executor.submit(command);

    if (block) {
      try {
        future.get();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return job;
  }

  private MediaContainerMetadata getFileMetadata(File file) {
    if (file == null) {
      throw new IllegalArgumentException("file to analyze cannot be null");
    }
    MediaContainerMetadata metadata = null;
    try {
      MediaAnalyzer analyzer = new MediaInfoAnalyzer();
      analyzer.setConfig(analyzerConfig);
      metadata = analyzer.analyze(file);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to create media analyzer", e);
    }
    return metadata;
  }

}
