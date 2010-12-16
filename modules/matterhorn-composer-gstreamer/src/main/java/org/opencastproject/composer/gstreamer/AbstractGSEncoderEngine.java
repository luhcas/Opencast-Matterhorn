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

import org.opencastproject.composer.api.EncoderEngine;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncoderListener;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.api.EncodingProfile.MediaType;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.activation.MimetypesFileTypeMap;

/**
 * Abstract base class for GStreamer encoder engines.
 */
public abstract class AbstractGSEncoderEngine implements EncoderEngine {

  /** Logging utility */
  private static Logger logger = LoggerFactory.getLogger(AbstractGSEncoderEngine.class);

  /** List of installed listeners */
  protected List<EncoderListener> listeners = new CopyOnWriteArrayList<EncoderListener>();

  /** Supported profiles for this engine */
  protected Map<String, EncodingProfile> supportedProfiles = new HashMap<String, EncodingProfile>();

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.opencastproject.composer.api.EncoderEngine#addEncoderListener(org.opencastproject.composer.api.EncoderListener)
   */
  @Override
  public void addEncoderListener(EncoderListener listener) {
    if (!listeners.contains(listener))
      listeners.add(listener);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.opencastproject.composer.api.EncoderEngine#removeEncoderListener(org.opencastproject.composer.api.EncoderListener
   * )
   */
  @Override
  public void removeEncoderListener(EncoderListener listener) {
    listeners.remove(listener);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.composer.api.EncoderEngine#encode(java.io.File,
   * org.opencastproject.composer.api.EncodingProfile, java.util.Map)
   */
  @Override
  public File encode(File mediaSource, EncodingProfile format, Map<String, String> properties) throws EncoderException {
    return process(null, mediaSource, format, properties);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.composer.api.EncoderEngine#mux(java.io.File, java.io.File,
   * org.opencastproject.composer.api.EncodingProfile, java.util.Map)
   */
  @Override
  public File mux(File audioSource, File videoSource, EncodingProfile format, Map<String, String> properties)
          throws EncoderException {
    return process(audioSource, videoSource, format, properties);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.composer.api.EncoderEngine#trim(java.io.File,
   * org.opencastproject.composer.api.EncodingProfile, long, long, java.util.Map)
   */
  @Override
  public File trim(File mediaSource, EncodingProfile format, long start, long duration, Map<String, String> properties)
          throws EncoderException {

    properties.put("trim.start", Long.toString(start));
    properties.put("trim.duration", Long.toString(duration));

    return process(null, mediaSource, format, properties);
  }

  /**
   * Executes encoding job. At least one source has to be specified.
   * 
   * @param audioSource
   *          File that contains audio source (if used)
   * @param videoSource
   *          File that contains video source (if used)
   * @param profile
   *          EncodingProfile used for this encoding job
   * @param properties
   *          Map containing any additional properties
   * @return File created as result of this encoding job
   * @throws EncoderException
   *           if encoding fails
   */
  protected File process(File audioSource, File videoSource, EncodingProfile profile, Map<String, String> properties)
          throws EncoderException {

    Map<String, String> params = new HashMap<String, String>(properties);

    try {
      if (audioSource == null && videoSource == null) {
        throw new IllegalArgumentException("At least one source must be specified.");
      }

      // Set encoding parameters
      if (audioSource != null) {
        String audioInput = FilenameUtils.normalize(audioSource.getAbsolutePath());
        params.put("in.audio.path", audioInput);
        params.put("in.audio.mimetype", MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(audioInput));
      }
      if (videoSource != null) {
        String videoInput = FilenameUtils.normalize(videoSource.getAbsolutePath());
        params.put("in.video.path", videoInput);
        params.put("in.video.mimetype", MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(videoInput));
      }
      File parentFile;
      if (videoSource == null) {
        parentFile = audioSource;
      } else {
        parentFile = videoSource;
      }
      String outDir = parentFile.getAbsoluteFile().getParent();
      String outFileName = FilenameUtils.getBaseName(parentFile.getName());
      String outSuffix = profile.getSuffix();

      if (new File(outDir, outFileName + outSuffix).exists()) {
        outFileName += "_reencode";
      }

      // where to put output file
      params.put("out.file.path", new File(outDir, outFileName + outSuffix).getAbsolutePath());

      // create and launch gstreamer pipeline
      createAndLaunchPipeline(profile, params);

      if (audioSource != null) {
        logger.info("Audio track {} and video track {} successfully encoded using profile '{}'",
                new String[] { (audioSource == null ? "N/A" : audioSource.getName()),
                        (videoSource == null ? "N/A" : videoSource.getName()), profile.getIdentifier() });
      } else {
        logger.info("Video track {} successfully encoded using profile '{}'", new String[] { videoSource.getName(),
                profile.getIdentifier() });
      }
      fireEncoded(this, profile, audioSource, videoSource);
      return new File(outDir, outFileName + outSuffix);
    } catch (EncoderException e) {
      if (audioSource != null) {
        logger.warn(
                "Error while encoding audio track {} and video track {} using '{}': {}",
                new String[] { (audioSource == null ? "N/A" : audioSource.getName()),
                        (videoSource == null ? "N/A" : videoSource.getName()), profile.getIdentifier(), e.getMessage() });
      } else {
        logger.warn("Error while encoding video track {} using '{}': {}", new String[] {
                (videoSource == null ? "N/A" : videoSource.getName()), profile.getIdentifier(), e.getMessage() });
      }
      fireEncodingFailed(this, profile, e, audioSource, videoSource);
      throw e;
    } catch (Exception e) {
      logger.warn("Error while encoding audio {} and video {} to {}:{}, {}",
              new Object[] { (audioSource == null ? "N/A" : audioSource.getName()),
                      (videoSource == null ? "N/A" : videoSource.getName()), profile.getName(), e.getMessage() });
      fireEncodingFailed(this, profile, e, audioSource, videoSource);
      throw new EncoderException(this, e.getMessage(), e);
    }
  }

  /**
   * Creates Pipeline from profile and additional properties and launches it.
   * 
   * @param profile
   *          EncodingProfile used for creating Pipeline
   * @param properties
   *          additional properties for creating Pipeline
   * @throws EncoderException
   *           if Pipeline creation or execution fails
   */
  protected abstract void createAndLaunchPipeline(EncodingProfile profile, Map<String, String> properties)
          throws EncoderException;

  /*
   * (non-Javadoc)
   * @see org.opencastproject.composer.api.EncoderEngine#supportsMultithreading()
   */
  @Override
  public boolean supportsMultithreading() {
    return true;
  }

  /*
   * (non-Javadoc)
   * @see org.opencastproject.composer.api.EncoderEngine#supportsProfile(java.lang.String, org.opencastproject.composer.api.EncodingProfile.MediaType)
   */
  @Override
  public boolean supportsProfile(String profile, MediaType type) {
    if (supportedProfiles.containsKey(profile)) {
      EncodingProfile p = supportedProfiles.get(profile);
      return p.isApplicableTo(type);
    }
    return false;
  }

  /*
   * (non-Javadoc)
   * @see org.opencastproject.composer.api.EncoderEngine#needsLocalWorkCopy()
   */
  @Override
  public boolean needsLocalWorkCopy() {
    // TODO probably yes, at least if filesrc is used (not extensively tested yet)
    return false;
  }

  /**
   * This method is called to send the <code>formatEncoded</code> event to registered encoding listeners.
   * 
   * @param engine
   *          the encoding engine
   * @param profile
   *          the media format
   * @param sourceFiles
   *          the source files encoded
   */
  protected void fireEncoded(EncoderEngine engine, EncodingProfile profile, File... sourceFiles) {
    for (EncoderListener l : listeners) {
      try {
        l.fileEncoded(engine, profile, sourceFiles);
      } catch (Throwable t) {
        logger.error("Encoder listener " + l + " threw exception while handling callback");
      }
    }
  }

  /**
   * This method is called to send the <code>trackEncodingFailed</code> event to registered encoding listeners.
   * 
   * @param engine
   *          the encoding engine
   * @param sourceFiles
   *          the files that were encoded
   * @param profile
   *          the media format
   * @param cause
   *          the reason of failure
   */
  protected void fireEncodingFailed(EncoderEngine engine, EncodingProfile profile, Throwable cause, File... sourceFiles) {
    for (EncoderListener l : listeners) {
      try {
        l.fileEncodingFailed(engine, profile, cause, sourceFiles);
      } catch (Throwable t) {
        logger.error("Encoder listener {} threw exception while handling callback", l);
      }
    }
  }

  /**
   * This method is called to send the <code>trackEncodingProgressed</code> event to registered encoding listeners.
   * 
   * @param engine
   *          the encoding engine
   * @param sourceFile
   *          the file that is being encoded
   * @param profile
   *          the media format
   * @param progress
   *          the progress value
   */
  protected void fireEncodingProgressed(EncoderEngine engine, File sourceFile, EncodingProfile profile, int progress) {
    for (EncoderListener l : listeners) {
      try {
        l.fileEncodingProgressed(engine, sourceFile, profile, progress);
      } catch (Throwable t) {
        logger.error("Encoder listener " + l + " threw exception while handling callback");
      }
    }
  }
}
