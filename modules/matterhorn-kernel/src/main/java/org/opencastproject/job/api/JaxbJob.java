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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;
import org.opencastproject.mediapackage.AbstractMediaPackageElement;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.Track;
import org.w3c.dom.Document;

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
   * @param job the job to use as a template for constructing this JaxbJob
   */
  public JaxbJob(Job job) {
    this();
    this.dateCompleted = job.getDateCompleted();
    this.dateCreated = job.getDateCreated();
    this.dateStarted = job.getDateStarted();
    this.element = job.getElement();
    this.host = job.getHost();
    this.id = job.getId();
    this.jobType = job.getJobType();
    if (this.dateCreated != null && this.dateStarted != null) {
      this.queueTime = this.dateStarted.getTime() - this.dateCompleted.getTime();
    }
    if (this.dateStarted != null && this.dateCompleted != null) {
      this.runTime = this.dateCompleted.getTime() - this.dateStarted.getTime();
    }
  }

  /** The job ID */
  protected String id;

  /** The job type */
  protected String jobType;

  /** The host */
  protected String host;

  /** The job status */
  protected Status status;

  /** The date this job was created */
  protected Date dateCreated;

  /** The date this job was started */
  protected Date dateStarted;

  /** The date this job was completed */
  protected Date dateCompleted;

  /** The queue time is denormalized in the database to enable cross-platform date arithmetic in JPA queries */
  protected Long queueTime;

  /** The run time is denormalized in the database to enable cross-platform date arithmetic in JPA queries */
  protected Long runTime;

  /** The element produced by this job, or null if it has not yet been generated (or was not due to an exception) */
  protected MediaPackageElement element;

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#getId()
   */
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
   * @see org.opencastproject.job.api.Job#getHost()
   */
  @XmlElement
  @Override
  public String getHost() {
    return host;
  }

  /**
   * Sets the host url
   * 
   * @param host
   *          the host's base URL
   */
  public void setHost(String host) {
    this.host = host;
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
   * @see org.opencastproject.job.api.Job#getElement()
   */
  @Override
  public MediaPackageElement getElement() {
    return element;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.Job#setElement(org.opencastproject.mediapackage.MediaPackageElement)
   */
  @Override
  public void setElement(MediaPackageElement element) {
    this.element = element;
  }

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
      element = MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
              .elementFromManifest(doc.getDocumentElement(), new DefaultMediaPackageSerializerImpl());
    }
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
