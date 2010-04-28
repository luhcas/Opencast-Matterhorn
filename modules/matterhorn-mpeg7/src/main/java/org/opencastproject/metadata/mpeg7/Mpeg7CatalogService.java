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
package org.opencastproject.metadata.mpeg7;

import org.opencastproject.media.mediapackage.Catalog;
import org.opencastproject.metadata.api.CatalogService;
import org.opencastproject.security.api.TrustedHttpClient;

/**
 * Loads {@link Mpeg7Catalog}s
 */
public class Mpeg7CatalogService implements CatalogService<Mpeg7Catalog> {

  protected TrustedHttpClient trustedHttpClient;
  
  public void setTrustedHttpClient(TrustedHttpClient trustedHttpClient) {
    this.trustedHttpClient = trustedHttpClient;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.metadata.api.CatalogService#load(org.opencastproject.media.mediapackage.Catalog)
   */
  @Override
  public Mpeg7Catalog load(Catalog catalog) {
    Mpeg7CatalogImpl cat = (Mpeg7CatalogImpl) Mpeg7CatalogImpl.fromURI(catalog.getURI());
    cat.trustedHttpClient = trustedHttpClient;
    return cat;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.metadata.api.CatalogService#newInstance()
   */
  @Override
  public Mpeg7Catalog newInstance() {
    Mpeg7CatalogImpl cat = Mpeg7CatalogImpl.newInstance();
    cat.trustedHttpClient = trustedHttpClient;
    return cat;
  }

}
