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
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.scheduler.api.SchedulerFilter;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowParser;
import org.opencastproject.workflow.api.WorkflowService;

import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.RRule;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
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
    logger.info("SchedulerService activating.");

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
      return WorkflowParser.parseWorkflowDefinition(in);
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
    } catch (WorkflowException workflowException) {
      throw new SchedulerException(workflowException);
    } catch (MediaPackageException mediaPackageException) {
      throw new SchedulerException(mediaPackageException);
    }

    try {
      event.setEventId(workflow.getId());
      event.setMetadataList(event.getMetadataList());
      event.setLastModified(new Date());
      em = emf.createEntityManager();
      tx = em.getTransaction();
      event = (EventImpl) event;
      tx.begin();
      em.persist(event);
      tx.commit();
    } catch (Exception ex) {
      if (tx.isActive()) {
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
   * @throws WorkflowException
   *           if the workflow can not be created
   * @throws MediaPackageException
   *           if the mediapackage can not be created
   */
  public WorkflowInstance startWorkflowInstance(Event event) throws WorkflowException, MediaPackageException {
    // Build a mediapackage using the event metadata
    MediaPackage mediapackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    mediapackage.setTitle(event.getTitle());
    mediapackage.setLanguage(event.getLanguage());
    mediapackage.setLicense(event.getMetadataValueByKey("license"));
    mediapackage.setSeries(event.getSeriesId());
    mediapackage.setSeriesTitle(event.getSeries());
    mediapackage.setDate(event.getStartDate());
    mediapackage.addCreator(event.getCreator());
    mediapackage.setDuration(event.getDuration());

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
    } catch (WorkflowException e) {
      logger.warn("can not stop workflow {}, {}", event.getEventId(), e);
    }
  }

  /**
   * Persist a recurring event
   * 
   * @param RecurringEvent
   *          e
   * @throws SchedulerException
   *           if a workflow
   * @return The recurring event that has been persisted
   */
  public void addRecurringEvent(Event recurrence) throws SchedulerException {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      for (Event event : recurrence.createEventsFromRecurrence()) {
        logger.debug("Adding recurring event {}", event.getEventId());

        // Start a workflow so we have an event id that we can associate the event with
        WorkflowInstance workflow = null;
        try {
          workflow = startWorkflowInstance(event);
        } catch (WorkflowException workflowException) {
          throw new SchedulerException(workflowException);
        } catch (MediaPackageException mediaPackageException) {
          throw new SchedulerException(mediaPackageException);
        }

        try {
          event.setEventId(workflow.getId());
          event.setMetadataList(event.getMetadataList());
          event.setLastModified(new Date());
          em.persist((EventImpl)event);
        } catch (Exception ex) {
          if (tx.isActive()) {
            tx.rollback();
          }
          throw new SchedulerException("Unable to add event: {}", ex);
        }
      }
      tx.commit();
    } catch (ParseException pEx) {
      throw new SchedulerException("Unable to parse recurrence rule: {}", pEx);
    } catch (IncompleteDataException iDEx) {
      throw new SchedulerException("Recurring event is missing data: {}", iDEx);
    } finally {
      if (em != null) {
        em.close();
      }
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
    } catch (Exception ex) {
      logger.debug("Could not find event {}: {}", eventId, ex);
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
    StringBuilder queryBase = new StringBuilder("SELECT e FROM Event e");
    ArrayList<String> where = new ArrayList<String>();
    EntityManager em = emf.createEntityManager();

    if (StringUtils.isNotEmpty(filter.getCreatorFilter())) {
      where.add("LOWER(e.creator) LIKE :creatorParam");
    }

    if (StringUtils.isNotEmpty(filter.getDeviceFilter())) {
      where.add("LOWER(e.device) LIKE :deviceParam");
    }

    if (StringUtils.isNotEmpty(filter.getTitleFilter())) {
      where.add("LOWER(e.title) LIKE :titleParam");
    }

    if (StringUtils.isNotEmpty(filter.getSeriesFilter())) {
      where.add("LOWER(e.series) LIKE :seriesParam");
    }

    if (filter.getSeriesId() != null) {
      where.add("LOWER(e.seriesId) = :seriesIdParam");
    }

    if (filter.getStart() != null && filter.getStop() != null) { // Events intersecting start and stop
      where.add("e.startDate < :stopParam AND e.endDate > :startParam");
    } else if (filter.getStart() != null && filter.getStop() == null) { // All events with dates after start
      where.add("e.startDate > :startParam");
    } else if (filter.getStart() == null && filter.getStop() != null) { // All events with dates before end
      where.add("e.startDate < :stopParam");
    }

    if (filter.getCurrentAndUpcoming()) {
      where.add("e.endDate > :now");
    }

    if (where.size() > 0) {
      queryBase.append(" WHERE " + StringUtils.join(where, " AND "));
    }

    if (filter.getOrder() != null) {
      if (filter.isOrderAscending()) {
        queryBase.append(" ORDER BY e.title ASC");
      } else {
        queryBase.append(" ORDER BY e.title DESC");
      }
    }

    TypedQuery<EventImpl> eventQuery = em.createQuery(queryBase.toString(), EventImpl.class);

    if (StringUtils.isNotEmpty(filter.getCreatorFilter())) {
      eventQuery.setParameter("creatorParam", "%" + filter.getCreatorFilter().toLowerCase() + "%");
    }
    if (StringUtils.isNotEmpty(filter.getDeviceFilter())) {
      eventQuery.setParameter("deviceParam", "%" + filter.getDeviceFilter().toLowerCase() + "%");
    }
    if (StringUtils.isNotEmpty(filter.getTitleFilter())) {
      eventQuery.setParameter("titleParam", "%" + filter.getTitleFilter().toLowerCase() + "%");
    }
    if (StringUtils.isNotEmpty(filter.getSeriesFilter())) {
      eventQuery.setParameter("seriesParam", "%" + filter.getSeriesFilter().toLowerCase() + "%");
    }
    if (filter.getSeriesId() != null) {
      eventQuery.setParameter("seriesIdParam", filter.getSeriesId());
    }
    if (filter.getStart() != null) {
      eventQuery.setParameter("startParam", filter.getStart());
    }
    if (filter.getStop() != null) {
      eventQuery.setParameter("stopParam", filter.getStop());
    }
    if (filter.getCurrentAndUpcoming()) {
      eventQuery.setParameter("now", new Date(System.currentTimeMillis()));
    }

    List<EventImpl> results = new ArrayList<EventImpl>();
    try {
      results = eventQuery.getResultList();
    } finally {
      em.close();
    }

    List<Event> returnList = new ArrayList<Event>();
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
      if (!(enddate == null) && !enddate.after(now)) {
        list.remove(e);
      }
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
    updateEvent(e, updateWorkflow, false);
  }

  /**
   * Updates an event.
   * 
   * @param e
   *          The event
   * @param updateWorkflow
   *          Whether to also update the associated workflow for this event
   * @param updateWithEmptyValues
   *          Overwrite stored event's fields with null if provided event's fields are null
   * @throws SchedulerException
   *           if the scheduled event can not be persisted
   * @throws NotFoundException
   *           if this event hasn't previously been saved
   */
  public void updateEvent(Event e, boolean updateWorkflow, boolean updateWithEmptyValues) throws NotFoundException,
          SchedulerException {
    EntityManager em = null;
    EntityTransaction tx = null;
    Event storedEvent = getEvent(e.getEventId());
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      storedEvent.update(e, updateWithEmptyValues);
      storedEvent.setLastModified(new Date());
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

  public void updateWorkflow(Event event) throws NotFoundException, WorkflowException, SchedulerException {
    WorkflowInstance workflow = workflowService.getWorkflowById(event.getEventId());
    WorkflowOperationInstance scheduleOperation = workflow.getCurrentOperation();

    // if the workflow is not in the hold state with 'schedule' as the current operation, we can't update the event
    if (!WorkflowInstance.WorkflowState.PAUSED.equals(workflow.getState())) {
      throw new SchedulerException("The workflow is not in the paused state, so it can not be updated");
    }
    if (!SCHEDULE_OPERATION_ID.equals(scheduleOperation.getTemplate())) {
      throw new SchedulerException("The workflow is not in the paused state, so it can not be updated");
    }
    MediaPackage mediapackage = workflow.getMediaPackage();

    // update the mediapackage
    mediapackage.setTitle(event.getTitle());
    mediapackage.setLanguage(event.getLanguage());
    mediapackage.setLicense(event.getMetadataValueByKey("license"));
    mediapackage.setSeries(event.getSeriesId());
    mediapackage.setSeriesTitle(event.getSeries());
    mediapackage.setDate(event.getStartDate());
    mediapackage.setDuration(event.getDuration());
    // mediapackage supports multiple creators, interface does not. replace them all with this one
    // We really should handle this better
    for (String creator : mediapackage.getCreators()) {
      mediapackage.removeCreator(creator);
    }
    mediapackage.addCreator(event.getCreator());

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
   * 
   * @param eventIdList
   *          List of event ids.
   * @param e
   *          Event containing metadata to be updated.
   */
  public void updateEvents(List<Long> eventIdList, Event e) throws NotFoundException, SchedulerException {
    List<Event> eventList = new LinkedList<Event>();
    for (Long id : eventIdList) {
      eventList.add(getEvent(id));
    }
    updateEvents(eventList, e, false);
  }

  public void updateEvents(List<Event> eventList, Event e, boolean updateWithEmptyValues) throws NotFoundException,
          SchedulerException {
    int sequence = 1;
    String title = e.getTitle();
    for (Event event : eventList) {
      e.setEventId(event.getEventId());
      if (eventList.size() > 1 && StringUtils.isNotEmpty(e.getTitle())) {
        e.setTitle(title + " " + String.valueOf(sequence));
      }
      updateEvent(e, true, updateWithEmptyValues);
      sequence++;
    }
  }

  /**
   * @param e
   * @return A list of events that conflict with the start, or end dates of provided event.
   */
  public List<Event> findConflictingEvents(String device, Date startDate, Date endDate) {
    SchedulerFilter filter = new SchedulerFilter();
    filter.withDeviceFilter(device).withStart(startDate).withStop(endDate);
    return getEvents(filter);
  }

  public List<Event> findConflictingEvents(String device, String rrule, Date startDate, Date endDate, Long duration)
          throws ParseException, ValidationException {
    RRule rule = new RRule(rrule);
    rule.validate();
    Recur recur = rule.getRecur();
    DateTime start = new DateTime(startDate.getTime());
    start.setUtc(true);
    DateTime end = new DateTime(endDate.getTime());
    end.setUtc(true);
    DateList dates = recur.getDates(start, end, Value.DATE_TIME);
    List<Event> events = new ArrayList<Event>();

    for (Object d : dates) {
      Date filterStart = (Date) d;
      SchedulerFilter filter = new SchedulerFilter().withDeviceFilter(device).withStart(filterStart)
              .withStop(new Date(filterStart.getTime() + duration));
      List<Event> filterEvents = getEvents(filter);
      events.addAll(filterEvents);
    }

    return events;
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
    // Only validate calendars with events. Without any events, the icalendar won't validate
    if (events.size() > 0) {
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
    filter.withDeviceFilter(captureAgentID).withCurrentAndUpcoming().withOrder("startDate");
    return filter;
  }

  public Date getScheduleLastModified(String captureAgentId) throws SchedulerException {
    EntityManager em = emf.createEntityManager();
    try {
      Query q = em.createNamedQuery("Event.getLastUpdated").setParameter("device", captureAgentId);
      return (Date) q.getSingleResult();
    } catch (NoResultException e) {
      logger.debug("No events scheduled for {}", captureAgentId);
    } catch (Exception e) {
      throw new SchedulerException(e);
    } finally {
      em.close();
    }
    return null;
  }
}
