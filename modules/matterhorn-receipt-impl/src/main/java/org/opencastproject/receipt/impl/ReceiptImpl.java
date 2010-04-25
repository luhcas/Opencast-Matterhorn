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
package org.opencastproject.receipt.impl;

import org.opencastproject.media.mediapackage.Attachment;
import org.opencastproject.media.mediapackage.Catalog;
import org.opencastproject.media.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.receipt.api.Receipt;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.StringWriter;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * A receipt for a long running, asynchronously executed job.
 */
@Entity(name="Receipt")
@Access(AccessType.PROPERTY)
@Table(name="MH_RECEIPT")
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="receipt", namespace="http://receipt.opencastproject.org/")
public class ReceiptImpl implements Receipt {
  public ReceiptImpl() {}

  public ReceiptImpl(String id, Status status, String type, String host, MediaPackageElement element) {
    this();
    this.id = id;
    this.status = status;
    this.type = type;
    this.host = host;
    this.element = element;
  }

  String id;
  
  String type;
  
  Status status;

  String host;

  MediaPackageElement element;

  /**
   * 
   * {@inheritDoc}
   * @see org.opencastproject.receipt.api.Receipt#getId()
   */
  @Id
  @XmlID
  @XmlAttribute
  @Override
  public String getId() {
    return id;
  }

  /**
   * 
   * {@inheritDoc}
   * @see org.opencastproject.receipt.api.Receipt#setId(java.lang.String)
   */
  @Override
  public void setId(String id) {
    this.id = id;
  }

  /**
   * 
   * {@inheritDoc}
   * @see org.opencastproject.receipt.api.Receipt#getStatus()
   */
  @Column
  @XmlAttribute
  @Override
  public Status getStatus() {
    return status;
  }

  /**
   * 
   * {@inheritDoc}
   * @see org.opencastproject.receipt.api.Receipt#setStatus(org.opencastproject.receipt.api.Receipt.Status)
   */
  @Override
  public void setStatus(Status status) {
    this.status = status;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.receipt.api.Receipt#getType()
   */
  @Column
  @XmlAttribute
  @Override
  public String getType() {
    return type;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.receipt.api.Receipt#setType(java.lang.String)
   */
  @Override
  public void setType(String type) {
    this.type = type;
  }

  /**
   * 
   * {@inheritDoc}
   * @see org.opencastproject.receipt.api.Receipt#getHost()
   */
  @Column
  @XmlElement
  @Override
  public String getHost() {
    return host;
  }

  @Override
  public void setHost(String host) {
    this.host = host;
  }

  /**
   * 
   * {@inheritDoc}
   * @see org.opencastproject.receipt.api.Receipt#getElement()
   */
  @Transient
  @Override
  public MediaPackageElement getElement() {
    return element;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.receipt.api.Receipt#setElement(org.opencastproject.media.mediapackage.MediaPackageElement)
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
  @Column(name="element")
  public String getElementAsXml() throws Exception {
    if(element == null) return null;
    DocumentBuilder docBuilder;
    docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document doc = docBuilder.newDocument();
    Node node = element.toManifest(doc, null);
    DOMSource domSource = new DOMSource(node);
    StringWriter writer = new StringWriter();
    StreamResult result = new StreamResult(writer);
    Transformer transformer;
    transformer = TransformerFactory.newInstance().newTransformer();
    transformer.transform(domSource, result);
    return writer.toString();
  }

  public void setElementAsXml(String xml) throws Exception {
    if(xml == null) return;
    DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document doc = docBuilder.parse(IOUtils.toInputStream(xml));
    element = MediaPackageElementBuilderFactory.newInstance().newElementBuilder().elementFromManifest(
            doc.getDocumentElement(), new DefaultMediaPackageSerializerImpl());
  }
}
