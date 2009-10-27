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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;

/**
 * A very simple (read: inadequate) implementation that stores all files under a root directory
 * using the media package ID as a subdirectory and the media package element ID as the
 * file name.
 */
public class WorkingFileRepositoryImpl implements WorkingFileRepository, ManagedService {
  private static final Logger logger = LoggerFactory.getLogger(WorkingFileRepositoryImpl.class);
  private String rootDirectory = null;

  public WorkingFileRepositoryImpl() {
    this(System.getProperty("java.io.tmpdir") + File.separator + "opencast" + File.separator + "workingfilerepo");
  }

  public WorkingFileRepositoryImpl(String rootDirectory) {
    this.rootDirectory = rootDirectory;
    createRootDirectory();
  }


  public void delete(String mediaPackageID, String mediaPackageElementID) {
    checkId(mediaPackageID);
    checkId(mediaPackageElementID);
    File f = getFile(mediaPackageID, mediaPackageElementID);
    logger.info("Attempting to delete file " + f.getAbsolutePath());
    if(f.canWrite()) {
      f.delete();
    } else {
      throw new SecurityException("Can not delete file in mediaPackage/mediaElement: " +
              mediaPackageID + "/" + mediaPackageElementID);
    }
    File d = getDirectory(mediaPackageID, mediaPackageElementID);
    logger.info("Attempting to delete directory " + d.getAbsolutePath());
    if(d.canWrite()){
      d.delete();
    }
    else{
      throw new SecurityException("Can not delete directory at mediaPackage/mediaElement " +
              mediaPackageID + "/" + mediaPackageElementID);
    }
  }

  public InputStream get(String mediaPackageID, String mediaPackageElementID) {
    checkId(mediaPackageID);
    checkId(mediaPackageElementID);
    File f = getFile(mediaPackageID, mediaPackageElementID);
    logger.info("Attempting to read file " + f.getAbsolutePath());
    try {
      return new FileInputStream(f);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public URL getURL(String mediaPackageID, String mediaPackageElementID) {
    // FIXME Either make this configurable, try to determine it from the system, or refer to another service
    // FIXME URL encode the IDs
    File f = getFile(mediaPackageID, mediaPackageElementID);
    try {
      if(f.getName().equals(mediaPackageElementID)) {
        return new URL("http://localhost:8080/files/" + mediaPackageID + "/" + mediaPackageElementID);
      } else {
        return new URL("http://localhost:8080/files/" + mediaPackageID + "/" + mediaPackageElementID + "/" + f.getName());
      }
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  public URL put(String mediaPackageID, String mediaPackageElementID, InputStream in){
    return put(mediaPackageID, mediaPackageElementID, mediaPackageElementID, in);
  }

  public URL put(String mediaPackageID, String mediaPackageElementID, String filename, InputStream in) {
    checkId(mediaPackageID);
    checkId(mediaPackageElementID);
    File f = new File(rootDirectory + File.separator + mediaPackageID + File.separator + 
            mediaPackageElementID + File.separator + filename);
    logger.info("Attempting to write a file to " + f.getAbsolutePath());
    try {
      if( ! f.exists()) {
        logger.info("Attempting to create a new file at " + f.getAbsolutePath());
        File mediaPackageElementDirectory = getDirectory(mediaPackageID, mediaPackageElementID);
        if( ! mediaPackageElementDirectory.exists()) {
          logger.info("Attempting to create a new directory at " + mediaPackageElementDirectory.getAbsolutePath());
          FileUtils.forceMkdir(mediaPackageElementDirectory);
        }
        else{
//          logger.error("Integrity error: directory " + mediaPackageID + "/" + mediaPackageElementID + 
//                  " already exists, but does not contain " + filename);
//          throw new RuntimeException("Element with ID: " + mediaPackageElementID + "already exists and does" + 
//                  "not contain filename");
        }
        f.createNewFile();
      } else {
        logger.info("Attempting to overwrite the file at " + f.getAbsolutePath());
      }
      IOUtils.copy(in, new FileOutputStream(f));
      return getURL(mediaPackageID, mediaPackageElementID);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void checkId(String id) {
    if(id == null) throw new NullPointerException("IDs can not be null");
    if(id.indexOf("..") > -1 || id.indexOf(File.separator) > -1) {
      throw new IllegalArgumentException("Invalid media package / element ID");
    }
  }

  private File getFile(String mediaPackageID, String mediaPackageElementID) {
    File directory = getDirectory(mediaPackageID, mediaPackageElementID);
    String[] files = directory.list();
    if(files.length != 1){
      logger.error("Integrity error: Element directory " + mediaPackageID + "/" + mediaPackageElementID + 
              "is empty or contains more than one element");
      throw new RuntimeException("Directory " + mediaPackageID + "/" + mediaPackageElementID +
              "does not contain exactly one element");
    }
    return new File(directory, files[0]);
  }

  private File getDirectory(String mediaPackageID, String mediaPackageElementID){
    return new File(rootDirectory + File.separator + mediaPackageID + File.separator + mediaPackageElementID);
  }

  @SuppressWarnings("unchecked")
  public void updated(Dictionary props) throws ConfigurationException {
    if(props.get("root") != null) {
      rootDirectory = (String)props.get("root");
      createRootDirectory();
    }
  }

  private void createRootDirectory() {
    File f = new File(rootDirectory);
    if( ! f.exists()) {
      try {
        FileUtils.forceMkdir(f);
      } catch(Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
