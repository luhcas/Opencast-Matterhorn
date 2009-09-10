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

package org.opencastproject.composer.impl.ffmpeg;

import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.impl.CmdlineEncoderEngine;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.util.ConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Implementation for the encoder engine backed by ffmpeg.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public class FFmpegEncoderEngine extends CmdlineEncoderEngine {

  /** Default location of the ffmepg binary (resembling the installer) */
  private static final String FFMPEG_BINARY_DEFAULT = "/usr/local/bin/ffmpeg";

  /** Name of the ffmpeg binary option */
  private static final String OPT_BINARY = "ffmpeg.binary";

  /** The ffmpeg properties file */
  public static final String PROPERTIES_FILE = "/ffmpeg.properties";

  /** The ffmpeg properties key prefix */
  private static final String PROFILE_PREFIX = "profile.";

  /** The ffmpeg commandline suffix */
  public static final String CMD_SUFFIX = "ffmpeg.commandline";

  /** The commandline definitions */
  protected static Map<String, String> commandlines = null;

  /** the logging facility provided by log4j */
  private static final Logger log_ = LoggerFactory.getLogger(FFmpegEncoderEngine.class);

  /**
   * Creates the ffmpeg encoder engine.
   */
  public FFmpegEncoderEngine() {
    super(FFMPEG_BINARY_DEFAULT);
    initEncoder();
  }

  /**
   * Initializes the FFMpeg encoder by looking up the commandlines for the various distribution formats.
   * 
   * @throws ConfigurationException
   */
  private void initEncoder() throws ConfigurationException {
    Properties ffmpegProperties = new Properties();
    try {
      ffmpegProperties.load(FFmpegEncoderEngine.class.getResourceAsStream(PROPERTIES_FILE));
    } catch (IOException e) {
      log_.error("Error reading configuration for ffmpeg encoder engine: " + e.getMessage());
    }

    // Check binary location
    String binary = ffmpegProperties.getProperty(OPT_BINARY);
    if (binary != null) {
      setBinary(binary);
    }

    // Read formats
    if (commandlines == null) {
      commandlines = new HashMap<String, String>();
      for (Map.Entry<Object, Object> entry : ffmpegProperties.entrySet()) {
        String key = entry.getKey().toString();
        if (key.startsWith(PROFILE_PREFIX) && key.endsWith(CMD_SUFFIX)) {
          int endIndex = key.length() - CMD_SUFFIX.length() - 1;
          String formatKey = key.substring(PROFILE_PREFIX.length(), endIndex);
          String commandline = entry.getValue().toString();
          commandlines.put(formatKey, commandline);
          log_.debug("Commandline for '" + formatKey + "' is " + commandline);
        }
      }
    }
  }

  /**
   * Creates the arguments for the commandline.
   * 
   * @param track
   *          the track that is to be encoded
   * @param format
   *          the format
   * @param profile
   *          the encoding profile
   * @return the argument list
   */
  protected List<String> buildArgumentList(Track track, EncodingProfile format, String profile) throws EncoderException {
    List<String> argumentList = new ArrayList<String>();
    String commandline = commandlines.get(format.getIdentifier());
    if (commandline == null) {
      commandline = format.getExtension(CMD_SUFFIX);
      if (commandline == null)
        throw new EncoderException(this, "No commandline configured for " + format);
    }
    String[] args = commandline.split(" ");
    for (String a : args)
      argumentList.add(a);
    return argumentList;
  }

  /**
   * Handles the encoder output by analyzing it first and then firing it off to the registered listeners.
   * 
   * @param track
   *          the track that is currently being encoded
   * @param format
   *          the target media format
   * @param message
   *          the message returned by the encoder
   */
  @Override
  protected void handleEncoderOutput(Track track, EncodingProfile format, String message) {
    super.handleEncoderOutput(track, format, message);
    message = message.trim();
    if (message.equals(""))
      return;

    // Completely skip these messages
    if (message.startsWith("Press ["))
      return;

    // Others go to trace logging
    if (message.startsWith("FFmpeg version") || message.startsWith("configuration") || message.startsWith("lib")
            || message.startsWith("frame=") || message.startsWith("built on"))

      log_.trace(message);

    // Some to debug
    else if (message.startsWith("Input #") || message.startsWith("Duration:") || message.startsWith("Stream #")
            || message.startsWith("Stream mapping") || message.startsWith("Output #") || message.startsWith("video:")
            || message.startsWith("PIX_FMT_YUV420P will be used as an intermediate format for rescaling"))

      log_.debug(message);

    // And the rest is likely to deserve warning status
    else
      log_.warn(message);
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "ffmpeg";
  }

}