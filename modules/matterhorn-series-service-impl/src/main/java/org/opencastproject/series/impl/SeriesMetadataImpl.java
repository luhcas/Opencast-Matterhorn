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

import org.opencastproject.series.api.Series;
import org.opencastproject.series.api.SeriesMetadata;
import org.opencastproject.series.endpoint.SeriesBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlRootElement;

@Entity(name = "SeriesMetadataImpl")
@Table(name = "SERIES_METADATA")
@Access(AccessType.FIELD)
@XmlType(name = "seriesMetadata", namespace = "http://series.opencastproject.org")
@XmlRootElement(name="metadata")
@XmlAccessorType(XmlAccessType.NONE)
public class SeriesMetadataImpl implements SeriesMetadata {

  private static final Logger logger = LoggerFactory.getLogger(SeriesMetadataImpl.class);
  
  @Id
  @Column(name = "METADATA_KEY", length = 128)
  @XmlElement(name = "key")
  protected String key;

  @Lob
  @Column(name = "METADATA_VAL")
  @XmlElement(name = "value")
  protected String value;

  @Id
  @ManyToOne
  @JoinColumn(name = "SERIES_ID")
  protected SeriesImpl series;

  /**
   * Empty constructor for use by JPA and/or JAXB only.
   */
  public SeriesMetadataImpl() {
    super();
  }

  /**
   * Adds a metadata item to the series metadata.
   * 
   * @param key
   *          the metadata item identifier
   * @param value
   *          the metadata value
   * @throws IllegalArgumentException
   *           if either one of <code>key</code>, <code>value</code> are <code>null</code>
   * @throws IllegalArgumentException
   *           if <code>key</code> is more than 128 characters long
   */
  public SeriesMetadataImpl(String key, String value) {
    this(null, key, value);
  }

  /**
   * Adds a metadata item to the series metadata.
   * 
   * @param the
   *          series object
   * @param key
   *          the metadata item identifier
   * @param value
   *          the metadata value
   * @throws IllegalArgumentException
   *           if either one of <code>key</code>, <code>value</code> are <code>null</code>
   * @throws IllegalArgumentException
   *           if <code>key</code> is more than 128 characters long
   */
  public SeriesMetadataImpl(Series series, String key, String value) {
    if (key == null)
      throw new IllegalArgumentException("Metadata key must not be null");
    if (key.length() > 128)
      throw new IllegalArgumentException("Metadata key cannot be longer than 128 characters");
    if (value == null)
      throw new IllegalArgumentException("Metadata value must not be null");
    this.key = key;
    this.value = value;
    if (series != null) {
      setSeries(series);
    }
  }

  /**
   * Returns the series object or <code>null</code> if no series has been associated.
   * 
   * {@inheritDoc}
   * 
   * @see org.opencastproject.series.api.SeriesMetadata#getSeries()
   */
  public Series getSeries() {
    return series;
  }

  /**
   * Sets the series.
   * 
   * {@inheritDoc}
   * 
   * @see org.opencastproject.series.api.SeriesMetadata#setSeries(org.opencastproject.series.api.Series)
   */
  public void setSeries(Series series) {
    logger.debug("Set series: {}", series.getSeriesId());
    if (series instanceof SeriesImpl) {
      this.series = (SeriesImpl) series;
    } else {
      throw new IllegalStateException("This code is not meant to be associated with external series implementations");
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.series.api.SeriesMetadata#getKey()
   */
  public String getKey() {
    return key;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.series.api.SeriesMetadata#setKey(java.lang.String)
   */
  public void setKey(String key) {
    if (key == null)
      throw new IllegalArgumentException("Metadata key must not be null");
    if (key.length() > 128)
      throw new IllegalArgumentException("Metadata key cannot be longer than 128 characters");
    this.key = key;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.series.api.SeriesMetadata#getValue()
   */
  public String getValue() {
    return value;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.series.api.SeriesMetadata#setValue(java.lang.String)
   */
  public void setValue(String value) {
    if (value == null)
      throw new IllegalArgumentException("Metadata value must not be null");
    this.value = value;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.series.api.SeriesMetadata#toString()
   */
  public String toString() {
    return "(" + series + ") " + key + ":" + value;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.series.api.SeriesMetadata#equals(java.lang.Object)
   */
  public boolean equals(Object o) {
    if (o == null)
      return false;
    if (!(o instanceof SeriesMetadataImpl))
      return false;
    SeriesMetadata m = (SeriesMetadata) o;
    if (m.getKey().equals(getKey()) && m.getValue().equals(getValue()))
      return true;
    return false;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.series.api.SeriesMetadata#hashCode()
   */
  public int hashCode() {
    return getKey().hashCode();
  }

  /**
   * Creates a new series metadata item from <code>xmlString</code>.
   * 
   * @param xmlString
   *          the serialized metadata item
   * @return the object representation
   * @throws Exception
   *           if deserialization failed
   */
  public static SeriesMetadataImpl valueOf(String xmlString) throws Exception {
    return SeriesBuilder.getInstance().parseSeriesMetadataImpl(xmlString);
  }

}
