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
 * <strong>Note:</strong> Registered {@link EncoderListener}s <em>won't</em> receive a file in method
 * {@link EncoderListener#fileEncoded(EncoderEngine, File, EncodingProfile)} because it cannot be guaranteed that only
 * <em>one</em> file will be the result of the encoding. Imagine encoding to an image series.
 */
public abstract class AbstractCmdlineEncoderEngine extends AbstractEncoderEngine implements EncoderEngine {

  /**
   * If true STDERR and STDOUT of the spawned process will be mixed so that both can be read via STDIN
   */
  private static final boolean REDIRECT_ERROR_STREAM = true;

  /** the encoder binary */
  private String binary = null;

  /** the command line options */
  private String cmdlineOptions = "";

  /** parameters substituted in the command line options string */
  private Map<String, Object> params = new HashMap<String, Object>();

  /** the logging facility provided by log4j */
  private final static Logger log_ = LoggerFactory.getLogger(AbstractCmdlineEncoderEngine.class.getName());

  /**
   * Creates a new CmdlineEncoderEngine with <code>binary</code> as the workhorse.
   */
  public AbstractCmdlineEncoderEngine(String binary) {
    super(false);

    if (binary == null)
      throw new IllegalArgumentException("binary is null");

    this.binary = binary;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.composer.api.EncoderEngine#needsLocalWorkCopy()
   */
  public boolean needsLocalWorkCopy() {
    return false;
  }

  /**
   * Encodes a track into the specified format. The method returns when the encoder process finishes.
   * 
   * @param source
   *          the track to encode
   * @param profile
   *          the media format definition
   * @param profile
   *          the profile name
   * 
   * @throws EncoderException
   *           if an error occurs during encoding
   */
  @Override
  public File encode(File source, EncodingProfile profile) throws EncoderException {
    // build command
    BufferedReader in = null;
    Process encoderProcess = null;
    try {
      // Set encoding parameters
      String inf = FilenameUtils.normalize(source.getAbsolutePath());
      params.put("in.path", inf);
      params.put("in.name", FilenameUtils.getBaseName(inf));
      params.put("in.extension", FilenameUtils.getExtension(inf));
      params.put("in.filename", FilenameUtils.getName(inf));
      params.put("out.dir", source.getParent());
      params.put("out.suffix", profile.getSuffix());

      // create encoder process.
      // no special working dir is set which means the working dir of the
      // current java process is used.
      // TODO: Parallelisation (threading)
      List<String> command = buildCommand(source, profile);
      StringBuilder sb = new StringBuilder();
      for(String cmd : command) {
        sb.append(cmd);
        sb.append(" ");
      }
      log_.info("Executing encoding command: " + sb.toString());
      ProcessBuilder pbuilder = new ProcessBuilder(command);
      pbuilder.redirectErrorStream(REDIRECT_ERROR_STREAM);
      encoderProcess = pbuilder.start();

      // tell encoder listeners about output
      in = new BufferedReader(new InputStreamReader(encoderProcess.getInputStream()));
      String line;
      while ((line = in.readLine()) != null) {
        handleEncoderOutput(source, profile, line);
      }

      // wait until the task is finished
      encoderProcess.waitFor();
      int exitCode = encoderProcess.exitValue();
      if (exitCode != 0) {
        throw new EncoderException(this, "Encoder exited abnormally with status " + exitCode);
      }

      log_.info("Track " + source + " successfully encoded to " + profile.getName());
      fireEncoded(this, source, profile);
      return new File(source.getParent(), FilenameUtils.getBaseName(inf) + profile.getSuffix());
    } catch (EncoderException e) {
      log_.warn("Error while encoding track " + source + " to " + profile.getName() + ": " + e.getMessage());
      fireEncodingFailed(this, source, profile, e);
      throw e;
    } catch (Exception e) {
      String msg = "Error while encoding track " + source + " to " + profile.getName() + ": " + e.getMessage();
      log_.warn(msg);
      fireEncodingFailed(this, source, profile, e);
      throw new EncoderException(this, msg, e);
    } finally {
      IoSupport.closeQuietly(in);
      IoSupport.closeQuietly(encoderProcess);
    }
  }

  /**
   * Handles the encoder output by analyzing it first and then firing it off to the registered listeners.
   * 
   * @param file
   *          the file that is currently being encoded
   * @param format
   *          the target media format
   * @param message
   *          the message returned by the encoder
   */
  protected void handleEncoderOutput(File file, EncodingProfile format, String message) {
    message = message.trim();
    fireEncoderMessage(file, format, message);
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
   * @throws EncoderException
   *           in case of any error
   */
  protected List<String> buildCommand(File file, EncodingProfile profile) throws EncoderException {
    List<String> command = new ArrayList<String>();
    command.add(binary);
    List<String> arguments = buildArgumentList(file, profile);
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
   * @param file
   *          the file that is to be encoded
   * @param profile
   *          the encoding profile
   * @return the argument list
   * @throws EncoderException
   *           in case of any error
   */
  protected List<String> buildArgumentList(File file, EncodingProfile format) throws EncoderException {
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
   * @param sourceFile
   *          the original track
   * @param format
   *          the used format
   * @param message
   *          the message
   */
  protected void fireEncoderMessage(File sourceFile, EncodingProfile format, String message) {
    for (EncoderListener l : this.listeners) {
      if (l instanceof CmdlineEncoderListener) {
        try {
          ((CmdlineEncoderListener) l).notifyEncoderOutput(sourceFile, format, message);
        } catch (Throwable th) {
          log_.error("EncoderListener " + l + " threw exception while processing callback", th);
        }
      }
    }
  }

}