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
package org.opencastproject.analysis.api;

import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.metadata.mpeg7.Mpeg7Catalog;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogImpl;

import org.junit.Ignore;

import java.net.URL;

/**
 * Test implementation for the abstract media analysis support.
 */
@Ignore
public class MediaAnalysisTestService extends MediaAnalysisServiceSupport {

  /**
   * Creates a new test implementation object.
   * 
   * @param resultingFlavor
   *          the resulting flavor
   * @param requiredFlavors
   *          the required flavors
   */
  public MediaAnalysisTestService(MediaPackageElementFlavor resultingFlavor, MediaPackageElementFlavor[] requiredFlavors) {
    super(resultingFlavor, requiredFlavors);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.analysis.api.MediaAnalysisServiceSupport#analyze(java.net.URL)
   */
  @Override
  public Mpeg7Catalog analyze(URL mediaUrl) throws MediaAnalysisException {
    return Mpeg7CatalogImpl.newInstance();
  }

}
