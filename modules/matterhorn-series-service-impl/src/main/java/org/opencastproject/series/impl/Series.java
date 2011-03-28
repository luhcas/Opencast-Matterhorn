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
package org.opencastproject.series.impl;

import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.security.api.AccessControlList;

/**
 * Wrapper class for series, storing both series Dublin Core and Access control.
 */
public class Series {

  private DublinCoreCatalog seriesCatalog;
  private AccessControlList accessControl;
  
  public Series(DublinCoreCatalog seriesCatalog, AccessControlList accessControl) {
    this.seriesCatalog = seriesCatalog;
    this.accessControl = accessControl;
  }

  public DublinCoreCatalog getSeriesCatalog() {
    return seriesCatalog;
  }

  public void setSeriesCatalog(DublinCoreCatalog seriesCatalog) {
    this.seriesCatalog = seriesCatalog;
  }

  public AccessControlList getAccessControl() {
    return accessControl;
  }

  public void setAccessControl(AccessControlList accessControl) {
    this.accessControl = accessControl;
  }
}
