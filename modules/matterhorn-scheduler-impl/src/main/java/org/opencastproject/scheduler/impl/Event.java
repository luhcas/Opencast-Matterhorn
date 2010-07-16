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

import org.opencastproject.scheduler.endpoint.SchedulerBuilder;
import org.opencastproject.scheduler.impl.IncompleteDataException;
import org.opencastproject.scheduler.impl.RecurringEvent;
import org.opencastproject.scheduler.impl.Metadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
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
import javax.xml.bind.annotation.XmlTransient;

/**
 * An Event has a unique ID, a relation to the recurring event from which it was created and a set of metadata. Even the
 * start- and end-time is stored in the set of metadata, with the keys "timeStart" and "timeEnd" as long value
 * converted to string. Resources and Attendees are store in the metadata too, as
 */

@NamedQueries( { @NamedQuery(name = "Event.getAll", query = "SELECT e FROM Event e") })
@XmlRootElement(name = "event")
@XmlAccessorType(XmlAccessType.FIELD)
@Entity(name = "Event")
@Access(AccessType.FIELD)
@Table(name = "SCHED_EVENT")
public class Event extends AbstractEvent {

  public Event() {

  }

  public Event(String xml) {
    try {
      Event e = Event.valueOf(xml);
      this.setEventId(e.getEventId());
      this.setMetadata(e.getCompleteMetadata());
    } catch (Exception e) {
      logger.warn("Could not parse Event XML {}", xml);
    }
  }

  private static final Logger logger = LoggerFactory.getLogger(Event.class);

  @XmlID
  @Id
  @Column(name = "ID", length = 128)
  protected String eventId;

  @Transient
  @XmlTransient
  RecurringEvent recurringEvent = null;

  // FIXME: Do we really need a join table here? How about a composite key (event id + metadata key) in the metadata
  // table?
  @XmlElementWrapper(name = "metadataList")
  @XmlElement(name = "metadata")
  @OneToMany(fetch = FetchType.EAGER, targetEntity = Metadata.class, cascade = CascadeType.ALL)
  @JoinTable(name = "SCHED_EVENT_METADATA", joinColumns = { @JoinColumn(name = "EVENT_ID") },
          inverseJoinColumns = { @JoinColumn(name = "METADATA_ID") })
  protected List<Metadata> metadata = new LinkedList<Metadata>();

  public String getRecurringEventId() {
    Metadata m = findMetadata("recurrenceId");
    if (m == null) {
      logger.debug("recurring event for event {} not found", getEventId());
      return null;
    }
    return m.getValue();
  }

  public void setRecurringEventId(String recurringEventId) {
    updateMetadata(new Metadata("recurrenceId", recurringEventId));
  }

  public void updateMetadata(Metadata data) {
    if (containsKey(data.getKey())) {
      for (Metadata olddata : getCompleteMetadata()) {
        if (olddata.getKey().equals(data.getKey())) {
          olddata.setValue(data.getValue());
          break;
        }
      }
    } else {
      metadata.add((Metadata)data);
    }
    metadataTable = null;
  }

  public int getPositionInRecurrence() {
    if (!containsKey("recurrencePosition"))
      return 0;
    try {
      return Integer.parseInt(getValue("recurrencePosition"));
    } catch (NumberFormatException e) {
      logger.warn("Could not parse value for recurrence position: {}", getValue("recurrencePosition"));
      return 0;
    }
  }

  public void setPositionInRecurrence(int positionInRecurrence) {
    updateMetadata(new Metadata("recurrencePosition", new Integer(positionInRecurrence).toString()));
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
    if (recurringEvent != null) return recurringEvent;
    if (getRecurringEventId() != null && recurringEvent == null) {
      recurringEvent = RecurringEvent.find(getRecurringEventId(), emf);
      metadataTable = null;
    }
    return recurringEvent;
  }

  public void setRecurringEvent(RecurringEvent recurringEvent) {
    this.recurringEvent = recurringEvent;
    if (getRecurringEventId() != null)
      setRecurringEventId(recurringEvent.getRecurringEventId());
  }

  @XmlElementWrapper(name = "completeMetadata")
  @XmlElement(name = "metadata")
  public List<Metadata> getCompleteMetadata() {
    Hashtable<String, Metadata> m = new Hashtable<String, Metadata>();
    for (Metadata data : getMetadata()) {
      m.put(data.getKey(), data);
    }
    if (getRecurringEvent() != null) {
      for (Metadata data : getRecurringEvent().getMetadata()) {
        m.put(data.getKey(), data);
      }
      m.put("timeStart", new Metadata("timeStart", "" + getStartdate().getTime()));
      m.put("timeEnd", new Metadata("timeEnd", "" + getEnddate().getTime()));
    }
    m.put("timeDuration", new Metadata("timeDuration", "" + (getEnddate().getTime() - getStartdate().getTime())));
    return new LinkedList<Metadata>(m.values());
  }

  public List<Metadata> getMetadata() {
    return this.metadata;
  }

  public void setMetadata(List<Metadata> metadata) {
    metadataTable = null;
    this.metadata = metadata;
  }

  public void buildMetadataTable() {
    RecurringEvent rEvent = getRecurringEvent(); // if recurring event is not yet set, this method has the sideeffect
                                                 // that the metadata table is nulled
    if (metadataTable == null)
      metadataTable = new Hashtable<String, String>();
    if (rEvent != null) {
      for (Metadata data : rEvent.getMetadata()) {
        if (data != null && data.getKey() != null && data.getValue() != null)
          metadataTable.put(data.getKey(), data.getValue()); // Inherit values
      }
    }
    super.buildMetadataTable(getMetadata());
  }

  public String getValue(String key) {
    if (metadataTable == null)
      buildMetadataTable();
    try {
      return super.getValue(key);
    } catch (IncompleteDataException e) {
      logger.warn("MetadataTable could not be build");
      return null;
    }
  }

  public Set<String> getKeySet() {
    if (metadataTable == null)
      buildMetadataTable();
    try {
      return super.getKeySet();
    } catch (IncompleteDataException e) {
      logger.warn("MetadataTable could not be build");
      return null;
    }
  }

  public Date getValueAsDate(String key) {
    if (metadataTable == null)
      buildMetadataTable();
    try {
      return super.getValueAsDate(key);
    } catch (IncompleteDataException e) {
      logger.warn("MetadataTable could not be build");
      return null;
    }
  }

  public boolean containsKey(String key) {
    if (metadataTable == null)
      buildMetadataTable();
    try {
      return super.containsKey(key);
    } catch (IncompleteDataException e) {
      logger.warn("MetadataTable could not be build");
      return false;
    } catch (NullPointerException e) {
      logger.warn("could not find key {}", key);
      return false;
    }
  }

  public Metadata findMetadata(String key) {
    for (Metadata m : metadata) {
      if (m.getKey().equals(key))
        return m;
    }
    return null;
  }

  public void update(Event e) {
    // eliminate removed keys
    for (Metadata m : getMetadata()) {
      if (e.findMetadata(m.getKey()) == null) {
        removeMetadata(m);
      }
    }
    logger.debug("Updating stored event with new metadata.");
    // update the list
    for (Metadata data : e.getMetadata()) {
      Metadata found = findMetadata(data.getKey());
      if (found != null) {
        found.setValue(data.getValue());
      } else {
        addMetadata(data);
      }
    }
    // metadata = e.getMetadata();
    metadataTable = null;
    // currently there is no reason to assume that ID or the parent recurringEvent can change;
  }

  public Date getStartdate() {
    if (containsKey("timeStart")) {
      return getValueAsDate("timeStart");
    }
    if (recurringEvent != null || (getRecurringEventId() != null && getRecurringEventId().length() > 0)) {
      return getRecurringEvent().getDateForEventByIndex(getPositionInRecurrence());
    }
    return null;
  }

  public Date getEnddate() {
    if (containsKey("timeEnd")) {
      return getValueAsDate("timeEnd");
    }
    if (getRecurringEventId() != null) {
      try {
        if (!containsKey("recurrenceDuration")) {
          logger.error("No default duration set in recurrent event {}.", getRecurringEventId());
        }
        return new Date(getRecurringEvent().getDateForEventByIndex(getPositionInRecurrence()).getTime()
                + Long.parseLong(getValue("recurrenceDuration")));
      } catch (NumberFormatException e) {
        logger.warn("Could not parse recurring event default duration");
        return null;
      }
    }
    return null;
  }

  public String toString() {
    String result;
    result = getEventId();
    if (getRecurringEventId() != null)
      result += ", Recurring Event: " + getRecurringEventId();
    else
      result += ", no recuring event";
    for (Metadata data : metadata) {
      result += ", " + data.toString();
    }
    return result;
  }

  public static Event find(String eventId, EntityManagerFactory emf) {
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
    if (e != null){
      e.setEntityManagerFactory(emf);
    }else{
      logger.warn("No event found for {}", eventId);
    }
    return e;
  }

  /**
   * valueOf function is called by JAXB to bind values. This function calls the ScheduleEvent factory.
   * 
   * @param xmlString
   *          string representation of an event.
   * @return instantiated event SchdeulerEventJaxbImpl.
   */
  public static Event valueOf(String xmlString) throws Exception {
    return (Event) SchedulerBuilder.getInstance().parseEvent(xmlString);
  }

  public boolean equals(Object o) {
    if (!(o instanceof Event))
      return false;
    Event e = (Event) o;
    if (e.getEventId() != this.getEventId())
      return false;
    for (Metadata m : metadata) {
      if (!e.containsKey(m.getKey()) || (!e.getValue(m.getKey()).equals(m.getValue())))
        return false;
    }
    return true;
  }

  public int hashCode() {
    return this.getEventId().hashCode();
  }
  
  public void addMetadata(Metadata m){
    this.metadata.add(m);
  }
  
  public void removeMetadata(Metadata m){
    this.metadata.remove(m);
  }
  
}
