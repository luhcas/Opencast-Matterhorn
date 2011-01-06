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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OrderColumn;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;
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
                + "where j.status = :status and j.creatorServiceRegistration.serviceType = :serviceType"),
        @NamedQuery(name = "Job.type", query = "SELECT j FROM Job j "
                + "where j.creatorServiceRegistration.serviceType = :serviceType"),
        @NamedQuery(name = "Job.status", query = "SELECT j FROM Job j " + "where j.status = :status "),
        @NamedQuery(name = "Job.all", query = "SELECT j FROM Job j"),
        // Job count queries
        @NamedQuery(name = "Job.count", query = "SELECT COUNT(j) FROM Job j "
                + "where j.status = :status and j.creatorServiceRegistration.serviceType = :serviceType"),
        @NamedQuery(name = "Job.count.type", query = "SELECT COUNT(j) FROM Job j "
                + "where j.creatorServiceRegistration.serviceType = :serviceType"),
        @NamedQuery(name = "Job.count.status", query = "SELECT COUNT(j) FROM Job j " + "where j.status = :status "),
        @NamedQuery(name = "Job.count.all", query = "SELECT COUNT(j) FROM Job j"),
        @NamedQuery(name = "Job.countByHost", query = "SELECT COUNT(j) FROM Job j "
                + "where j.status = :status and j.processorServiceRegistration is not null and "
                + "j.processorServiceRegistration.serviceType = :serviceType and "
                + "j.creatorServiceRegistration.hostRegistration.baseUrl = :host") })
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
  public JobJpaImpl(ServiceRegistrationJpaImpl creatorServiceRegistration, String operation, List<String> arguments,
          boolean startImmediately) {
    this();
    this.operationType = operation;
    if(arguments != null) {
      this.arguments = new ArrayList<String>(arguments);
    }
    setDateCreated(new Date());
    setCreatedHost(creatorServiceRegistration.getHost());
    setJobType(creatorServiceRegistration.getServiceType());
    this.creatorServiceRegistration = creatorServiceRegistration;
    if (startImmediately) {
      this.processorServiceRegistration = creatorServiceRegistration;
      setDateStarted(getDateCreated());
      setStatus(Status.RUNNING);
    } else {
      setStatus(Status.QUEUED);
    }

  }

  /** The service that produced this job */
  protected ServiceRegistrationJpaImpl creatorServiceRegistration;

  /** The service that is processing, or processed, this job */
  protected ServiceRegistrationJpaImpl processorServiceRegistration;

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
   * @see org.opencastproject.job.api.JaxbJob#getVersion()
   */
  @Column
  @Version
  @Override
  public int getVersion() {
    return version;
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
   * @see org.opencastproject.job.api.JaxbJob#getOperationType()
   */
  @Column(name = "operation")
  @XmlAttribute
  @Override
  public String getOperationType() {
    return operationType;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JaxbJob#getArguments()
   */
  @Lob
  @Column(name = "argument")
  @OrderColumn(name = "index")
  @ElementCollection
  @CollectionTable(name = "JOB_ARG", joinColumns = @JoinColumn(name = "ID", referencedColumnName = "ID"))
  @Override
  public List<String> getArguments() {
    return arguments;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getProcessingHost()
   */
  @Transient
  @XmlElement
  @Override
  public String getProcessingHost() {
    return processingHost;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JaxbJob#getCreatedHost()
   */
  @Transient
  @XmlElement
  @Override
  public String getCreatedHost() {
    return createdHost;
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
   * @return the serviceRegistration where this job was created
   */
  @ManyToOne
  @JoinColumns({ @JoinColumn(name = "CREATOR_SVC_TYPE", referencedColumnName = "SERVICE_TYPE", updatable = false),
          @JoinColumn(name = "CREATOR_HOST", referencedColumnName = "HOST", updatable = false) })
  public ServiceRegistrationJpaImpl getCreatorServiceRegistration() {
    return creatorServiceRegistration;
  }

  /**
   * @param serviceRegistration
   *          the serviceRegistration to set
   */
  public void setCreatorServiceRegistration(ServiceRegistrationJpaImpl serviceRegistration) {
    this.creatorServiceRegistration = serviceRegistration;
  }

  /**
   * @return the processorServiceRegistration
   */
  @ManyToOne
  @JoinColumns({ @JoinColumn(name = "PROCESSOR_SVC_TYPE", referencedColumnName = "SERVICE_TYPE", updatable = false),
          @JoinColumn(name = "PROCESSOR_HOST", referencedColumnName = "HOST", updatable = false) })
  public ServiceRegistrationJpaImpl getProcessorServiceRegistration() {
    return processorServiceRegistration;
  }

  /**
   * @param processorServiceRegistration
   *          the processorServiceRegistration to set
   */
  public void setProcessorServiceRegistration(ServiceRegistrationJpaImpl processorServiceRegistration) {
    this.processorServiceRegistration = processorServiceRegistration;
  }

  @PostLoad
  public void postLoad() {
    if (payload != null) {
      payload.getBytes(); // force the clob to load
    }
    if (creatorServiceRegistration == null) {
      logger.warn("creator service registration is null");
    } else {
      super.createdHost = creatorServiceRegistration.getHost();
      super.jobType = creatorServiceRegistration.getServiceType();
    }
    if (processorServiceRegistration == null) {
      logger.debug("processor service registration is null");
    } else {
      super.processingHost = creatorServiceRegistration.getHost();
      super.jobType = creatorServiceRegistration.getServiceType();
    }
  }
}
