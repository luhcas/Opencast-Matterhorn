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
package org.opencastproject.job.api;

import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A long running, asynchronously executed job.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "job", namespace = "http://job.opencastproject.org/")
@XmlRootElement(name = "job", namespace = "http://job.opencastproject.org/")
public class JaxbJob implements Job {

  /** Default constructor needed by jaxb */
  public JaxbJob() {
  }

  /**
   * Constructs a JaxbJob from an existing job
   * 
   * @param job
   *          the job to use as a template for constructing this JaxbJob
   */
  public JaxbJob(Job job) {
    this();
    this.dateCompleted = job.getDateCompleted();
    this.dateCreated = job.getDateCreated();
    this.dateStarted = job.getDateStarted();
    this.payload = job.getPayload();
    this.processingHost = job.getProcessingHost();
    this.createdHost = job.getCreatedHost();
    this.id = job.getId();
    this.jobType = job.getJobType();
    this.operationType = job.getOperationType();
    this.arguments = job.getArguments();
    this.status = job.getStatus();
    if (this.dateCreated != null && this.dateStarted != null) {
      this.queueTime = this.dateStarted.getTime() - this.dateCreated.getTime();
    }
    if (this.dateStarted != null && this.dateCompleted != null) {
      this.runTime = this.dateCompleted.getTime() - this.dateStarted.getTime();
    }
  }

  /** The job ID */
  protected long id;

  /** The version, used for optimistic locking */
  protected int version;

  /** The job type */
  protected String jobType;

  /** The operation type */
  protected String operationType;

  /** The arguments passed to the service operation */
  protected List<String> arguments;

  /** The server that created this job. */
  protected String createdHost;

  /** The server that is or was processing this job. Null if the job has not yet started. */
  protected String processingHost;

  /** The job status */
  protected Status status;

  /** The date this job was created */
  protected Date dateCreated;

  /** The date this job was started */
  protected Date dateStarted;

  /** The date this job was completed */
  protected Date dateCompleted;

  /** The queue time is denormalized in the database to enable cross-platform date arithmetic in JPA queries */
  protected Long queueTime = 0L;

  /** The run time is denormalized in the database to enable cross-platform date arithmetic in JPA queries */
  protected Long runTime = 0L;

  /** The output produced by this job, or null if it has not yet been generated (or was not due to an exception) */
  // @XmlJavaTypeAdapter(value = CdataAdapter.class)
  protected String payload;

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getId()
   */
  @XmlAttribute
  @Override
  public long getId() {
    return id;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getVersion()
   */
  @XmlAttribute
  @Override
  public int getVersion() {
    return version;
  }

  /**
   * @param version
   *          the version to set
   */
  public void setVersion(int version) {
    this.version = version;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#setId(long)
   */
  @Override
  public void setId(long id) {
    this.id = id;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getStatus()
   */
  @XmlAttribute
  @Override
  public Status getStatus() {
    return status;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#setStatus(org.opencastproject.job.api.Job.Status)
   */
  @Override
  public void setStatus(Status status) {
    this.status = status;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getType()
   */
  @XmlAttribute(name = "type")
  @Override
  public String getJobType() {
    return jobType;
  }

  /**
   * Sets the job type
   * 
   * @param jobType
   *          the job type
   */
  public void setJobType(String jobType) {
    this.jobType = jobType;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getOperationType()
   */
  @XmlElement
  @Override
  public String getOperationType() {
    return operationType;
  }

  /**
   * @param operationType
   *          the operationType to set
   */
  public void setOperationType(String operationType) {
    this.operationType = operationType;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getArguments()
   */
  @XmlElement
  @Override
  public List<String> getArguments() {
    return arguments;
  }

  /**
   * @param arguments
   *          the arguments to set
   */
  public void setArguments(List<String> arguments) {
    this.arguments = arguments;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getProcessingHost()
   */
  @XmlElement
  @Override
  public String getProcessingHost() {
    return processingHost;
  }

  /**
   * Sets the host url
   * 
   * @param processingHost
   *          the host's base URL
   */
  public void setProcessingHost(String processingHost) {
    this.processingHost = processingHost;
  }

  /**
   * @param createdHost
   *          the createdHost to set
   */
  public void setCreatedHost(String createdHost) {
    this.createdHost = createdHost;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getCreatedHost()
   */
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
  @XmlElement
  @Override
  public Date getDateStarted() {
    return dateStarted;
  }

  /**
   * @return the queueTime
   */
  public Long getQueueTime() {
    return queueTime;
  }

  /**
   * @param queueTime
   *          the queueTime to set
   */
  public void setQueueTime(Long queueTime) {
    this.queueTime = queueTime;
  }

  /**
   * @return the runTime
   */
  public Long getRunTime() {
    return runTime;
  }

  /**
   * @param runTime
   *          the runTime to set
   */
  public void setRunTime(Long runTime) {
    this.runTime = runTime;
  }

  /**
   * @param dateCreated
   *          the dateCreated to set
   */
  public void setDateCreated(Date dateCreated) {
    this.dateCreated = dateCreated;
  }

  /**
   * @param dateStarted
   *          the dateStarted to set
   */
  public void setDateStarted(Date dateStarted) {
    this.dateStarted = dateStarted;
  }

  /**
   * @param dateCompleted
   *          the dateCompleted to set
   */
  public void setDateCompleted(Date dateCompleted) {
    this.dateCompleted = dateCompleted;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getPayload()
   */
  @XmlElement
  @Override
  public String getPayload() {
    return payload;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#setPayload(java.lang.String)
   */
  @Override
  public void setPayload(String payload) {
    this.payload = payload;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Job) {
      return ((Job) obj).getId() == id;
    }
    return false;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return (int) id >> 32;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "Job {" + this.id + "}";
  }
}
