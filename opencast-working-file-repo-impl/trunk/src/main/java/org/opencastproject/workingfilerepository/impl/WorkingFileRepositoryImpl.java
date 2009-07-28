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
package org.opencastproject.workingfilerepository.impl;

import org.opencastproject.workingfilerepository.api.WorkingFileRepository;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;

/**
 * A very simple (read: inadequate) implementation that stores all files under a root directory
 * using the media package ID as a subdirectory and the media package element ID as the
 * file name.
 *
 */
public class WorkingFileRepositoryImpl implements WorkingFileRepository, ManagedService {
  private static final Logger logger = LoggerFactory.getLogger(WorkingFileRepositoryImpl.class);
  private String rootDirectory = "/tmp/matterhorn/workingfilerepo";
  
  public void delete(String mediaPackageID, String mediaPackageElementID) {
    File f = getFile(mediaPackageID, mediaPackageElementID);
    logger.info("Attempting to delete file " + f.getAbsolutePath());
    if(f.canWrite()) {
      f.delete();
    } else {
      throw new SecurityException("Can not delete file at mediaPackage/mediaElement: " +
          mediaPackageID + "/" + mediaPackageElementID);
    }
  }

  public InputStream get(String mediaPackageID, String mediaPackageElementID) {
    File f = getFile(mediaPackageID, mediaPackageElementID);
    logger.info("Attempting to read file " + f.getAbsolutePath());
    try {
      return new FileInputStream(f);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public void put(String mediaPackageID, String mediaPackageElementID, InputStream in) {
    File f = getFile(mediaPackageID, mediaPackageElementID);
    logger.info("Attempting to write a file to " + f.getAbsolutePath());
    try {
      if( ! f.exists()) {
        logger.info("Attempting to create a new file at " + f.getAbsolutePath());
        File mediaPackageDirectory = new File(rootDirectory + File.separator + mediaPackageID);
        if( ! mediaPackageDirectory.exists()) {
          logger.info("Attempting to create a new directory at " + mediaPackageDirectory.getAbsolutePath());
          FileUtils.forceMkdir(mediaPackageDirectory);
        }
        f.createNewFile();
      } else {
        logger.info("Attempting to overwrite the file at " + f.getAbsolutePath());
      }
      IOUtils.copy(in, new FileOutputStream(f));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private File getFile(String mediaPackageID, String mediaPackageElementID) {
    return new File(rootDirectory + File.separator + mediaPackageID + File.separator + mediaPackageElementID);
  }

  public void updated(Dictionary props) throws ConfigurationException {
    if(props.get("root") != null) {
      rootDirectory = (String)props.get("root");
    }
  }

}
