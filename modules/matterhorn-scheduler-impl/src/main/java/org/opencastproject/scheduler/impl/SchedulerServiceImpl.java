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

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.scheduler.api.Event;
import org.opencastproject.scheduler.api.IncompleteDataException;
import org.opencastproject.scheduler.api.Metadata;
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.scheduler.api.SchedulerFilter;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowService;

import net.fortuna.ical4j.model.ValidationException;

import org.apache.commons.io.IOUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.IllegalArgumentException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
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
public class SchedulerServiceImpl implements SchedulerService, ManagedService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(SchedulerServiceImpl.class);

  /** The metadata key used to store the workflow identifier in an event's metadata */
  public static final String WORKFLOW_INSTANCE_ID_KEY = "org.opencastproject.workflow.id";

  /** The metadata key used to store the workflow definition in an event's metadata */
  public static final String WORKFLOW_DEFINITION_ID_KEY = "org.opencastproject.workflow.definition";
  
  /** The schedule workflow operation identifier */
  public static final String SCHEDULE_OPERATION_ID = "schedule";

  /** The workflow operation property that stores the event start time, as milliseconds since 1970 */
  public static final String WORKFLOW_OPERATION_KEY_SCHEDULE_START = "schedule.start";

  /** The workflow operation property that stores the event stop time, as milliseconds since 1970 */
  public static final String WORKFLOW_OPERATION_KEY_SCHEDULE_STOP = "schedule.stop";

  /** The workflow operation property that stores the event location */
  public static final String WORKFLOW_OPERATION_KEY_SCHEDULE_LOCATION = "schedule.location";

  /** The JPA persistence provider */
  protected PersistenceProvider persistenceProvider;

  /** The JPA persistence properties */
  protected Map<String, Object> persistenceProperties;

  /** JPA entity manager factory */
  protected EntityManagerFactory emf = null;

  /** The component context that is passed when activate is called */
  protected ComponentContext componentContext;

  /** The dublin core generator */
  protected DublinCoreGenerator dcGenerator;

  /** The metadata generator to feed the capture agents */
  protected CaptureAgentMetadataGenerator caGenerator;

  /** The series service */
  protected SeriesService seriesService;

  /** The workflow service */
  protected WorkflowService workflowService;

  /**
   * Properties that are updated by ManagedService updated method
   */
  @SuppressWarnings("rawtypes")
  protected Dictionary properties;

  /**
   * This method will be called, when the bundle gets loaded from OSGI
   * 
   * @param componentContext
   *          The ComponetnContext of the OSGI bundle
   */
  public void activate(ComponentContext componentContext) {
    emf = persistenceProvider.createEntityManagerFactory("org.opencastproject.scheduler.impl", persistenceProperties);
    logger.debug("SchedulerService activating.");

    if (componentContext == null) {
      logger.warn("Could not activate because of missing ComponentContext");
      return;
    }
    this.componentContext = componentContext;
    URL dcMappingURL = componentContext.getBundleContext().getBundle()
            .getResource("config/dublincoremapping.properties");
    logger.debug("Using Dublin Core Mapping from {}.", dcMappingURL);
    InputStream is = null;
    try {
      if (dcMappingURL != null) {
        URLConnection con = dcMappingURL.openConnection();
        is = con.getInputStream();
        dcGenerator = new DublinCoreGenerator(is);
      }
    } catch (IOException e) {
      logger.error("Could not open URL connection to Dublin Core Mapping File after activation");
    } finally {
      IOUtils.closeQuietly(is);
    }

    URL caMappingURL = componentContext.getBundleContext().getBundle()
            .getResource("config/captureagentmetadatamapping.properties");
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
  
  public WorkflowDefinition getPreProcessingWorkflowDefinition() throws IllegalStateException {
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream("/scheduler-workflow-definition.xml");
      return WorkflowBuilder.getInstance().parseWorkflowDefinition(in);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to load the preprocessing workflow definition", e);
    } finally {
      IOUtils.closeQuietly(in);
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
   * 
   * @param dcGenerator
   *          The DublinCoreGenerator that should be used
   */
  public void setDublinCoreGenerator(DublinCoreGenerator dcGenerator) {
    this.dcGenerator = dcGenerator;
  }

  /**
   * Sets the CaptureAgentMetadataGenerator
   * 
   * @param caGenerator
   *          The CaptureAgentMetadataGenerator that should be used
   */
  public void setCaptureAgentMetadataGenerator(CaptureAgentMetadataGenerator caGenerator) {
    this.caGenerator = caGenerator;
  }

  /**
   * Persist an event
   * 
   * @param event
   *          the event to add
   * 
   * @return The event that has been persisted
   */
  public Event addEvent(Event event) throws SchedulerException {
    EntityManager em = null;
    EntityTransaction tx = null;

    // Start a workflow so we have an event id that we can associate the event with
    WorkflowInstance workflow = null;
    try {
      workflow = startWorkflowInstance(event);
    } catch (WorkflowDatabaseException workflowException) {
      throw new SchedulerException(workflowException);
    } catch (MediaPackageException mediaPackageException) {
      throw new SchedulerException(mediaPackageException);
    }
    
    for(Metadata m : event.getMetadataList()){
    	m.setEvent(event);
    }
    
    try {
      event.setEventId(workflow.getId());
      em = emf.createEntityManager();
      tx = em.getTransaction();
      event = (EventImpl) event;
      tx.begin();
      em.persist(event);
      tx.commit();
    } catch (Exception ex) {
      if (tx != null) {
        tx.rollback();
      }
      throw new SchedulerException("Unable to add event: {}", ex);
    } finally {
      if (em != null) {
        em.close();
      }
    }
    return event;
  }

  /**
   * Starts a workflow to track this scheduled event.
   * 
   * @param event
   *          the scheduled event
   * @return the workflow instance
   * @throws WorkflowDatabaseException
   *           if the workflow can not be created
   * @throws MediaPackageException
   *           if the mediapackage can not be created
   */
  public WorkflowInstance startWorkflowInstance(Event event) throws WorkflowDatabaseException, MediaPackageException {
    // Build a mediapackage using the event metadata
    MediaPackage mediapackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    mediapackage.setTitle(event.getTitle());
    mediapackage.setLanguage(event.getLanguage());
    mediapackage.setLicense(event.getLicense());
    mediapackage.setSeries(event.getSeriesId());
    mediapackage.setSeriesTitle(event.getSeries());

    // Build a properties set for this event
    Map<String, String> properties = new HashMap<String, String>();
    properties.put(WORKFLOW_OPERATION_KEY_SCHEDULE_START, Long.toString(event.getStartDate().getTime()));
    properties.put(WORKFLOW_OPERATION_KEY_SCHEDULE_STOP, Long.toString(event.getEndDate().getTime()));
    properties.put(WORKFLOW_OPERATION_KEY_SCHEDULE_LOCATION, event.getDevice());

    // Add the operations from the chosen workflow

    // Start the workflow
    return workflowService.start(getPreProcessingWorkflowDefinition(), mediapackage, properties);
  }

  /**
   * Removes the workflow associated with a scheduled event that is being removed.
   * 
   * @param event
   *          the scheduled event
   * @throws NotFoundException
   *           if the workflow associated with this scheduled event can not be found
   * @throws WorkflowDatabaseException
   *           if the workflow can not be stopped
   */
  public void stopWorkflowInstance(Event event) throws NotFoundException {
    try {
      workflowService.stop(event.getEventId());
    } catch (WorkflowDatabaseException e) {
      logger.warn("can not stop workflow {}, {}", event.getEventId(), e);
    }
  }

  /**
   * Persist a recurring event
   * 
   * @param RecurringEvent
   *          e
   * @throws SchedulerException
   *         if a workflow
   * @return The recurring event that has been persisted
   */
  public void addRecurringEvent(Event recurrence) throws SchedulerException {
    EntityManager em = emf.createEntityManager();
    
    try {
      for (Event e : recurrence.createEventsFromRecurrence()) {
        logger.debug("Adding recurring event {}", e.getEventId());
        addEvent(e);
      }
    } catch (ParseException pEx) {
      throw new SchedulerException("Unable to parse recurrence rule: {}", pEx);
    } catch (IncompleteDataException iDEx) {
      throw new SchedulerException("Recurring event is missing data: {}", iDEx);
    }
  }

  /**
   * Gets an event by its identifier
   * 
   * @param eventId
   *          the event identifier
   * @return An event that matches eventId
   * @throws IllegalArgumentException
   *           if the eventId is null
   * @throws IllegalStateException
   *           if the entity manager factory is not available
   * @throws NotFoundException
   *           if no event with this identifier exists
   */
  public Event getEvent(Long eventId) throws NotFoundException {
    if (eventId == null) {
      throw new IllegalArgumentException("eventId must not be null");
    }
    if (emf == null) {
      throw new IllegalStateException("entity manager factory is missing");
    }
    EntityManager em = emf.createEntityManager();
    EventImpl e = null;
    try {
      e = em.find(EventImpl.class, eventId);
    } finally {
      em.close();
    }
    if (e == null) {
      throw new NotFoundException("No event found for " + eventId);
    }
    return e;
  }

  /**
   * @param filter
   * @return List of events that match the supplied filter, or all events if no filter is supplied
   */
  public List<Event> getEvents(SchedulerFilter filter) {
    if (filter == null) {
      logger.debug("returning all events");
      return getAllEvents();
    }

    EntityManager em = emf.createEntityManager();
    CriteriaBuilder builder = emf.getCriteriaBuilder();
    CriteriaQuery<EventImpl> query = builder.createQuery(EventImpl.class);
    
    Root<EventImpl> rootEvent = query.from(EventImpl.class);
    query.select(rootEvent);

    EntityType<EventImpl> Event_ = rootEvent.getModel();
    Predicate wherePred = builder.conjunction();

    ParameterExpression<String> creatorParam = null;
    if (filter.getCreatorFilter() != null && !filter.getCreatorFilter().isEmpty()) {
      creatorParam = builder.parameter(String.class);
      wherePred = builder.and(wherePred,
              builder.like(rootEvent.get(Event_.getSingularAttribute("creator", String.class)), creatorParam));
    }

    ParameterExpression<String> deviceParam = null;
    if (filter.getDeviceFilter() != null && !filter.getDeviceFilter().isEmpty()) {
      deviceParam = builder.parameter(String.class);
      wherePred = builder.and(wherePred,
              builder.like(rootEvent.get(Event_.getSingularAttribute("device", String.class)), deviceParam));
    }

    ParameterExpression<String> titleParam = null;
    if (filter.getTitleFilter() != null && !filter.getTitleFilter().isEmpty()) {
      titleParam = builder.parameter(String.class);
      wherePred = builder.and(wherePred,
              builder.like(rootEvent.get(Event_.getSingularAttribute("title", String.class)), titleParam));
    }

    ParameterExpression<String> seriesParam = null;
    if (filter.getSeriesFilter() != null && !filter.getSeriesFilter().isEmpty()) {
      seriesParam = builder.parameter(String.class);
      wherePred = builder.and(wherePred,
              builder.like(rootEvent.get(Event_.getSingularAttribute("series", String.class)), seriesParam));
    }

    ParameterExpression<Date> startParam = null;
    ParameterExpression<Date> stopParam = null;
    if (filter.getStart() != null && filter.getStop() != null) { // Events with dates between start and stop
      startParam = builder.parameter(Date.class);
      stopParam = builder.parameter(Date.class);
      wherePred = builder.between(rootEvent.get(Event_.getSingularAttribute("startDate", Date.class)), startParam,
              stopParam);
    } else if (filter.getStart() != null && filter.getStop() == null) { // All events with dates after start
      startParam = builder.parameter(Date.class);
      wherePred = builder.greaterThan(rootEvent.get(Event_.getSingularAttribute("startDate", Date.class)), startParam);
    } else if (filter.getStart() != null && filter.getStop() == null) { // All events with dates after start
      stopParam = builder.parameter(Date.class);
      wherePred = builder.lessThan(rootEvent.get(Event_.getSingularAttribute("endDate", Date.class)), stopParam);
    }

    query.where(wherePred);

    if (filter.getOrder() != null) {
      if (filter.isOrderAscending()) {
        query.orderBy(builder.asc(rootEvent.get(Event_.getSingularAttribute(filter.getOrder()))));
      } else {
        query.orderBy(builder.desc(rootEvent.get(Event_.getSingularAttribute(filter.getOrder()))));
      }
    }

    TypedQuery<EventImpl> eventQuery = em.createQuery(query);

    if (creatorParam != null) {
      eventQuery.setParameter(creatorParam, filter.getCreatorFilter());
    }
    if (deviceParam != null) {
      eventQuery.setParameter(deviceParam, filter.getDeviceFilter());
    }
    if (titleParam != null) {
      eventQuery.setParameter(titleParam, filter.getTitleFilter());
    }
    if (seriesParam != null) {
      eventQuery.setParameter(seriesParam, filter.getSeriesFilter());
    }
    if (startParam != null) {
      eventQuery.setParameter(startParam, filter.getStart());
    }
    if (stopParam != null) {
      eventQuery.setParameter(stopParam, filter.getStop());
    }

    List<EventImpl> results = eventQuery.getResultList();
    List<Event> returnList = new LinkedList<Event>();
    for (EventImpl event : results) {
      returnList.add((Event) event);
    }
    
    return returnList;
  }

  /**
   * @return A list of all events
   */
  @SuppressWarnings("unchecked")
  public List<Event> getAllEvents() {
    EntityManager em = emf.createEntityManager();
    Query query = em.createNamedQuery("Event.getAll");
    try {
      return query.getResultList();
    } finally {
      em.close();
    }
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
      if (!(enddate == null) && !enddate.after(now))
        list.remove(e);
    }
    return list;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.scheduler.impl.SchedulerServiceImpl#removeEvent(java.lang.String)
   */
  public void removeEvent(Long eventID) throws NotFoundException {
    logger.info("Removing event with the ID {}", eventID);
    Event event;
    EntityManager em = emf.createEntityManager();
    try {
      em.getTransaction().begin();
      event = em.find(EventImpl.class, eventID);
      if (event == null)
        throw new NotFoundException("Event " + eventID + " does not exist");
      stopWorkflowInstance(event);
      em.remove(event);

      em.getTransaction().commit();
    } finally {
      em.close();
    }
  }

  /**
   * Updates an event.
   * 
   * @param e
   *          The event
   * @throws NotFoundException
   *           if the event hasn't previously been saved
   * @throws SchedulerException
   *           if the event's persistent representation can not be updated
   */
  public void updateEvent(Event e) throws NotFoundException, SchedulerException {
    updateEvent(e, true); // true, since we want to update the workflow instance too
  }

  /**
   * Updates an event.
   * 
   * @param e
   *          The event
   * @param updateWorkflow
   *          Whether to also update the associated workflow for this event
   * @throws SchedulerException
   *           if the scheduled event can not be persisted
   * @throws NotFoundException
   *           if this event hasn't previously been saved
   */
  public void updateEvent(Event e, boolean updateWorkflow) throws NotFoundException, SchedulerException {
    EntityManager em = null;
    EntityTransaction tx = null;
    Event storedEvent = getEvent(e.getEventId());
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      storedEvent.update(e);
      em.merge(storedEvent);
      tx.commit();
      if (updateWorkflow) {
        updateWorkflow(storedEvent);
      }
    } catch (Exception ex) {
      throw new SchedulerException(ex);
    } finally {
      em.close();
    }
  }

  public void updateWorkflow(Event event) throws NotFoundException, WorkflowDatabaseException, SchedulerException {
    WorkflowInstance workflow = workflowService.getWorkflowById(event.getEventId());
    WorkflowOperationInstance scheduleOperation = workflow.getCurrentOperation();

    // if the workflow is not in the hold state with 'schedule' as the current operation, we can't update the event
    if (!WorkflowInstance.WorkflowState.PAUSED.equals(workflow.getState())) {
      throw new SchedulerException("The workflow is not in the paused state, so it can not be updated");
    }
    if (!SCHEDULE_OPERATION_ID.equals(scheduleOperation.getId())) {
      throw new SchedulerException("The workflow is not in the paused state, so it can not be updated");
    }
    MediaPackage mediapackage = workflow.getMediaPackage();

    // update the mediapackage
    mediapackage.setTitle(event.getTitle());
    mediapackage.setLanguage(event.getLanguage());
    mediapackage.setLicense(event.getLicense());
    mediapackage.setSeries(event.getSeriesId());
    mediapackage.setSeriesTitle(event.getSeries());

    // Update the properties
    scheduleOperation.setConfiguration(WORKFLOW_OPERATION_KEY_SCHEDULE_START,
            Long.toString(event.getStartDate().getTime()));
    scheduleOperation.setConfiguration(WORKFLOW_OPERATION_KEY_SCHEDULE_STOP,
            Long.toString(event.getEndDate().getTime()));
    scheduleOperation.setConfiguration(WORKFLOW_OPERATION_KEY_SCHEDULE_LOCATION, event.getDevice());

    // update the workflow
    workflowService.update(workflow);
  }

  /**
   * Updates each event with an id in the list with the passed event.
   * @param eventIdList
   *        List of event ids.
   * @param e
   *        Event containing metadata to be updated.
   */
  public boolean updateEvents(List<Long> eventIdList, Event e) throws NotFoundException, SchedulerException {
    EntityManager em = emf.createEntityManager();
    em.getTransaction().begin();
    try{
      for(Long eventId : eventIdList) {
        e.setEventId(eventId);
        Event storedEvent = getEvent(e.getEventId());
        logger.debug("Found stored event. {} -", storedEvent);
        if (storedEvent == null){
          em.getTransaction().rollback();
          em.close();
          return false; // nothing found to update
        }
        storedEvent.update(e);
        em.merge(storedEvent);
        em.getTransaction().commit();
        updateWorkflow(storedEvent);
      }
    } catch( Exception ex) {
      logger.warn("Unable to update events: {}", ex);
      em.getTransaction().rollback();
      throw new SchedulerException(ex);
    } finally {
      em.close();
    }
    return true;
  }
  
  /**
   * @param e
   * @return A list of events that conflict with the start, or end dates of provided event.
   */
  public List<Event> findConflictingEvents(Event e) {
    return null;
  }

  public void destroy() {
    emf.close();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  @SuppressWarnings("rawtypes")
  @Override
  public void updated(Dictionary properties) throws ConfigurationException {
    this.properties = properties;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.scheduler.api.SchedulerService#getCalendarForCaptureAgent(java.lang.String)
   */
  public String getCalendarForCaptureAgent(String captureAgentID) {
    SchedulerFilter filter = getFilterForCaptureAgent(captureAgentID);
    CalendarGenerator cal = new CalendarGenerator(dcGenerator, caGenerator, seriesService);
    List<Event> events = getEvents(filter);
    logger.debug("Events with CA '{}': {}", captureAgentID, events);
    for (Event event : events) {
      cal.addEvent(event);
    }
    // Only validate calendars with events.  Without any events, the icalendar won't validate
    if(events.size() > 0) {
      try {
        cal.getCalendar().validate();
      } catch (ValidationException e1) {
        logger.warn("Could not validate Calendar: {}", e1.getMessage());
      }
    }
    return cal.getCalendar().toString(); // CalendarOutputter performance sucks (jmh)
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.scheduler.api.SchedulerService#getDublinCoreMetadata(java.lang.String)
   */
  public String getDublinCoreMetadata(Long eventID) throws NotFoundException {
    Event event = getEvent(eventID);
    if (dcGenerator == null) {
      logger.error("Dublin Core generator not initialized");
      return null;
    }
    return dcGenerator.generateAsString(event);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.scheduler.api.SchedulerService#getDublinCoreMetadata(java.lang.String)
   */
  public String getCaptureAgentMetadata(Long eventID) throws NotFoundException {
    Event event = getEvent(eventID);
    if (caGenerator == null) {
      logger.error("Capture Agent Metadata generator not initialized");
      return null;
    }
    return caGenerator.generateAsString(event);
  }

  /**
   * Sets the series service
   * 
   * @param s
   */
  public void setSeriesService(SeriesService s) {
    seriesService = s;
  }

  /**
   * Sets the workflow service
   * 
   * @param workflowService
   *          the workflowService to set
   */
  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  /**
   * @return An empty Event
   */
  public Event getNewEvent() {
    return new EventImpl();
  }
  
  /**
   * resolves the appropriate Filter for the Capture Agent
   * 
   * @param captureAgentID
   *          The ID as provided by the capture agent
   * @return the Filter for this capture Agent.
   */
  public SchedulerFilter getFilterForCaptureAgent(String captureAgentID) {
    SchedulerFilter filter = new SchedulerFilter();
    filter.withDeviceFilter(captureAgentID).withOrder("startDate").withStart(new Date(System.currentTimeMillis()));
    return filter;
  }

}
