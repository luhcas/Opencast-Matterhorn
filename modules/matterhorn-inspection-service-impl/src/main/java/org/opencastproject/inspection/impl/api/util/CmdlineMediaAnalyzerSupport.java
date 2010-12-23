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

package org.opencastproject.inspection.impl.api.util;

import org.opencastproject.inspection.impl.api.MediaAnalyzer;
import org.opencastproject.inspection.impl.api.MediaAnalyzerException;
import org.opencastproject.inspection.impl.api.MediaContainerMetadata;
import org.opencastproject.util.ProcessExcecutorException;
import org.opencastproject.util.ProcessExecutor;

import java.io.File;

/**
 * Support class for {@link org.opencastproject.inspection.impl.api.MediaAnalyzer} implementations that use an external
 * program for analysis.
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

  public MediaContainerMetadata analyze(File media) throws MediaAnalyzerException {
    if (binary == null) {
      throw new IllegalStateException("Binary is not set");
    }

    ProcessExecutor<MediaAnalyzerException> mediaAnalyzer = null;

    // Version check (optional)
    String versionCheck = getVersionCheckOptions();
    if (versionCheck != null) {

      // This is an array only because it needs to be final and hold a modifiable boolean
      final boolean[] ok = new boolean[] { false };
      mediaAnalyzer = new ProcessExecutor<MediaAnalyzerException>(binary, versionCheck) {
        @Override
        protected boolean onLineRead(String line) {
          ok[0] |= onVersionCheck(line);
          return true;
        }

        @Override
        protected void onProcessFinished(int exitCode) throws MediaAnalyzerException {
          onFinished(exitCode);
        }
      };

      try {
        mediaAnalyzer.execute();
      } catch (ProcessExcecutorException e) {
        throw new MediaAnalyzerException("Excecuting the version check on " + binary + " failed", e);
      }

      if (!ok[0]) {
        throw new MediaAnalyzerException(this.getClass().getSimpleName() + ": Binary does not have the right version");
      }
    }

    // Analyze
    mediaAnalyzer = new ProcessExecutor<MediaAnalyzerException>(binary, getAnalysisOptions(media)) {
      @Override
      protected boolean onLineRead(String line) {
        onAnalysis(line);
        return true;
      }

      @Override
      protected void onProcessFinished(int exitCode) throws MediaAnalyzerException {
        onFinished(exitCode);
      }
    };

    try {
      mediaAnalyzer.execute();
    } catch (ProcessExcecutorException e) {
      throw new MediaAnalyzerException("Error while running media analyzer " + binary, e);
    }

    postProcess();
    return metadata;
  }

  /**
   * Override this method to do any post processing on the gathered metadata. The default implementation does nothing.
   */
  protected void postProcess() {
  }

  protected abstract String getAnalysisOptions(File media);

  protected abstract void onAnalysis(String line);

  protected String getVersionCheckOptions() {
    return null;
  }

  protected boolean onVersionCheck(String line) {
    return false;
  }

  protected void onFinished(int exitCode) throws MediaAnalyzerException {
    // Windows binary will return -1 when queried for options
    if (exitCode != -1 && exitCode != 0 && exitCode != 255) {
      throw new MediaAnalyzerException("Cmdline tool " + binary + " exited with exit code " + exitCode);
    }
  }

}
