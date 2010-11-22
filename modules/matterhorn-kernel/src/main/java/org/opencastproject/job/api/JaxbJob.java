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

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

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
    this.payload = job.getPayload();
    this.host = job.getHost();
    this.id = job.getId();
    this.jobType = job.getJobType();
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
  protected Long queueTime = 0L;

  /** The run time is denormalized in the database to enable cross-platform date arithmetic in JPA queries */
  protected Long runTime = 0L;

  /** The output produced by this job, or null if it has not yet been generated (or was not due to an exception) */
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
   * @see org.opencastproject.job.api.Job#getPayload()
   */
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
  
  @XmlAnyElement(lax=true)
  public Element getPayloadAsDom() throws IOException, ParserConfigurationException, SAXException {
    if(payload == null) return null;
    final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    final DocumentBuilder builder = factory.newDocumentBuilder();
    final Document doc = builder.parse(IOUtils.toInputStream(payload, "UTF-8"));
    return doc.getDocumentElement();
  }
  
  public void setPayloadAsDom(Element element) throws TransformerFactoryConfigurationError, TransformerException {
    if(element == null) return;
    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    StreamResult result = new StreamResult(new StringWriter());
    DOMSource source = new DOMSource(element);
    transformer.transform(source, result);
    payload = result.getWriter().toString();
  }

  
  /**
   * {@inheritDoc}
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Job) {
      return ((Job)obj).getId() == id;
    }
    return false;
  }

  /**
   * {@inheritDoc}
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return (int)id >> 32;
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
