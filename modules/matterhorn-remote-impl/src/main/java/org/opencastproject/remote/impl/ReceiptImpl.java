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

import org.opencastproject.mediapackage.AbstractMediaPackageElement;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.remote.api.Receipt;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;

import java.util.Date;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
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
 * A receipt for a long running, asynchronously executed job.
 */
@Entity(name="Receipt")
@Access(AccessType.PROPERTY)
@Table(name="JOB")
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="receipt", namespace="http://receipt.opencastproject.org/")
public class ReceiptImpl implements Receipt {

  /** Default constructor needed by jaxb and jpa */
  public ReceiptImpl() {}

  /** Constructor with everything needed for a newly instantiated receipt. */
  public ReceiptImpl(String id, Status status, String type, String host, String context) {
    this();
    setId(id);
    setStatus(status);
    setType(type);
    setHost(host);
  }

  /** The receipt ID */
  String id;
  
  /** The receipt type */
  String type;
  
  /** The receipt status */
  Status status;

  /** The host responsible for this receipt */
  String host;

  /** The date this receipt was created */
  Date dateCreated;
  
  /** The date this receipt was started */
  Date dateStarted;

  /** The date this receipt was completed */
  Date dateCompleted;

  /** The receipt's context.  Not currently utilized.  See http://opencast.jira.com/browse/MH-4492 */
//  String context;

  /** The element produced by this job, or null if it has not yet been generated (or was not due to an exception) */
  MediaPackageElement element;

  /**
   * {@inheritDoc}
   * @see org.opencastproject.remote.api.Receipt#getId()
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
   * @see org.opencastproject.remote.api.Receipt#setId(java.lang.String)
   */
  @Override
  public void setId(String id) {
    this.id = id;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.remote.api.Receipt#getStatus()
   */
  @Column
  @XmlAttribute
  @Override
  public Status getStatus() {
    return status;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.remote.api.Receipt#setStatus(org.opencastproject.remote.api.Receipt.Status)
   */
  @Override
  public void setStatus(Status status) {
    Date now = new Date();
    if(Status.QUEUED.equals(status)) {
      this.dateCreated = now;
    } else if(Status.RUNNING.equals(status)) {
      this.dateStarted = now;
    } else if(Status.FAILED.equals(status)) {
      this.dateCompleted = now;
    } else if(Status.FINISHED.equals(status)) {
      this.dateCompleted = now;
    }
    this.status = status;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.remote.api.Receipt#getType()
   */
  @Column
  @XmlAttribute
  @Override
  public String getType() {
    return type;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.remote.api.Receipt#setType(java.lang.String)
   */
  @Override
  public void setType(String type) {
    this.type = type;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.remote.api.Receipt#getHost()
   */
  @Column
  @XmlElement
  @Override
  public String getHost() {
    return host;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.remote.api.Receipt#setHost(java.lang.String)
   */
  @Override
  public void setHost(String host) {
    this.host = host;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.remote.api.Receipt#getDateCompleted()
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
   * @see org.opencastproject.remote.api.Receipt#getDateCreated()
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
   * @see org.opencastproject.remote.api.Receipt#getDateStarted()
   */
  @Column
  @Temporal(TemporalType.TIMESTAMP)
  @XmlElement
  @Override
  public Date getDateStarted() {
    return dateStarted;
  }
  
  /**
   * @param dateCreated the dateCreated to set
   */
  public void setDateCreated(Date dateCreated) {
    this.dateCreated = dateCreated;
  }

  /**
   * @param dateStarted the dateStarted to set
   */
  public void setDateStarted(Date dateStarted) {
    this.dateStarted = dateStarted;
  }

  /**
   * @param dateCompleted the dateCompleted to set
   */
  public void setDateCompleted(Date dateCompleted) {
    this.dateCompleted = dateCompleted;
  }

//  /**
//   * {@inheritDoc}
//   * @see org.opencastproject.remote.api.Receipt#getContext()
//   */
//  @Override
//  public String getContext() {
//    return context;
//  }
//  
//  /**
//   * {@inheritDoc}
//   * @see org.opencastproject.remote.api.Receipt#setContext(java.lang.String)
//   */
//  @Override
//  public void setContext(String context) {
//    this.context = context;
//  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.remote.api.Receipt#getElement()
   */
  @Transient
  @Override
  public MediaPackageElement getElement() {
    return element;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.remote.api.Receipt#setElement(org.opencastproject.mediapackage.MediaPackageElement)
   */
  @Transient
  @Override
  public void setElement(MediaPackageElement element) {
    this.element = element;
  }

  @Transient
  @XmlElement(name="track")
  public Track getTrack() {
    if(element != null && element instanceof Track) {
      return (Track)element;
    } else {
      return null;
    }
  }
  public void setTrack(Track track) {
    this.element = track;
  }

  @Transient
  @XmlElement(name="attachment")
  public Attachment getAttachment() {
    if(element != null && element instanceof Attachment) {
      return (Attachment)element;
    } else {
      return null;
    }
  }
  public void setAttachment(Attachment attachment) {
    this.element = attachment;
  }

  @Transient
  @XmlElement(name="catalog")
  public Catalog getCatalog() {
    if(element != null && element instanceof Catalog) {
      return (Catalog)element;
    } else {
      return null;
    }
  }
  public void setCatalog(Catalog catalog) {
    this.element = catalog;
  }

  @Lob
  @Column(name="ELEMENT_XML")
  public String getElementAsXml() throws Exception {
    if(element == null) return null;
    return ((AbstractMediaPackageElement)element).getAsXml();
  }

  public void setElementAsXml(String xml) throws Exception {
    if(xml == null) {
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
   * @see org.opencastproject.remote.api.Receipt#toXml()
   */
  @Override
  public String toXml() {
    try {
      return ReceiptBuilder.getInstance().toXml(this);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
