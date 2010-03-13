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
package org.opencastproject.workspace.impl;

import org.opencastproject.util.IoSupport;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

/**
 * Implements a simple cache for remote URIs.  Delegates methods to {@link WorkingFileRepository}
 * wherever possible.
 * 
 * TODO Implement cache invalidation using the caching headers, if provided, from the remote server.
 */
public class WorkspaceImpl implements Workspace, ManagedService {
  public static final String WORKSPACE_ROOTDIR = "workspace.rootdir";
  public static final String WORKSPACE_WORKING_FILEDIR = "workspace.workingfiledir";
  private static final Logger logger = LoggerFactory.getLogger(WorkspaceImpl.class);

  private WorkingFileRepository repo;
  private String rootDirectory = null;
  private Map<String, String> filesystemMappings;
  
  public WorkspaceImpl() {
    this(null);
  }
  
  public WorkspaceImpl(String rootDirectory) {
    if (rootDirectory == null) {
      // MH-2159 - we will still set the default tmp dir here
      rootDirectory = IoSupport.getSystemTmpDir() + "opencast" + File.separator + "workspace";
    }
    this.rootDirectory = rootDirectory;
    createRootDirectory();
  }

  public void activate(ComponentContext cc) {
    // NOTE: warning - the test calls activate() with a NULL cc
    String tmpdir = IoSupport.getSystemTmpDir();

    // MH-2159 - load the root directory from config
    if (cc == null || cc.getBundleContext().getProperty(WORKSPACE_ROOTDIR) == null) {
      // DEFAULT rootDirectory
      if (this.rootDirectory == null) {
        this.rootDirectory = tmpdir + "opencast" + File.separator + "workspace";
        createRootDirectory();
      }
      logger.info("DEFAULT "+WORKSPACE_ROOTDIR+": "+this.rootDirectory);
    } else {
      // use rootDir from CONFIG
      this.rootDirectory = cc.getBundleContext().getProperty(WORKSPACE_ROOTDIR);
      logger.info("CONFIG "+WORKSPACE_ROOTDIR+": "+this.rootDirectory);
      createRootDirectory();
    }
    String workingFileDir = tmpdir + "opencast" + File.separator + "workingfilerepo";
    if (cc != null && cc.getBundleContext().getProperty(WORKSPACE_WORKING_FILEDIR) != null) {
      // use CONFIG
      workingFileDir = cc.getBundleContext().getProperty(WORKSPACE_WORKING_FILEDIR);
      logger.info("CONFIG "+WORKSPACE_WORKING_FILEDIR+": "+workingFileDir);
    } else {
      // DEFAULT
      logger.info("DEFAULT "+WORKSPACE_WORKING_FILEDIR+": "+workingFileDir);
    }

    filesystemMappings = new HashMap<String, String>();
    String filesUrl;
    if (cc == null || cc.getBundleContext().getProperty("serverUrl") == null) {
      filesUrl = UrlSupport.DEFAULT_BASE_URL + "/files";
    } else {
      filesUrl = cc.getBundleContext().getProperty("serverUrl") + "/files";
    }
    logger.info("Workspace filesystem mapping "+filesUrl+" => "+workingFileDir);
    filesystemMappings.put(filesUrl, workingFileDir);
  }

  @SuppressWarnings("unchecked")
  public void updated(Dictionary props) throws ConfigurationException {
    if (props != null) {
      if (props.get("root") != null) {
        rootDirectory = (String)props.get("root");
        createRootDirectory();
      }
    }
  }

  public File get(URI uri) {
    String urlString = uri.toString();
    
    // If any local filesystem mappings match this uri, just return the file handle
    for(Entry<String, String> entry : filesystemMappings.entrySet()) {
      String baseUrl = entry.getKey();
      String baseFilesystemPath = entry.getValue();
      if(urlString.startsWith(baseUrl)) {
        String pathExtraInfo = urlString.substring(baseUrl.length());
        File f = new File(baseFilesystemPath + pathExtraInfo);
        if(f.exists() && f.isFile() && f.canRead()) {
          logger.debug("found local file {} for URL {}", f.getAbsolutePath(), urlString);
          return f;
        }
      }
    }
    
    String safeFilename = getSafeFilename(urlString);
    String fullPath = rootDirectory + File.separator + safeFilename;
    // See if there's a matching file under the root directory
    File f = new File(fullPath);
    if(f.exists()) {
      return f;
    } else {
      logger.info("Copying {} to {}", urlString, f.getAbsolutePath());
      try {
        FileUtils.copyURLToFile(uri.toURL(), f);
        return f;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  protected String getSafeFilename(String urlString) {
    String urlExtension = FilenameUtils.getExtension(urlString);
    String baseName = urlString.substring(0, urlString.length() - urlExtension.length());
    String safeBaseName = baseName.replaceAll("\\W", ""); // FIXME -- make this safe and platform independent
    String safeString = null;
    if("".equals(urlExtension)) {
      safeString = safeBaseName;
    } else {
      safeString = safeBaseName + "." + urlExtension;
    }
    if(safeString.length() < 255) return safeString;
    String random = UUID.randomUUID().toString();
    random = random.concat(".").concat(urlExtension);
    logger.info("using '{}' to represent url '{}', which is too long to store as a filename", random, urlString);
    return random;
  }
  
  public void delete(String mediaPackageID, String mediaPackageElementID) {
    repo.delete(mediaPackageID, mediaPackageElementID);
  }

  public InputStream get(String mediaPackageID, String mediaPackageElementID) {
    return repo.get(mediaPackageID, mediaPackageElementID);
  }

  public URI put(String mediaPackageID, String mediaPackageElementID, InputStream in) {
    repo.put(mediaPackageID, mediaPackageElementID, in);
    return getURI(mediaPackageID, mediaPackageElementID);
  }

  public URI put(String mediaPackageID, String mediaPackageElementID, String fileName, InputStream in) {
    // if this is a url encoded filename, decoded it and take the last part of the path
//    File mediaPackageDir = new File(rootDirectory, mediaPackageID); // TODO Make sure the clients are using path-safe directories
//    mediaPackageDir.mkdir();
//    File elementDir = new File(mediaPackageDir, mediaPackageElementID);
//    elementDir.mkdir();
    String shortFileName;
    try {
      String decoded = URLDecoder.decode(fileName, "UTF-8");
      logger.debug("{} decoded to {}", fileName, decoded);
      String[] sa = decoded.split("/");
      shortFileName = sa[sa.length-1];
    } catch (Exception e) {
      // just be sure to deal with a null decoded
      logger.debug(e.getMessage());
      shortFileName = fileName;
    }
    repo.put(mediaPackageID, mediaPackageElementID, shortFileName, in);
    return getURI(mediaPackageID, mediaPackageElementID);
  }

  public void setRepository(WorkingFileRepository repo) {
    this.repo = repo;
  }

  public void unsetRepository(WorkingFileRepository repo) {
    this.repo = null;
  }

  private void createRootDirectory() {
    File f = new File(this.rootDirectory);
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
   * @see org.opencastproject.workspace.api.Workspace#getURI(java.lang.String, java.lang.String)
   */
  public URI getURI(String mediaPackageID, String mediaPackageElementID) {
    return repo.getURI(mediaPackageID, mediaPackageElementID);
  }
}
