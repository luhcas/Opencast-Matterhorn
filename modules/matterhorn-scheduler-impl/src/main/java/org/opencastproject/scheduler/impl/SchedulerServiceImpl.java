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
import org.opencastproject.scheduler.api.SchedulerFilter;
import org.opencastproject.scheduler.api.IncompleteDataException;
import org.opencastproject.series.api.SeriesService;

import net.fortuna.ical4j.model.ValidationException;

import org.apache.commons.io.IOUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import java.text.ParseException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.EntityExistsException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;
import javax.persistence.spi.PersistenceProvider;

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
  @SuppressWarnings("rawtypes")
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
    InputStream is = null;
    try {
      if (dcMappingURL != null)  {
        URLConnection con = dcMappingURL.openConnection();
        is = con.getInputStream();
        dcGenerator = new DublinCoreGenerator(is);
      }
    } catch (IOException e) {
      logger.error("Could not open URL connection to Dublin Core Mapping File after activation");
    } finally {
      IOUtils.closeQuietly(is);
    }
    
    URL caMappingURL = componentContext.getBundleContext().getBundle().getResource("config/captureagentmetadatamapping.properties");
    logger.debug("Using Capture Agent Metadata Mapping from {}.", caMappingURL);
    try {
      if (caMappingURL != null) {
        URLConnection con = caMappingURL.openConnection();
        is = con.getInputStream();
        caGenerator = new CaptureAgentMetadataGenerator(is);
      }
    } catch (IOException e) {
      logger.error("Could not open URL connection to Capture Agent Metadata Mapping File after activation");
    } finally {
      IOUtils.closeQuietly(is);
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
  public Event addEvent(Event e) throws EntityExistsException {
    EntityManager em = emf.createEntityManager();
    EventImpl event = (EventImpl)e;
    EntityTransaction tx = em.getTransaction();
    tx.begin();
    em.persist(event);
    tx.commit();
    em.close();
    updated = System.currentTimeMillis();
    return event;
  }
  
  /**
   * Persist a recurring event
   * @param RecurringEvent e
   * @return The recurring event that has been persisted
   */
  public void addRecurringEvent(Event recurrence) throws ParseException, IncompleteDataException, EntityExistsException {
    EntityManager em = emf.createEntityManager();
    List<Event> events = recurrence.createEventsFromRecurrence();
    for(Event e : events) {
      logger.debug("Adding recurring event {}", e.getEventId());
      EntityTransaction tx = em.getTransaction();
      tx.begin();
      em.persist((EventImpl)e);
      tx.commit();
    }
    em.close();
    updated = System.currentTimeMillis();
  }

  
  /**
   * @param eventID
   * @return An event that matches eventID
   */
  public Event getEvent(String eventID) {
    return EventImpl.find(eventID, emf);
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

    EntityManager em = emf.createEntityManager();
    CriteriaBuilder builder = emf.getCriteriaBuilder();
    CriteriaQuery<EventImpl> query = builder.createQuery(EventImpl.class);
    Root<EventImpl> rootEvent = query.from(EventImpl.class);
    EntityType<EventImpl> Event_ = rootEvent.getModel();
    Predicate wherePred = builder.conjunction();
    
    ParameterExpression<String> creatorParam = null;
    if(filter.getCreatorFilter() != null && !filter.getCreatorFilter().isEmpty()){
      creatorParam = builder.parameter(String.class);
      wherePred = builder.and(wherePred, builder.like(rootEvent.get(Event_.getSingularAttribute("creator", String.class)), creatorParam));
    }
    
    ParameterExpression<String> deviceParam = null;
    if(filter.getDeviceFilter() != null && !filter.getDeviceFilter().isEmpty()){
      deviceParam = builder.parameter(String.class);
      wherePred = builder.and(wherePred, builder.like(rootEvent.get(Event_.getSingularAttribute("device", String.class)), deviceParam));
    }
    
    ParameterExpression<String> titleParam = null;
    if(filter.getTitleFilter() != null && !filter.getTitleFilter().isEmpty()){
      titleParam = builder.parameter(String.class);
      wherePred = builder.and(wherePred, builder.like(rootEvent.get(Event_.getSingularAttribute("title", String.class)), titleParam));
    }
    
    ParameterExpression<String> seriesParam = null;
    if(filter.getSeriesFilter() != null && !filter.getSeriesFilter().isEmpty()){
      seriesParam = builder.parameter(String.class);
      wherePred = builder.and(wherePred, builder.like(rootEvent.get(Event_.getSingularAttribute("series", String.class)), seriesParam));
    }
    
    ParameterExpression<Date> startParam = null;
    ParameterExpression<Date> stopParam = null;
    if(filter.getStart() != null && filter.getStop() != null) { // Events with dates between start and stop
      startParam = builder.parameter(Date.class);
      stopParam = builder.parameter(Date.class);
      wherePred = builder.between(rootEvent.get(Event_.getSingularAttribute("startDate", Date.class)), startParam, stopParam);
    } else if( filter.getStart() != null && filter.getStop() == null) { //All events with dates after start
      startParam = builder.parameter(Date.class);
      wherePred = builder.greaterThan(rootEvent.get(Event_.getSingularAttribute("startDate", Date.class)), startParam);
    } else if( filter.getStart() != null && filter.getStop() == null) { //All events with dates after start
      stopParam = builder.parameter(Date.class);
      wherePred = builder.lessThan(rootEvent.get(Event_.getSingularAttribute("endDate", Date.class)), stopParam);
    }
    
    query.where(wherePred);
    TypedQuery<EventImpl> eventQuery = em.createQuery(query);
    
    if(creatorParam != null){
      eventQuery.setParameter(creatorParam, filter.getCreatorFilter());
    }
    if(deviceParam != null){
      eventQuery.setParameter(deviceParam, filter.getDeviceFilter());
    }
    if(titleParam != null){
      eventQuery.setParameter(titleParam, filter.getTitleFilter());
    }
    if(seriesParam != null){
      eventQuery.setParameter(seriesParam, filter.getSeriesFilter());
    }
    if(startParam != null){
      eventQuery.setParameter(startParam, filter.getStart());
    }
    if(stopParam != null){
      eventQuery.setParameter(stopParam, filter.getStop());
    }
    
    List<EventImpl> results = eventQuery.getResultList();
    List<Event> returnList = new LinkedList<Event>();
    for(EventImpl event : results){
      returnList.add((Event)event);
    }
    return returnList;
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
    cachedEvents = events;
    updatedAllEvents = System.currentTimeMillis();
    return cachedEvents;
  }
  
  /**
   * @return List of all events that start after the current time.
   */
  public List<Event> getUpcomingEvents() {
    SchedulerFilter upcoming = new SchedulerFilter();
    upcoming.withStart(new Date(System.currentTimeMillis()));
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
      Date enddate = e.getEndDate();
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
   * @return A list of events that conflict with the start, or end dates of provided event.
   */
  public List<Event> findConflictingEvents (Event e) {
    return null;
  }
  
  public void destroy() {
    emf.close();
  }
  
  /**
   * {@inheritDoc}
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  @SuppressWarnings("rawtypes")
  @Override
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
    logger.debug("Events with CA '{}': {}", captureAgentID, events);
    for (Event event : events) {
      cal.addEvent(event);
    }
    
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
    SchedulerFilter filter = new SchedulerFilter();
    filter.withDeviceFilter(captureAgentID).withOrder("time-desc").withStart(new Date(System.currentTimeMillis()));
    return filter;
  }
  
}
