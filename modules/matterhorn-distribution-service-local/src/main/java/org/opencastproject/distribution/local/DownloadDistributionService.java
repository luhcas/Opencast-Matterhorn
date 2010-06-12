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
package org.opencastproject.distribution.local;

import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.remote.api.RemoteServiceManager;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.UrlSupport;

import org.apache.commons.io.FilenameUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Distributes media to the local media delivery directory.
 */
public class DownloadDistributionService extends AbstractLocalDistributionService {
  private static final Logger logger = LoggerFactory.getLogger(DownloadDistributionService.class);
  
  public static final String DEFAULT_DISTRIBUTION_DIR = "opencast" + File.separator + "static";
  protected RemoteServiceManager remoteServiceManager;
  protected File distributionDirectory = null;


  /** this server's base URL */
  protected String serverUrl = null;

  /* the configured id for this distribution channel */
  protected String distChannelId = null;

  /**
   * Creates a download distribution service publishing to the default directory {@link #DEFAULT_DISTRIBUTION_DIR} located
   * in <code>java.io.tmmpdir</code>.
   */
  public DownloadDistributionService() {
    this(new File(System.getProperty("java.io.tmpdir") + File.separator + DEFAULT_DISTRIBUTION_DIR));
  }
  
  public void setRemoteServiceManager(RemoteServiceManager remoteServiceManager) {
    this.remoteServiceManager = remoteServiceManager;
  }

  /**
   * Creates a download distribution service that will move files to the given directory.
   * 
   * @param distributionRoot
   *          the distribution directory
   */
  public DownloadDistributionService(File distributionRoot) {
    this.distributionDirectory = distributionRoot;
  }

  protected void activate(ComponentContext cc) {
    // Get the configured server URL
    if (cc == null) {
      serverUrl = UrlSupport.DEFAULT_BASE_URL;
    } else {
      serverUrl = cc.getBundleContext().getProperty("org.opencastproject.server.url");
      String ccDistributionDirectory = cc.getBundleContext().getProperty("org.opencastproject.download.directory");
      logger.info("configured download directory is {}", ccDistributionDirectory);
      if(ccDistributionDirectory != null) {
        this.distributionDirectory = new File(ccDistributionDirectory);
      }
      distChannelId = (String)cc.getProperties().get("distribution.channel");
      remoteServiceManager.registerService(JOB_TYPE_PREFIX + distChannelId, serverUrl);
    }
  }

  protected void deactivate() {
    remoteServiceManager.unRegisterService(JOB_TYPE_PREFIX + distChannelId, serverUrl);
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.distribution.local.AbstractLocalDistributionService#getDistributionFile(org.opencastproject.mediapackage.MediaPackageElement)
   */
  @Override
  protected File getDistributionFile(MediaPackageElement element) {
    String mediaPackageId = element.getMediaPackage().getIdentifier().compact();
    String elementId = element.getIdentifier(); 
    String fileName = FilenameUtils.getName(element.getURI().toString());
    String directoryName = distributionDirectory.getAbsolutePath();
    String destinationFileName = PathSupport.concat(new String[] { directoryName, mediaPackageId, elementId, fileName });
    return new File(destinationFileName);
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.distribution.local.AbstractLocalDistributionService#getDistributionUri(org.opencastproject.mediapackage.MediaPackageElement)
   */
  @Override
  protected URI getDistributionUri(MediaPackageElement element) throws URISyntaxException {
    String mediaPackageId = element.getMediaPackage().getIdentifier().compact();
    String elementId = element.getIdentifier(); 
    String fileName = FilenameUtils.getName(element.getURI().toString());
    String destinationURI = UrlSupport.concat(new String[] { serverUrl + "/static", mediaPackageId, elementId, fileName });
    return new URI(destinationURI);
  }
  
}
