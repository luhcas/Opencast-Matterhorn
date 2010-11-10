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

package org.opencastproject.series.impl;

import org.opencastproject.mediapackage.EName;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogImpl;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.series.api.Series;
import org.opencastproject.series.api.SeriesMetadata;
import org.opencastproject.series.endpoint.SeriesBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Data type for a Series that stores the metadata that belongs to the series
 * 
 */

@Entity(name = "SeriesImpl")
@Table(name = "SERIES")
@Access(AccessType.FIELD)
@XmlType(name = "series", namespace = "http://series.opencastproject.org")
@XmlRootElement(name = "series")
@XmlAccessorType(XmlAccessType.NONE)
public class SeriesImpl implements Series {
  private static final Logger logger = LoggerFactory.getLogger(SeriesImpl.class);

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "ID", length = 36)
  @XmlID
  @XmlAttribute(name = "id")
  String seriesId;

  @Lob
  @Column
  @XmlElement
  String description;

  @Transient
  DublinCoreCatalog dublinCore;

  @OneToMany(cascade = CascadeType.ALL, targetEntity = SeriesMetadataImpl.class, mappedBy = "series", fetch = FetchType.EAGER)
  @XmlElementWrapper(name = "metadataList")
  @XmlElement(name = "metadata")
  List<SeriesMetadata> metadata;

  /**
   * Default constructor without any import.
   */
  public SeriesImpl() {
  }

  public SeriesImpl(String seriesXml) {
    try {
      SeriesImpl series = SeriesImpl.valueOf(seriesXml);
      setSeriesId(series.getSeriesId());
      setMetadata(series.getMetadata());
      setDescription(series.getDescription());
    } catch (Exception e) {
      logger.debug("Unable to load series: {}", e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.series.api.Series#getSeriesId()
   */
  public String getSeriesId() {
    return seriesId;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.series.api.Series#setSeriesId(java.lang.String)
   */
  public void setSeriesId(String seriesId) {
    this.seriesId = seriesId;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.series.api.Series#addToMetadata(java.lang.String, java.lang.String)
   */
  public void addToMetadata(String key, String value) {
    if (key == null)
      throw new IllegalArgumentException("Metadata key must not be null");
    if (key.length() > 128)
      throw new IllegalArgumentException("Metadata key cannot be longer than 128 characters");
    if (value == null)
      throw new IllegalArgumentException("Metadata value must not be null");

    boolean updated = false;

    // If there has been no metadata at all, creat it.
    if (metadata == null) {
      metadata = new LinkedList<SeriesMetadata>();
    }

    // See if this is an update
    for (SeriesMetadata m : getMetadata()) {
      if (m.getKey().equals(key)) {
        m.setValue(value);
        updated = true;
        break;
      }
    }

    // Seems to be a new entry
    if (!updated) {
      metadata.add(new SeriesMetadataImpl(this, key, value));
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.series.api.Series#getFromMetadata(java.lang.String)
   */
  public String getFromMetadata(String key) {
    for (SeriesMetadata m : getMetadata())
      if (m.getKey().equals(key))
        return m.getValue();
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.series.api.Series#getMetadata()
   */
  public List<SeriesMetadata> getMetadata() {
    LinkedList<SeriesMetadata> list = new LinkedList<SeriesMetadata>();
    if (metadata != null)
      list.addAll(metadata);
    return list;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.series.api.Series#setMetadata(java.util.List)
   */
  public void setMetadata(List<SeriesMetadata> metadata) {
    this.metadata = metadata;
    if (metadata == null)
      return;
    for (SeriesMetadata m : metadata) {
      m.setSeries(this);
    }
    dublinCore = null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.series.api.Series#getDublinCore()
   */
  public DublinCoreCatalog getDublinCore() {
    if (dublinCore == null)
      dublinCore = buildDublinCore();
    return dublinCore;
  }

  private static boolean isDateKey(String key) {
    String[] dateKeys = new String[] { "valid", "extent", "created", "available", "modified", "issued" };
    List<String> dk = Arrays.asList(dateKeys);
    if (dk.contains(key))
      return true;
    return false;
  }

  public static String formatW3CDTF(Date date) {
    SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
    dateFormater.setTimeZone(TimeZone.getTimeZone("GMT"));
    return dateFormater.format(date);
  }

  private DublinCoreCatalog buildDublinCore() {
    DublinCoreCatalog dc = DublinCoreCatalogImpl.newInstance();

    dc.add(DublinCoreCatalog.PROPERTY_IDENTIFIER, new DublinCoreValue(getSeriesId()));
    dc.add(DublinCoreCatalog.PROPERTY_DESCRIPTION, new DublinCoreValue(description));
    for (SeriesMetadata m : getMetadata()) {
      String v = null;
      if (isDateKey(m.getKey()))
        v = formatW3CDTF(new Date(Long.parseLong(m.getValue())));
      else
        v = m.getValue();
      DublinCoreValue value = new DublinCoreValue(v);
      EName property = new EName("http://purl.org/dc/terms/", m.getKey());
      dc.add(property, value);
    }

    return dc;
  }

  public static SeriesImpl buildSeries(DublinCoreCatalog dc) {
    SeriesImpl s = new SeriesImpl();
    s.setSeriesId(dc.getFirst(DublinCoreCatalog.PROPERTY_IDENTIFIER));
    s.setDescription(dc.getFirst(DublinCoreCatalog.PROPERTY_DESCRIPTION));
    s.updateMetadata(dc);
    return s;
  }

  /**
   * Adds the metadata found in the dublin core catalog to the series. Note that only the first value of every metadata
   * item will be store.
   * 
   * @param dc
   *          the dublin core catalog
   */
  public void updateMetadata(DublinCoreCatalog dc) {
    for (EName prop : dc.getProperties()) {
      if (dc.get(prop).size() > 0)
        addToMetadata(prop.getLocalName(), dc.getFirst(prop));
    }
  }

  @Override
  public String getBriefDescription() {
    String result = getFromMetadata("title");
    String creator = getFromMetadata("creator");
    String temporal = getFromMetadata("temporal");
    if (creator != null && !creator.isEmpty()) {
      result += " - " + creator;
    }
    if (temporal != null && !temporal.isEmpty()) {
      result += " (" + temporal + ")";
    }
    return result;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.series.api.Series#getDescription()
   */
  @Override
  public String getDescription() {
    return description;
  }

  /**
   * @param description
   *          the description to set
   */
  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public int compareTo(Series o) {
    return getBriefDescription().compareTo(o.getBriefDescription());
  }

  /**
   * valueOf function is called by JAXB to bind values. This function calls the series factory.
   * 
   * @param xmlString
   *          string representation of an series.
   * @return instantiated event SeriesJaxbImpl.
   */
  public static SeriesImpl valueOf(String xmlString) throws Exception {
    return SeriesBuilder.getInstance().parseSeriesImpl(xmlString);
  }
  
}
