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

import org.opencastproject.workingfilerepository.api.WorkingFileRepository;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.felix.fileinstall.ArtifactInstaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Places a file named "inbox*" from any fileinstall watch directory into the inbox collection.  Fileinstall takes care
 * of installing artifacts only once they are fully copied into the watch directory.
 */
public class InboxScanner implements ArtifactInstaller {
  private static final Logger logger = LoggerFactory.getLogger(InboxScanner.class);

  protected WorkingFileRepository fileRepository;
  
  public void setFileRepository(WorkingFileRepository fileRepository) {
    this.fileRepository = fileRepository;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.felix.fileinstall.ArtifactInstaller#install(java.io.File)
   */
  public void install(File artifact) throws Exception {
    FileInputStream in = null;
    try {
      in = new FileInputStream(artifact);
      fileRepository.putInCollection("inbox", artifact.getName().substring(5), in); // strip the word "inbox"
    } catch(IOException e) {
      logger.warn("Unable to store file {} in the inbox, {}", artifact.getAbsolutePath(), e);
      return;
    } finally {
      IOUtils.closeQuietly(in);
    }
    try {
      FileUtils.forceDelete(artifact);
    } catch(IOException e) {
      logger.warn("Unable to delete file {}, {}", artifact.getAbsolutePath(), e);
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
    return artifact.getName().startsWith("inbox");
  }
}
