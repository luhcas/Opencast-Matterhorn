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

package org.opencastproject.scheduler.impl.jpa;


import java.text.ParseException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.opencastproject.scheduler.endpoint.SchedulerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.RRule;

@XmlType(name="RecurringEvent", namespace="http://scheduler.opencastproject.org")
@XmlRootElement(name="RecurringEvent", namespace="http://scheduler.opencastproject.org")
@XmlAccessorType(XmlAccessType.FIELD)
@Entity(name="RecurringEvent")
@Table(name="RecurringEvent")
public class RecurringEvent extends AbstractEvent{
  private static final Logger logger = LoggerFactory.getLogger(RecurringEvent.class);
  
  @XmlID
  @Id 
  protected String rEventId;
  
  @XmlElement(name="recurrence")
  @Column(name="recurrence")
  protected String recurrence;
  
  @XmlElementWrapper(name="metadata-list")
  @XmlElement(name="metadata")
  @OneToMany (fetch=FetchType.EAGER, cascade=CascadeType.ALL)
  @MapKey(name="metadata")
  protected List<Metadata> metadata = new LinkedList<Metadata>();

  @OneToMany(cascade = CascadeType.ALL)
  @MapKey(name="generatedEvents")
  protected List<Event> generatedEvents = new LinkedList<Event>();
  
  @Transient
  List<Date> generatedDates;

  public RecurringEvent () {
  }
  
  public RecurringEvent (String recurrence) {
    setRecurrence(recurrence);
  }

  public RecurringEvent (String recurrence, List<Metadata> metadata) {
    this(recurrence);
    setMetadata(metadata);
  }

  public RecurringEvent (String recurrence, String rEventId) {
    this(recurrence);
    setRecurringEventId(rEventId);
  }  
  
  public RecurringEvent (String recurrence, String rEventId, List<Metadata> metadata) {
    this(recurrence, metadata);
    setRecurringEventId(rEventId);
  }  
  
  public String getRecurringEventId() {
    return rEventId;
  }

  public void setRecurringEventId(String rEventId) {
    this.rEventId = rEventId;
  }
 
  public String generateRecurringEventID (){
    return rEventId = generateId();
  }

  public String getRecurrence() {
    return recurrence;
  }

  public void setRecurrence(String recurrence) {
    generatedDates = null;
    try {
      RRule rrule = new RRule(recurrence);
      rrule.validate();
    } catch (ParseException e) {
      logger.warn("Recurring event {}: Recurrance rule could not be parsed for {}", rEventId, recurrence);
      return;
    } catch (ValidationException e) {
      logger.warn("Recurring event {}: Recurrance rule could not be validated for {}", rEventId, recurrence);
      return; // TODO What should happen if recurrence is wrong?
    }
    this.recurrence = recurrence;
  }

  public List<Metadata> getMetadata() {
    return metadata;
  }

  public void setMetadata(List<Metadata> metadata) {
    metadataTable = null;
    this.metadata = metadata;
  }

  public List<Event> generatedEvents() {
    if (generatedEvents != null) return generatedEvents;
    generatedEvents = new LinkedList<Event>();
    generateDates();

    for (int i = 0; i < generatedDates.size(); i++) {
      Event event = new Event();
      event.setPositionInRecurrence(i);
      event.setRecurringEventId(getRecurringEventId());
      generatedEvents.add(event);
    }
    
    return generatedEvents;
  }
  
  protected void generateDates () {
    generatedDates = new LinkedList<Date>();
    if (metadataTable == null) buildMetadataTable(metadata);
    if (recurrence == null) {
      logger.warn("Could not generate events because of missing recurrence pattern");
      return;
    }
    try {
      Recur recur = new RRule(recurrence).getRecur();
      Date start = getValueAsDate("recurrence.start");
      if (start == null) start = new Date (System.currentTimeMillis());
      Date end = getValueAsDate("recurrence.end");
      if (end == null) {
        logger.error("No end date specified for recurring event {}. "+rEventId);
        return;
      }
      DateList dates = recur.getDates(new net.fortuna.ical4j.model.Date(start), 
                     new net.fortuna.ical4j.model.Date(end), Value.DATE_TIME);
      for (Object date : dates) {
        generatedDates.add((Date) date); 
      }
    } catch (ParseException e) {
      logger.error("Could not parse recurrence {}.", recurrence);
      return;
    }
  }
  
  public List<Event> getEvents () {
    return generatedEvents();
  }
  
  public Date getDateForEventByIndex (int index) {
    if (generatedDates == null) generateDates();
    if (generatedDates.size() < index) {
      logger.warn("trying to get date that does not exist: index {} of {}", index, generatedDates.size());
    }
    return generatedDates.get(index);
  }
  
  public Date getValueAsDate (String key) {
    if (metadataTable == null) buildMetadataTable(metadata);
    try {
      return super.getValueAsDate(key);
    } catch (IncompleteDataException e) {
      logger.warn("MetadataTable could not be build");
      return null;
    }
  }
  
  public boolean containsKey (String key) {
    if (metadataTable == null) buildMetadataTable(metadata);
    try {
      return super.containsKey(key);
    } catch (IncompleteDataException e) {
      logger.warn("MetadataTable could not be build");
      return false;
    }
  }
  
  public void update(RecurringEvent e) {
    //eliminate removed keys
    if (metadataTable == null) buildMetadataTable(metadata);
    try {
      for (String key : getKeySet()) {
        if (! e.containsKey(key)) {
          for (int i = 0; i < getMetadata().size(); i++) {
            if (getMetadata().get(i).getKey().equals(key)) {
              getMetadata().remove(i);
              break; //skip rest of the loop if found
            }
          }
        }
      }
    } catch (IncompleteDataException e1) {
      logger.warn("Metadata could not be processed for recurring event {}. Recurring event could not be updated.", getRecurringEventId());
      return; 
    }
    
    //update the list
    for (Metadata data : e.getMetadata()) {
      if (containsKey(data.getKey())) {
        for (Metadata dataOld : getMetadata()) {
          if (dataOld.getKey().equals(data.key) && ! dataOld.equals(data)) { 
            dataOld.setValue(data.getValue());
            break;
          }
        }
      } else {
        getMetadata().add(data);
      }
    } 
    setRecurrence(e.getRecurrence());
    metadataTable = null;
    // currently there is no reason to assume that ID or the parent recurringEvent can change;
  }  
  
  public static RecurringEvent find (String recurringEventId, EntityManagerFactory emf) {
    logger.debug("loading recurring event with the ID {}", recurringEventId);
    if (recurringEventId == null || emf == null) {
      logger.warn("could not find reccuring event {}. Null Pointer exeption");
      return null;
    }
    EntityManager em = emf.createEntityManager();
    RecurringEvent e = null;
    try {
       e = em.find(RecurringEvent.class, recurringEventId);
    } finally {
      em.close();
    }
    e.setEntityManagerFactory(emf);
    return e;
  }
  
  public String toString () {
    String result = "Recurring Event " + rEventId +", pattern: " +recurrence+ ", generated events: "+System.getProperty("line.separator");
    for (Event e : generatedEvents) 
      result += e.toString() + System.getProperty("line.separator");   
    return result;
  }
  
  protected void updateMetadata (Metadata data) {
    if (containsKey(data.getKey())) {
      for (Metadata olddata : getMetadata()) {
        if (olddata.getKey().equals(data.getKey())) {
          olddata.setValue(data.value);
          break;
        }
      }
    } else {
      metadata.add(data);
    }
    metadataTable = null;
  }
  
  /**
   * valueOf function is called by JAXB to bind values. This function calls the ScheduleEvent factory.
   *
   *  @param    xmlString string representation of an event.
   *  @return   instantiated event SchdeulerEventJaxbImpl.
   */
  public static RecurringEvent valueOf(String xmlString) throws Exception {
    return (RecurringEvent) SchedulerBuilder.getInstance().parseRecurringEvent(xmlString);
  }   
  
  public String generateId () {
    return rEventId = super.generateId();
  }
  
}
