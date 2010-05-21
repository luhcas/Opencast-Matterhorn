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
package org.opencastproject.receipt.impl;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * A record of a service that creates and manages receipts.
 */
@Entity
public class ReceiptHandler {

  @Id
  @GeneratedValue
  protected Long id;
  
  @Column
  protected String host;
  @Column
  protected String receiptType;
  
  public ReceiptHandler() {}

  public ReceiptHandler(String host, String receiptType) {
    this.host = host;
    this.receiptType = receiptType;
  }

  /**
   * @return the host
   */
  public String getHost() {
    return host;
  }
  /**
   * @param host the host to set
   */
  public void setHost(String host) {
    this.host = host;
  }
  /**
   * @return the receiptType
   */
  public String getReceiptType() {
    return receiptType;
  }
  /**
   * @param receiptType the receiptType to set
   */
  public void setReceiptType(String receiptType) {
    this.receiptType = receiptType;
  }

  /**
   * @return the id
   */
  public Long getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public void setId(Long id) {
    this.id = id;
  }

  
}
