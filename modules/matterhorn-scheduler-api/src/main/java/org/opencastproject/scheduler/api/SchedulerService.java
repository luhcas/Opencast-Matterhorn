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
package org.opencastproject.scheduler.api;

import java.util.List;

import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.WorkflowDefinition;

public interface SchedulerService{

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
  
  public WorkflowDefinition getPreProcessingWorkflowDefinition() throws IllegalStateException;
  

  /**
   * Persist an event
   * 
   * @param event
   *          the event to add
   * 
   * @return The event that has been persisted
   */
  public Event addEvent(Event event) throws SchedulerException;

  /**
   * Persist a recurring event
   * 
   * @param RecurringEvent
   *          e
   * @return The recurring event that has been persisted
   */
  public void addRecurringEvent(Event recurrence) throws SchedulerException;

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
  public Event getEvent(Long eventId) throws NotFoundException;

  /**
   * @param filter
   * @return List of events that match the supplied filter, or all events if no filter is supplied
   */
  public List<Event> getEvents(SchedulerFilter filter);

  /**
   * @return A list of all events
   */
  public List<Event> getAllEvents();

  /**
   * @return List of all events that start after the current time.
   */
  public List<Event> getUpcomingEvents();

  /**
   * @param list
   * @return The list of events in a list of events that occur after the current time.
   */
  public List<Event> getUpcomingEvents(List<Event> list);

  /**
   * @param Long
   *        the eventId of the event to be removed.
   * @throws NotFoundException
   *        If the eventId cannot be found.
   */
  public void removeEvent(Long eventID) throws NotFoundException;

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
  public void updateEvent(Event e) throws NotFoundException, SchedulerException;

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
  public void updateEvent(Event e, boolean updateWorkflow) throws NotFoundException, SchedulerException;

  /**
   * Updates each event with an id in the list with the passed event.
   * @param eventIdList
   *        List of event ids.
   * @param e
   *        Event containing metadata to be updated.
   */
  public void updateEvents(List<Long> eventIdList, Event e) throws NotFoundException, SchedulerException;
  
  /**
   * @param e
   * @return A list of events that conflict with the start, or end dates of provided event.
   */
  public List<Event> findConflictingEvents(Event e);
  
  /**
   * 
   * @param captureAgentID
   *        The name of the capture agent
   * @return An iCalendar containing all of the events for the specified capture agent.
   */

  public String getCalendarForCaptureAgent(String captureAgentID);

  /**
   * 
   * @param eventID
   * @return The DublinCore metadata document of an event
   * @throws NotFoundException
   */
  public String getDublinCoreMetadata(Long eventID) throws NotFoundException;

  /**
   * 
   * @param eventID
   * @return
   * @throws NotFoundException
   */
  public String getCaptureAgentMetadata(Long eventID) throws NotFoundException;

  /**
   * @return An empty Event
   */
  public Event getNewEvent();

  /**
   * resolves the appropriate Filter for the Capture Agent
   * 
   * @param captureAgentID
   *          The ID as provided by the capture agent
   * @return the Filter for this capture Agent.
   */
  public SchedulerFilter getFilterForCaptureAgent(String captureAgentID);

}
