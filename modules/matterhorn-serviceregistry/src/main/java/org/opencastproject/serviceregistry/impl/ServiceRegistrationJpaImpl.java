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
package org.opencastproject.serviceregistry.impl;

import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.opencastproject.serviceregistry.api.JaxbServiceRegistration;

/**
 * A record of a service that creates and manages receipts.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "service", namespace = "http://serviceregistry.opencastproject.org")
@XmlRootElement(name = "service", namespace = "http://serviceregistry.opencastproject.org")
@Entity(name = "ServiceRegistration")
@Access(AccessType.PROPERTY)
@Table(name = "SERVICE_REGISTRATION")
@NamedQueries({
        @NamedQuery(name = "ServiceRegistration.statistics", query = "SELECT sr, job.status, "
                + "count(job.status) as numJobs, " + "avg(job.queueTime) as meanQueue, "
                + "avg(job.runTime) as meanRun " + "FROM ServiceRegistration sr LEFT OUTER JOIN sr.jobs job "
                + "group by sr, job.status"),
        @NamedQuery(name = "ServiceRegistration.getRegistration", query = "SELECT r from ServiceRegistration r "
                + "where r.host = :host and r.serviceType = :serviceType"),
        @NamedQuery(name = "ServiceRegistration.getAll", query = "SELECT rh FROM ServiceRegistration rh"),
        @NamedQuery(name = "ServiceRegistration.getByHost", query = "SELECT rh FROM ServiceRegistration rh "
                + "where rh.host=:host"),
        @NamedQuery(name = "ServiceRegistration.getByType", query = "SELECT rh FROM ServiceRegistration rh "
                + "where rh.serviceType=:serviceType") })
public class ServiceRegistrationJpaImpl extends JaxbServiceRegistration {

  protected Set<JobJpaImpl> jobs;

  /**
   * Creates a new service registration which is online and not in maintenance mode.
   */
  public ServiceRegistrationJpaImpl() {
    super();
  }

  /**
   * Creates a new service registration which is online and not in maintenance mode.
   * 
   * @param host
   *          the host
   * @param serviceId
   *          the job type
   */
  public ServiceRegistrationJpaImpl(String serviceType, String host, String path) {
    super(serviceType, host, path);
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
  public ServiceRegistrationJpaImpl(String serviceType, String host, String path, boolean jobProducer) {
    super(serviceType, host, path, jobProducer);
  }

  @Id
  @Column(name = "SERVICE_TYPE", nullable = false)
  @XmlElement(name = "type")
  @Override
  public String getServiceType() {
    return super.getServiceType();
  }

  @Id
  @Column(name = "HOST", nullable = false)
  @XmlElement(name = "host")
  @Override
  public String getHost() {
    return super.getHost();
  }

  @Column(name = "PATH", nullable = false)
  @XmlElement(name = "path")
  @Override
  public String getPath() {
    return super.getPath();
  }

  @Column(name = "ONLINE", nullable = false)
  @XmlElement(name = "online")
  @Override
  public boolean isOnline() {
    return super.isOnline();
  }

  @Column(name = "MAINTENANCE", nullable = false)
  @XmlElement(name = "maintenance")
  @Override
  public boolean isInMaintenanceMode() {
    return super.isInMaintenanceMode();
  }

  @Column(name = "JOB_PRODUCER", nullable = false)
  @XmlElement(name = "jobproducer")
  @Override
  public boolean isJobProducer() {
    return super.isJobProducer();
  }

  @OneToMany(mappedBy = "serviceRegistration")
  @JoinColumns({ @JoinColumn(name = "host", referencedColumnName = "host"),
          @JoinColumn(name = "service_type", referencedColumnName = "service_type") })
  public Set<JobJpaImpl> getJobs() {
    return jobs;
  }

  public void setJobs(Set<JobJpaImpl> jobs) {
    this.jobs = jobs;
  }
}
