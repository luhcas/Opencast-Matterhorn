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

import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.AbstractMediaPackageElement;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.Track;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;

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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * A long running, asynchronously executed job.
 */
@Entity(name = "Job")
@Access(AccessType.PROPERTY)
@Table(name = "JOB")
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "job", namespace = "http://remote.opencastproject.org/")
@NamedQueries( {
        @NamedQuery(name = "Job.count", query = "SELECT COUNT(j) FROM Job j "
                + "where j.status = :status and j.serviceRegistration.serviceType = :serviceType"),
        @NamedQuery(name = "Job.countByHost", query = "SELECT COUNT(j) FROM Job j "
                + "where j.status = :status and j.serviceRegistration.serviceType = :serviceType and "
                + "j.serviceRegistration.host = :host") })
public class JobImpl implements Job {

  /** Default constructor needed by jaxb and jpa */
  public JobImpl() {
  }

  /**
   * Constructor with everything needed for a newly instantiated job, using a random ID and setting the status to
   * queued.
   */
  public JobImpl(ServiceRegistrationImpl serviceRegistration) {
    this();
    setId(UUID.randomUUID().toString());
    setStatus(Status.QUEUED);
    setDateCreated(new Date());
    this.serviceRegistration = serviceRegistration;
  }

  /** Constructor with everything needed for a newly instantiated job, using a random ID. */
  public JobImpl(Status status, ServiceRegistrationImpl serviceRegistration) {
    this(serviceRegistration);
    setStatus(status);
  }

  /** The job ID */
  String id;

  /** The service that produced this job */
  @ManyToOne
  ServiceRegistrationImpl serviceRegistration;

  /** The job status */
  Status status;

  /** The date this job was created */
  Date dateCreated;

  /** The date this job was started */
  Date dateStarted;

  /** The date this job was completed */
  Date dateCompleted;

  /** The queue time is denormalized in the database to enable cross-platform date arithmetic in JPA queries */
  Long queueTime;

  /** The run time is denormalized in the database to enable cross-platform date arithmetic in JPA queries */
  Long runTime;

  /** The job's context. Not currently utilized. See http://opencast.jira.com/browse/MH-4492 */
  // String context;

  /** The element produced by this job, or null if it has not yet been generated (or was not due to an exception) */
  MediaPackageElement element;

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
   * @see org.opencastproject.job.api.Job#setId(java.lang.String)
   */
  @Override
  public void setId(String id) {
    this.id = id;
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
    return serviceRegistration.getServiceType();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getHost()
   */
  @XmlElement
  @Override
  public String getHost() {
    return serviceRegistration.getHost();
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
   * @param queueTime
   *          the queueTime to set
   */
  public void setQueueTime(Long queueTime) {
    this.queueTime = queueTime;
  }

  /**
   * @return the runTime
   */
  @Column
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

  public void setTrack(Track track) {
    this.element = track;
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

  public void setAttachment(Attachment attachment) {
    this.element = attachment;
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

  public void setCatalog(Catalog catalog) {
    this.element = catalog;
  }

  @Lob
  @Column(name = "ELEMENT_XML")
  public String getElementAsXml() throws Exception {
    if (element == null)
      return null;
    return ((AbstractMediaPackageElement) element).getAsXml();
  }

  public void setElementAsXml(String xml) throws Exception {
    if (xml == null) {
      element = null;
    } else {
      DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document doc = docBuilder.parse(IOUtils.toInputStream(xml, "UTF-8"));
      element = MediaPackageElementBuilderFactory.newInstance().newElementBuilder().elementFromManifest(
              doc.getDocumentElement(), new DefaultMediaPackageSerializerImpl());
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#toXml()
   */
  @Override
  public String toXml() {
    try {
      return JobBuilder.getInstance().toXml(this);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return the serviceRegistration
   */
  public ServiceRegistrationImpl getServiceRegistration() {
    return serviceRegistration;
  }

  /**
   * @param serviceRegistration
   *          the serviceRegistration to set
   */
  public void setServiceRegistration(ServiceRegistrationImpl serviceRegistration) {
    this.serviceRegistration = serviceRegistration;
  }
  
  /**
   * {@inheritDoc}
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "Job {" + this.id + "}";
  }
}
