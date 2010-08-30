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
import java.util.Set;

import javax.persistence.EntityManagerFactory;

/**
 * Event provides methods and properties belonging to single events. It contains {@link Metadata),
 * as well as JAXB and JPA Annotations. 
 */
public interface Event {

  /**
   * @return The recurring event id of the recurring event that this event belongs to
   */
  public String getRecurringEventId();

  /**
   * @param recurringEventId, The id of the recurring event that this event belongs to
   */
  public void setRecurringEventId(String recurringEventId);

  /**
   * Update a specific metadata field in the Event.
   * @param data
   */
  public void updateMetadata(Metadata data);

  /**
   * The position of the event in the series of recurring events. 
   * @return the position of this event in the recurrence
   */
  public int getPositionInRecurrence();

  /**
   * @param positionInRecurrence
   */
  public void setPositionInRecurrence(int positionInRecurrence);
  
  /**
   * @return This events Id.
   */
  public String getEventId();

  /**
   * @param eventId
   */
  public void setEventId(String eventId);

  /**
   * Create a new UUID and set this events Id to it.
   * @return This events Id.
   */
  public String generateId();

  /**
   * @return The recurring event that this event belongs to.
   */
  public RecurringEvent getRecurringEvent();

  /**
   * Set the recurring event that this event belongs to.
   * @param recurringEvent
   */
  public void setRecurringEvent(RecurringEvent recurringEvent);

  /**
   * @return List containing both this and parent recurring event's metadata.
   * If there is not parent recurring event, this will contain only this event's metadata.
   */
  public List<Metadata> getCompleteMetadata();

  /**
   * @return List containing just this events metadata
   */
  public List<Metadata> getMetadata();

  /**
   * @param metadata
   */
  public void setMetadata(List<Metadata> metadata);

  /**
   * buildMetadataTable takes both this and parent recurring event's metadata and builds a table
   * which is the union of this and the parents metadata. This event's metadata overrides the parents.
   */
  public void buildMetadataTable();

  /**
   * @param key
   *        The name of a specific metadata field
   * @return The value of a specific metadata field in the metadataTable
   */
  public String getValue(String key);

  /**
   * @return Set of all metadata keys
   */
  public Set<String> getKeySet();

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
   * @param key
   * @return A specific metadata field in the metadata list for this event (not metadataTable)
   */
  public Metadata findMetadata(String key);

  /**
   * Update the event and persist it in the database
   * @param e
   *        Event to update this one with
   */
  public void update(Event e);

  /**
   * @return This event's start date.
   */
  public Date getStartdate();

  /**
   * @return This event's end date.
   */
  public Date getEnddate();

  @Override
  public String toString();

  @Override
  public boolean equals(Object o);

  @Override
  public int hashCode();
  
  /**
   * Add new metadata to this event's metadata list.
   * @param m
   */
  public void addMetadata(Metadata m);
  
  /**
   * Remove a specific metadata field from this event's metadata list.
   * @param m
   */
  public void removeMetadata(Metadata m);
  
  /**
   * @param emf
   */
  public void setEntityManagerFactory(EntityManagerFactory emf);
}
