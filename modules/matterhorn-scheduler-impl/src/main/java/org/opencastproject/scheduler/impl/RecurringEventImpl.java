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

import org.opencastproject.scheduler.api.Event;
import org.opencastproject.scheduler.api.Metadata;
import org.opencastproject.scheduler.api.RecurringEvent;
import org.opencastproject.scheduler.endpoint.SchedulerBuilder;
import org.opencastproject.scheduler.impl.IncompleteDataException;

import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.RRule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
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

@NamedQueries( { @NamedQuery(name = "RecurringEvent.getAll", query = "SELECT e FROM RecurringEvent e") })
@XmlRootElement(name = "recurringEvent")
@XmlAccessorType(XmlAccessType.FIELD)
@Entity(name = "RecurringEvent")
@Table(name = "SCHED_R_EVENT")
@Access(AccessType.FIELD)
public class RecurringEventImpl extends AbstractEvent implements RecurringEvent {
  private static final Logger logger = LoggerFactory.getLogger(RecurringEventImpl.class);

  @XmlID
  @Id
  @GeneratedValue
  @Column(name = "ID", length=128)
  @XmlElement(name = "recurringEventId")
  protected String rEventId;

  @XmlElement(name = "recurrence")
  @Column(name = "RECURRENCE")
  protected String recurrence;

  @XmlElementWrapper(name = "metadataList")
  @XmlElement(name = "metadata")
  @OneToMany(fetch = FetchType.EAGER, targetEntity = MetadataImpl.class, cascade = CascadeType.ALL)
  @JoinTable(
          name = "SCHED_R_EVENT_METADATA",
          joinColumns = { @JoinColumn(name = "REC_EVENT_ID") },
          inverseJoinColumns = { @JoinColumn(name = "MD_ID") })
  protected List<MetadataImpl> metadata = new LinkedList<MetadataImpl>();

  @XmlElementWrapper(name = "events")
  @XmlElement(name = "event")
  @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
  @JoinTable(
          name = "SCHED_R_EVENT_ITEM",
          joinColumns = { @JoinColumn(name = "REC_EVENT_ID") },
          inverseJoinColumns = { @JoinColumn(name = "EVENT_ID") })
  protected List<EventImpl> generatedEvents = new LinkedList<EventImpl>();

  @XmlElementWrapper(name = "dates")
  @XmlElement(name = "date")
  @Transient
  List<Date> generatedDates;

  public RecurringEventImpl() {
  }

  public RecurringEventImpl(String xml) {
    try {
      RecurringEvent e = RecurringEventImpl.valueOf(xml);
      this.setRecurringEventId(e.getRecurringEventId());
      this.setMetadata(e.getMetadata());
      this.setRecurrence(e.getRecurrence());
    } catch (Exception e) {
      logger.warn("Could not parse Event XML {}", xml);
    }
  }

  // public RecurringEvent (String recurrence) {
  // setRecurrence(recurrence);
  // }

  public RecurringEventImpl(String recurrence, List<Metadata> metadata) {
    this(recurrence);
    setMetadata(metadata);
  }

  public RecurringEventImpl(String recurrence, String rEventId) {
    this(recurrence);
    setRecurringEventId(rEventId);
  }

  public RecurringEventImpl(String recurrence, String rEventId, List<Metadata> metadata) {
    this(recurrence, metadata);
    setRecurringEventId(rEventId);
  }

  @Override
  public String getRecurringEventId() {
    if( rEventId == null){
      return generateRecurringEventID();
    }
    return rEventId;
  }

  @Override
  public void setRecurringEventId(String rEventId) {
    this.rEventId = rEventId;
  }

  @Override
  public String generateRecurringEventID() {
    return rEventId = generateId();
  }

  @Override
  public String getRecurrence() {
    return recurrence;
  }

  @Override
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

  @Override
  public List<Metadata> getMetadata() {
    List<Metadata> list = new LinkedList<Metadata>();
    for(Metadata m :metadata) list.add(m);
    return list;
  }

  @Override
  public void setMetadata(List<Metadata> metadata) {
    metadataTable = null;
    this.metadata = new LinkedList<MetadataImpl>();
    for(Metadata m :metadata) this.metadata.add((MetadataImpl) m);
  }

  @Override
  public List<Event> generatedEvents() {
    if (this.generatedEvents != null && this.generatedEvents.size() > 0){
      List<Event> list = new LinkedList<Event>();
      for(Event e : generatedEvents) list.add(e);
      return list;
    }
    this.generatedEvents = new LinkedList<EventImpl>();
    generateDates();

    for (int i = 0; i < this.generatedDates.size(); i++) {
      EventImpl event = new EventImpl();
      event.generateId();
      event.setEntityManagerFactory(emf);
      event.setPositionInRecurrence(i);
      event.setRecurringEventId(getRecurringEventId());
      String title = "Episode";
      try {
        if (getValue("title") != null)
          title = getValue("title");
      } catch (IncompleteDataException e) {
        logger.warn("Could not get default title for series");
      }
      event.addMetadata(new MetadataImpl("title", title + " " + (i + 1)));
      logger.debug("Recur event: {}", event);
      this.generatedEvents.add(event);
    }
    
    List<Event> list = new LinkedList<Event>();
    for(Event e : generatedEvents) list.add(e);
    return list;
  }

  @Override
  public void generateDates() {
    generatedDates = new LinkedList<Date>();
    if (metadataTable == null)
      buildMetadataTable(getMetadata());
    if (recurrence == null) {
      logger.warn("Could not generate events because of missing recurrence pattern");
      return;
    }
    try {
      Recur recur = new RRule(recurrence).getRecur();
      logger.debug("Recur: {}", recur);
      Date start = getValueAsDate("recurrenceStart");
      logger.debug("Recur start: {}", start);
      if (start == null)
        start = new Date(System.currentTimeMillis());
      Date end = getValueAsDate("recurrenceEnd");
      logger.debug("Recur end: {}", end);
      if (end == null) {
        logger.error("No end date specified for recurring event {}. " + rEventId);
        return;
      }

      DateTime seed = new DateTime(start.getTime());
      seed.setUtc(true);
      DateTime period = new DateTime(end.getTime());
      period.setUtc(true);
      DateList dates = recur.getDates(seed, period, Value.DATE_TIME);
      logger.debug("DateList: {}", dates);
      for (Object date : dates) {
        // Dates in the DateList do not have times. Add the start time to the date so we know what time to start as well
        // as what day.
        Date d = (Date) date;
        generatedDates.add(d);
      }
    } catch (ParseException e) {
      logger.error("Could not parse recurrence {}.", recurrence);
      return;
    }
  }

  @Override
  public List<Event> getEvents() {
    List<Event> events = generatedEvents();
    for (Event e : events){
      e.setEntityManagerFactory(emf);
    }
    return events;
  }

  @Override
  public Date getDateForEventByIndex(int index) {
    if (generatedDates == null)
      generateDates();
    if (generatedDates.size() < index) {
      logger.warn("trying to get date that does not exist: index {} of {}", index, generatedDates.size());
    }
    return generatedDates.get(index);
  }

  @Override
  public Date getValueAsDate(String key) {
    if (metadataTable == null)
      buildMetadataTable(getMetadata());
    try {
      return super.getValueAsDate(key);
    } catch (IncompleteDataException e) {
      logger.warn("MetadataTable could not be build");
      return null;
    }
  }

  @Override
  public boolean containsKey(String key) {
    if (metadataTable == null)
      buildMetadataTable(getMetadata());
    try {
      return super.containsKey(key);
    } catch (IncompleteDataException e) {
      logger.warn("MetadataTable could not be build");
      return false;
    }
  }

  @Override
  public void update(RecurringEvent e) {
    // eliminate removed keys
    if (metadataTable == null) {
      buildMetadataTable(getMetadata());
    }
    try {
      for (String key : getKeySet()) {
        if (!e.containsKey(key)) {
          for (int i = 0; i < getMetadata().size(); i++) {
            if (getMetadata().get(i).getKey().equals(key)) {
              getMetadata().remove(i);
              break; // skip rest of the loop if found
            }
          }
        }
      }
      metadataTable = null;

      generateDates();
      List<Date> oldDates = generatedDates;
      if (!getRecurrence().equals(e.getRecurrence())) {
        setRecurrence(e.getRecurrence());
      }
      generateDates();
      if (oldDates != null && generatedDates != null && oldDates.size() != generatedDates.size()) {
        // TODO What happens when recurrence pattern changed??
        logger.info("adding / deleting events by changing pattern currently not implemented");
      }

      for (Event event : generatedEvents) {
        ((EventImpl)event).deleteMetadataTable(); // cached metadata needs to be resetted
      }

    } catch (IncompleteDataException e1) {
      logger.warn("Metadata could not be processed for recurring event {}. Recurring event could not be updated.",
              getRecurringEventId());
      return;
    }

    // update the list
    for (Metadata data : e.getMetadata()) {
      if (containsKey(data.getKey())) {
        for (Metadata dataOld : getMetadata()) {
          if (dataOld.getKey().equals(data.getKey()) && !dataOld.equals(data)) {
            dataOld.setValue(data.getValue());
            break;
          }
        }
      } else {
        this.metadata.add((MetadataImpl)data);
      }
    }
    setRecurrence(e.getRecurrence());
    metadataTable = null;
    // currently there is no reason to assume that ID or the parent recurringEvent can change;
  }

  public static RecurringEventImpl find(String recurringEventId, EntityManagerFactory emf) {
    logger.debug("loading recurring event with the ID {}", recurringEventId);
    if (recurringEventId == null || emf == null) {
      logger.warn("could not find reccuring event {}. Null Pointer exeption", recurringEventId);
      return null;
    }
    EntityManager em = emf.createEntityManager();
    RecurringEventImpl e = null;
    try {
      e = em.find(RecurringEventImpl.class, recurringEventId);
    } finally {
      em.close();
    }
    e.setEntityManagerFactory(emf);
    return e;
  }

  @Override
  public String toString() {
    String result = "Recurring Event " + rEventId + ", pattern: " + recurrence + ", generated events: "
            + System.getProperty("line.separator");
    for (Event e : generatedEvents)
      result += e.toString() + System.getProperty("line.separator");
    return result;
  }

  @Override
  public void updateMetadata(Metadata data) {
    if (containsKey(data.getKey())) {
      for (Metadata olddata : getMetadata()) {
        if (olddata.getKey().equals(data.getKey())) {
          olddata.setValue(data.getValue());
          break;
        }
      }
    } else {
      this.metadata.add((MetadataImpl)data);
    }
    metadataTable = null;
  }

  /**
   * valueOf function is called by JAXB to bind values. This function calls the ScheduleEvent factory.
   * 
   * @param xmlString
   *          string representation of an event.
   * @return instantiated event SchdeulerEventJaxbImpl.
   */
  public static RecurringEventImpl valueOf(String xmlString) throws Exception {
    return SchedulerBuilder.getInstance().parseRecurringEvent(xmlString);
  }

  @Override
  public String generateId() {
    return rEventId = super.generateId();
  }

  /**
   * removes a generated event from the list of events that belong to this recurring event. If an event is removed the
   * date at which this event would have happened will not be recorded!
   * 
   * @param e
   *          the event that should be removed
   * @return true if the event could be deleted
   */
  @Override
  public boolean removeEvent(Event e) {
    metadataTable = null;
    return generatedEvents.remove(e);
  }

  @Override
  public void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
    for (Event e : generatedEvents) {
      e.setEntityManagerFactory(emf);
    }
  }

  @Override
  public void addMetadata(Metadata m){
    this.metadata.add((MetadataImpl)m);
  }
  
  @Override
  public void removeMetadata(Metadata m){
    this.metadata.remove(m);
  }

}
