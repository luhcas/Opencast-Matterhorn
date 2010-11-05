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

import java.text.ParseException;

/**
 * Event provides methods and properties belonging to single events. It contains {@link Metadata),
 * as well as JAXB and JPA Annotations. 
 */
public interface Event {
  /**
   * @return Event contributor
   */
  public String getContributor();
  /**
   * @param Event contributor
   */
  public void setContributor(String contributor);
  /**
   * @return Event creator
   */
  public String getCreator();
  /**
   * @param Event creator
   */
  public void setCreator(String creator);
  /**
   * @return Event description
   */
  public String getDescription();
  /**
   * @param Event description
   */
  public void setDescription(String description);
  /**
   * @return Event capture device name
   */
  public String getDevice();
  /**
   * @param Event capture device name
   */
  public void setDevice(String device);
  /**
   * @return Event duration
   */
  public long getDuration();
  /**
   * @param Event duration
   */
  public void setDuration(long duration);
  /**
   * @return Event end date
   */
  public Date getEndDate();
  /**
   * @param Event end date
   */
  public void setEndDate(Date endDate);
  /**
   * @return Event id
   */
  public String getEventId();
  /**
   * @param eventId
   */
  public void setEventId(String eventId);
  /**
   * @return Event langauge
   */
  public String getLanguage();
  /**
   * @param Event languge
   */
  public void setLanguage(String langauge);
  /**
   * @return Event license
   */
  public String getLicense();
  /**
   * @param Event license
   */
  public void setLicense(String license);
  /**
   * @return Event recurrence name
   */
  public String getRecurrence();
  /**
   * @param Event recurrence name
   */
  public void setRecurrence(String recurrence);
  /**
   * @return Event recurrence pattern
   */
  public String getRecurrencePattern();
  /**
   * @param Event recurrence pattern
   */
  public void setRecurrencePattern(String recurrence);
  /**
   * @return Capture agent resources
   */
  public String getResources();
  /**
   * @param Capture agent resources
   */
  public void setResources(String resources);
  /**
   * @return Event series name
   */
  public String getSeries();
  /**
   * @param Event series name
   */
  public void setSeries(String series);
  /**
   * @return Event series id
   */
  public String getSeriesId();
  /**
   * @param Event series id
   */
  public void setSeriesId(String seriesId);
  /**
   * @return Event start date
   */
  public Date getStartDate();
  /**
   * @param Event start date
   */
  public void setStartDate(Date startDate);
  /**
   * @return String Event subject
   */
  public String getSubject();
  /**
   * @param Event subject
   */
  public void setSubject(String subject);
  /**
   * @return String Event title
   */
  public String getTitle();
  /**
   * @param Event title
   */
  public void setTitle(String title);

  /**
   * Update a specific metadata field in the Event.
   * @param data
   */
  public void updateMetadata(Metadata data);

  /**
   * Create a new UUID and set this events Id to it.
   * @return This events Id.
   */
  public String generateId();

  /**
   * @return List containing this events additional metadata
   */
  public List<Metadata> getMetadataList();

  /**
   * @param metadata
   */
  public void setMetadataList(List<Metadata> metadata);


  /**
   * @param key
   *        The name of a specific metadata field
   * @return The value of a specific metadata field in the metadataTable
   */
  public String getMetadataValueByKey(String key);

  /**
   * @return Set of all metadata keys
   */
  public Set<String> getKeySet();

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
   * Update the event and persist it in the database
   * @param e
   *        Event to update this one with
   */
  public void update(Event e);

  public String toString();

  public boolean equals(Object o);

  public int hashCode();
    
  public void initializeFromEvent(Event e);
  
  public List<Event> createEventsFromRecurrence() throws ParseException, IncompleteDataException;
}
