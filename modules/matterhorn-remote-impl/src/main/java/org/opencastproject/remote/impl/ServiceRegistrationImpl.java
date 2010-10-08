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
@NamedQueries( {
        @NamedQuery(name = "ServiceRegistration.statistics", query = "SELECT sr, job.status, "
                + "count(job.status) as numJobs, " + "avg(job.queueTime) as meanQueue, "
                + "avg(job.runTime) as meanRun " + "FROM ServiceRegistration sr LEFT OUTER JOIN sr.jobs job "
                + "group by sr, job.status"),
        @NamedQuery(name = "ServiceRegistration.getRegistration", query = "SELECT r from ServiceRegistration r "
                + "where r.host = :host and r.serviceType = :serviceType"),
        @NamedQuery(name = "ServiceRegistration.getAll", query = "SELECT rh FROM ServiceRegistration rh") })
public class ServiceRegistrationImpl implements ServiceRegistration {

  @Id
  @Column(name = "SERVICE_TYPE", nullable = false)
  protected String serviceType;

  @Id
  @Column(name = "HOST", nullable = false)
  protected String host;

  @Id
  @Column(name = "PATH", nullable = false)
  protected String path;

  @Column(name = "ONLINE", nullable = false)
  protected boolean online;

  @Column(name = "MAINTENANCE", nullable = false)
  protected boolean maintenanceMode;

  @Column(name = "JOB_PRODUCER", nullable = false)
  protected boolean jobProducer;

  @OneToMany
  @JoinColumns( { @JoinColumn(name = "host", referencedColumnName = "host"),
          @JoinColumn(name = "service_type", referencedColumnName = "service_type") })
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
   * @param serviceId
   *          the job type
   */
  public ServiceRegistrationImpl(String serviceType, String host, String path) {
    this();
    this.serviceType = serviceType;
    this.host = host;
    this.path = path;
  }

  /**
   * Creates a new service registration which is online and not in maintenance mode.
   * 
   * @param host
   *          the host
   * @param serviceId
   *          the job type
   * @param jobProducer
   */
  public ServiceRegistrationImpl(String serviceType, String host, String path, boolean jobProducer) {
    this();
    this.serviceType = serviceType;
    this.host = host;
    this.path = path;
    this.jobProducer = jobProducer;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.remote.api.ServiceRegistration#getHost()
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
   * {@inheritDoc}
   * 
   * @see org.opencastproject.remote.api.ServiceRegistration#getServiceType()
   */
  @Override
  public String getServiceType() {
    return serviceType;
  }

  /**
   * @param serviceType
   *          the serviceType to set
   */
  public void setServiceType(String serviceType) {
    this.serviceType = serviceType;
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

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.remote.api.ServiceRegistration#getPath()
   */
  @Override
  public String getPath() {
    return path;
  }

  /**
   * @param path
   *          the path to set
   */
  public void setPath(String path) {
    this.path = path;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.remote.api.ServiceRegistration#isJobProducer()
   */
  @Override
  public boolean isJobProducer() {
    return jobProducer;
  }

  /**
   * Sets whether this service registration is a job producer.
   * 
   * @param jobProducer
   *          the jobProducer to set
   */
  public void setJobProducer(boolean jobProducer) {
    this.jobProducer = jobProducer;
  }
}
