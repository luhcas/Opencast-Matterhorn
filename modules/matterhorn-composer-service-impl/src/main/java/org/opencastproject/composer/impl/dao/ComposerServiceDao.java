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
package org.opencastproject.composer.impl.dao;

import org.opencastproject.composer.api.Receipt;
import org.opencastproject.composer.api.Receipt.Status;

/**
 * Provides persistence support for {@link Receipt}s
 */
public interface ComposerServiceDao {
  /**
   * Create and store a new receipt in {@link Status#QUEUED}
   * @return
   */
  Receipt createReceipt();

  /**
   * Update the receipt in the database
   * @param receipt
   */
  void updateReceipt(Receipt receipt);

  /**
   * Gets a receipt by its ID
   * @param id
   * @return
   */
  Receipt getReceipt(String id);
  
  /**
   * Count the number of receipts in this {@link Status}
   * @param status
   * @return
   */
  long count(Status status);

  /**
   * Count the number of receipts in this {@link Status} on this host
   * @param status
   * @param host The server that created and will be handling the encoding job
   * @return
   */
  long count(Status status, String host);
}
