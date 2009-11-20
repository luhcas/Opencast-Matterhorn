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
package org.opencastproject.distribution.local;

import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.media.mediapackage.Attachment;
import org.opencastproject.media.mediapackage.Catalog;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.Dictionary;

/**
 * Distributes media to the local media delivery directory. TODO: Add metadata to the search service.
 */
public class DistributionServiceImpl implements DistributionService, ManagedService {
  
  private static final Logger logger = LoggerFactory.getLogger(DistributionServiceImpl.class);
  public static final String DEFAULT_DISTRIBUTION_DIR = "opencast" + File.separator + "static"; 
  protected Workspace workspace = null;
  protected File distributionDirectory = null;

  /**
   * Creates a local distribution service publishing to the default directory {@link #DEFAULT_DISTRIBUTION_DIR}
   * located in <code>java.io.tmmpdir</code>.
   */
  public DistributionServiceImpl() {
    this(new File(System.getProperty("java.io.tmpdir") + File.separator + DEFAULT_DISTRIBUTION_DIR));
  }

  /**
   * Creates a local distribution service that will move files to the given directory.
   * 
   * @param distributionRoot
   *          the distribution directory
   */
  public DistributionServiceImpl(File distributionRoot) {
    this.distributionDirectory = distributionRoot;
  }

  /**
   * Creates directories for the media package and subdirectories for media, metadata, and attachments. {@inheritDoc}
   * 
   * @see org.opencastproject.distribution.api.DistributionService#distribute(org.opencastproject.media.mediapackage.MediaPackage)
   */
  public MediaPackage distribute(MediaPackage mediaPackage, String... elementIds) {
    Arrays.sort(elementIds);
    File mediaPackageDirectory = new File(distributionDirectory, mediaPackage.getIdentifier().compact());
    File mediaDirectory = new File(mediaPackageDirectory, "media");
    File metadataDirectory = new File(mediaPackageDirectory, "metadata");
    File attachmentsDirectory = new File(mediaPackageDirectory, "attachments");
    try {
      FileUtils.forceMkdir(mediaPackageDirectory);
      logger.info("created directory " + mediaPackageDirectory);
      FileUtils.forceMkdir(mediaDirectory);
      FileUtils.forceMkdir(metadataDirectory);
      FileUtils.forceMkdir(attachmentsDirectory);
      for (Track track : mediaPackage.getTracks()) {
        if(Arrays.binarySearch(elementIds, track.getIdentifier()) >= 0) {
          File trackFile = workspace.get(track.getURL());
          FileUtils.copyFile(trackFile, new File(mediaDirectory, trackFile.getName()));
        }
      }
      for (Catalog catalog : mediaPackage.getCatalogs()) {
        if(Arrays.binarySearch(elementIds, catalog.getIdentifier()) >= 0) {
          File catalogFile = workspace.get(catalog.getURL());
          FileUtils.copyFile(catalogFile, new File(metadataDirectory, catalogFile.getName()));
        }
      }
      for (Attachment attachment : mediaPackage.getAttachments()) {
        if(Arrays.binarySearch(elementIds, attachment.getIdentifier()) >= 0) {
          File attachmentFile = workspace.get(attachment.getURL());
          FileUtils.copyFile(attachmentFile, new File(attachmentsDirectory, attachmentFile.getName()));
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    return mediaPackage; // TODO: Should the URLs for the distributed files be added to the media package?
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  @SuppressWarnings("unchecked")
  public void updated(Dictionary properties) throws ConfigurationException {
    String updatedRootDir = (String)properties.get("distributionDirectory");
    if(updatedRootDir != null) {
      File f = new File(updatedRootDir);
      if(f.exists()) {
        this.distributionDirectory = f;
        logger.info("Set distribution directory to " + updatedRootDir);
      } else {
        logger.warn("Can not set distribution directory to " + updatedRootDir + ".  Directory does not exist");
      }
    }
  }

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

}
