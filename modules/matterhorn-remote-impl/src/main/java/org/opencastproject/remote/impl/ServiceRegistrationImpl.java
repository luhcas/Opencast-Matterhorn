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

import java.util.Collection;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * A record of a service that creates and manages receipts.
 */
@Entity(name = "ServiceRegistration")
@Table(name = "SERVICE_REGISTRATION")
@NamedQueries( { @NamedQuery(name = "ServiceRegistration.statistics", query = "SELECT sr, job.status, "
        + "count(job.status) as numJobs, "
        + "avg(job.queueTime) as meanQueue, "
        + "avg(job.runTime) as meanRun "
        + "FROM ServiceRegistration sr LEFT OUTER JOIN sr.jobs job " + "group by sr, job.status") })
public class ServiceRegistrationImpl implements ServiceRegistration {

  @Id
  @Column(name = "HOST", nullable = false)
  protected String host;

  @Id
  @Column(name = "JOB_TYPE", nullable = false)
  protected String jobType;

  @Column(name = "ONLINE", nullable = false)
  protected boolean online;

  @Column(name = "MAINTENANCE", nullable = false)
  protected boolean maintenanceMode;

  @OneToMany
  @JoinColumns( { @JoinColumn(name = "host", referencedColumnName = "host"),
          @JoinColumn(name = "job_type", referencedColumnName = "job_type") })
  protected Collection<JobImpl> jobs;

  /**
   * Creates a new service registration which is online and not in maintenance mode.
   */
  public ServiceRegistrationImpl() {
    this.online = true;
    this.maintenanceMode = false;
  }

  /**
   * Creates a new service registration which is online and not in maintenance mode.
   * 
   * @param host
   *          the host
   * @param receiptType
   *          the job type
   */
  public ServiceRegistrationImpl(String host, String receiptType) {
    this();
    this.host = host;
    this.jobType = receiptType;
  }

  /**
   * @return the host
   */
  @Override
  public String getHost() {
    return host;
  }

  /**
   * @param host
   *          the host to set
   */
  public void setHost(String host) {
    this.host = host;
  }

  /**
   * @return the receiptType
   */
  @Override
  public String getJobType() {
    return jobType;
  }

  /**
   * @param receiptType
   *          the receiptType to set
   */
  public void setReceiptType(String receiptType) {
    this.jobType = receiptType;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.remote.api.ServiceRegistration#isInMaintenanceMode()
   */
  @Override
  public boolean isInMaintenanceMode() {
    return maintenanceMode;
  }

  /**
   * Sets the maintenance status of this service registration
   * 
   * @param maintenanceMode
   */
  public void setMaintenanceMode(boolean maintenanceMode) {
    this.maintenanceMode = maintenanceMode;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.remote.api.ServiceRegistration#isOnline()
   */
  @Override
  public boolean isOnline() {
    return online;
  }

  /**
   * Sets the online status of this service registration
   * 
   * @param online
   */
  public void setOnline(boolean online) {
    this.online = online;
  }
}
