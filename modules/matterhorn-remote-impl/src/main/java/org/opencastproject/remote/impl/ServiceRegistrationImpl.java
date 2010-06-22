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
package org.opencastproject.remote.impl;

import org.opencastproject.remote.api.ServiceRegistration;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * A record of a service that creates and manages receipts.
 */
@Entity
@Table(name="SERVICE_REGISTRATION")
public class ServiceRegistrationImpl implements ServiceRegistration {

  @Id
  @Column(name="HOST", nullable=false)
  protected String host;

  @Id
  @Column(name="JOB_TYPE", nullable=false)
  protected String receiptType;
  
  @Column(name="MAINTENANCE", nullable=false)
  protected boolean inMaintenanceMode;
  
  public ServiceRegistrationImpl() {}

  public ServiceRegistrationImpl(String host, String receiptType, boolean inMaintenanceMode) {
    this.host = host;
    this.receiptType = receiptType;
    this.inMaintenanceMode = inMaintenanceMode;
  }

  /**
   * @return the host
   */
  @Override
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
  @Override
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
   * @return the inMaintenanceMode
   */
  @Override
  public boolean isInMaintenanceMode() {
    return inMaintenanceMode;
  }

  /**
   * @param inMaintenanceMode the inMaintenanceMode to set
   */
  public void setInMaintenanceMode(boolean inMaintenanceMode) {
    this.inMaintenanceMode = inMaintenanceMode;
  }
}
