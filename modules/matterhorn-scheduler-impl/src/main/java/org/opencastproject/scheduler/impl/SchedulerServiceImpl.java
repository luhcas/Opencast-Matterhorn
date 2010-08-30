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

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.persistence.spi.PersistenceProvider;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.ValidationException;
import org.opencastproject.scheduler.api.Event;
import org.opencastproject.scheduler.api.RecurringEvent;
import org.opencastproject.scheduler.api.SchedulerFilter;
import org.opencastproject.scheduler.impl.CalendarGenerator;
import org.opencastproject.scheduler.impl.IncompleteDataException;

import org.opencastproject.series.api.SeriesService;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of the Scheduler service based on JPA. This version knows about series too.
 *
 */
public class SchedulerServiceImpl implements ManagedService{
  
  private static final Logger logger = LoggerFactory.getLogger(SchedulerServiceImpl.class);

  protected PersistenceProvider persistenceProvider;
  protected Map<String, Object> persistenceProperties;
  protected EntityManagerFactory emf = null;
    
  /**
   * The component context that is passed when activate is called
   */
  protected ComponentContext componentContext;  
  
  protected DublinCoreGenerator dcGenerator;
  protected CaptureAgentMetadataGenerator caGenerator;
  protected SeriesService seriesService;
  
  private long updated = System.currentTimeMillis();
  private long updatedCalendar = 0;
  private long updatedAllEvents = 0;
  
  private Hashtable<String, String> calendars;
  private List<Event> cachedEvents;
  
  /** 
   * Properties that are updated by ManagedService updated method
   */
  @SuppressWarnings("unchecked")
  protected Dictionary properties;
  
  /**
   * This method will be called, when the bundle gets loaded from OSGI
   * @param componentContext The ComponetnContext of the OSGI bundle
   */
  public void activate(ComponentContext componentContext) {
    emf = persistenceProvider.createEntityManagerFactory("org.opencastproject.scheduler.impl", persistenceProperties);
    logger.info("SchedulerService activated.");
    if (componentContext == null) {
      logger.warn("Could not activate because of missing ComponentContext");
      return;
    }
    this.componentContext = componentContext;
    URL dcMappingURL = componentContext.getBundleContext().getBundle().getResource("config/dublincoremapping.properties");
    logger.debug("Using Dublin Core Mapping from {}.",dcMappingURL);
    try {
      if (dcMappingURL != null)  {
        URLConnection con = dcMappingURL.openConnection();
        dcGenerator = new DublinCoreGenerator(con.getInputStream());
      }
    } catch (IOException e) {
      logger.error("Could not open URL connection to Dublin Core Mapping File after activation");
    }
    
    URL caMappingURL = componentContext.getBundleContext().getBundle().getResource("config/captureagentmetadatamapping.properties");
    logger.debug("Using Capture Agent Metadata Mapping from {}.", caMappingURL);
    try {
      if (caMappingURL != null) {
        URLConnection con = caMappingURL.openConnection();
        caGenerator = new CaptureAgentMetadataGenerator(con.getInputStream());
      }
    } catch (IOException e) {
      logger.error("Could not open URL connection to Capture Agent Metadata Mapping File after activation");
    }
  } 
  
  public Map<String, Object> getPersistenceProperties() {
    return persistenceProperties;
  }

  public void setPersistenceProperties(Map<String, Object> persistenceProperties) {
    this.persistenceProperties = persistenceProperties;
  }
  
  public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }
  
  public PersistenceProvider getPersistenceProvider() {
    return persistenceProvider;
  }
 
  /**
   * Sets a DublinCoreGenerator
   * @param dcGenerator The DublinCoreGenerator that should be used
   */
  public void setDublinCoreGenerator(DublinCoreGenerator dcGenerator) {
    this.dcGenerator = dcGenerator;
  }

  /**
   * Sets the CaptureAgentMetadataGenerator 
   * @param caGenerator The CaptureAgentMetadataGenerator that should be used
   */
  public void setCaptureAgentMetadataGenerator(CaptureAgentMetadataGenerator caGenerator) {
    this.caGenerator = caGenerator;
  }

  /**
   * Persist an event
   * @param Event e
   * @return The event that has been persisted
   */
  public Event addEvent(Event e) {
    EntityManager em = emf.createEntityManager();
    EventImpl event = (EventImpl)e;
    event.setEntityManagerFactory(emf);
    try {
      EntityTransaction tx = em.getTransaction();
      tx.begin();
      em.persist(event);
      tx.commit();
    } finally {
      em.close();
      updated = System.currentTimeMillis();
    }
    
    return event;
  }
  
  /**
   * Persist a recurring event
   * @param RecurringEvent e
   * @return The recurring event that has been persisted
   */
  public RecurringEvent addRecurringEvent(RecurringEvent e) {
    EntityManager em = emf.createEntityManager();
    RecurringEventImpl event = (RecurringEventImpl)e;
    event.setEntityManagerFactory(emf);
    event.generatedEvents();
    try {
      EntityTransaction tx = em.getTransaction();
      tx.begin();
      em.persist(event);
      tx.commit();
    } finally {
      em.close();
      updated = System.currentTimeMillis();
    }
    
    return e;
  }
  
  /**
   * @param eventID
   * @return An event that matches eventID
   */
  public Event getEvent(String eventID) {
    return EventImpl.find(eventID, emf);
  }  

  /**
   * @param recurringEventID
   * @return Returing recurring event matching recurringEventID, or null if not found
   */
  public RecurringEvent getRecurringEvent(String recurringEventID) {
    logger.debug("loading recurring event with the ID {}", recurringEventID);
    EntityManager em = emf.createEntityManager();
    RecurringEvent e = null;
    try {
       e = em.find(RecurringEventImpl.class, recurringEventID);
    } finally {
      em.close();
      updated = System.currentTimeMillis();
    }
    e.setEntityManagerFactory(emf);
    return e;
  }  
  
  /**
   * @param filter
   * @return List of events that match the supplied filter, or all events if no filter is supplied
   */
  public List<Event> getEvents (SchedulerFilter filter) {
    if (updatedCalendar < updated) calendars = new Hashtable<String, String>(); // reset all calendars, if data has been changed 
    if (filter == null) {
      logger.debug("returning all events");
      return getAllEvents();
    }
    List<Event> events = new LinkedList<Event>();
    // catch the case that the event id is given, what may be unrealistic
    if (filter.getEventIDFilter() != null && filter.getEventIDFilter().length() > 0) {
      Event e = getEvent(filter.getEventIDFilter());
      if (e != null) {
        logger.debug("using only single event with id {}.", filter.getEventIDFilter());
        events.add(e);
      } 
    } else {
        // all other cases
        events = new LinkedList<Event>(getAllEvents());
        logger.debug("using all {} events.", events.size());
    }
    
    // filter for device
    if (filter.getDeviceFilter() != null && filter.getDeviceFilter().length() > 0) {
      events = filterEventsForExactValue(events, "device", filter.getDeviceFilter());
      logger.debug("filtered for device. {} events left.", events.size());
    }
    
    // filter for title
    if (filter.getTitleFilter() != null && filter.getTitleFilter().length() > 0) {
      events = filterEvents(events, "title", filter.getTitleFilter());
      logger.debug("filtered for Title. {} events left.", events.size());
    }
    
    // filter for creator
    if (filter.getCreatorFilter() != null && filter.getCreatorFilter().length() > 0) {
      events = filterEvents(events, "creator", filter.getCreatorFilter());
      logger.debug("filtered for creator. {} events left.", events.size());
    }
    
    // filter for abstract
    if (filter.getAbstractFilter() != null && filter.getAbstractFilter().length() > 0) {
      events = filterEvents(events, "abstract", filter.getAbstractFilter());
      logger.debug("filtered for abstract. {} events left.", events.size());
    }
    
    // filter for contributor
    if (filter.getContributorFilter() != null && filter.getContributorFilter().length() > 0) {
      events = filterEvents(events, "contributor", filter.getContributorFilter());
      logger.debug("filtered for contributor. {} events left.", events.size());
    }
    
    // filter for location
    if (filter.getLocationFilter() != null && filter.getLocationFilter().length() > 0) {
      events = filterEventsForExactValue(events, "location", filter.getLocationFilter());
      logger.debug("filtered for location. {} events left.", events.size());
    }
    
    // filter for series
    if (filter.getSeriesIDFilter() != null && filter.getSeriesIDFilter().length() > 0) {
      events = filterEventsForExactValue(events, "series-id", filter.getSeriesIDFilter());
      logger.debug("filtered for series. {} events left.", events.size());
    }
    
    // filter for channel
    if (filter.getChannelIDFilter() != null && filter.getChannelIDFilter().length() > 0) {
      events = filterEvents(events, "channel-id", filter.getChannelIDFilter());
      logger.debug("filtered for channel. {} events left.", events.size());
    }
    
    // filter for resources
    if (filter.getResourceFilter() != null && filter.getResourceFilter().length() > 0) {
      events = filterEvents(events, "resources", filter.getResourceFilter());
      logger.debug("filtered for resources. {} events left.", events.size());
    }
    
    // filter for attendees
    if (filter.getAttendeeFilter() != null && filter.getAttendeeFilter().length() > 0) {
      events = filterEvents(events, "attendes", filter.getAttendeeFilter());
      logger.debug("filtered for attendees. {} events left.", events.size());
    }
    
    // filter for later Dates
    if (filter.getStart() != null && filter.getStart().getTime() > 0) {
      events = filterEventsForAfterDate(events, filter.getStart());
      logger.debug("Setting start date. {} events left.", events.size());
    }
    
    // filter for later Dates
    if (filter.getEnd() != null && filter.getEnd().getTime() > 0) {
      events = filterEventsForBeforeDate(events, filter.getEnd());
      logger.debug("Setting end date. {} events left.", events.size());
    }
    
    return events;
  }  
  
  /**
   * Filters a list of events by a metadata field and removes any that do not contain key,
   *  and any that contains key by does not contain the value.
   * @param list
   * @param key
   * @param value
   * @return Filtered list of events
   */
  private List<Event> filterEvents (List<Event> list, String key, String value) {
    LinkedList<Event> marked = new LinkedList<Event>(); //needed because loop will not terminate correctly, if list is modified
    for (Event e: list) {
      if (! e.containsKey(key)) marked.add(e);
      else if (! e.getValue(key).contains(value)) marked.add(e);
    }
    for (Event e: marked) list.remove(e);
    
    return list;    
  }
  
  /**
   * Filters a list of events by metadata and removes any that does not contain key,
   * or any that contain key but do not equal value.
   * @param list
   * @param key
   * @param value
   * @return Filtered list of events
   */
  private List<Event> filterEventsForExactValue (List<Event> list, String key, String value) {
    LinkedList<Event> marked = new LinkedList<Event>(); //needed because loop will not terminate correctly, if list is modified
    for (Event e: list) {
      if (! e.containsKey(key)) marked.add(e);
      else if (! e.getValue(key).equals(value)) marked.add(e);
    }
    for (Event e: marked) list.remove(e);
    
    return list;
  }
  
  /**
   * Filters a list of events, removing any of those that start before provided date.
   * @param list
   * @param time
   * @return Filtered list of events
   */
  private List<Event> filterEventsForBeforeDate (List<Event> list, Date time) {
    if (time == null) return list;
    LinkedList<Event> marked = new LinkedList<Event>(); //needed because loop will not terminate correctly, if list is modified
    for (Event e: list) {
      if (e.getStartdate() == null || (! e.getStartdate().before(time))) marked.add(e);
    }
    for (Event e: marked) list.remove(e);
    
    return list;
  }  
  
  /**
   * Filters a list of recurring events, removing any of those that start before provided date.
   * @param list
   * @param time
   * @return Filtered list of recurring events
   */
  public List<RecurringEvent> filterRecurringEventsForBeforeDate (List<RecurringEvent> list, String key, Date time) {
    List<RecurringEvent> marked = new LinkedList<RecurringEvent>(); //needed because loop will not terminate correctly, if list is modified
    for (RecurringEvent e: list) {
      if (! e.containsKey(key)) marked.add(e);
      else if (! e.getValueAsDate(key).before(time)) marked.add(e);
    }
    for (RecurringEvent e: marked) list.remove(e);
    
    return list;
  }  
  
  /**
   * Filters a list of events, removing any of those that end after provided date
   * @param list
   * @param time
   * @return Filtered list of events
   */
  private List<Event> filterEventsForAfterDate (List<Event> list, Date time) {
    if (time == null) return list;
    List<Event> marked = new LinkedList<Event>(); //needed because loop will not terminate correctly, if list is modified
    for (Event e: list) {
      if (e.getEnddate() == null || (! e.getEnddate().after(time))) marked.add(e);    
    }
    for (Event e: marked) list.remove(e);
    
    return list;    
  }  
  
  /**
   * @return A list of all events
   */
  @SuppressWarnings("unchecked")
  public List<Event> getAllEvents () {
    if (updatedAllEvents > updated && cachedEvents != null) {
      return cachedEvents;
    }
    EntityManager em = emf.createEntityManager();
    Query query = em.createNamedQuery("Event.getAll");
    List<Event> events = null;
    try {
      events = query.getResultList();
    } finally {
      em.close();
    }
    for (Event e : events) e.setEntityManagerFactory(emf);
    cachedEvents = events;
    updatedAllEvents = System.currentTimeMillis();
    return cachedEvents;
  }
  
  /**
   * @return List of all recurring events
   */
  @SuppressWarnings("unchecked")
  public List<RecurringEvent> getAllRecurringEvents () {
    EntityManager em = emf.createEntityManager();
    Query query = em.createNamedQuery("RecurringEvent.getAll");
    List<RecurringEvent> events = null;
    try {
      events = query.getResultList();
    } finally {
      em.close();
    }
    for (RecurringEvent e : events) e.setEntityManagerFactory(emf);
    return events;
  }  
  
  /**
   * @return List of all events that start after the current time.
   */
  public List<Event> getUpcomingEvents() {
    SchedulerFilter upcoming = new SchedulerFilterImpl();
    upcoming.setStart(new Date(System.currentTimeMillis()));
    List<Event> events = getEvents(upcoming);
    return events;
  }
  
  /**
   * @param list
   * @return The list of events in a list of events that occur after the current time.
   */
  public List<Event> getUpcomingEvents(List<Event> list) {
    Date now = new Date(System.currentTimeMillis());
    for (Event e : list) {
      Date enddate = e.getEnddate();
      if (!(enddate == null) && ! enddate.after(now)) list.remove(e);
    }
    return list;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.impl.SchedulerServiceImpl#removeEvent(java.lang.String)
   */
  public boolean removeEvent(String eventID) {
    logger.info("Removing event with the ID {}", eventID);
    Event event;
    EntityManager em = emf.createEntityManager();
    try {
      em.getTransaction().begin();
      event = em.find(EventImpl.class, eventID);
      if (event == null) return false; // Event not in database
      String rEventID = event.getRecurringEventId();
      if (rEventID != null) { // remove Event from recuring Event list, if necessary
        RecurringEvent rEvent = em.find(RecurringEventImpl.class, rEventID);
        rEvent.removeEvent(event);
        em.merge(rEvent);
      }
      em.remove(event);
      
      em.getTransaction().commit();
    } finally {
      em.close();
      updated = System.currentTimeMillis();
    }
    return true; 
  }
  
  /**
   * @param rEventID
   * @return True if the reucrring event was removed
   */
  public boolean removeRecurringEvent(String rEventID) {
    logger.info("Removing recurring event with the ID {}", rEventID);
    RecurringEvent event;
    EntityManager em = emf.createEntityManager();
    try {
      em.getTransaction().begin();
      event = em.find(RecurringEventImpl.class, rEventID);
      if (event == null) return false; // Event not in database
      em.remove(event);
      em.getTransaction().commit();
    } finally {
      em.close();
      updated = System.currentTimeMillis();
    }
    return true; 
  }
  
  /**
   * @param e
   * @return True if the event was updated
   */
  public boolean updateEvent(Event e) {
    
    EntityManager em = emf.createEntityManager();
    try {
      em.getTransaction().begin();
      Event storedEvent =  getEvent(e.getEventId());
      logger.debug("Found stored event. {} -", storedEvent);
      if (storedEvent == null) return false; //nothing found to update
      storedEvent.setEntityManagerFactory(emf);
      storedEvent.update(e);
      em.merge(storedEvent);
      em.getTransaction().commit();
    } catch (Exception e1) {
      logger.warn("Could not update event {}. Reason: {}",e,e1.getMessage());
      return false;
    } finally {
      em.close();
      updated = System.currentTimeMillis();
    }
    return true;
  }  
  
  /**
   * @param e
   * @return True if the recurring event was updated
   */
  public boolean updateRecurringEvent(RecurringEvent e) {
    EntityManager em = emf.createEntityManager();
    try {
      em.getTransaction().begin();
      RecurringEvent storedEvent =  getRecurringEvent(e.getRecurringEventId());
      if (storedEvent == null) return false;
      storedEvent.setEntityManagerFactory(emf);
      storedEvent.update(e);      
      em.merge(storedEvent);
      em.getTransaction().commit();
    } finally {
      em.close();
      updated = System.currentTimeMillis();
    }
    return true;
  }  
  
  /**
   * @param e
   * @return A list of events that conflict with the start, or end dates of provided event.
   */
  public List<Event> findConflictingEvents (Event e) {
    logger.debug("finding conflicts for event {}.", e);
    List<Event> events = new LinkedList<Event>(getAllEvents());
    //reduce to device first
    events = filterEventsForExactValue(events, "device", e.getValue("device"));
    
    //all events that start at the same time or later
    long start = e.getStartdate().getTime() -1; // make sure that the same start time is included too;
    events = filterEventsForAfterDate(events, new DateTime(start));
    
    //all events that stop at the same time or earlier
    long end = e.getEnddate().getTime() + 1; // make sure that the same stop time is included too;
    events = filterEventsForBeforeDate(events, new DateTime(end));
    
    return events;
  }
  
  //TODO: Remove, or indicate that it is currently unused.
  
  /**
   * @param rEvent
   * @return A list of events that conflict with the provided recurring event
   * @throws IncompleteDataException
   */
  public List<Event> findConflictingEvents (RecurringEvent rEvent) throws IncompleteDataException {
    ((RecurringEventImpl)rEvent).buildMetadataTable(rEvent.getMetadata());
    if (rEvent.getRecurrence() == null || ((RecurringEventImpl)rEvent).getValue("recurrenceStart") == null || ((RecurringEventImpl)rEvent).getValue("device") == null ||
            ((RecurringEventImpl)rEvent).getValue("recurrenceEnd") == null || ((RecurringEventImpl)rEvent).getValue("recurrenceDuration") == null) 
      throw new IncompleteDataException();
    if (rEvent.getRecurringEventId() == null) rEvent.generateId();
    List<Event> events = rEvent.generatedEvents();
    LinkedList<Event> results = new LinkedList<Event>();
    for (Event event : events) {
      event.setRecurringEvent(rEvent);
      List<Event> conflicts = findConflictingEvents(event);
      results.addAll(conflicts);
    }
    
    return results;
  }  
  
  public void destroy() {
    emf.close();
  }
  
  /**
   * {@inheritDoc}
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  @Override
  @SuppressWarnings("unchecked")
  public void updated(Dictionary properties) throws ConfigurationException {
    this.properties = properties;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerService#getCalendarForCaptureAgent(java.lang.String)
   */
  public String getCalendarForCaptureAgent(String captureAgentID) {
    if (updatedCalendar > updated && calendars.containsKey(captureAgentID) && calendars.get(captureAgentID) != null) {
      logger.debug("Using cached calendar for {}", captureAgentID);
      return calendars.get(captureAgentID);
    } 
    if (updatedCalendar < updated) calendars = new Hashtable<String, String>(); // reset all calendars, if data has been changed 
    
    SchedulerFilter filter = getFilterForCaptureAgent (captureAgentID); 
    CalendarGenerator cal = new CalendarGenerator(dcGenerator, caGenerator, seriesService);
    List<Event> events = getEvents(filter);
    
    for (Event event : events) cal.addEvent(event);
    
    try {
      cal.getCalendar().validate();
    } catch (ValidationException e1) {
      logger.warn("Could not validate Calendar: {}", e1.getMessage());
    }
    
    String result = cal.getCalendar().toString(); // CalendarOutputter performance sucks (jmh)
    
    updatedCalendar = System.currentTimeMillis();
    calendars.put(captureAgentID, result);
    return result;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerService#getDublinCoreMetadata(java.lang.String)
   */
  public String getDublinCoreMetadata (String eventID) {
    Event event = getEvent(eventID);
    if (dcGenerator == null){
      logger.error("Dublin Core generator not initialized");
      return null;
    }
    return dcGenerator.generateAsString(event);
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerService#getDublinCoreMetadata(java.lang.String)
   */
  public String getCaptureAgentMetadata (String eventID) {
    Event event = getEvent(eventID);
    if (caGenerator == null){
      logger.error("Capture Agent Metadata generator not initialized");
      return null;
    }
    return caGenerator.generateAsString(event);
  }  
  
  /**
   * Sets the series service
   * @param s
   */
  public void setSeriesService (SeriesService s) {
    seriesService = s;
  }

  /**
   * @return An empty SchedulerFilter
   */
  public SchedulerFilter getNewSchedulerFilter () {
    return new SchedulerFilterImpl();
  }
  
  /**
   * @return An empty Event
   */
  public Event getNewEvent () {
    return new EventImpl();
  }
  
  /**
   * resolves the appropriate Filter for the Capture Agent 
   * @param captureAgentID The ID as provided by the capture agent 
   * @return the Filter for this capture Agent.
   */
  protected SchedulerFilter getFilterForCaptureAgent(String captureAgentID) {
    SchedulerFilter filter = new SchedulerFilterImpl();
    filter.setDeviceFilter(captureAgentID);
    filter.setOrderBy("time-desc");
    filter.setStart(new Date(System.currentTimeMillis()));
    return filter;
  }
}
