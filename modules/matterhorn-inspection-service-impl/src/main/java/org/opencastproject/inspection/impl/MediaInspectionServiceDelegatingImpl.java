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
package org.opencastproject.inspection.impl;

import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.receipt.api.Receipt;

import org.osgi.service.component.ComponentContext;

import java.net.URI;

/**
 * Delegates composer methods to either the local composer service impl, or to a remote service. If a "composer.remote"
 * property is provided during activation, the composer service at that URL will be used.
 */
public class MediaInspectionServiceDelegatingImpl implements MediaInspectionService {

  /**
   * The composer service handling the actual work.
   */
  MediaInspectionService delegate;

  /**
   * The local composer service implementation
   */
  MediaInspectionServiceImpl local;

  /**
   * @param local
   *          the local to set
   */
  public void setLocal(MediaInspectionServiceImpl local) {
    this.local = local;
  }

  /**
   * The remote composer service implementation
   */
  MediaInspectionServiceRemoteImpl remote;

  /**
   * @param remote
   *          the remote to set
   */
  public void setRemote(MediaInspectionServiceRemoteImpl remote) {
    this.remote = remote;
  }

  public void activate(ComponentContext cc) {
    String remoteHost = cc.getBundleContext().getProperty(MediaInspectionServiceRemoteImpl.REMOTE_MEDIA_INSPECTION);
    if (remoteHost == null) {
      delegate = local;
    } else {
      delegate = remote;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.inspection.api.MediaInspectionService#enrich(org.opencastproject.media.mediapackage.AbstractMediaPackageElement,
   *      boolean, boolean)
   */
  @Override
  public Receipt enrich(MediaPackageElement original, boolean override, boolean block) {
    return delegate.enrich(original, override, block);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.inspection.api.MediaInspectionService#getReceipt(java.lang.String)
   */
  @Override
  public Receipt getReceipt(String id) {
    return delegate.getReceipt(id);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.inspection.api.MediaInspectionService#inspect(java.net.URI, boolean)
   */
  @Override
  public Receipt inspect(URI uri, boolean block) {
    return delegate.inspect(uri, block);
  }

}
