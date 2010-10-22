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

import java.util.Date;
import java.util.UUID;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
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
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.mediapackage.AbstractMediaPackageElement;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.Track;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A long running, asynchronously executed job. This concrete implementations adds JPA annotations to {@link JaxbJob}.
 */
@Entity(name = "Job")
@Access(AccessType.PROPERTY)
@Table(name = "JOB")
@NamedQueries({
        @NamedQuery(name = "Job.count", query = "SELECT COUNT(j) FROM Job j "
                + "where j.status = :status and j.serviceRegistration.serviceType = :serviceType"),
        @NamedQuery(name = "Job.countByHost", query = "SELECT COUNT(j) FROM Job j "
                + "where j.status = :status and j.serviceRegistration.serviceType = :serviceType and "
                + "j.serviceRegistration.host = :host") })
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
    setId(UUID.randomUUID().toString());
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
    if(Status.RUNNING.equals(status)) {
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
  @XmlID
  @XmlAttribute
  @Override
  public String getId() {
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
   * @see org.opencastproject.job.api.Job#getElement()
   */
  @Transient
  @Override
  public MediaPackageElement getElement() {
    return element;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#setElement(org.opencastproject.mediapackage.MediaPackageElement)
   */
  @Transient
  @Override
  public void setElement(MediaPackageElement element) {
    this.element = element;
  }

  @Transient
  @XmlElement(name = "track")
  public Track getTrack() {
    if (element != null && element instanceof Track) {
      return (Track) element;
    } else {
      return null;
    }
  }

  @Transient
  @XmlElement(name = "attachment")
  public Attachment getAttachment() {
    if (element != null && element instanceof Attachment) {
      return (Attachment) element;
    } else {
      return null;
    }
  }

  @Transient
  @XmlElement(name = "catalog")
  public Catalog getCatalog() {
    if (element != null && element instanceof Catalog) {
      return (Catalog) element;
    } else {
      return null;
    }
  }

  @Lob
  @Column(name = "ELEMENT_XML")
  public String getElementAsXml() throws Exception {
    if (element == null)
      return null;
    return ((AbstractMediaPackageElement) element).getAsXml();
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
    if(serviceRegistration == null) {
      logger.warn("service registration is null");
    } else {
      super.host = serviceRegistration.getHost();
      super.jobType = serviceRegistration.getServiceType();
    }
  }
}
