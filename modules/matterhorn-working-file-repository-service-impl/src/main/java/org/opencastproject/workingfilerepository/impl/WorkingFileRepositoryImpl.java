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
package org.opencastproject.workingfilerepository.impl;

import org.opencastproject.util.UrlSupport;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Dictionary;

/**
 * A very simple (read: inadequate) implementation that stores all files under a root directory
 * using the media package ID as a subdirectory and the media package element ID as the
 * file name.
 */
public class WorkingFileRepositoryImpl implements WorkingFileRepository, ManagedService {
  private static final Logger logger = LoggerFactory.getLogger(WorkingFileRepositoryImpl.class);
  private String rootDirectory = null;
  private String serverUrl = null;

  public WorkingFileRepositoryImpl() {
  }

  public WorkingFileRepositoryImpl(String rootDirectory, String serverUrl) {
    this.rootDirectory = rootDirectory;
    this.serverUrl = serverUrl;
  }
  
  public void activate(ComponentContext cc) {
    if(rootDirectory != null) return; // If the root directory was set by the constructor, respect that setting
    if(cc == null || cc.getBundleContext().getProperty("serverUrl") == null) {
      serverUrl = UrlSupport.DEFAULT_BASE_URL;
    } else {
      serverUrl = cc.getBundleContext().getProperty("serverUrl");
    }
    if(cc == null || cc.getBundleContext().getProperty("workingFileRepoPath") == null) {
      rootDirectory = System.getProperty("java.io.tmpdir") + File.separator + "opencast" + File.separator + "workingfilerepo";
    } else {
      rootDirectory = cc.getBundleContext().getProperty("workingFileRepoPath");
    }
    createRootDirectory();
    
    logger.info(getDiskSpace());
}


  public void delete(String mediaPackageID, String mediaPackageElementID) {
    checkId(mediaPackageID);
    checkId(mediaPackageElementID);
    File f = getFile(mediaPackageID, mediaPackageElementID);
    if (f == null) {
      logger.info("Unable to delete non existing object {}/{}", mediaPackageID, mediaPackageElementID);
      return;
    }
    logger.debug("Attempting to delete file {}", f.getAbsolutePath());
    if(f.canWrite()) {
      f.delete();
    } else {
      throw new SecurityException("Can not delete file in mediaPackage/mediaElement: " +
              mediaPackageID + "/" + mediaPackageElementID);
    }
    File d = getDirectory(mediaPackageID, mediaPackageElementID);
    logger.debug("Attempting to delete directory {}", d.getAbsolutePath());
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
    if (f == null) {
      logger.warn("Tried to read from non existing object {}/{}", mediaPackageID, mediaPackageElementID);
      return null;
    }
    logger.debug("Attempting to read file {}", f.getAbsolutePath());
    try {
      return new FileInputStream(f);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public URI getURI(String mediaPackageID, String mediaPackageElementID) {
    // FIXME Either make this configurable, try to determine it from the system, or refer to another service
    // FIXME URL encode the IDs
    File f = getFile(mediaPackageID, mediaPackageElementID);
    if (f == null) {
      logger.warn("Tried to look up uri for non existing object {}/{}", mediaPackageID, mediaPackageElementID);
      return null;
    }
    try {
      if(f.getName().equals(mediaPackageElementID)) {
        return new URI(serverUrl + "/files/" + mediaPackageID + "/" + mediaPackageElementID);
      } else {
        try {
          return new URI(serverUrl + "/files/" + mediaPackageID + "/" + mediaPackageElementID + "/" + URLEncoder.encode(f.getName(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
          throw new RuntimeException(e);
        }
      }
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public URI put(String mediaPackageID, String mediaPackageElementID, InputStream in){
    return put(mediaPackageID, mediaPackageElementID, mediaPackageElementID, in);
  }

  public URI put(String mediaPackageID, String mediaPackageElementID, String filename, InputStream in) {
    checkId(mediaPackageID);
    checkId(mediaPackageElementID);
    File f = null;
    try {
      f = new File(rootDirectory + File.separator + mediaPackageID + File.separator + 
              mediaPackageElementID + File.separator + URLEncoder.encode(filename, "UTF-8"));
    } catch (UnsupportedEncodingException e1) {
      throw new RuntimeException(e1);
    }
    logger.debug("Attempting to write a file to {}", f.getAbsolutePath());
    FileOutputStream out = null;
    try {
      if( ! f.exists()) {
        logger.debug("Attempting to create a new file at {}", f.getAbsolutePath());
        File mediaPackageElementDirectory = getDirectory(mediaPackageID, mediaPackageElementID);
        if( ! mediaPackageElementDirectory.exists()) {
          logger.debug("Attempting to create a new directory at {}", mediaPackageElementDirectory.getAbsolutePath());
          FileUtils.forceMkdir(mediaPackageElementDirectory);
        }
        f.createNewFile();
      } else {
        logger.debug("Attempting to overwrite the file at {}", f.getAbsolutePath());
      }
      out = new FileOutputStream(f);
      IOUtils.copy(in, out);
      return getURI(mediaPackageID, mediaPackageElementID);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      IOUtils.closeQuietly(out);
      IOUtils.closeQuietly(in);
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
    if (files == null) {
      logger.debug("Element directory {} does not exist", directory); 
      return null;
    } else if (files.length != 1) {
      logger.error("Integrity error: Element directory {} is empty or contains more than one element", 
              mediaPackageID + "/" + mediaPackageElementID);
      throw new RuntimeException("Directory " + mediaPackageID + "/" + mediaPackageElementID +
              "does not contain exactly one element");
    }
    return new File(directory, files[0]);
  }

  private File getFileFromCollection(String collectionId, String fileName) {
    File directory = getDirectory(collectionId);
    return new File(directory, fileName);
  }

  private File getDirectory(String mediaPackageID, String mediaPackageElementID){
    return new File(rootDirectory + File.separator + mediaPackageID + File.separator + mediaPackageElementID);
  }

  private File getDirectory(String collectionId){
    File collectionDir = new File(rootDirectory + File.separator + collectionId);
    if( ! collectionDir.exists()) {
      try {
        FileUtils.forceMkdir(collectionDir);
        logger.info("created collection directory " + collectionId);
      } catch (IOException e) {
        throw new IllegalStateException("can not create collection directory" + collectionDir);
      }
    }
    return collectionDir;
  }

  @SuppressWarnings("unchecked")
  public void updated(Dictionary props) throws ConfigurationException {
    logger.info("updating properties on {}", this);
    String newRootDirectory = (String)props.get("root");
    if(newRootDirectory != null) {
      logger.info("setting root directory to {}", newRootDirectory);
      rootDirectory = newRootDirectory;
      createRootDirectory();
    }
    String newServerUrl = (String)props.get("serverUrl");
    if(newServerUrl != null) {
      logger.info("setting serverUrl to {}", newServerUrl);
      serverUrl = newServerUrl;
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

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getCollectionSize(java.lang.String)
   */
  @Override
  public long getCollectionSize(String id) {
    File collectionDir = getDirectory(id);
    if( ! collectionDir.exists() || ! collectionDir.canRead()) throw new IllegalArgumentException("can not find collection " + id);
    File[] files = collectionDir.listFiles();
    if(files == null) throw new IllegalArgumentException("collection " + id + " is not a directory");
    return files.length;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getFromCollection(java.lang.String, java.lang.String)
   */
  @Override
  public InputStream getFromCollection(String collectionId, String fileName) {
    checkId(collectionId);
    File f = getFileFromCollection(collectionId, fileName);
    if (f == null || ! f.exists() || ! f.isFile()) {
      logger.warn("Tried to read from non existing object {}/{}", collectionId, fileName);
      return null;
    }
    logger.debug("Attempting to read file {}", f.getAbsolutePath());
    try {
      return new FileInputStream(f);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#putInCollection(java.lang.String, java.lang.String, java.io.InputStream)
   */
  @Override
  public URI putInCollection(String collectionId, String fileName, InputStream in) throws URISyntaxException {
    checkId(collectionId);
    checkId(fileName);
    File f = null;
    try {
      f = new File(rootDirectory + File.separator + collectionId + File.separator +  URLEncoder.encode(fileName, "UTF-8"));
    } catch (UnsupportedEncodingException e1) {
      throw new RuntimeException(e1);
    }
    logger.debug("Attempting to write a file to {}", f.getAbsolutePath());
    FileOutputStream out = null;
    try {
      if( ! f.exists()) {
        logger.debug("Attempting to create a new file at {}", f.getAbsolutePath());
        File collectionDirectory = getDirectory(collectionId);
        if( ! collectionDirectory.exists()) {
          logger.debug("Attempting to create a new directory at {}", collectionDirectory.getAbsolutePath());
          FileUtils.forceMkdir(collectionDirectory);
        }
        f.createNewFile();
      } else {
        logger.debug("Attempting to overwrite the file at {}", f.getAbsolutePath());
      }
      out = new FileOutputStream(f);
      IOUtils.copy(in, out);
      return new URI(serverUrl + "/files/" + collectionId + "/" + fileName);

    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      IOUtils.closeQuietly(out);
      IOUtils.closeQuietly(in);
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#copyTo(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public URI copyTo(String fromCollection, String fromFileName, String toMediaPackage, String toMediaPackageElement) {
    File source = getFileFromCollection(fromCollection, fromFileName);
    File dest = getFile(toMediaPackage, toMediaPackageElement);
    try {
      FileUtils.copyFile(source, dest);
    } catch (IOException e) {
      throw new IllegalStateException("unable to copy file" + e);
    }
    return getURI(toMediaPackage, toMediaPackageElement);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#moveTo(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public URI moveTo(String fromCollection, String fromFileName, String toMediaPackage, String toMediaPackageElement) {
    File source = getFileFromCollection(fromCollection, fromFileName);
    File dest = getFile(toMediaPackage, toMediaPackageElement);
    try {
      FileUtils.moveFile(source, dest);
    } catch (IOException e) {
      throw new IllegalStateException("unable to copy file" + e);
    }
    return getURI(toMediaPackage, toMediaPackageElement);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#removeFromCollection(java.lang.String, java.lang.String)
   */
  @Override
  public void removeFromCollection(String collectionId, String fileName) {
    File f = getFileFromCollection(collectionId, fileName);
    if(f.exists() && f.isFile() && f.canWrite()) {
      boolean success = f.delete();
      if(!success) throw new IllegalStateException("can not delete " + f);
    } else {
      throw new IllegalStateException("file " + f + " either does not exist, is not a file, or is not writable");
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getCollectionContents(java.lang.String)
   */
  @Override
  public URI[] getCollectionContents(String collectionId) {
    File collectionDir = getDirectory(collectionId);
    
    File[] files = collectionDir.listFiles();
    URI[] uris = new URI[files.length];
    for(int i=0; i<files.length; i++) {
      try {
        uris[i] = new URI(serverUrl + "/files/" + collectionId + "/" + files[i].getName());
      } catch (URISyntaxException e) {
        logger.warn(e.getMessage(), e);
      }
    }
    return uris;
  }
  
  public long getTotalSpace() {
    String temp = "getSpace";
    //if (rootDirectory == null) 
    File f = new File(rootDirectory);
    return f.getTotalSpace();
  }
  
  public long getUsableSpace() {
    String temp = "getSpace";
    //if (rootDirectory == null) 
    File f = new File(rootDirectory);
    return f.getUsableSpace();
  }
  
  public String getDiskSpace() {
    int usable = Math.round(getUsableSpace() / 1024 / 1024 / 1024);
    int total = Math.round(getTotalSpace() / 1024 / 1024 / 1024);
    long percent = Math.round(100.0 * getUsableSpace()/(1+getTotalSpace()));
    return "Usable space "+usable+" Gb out of "+total+" Gb ("+percent+"%)";
  }
}
