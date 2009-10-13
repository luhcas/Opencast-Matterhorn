/**
 *  Copyright 2009 The Regents of the University of California
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

/**
 * FIXME -- Add javadocs
 */
public interface SchedulerService {

  /**
   * Adds a new event to the database and returns the event with an updated event-id, to make sure the event-id stays unique 
   * @param e The Event that will be added to the scheduler
   * @return the updated event, with a new ID. null if there are errors in adding the event.
   */
  public SchedulerEvent addEvent (SchedulerEvent e);
  
  /**
   * removes the event with the given event-id from the database. Returns true is operation was successfull
   * @param eventID The ID of the event that schould be removed
   * @return true if the event was removed
   */
  public boolean removeEvent (String eventID);
  
  /**
   * updates an event in the database and returns true if the operation was successfull 
   * @param e The event that should be updated. An event with the given ID has to be in the database already! 
   * @return true if the event could be updated
   */
  public boolean updateEvent (SchedulerEvent e);
  
  /**
   * returns the event with the provided ID 
   * @param eventID The ID of the requested event
   * @return The requested event
   */
  public SchedulerEvent getEvent (String eventID);
  
  /**
   * returns all events that pass the filter als an array. 
   * @param filter A Filter object to search specific events. Null if no filter should be applied.
   * @return An array with the events
   */
  public SchedulerEvent [] getEvents (SchedulerFilter filter);
  
  /**
   * returns an URL under which the iCalendar for the capture agent specified by the captureAgentId can be found. 
   * @param captureAgentID the ID of the capture agent
   * @return An URL where the schedule can be found.
   */
  public String getCalendarForCaptureAgent (String captureAgentID);
  
}

