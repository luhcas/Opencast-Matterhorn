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
package org.opencastproject.ingest.scanner;

import org.opencastproject.ingest.api.IngestService;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.felix.fileinstall.ArtifactInstaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Ingests mediapackages named "*.zip" from any fileinstall watch directory.  Fileinstall takes care of installing
 * artifacts only once they are fully copied into the watch directory.
 */
public class MediapackageScanner implements ArtifactInstaller {
  private static final Logger logger = LoggerFactory.getLogger(MediapackageScanner.class);

  protected IngestService ingestService;
  
  public void setIngestService(IngestService ingestService) {
    this.ingestService = ingestService;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.felix.fileinstall.ArtifactInstaller#install(java.io.File)
   */
  public void install(File artifact) throws Exception {
    FileInputStream in = null;
    try {
      in = new FileInputStream(artifact);
      ingestService.addZippedMediaPackage(in);
    } catch(IOException e) {
      logger.warn("Unable to ingest mediapackage {}, {}", artifact.getAbsolutePath(), e);
      return;
    } finally {
      IOUtils.closeQuietly(in);
    }
    try {
      FileUtils.forceDelete(artifact);
    } catch(IOException e) {
      logger.warn("Unable to delete mediapackage {}, {}", artifact.getAbsolutePath(), e);
    }
  }

  /**
   * {@inheritDoc}
   * @see org.apache.felix.fileinstall.ArtifactInstaller#uninstall(java.io.File)
   */
  public void uninstall(File artifact) throws Exception {
    // nothing to do
  }

  /**
   * {@inheritDoc}
   * @see org.apache.felix.fileinstall.ArtifactInstaller#update(java.io.File)
   */
  public void update(File artifact) throws Exception {
    // nothing to do
  }

  /**
   * {@inheritDoc}
   * @see org.apache.felix.fileinstall.ArtifactListener#canHandle(java.io.File)
   */
  public boolean canHandle(File artifact) {
    return artifact.getParentFile().getName().equals("mediapackages") && artifact.getName().endsWith(".zip");
  }
}
