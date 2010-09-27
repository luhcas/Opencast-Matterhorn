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

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Access(AccessType.FIELD)
@Entity(name="SeriesMetadataImpl")
@Table(name="SERIES_METADATA")
@XmlType(name="seriesMetadata", namespace="http://series.opencastproject.org")
@XmlRootElement(name="series")
@XmlAccessorType(XmlAccessType.FIELD)
public class SeriesMetadataImpl implements SeriesMetadata {
  private static final Logger logger = LoggerFactory.getLogger(SeriesImpl.class);
    
  @Id
  @Column(name="METADATA_KEY", length=128)
  @XmlElement(name="key")
  protected String key;

  @Column(name="METADATA_VAL", length=256)
  @XmlElement(name="value")
  protected String value;
  
  @Id
  @ManyToOne
  @JoinColumn(name="SERIES_ID")
  protected SeriesImpl series;
  
  public SeriesImpl getSeries() {
    return series;
  }

  public void setSeries(Series series) {
    logger.debug("Set series: {}", series.getSeriesId());
    if (series instanceof SeriesImpl) {
      this.series = (SeriesImpl) series;
    }
  }

  public SeriesMetadataImpl () {
    super();
  }

  public SeriesMetadataImpl (String key, String value) {
    this.key = key;
    this.value = value;
  }

  public SeriesMetadataImpl (Series series, String key, String value) {
    this.key = key;
    this.value = value;
    setSeries(series);
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.series.api.SeriesMetadata#getKey()
   */
  public String getKey() {
    return key;
  }
  /**
   * {@inheritDoc}
   * @see org.opencastproject.series.api.SeriesMetadata#setKey(java.lang.String)
   */
  public void setKey(String key) {
    this.key = key;
  }
  /**
   * {@inheritDoc}
   * @see org.opencastproject.series.api.SeriesMetadata#getValue()
   */
  public String getValue() {
    return value;
  }
  /**
   * {@inheritDoc}
   * @see org.opencastproject.series.api.SeriesMetadata#setValue(java.lang.String)
   */
  public void setValue(String value) {
    this.value = value;
  }  
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.series.api.SeriesMetadata#toString()
   */
  public String toString () {
    return "("+ series +") "+key+":"+value;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.series.api.SeriesMetadata#equals(java.lang.Object)
   */
  public boolean equals (Object o) {
    if (o == null) return false;
    if (! (o instanceof SeriesMetadataImpl)) return false;
    SeriesMetadata m = (SeriesMetadata) o;
    if (m.getKey().equals(getKey()) && m.getValue().equals(getValue())) return true;
    return false;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.series.api.SeriesMetadata#hashCode()
   */
  public int hashCode () {
    return getKey().hashCode();
  }
  
  public static SeriesMetadataImpl valueOf(String xmlString) throws Exception {
    return SeriesBuilder.getInstance().parseSeriesMetadataImpl(xmlString);
  }
}
