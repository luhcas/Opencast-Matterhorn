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
package org.opencastproject.analysis.dictionary;

import org.opencastproject.metadata.mpeg7.Mpeg7Catalog;
import org.opencastproject.receipt.api.Receipt;

import org.osgi.service.component.ComponentContext;

/**
 * Delegates Dictionary methods to either the local Dictionary service impl, or to a remote service. If a
 * "remote.dictionary" property is provided during activation, the service at that URL will be used.
 */
public class DictionaryServiceDelegatingImpl extends DictionaryService {

  /**
   * The dictionary service handling the actual work.
   */
  DictionaryService delegate;

  /**
   * The local dictionary service implementation
   */
  DictionaryService local;

  /**
   * @param local
   *          the local to set
   */
  public void setLocal(DictionaryService local) {
    this.local = local;
  }

  /**
   * The remote dictionary service implementation
   */
  DictionaryServiceRemoteImpl remote;

  /**
   * @param remote
   *          the remote to set
   */
  public void setRemote(DictionaryServiceRemoteImpl remote) {
    this.remote = remote;
  }

  public void activate(ComponentContext cc) {
    String remoteHost = cc.getBundleContext().getProperty(DictionaryServiceRemoteImpl.REMOTE_DICTIONARY);
    if (remoteHost == null) {
      delegate = local;
    } else {
      delegate = remote;
    }
  }

  @Override
  public Receipt clean(Mpeg7Catalog catalog, boolean block) {
    return delegate.clean(catalog, block);
  }

  @Override
  public Receipt getReceipt(String id) {
    return delegate.getReceipt(id);
  }

}
