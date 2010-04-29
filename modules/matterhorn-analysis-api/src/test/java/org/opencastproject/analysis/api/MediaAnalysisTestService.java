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

import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogImpl;
import org.opencastproject.receipt.api.Receipt;

import org.junit.Ignore;

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
  public Receipt analyze(MediaPackageElement element, boolean block) throws MediaAnalysisException {
    ReceiptStub receipt = new ReceiptStub();
    receipt.element = Mpeg7CatalogImpl.newInstance();
    return receipt;
  }

  class ReceiptStub implements Receipt {
    MediaPackageElement element;
    Status status;
    public MediaPackageElement getElement() {
      return element;
    }
    public String getHost() {
      return null;
    }
    public String getId() {
      return null;
    }
    public Status getStatus() {
      return status;
    }
    public String getType() {
      return "analysis-test";
    }
    public void setElement(MediaPackageElement element) {
      this.element = element;
    }
    public void setHost(String host) {
    }
    public void setId(String id) {
    }
    public void setStatus(Status status) {
      this.status = status;
    }
    public void setType(String type) {
    }
  }

}
