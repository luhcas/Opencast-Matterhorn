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

import org.apache.commons.codec.digest.DigestUtils;
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
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Dictionary;

/**
 * A very simple (read: inadequate) implementation that stores all files under a root directory using the media package
 * ID as a subdirectory and the media package element ID as the file name.
 */
public class WorkingFileRepositoryImpl implements WorkingFileRepository, ManagedService {
  private static final Logger logger = LoggerFactory.getLogger(WorkingFileRepositoryImpl.class);

  /** The character encoding used for URLs */
  private static final String CHAR_ENCODING = "UTF-8";

  /** The filename filter matching .md5 files */
  private static final FilenameFilter MD5_FINAME_FILTER = new FilenameFilter() {
    public boolean accept(File dir, String name) {
      return name.endsWith(".md5");
    }
  };

  /** The filename filter matching .md5 files */
  private static final FilenameFilter NOT_MD5_FINAME_FILTER = new FilenameFilter() {
    public boolean accept(File dir, String name) {
      return ! name.endsWith(".md5");
    }
  };

  /* The root directory for storing files */
  private String rootDirectory = null;

  /** The Base URL for this server */
  private String serverUrl = null;

  /** No arg constructor */
  public WorkingFileRepositoryImpl() {
  }

  public WorkingFileRepositoryImpl(String rootDirectory, String serverUrl) {
    this.rootDirectory = rootDirectory;
    this.serverUrl = serverUrl;
  }

  public void activate(ComponentContext cc) {
    if (rootDirectory != null)
      return; // If the root directory was set by the constructor, respect that setting
    if (cc == null || cc.getBundleContext().getProperty("org.opencastproject.server.url") == null) {
      serverUrl = UrlSupport.DEFAULT_BASE_URL;
    } else {
      serverUrl = cc.getBundleContext().getProperty("org.opencastproject.server.url");
    }
    if (cc == null || cc.getBundleContext().getProperty("org.opencastproject.file.repo.path") == null) {
      rootDirectory = System.getProperty("java.io.tmpdir") + File.separator + "opencast" + File.separator
              + "workingfilerepo";
    } else {
      rootDirectory = cc.getBundleContext().getProperty("org.opencastproject.file.repo.path");
    }
    createRootDirectory();

    logger.info(getDiskSpace());
  }

  public void delete(String mediaPackageID, String mediaPackageElementID) {
    checkPathSafe(mediaPackageID);
    checkPathSafe(mediaPackageElementID);
    File f = getFile(mediaPackageID, mediaPackageElementID);
    if (f == null) {
      logger.info("Unable to delete non existing object {}/{}", mediaPackageID, mediaPackageElementID);
      return;
    }
    logger.debug("Attempting to delete file {}", f.getAbsolutePath());
    if (f.canWrite()) {
      f.delete();
    } else {
      throw new SecurityException("Can not delete file in mediaPackage/mediaElement: " + mediaPackageID + "/"
              + mediaPackageElementID);
    }
    File d = getElementDirectory(mediaPackageID, mediaPackageElementID);
    logger.debug("Attempting to delete directory {}", d.getAbsolutePath());
    if (d.canWrite()) {
      d.delete();
    } else {
      throw new SecurityException("Can not delete directory at mediaPackage/mediaElement " + mediaPackageID + "/"
              + mediaPackageElementID);
    }
  }

  public InputStream get(String mediaPackageID, String mediaPackageElementID) {
    checkPathSafe(mediaPackageID);
    checkPathSafe(mediaPackageElementID);
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
      if (f.getName().equals(mediaPackageElementID)) {
        return new URI(serverUrl + "/files/mp/" + mediaPackageID + "/" + mediaPackageElementID);
      } else {
        try {
          return new URI(serverUrl + "/files/mp/" + mediaPackageID + "/" + mediaPackageElementID + "/"
                  + URLEncoder.encode(f.getName(), CHAR_ENCODING));
        } catch (UnsupportedEncodingException e) {
          throw new IllegalStateException("Can not encode to " + CHAR_ENCODING);
        }
      }
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public URI put(String mediaPackageID, String mediaPackageElementID, InputStream in) {
    return put(mediaPackageID, mediaPackageElementID, mediaPackageElementID, in);
  }

  public URI put(String mediaPackageID, String mediaPackageElementID, String filename, InputStream in) {
    checkPathSafe(mediaPackageID);
    checkPathSafe(mediaPackageElementID);
    File f = null;
    File dir = getElementDirectory(mediaPackageID, mediaPackageElementID);
    if(dir.exists()) {
      // clear the directory
      File[] filesToDelete = dir.listFiles();
      if(filesToDelete != null && filesToDelete.length > 0) {
        for(File fileToDelete : filesToDelete) {
          if( ! fileToDelete.delete()) {
            throw new IllegalStateException("Unable to delete file: " + fileToDelete.getAbsolutePath());
          }
        }
      }
    } else {
      try {
        logger.debug("Attempting to create a new directory at {}", dir.getAbsolutePath());
        FileUtils.forceMkdir(dir);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    try {
      f = new File(dir, URLEncoder.encode(filename, CHAR_ENCODING));
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("Can not encode to " + CHAR_ENCODING);
    }
    logger.debug("Attempting to write a file to {}", f.getAbsolutePath());
    FileOutputStream out = null;
    try {
      if (!f.exists()) {
        f.createNewFile();
      } else {
        logger.debug("Attempting to overwrite the file at {}", f.getAbsolutePath());
      }
      out = new FileOutputStream(f);
      IOUtils.copy(in, out);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      IOUtils.closeQuietly(out);
      IOUtils.closeQuietly(in);
    }
    addMd5(f);
    return getURI(mediaPackageID, mediaPackageElementID);
  }

  /**
   * Creates a file containing the md5 hash for the contents of a source file
   * @param f The source file containing the data to hash
   */
  protected File addMd5(File f) {
    // Create an md5 file
    FileInputStream md5In = null;
    try {
      md5In = new FileInputStream(f);
      String md5 = DigestUtils.md5Hex(md5In);
      IOUtils.closeQuietly(md5In);
      File md5File = getMd5File(f);
      FileUtils.writeStringToFile(md5File, md5);
      return md5File;
    } catch(IOException e) {
      throw new RuntimeException(e);
    } finally {
      IOUtils.closeQuietly(md5In);
    }
  }
  
  /**
   * Gets the file handle for an md5 associated with a content file.  Calling this method and obtaining a File handle
   * is not a guarantee that the md5 file exists.
   * @param f The source file
   * @return The md5 file
   */
  protected File getMd5File(File f) {
    return new File(f.getParent(), f.getName() + ".md5");
  }
  
  /**
   * Gets the file handle for a source file from its md5 file.
   * @param md5File The md5 file
   * @return The source file
   */
  protected File getSourceFile(File md5File) {
    return new File(md5File.getParent(), md5File.getName().substring(0, md5File.getName().length() - 4));
  }
  
  protected void checkPathSafe(String id) {
    if (id == null)
      throw new NullPointerException("IDs can not be null");
    if (id.indexOf("..") > -1 || id.indexOf(File.separator) > -1) {
      throw new IllegalArgumentException("Invalid media package, element ID, or file name");
    }
  }

  private File getFile(String mediaPackageID, String mediaPackageElementID) {
    File directory = getElementDirectory(mediaPackageID, mediaPackageElementID);
    
    File[] md5Files = directory.listFiles(MD5_FINAME_FILTER);
    if (md5Files == null) {
      logger.debug("Element directory {} does not exist", directory);
      return null;
    } else if (md5Files.length == 0) {
      logger.debug("There are no complete files in the element directory {}", directory.getAbsolutePath());
      return null;
    } else if (md5Files.length == 1) {
      File f = getSourceFile(md5Files[0]);
      if(f.exists()) {
        return f;
      } else {
        return null;
      }
    } else {
      logger.error("Integrity error: Element directory {} contains more than one element", mediaPackageID + "/"
              + mediaPackageElementID);
      throw new RuntimeException("Directory " + mediaPackageID + "/" + mediaPackageElementID
              + "does not contain exactly one element");
    }
  }

  private File getFileFromCollection(String collectionId, String fileName) {
    File directory = getCollectionDirectory(collectionId);
    File sourceFile = new File(directory, fileName);
    File md5File = getMd5File(sourceFile);
    if( ! sourceFile.exists() || ! md5File.exists()) {
      return null;
    }
    return sourceFile;
  }

  private File getElementDirectory(String mediaPackageID, String mediaPackageElementID) {
    return new File(rootDirectory + File.separator + "mp" + File.separator + mediaPackageID
            + File.separator + mediaPackageElementID);
  }

  private File getCollectionDirectory(String collectionId) {
    File collectionDir = new File(rootDirectory + File.separator + collectionId);
    if (!collectionDir.exists()) {
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
    String newRootDirectory = (String) props.get("root");
    if (newRootDirectory != null) {
      logger.info("setting root directory to {}", newRootDirectory);
      rootDirectory = newRootDirectory;
      createRootDirectory();
    }
    String newServerUrl = (String) props.get("org.opencastproject.server.url");
    if (newServerUrl != null) {
      logger.info("setting serverUrl to {}", newServerUrl);
      serverUrl = newServerUrl;
    }
  }

  private void createRootDirectory() {
    File f = new File(rootDirectory);
    if (!f.exists()) {
      try {
        FileUtils.forceMkdir(f);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getCollectionSize(java.lang.String)
   */
  @Override
  public long getCollectionSize(String id) {
    File collectionDir = getCollectionDirectory(id);
    if (!collectionDir.exists() || !collectionDir.canRead())
      throw new IllegalArgumentException("can not find collection " + id);
    File[] files = collectionDir.listFiles(MD5_FINAME_FILTER);
    if (files == null)
      throw new IllegalArgumentException("collection " + id + " is not a directory");
    return files.length;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getFromCollection(java.lang.String,
   *      java.lang.String)
   */
  @Override
  public InputStream getFromCollection(String collectionId, String fileName) {
    checkPathSafe(collectionId);
    File f = getFileFromCollection(collectionId, fileName);
    if (f == null || !f.exists() || !f.isFile()) {
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
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#putInCollection(java.lang.String,
   *      java.lang.String, java.io.InputStream)
   */
  @Override
  public URI putInCollection(String collectionId, String fileName, InputStream in) throws URISyntaxException {
    checkPathSafe(collectionId);
    checkPathSafe(fileName);
    File f = null;
    try {
      f = new File(rootDirectory + File.separator + collectionId + File.separator
              + URLEncoder.encode(fileName, CHAR_ENCODING));
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("Can not encode to " + CHAR_ENCODING);
    }
    logger.debug("Attempting to write a file to {}", f.getAbsolutePath());
    FileOutputStream out = null;
    try {
      if (!f.exists()) {
        logger.debug("Attempting to create a new file at {}", f.getAbsolutePath());
        File collectionDirectory = getCollectionDirectory(collectionId);
        if (!collectionDirectory.exists()) {
          logger.debug("Attempting to create a new directory at {}", collectionDirectory.getAbsolutePath());
          FileUtils.forceMkdir(collectionDirectory);
        }
        f.createNewFile();
      } else {
        logger.debug("Attempting to overwrite the file at {}", f.getAbsolutePath());
      }
      out = new FileOutputStream(f);
      IOUtils.copy(in, out);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      IOUtils.closeQuietly(out);
      IOUtils.closeQuietly(in);
    }
    addMd5(f);
    try {
      return new URI(serverUrl + "/files/collection/" + collectionId + "/" + URLEncoder.encode(fileName, CHAR_ENCODING));
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("Can not encode to " + CHAR_ENCODING);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#copyTo(java.lang.String, java.lang.String,
   *      java.lang.String, java.lang.String)
   */
  @Override
  public URI copyTo(String fromCollection, String fromFileName, String toMediaPackage, String toMediaPackageElement) {
    File source = getFileFromCollection(fromCollection, fromFileName);
    if(source == null) throw new IllegalArgumentException("Source file " + fromCollection + "/" + fromFileName + " does not exist");
    File destDir = getElementDirectory(toMediaPackage, toMediaPackageElement);
    if (!destDir.exists()) {
      // we needed to create the directory, but couldn't
      try {
        FileUtils.forceMkdir(destDir);
      } catch (IOException e) {
        throw new IllegalStateException("could not create mediapackage/element directory '" + destDir.getAbsolutePath()
                + "' : " + e);
      }
    }
    InputStream in = null;
    try {
      in = new FileInputStream(source);
      return put(toMediaPackage, toMediaPackageElement, source.getName(), in);
    } catch (FileNotFoundException e) {
      throw new IllegalStateException("unable to copy file" + e);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#moveTo(java.lang.String, java.lang.String,
   *      java.lang.String, java.lang.String)
   */
  @Override
  public URI moveTo(String fromCollection, String fromFileName, String toMediaPackage, String toMediaPackageElement) {
    File source = getFileFromCollection(fromCollection, fromFileName);
    if(source == null) throw new IllegalArgumentException("Source file " + fromCollection + "/" + fromFileName + " does not exist");
    File sourceMd5 = getMd5File(source);
    File destDir = getElementDirectory(toMediaPackage, toMediaPackageElement);
    if (!destDir.exists()) {
      // we needed to create the directory, but couldn't
      try {
        FileUtils.forceMkdir(destDir);
      } catch (IOException e) {
        throw new IllegalStateException("could not create mediapackage/element directory '" + destDir.getAbsolutePath()
                + "' : " + e);
      }
    }
    File dest = getFile(toMediaPackage, toMediaPackageElement);
    if(dest == null) {
      dest = new File(getElementDirectory(toMediaPackage, toMediaPackageElement), source.getName());
    }
    
    try {
      FileUtils.moveFile(source, dest);
      addMd5(dest);
      if(!sourceMd5.delete()) {
        throw new IllegalStateException("Unable to delete " + sourceMd5.getAbsolutePath());
      }
    } catch (IOException e) {
      throw new IllegalStateException("unable to copy file" + e);
    }
    return getURI(toMediaPackage, toMediaPackageElement);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#removeFromCollection(java.lang.String,
   *      java.lang.String)
   */
  @Override
  public void removeFromCollection(String collectionId, String fileName) {
    File f = getFileFromCollection(collectionId, fileName);
    if(f == null) throw new IllegalArgumentException("Source file " + collectionId + "/" + fileName + " does not exist");
    File md5File = getMd5File(f);
    if (f.exists() && f.isFile() && f.canWrite() && md5File.exists() && md5File.isFile() && md5File.canWrite()) {
      boolean md5Success = md5File.delete();
      boolean sourceSuccess = f.delete();
      if (!sourceSuccess || !md5Success)
        throw new IllegalStateException("can not delete " + f);
    } else {
      throw new IllegalStateException("file " + f + " either does not exist, is not a file, or is not writable");
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getCollectionContents(java.lang.String)
   */
  @Override
  public URI[] getCollectionContents(String collectionId) {
    File collectionDir = getCollectionDirectory(collectionId);

    File[] files = collectionDir.listFiles(MD5_FINAME_FILTER);
    URI[] uris = new URI[files.length];
    for (int i = 0; i < files.length; i++) {
      try {
        uris[i] = new URI(serverUrl + "/files/collection/" + collectionId + "/" + URLEncoder.encode(getSourceFile(files[i]).getName(), CHAR_ENCODING));
      } catch (URISyntaxException e) {
        throw new IllegalStateException("Invalid URI for " + files[i]);
      } catch (UnsupportedEncodingException e) {
        throw new IllegalStateException("Can not encode to " + CHAR_ENCODING);
      }
    }
    return uris;
  }

  public long getTotalSpace() {
    File f = new File(rootDirectory);
    return f.getTotalSpace();
  }

  public long getUsableSpace() {
    File f = new File(rootDirectory);
    return f.getUsableSpace();
  }

  public String getDiskSpace() {
    int usable = Math.round(getUsableSpace() / 1024 / 1024 / 1024);
    int total = Math.round(getTotalSpace() / 1024 / 1024 / 1024);
    long percent = Math.round(100.0 * getUsableSpace() / (1 + getTotalSpace()));
    return "Usable space " + usable + " Gb out of " + total + " Gb (" + percent + "%)";
  }
}
