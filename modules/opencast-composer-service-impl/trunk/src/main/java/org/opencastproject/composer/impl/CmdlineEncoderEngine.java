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

package org.opencastproject.composer.impl;

import org.opencastproject.composer.api.EncoderEngine;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncoderListener;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.util.IoSupport;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wrapper around any kind of command line controllable encoder.
 * <p/>
 * <strong>Note:</strong> Registered {@link ch.ethz.replay.core.api.composer.EncoderListener}s <em>won't</em> receive a file in
 * method
 * {@link ch.ethz.replay.core.api.composer.EncoderListener#trackEncoded(EncoderEngine, Track, EncodingProfile.ethz.replay.core.api.common.media.EncodingFormat , String)}
 * because it cannot be guaranteed that only <em>one</em> file will be the result of the encoding. Imagine encoding to
 * an image series.
 * 
 * @author Christoph E. Drießen <ced@neopoly.de>
 * @version $Id$ todo
 */
public class CmdlineEncoderEngine extends AbstractEncoderEngine implements EncoderEngine {

  /**
   * If true STDERR and STDOUT of the spawned process will be mixed so that both can be read via STDIN
   */
  private static final boolean REDIRECT_ERROR_STREAM = true;

  /** the encoder binary */
  private String binary;

  /** the command line options */
  private String cmdlineOptions = "";

  /** Base output directory */
  private File outRootDir = null;

  /** parameters substituted in the command line options string */
  private Map<String, Object> params = new HashMap<String, Object>();

  /** the logging facility provided by log4j */
  private final static Logger log_ = LoggerFactory.getLogger(CmdlineEncoderEngine.class.getName());

  /**
   * Creates a new CmdlineEncoderEngine with <code>binary</code> as the workhorse.
   */
  public CmdlineEncoderEngine(String binary) {
    super(false);

    if (binary == null)
      throw new IllegalArgumentException("binary is null");

    this.binary = binary;
  }

  /**
   * Sets the root output directory.
   * 
   * @param dir
   *          the directory
   */
  protected void setOutputDirectory(File dir) {
    outRootDir = dir;
  }

  /**
   * @see ch.ethz.replay.core.api.composer.EncoderEngine#needsLocalWorkCopy()
   */
  public boolean needsLocalWorkCopy() {
    return false;
  }

  /**
   * Encodes a track into the specified format. The method returns when the encoder process finishes.
   * 
   * @param track
   *          the track to encode
   * @param format
   *          the media format definition
   * @param profile
   *          the profile name
   * 
   * @throws EncoderException
   *           if an error occurs during encoding
   */
  @Override
  public void encodeTrack(Track track, EncodingProfile format) throws EncoderException {
    // build command
    BufferedReader in = null;
    Process encoderProcess = null;
    try {

      // Set in and out files/directories
      setInputFile(download(track).getAbsolutePath());
      if (outRootDir != null) {
        File outDir = new File(outRootDir, format.getIdentifier());
        if (!outDir.exists()) {
          log_.trace("Created output directory " + outDir);
          outDir.mkdirs();
        }
        setOutputDirectory(outDir.getAbsolutePath());
      }

      // Define the suffix as a template
      params.put("out.suffix", format.getSuffix());

      // create encoder process.
      // no special working dir is set which means the working dir of the
      // current java process is used.
      // TODO: Parallelisation (threading)
      List<String> command = buildCommand(track, format);
      ProcessBuilder pbuilder = new ProcessBuilder(command);
      pbuilder.redirectErrorStream(REDIRECT_ERROR_STREAM);
      encoderProcess = pbuilder.start();

      // tell encoder listeners about output
      in = new BufferedReader(new InputStreamReader(encoderProcess.getInputStream()));
      String line;
      while ((line = in.readLine()) != null) {
        handleEncoderOutput(track, format, line);
      }

      // wait until the task is finished
      encoderProcess.waitFor();
      int exitCode = encoderProcess.exitValue();
      if (exitCode != 0) {
        throw new EncoderException(this, "Encoder exited abnormally with status " + exitCode);
      }

      log_.info("Track " + track + " successfully encoded to " + format.getName());
      fireTrackEncoded(this, track, format);
    } catch (EncoderException e) {
      log_.warn("Error while encoding track " + track + " to " + format.getName() + ": " + e.getMessage());
      fireTrackEncodingFailed(this, track, format, e);
      throw e;
    } catch (Exception e) {
      String msg = "Error while encoding track " + track + " to " + format.getName() + ": " + e.getMessage();
      log_.warn(msg);
      fireTrackEncodingFailed(this, track, format, e);
      throw new EncoderException(this, msg, e);
    } finally {
      IoSupport.closeQuietly(in);
      IoSupport.closeQuietly(encoderProcess);
    }
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
  protected void handleEncoderOutput(Track track, EncodingProfile format, String message) {
    message = message.trim();
    fireEncoderMessage(track, format, message);
  }

  /**
   * Specifies the encoder binary.
   * 
   * @param binary
   *          path to the binary
   */
  protected void setBinary(String binary) {
    if (binary == null)
      throw new IllegalArgumentException("binary is null");
    this.binary = binary;
  }

  /**
   * Creates the command that is sent to the commandline encoder.
   * 
   * @return the commandline
   * @throws ch.ethz.replay.core.api.composer.EncoderException
   *           in case of any error
   */
  protected List<String> buildCommand(Track track, EncodingProfile profile) throws EncoderException {
    List<String> command = new ArrayList<String>();
    command.add(binary);
    List<String> arguments = buildArgumentList(track, profile);
    for (String arg : arguments) {
      for (Map.Entry<String, Object> e : params.entrySet()) {
        arg = arg.replace("#{" + e.getKey() + "}", e.getValue().toString());
      }
      command.add(arg);
    }
    return command;
  }

  /**
   * Creates the arguments for the commandline.
   * 
   * @param track
   *          the track that is to be encoded
   * @param profile
   *          the encoding profile
   * @return the argument list
   * @throws ch.ethz.replay.core.api.composer.EncoderException
   *           in case of any error
   */
  protected List<String> buildArgumentList(Track track, EncodingProfile format) throws EncoderException {
    String optionString = processCommandTemplates(cmdlineOptions);
    String[] options = optionString.split(" ");
    List<String> arguments = new ArrayList<String>(options.length);
    arguments.addAll(Arrays.asList(options));
    return arguments;
  }

  /**
   * Processes the command options by replacing the templates with their actual values.
   * 
   * @return the commandline
   */
  protected String processCommandTemplates(String cmd) {
    String r = cmd;
    for (Map.Entry<String, Object> e : params.entrySet()) {
      r = r.replace("#{" + e.getKey() + "}", e.getValue().toString());
    }
    return r;
  }

  // -- Attributes

  /**
   * Set the commandline options in a single string. Parameters in the form of <code>#{param}</code> will be
   * substituted.
   * 
   * @see #setInputFile(String)
   * @see #setOutputDirectory(String)
   * @see #addParam(String, String)
   */
  public void setCmdlineOptions(String cmdlineOptions) {
    this.cmdlineOptions = cmdlineOptions;
  }

  /**
   * Set the file to encode. If set the following commandline parameters are available.
   * <ul>
   * <li>#{in.path} the absolute path of the input file as set in {@link #setInputFile(String)}, e.g. "/movies/track.dv"
   * <li>#{in.name} the name of the track, e.g. "track"
   * <li>#{in.extension} the extension, e.g. "dv"
   * <li>#{in.filename} the complete filename consisting of #{in.name}.#{in.extension}, e.g. "track.dv"
   * </ul>
   */
  protected void setInputFile(String file) {
    String inf = FilenameUtils.normalize(file);
    params.put("in.path", inf);
    params.put("in.name", FilenameUtils.getBaseName(inf));
    params.put("in.extension", FilenameUtils.getExtension(inf));
    params.put("in.filename", FilenameUtils.getName(inf));
  }

  /**
   * Set the optional output directory. If a directory is specified here registered listeners will receive it via
   * {@link ch.ethz.replay.core.api.composer.EncoderListener#trackEncoded(EncoderEngine, Track, EncodingProfile.ethz.replay.core.api.common.media.EncodingFormat , String)}
   * and the #{out.dir} commandline parameter will be substituted.
   */
  public void setOutputDirectory(String dir) {
    params.put("out.dir", FilenameUtils.normalize(dir));
  }

  /**
   * Adds a command line parameter that will be substituted along with the default parameters.
   * 
   * @see #setCmdlineOptions(String)
   */
  public void addParam(String name, String value) {
    params.put(name, value);
  }

  /**
   * Tells the registered listeners that the given track has been encoded into <code>file</code>, using the encoding
   * format <code>format</code>.
   * 
   * @param track
   *          the original track
   * @param format
   *          the used format
   * @param message
   *          the message
   */
  protected void fireEncoderMessage(Track track, EncodingProfile format, String message) {
    for (EncoderListener l : this.listeners) {
      if (l instanceof CmdlineEncoderListener) {
        try {
          ((CmdlineEncoderListener) l).notifyEncoderOutput(track, format, message);
        } catch (Throwable th) {
          log_.error("EncoderListener " + l + " threw exception while processing callback", th);
        }
      }
    }
  }

}