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

import org.opencastproject.media.mediapackage.MediaPackageElement;
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
  protected File distributionDirectory = null;
  protected String serverUrl = null;

  /**
   * Creates a download distribution service publishing to the default directory {@link #DEFAULT_DISTRIBUTION_DIR} located
   * in <code>java.io.tmmpdir</code>.
   */
  public DownloadDistributionService() {
    this(new File(System.getProperty("java.io.tmpdir") + File.separator + DEFAULT_DISTRIBUTION_DIR));
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

  public void activate(ComponentContext cc) {
    // Get the configured server URL
    if (cc == null) {
      serverUrl = UrlSupport.DEFAULT_BASE_URL + "/static";
    } else {
      String ccServerUrl = cc.getBundleContext().getProperty("org.opencastproject.server.url");
      logger.info("configured server url is {}", ccServerUrl);
      if (ccServerUrl == null) {
        serverUrl = UrlSupport.DEFAULT_BASE_URL + "/static";
      } else {
        serverUrl = ccServerUrl + "/static";
      }
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.distribution.local.AbstractLocalDistributionService#getDistributionFile(org.opencastproject.media.mediapackage.MediaPackageElement)
   */
  @Override
  protected File getDistributionFile(MediaPackageElement element) {
    String mediaPackageId = element.getMediaPackage().getIdentifier().compact();
    String elementType = element.getElementType().toString().toLowerCase(); 
    String fileName = FilenameUtils.getName(element.getURI().toString());
    String directoryName = distributionDirectory.getAbsolutePath();
    String destinationFileName = PathSupport.concat(new String[] { directoryName, mediaPackageId, elementType, fileName });
    return new File(destinationFileName);
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.distribution.local.AbstractLocalDistributionService#getDistributionUri(org.opencastproject.media.mediapackage.MediaPackageElement)
   */
  @Override
  protected URI getDistributionUri(MediaPackageElement element) throws URISyntaxException {
    String mediaPackageId = element.getMediaPackage().getIdentifier().compact();
    String elementType = element.getElementType().toString().toLowerCase(); 
    String fileName = FilenameUtils.getName(element.getURI().toString());
    String destinationURI = UrlSupport.concat(new String[] { serverUrl, mediaPackageId, elementType, fileName });
    return new URI(destinationURI);
  }
  
}
