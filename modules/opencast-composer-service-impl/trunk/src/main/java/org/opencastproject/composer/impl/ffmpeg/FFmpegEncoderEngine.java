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
import org.opencastproject.composer.impl.AbstractCmdlineEncoderEngine;
import org.opencastproject.util.ConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Implementation for the encoder engine backed by ffmpeg.
 */
public class FFmpegEncoderEngine extends AbstractCmdlineEncoderEngine {

  /** Default location of the ffmepg binary (resembling the installer) */
  public static final String FFMPEG_BINARY_DEFAULT = "/usr/local/bin/ffmpeg";
  public static final String CONFIG_FFMPEG_BINARY = "ffmpeg.binary";

  /** Name of the ffmpeg binary option */
  private static final String OPT_BINARY = "ffmpeg.binary";

  /** The ffmpeg properties file */
  public static final String PROPERTIES_FILE = "/ffmpeg.properties";

  /** The ffmpeg commandline suffix */
  public static final String CMD_SUFFIX = "ffmpeg.command";

  /** the logging facility provided by log4j */
  private static final Logger log_ = LoggerFactory
          .getLogger(FFmpegEncoderEngine.class);

  /**
   * Creates the ffmpeg encoder engine.
   */
  public FFmpegEncoderEngine() {
    super(FFMPEG_BINARY_DEFAULT);
    initEncoder();
  }

  /**
   * Allows for configuration
   * @param config
   */
  public void setConfig(Map<String, Object> config) {
    if (config != null) {
      if (config.containsKey(CONFIG_FFMPEG_BINARY)) {
        String binary = (String)config.get(CONFIG_FFMPEG_BINARY);
        setBinary(binary);
        log_.info("FFmpegEncoderEngine config binary: "+binary);
      }
    }
  }

  /**
   * Initializes the FFMpeg encoder by looking up the commandlines for the
   * various distribution formats.
   * 
   * @throws ConfigurationException
   */
  private synchronized void initEncoder() throws ConfigurationException {
    Properties ffmpegProperties = new Properties();
    try {
      ffmpegProperties.load(FFmpegEncoderEngine.class
              .getResourceAsStream(PROPERTIES_FILE));
    } catch (IOException e) {
      log_.error("Error reading configuration for ffmpeg encoder engine: "
              + e.getMessage());
    }

    // Check binary location
    String binary = ffmpegProperties.getProperty(OPT_BINARY);
    if (binary != null) {
      setBinary(binary);
    }
  }

  /**
   * Creates the arguments for the commandline.
   * 
   * @param file
   *          the file that is to be encoded
   * @param profile
   *          the format
   * @return the argument list
   */
  @Override
  protected List<String> buildArgumentList(EncodingProfile format)
          throws EncoderException {
    String commandline = format.getExtension(CMD_SUFFIX);
    if (commandline == null)
      throw new EncoderException(this, "No commandline configured for "
              + format);

    // Process the commandline. The variables in that commandline might either
    // be replaced by commandline parts from the configuration or commandline
    // parameters as specified at runtime.
    List<String> argumentList = new ArrayList<String>();
    for (Map.Entry<String, String> entry : format.getExtensions().entrySet()) {
      String key = entry.getKey();
      if (key.startsWith(CMD_SUFFIX) && key.length() > CMD_SUFFIX.length()) {
        String value = replaceCommandlineParameters(entry.getValue());
        String partName = "#\\{" + key.substring(CMD_SUFFIX.length() + 1) + "\\}";
        if (!value.matches(".*#\\{.*\\}.*"))
          commandline = commandline.replaceAll(partName, value);
      }
    }

    // Replace the commandline parameters passed in at compile time
    commandline = replaceCommandlineParameters(commandline);

    // Remove unused commandline parts
    commandline = commandline.replaceAll("#\\{.*\\}", "");

    String[] args = commandline.split(" ");
    for (String a : args)
      if (!"".equals(a.trim()))
        argumentList.add(a);
    return argumentList;
  }

  /**
   * Handles the encoder output by analyzing it first and then firing it off to
   * the registered listeners.
   * 
   * @param track
   *          the track that is currently being encoded
   * @param format
   *          the target media format
   * @param message
   *          the message returned by the encoder
   */
  @Override
  protected void handleEncoderOutput(EncodingProfile format, String message,
          File... sourceFiles) {
    super.handleEncoderOutput(format, message, sourceFiles);
    message = message.trim();
    if (message.equals(""))
      return;

    // Completely skip these messages
    if (message.startsWith("Press ["))
      return;

    // Others go to trace logging
    if (message.startsWith("FFmpeg version")
            || message.startsWith("configuration") || message.startsWith("lib")
            || message.startsWith("frame=") || message.startsWith("built on"))

      log_.trace(message);

    // Some to debug
    else if (message.startsWith("Input #")
            || message.startsWith("Duration:")
            || message.startsWith("Stream #")
            || message.startsWith("Stream mapping")
            || message.startsWith("Output #")
            || message.startsWith("video:")
            || message
                    .startsWith("PIX_FMT_YUV420P will be used as an intermediate format for rescaling"))

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

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.impl.AbstractEncoderEngine#getOutputFile(java.io.File,
   *      org.opencastproject.composer.api.EncodingProfile)
   */
  @Override
  protected File getOutputFile(File source, EncodingProfile profile) {
    File outputFile = null;
    try {
      List<String> arguments = buildArgumentList(profile);
      // TODO: Very unsafe! Improve!
      outputFile = new File(arguments.get(arguments.size() - 1));
    } catch (EncoderException e) {
      // Unlikely. We checked that before
    }
    return outputFile;
  }
}
