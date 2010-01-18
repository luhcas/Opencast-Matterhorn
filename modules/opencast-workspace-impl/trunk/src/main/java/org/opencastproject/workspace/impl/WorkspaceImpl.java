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
package org.opencastproject.workspace.impl;

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
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
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
  private static final Logger logger = LoggerFactory.getLogger(WorkspaceImpl.class);
  
  private WorkingFileRepository repo;
  private String rootDirectory = null;
  private Map<String, String> filesystemMappings;
  
  public WorkspaceImpl() {
    this(System.getProperty("java.io.tmpdir") + File.separator + "opencast" + File.separator + "workspace");
  }
  
  public WorkspaceImpl(String rootDirectory) {
    this.rootDirectory = rootDirectory;
    createRootDirectory();
  }

  public void activate(ComponentContext cc) {
    filesystemMappings = new HashMap<String, String>();
    String filesUrl;
    if(cc == null || cc.getBundleContext().getProperty("serverUrl") == null) {
      filesUrl = UrlSupport.DEFAULT_BASE_URL + "/files";
    } else {
      filesUrl = cc.getBundleContext().getProperty("serverUrl") + "/files";
    }
    // TODO Remove hard coded path
    filesystemMappings.put(filesUrl, System.getProperty("java.io.tmpdir") + File.separator + "opencast" + File.separator + "workingfilerepo");
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
    
    String urlHash = getFilenameSafeHash(urlString);
    String fileName = rootDirectory + File.separator + urlHash;
    // See if there's a matching file under the root directory
    File f = new File(fileName);
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

  protected String getFilenameSafeHash(String urlString) {
    try {
      String urlEncoded = URLEncoder.encode(urlString, "UTF-8").replaceAll("\\.", "-");
      if(urlEncoded.length() < 255) return urlEncoded;
    } catch(UnsupportedEncodingException e) {}
    String random = UUID.randomUUID().toString();
    String urlExtension = FilenameUtils.getExtension(urlString);
    if( ! org.apache.commons.lang.StringUtils.isEmpty(urlExtension)) {
      random = random.concat(".").concat(urlExtension);
    }
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

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workspace.api.Workspace#getURI(java.lang.String, java.lang.String)
   */
  public URI getURI(String mediaPackageID, String mediaPackageElementID) {
    return repo.getURI(mediaPackageID, mediaPackageElementID);
  }
}
