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
package org.opencastproject.distribution.streaming;

import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.Track;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Distributes media to the local media delivery directory.
 */
public class StreamingDistributionService extends AbstractLocalDistributionService {
  private static final Logger logger = LoggerFactory.getLogger(StreamingDistributionService.class);
  
  protected RemoteServiceManager remoteServiceManager;
  public static final String DEFAULT_DISTRIBUTION_DIR = "opencast" + File.separator;
  protected File distributionDirectory = null;

  /** this server's base URL */
  protected String serverUrl = null;

  /** the base URL for streaming */
  protected String streamingUrl = null;

  public void setRemoteServiceManager(RemoteServiceManager remoteServiceManager) {
    this.remoteServiceManager = remoteServiceManager;
  }

  protected void activate(ComponentContext cc) {
    // Get the configured streaming and server URLs
    if (cc != null) {
      streamingUrl = cc.getBundleContext().getProperty("org.opencastproject.streaming.url");
      if (streamingUrl == null)
        throw new IllegalStateException("Stream url must be set (org.opencastproject.streaming.url)");
      logger.info("streaming url is {}", streamingUrl);

      serverUrl = (String)cc.getBundleContext().getProperty("org.opencastproject.server.url");
      if (serverUrl == null)
        throw new IllegalStateException("Server url must be set (org.opencastproject.server.url)");
      
      distributionDirectory = new File(cc.getBundleContext().getProperty("org.opencastproject.streaming.directory"));
      if (distributionDirectory == null)
        throw new IllegalStateException("Distribution directory must be set (org.opencastproject.streaming.directory)");
      logger.info("Streaming distribution directory is {}", distributionDirectory);
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.distribution.download.AbstractLocalDistributionService#distribute(org.opencastproject.mediapackage.MediaPackage, java.lang.String[])
   */
  @Override
  public MediaPackage distribute(MediaPackage mediaPackage, String... elementIds) throws DistributionException {
    // The streaming distribution service should only copy tracks, not catalogs or attachments
    List<String> trackIds = new ArrayList<String>();
    for(String elementId : elementIds) {
      MediaPackageElement element = mediaPackage.getElementById(elementId);
      if(element instanceof Track) trackIds.add(elementId);
    }
    return super.distribute(mediaPackage, trackIds.toArray(new String[trackIds.size()]));
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.distribution.download.AbstractLocalDistributionService#getDistributionFile(org.opencastproject.mediapackage.MediaPackageElement)
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
   * @see org.opencastproject.distribution.download.AbstractLocalDistributionService#getDistributionUri(org.opencastproject.mediapackage.MediaPackageElement)
   */
  @Override
  protected URI getDistributionUri(MediaPackageElement element) throws URISyntaxException {
    String mediaPackageId = element.getMediaPackage().getIdentifier().compact();
    String elementId = element.getIdentifier(); 
    String fileName = FilenameUtils.getName(element.getURI().toString());
    String destinationURI = UrlSupport.concat(new String[] { streamingUrl, mediaPackageId, elementId, fileName });
    return new URI(destinationURI);
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.distribution.streaming.AbstractLocalDistributionService#getMediaPackageDirectory(java.lang.String)
   */
  @Override
  protected File getMediaPackageDirectory(String mediaPackageId) {
    return new File(distributionDirectory, mediaPackageId);
  }
}
