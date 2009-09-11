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
import java.util.Dictionary;

/**
 * Distributes media to the local media delivery directory.  TODO: Add metadata to the search service.
 */
public class DistributionServiceImpl implements DistributionService, ManagedService {
  private static final Logger logger = LoggerFactory.getLogger(DistributionServiceImpl.class);

  protected Workspace workspace;
  protected String distributionDirectory = System.getProperty("java.io.tmpdir") + File.separator + "opencast" +
    File.separator + "static";

  /**
   * Creates directories for the media package and subdirectories for media, metadata, and attachments.
   * {@inheritDoc}
   * @see org.opencastproject.distribution.api.DistributionService#distribute(org.opencastproject.media.mediapackage.MediaPackage)
   */
  public void distribute(MediaPackage mediaPackage) {
    File mediaPackageDirectory = new File(distributionDirectory, mediaPackage.getIdentifier().getLocalName());
    File mediaDirectory = new File(mediaPackageDirectory, "media");
    File metadataDirectory = new File(mediaPackageDirectory, "metadata");
    File attachmentsDirectory = new File(mediaPackageDirectory, "attachments");
    try {
      FileUtils.forceMkdir(mediaPackageDirectory);
      logger.info("created directory " + mediaPackageDirectory);
      FileUtils.forceMkdir(mediaDirectory);
      FileUtils.forceMkdir(metadataDirectory);
      FileUtils.forceMkdir(attachmentsDirectory);
      for(Track track : mediaPackage.getTracks()) {
        File trackFile = workspace.get(track.getURL());
        FileUtils.copyFile(trackFile, new File(mediaDirectory, trackFile.getName()));
      }
      for(Catalog catalog : mediaPackage.getCatalogs()) {
        File catalogFile = workspace.get(catalog.getURL());
        FileUtils.copyFile(catalogFile, new File(metadataDirectory, catalogFile.getName()));
      }
      for(Attachment attachment : mediaPackage.getAttachments()) {
        File attachmentFile = workspace.get(attachment.getURL());
        FileUtils.copyFile(attachmentFile, new File(attachmentsDirectory, attachmentFile.getName()));
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  @SuppressWarnings("unchecked")
  public void updated(Dictionary properties) throws ConfigurationException {
  }
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }
}
