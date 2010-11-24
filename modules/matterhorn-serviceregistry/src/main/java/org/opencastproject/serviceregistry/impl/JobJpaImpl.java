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

import org.opencastproject.job.api.JaxbJob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A long running, asynchronously executed job. This concrete implementations adds JPA annotations to {@link JaxbJob}.
 */
@Entity(name = "Job")
@Access(AccessType.PROPERTY)
@Table(name = "JOB")
@NamedQueries({
        // Job queries
        @NamedQuery(name = "Job", query = "SELECT j FROM Job j "
                + "where j.status = :status and j.serviceRegistration.serviceType = :serviceType"),
        @NamedQuery(name = "Job.type", query = "SELECT j FROM Job j "
                + "where j.serviceRegistration.serviceType = :serviceType"),
        @NamedQuery(name = "Job.status", query = "SELECT j FROM Job j " + "where j.status = :status "),
        @NamedQuery(name = "Job.all", query = "SELECT j FROM Job j"),
        // Job count queries
        @NamedQuery(name = "Job.count", query = "SELECT COUNT(j) FROM Job j "
                + "where j.status = :status and j.serviceRegistration.serviceType = :serviceType"),
        @NamedQuery(name = "Job.count.type", query = "SELECT COUNT(j) FROM Job j "
                + "where j.serviceRegistration.serviceType = :serviceType"),
        @NamedQuery(name = "Job.count.status", query = "SELECT COUNT(j) FROM Job j " + "where j.status = :status "),
        @NamedQuery(name = "Job.count.all", query = "SELECT COUNT(j) FROM Job j"),
        @NamedQuery(name = "Job.countByHost", query = "SELECT COUNT(j) FROM Job j "
                + "where j.status = :status and j.serviceRegistration.serviceType = :serviceType and "
                + "j.serviceRegistration.host = :host") }
)
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "job", namespace = "http://job.opencastproject.org/")
@XmlRootElement(name = "job", namespace = "http://job.opencastproject.org/")
public class JobJpaImpl extends JaxbJob {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(JobJpaImpl.class);

  /** Default constructor needed by jaxb and jpa */
  public JobJpaImpl() {
    super();
  }

  /**
   * Constructor with everything needed for a newly instantiated job, using a random ID and setting the status to
   * queued.
   */
  public JobJpaImpl(ServiceRegistrationJpaImpl serviceRegistration) {
    this();
    setStatus(Status.QUEUED);
    setDateCreated(new Date());
    setHost(serviceRegistration.getHost());
    setJobType(serviceRegistration.getServiceType());
    this.serviceRegistration = serviceRegistration;
  }

  /** Constructor with everything needed for a newly instantiated job, using a random ID. */
  public JobJpaImpl(Status status, ServiceRegistrationJpaImpl serviceRegistration) {
    this(serviceRegistration);
    setStatus(status);
    if (Status.RUNNING.equals(status)) {
      setDateStarted(getDateCreated());
    }
  }

  /** The service that produced this job */
  ServiceRegistrationJpaImpl serviceRegistration;

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getId()
   */
  @Id
  @GeneratedValue
  @XmlAttribute
  @Override
  public long getId() {
    return id;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getStatus()
   */
  @Column
  @XmlAttribute
  @Override
  public Status getStatus() {
    return status;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getType()
   */
  @Transient
  @XmlAttribute(name = "type")
  @Override
  public String getJobType() {
    return jobType;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getHost()
   */
  @Transient
  @XmlElement
  @Override
  public String getHost() {
    return host;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getDateCompleted()
   */
  @Column
  @Temporal(TemporalType.TIMESTAMP)
  @XmlElement
  @Override
  public Date getDateCompleted() {
    return dateCompleted;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getDateCreated()
   */
  @Column
  @Temporal(TemporalType.TIMESTAMP)
  @XmlElement
  @Override
  public Date getDateCreated() {
    return dateCreated;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getDateStarted()
   */
  @Column
  @Temporal(TemporalType.TIMESTAMP)
  @XmlElement
  @Override
  public Date getDateStarted() {
    return dateStarted;
  }

  /**
   * @return the queueTime
   */
  @Column
  public Long getQueueTime() {
    return queueTime;
  }

  /**
   * @return the runTime
   */
  @Column
  public Long getRunTime() {
    return runTime;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JaxbJob#getPayload()
   */
  @Lob
  @Column(name = "PAYLOAD")
  @XmlElement
  @Override
  public String getPayload() {
    return super.getPayload();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JaxbJob#setPayload(java.lang.String)
   */
  @Override
  public void setPayload(String payload) {
    super.setPayload(payload);
  }

  /**
   * @return the serviceRegistration
   */
  @ManyToOne
  public ServiceRegistrationJpaImpl getServiceRegistration() {
    return serviceRegistration;
  }

  /**
   * @param serviceRegistration
   *          the serviceRegistration to set
   */
  public void setServiceRegistration(ServiceRegistrationJpaImpl serviceRegistration) {
    this.serviceRegistration = serviceRegistration;
  }

  @PostLoad
  public void postLoad() {
    if (payload != null) {
      payload.getBytes(); // force the clob to load
    }
    if (serviceRegistration == null) {
      logger.warn("service registration is null");
    } else {
      super.host = serviceRegistration.getHost();
      super.jobType = serviceRegistration.getServiceType();
    }
  }
}
