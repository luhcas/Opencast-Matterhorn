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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.opencastproject.series.api.SeriesMetadata;


@Entity(name="SeriesMetadata")
@Table(name="SeriesMetadata")
public class SeriesMetadataImpl implements SeriesMetadata {
  @Id
  @GeneratedValue
  protected long id;
  
  @Column(name="key")
  protected String key;
  @Column(name="value")
  protected String value;
  
  public SeriesMetadataImpl () {
    super();
  }
  
  public SeriesMetadataImpl (String key, String value) {
    this.key = key;
    this.value = value;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.series.api.SeriesMetadata#getId()
   */
  public long getId() {
    return id;
  }
  /**
   * {@inheritDoc}
   * @see org.opencastproject.series.api.SeriesMetadata#setId(long)
   */
  public void setId(long id) {
    this.id = id;
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
    return "("+id+") "+key+":"+value;
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
}
