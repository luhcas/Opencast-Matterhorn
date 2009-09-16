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

import org.opencastproject.workingfilerepository.api.WorkingFileRepository;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;

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

  public WorkspaceImpl() {
    rootDirectory = System.getProperty("java.io.tmpdir") + File.separator + "opencast" + File.separator + "workspace";
    createRootDirectory();
  }

  public File get(URL url) {
    String uriHash = DigestUtils.md5Hex(url.toString());
    logger.debug("Hashed " + url.toString() + " to " + uriHash);
    // See if there's a matching file under the root directory
    File f = new File(rootDirectory + File.separator + uriHash);
    if(f.exists()) {
      return f;
    } else {
      try {
        FileUtils.copyURLToFile(url, f);
        return f;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  public void delete(String mediaPackageID, String mediaPackageElementID) {
    repo.delete(mediaPackageID, mediaPackageElementID);
  }

  public InputStream get(String mediaPackageID, String mediaPackageElementID) {
    return repo.get(mediaPackageID, mediaPackageElementID);
  }

  public void put(String mediaPackageID, String mediaPackageElementID, InputStream in) {
    repo.put(mediaPackageID, mediaPackageElementID, in);
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
}
