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
package org.opencastproject.composer.gstreamer;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EmbedderException;
import org.opencastproject.composer.api.EncoderEngine;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.inspection.api.MediaInspectionException;
import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.mediapackage.AbstractMediaPackageElement;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.identifier.IdBuilder;
import org.opencastproject.mediapackage.identifier.IdBuilderFactory;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.serviceregistry.api.ServiceUnavailableException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.NotImplementedException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author nejc
 * 
 */
public class GStreamerComposerService implements ComposerService {

  /** The logging instance */
  private static final Logger logger = LoggerFactory.getLogger(GStreamerComposerService.class);

  /** The collection name */
  public static final String COLLECTION = "composer-gs";

  /** Encoding profile manager */
  private GSEncodingProfileScanner profileScanner = null;

  /** Reference to the media inspection service */
  private MediaInspectionService inspectionService = null;

  /** Reference to the workspace service */
  private Workspace workspace = null;

  /** Reference to the receipt service */
  private ServiceRegistry serviceRegistry;

  /** Reference to the encoder engine factory */
  private GStreamerFactory encoderEngineFactory;

  /** Id builder used to create ids for encoded tracks */
  private final IdBuilder idBuilder = IdBuilderFactory.newInstance().newIdBuilder();

  /** Thread pool */
  private ExecutorService executor = null;

  /** The configuration property containing the number of concurrent encoding threads to run */
  public static final String CONFIG_THREADS = "org.opencastproject.gscomposer.threads";

  /** The default number of concurrent encoding threads to run */
  public static final int DEFAULT_THREADS = 2;

  /**
   * Sets the media inspection service
   * 
   * @param mediaInspectionService
   *          an instance of the media inspection service
   */
  public void setMediaInspectionService(MediaInspectionService mediaInspectionService) {
    this.inspectionService = mediaInspectionService;
  }

  /**
   * Sets the gstreamer encoder engine factory
   * 
   * @param encoderEngineFactory
   *          The encoder engine factory
   */
  public void setGSEncoderEngineFactory(GStreamerFactory gsFactory) {
    this.encoderEngineFactory = gsFactory;
  }

  /**
   * Sets the workspace
   * 
   * @param workspace
   *          an instance of the workspace
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * Sets the receipt service
   * 
   * @param remoteServiceManager
   */
  public void setRemoteServiceManager(ServiceRegistry remoteServiceManager) {
    this.serviceRegistry = remoteServiceManager;
  }

  /**
   * Activator that will make sure the encoding profiles are loaded.
   */
  protected void activate(ComponentContext cc) {
    // set up threading
    int threads;
    String configredThreads = (String) cc.getBundleContext().getProperty(CONFIG_THREADS);
    // try to parse the value as a number. If it fails to parse, there is a config problem so we throw an exception.
    if (configredThreads == null) {
      threads = DEFAULT_THREADS;
    } else {
      threads = Integer.parseInt(configredThreads);
    }
    if (threads < 1) {
      throw new IllegalStateException("The composer needs one or more threads to function.");
    }
    setExecutorThreads(threads);
  }

  void setExecutorThreads(int threads) {
    executor = Executors.newFixedThreadPool(threads);
    logger.info("Thread pool size = {}", threads);
  }

  public void setProfileScanner(GSEncodingProfileScanner scanner) {
    this.profileScanner = scanner;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.job.api.JobProducer#getJob(long)
   */
  @Override
  public Job getJob(long id) throws NotFoundException, ServiceRegistryException {
    return serviceRegistry.getJob(id);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.job.api.JobProducer#countJobs(org.opencastproject.job.api.Job.Status)
   */
  @Override
  public long countJobs(Status status) throws ServiceRegistryException {
    return serviceRegistry.count(JOB_TYPE, status);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.job.api.JobProducer#countJobs(org.opencastproject.job.api.Job.Status, java.lang.String)
   */
  @Override
  public long countJobs(Status status, String host) throws ServiceRegistryException {
    return serviceRegistry.count(JOB_TYPE, status, host);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.composer.api.ComposerService#encode(org.opencastproject.mediapackage.Track,
   * java.lang.String)
   */
  @Override
  public Job encode(Track sourceTrack, String profileId) throws EncoderException, MediaPackageException {
    return encode(sourceTrack, profileId, false);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.composer.api.ComposerService#encode(org.opencastproject.mediapackage.Track,
   * java.lang.String, boolean)
   */
  @Override
  public Job encode(Track sourceTrack, String profileId, boolean block) throws EncoderException, MediaPackageException {
    return encode(sourceTrack, null, profileId, null, block);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.composer.api.ComposerService#mux(org.opencastproject.mediapackage.Track,
   * org.opencastproject.mediapackage.Track, java.lang.String)
   */
  @Override
  public Job mux(Track sourceVideoTrack, Track sourceAudioTrack, String profileId) throws EncoderException, MediaPackageException {
    return encode(sourceVideoTrack, sourceAudioTrack, profileId, null, false);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.composer.api.ComposerService#mux(org.opencastproject.mediapackage.Track,
   * org.opencastproject.mediapackage.Track, java.lang.String, boolean)
   */
  @Override
  public Job mux(Track sourceVideoTrack, Track sourceAudioTrack, String profileId, boolean block)
          throws EncoderException, MediaPackageException {
    return encode(sourceVideoTrack, sourceAudioTrack, profileId, null, block);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.composer.api.ComposerService#trim(org.opencastproject.mediapackage.Track,
   * java.lang.String, long, long)
   */
  @Override
  public Job trim(Track sourceTrack, String profileId, long start, long duration) throws EncoderException,
          MediaPackageException {
    return trim(sourceTrack, profileId, start, duration, false);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.composer.api.ComposerService#trim(org.opencastproject.mediapackage.Track,
   * java.lang.String, long, long, boolean)
   */
  @Override
  public Job trim(final Track sourceTrack, final String profileId, final long start, final long duration, boolean block)
          throws EncoderException, MediaPackageException {

    final String targetTrackId = idBuilder.createNew().toString();
    final Job job;
    try {
      job = serviceRegistry.createJob(JOB_TYPE, TRIM_OPERATION,
              Arrays.asList(sourceTrack.getAsXml(), profileId, Long.toString(start), Long.toString(duration)));
    } catch (ServiceUnavailableException e) {
      throw new EncoderException("The " + JOB_TYPE
              + " service is not registered on this host, so no job can be created", e);
    } catch (ServiceRegistryException e) {
      throw new EncoderException("Unable to create a job", e);
    }

    Callable<Void> command = new Callable<Void>() {
      /**
       * {@inheritDoc}
       * 
       * @see java.util.concurrent.Callable#call()
       */
      @Override
      public Void call() throws EncoderException {
        try {
          job.setStatus(Status.RUNNING);
          updateJob(job);

          // Get the track and make sure it exists
          final File trackFile;
          if (sourceTrack == null) {
            trackFile = null;
          } else {
            trackFile = getTrack(sourceTrack);
          }

          // Get the encoding profile
          final EncodingProfile profile = profileScanner.getProfile(profileId);
          if (profile == null) {
            throw new EncoderException("Profile '" + profileId + " is unkown");
          }

          // Create the engine
          final EncoderEngine encoderEngine = encoderEngineFactory.newEncoderEngine(profile);
          if (encoderEngine == null) {
            throw new EncoderException(encoderEngine, "No encoder engine available for profile '" + profileId + "'");
          }

          // Do the work
          File encodingOutput = encoderEngine.trim(trackFile, profile, start, duration, null);

          // Put the file in the workspace
          URI returnURL = null;
          InputStream in = null;
          try {
            in = new FileInputStream(encodingOutput);
            returnURL = workspace.putInCollection(COLLECTION,
                    job.getId() + "." + FilenameUtils.getExtension(encodingOutput.getAbsolutePath()), in);
            logger.info("Copied the trimmed file to the workspace at {}", returnURL);
          } catch (FileNotFoundException e) {
            throw new EncoderException("Encoded file " + encodingOutput + " not found", e);
          } catch (IOException e) {
            throw new EncoderException("Error putting " + encodingOutput + " into the workspace", e);
          } finally {
            IOUtils.closeQuietly(in);
          }
          if (encodingOutput != null) {
            String encodingOutputPath = encodingOutput.getAbsolutePath();
            if (encodingOutput.delete()) {
              logger.info("Deleted local copy of the trimmed file at {}", encodingOutputPath);
            } else {
              logger.warn("Could not delete local copy of the trimmed file at {}", encodingOutputPath);
            }
          }
          // Have the encoded track inspected and return the result
          Job inspectionJob = null;
          try {
            inspectionJob = inspectionService.inspect(returnURL, true);
          } catch (MediaInspectionException e) {
            throw new EncoderException("Media inspection of " + returnURL + " failed", e);
          }
          if (inspectionJob.getStatus() == Job.Status.FAILED)
            throw new EncoderException("Media inspection of " + returnURL + " failed");
          Track inspectedTrack = (Track) AbstractMediaPackageElement.getFromXml(inspectionJob.getPayload());
          inspectedTrack.setIdentifier(targetTrackId);

          job.setPayload(inspectedTrack.getAsXml());
          job.setStatus(Status.FINISHED);
          updateJob(job);

          return null;
        } catch (Exception e) {
          logger.warn("Error trimming " + sourceTrack, e);
          try {
            job.setStatus(Status.FAILED);
            updateJob(job);
          } catch (Exception failureToFail) {
            logger.warn("Unable to update job to failed state", failureToFail);
          }
          if (e instanceof EncoderException) {
            throw (EncoderException) e;
          } else {
            throw new EncoderException(e);
          }
        }
      }
    };

    Future<?> future = executor.submit(command);
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
        if (e instanceof EncoderException) {
          throw (EncoderException) e;
        } else {
          throw new EncoderException(e);
        }
      }
    }

    return job;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.composer.api.ComposerService#image(org.opencastproject.mediapackage.Track,
   * java.lang.String, long)
   */
  @Override
  public Job image(Track sourceTrack, String profileId, long time) throws EncoderException, MediaPackageException {
    return image(sourceTrack, profileId, time, false);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.composer.api.ComposerService#image(org.opencastproject.mediapackage.Track,
   * java.lang.String, long, boolean)
   */
  @Override
  public Job image(final Track sourceTrack, final String profileId, final long time, boolean block)
          throws EncoderException, MediaPackageException {

    final Job job;
    try {
      job = serviceRegistry.createJob(JOB_TYPE, IMAGE_OPERATION,
              Arrays.asList(sourceTrack.getAsXml(), profileId, Long.toString(time)));
    } catch (ServiceUnavailableException e) {
      throw new EncoderException("The " + JOB_TYPE
              + " service is not registered on this host, so no job can be created", e);
    } catch (ServiceRegistryException e) {
      throw new EncoderException("Unable to create a job", e);
    }

    Callable<Void> command = new Callable<Void>() {
      /**
       * {@inheritDoc}
       * 
       * @see java.util.concurrent.Callable#call()
       */
      @Override
      public Void call() throws EncoderException {
        try {
          job.setStatus(Status.RUNNING);
          updateJob(job);

          if (sourceTrack == null)
            throw new EncoderException("SourceTrack cannot be null");

          logger.info("creating an image using video track {}", sourceTrack.getIdentifier());

          job.setStatus(Status.RUNNING);
          updateJob(job);

          // Get the encoding profile
          final EncodingProfile profile = profileScanner.getProfile(profileId);
          if (profile == null) {
            throw new EncoderException("Profile '" + profileId + "' is unknown");
          }

          // Create the encoding engine
          final EncoderEngine encoderEngine = encoderEngineFactory.newEncoderEngine(profile);
          if (encoderEngine == null) {
            throw new EncoderException("No encoder engine available for profile '" + profileId + "'");
          }

          // make sure there is a video stream in the track
          if (sourceTrack != null && !sourceTrack.hasVideo()) {
            throw new EncoderException("Cannot extract an image without a video stream");
          }

          // The time should not be outside of the track's duration
          if (time < 0 || time > sourceTrack.getDuration()) {
            throw new EncoderException("Can not extract an image at time " + Long.valueOf(time)
                    + " from a track with duration " + Long.valueOf(sourceTrack.getDuration()));
          }

          // Finally get the file that needs to be encoded
          final File videoFile = getTrack(sourceTrack);

          Map<String, String> properties = new HashMap<String, String>();
          String timeAsString = Long.toString(time);
          properties.put("time", timeAsString);

          // Do the work
          File encodingOutput = encoderEngine.encode(videoFile, profile, properties);

          if (encodingOutput == null || !encodingOutput.isFile()) {
            throw new EncoderException("Image extraction failed: encoding output doesn't exist at " + encodingOutput);
          }

          // Put the file in the workspace
          URI returnURL = null;
          InputStream in = null;
          try {
            in = new FileInputStream(encodingOutput);
            returnURL = workspace.putInCollection(COLLECTION,
                    job.getId() + "." + FilenameUtils.getExtension(encodingOutput.getAbsolutePath()), in);
            logger.debug("Copied image file to the workspace at {}", returnURL);
          } catch (Exception e) {
            throw new EncoderException("Unable to put image file into the workspace", e);
          } finally {
            IOUtils.closeQuietly(in);
          }
          if (encodingOutput != null) {
            String encodingOutputPath = encodingOutput.getAbsolutePath();
            if (encodingOutput.delete()) {
              logger.info("Deleted local copy of the image file at {}", encodingOutputPath);
            } else {
              logger.warn("Could not delete local copy of the image file at {}", encodingOutputPath);
            }
          }

          MediaPackageElementBuilder builder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
          Attachment attachment = (Attachment) builder.elementFromURI(returnURL, Attachment.TYPE, null);

          job.setPayload(attachment.getAsXml());
          job.setStatus(Status.FINISHED);
          updateJob(job);

          return null;
        } catch (Exception e) {
          logger.warn("Error extracting image from " + sourceTrack, e);
          try {
            job.setStatus(Status.FAILED);
            updateJob(job);
          } catch (Exception failureToFail) {
            logger.warn("Unable to update job to failed state", failureToFail);
          }
          if (e instanceof EncoderException) {
            throw (EncoderException) e;
          } else {
            throw new EncoderException(e);
          }
        }
      }
    };

    Future<?> future = executor.submit(command);
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
        if (e instanceof EncoderException) {
          throw (EncoderException) e;
        } else {
          throw new EncoderException(e);
        }
      }
    }

    return job;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.composer.api.ComposerService#captions(org.opencastproject.mediapackage.Track,
   * org.opencastproject.mediapackage.Catalog[])
   */
  @Override
  public Job captions(Track mediaTrack, Catalog[] captions) throws EmbedderException {
    throw new NotImplementedException("Adding captions not implemented in gstreamer composer");
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.composer.api.ComposerService#captions(org.opencastproject.mediapackage.Track,
   * org.opencastproject.mediapackage.Catalog[], boolean)
   */
  @Override
  public Job captions(Track mediaTrack, Catalog[] captions, boolean block) throws EmbedderException {
    throw new NotImplementedException("Adding captions not implemented in gstreamer composer");
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.composer.api.ComposerService#listProfiles()
   */
  @Override
  public EncodingProfile[] listProfiles() {
    Collection<EncodingProfile> profiles = profileScanner.getProfiles().values();
    return profiles.toArray(new EncodingProfile[profiles.size()]);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.composer.api.ComposerService#getProfile(java.lang.String)
   */
  @Override
  public EncodingProfile getProfile(String profileId) {
    return profileScanner.getProfiles().get(profileId);
  }

  /**
   * Encodes audio and video track to a file. If both an audio and a video track are given, they are muxed together into
   * one movie container.
   * 
   * @param videoTrack
   *          the video track
   * @param audioTrack
   *          the audio track
   * @param profileId
   *          the encoding profile
   * @param properties
   *          encoding properties
   * @param block
   *          <code>true</code> to only return once encoding is finished
   * @return the receipt
   * @throws EncoderException
   *           if encoding fails
   */
  private Job encode(final Track videoTrack, final Track audioTrack, final String profileId,
          Dictionary<String, String> properties, final boolean block) throws EncoderException, MediaPackageException {

    String video = videoTrack == null ? null : videoTrack.getAsXml();
    String audio = audioTrack == null ? null : audioTrack.getAsXml();
    StringBuilder propertiesAsString = new StringBuilder();
    if(properties != null) {
      Enumeration<String> elements = properties.elements();
      while(elements.hasMoreElements()) {
        String key = elements.nextElement();
        propertiesAsString.append(key);
        propertiesAsString.append("=");
        propertiesAsString.append(properties.get(key));
        propertiesAsString.append("\n");
      }
    }

    final String targetTrackId = idBuilder.createNew().toString();
    final Job job;
    try {
      job = serviceRegistry.createJob(JOB_TYPE, ENCODE_OPERATION,
              Arrays.asList(video, audio, profileId, propertiesAsString.toString()));
    } catch (ServiceUnavailableException e) {
      throw new EncoderException("The " + JOB_TYPE
              + " service is not registered on this host, so no job can be created", e);
    } catch (ServiceRegistryException e) {
      throw new EncoderException("Unable to create a job", e);
    }

    Callable<Void> command = new Callable<Void>() {
      /**
       * {@inheritDoc}
       * 
       * @see java.util.concurrent.Callable#call()
       */
      @Override
      public Void call() throws EncoderException {
        try {
          job.setStatus(Status.RUNNING);
          updateJob(job);

          // Get the tracks and make sure they exist
          final File audioFile;
          if (audioTrack == null) {
            audioFile = null;
          } else {
            audioFile = getTrack(audioTrack);
          }

          final File videoFile;
          if (videoTrack == null) {
            videoFile = null;
          } else {
            videoFile = getTrack(videoTrack);
          }

          // Create the engine
          final EncodingProfile profile = profileScanner.getProfile(profileId);
          if (profile == null) {
            throw new EncoderException(null, "Profile '" + profileId + " is unkown");
          }
          final EncoderEngine encoderEngine = encoderEngineFactory.newEncoderEngine(profile);
          if (encoderEngine == null) {
            throw new EncoderException(null, "No encoder engine available for profile '" + profileId + "'");
          }

          if (audioTrack != null && videoTrack != null)
            logger.info("Muxing audio track {} and video track {} into {}", new String[] { audioTrack.getIdentifier(),
                    videoTrack.getIdentifier(), targetTrackId });
          else if (audioTrack == null)
            logger.info("Encoding video track {} to {} using profile '{}'", new String[] { videoTrack.getIdentifier(),
                    targetTrackId, profileId });
          else if (videoTrack == null)
            logger.info("Encoding audio track {} to {} using profile '{}'", new String[] { audioTrack.getIdentifier(),
                    targetTrackId, profileId });

          // Do the work
          File encodingOutput = encoderEngine.mux(audioFile, videoFile, profile, null);

          // Put the file in the workspace
          URI returnURL = null;
          InputStream in = null;
          try {
            in = new FileInputStream(encodingOutput);
            returnURL = workspace.putInCollection(COLLECTION,
                    job.getId() + "." + FilenameUtils.getExtension(encodingOutput.getAbsolutePath()), in);
            logger.info("Copied the encoded file to the workspace at {}", returnURL);
          } catch (Exception e) {
            throw new EncoderException("Unable to put the encoded file into the workspace", e);
          } finally {
            IOUtils.closeQuietly(in);
          }
          if (encodingOutput != null) {
            String encodingOutputPath = encodingOutput.getAbsolutePath();
            if (encodingOutput.delete()) {
              logger.info("Deleted local copy of encoded file at {}", encodingOutputPath);
            } else {
              logger.warn("Could not delete local copy of encoded file at {}", encodingOutputPath);
            }
          }

          // Have the encoded track inspected and return the result
          Job inspectionJob = null;
          try {
            inspectionJob = inspectionService.inspect(returnURL, true);
          } catch (MediaInspectionException e) {
            throw new EncoderException("Media inspection of " + returnURL + " failed", e);
          }
          if (inspectionJob.getStatus() == Job.Status.FAILED)
            throw new EncoderException("Media inspection of " + returnURL + " failed");
          Track inspectedTrack = (Track) AbstractMediaPackageElement.getFromXml(inspectionJob.getPayload());
          inspectedTrack.setIdentifier(targetTrackId);

          job.setPayload(inspectedTrack.getAsXml());
          job.setStatus(Status.FINISHED);
          updateJob(job);

          return null;
        } catch (Exception e) {
          logger.warn("Error encoding " + videoTrack + " and " + audioTrack, e);
          try {
            job.setStatus(Status.FAILED);
            updateJob(job);
          } catch (Exception failureToFail) {
            logger.warn("Unable to update job to failed state", failureToFail);
          }
          if (e instanceof EncoderException) {
            throw (EncoderException) e;
          } else {
            throw new EncoderException(e);
          }
        }
      }
    };

    Future<?> future = executor.submit(command);
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
        if (e instanceof EncoderException) {
          throw (EncoderException) e;
        } else {
          throw new EncoderException(e);
        }
      }
    }

    return job;
  }

  /**
   * Updates the job in the service registry. The exceptions that are possibly been thrown are wrapped in a
   * {@link EncoderException}.
   * 
   * @param job
   *          the job to update
   * @throws EncoderException
   *           the exception that is being thrown
   */
  private void updateJob(Job job) throws EncoderException {
    try {
      serviceRegistry.updateJob(job);
    } catch (NotFoundException notFound) {
      throw new EncoderException("Unable to find job " + job, notFound);
    } catch (ServiceRegistryException serviceRegException) {
      throw new EncoderException("Unable to update job '" + job + "' in service registry", serviceRegException);
    } catch (ServiceUnavailableException e) {
      throw new EncoderException("No service of type '" + JOB_TYPE + "' available", e);
    }
  }

  private File getTrack(Track track) throws EncoderException {
    try {
      return workspace.get(track.getURI());
    } catch (NotFoundException e) {
      throw new EncoderException("Requested track " + track + " is not found");
    } catch (IOException e) {
      throw new EncoderException("Unable to access track " + track);
    }
  }

}
