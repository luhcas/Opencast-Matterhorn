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
package org.opencastproject.stream.red5;

import org.red5.server.api.IScope;
import org.red5.server.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Overrides Red5's {@link org.red5.server.persistence.FilePersistence} class to set the streaming directory properly.
 */
public class FilePersistence extends org.red5.server.persistence.FilePersistence {
  private static final Logger logger = LoggerFactory.getLogger(FilePersistence.class);
  protected String rootDir = System.getProperty("java.io.tmpdir") + File.separator + "opencast" + 
    File.separator + "stream";

  public FilePersistence(IScope scope) {
    super(scope);
    if(new File(rootDir).exists()) {
      logger.info("Using root streaming directory " + rootDir);
    } else {
      logger.info("Creating root streaming directory " + rootDir);
      try {
        FileUtil.makeDirectory(rootDir, true);
      } catch (IOException e) {
        logger.warn("Unable to create root streaming directory " + rootDir);
      }
    }
  }

  @Override
  public void setPath(String path) {
    logger.info("Not setting path, keeping " + rootDir);
  }
}
