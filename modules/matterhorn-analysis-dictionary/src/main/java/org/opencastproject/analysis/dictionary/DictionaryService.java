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
import org.opencastproject.receipt.api.ReceiptService;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.workspace.api.Workspace;

/**
 * The dictionary service can be used to clean a list of words with respect to a given dictionary.
 */
public class DictionaryService {

  /** Reference to the receipt service */
  private ReceiptService receiptService;

  /** The workspace to ue when retrieving remote media files */
  private Workspace workspace;

  /** The http client to use for retrieving protected mpeg7 files */
  protected TrustedHttpClient trustedHttpClient;

  /**
   * Takes a look at the catalog and tries to remove those words that can't be found in the dictionary.
   * 
   * @param catalog
   *          the original catalog
   * @param block
   *          <code>true</code> to make the call synchronous
   * @return a receipt that will contain the cleaned <code>Mpeg7Catalog</code>
   */
  public Receipt clean(Mpeg7Catalog catalog, boolean block) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * Gets the receipt with this identifier
   * 
   * @param id
   *          The ID of the receipt
   * @return The receipt, or null if none is found.
   */
  public Receipt getReceipt(String id) {
    return receiptService.getReceipt(id);
  }

  /**
   * Sets the workspace
   * 
   * @param workspace
   *          an instance of the workspace
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * Sets the receipt service
   * 
   * @param receiptService
   *          the receipt service
   */
  public void setReceiptService(ReceiptService receiptService) {
    this.receiptService = receiptService;
  }

  /**
   * Sets the trusted http client which is used for authenticated service distribution.
   * 
   * @param trustedHttpClient
   *          the trusted http client
   */
  public void setTrustedHttpClient(TrustedHttpClient trustedHttpClient) {
    this.trustedHttpClient = trustedHttpClient;
  }

}
