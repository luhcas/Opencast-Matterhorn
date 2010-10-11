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

package org.opencastproject.analysis.text.ocropus;

import org.opencastproject.analysis.api.MediaAnalysisException;
import org.opencastproject.util.ProcessExecutor;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Commandline wrapper around ocropus' <code>ocrocmd</code> command.
 */
public class OcropusTextAnalyzer {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(OcropusTextAnalyzer.class);

  /** Default name of the ocrocmd binary */
  public static final String OCROPUS_BINARY_DEFAULT = "ocrocmd";

  /** Binary of the ocropus command */
  protected String binary = OCROPUS_BINARY_DEFAULT;

  /** The text frame containing the result of the analysis operation */
  protected OcropusTextFrame textFrame = null;

  /** Output of the commandline process */
  protected StringBuffer ocrocmdOutput = null;

  /**
   * Creates a new ocropus command wrapper that will be using the given binary.
   * 
   * @param binary
   *          the ocropus binary
   */
  public OcropusTextAnalyzer(String binary) {
    this.binary = binary;
  }

  /**
   * Returns the path to the <code>ocrocmd</code> binary.
   * 
   * @return path to the binary
   */
  public String getBinary() {
    return binary;
  }

  /**
   * Sets the path to the <code>ocrocmd</code> binary.
   * 
   * @param binary
   */
  public void setBinary(String binary) {
    this.binary = binary;
  }

  /**
   * Analyzes the image and returns any text found int it.
   * 
   * @param image
   * @return the text found in this image
   * @throws IllegalStateException
   *           if the binary is not set
   */
  public OcropusTextFrame analyze(File image) {
    if (binary == null)
      throw new IllegalStateException("Binary is not set");
    
    ocrocmdOutput = new StringBuffer();

    // Analyze
    new ProcessExecutor(binary, getAnalysisOptions(image)) {
      @Override
      protected boolean onLineRead(String line) {
        onAnalysis(line);
        return true;
      }

      @Override
      protected void onProcessFinished(int exitCode) {
        onFinished(exitCode);
      }

      @Override
      protected void onError(Exception e) {
        OcropusTextAnalyzer.this.onError(e);
      }
    }.execute();

    postProcess();
    return textFrame;
  }

  /**
   * Override this method to do any post processing on the gathered metadata. The default implementation does nothing.
   */
  protected void postProcess() {
    try {
      InputStream is = IOUtils.toInputStream(ocrocmdOutput.toString(), "UTF-8");
      textFrame = OcropusTextFrame.parse(is);
    } catch (IOException e) {
      onError(e);
    }
  }

  /**
   * The only parameter to <code>ocrocmd</code> is the filename, so this is what this method returns.
   * 
   * @param image
   *          the image file
   * @return the options to run analysis on the image
   */
  protected String getAnalysisOptions(File image) {
    return image.getAbsolutePath();
  }

  /**
   * Callback for the analysis itself. The parameter contains line after line of output, which will be collected so that
   * it's accessible in whole at a later time.
   * 
   * @param line
   *          a line of output
   */
  protected void onAnalysis(String line) {
    logger.trace(line);
    ocrocmdOutput.append(line).append('\n');
  }

  /**
   * Callback after successful execution of the commandline tool.
   * 
   * @param exitCode
   *          the exit code
   */
  protected void onFinished(int exitCode) {
    // Windows binary will return -1 when queried for options
    if (exitCode != -1 && exitCode != 0 && exitCode != 255) {
      logger.error(ocrocmdOutput.toString());
      throw new MediaAnalysisException("Text analyzer " + binary + " exited with code " + exitCode);
    }
  }

  /**
   * Exception handler callback. Any occuring {@link org.opencastproject.inspection.impl.api.MediaAnalyzerException}
   * will <em>not</em> be passed to this handler.
   * 
   * @param e
   *          the exception
   */
  protected void onError(Exception e) {
    throw new MediaAnalysisException(e);
  }

}
