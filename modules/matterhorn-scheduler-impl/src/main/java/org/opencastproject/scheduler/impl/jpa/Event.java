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

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.MapKey;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
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

import org.opencastproject.scheduler.api.SchedulerEvent;
import org.opencastproject.scheduler.endpoint.SchedulerBuilder;
import org.opencastproject.scheduler.impl.SchedulerEventImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An Event has a unique ID, a relation to the recurring event from which it was created and a set of metadata.
 * Even the start- and end-time is stored in the set of metadata, with the keys "time.start" and "time.end" as long value converted to string.
 * Resources and Attendees are store in the metadata too, as 
 */

@NamedQueries( {
  @NamedQuery(name = "Event.getAll", query = "SELECT e FROM Event e")
})

@XmlType(name="Event", namespace="http://scheduler.opencastproject.org")
@XmlRootElement(name="Event", namespace="http://scheduler.opencastproject.org")
@XmlAccessorType(XmlAccessType.FIELD)
@Entity
@Table(name="Event")
public class Event extends AbstractEvent {

  private static final Logger logger = LoggerFactory.getLogger(Event.class);
  
  @XmlID
  @Id
  protected String eventId;
  
  @Transient 
  RecurringEvent recurringEvent = null;
  
  @XmlElementWrapper(name="metadata-list")
  @XmlElement(name="metadata")
  @OneToMany (fetch=FetchType.EAGER, cascade=CascadeType.ALL)
  @MapKey(name="metadata")
  protected List<Metadata> metadata = new LinkedList<Metadata>();  
  
  public String getRecurringEventId() {
    if (! containsKey("recurrence.id")) return null;
    try {
      return getValue("recurrence.id");
    } catch (NumberFormatException e) {
      logger.warn("Could not parse value for recurrence position: {}", getValue("recurrence.position"));
      return null;
    }
  }

  public void setRecurringEventId(String recurringEventId) {
    updateMetadata(new Metadata("recurrence.position", recurringEventId));
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

  public int getPositionInRecurrence() {
    if (! containsKey("recurrence.position")) return 0;
    try {
      return Integer.parseInt(getValue("recurrence.position"));
    } catch (NumberFormatException e) {
      logger.warn("Could not parse value for recurrence position: {}", getValue("recurrence.position"));
      return 0;
    }
  }

  public void setPositionInRecurrence(int positionInRecurrence) {
    updateMetadata(new Metadata("recurrence.position", new Integer(positionInRecurrence).toString()));
  }

  public String getEventId() {
    return eventId;
  }

  public void setEventId(String eventId) {
    this.eventId = eventId;
  }
  
  public String generateId() {
    return eventId = super.generateId();
  }

  public RecurringEvent getRecurringEvent() {
    if (recurringEvent == null)
      recurringEvent = RecurringEvent.find(getRecurringEventId(), emf);
    return recurringEvent;
  }

  public void setRecurringEvent(RecurringEvent recurringEvent) {
    this.recurringEvent = recurringEvent;
    if (getRecurringEventId() != null)
      setRecurringEventId(recurringEvent.getRecurringEventId());
  }

  public List<Metadata> getMetadata() {
    return metadata;
  }

  public void setMetadata(List<Metadata> metadata) {
    metadataTable = null;
    this.metadata = metadata;
  }
  
  protected void buildMetadataTable () {
    if (recurringEvent != null) 
      for (Metadata data : recurringEvent.getMetadata()) metadataTable.put(data.key, data.value); // Inherit values
    super.buildMetadataTable(metadata);
  }
  
  public String getValue (String key) {
   if (metadataTable == null) buildMetadataTable();
   try {
    return super.getValue(key);
   } catch (IncompleteDataException e) {
     logger.warn("MetadataTable could not be build");
     return null;
   }
  }
  
  public Set<String> getKeySet () {
    if (metadataTable == null) buildMetadataTable();
    try {
      return super.getKeySet();
    } catch (IncompleteDataException e) {
      logger.warn("MetadataTable could not be build");
      return null;
    }
  }
  
  public SchedulerEvent toSchedulerEvent () {
    SchedulerEventImpl e = new SchedulerEventImpl();
    if (metadataTable == null) buildMetadataTable();
    e.setMetadata(metadataTable);
    e.setID(eventId);
    e.setStartdate(getStartdate()); 
    e.setEnddate(getEnddate());
    StringTokenizer attendees =  new StringTokenizer(metadataTable.get("attendees"),",");
    while (attendees.hasMoreTokens()) e.addAttendee(attendees.nextToken());
    StringTokenizer resources =  new StringTokenizer(metadataTable.get("resources"),",");
    while (resources.hasMoreTokens()) e.addResource(resources.nextToken());  
    return e;
  }
  
  public Date getValueAsDate (String key) {
    if (metadataTable == null) buildMetadataTable();
    try {
      return super.getValueAsDate(key);
    } catch (IncompleteDataException e) {
      logger.warn("MetadataTable could not be build");
      return null;
    }
  }
  
  public boolean containsKey (String key) {
    if (metadataTable == null) buildMetadataTable();
    try {
      return super.containsKey(key);
    } catch (IncompleteDataException e) {
      logger.warn("MetadataTable could not be build");
      return false;
    }
  }
  
  public void update(Event e) {
    //eliminate removed keys
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
    //metadata = e.getMetadata(); 
    metadataTable = null;
    // currently there is no reason to assume that ID or the parent recurringEvent can change;
  } 
  
  
  public Date getStartdate () {
    if (containsKey("time.start")) {
     return getValueAsDate("time.start"); 
    }
    if (getRecurringEventId() != null) {
      return getRecurringEvent().getDateForEventByIndex(getPositionInRecurrence());
    }
    return null; 
  }

  public Date getEnddate () {
    if (containsKey("time.end")) {
     return getValueAsDate("time.end"); 
    }
    if (getRecurringEventId() != null) {
      try {
        if (! containsKey("recurrence.duration")){
          logger.error("No default duration set in recurrent event {}.", getRecurringEventId());
        }          
        return new Date (getRecurringEvent().getDateForEventByIndex(getPositionInRecurrence()).getTime() + 
                          Long.parseLong(getValue("recurrence.duration")));
      } catch (NumberFormatException e) {
        logger.warn("Could not parse recurring event default duration");
        return null;
      } 
    }
    return null; 
  }
  
  public String toString () {
    String result;
    result = getEventId();
    if (getRecurringEventId() != null) result += ", Recurring Event: "+getRecurringEvent().getRecurringEventId();
    else result += ", no recuring event";
    for (Metadata data : metadata) {
      result += ", "+data.toString();
    }
    return result;
  }
  
  public static Event find (String eventId, EntityManagerFactory emf) {
    logger.debug("loading event with the ID {}", eventId);
    if (eventId == null || emf == null) {
      logger.warn("could not find event {}. Null Pointer exeption");
      return null;
    }
    EntityManager em = emf.createEntityManager();
    Event e = null;
    try {
       e = em.find(Event.class, eventId);
    } finally {
      em.close();
    }
    if (e != null) e.setEntityManagerFactory(emf);
    return e;
  }
  
  /**
   * valueOf function is called by JAXB to bind values. This function calls the ScheduleEvent factory.
   *
   *  @param    xmlString string representation of an event.
   *  @return   instantiated event SchdeulerEventJaxbImpl.
   */
  public static Event valueOf(String xmlString) throws Exception {
    return (Event) SchedulerBuilder.getInstance().parseEvent(xmlString);
  }  
  
  public boolean equals (Object o) {
    if (! (o instanceof Event)) return false;
    Event e = (Event) o;
    if (e.getEventId() != this.getEventId()) return false;
    for (Metadata m : metadata) {
      if (! e.containsKey(m.getKey()) || (! e.getValue(m.getKey()).equals(m.getValue()))) return false;
    }
    return true;
  }
  
  public int hashCode () {
    return this.getEventId().hashCode();
  }
  
}
