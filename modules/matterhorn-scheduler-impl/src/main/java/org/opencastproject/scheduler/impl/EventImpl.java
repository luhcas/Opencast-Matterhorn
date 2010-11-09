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
import org.opencastproject.scheduler.api.IncompleteDataException;
import org.opencastproject.scheduler.api.Metadata;
import org.opencastproject.scheduler.endpoint.SchedulerBuilder;

import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.RRule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
/**
 * An Event has a unique ID, a relation to the recurring event from which it was created and a set of metadata. Even the
 * start- and end-time is stored in the set of metadata, with the keys "timeStart" and "timeEnd" as long value
 * converted to string. Resources and Attendees are store in the metadata too, as
 */

@NamedQueries( { @NamedQuery(name = "Event.getAll", query = "SELECT e FROM Event e") })
@XmlRootElement(name = "event")
@XmlAccessorType(XmlAccessType.NONE)
@Entity(name = "Event")
@Access(AccessType.FIELD)
@Table(name = "SCHED_EVENT")
public class EventImpl implements Event {
  
  @XmlAttribute(name="id")
  @Id
  @GeneratedValue
  @Column(name = "EVENT_ID", length = 36)
  protected Long eventId;
  
  @XmlElement(name="contributor")
  protected String contributor;
  @XmlElement(name="creator")
  protected String creator;
  @XmlElement(name="description")
  @Lob
  @Column
  protected String description;
  @XmlElement(name="device")
  protected String device;
  @XmlElement(name="duration")
  protected long duration;
  @XmlElement(name="endDate")
  @XmlJavaTypeAdapter(value=DateAdapter.class, type=Date.class)
  @Temporal(TemporalType.TIMESTAMP)
  protected Date   endDate;
  @XmlElement(name="language")
  protected String language;
  @XmlElement(name="license")
  protected String license;
  @XmlElement(name="recurrence")
  protected String recurrence;
  @XmlElement(name="recurrencePattern")
  protected String recurrencePattern;
  @XmlElement(name="resources")
  protected String resources;
  @XmlElement(name="series")
  protected String series;
  @XmlElement(name="seriesId")
  protected String seriesId;
  @XmlElement(name="startDate")
  @XmlJavaTypeAdapter(value=DateAdapter.class, type=Date.class)
  @Temporal(TemporalType.TIMESTAMP)
  protected Date   startDate;
  @XmlElement(name="subject")
  protected String subject;
  @XmlElement(name="title")
  protected String title;
  
  // FIXME: Do we really need a join table here? How about a composite key (event id + metadata key) in the metadata
  // table?
  @XmlElementWrapper(name = "additionalMetadata")
  @XmlElement(name = "metadata")
  @OneToMany(fetch = FetchType.EAGER, targetEntity = MetadataImpl.class, cascade = CascadeType.ALL)
  @JoinTable(name = "SCHED_EVENT_METADATA", joinColumns = { @JoinColumn(name = "EVENT_ID") },
             inverseJoinColumns = { @JoinColumn(name = "METADATA_ID") })
  protected List<MetadataImpl> additionalMetadata = new LinkedList<MetadataImpl>();
  
  public EventImpl() {}
  
  public EventImpl(String xml) {
    try {
      EventImpl e = EventImpl.valueOf(xml);
      this.setEventId(e.getEventId());
      initializeFromEvent(e);
    } catch (Exception e) {
      logger.warn("Could not parse Event XML {}", xml);
    }
  }
  
  private static final Logger logger = LoggerFactory.getLogger(Event.class);
  
  /**
   * @return Event contributor
   */
  public String getContributor() {
    return contributor;
  }
  /**
   * @param Event contributor
   */
  public void setContributor(String contributor){
    this.contributor = contributor;
  }
  /**
   * @return Event creator
   */
  public String getCreator(){
    return creator;
  }
  /**
   * @param Event creator
   */
  public void setCreator(String creator){
    this.creator = creator;
  }
  /**
   * @return Event description
   */
  public String getDescription(){
    return description;
  }
  /**
   * @param Event description
   */
  public void setDescription(String description){
    this.description = description;
  }
  /**
   * @return Event capture device name
   */
  public String getDevice(){
    return device;
  }
  /**
   * @param Event capture device name
   */
  public void setDevice(String device){
    this.device = device;
  }
  /**
   * @return Event duration
   */
  public long getDuration(){
    if(duration == 0 && endDate != null && startDate != null){
      duration = endDate.getTime() - startDate.getTime();
    }
    return duration;
  }
  /**
   * @param Event duration
   */
  public void setDuration(long duration){
    this.duration = duration;
  }
  /**
   * @return Event end date
   */
  public Date getEndDate(){
    return endDate;
  }
  /**
   * @param Event end date
   */
  public void setEndDate(Date endDate){
    this.endDate = endDate;
  }
  /**
   * @return Event id
   */
  public Long getEventId(){
    return eventId;
  }
  /**
   * @param eventId
   */
  public void setEventId(Long eventId){
    this.eventId = eventId;
  }
  /**
   * @return Event langauge
   */
  public String getLanguage(){
    return language;
  }
  /**
   * @param Event languge
   */
  public void setLanguage(String language){
    this.language = language;
  }
  /**
   * @return Event license
   */
  public String getLicense(){
    return license;
  }
  /**
   * @param Event license
   */
  public void setLicense(String license){
    this.license = license;
  }
  /**
   * @return Event recurrence name
   */
  public String getRecurrence(){
    return recurrence;
  }
  /**
   * @param Event recurrence name
   */
  public void setRecurrence(String recurrence){
    this.recurrence = recurrence;
  }
  /**
   * @return Event recurrence pattern
   */
  public String getRecurrencePattern(){
    return recurrencePattern;
  }
  /**
   * @param Event recurrence pattern
   */
  public void setRecurrencePattern(String recurrence){
    this.recurrencePattern = recurrence;
  }
  /**
   * @return Capture agent resources
   */
  public String getResources(){
    return resources;
  }
  /**
   * @param Capture agent resources
   */
  public void setResources(String resources){
    this.resources = resources;
  }
  /**
   * @return Event series name
   */
  public String getSeries(){
    return series;
  }
  /**
   * @param Event series name
   */
  public void setSeries(String series){
    this.series = series;
  }
  /**
   * @return Event series id
   */
  public String getSeriesId(){
    return seriesId;
  }
  /**
   * @param Event series id
   */
  public void setSeriesId(String seriesId){
    this.seriesId = seriesId;
  }
  /**
   * @return Event start date
   */
  public Date getStartDate(){
    return startDate;
  }
  /**
   * @param Event start date
   */
  public void setStartDate(Date startDate){
    this.startDate = startDate;
  }
  /**
   * @return String Event subject
   */
  public String getSubject(){
    return subject;
  }
  /**
   * @param Event subject
   */
  public void setSubject(String subject){
    this.subject = subject;
  }
  /**
   * @return String Event title
   */
  public String getTitle(){
    return title;
  }
  /**
   * @param Event title
   */
  public void setTitle(String title){
    this.title = title;
  }
  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.opencastproject.scheduler.api.Event#updateMetadata()
   */
  public void updateMetadata(Metadata data) {
    Metadata found = findMetadata(data.getKey());
    if (found != null) {
      found.setValue(data.getValue());
    } else {
      additionalMetadata.add((MetadataImpl)data);
    }
  }
  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.opencastproject.scheduler.api.Event#getMetadata()
   */
  public List<Metadata> getMetadataList() {
    List<Metadata> events = new LinkedList<Metadata>();
    events.addAll(additionalMetadata);
    return events;
  }
  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.opencastproject.scheduler.api.Event#setMetadata()
   */
  public void setMetadataList(List<Metadata> metadata) {
    for(Metadata m : metadata){
      this.additionalMetadata.add((MetadataImpl)m);
    }
  }
  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.opencastproject.scheduler.api.Event#getValue()
   */
  public String getMetadataValueByKey(String key) {
    Metadata m = this.findMetadata(key);
    if(m != null) {
      return m.getValue();
    }
    return null;
  }
  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.opencastproject.scheduler.api.Event#getKeySet()
   */
  public Set<String> getKeySet() {
    //TODO
    return null;
  }
  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.opencastproject.scheduler.api.Event#containsKey()
   */
  public boolean containsKey(String key) {
    for(Metadata m : additionalMetadata){
      if(m.getKey().equals(key)){
        return true;
      }
    }
    return false;
  }
  
  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.opencastproject.scheduler.api.Event#findMetadata()
   */
  public Metadata findMetadata(String key) {
    for (Metadata m : additionalMetadata) {
      if (m.getKey().equals(key)){
        return m;
      }
    }
    return null;
  }
  
  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.opencastproject.scheduler.api.Event#update()
   */
  public void update(Event e) {
    this.setEventId(e.getEventId());
    this.setCreator(e.getCreator());
    this.setContributor(e.getContributor());
    this.setDescription(e.getDescription());
    this.setDevice(e.getDevice());
    this.setDuration(e.getDuration());
    this.setEndDate(e.getEndDate());
    this.setLanguage(e.getLanguage());
    this.setLicense(e.getLicense());
    this.setRecurrence(e.getRecurrence());
    this.setRecurrencePattern(e.getRecurrencePattern());
    this.setResources(e.getResources());
    this.setSeries(e.getSeries());
    this.setStartDate(e.getStartDate());
    this.setSubject(e.getSubject());
    this.setTitle(e.getTitle());
    // eliminate removed keys
    for (Metadata m : getMetadataList()) {
      if (e.findMetadata(m.getKey()) == null) {
        removeMetadata(m);
      }
    }
    // update the list
    for (Metadata data : e.getMetadataList()) {
      Metadata found = findMetadata(data.getKey());
      if (found != null) {
        found.setValue(data.getValue());
      } else {
        addMetadata(data);
      }
    }
  }
  
  public String toString() {
    //TODO
    return "";
  }
  
  /**
   * valueOf function is called by JAXB to bind values. This function calls the ScheduleEvent factory.
   * 
   * @param xmlString
   *          string representation of an event.
   * @return instantiated event SchdeulerEventJaxbImpl.
   */
  public static EventImpl valueOf(String xmlString) throws Exception {
    return SchedulerBuilder.getInstance().parseEvent(xmlString);
  }
  
  
  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((eventId == null) ? 0 : eventId.hashCode());
    return result;
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    EventImpl other = (EventImpl) obj;
    if (eventId == null) {
      if (other.eventId != null)
        return false;
    } else if (!eventId.equals(other.eventId))
      return false;
    return true;
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.opencastproject.scheduler.api.Event#addMetadata()
   */
  public void addMetadata(Metadata m){
    this.additionalMetadata.add((MetadataImpl)m);
  }
  
  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.opencastproject.scheduler.api.Event#removeMetadata()
   */
  public void removeMetadata(Metadata m){
    this.additionalMetadata.remove(m);
  }
  
  public String generateId() {
    return UUID.randomUUID().toString();
  }
  
  public void initializeFromEvent(Event e) {
    this.setCreator(e.getCreator());
    this.setContributor(e.getContributor());
    this.setDescription(e.getDescription());
    this.setDevice(e.getDevice());
    this.setDuration(e.getDuration());
    this.setEndDate(e.getEndDate());
    this.setLanguage(e.getLanguage());
    this.setLicense(e.getLicense());
    this.setRecurrence(e.getRecurrence());
    this.setRecurrencePattern(e.getRecurrencePattern());
    this.setResources(e.getResources());
    this.setSeries(e.getSeries());
    this.setStartDate(e.getStartDate());
    this.setSubject(e.getSubject());
    this.setTitle(e.getTitle());
    this.setMetadataList(e.getMetadataList());
  }
  
  public List<Event> createEventsFromRecurrence() throws ParseException, IncompleteDataException {
    if(getRecurrencePattern() == null || getRecurrencePattern().isEmpty()){
      throw new IncompleteDataException("Event has no recurrence pattern.");
    }
    Recur recur = new RRule(getRecurrencePattern()).getRecur();
    Date start = getStartDate();
    if (start == null) {
      start = new Date(System.currentTimeMillis());
    }
    Date end = getEndDate();
    if (end == null) {
      throw new IncompleteDataException("Event has no end date.");
    }
    DateTime seed = new DateTime(start.getTime());
    seed.setUtc(true);
    DateTime period = new DateTime(end.getTime());
    period.setUtc(true);
    DateList dates = recur.getDates(seed, period, Value.DATE_TIME);
    logger.debug("DateList: {}", dates);
    List<Event> events = new LinkedList<Event>();
    int i = 1;
    for (Object date : dates) {
      Date d = (Date) date;
      //Adjust for DST, if start of event 
      //Create timezone based on CA's reported TZ.
      TimeZone tz = null;
      if(!this.getMetadataValueByKey("agentTimeZone").isEmpty()){
        tz = TimeZone.getTimeZone(this.getMetadataValueByKey("agentTimeZone"));
      }
      if(tz == null){ //No timezone was present, assume the serve's local timezone.
        tz = TimeZone.getDefault();
      }
      if(tz.inDaylightTime(seed)){ //Event starts in DST
        if(!tz.inDaylightTime(d)){//Date not in DST?
          d.setTime(d.getTime() + tz.getDSTSavings()); //Ajust for Fall back one hour
        }
      }else{ //Event doesn't start in DST
        if(tz.inDaylightTime(d)){
          d.setTime(d.getTime() - tz.getDSTSavings()); //Adjust for Spring forward one hour
        }
      }
      Event event = new EventImpl();
      event.initializeFromEvent((Event)this);
      event.setTitle(getTitle() + " " + i);
      event.setStartDate(d);
      d.setTime(d.getTime() + getDuration());
      event.setEndDate(d);
      events.add((Event)event);
      i++;
    }
    return events;
  }
  
}
