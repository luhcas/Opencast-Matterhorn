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
package org.opencastproject.ingest.impl;

import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.UnsupportedElementException;
import org.opencastproject.mediapackage.identifier.HandleException;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.WorkflowInstance;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Map;

/**
 * Delegates composer methods to either the local composer service impl, or to a remote service. If a "composer.remote"
 * property is provided during activation, the composer service at that URL will be used.
 */
public class IngestServiceDelegatingImpl implements IngestService {
  private static final Logger logger = LoggerFactory.getLogger(IngestServiceRemoteImpl.class);

  /**
   * The composer service handling the actual work.
   */
  IngestService delegate;

  /**
   * The local composer service implementation
   */
  IngestService local;

  /**
   * @param local
   *          the local to set
   */
  public void setLocal(IngestService local) {
    this.local = local;
  }

  /**
   * The remote composer service implementation
   */
  IngestServiceRemoteImpl remote;

  /**
   * @param remote
   *          the remote to set
   */
  public void setRemote(IngestServiceRemoteImpl remote) {
    this.remote = remote;
  }

  public void activate(ComponentContext cc) {
    String remoteHost = cc.getBundleContext().getProperty(IngestServiceRemoteImpl.REMOTE_INGEST);
    if (remoteHost == null) {
      delegate = local;
    } else {
      logger.info("Ingest service is running on a remote server: " + remoteHost);
      delegate = remote;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.ingest.api.IngestService#addAttachment(java.net.URI, MediaPackageElementFlavor,
   *      MediaPackage)
   */
  @Override
  public MediaPackage addAttachment(URI uri, MediaPackageElementFlavor flavor, MediaPackage mediaPackage)
          throws MediaPackageException, UnsupportedElementException, IOException {
    return delegate.addAttachment(uri, flavor, mediaPackage);
  }

  @Override
  public MediaPackage addAttachment(InputStream file, String fileName, MediaPackageElementFlavor flavor,
          MediaPackage mediaPackage) throws MediaPackageException, UnsupportedElementException, MalformedURLException,
          IOException {
    return delegate.addAttachment(file, fileName, flavor, mediaPackage);
  }

  @Override
  public MediaPackage addCatalog(URI uri, MediaPackageElementFlavor flavor, MediaPackage mediaPackage)
          throws MediaPackageException, UnsupportedElementException, IOException, NotFoundException {
    return delegate.addCatalog(uri, flavor, mediaPackage);
  }

  @Override
  public MediaPackage addCatalog(InputStream catalog, String fileName, MediaPackageElementFlavor flavor,
          MediaPackage mediaPackage) throws MediaPackageException, UnsupportedElementException, MalformedURLException,
          IOException, NotFoundException {
    return delegate.addCatalog(catalog, fileName, flavor, mediaPackage);
  }

  @Override
  public MediaPackage addTrack(URI uri, MediaPackageElementFlavor flavor, MediaPackage mediaPackage)
          throws MediaPackageException, UnsupportedElementException, IOException {
    return delegate.addTrack(uri, flavor, mediaPackage);
  }

  @Override
  public MediaPackage addTrack(InputStream mediaFile, String fileName, MediaPackageElementFlavor flavor,
          MediaPackage mediaPackage) throws MediaPackageException, UnsupportedElementException, MalformedURLException,
          IOException {
    return delegate.addTrack(mediaFile, fileName, flavor, mediaPackage);
  }

  @Override
  public WorkflowInstance addZippedMediaPackage(InputStream ZippedMediaPackage) throws MediaPackageException,
          FileNotFoundException, IOException, Exception {
    return delegate.addZippedMediaPackage(ZippedMediaPackage);
  }

  @Override
  public WorkflowInstance addZippedMediaPackage(InputStream ZippedMediaPackage, String workflowDefinitionID)
          throws MediaPackageException, FileNotFoundException, IOException, Exception {
    return delegate.addZippedMediaPackage(ZippedMediaPackage, workflowDefinitionID);
  }

  @Override
  public WorkflowInstance addZippedMediaPackage(InputStream ZippedMediaPackage, String workflowDefinitionID,
          Map<String, String> wfConfig) throws MediaPackageException, FileNotFoundException, IOException, Exception {
    return delegate.addZippedMediaPackage(ZippedMediaPackage, workflowDefinitionID, wfConfig);
  }

  @Override
  public MediaPackage createMediaPackage() throws MediaPackageException, ConfigurationException, HandleException,
          IOException {
    return delegate.createMediaPackage();
  }

  @Override
  public void discardMediaPackage(MediaPackage mediaPackage) throws IOException {
    delegate.discardMediaPackage(mediaPackage);
  }

  @Override
  public WorkflowInstance ingest(MediaPackage mediaPackage) throws IllegalStateException, Exception {
    return delegate.ingest(mediaPackage);
  }

  @Override
  public WorkflowInstance ingest(MediaPackage mediaPackage, String workflowDefinitionID) throws IllegalStateException,
          Exception {
    return delegate.ingest(mediaPackage, workflowDefinitionID);
  }

  @Override
  public WorkflowInstance ingest(MediaPackage mediaPackage, String workflowDefinitionID, Map<String, String> properties)
          throws IllegalStateException, Exception {
    return delegate.ingest(mediaPackage, workflowDefinitionID, properties);
  }
}
