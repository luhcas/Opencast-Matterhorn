/**
 *  Copyright 2009 Opencast Project (http://www.opencastproject.org)
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

package org.opencastproject.media.analysis;

import org.opencastproject.util.ProcessExecutor;

import java.io.File;

/**
 * Support class for {@link org.opencastproject.media.analysis.MediaAnalyzer}
 * implementations that use an external program for analysis.
 * 
 * @author Christoph E. Driessen <ced@neopoly.de>
 */
public abstract class CmdlineMediaAnalyzerSupport implements MediaAnalyzer {

  protected String binary;
  protected MediaContainerMetadata metadata = new MediaContainerMetadata();

  protected CmdlineMediaAnalyzerSupport(String binary) {
    this.binary = binary;
  }

  public String getBinary() {
    return binary;
  }

  public void setBinary(String binary) {
    this.binary = binary;
  }

  public MediaContainerMetadata analyze(File media) {
    if (binary == null) {
      throw new IllegalStateException("Binary is not set");
    }
    // Version check (optional)
    String versionCheck = getVersionCheckOptions();
    if (versionCheck != null) {
      final boolean[] ok = new boolean[] { false };
      new ProcessExecutor(binary, versionCheck) {
        @Override
        protected boolean onLineRead(String line) {
          ok[0] |= onVersionCheck(line);
          return true;
        }

        @Override
        protected void onProcessFinished(int exitCode) {
          onFinished(exitCode);
        }

        @Override
        protected void onError(Exception e) {
          CmdlineMediaAnalyzerSupport.this.onError(e);
        }
      }.execute();
      if (!ok[0]) {
        throw new MediaAnalyzerException(this.getClass().getSimpleName()
            + ": Binary does not have the right version");
      }
    }
    // Analyze
    new ProcessExecutor(binary, getAnalysisOptions(media)) {
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
        CmdlineMediaAnalyzerSupport.this.onError(e);
      }
    }.execute();
    return metadata;
  }

  protected abstract String getAnalysisOptions(File media);

  protected abstract void onAnalysis(String line);

  protected String getVersionCheckOptions() {
    return null;
  }

  protected boolean onVersionCheck(String line) {
    return false;
  }

  protected void onFinished(int exitCode) {
    if (exitCode != 0 && exitCode != 255) {
      throw new MediaAnalyzerException("Cmdline tool " + binary
          + " exited with exit code " + exitCode);
    }
  }

  /**
   * Exception handler callback. Any occuring
   * {@link org.opencastproject.media.analysis.MediaAnalyzerException} will
   * <em>not</em> be passed to this handler.
   */
  protected void onError(Exception e) {
    throw new MediaAnalyzerException(e);
  }
}
