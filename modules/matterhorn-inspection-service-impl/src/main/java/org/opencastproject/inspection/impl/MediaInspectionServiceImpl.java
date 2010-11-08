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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.opencastproject.inspection.api.MediaInspectionException;
import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.inspection.impl.api.AudioStreamMetadata;
import org.opencastproject.inspection.impl.api.MediaAnalyzer;
import org.opencastproject.inspection.impl.api.MediaAnalyzerException;
import org.opencastproject.inspection.impl.api.MediaContainerMetadata;
import org.opencastproject.inspection.impl.api.VideoStreamMetadata;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElement.Type;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.Stream;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.UnsupportedElementException;
import org.opencastproject.mediapackage.track.AudioStreamImpl;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.mediapackage.track.VideoStreamImpl;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.serviceregistry.api.ServiceUnavailableException;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.ChecksumType;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UnknownFileTypeException;
import org.opencastproject.workspace.api.Workspace;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Inspects media via the 3rd party MediaInfo tool by default, and can be configured to use other media analyzers.
 */
public class MediaInspectionServiceImpl implements MediaInspectionService {

  private static final Logger logger = LoggerFactory.getLogger(MediaInspectionServiceImpl.class);

  // FIXME move this to media info analyzer service
  public static final String CONFIG_ANALYZER_MEDIAINFOPATH = "inspection.analyzer.mediainfopath";

  /** The configuration key for setting the number of worker threads */
  public static final String CONFIG_THREADS = "org.opencastproject.mediainspection.threads";

  /** The default worker thread pool size to use if no configuration is specified */
  public static final int DEFAULT_THREADS = 1;

  protected Workspace workspace;
  protected ServiceRegistry serviceRegistry;
  protected ExecutorService executor = null;
  protected Map<String, Object> analyzerConfig = new ConcurrentHashMap<String, Object>();

  public void setWorkspace(Workspace workspace) {
    logger.debug("setting " + workspace);
    this.workspace = workspace;
  }

  public void setRemoteServiceManager(ServiceRegistry jobManager) {
    this.serviceRegistry = jobManager;
  }

  public void activate(ComponentContext cc) {
    if (cc != null) {
      if (cc.getBundleContext().getProperty(CONFIG_ANALYZER_MEDIAINFOPATH) != null) {
        // use binary path from CONFIG
        String path = cc.getBundleContext().getProperty(CONFIG_ANALYZER_MEDIAINFOPATH);
        analyzerConfig.put(MediaInfoAnalyzer.CONFIG_MEDIAINFO_BINARY, path);
        logger.info("CONFIG " + CONFIG_ANALYZER_MEDIAINFOPATH + ": " + path);
      }

      // Set the number of concurrent threads
      int threads = DEFAULT_THREADS;
      String threadsConfig = StringUtils.trimToNull(cc.getBundleContext().getProperty(CONFIG_THREADS));
      if (threadsConfig != null) {
        try {
          threads = Integer.parseInt(threadsConfig);
        } catch (NumberFormatException e) {
          logger.warn("Caption converter threads configuration is malformed: '{}'", threadsConfig);
        }
      }
      executor = Executors.newFixedThreadPool(threads);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JobProducer#getJob(java.lang.String)
   */
  public Job getJob(String id) throws NotFoundException, ServiceRegistryException {
    return serviceRegistry.getJob(id);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JobProducer#countJobs(org.opencastproject.job.api.Job.Status)
   */
  public long countJobs(Status status) throws ServiceRegistryException {
    if (status == null)
      throw new IllegalArgumentException("status must not be null");
    return serviceRegistry.count(JOB_TYPE, status);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JobProducer#countJobs(org.opencastproject.job.api.Job.Status, java.lang.String)
   */
  public long countJobs(Status status, String host) throws ServiceRegistryException {
    if (status == null)
      throw new IllegalArgumentException("status must not be null");
    if (host == null)
      throw new IllegalArgumentException("host must not be null");
    return serviceRegistry.count(JOB_TYPE, status, host);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.inspection.api.MediaInspectionService#inspect(java.net.URI, boolean)
   */
  public Job inspect(final URI uri, final boolean block) throws MediaInspectionException {
    logger.debug("inspect(" + uri + ") called, using workspace " + workspace);

    // Construct a receipt for this operation
    final Job job;
    try {
      job = serviceRegistry.createJob(JOB_TYPE);
    } catch (ServiceUnavailableException e) {
      throw new MediaInspectionException("No service of type '" + JOB_TYPE + "' available", e);
    } catch (ServiceRegistryException e) {
      throw new MediaInspectionException(e);
    }

    Callable<Track> command = new Callable<Track>() {
      public Track call() throws MediaInspectionException {
        try {
          job.setStatus(Status.RUNNING);
          updateJob(job);

          // Get the file from the URL (runtime exception if invalid)
          File file = null;
          try {
            file = workspace.get(uri);
          } catch (NotFoundException notFound) {
            throw new MediaInspectionException("Unable to find resource " + uri, notFound);
          } catch (IOException ioe) {
            throw new MediaInspectionException("Error reading " + uri + " from workspace", ioe);
          }

          // Make sure the file has an extension. Otherwise, tools like ffmpeg will not work.
          // TODO: Try to guess the extension from the container's metadata
          if ("".equals(FilenameUtils.getExtension(file.getName()))) {
            throw new MediaInspectionException("Can not inspect files without a filename extension");
          }

          MediaContainerMetadata metadata = getFileMetadata(file);
          if (metadata == null) {
            throw new MediaInspectionException("Media analyzer returned no metadata from " + file);
          } else {
            MediaPackageElementBuilder elementBuilder = MediaPackageElementBuilderFactory.newInstance()
                    .newElementBuilder();
            TrackImpl track;
            MediaPackageElement element;
            try {
              element = elementBuilder.elementFromURI(uri, Type.Track, null);
            } catch (UnsupportedElementException e) {
              throw new MediaInspectionException("Unable to create track element from " + file, e);
            }
            track = (TrackImpl) element;

            // Duration
            if (metadata.getDuration() != null)
              track.setDuration(metadata.getDuration());

            // Checksum
            try {
              track.setChecksum(Checksum.create(ChecksumType.DEFAULT_TYPE, file));
            } catch (IOException e) {
              throw new MediaInspectionException("Unable to read " + file, e);
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
              throw new MediaInspectionException("Unable to extract audio metadata from " + file, e);
            }

            // Videometadata
            try {
              addVideoStreamMetadata(track, metadata);
            } catch (Exception e) {
              throw new MediaInspectionException("Unable to extract video metadata from " + file, e);
            }

            job.setElement(track);
            job.setStatus(Status.FINISHED);
            updateJob(job);
            return track;
          }
        } catch(Exception e) {
          logger.warn("Error inspecting " + uri, e);
          try {
            job.setStatus(Status.FAILED);
            updateJob(job);
          } catch (Exception failureToFail) {
            logger.warn("Unable to update job to failed state", failureToFail);
          }
          if (e instanceof MediaInspectionException) {
            throw (MediaInspectionException) e;
          } else {
            throw new MediaInspectionException(e);
          }
        }
      }
    };

    Future<Track> future = executor.submit(command);

    if (block) {
      try {
        future.get();
      } catch (Exception e) {
        try {
          job.setStatus(Status.FAILED);
          updateJob(job);
        } catch (Exception failureToFail) {
          logger.warn("Unable to update job to failed state", failureToFail);
        }
        if (e instanceof MediaInspectionException) {
          throw (MediaInspectionException) e;
        } else {
          throw new MediaInspectionException(e);
        }
      }
    }

    return job;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.inspection.api.MediaInspectionService#enrich(org.opencastproject.mediapackage.MediaPackageElement,
   *      boolean, boolean)
   */
  @Override
  public Job enrich(final MediaPackageElement element, final boolean override, final boolean block)
          throws MediaInspectionException {
    Callable<MediaPackageElement> command;
    final Job job;
    try {
      job = serviceRegistry.createJob(JOB_TYPE);
    } catch (ServiceUnavailableException e) {
      throw new MediaInspectionException("No service of type '" + JOB_TYPE + "' available", e);
    } catch (ServiceRegistryException e) {
      throw new MediaInspectionException(e);
    }
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
      } catch (Throwable e) {
        try {
          job.setStatus(Status.FAILED);
          updateJob(job);
        } catch (Exception failureToFail) {
          logger.warn("Unable to update job to failed state", failureToFail);
        }
        if (e instanceof MediaInspectionException) {
          throw (MediaInspectionException) e;
        } else {
          throw new MediaInspectionException(e);
        }
      }
    }
    return job;
  }

  /**
   * Creates a {@link Callable} that will enrich the track's metadata and can be executed in an asynchronous way.
   * 
   * @param originalTrack
   *          the original track
   * @param override
   *          <code>true</code> to override existing metadata
   * @param job
   *          the job
   * @return the callable
   */
  protected Callable<MediaPackageElement> getEnrichTrackCommand(final Track originalTrack, final boolean override,
          final Job job) {

    return new Callable<MediaPackageElement>() {
      public MediaPackageElement call() throws MediaInspectionException {
        try {
          // Set the job state to running
          job.setStatus(Status.RUNNING);
          updateJob(job);

          URI originalTrackUrl = originalTrack.getURI();
          MediaPackageElementFlavor flavor = originalTrack.getFlavor();
          logger.debug("enrich(" + originalTrackUrl + ") called");

          // Get the file from the URL
          File file = null;
          try {
            file = workspace.get(originalTrackUrl);
          } catch (NotFoundException e) {
            throw new MediaInspectionException("File " + file + " was not found and can therefore not be inspected", e);
          } catch (IOException e) {
            throw new MediaInspectionException("Error accessing " + file, e);
          }

          // Make sure the file has an extension. Otherwise, tools like ffmpeg will not work.
          // TODO: Try to guess the extension from the container's metadata
          if (StringUtils.trimToNull(FilenameUtils.getExtension(file.getName())) == null) {
            throw new MediaInspectionException("Element " + file + " has no file extension");
          }

          MediaContainerMetadata metadata = getFileMetadata(file);
          if (metadata == null) {
            throw new MediaInspectionException("Unable to acquire media metadata for " + originalTrackUrl);
          } else if (metadata.getAudioStreamMetadata().size() == 0 && metadata.getVideoStreamMetadata().size() == 0) {
            throw new MediaInspectionException("File at " + originalTrackUrl + " does not seem to be a/v media");
          } else {
            TrackImpl track = null;
            try {
              track = (TrackImpl) MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
                      .elementFromURI(originalTrackUrl, Type.Track, flavor);
            } catch (UnsupportedElementException e) {
              throw new MediaInspectionException("Unable to create track element from " + file, e);
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
              } catch (IOException e) {
                throw new MediaInspectionException("Unable to read " + file, e);
              }
            }

            // Add the mime type if it's not already present
            if (track.getMimeType() == null || override) {
              try {
                track.setMimeType(MimeTypes.fromURI(track.getURI()));
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
              throw new MediaInspectionException("Unable to extract audio metadata from " + file, e);
            }

            // video list
            try {
              addVideoStreamMetadata(track, metadata);
            } catch (Exception e) {
              throw new MediaInspectionException("Unable to extract video metadata from " + file, e);
            }

            job.setElement(track);
            job.setStatus(Status.FINISHED);
            updateJob(job);

            logger.info("Successfully inspected track {}", track);
            return track;
          }
        } catch (Exception e) {
          logger.warn("Error enriching track " + originalTrack, e);
          try {
            job.setStatus(Status.FAILED);
            updateJob(job);
          } catch (Exception failureToFail) {
            logger.warn("Unable to update job to failed state", failureToFail);
          }
          if (e instanceof MediaInspectionException) {
            throw (MediaInspectionException) e;
          } else {
            throw new MediaInspectionException(e);
          }
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
   * @throws Exception
   *           Media analysis is fragile, and may throw any kind of runtime exceptions due to inconsistencies in the
   *           media's metadata
   */
  protected Track addVideoStreamMetadata(TrackImpl track, MediaContainerMetadata metadata) throws Exception {
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
   * @throws Exception
   *           Media analysis is fragile, and may throw any kind of runtime exceptions due to inconsistencies in the
   *           media's metadata
   */
  protected Track addAudioStreamMetadata(TrackImpl track, MediaContainerMetadata metadata) throws Exception {
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

  /**
   * This command will create a callable which tries to extract common media package element metadata such as the
   * mimetype, the file size etc.
   * 
   * @param element
   *          the media package element
   * @param override
   *          <code>true</code> to overwrite existing metadata
   * @param job
   *          the associated job
   * @return the callable
   */
  protected Callable<MediaPackageElement> getEnrichElementCommand(final MediaPackageElement element,
          final boolean override, final Job job) {

    return new Callable<MediaPackageElement>() {
      public MediaPackageElement call() throws MediaInspectionException {
        try {
          job.setStatus(Status.RUNNING);
          updateJob(job);

          File file;
          try {
            file = workspace.get(element.getURI());
          } catch (NotFoundException e) {
            throw new MediaInspectionException("Unable to find " + element.getURI() + " in the workspace", e);
          } catch (IOException e) {
            throw new MediaInspectionException("Error accessing " + element.getURI() + " in the workspace", e);
          }

          // Checksum
          if (element.getChecksum() == null || override) {
            try {
              element.setChecksum(Checksum.create(ChecksumType.DEFAULT_TYPE, file));
            } catch (IOException e) {
              throw new MediaInspectionException("Error generating checksum for " + element.getURI(), e);
            }
          }

          // Mimetype
          if (element.getMimeType() == null || override) {
            try {
              element.setMimeType(MimeTypes.fromURI(file.toURI()));
            } catch (UnknownFileTypeException e) {
              logger.info("unable to determine the mime type for {}", file.getName());
            }
          }

          job.setElement(element);
          job.setStatus(Status.FINISHED);
          updateJob(job);
          logger.info("Successfully inspected element {}", element);

          return element;
        } catch(Exception e) {
          logger.warn("Error enriching element " + element, e);
          try {
            job.setStatus(Status.FAILED);
            updateJob(job);
          } catch (Exception failureToFail) {
            logger.warn("Unable to update job to failed state", failureToFail);
          }
          if (e instanceof MediaInspectionException) {
            throw (MediaInspectionException) e;
          } else {
            throw new MediaInspectionException(e);
          }
        }
      }
    };

  }

  /**
   * Asks the media analyzer to extract the file's metadata.
   * 
   * @param file
   *          the file
   * @return the file container metadata
   * @throws MediaInspectionException
   *           if metadata extraction fails
   */
  private MediaContainerMetadata getFileMetadata(File file) throws MediaInspectionException {
    if (file == null) {
      throw new IllegalArgumentException("file to analyze cannot be null");
    }
    MediaContainerMetadata metadata = null;
    try {
      MediaAnalyzer analyzer = new MediaInfoAnalyzer();
      analyzer.setConfig(analyzerConfig);
      metadata = analyzer.analyze(file);
    } catch (MediaAnalyzerException e) {
      throw new MediaInspectionException(e);
    }
    return metadata;
  }

  /**
   * Updates the job in the service registry. The exceptions that are possibly been thrown are wrapped in a
   * {@link MediaInspectionException}.
   * 
   * @param job
   *          the job to update
   * @throws MediaInspectionException
   *           the exception that is being thrown
   */
  private void updateJob(Job job) throws MediaInspectionException {
    try {
      serviceRegistry.updateJob(job);
    } catch (NotFoundException notFound) {
      throw new MediaInspectionException("Unable to find job " + job, notFound);
    } catch (ServiceUnavailableException e) {
      throw new MediaInspectionException("No service of type '" + JOB_TYPE + "' available", e);
    } catch (ServiceRegistryException serviceRegException) {
      throw new MediaInspectionException("Unable to update job '" + job + "' in service registry", serviceRegException);
    }
  }

}
