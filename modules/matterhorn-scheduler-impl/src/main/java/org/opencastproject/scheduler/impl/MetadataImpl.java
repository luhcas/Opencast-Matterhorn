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

package org.opencastproject.scheduler.impl;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.opencastproject.scheduler.api.Event;
import org.opencastproject.scheduler.api.Metadata;
import org.opencastproject.scheduler.endpoint.SchedulerBuilder;

@Entity(name="Metadata")
@Table(name="SCHED_METADATA")
@Access(AccessType.FIELD)
@XmlType(name="Metadata")
public class MetadataImpl implements Metadata {

  @Id
  @ManyToOne
  @JoinColumn(name = "EVENT_ID")
  @XmlTransient
  protected EventImpl event;
  
  @Id
  @Column(name="MD_KEY")
  protected String key;
  @Column(name="MD_VAL")
  protected String value;
  
  public MetadataImpl () {
    super();
  }
  
  public MetadataImpl (Event event, String key, String value) {
    if(event != null) {
      setEvent(event);
    }
    setKey(key);
    setValue(value);
  }
  
  @XmlTransient
  public void setEvent(Event event) {
    this.event = (EventImpl) event;
  }
  
  public Event getEvent() {
    return this.event;
  }
  
  @Override
  public String getKey() {
    return key;
  }
  @Override
  public void setKey(String key) {
    this.key = key;
  }
  @Override
  public String getValue() {
    return value;
  }
  @Override
  public void setValue(String value) {
    this.value = value;
  }  
  
  @Override
  public String toString () {
    return key+":"+value;
  }
  
  @Override
  public boolean equals (Object o) {
    if (o == null) return false;
    if (! (o instanceof Metadata)) return false;
    Metadata m = (Metadata) o;
    if (m.getKey().equals(getKey()) && m.getValue().equals(getValue())) return true;
    return false;
  }
  
  @Override
  public int hashCode () {
    return getKey().hashCode();
  }
  
  /**
   * valueOf function is called by JAXB to bind values. This function calls the ScheduleEvent factory.
   *
   *  @param    xmlString string representation of an event.
   *  @return   instantiated event SchdeulerEventJaxbImpl.
   */
  public static MetadataImpl valueOf(String xmlString) throws Exception {
    return SchedulerBuilder.getInstance().parseMetadata(xmlString);
  }
  
  static class Adapter extends XmlAdapter<MetadataImpl, Metadata> {
    @Override
    public MetadataImpl marshal(Metadata v) throws Exception {
      return (MetadataImpl)v;
    }
    @Override
    public Metadata unmarshal(MetadataImpl v) throws Exception {
      return v;
    }
  }
}
