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

import java.util.Date;
import java.util.List;

import javax.persistence.EntityManagerFactory;

/**
 * Recurring Event
 *
 */
public interface RecurringEvent {

  /**
   * @return This recurring event's Id.
   */
  public String getRecurringEventId();

  /**
   * @param rEventId
   */
  public void setRecurringEventId(String rEventId);

  /**
   * Generate and set this recurring event's id, then return it.
   * @return This recurring event's Id.
   */
  public String generateRecurringEventID();

  /**
   * @return The recurrence rule, {@link http://www.ietf.org/rfc/rfc2445.txt section 4.8.5}
   */
  public String getRecurrence();

  /**
   * @param recurrence
   */
  public void setRecurrence(String recurrence);

  /**
   * @return The list of metadata for this recurring event
   */
  public List<Metadata> getMetadata();

  /**
   * @param metadata
   */
  public void setMetadata(List<Metadata> metadata);

  /**
   * @return List of events that have been generated based on the recurrence rule
   */
  public List<Event> generatedEvents();

  /**
   * Generates a list of dates for events based on the recurrence rule
   */
  public void generateDates();

  /**
   * @return Get the list of events that are children of this recurring event
   */
  public List<Event> getEvents();

  /**
   * 
   * @param index
   * @return The date of an event based on it's recurrence position
   */
  public Date getDateForEventByIndex(int index);

  /**
   * @param key
   * @return A specific metadata field that is a Date, or return null.
   */
  public Date getValueAsDate(String key);

  /**
   * @param key
   * @return True if a specific metadata key exists in the metadataTable
   */
  public boolean containsKey(String key);

  /**
   * @param e
   */
  public void update(RecurringEvent e);

  @Override
  public String toString();

  /**
   * Update a specific metadata item
   * @param data
   */
  public void updateMetadata(Metadata data);

  public String generateId();

  /**
   * removes a generated event from the list of events that belong to this recurring event. If an event is removed the
   * date at which this event would have happened will not be recorded!
   * 
   * @param e
   *          the event that should be removed
   * @return true if the event could be deleted
   */
  public boolean removeEvent(Event e);

  /**
   * Add new metadata to this recurring event's metadata list.
   * @param m
   */
  public void addMetadata(Metadata m);
  
  /**
   * Remove a specific metadata field from this recurring event's metadata list.
   * @param m
   */
  public void removeMetadata(Metadata m);
  
  public void setEntityManagerFactory(EntityManagerFactory emf);
}
