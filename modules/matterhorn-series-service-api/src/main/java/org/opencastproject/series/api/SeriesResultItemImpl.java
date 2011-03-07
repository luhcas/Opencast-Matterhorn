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
package org.opencastproject.series.api;

import java.util.Collection;
import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Implements {@link SeriesResultItem}.
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "result-item", namespace = "http://series.opencastproject.org/")
public class SeriesResultItemImpl implements SeriesResultItem {

  // fields that will be shown in search result
  @XmlAttribute(name = "id")
  private String id;
  @XmlElement(name = "title")
  private String title;
  @XmlElementWrapper(name = "subjects")
  @XmlElement(name = "subject")
  private Collection<String> subject;
  @XmlElementWrapper(name = "creators")
  @XmlElement(name = "creator")
  private Collection<String> creator;
  @XmlElementWrapper(name = "publishers")
  @XmlElement(name = "publisher")
  private Collection<String> publisher;
  @XmlElementWrapper(name = "contributors")
  @XmlElement(name = "contributor")
  private Collection<String> contributor;
  @XmlElementWrapper(name = "abstracts")
  @XmlElement(name = "abstract")
  private Collection<String> seriesAbstract;
  @XmlElementWrapper(name = "descriptions")
  @XmlElement(name = "description")
  private Collection<String> description;
  @XmlElement(name = "created")
  private Date created;
  @XmlElement(name = "available-from")
  private Date availableFrom;
  @XmlElement(name = "available-to")
  private Date availableTo;
  @XmlElementWrapper(name = "languages")
  @XmlElement(name = "language")
  private Collection<String> language;
  @XmlElementWrapper(name = "rights-holders")
  @XmlElement(name = "rights-holder")
  private Collection<String> rightsHolder;
  @XmlElementWrapper(name = "spatials")
  @XmlElement(name = "spatial")
  private Collection<String> spatial;
  @XmlElementWrapper(name = "temporals")
  @XmlElement(name = "temporal")
  private Collection<String> temporal;
  @XmlElementWrapper(name = "is-part-of")
  @XmlElement(name = "part-of")
  private Collection<String> isPartOf;
  @XmlElementWrapper(name = "replaces")
  @XmlElement(name = "replace")
  private Collection<String> replaces;
  @XmlElementWrapper(name = "types")
  @XmlElement(name = "type")
  private Collection<String> type;
  @XmlElementWrapper(name = "access")
  @XmlElement(name = "access-rights")
  private Collection<String> accessRights;
  @XmlElementWrapper(name = "licenses")
  @XmlElement(name = "license")
  private Collection<String> license;

  /**
   * Empty constructor needed by JAXB.
   */
  public SeriesResultItemImpl() {

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesResultItem#getId()
   */
  @Override
  public String getId() {
    return id;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesResultItem#getTitle()
   */
  @Override
  public String getTitle() {
    return title;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesResultItem#getSubject()
   */
  @Override
  public Collection<String> getSubject() {
    return subject;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesResultItem#getCreator()
   */
  @Override
  public Collection<String> getCreator() {
    return creator;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesResultItem#getPublisher()
   */
  @Override
  public Collection<String> getPublisher() {
    return publisher;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesResultItem#getContributor()
   */
  @Override
  public Collection<String> getContributor() {
    return contributor;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesResultItem#getAbstract()
   */
  @Override
  public Collection<String> getAbstract() {
    return seriesAbstract;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesResultItem#getDescription()
   */
  @Override
  public Collection<String> getDescription() {
    return description;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesResultItem#getCreated()
   */
  @Override
  public Date getCreated() {
    return created;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesResultItem#getAvailableFrom()
   */
  @Override
  public Date getAvailableFrom() {
    return availableFrom;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesResultItem#getAvailableTo()
   */
  @Override
  public Date getAvailableTo() {
    return availableTo;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesResultItem#getLanguage()
   */
  @Override
  public Collection<String> getLanguage() {
    return language;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesResultItem#getRightsHolder()
   */
  @Override
  public Collection<String> getRightsHolder() {
    return rightsHolder;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesResultItem#getSpatial()
   */
  @Override
  public Collection<String> getSpatial() {
    return spatial;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesResultItem#getTemporal()
   */
  @Override
  public Collection<String> getTemporal() {
    return temporal;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesResultItem#getIsPartOf()
   */
  @Override
  public Collection<String> getIsPartOf() {
    return isPartOf;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesResultItem#getReplaces()
   */
  @Override
  public Collection<String> getReplaces() {
    return replaces;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesResultItem#getType()
   */
  @Override
  public Collection<String> getType() {
    return type;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesResultItem#getAccessRights()
   */
  @Override
  public Collection<String> getAccessRights() {
    return accessRights;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesResultItem#getLicense()
   */
  @Override
  public Collection<String> getLicense() {
    return license;
  }

  /**
   * Sets id.
   * 
   * @param id
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Sets title.
   * 
   * @param title
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * Sets subject.
   * 
   * @param subject
   */
  public void setSubject(Collection<String> subject) {
    this.subject = subject;
  }

  /**
   * Sets creator.
   * 
   * @param creator
   */
  public void setCreator(Collection<String> creator) {
    this.creator = creator;
  }

  /**
   * Sets publisher.
   * 
   * @param publisher
   */
  public void setPublisher(Collection<String> publisher) {
    this.publisher = publisher;
  }

  /**
   * Sets contributor.
   * 
   * @param contributor
   */
  public void setContributor(Collection<String> contributor) {
    this.contributor = contributor;
  }

  /**
   * Sets abstract.
   * 
   * @param seriesAbstract
   */
  public void setAbstract(Collection<String> seriesAbstract) {
    this.seriesAbstract = seriesAbstract;
  }

  /**
   * Sets description.
   * 
   * @param description
   */
  public void setDescription(Collection<String> description) {
    this.description = description;
  }

  /**
   * Sets created.
   * 
   * @param created
   */
  public void setCreated(Date created) {
    this.created = created;
  }

  /**
   * Sets available from.
   * 
   * @param availableFrom
   */
  public void setAvailableFrom(Date availableFrom) {
    this.availableFrom = availableFrom;
  }

  /**
   * Sets avaibale to.
   * 
   * @param availableTo
   */
  public void setAvailableTo(Date availableTo) {
    this.availableTo = availableTo;
  }

  /**
   * Sets language
   * 
   * @param language
   */
  public void setLanguage(Collection<String> language) {
    this.language = language;
  }

  /**
   * Sets rights holder.
   * 
   * @param rightsHolder
   */
  public void setRightsHolder(Collection<String> rightsHolder) {
    this.rightsHolder = rightsHolder;
  }

  /**
   * Sets spatial.
   * 
   * @param spatial
   */
  public void setSpatial(Collection<String> spatial) {
    this.spatial = spatial;
  }

  /**
   * Sets temporal.
   * 
   * @param temporal
   */
  public void setTemporal(Collection<String> temporal) {
    this.temporal = temporal;
  }

  /**
   * Sets part of.
   * 
   * @param isPartOf
   */
  public void setIsPartOf(Collection<String> isPartOf) {
    this.isPartOf = isPartOf;
  }

  /**
   * Sets replaces.
   * 
   * @param replaces
   */
  public void setReplaces(Collection<String> replaces) {
    this.replaces = replaces;
  }

  /**
   * Sets type.
   * 
   * @param type
   */
  public void setType(Collection<String> type) {
    this.type = type;
  }

  /**
   * Sets access rights.
   * 
   * @param accessRights
   */
  public void setAccessRights(Collection<String> accessRights) {
    this.accessRights = accessRights;
  }

  /**
   * Sets license.
   * 
   * @param license
   */
  public void setLicense(Collection<String> license) {
    this.license = license;
  }

  static class Adapter extends XmlAdapter<SeriesResultItemImpl, SeriesResultItem> {

    @Override
    public SeriesResultItem unmarshal(SeriesResultItemImpl v) throws Exception {
      return v;
    }

    @Override
    public SeriesResultItemImpl marshal(SeriesResultItem v) throws Exception {
      return (SeriesResultItemImpl) v;
    }
  }
}
