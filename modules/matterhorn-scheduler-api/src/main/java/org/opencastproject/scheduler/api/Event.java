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
  String getContributor();
  /**
   * @param Event contributor
   */
  void setContributor(String contributor);
  /**
   * @return Event creator
   */
  String getCreator();
  /**
   * @param Event creator
   */
  void setCreator(String creator);
  /**
   * @return Event description
   */
  String getDescription();
  /**
   * @param Event description
   */
  void setDescription(String description);
  /**
   * @return Event capture device name
   */
  String getDevice();
  /**
   * @param Event capture device name
   */
  void setDevice(String device);
  /**
   * @return Event duration
   */
  Long getDuration();
  /**
   * @param Event duration
   */
  void setDuration(long duration);
  /**
   * @return Event end date
   */
  Date getEndDate();
  /**
   * @param Event end date
   */
  void setEndDate(Date endDate);
  /**
   * @return Event id
   */
  Long getEventId();
  /**
   * @param eventId
   */
  void setEventId(Long eventId);
  /**
   * @return Event langauge
   */
  String getLanguage();
  /**
   * @param Event languge
   */
  void setLanguage(String langauge);
  /**
   * @return Event license
   */
  String getLicense();
  /**
   * @param Event license
   */
  void setLicense(String license);
  /**
   * @return Event recurrence name
   */
  String getRecurrence();
  /**
   * @param Event recurrence name
   */
  void setRecurrence(String recurrence);
  /**
   * @return Event recurrence pattern
   */
  String getRecurrencePattern();
  /**
   * @param Event recurrence pattern
   */
  void setRecurrencePattern(String recurrence);
  /**
   * @return Capture agent resources
   */
  String getResources();
  /**
   * @param Capture agent resources
   */
  void setResources(String resources);
  /**
   * @return Event series name
   */
  String getSeries();
  /**
   * @param Event series name
   */
  void setSeries(String series);
  /**
   * @return Event series id
   */
  String getSeriesId();
  /**
   * @param Event series id
   */
  void setSeriesId(String seriesId);
  /**
   * @return Event start date
   */
  Date getStartDate();
  /**
   * @param Event start date
   */
  void setStartDate(Date startDate);
  /**
   * @return String Event subject
   */
  String getSubject();
  /**
   * @param Event subject
   */
  void setSubject(String subject);
  /**
   * @return String Event title
   */
  String getTitle();
  /**
   * @param Event title
   */
  void setTitle(String title);

  /**
   * Update a specific metadata field in the Event.
   * @param data
   */
  void updateMetadata(Metadata data);

  /**
   * @return List containing this events additional metadata
   */
  List<Metadata> getMetadataList();

  /**
   * @param metadata
   */
  void setMetadataList(List<Metadata> metadata);


  /**
   * @param key
   *        The name of a specific metadata field
   * @return The value of a specific metadata field in the metadataTable
   */
  String getMetadataValueByKey(String key);

  /**
   * @return Set of all metadata keys
   */
  Set<String> getKeySet();

  /**
   * @param key
   * @return True if a specific metadata key exists in the metadataTable
   */
  boolean containsKey(String key);

  /**
   * @param key
   * @return A specific metadata field in the metadata list for this event (not metadataTable)
   */
  Metadata findMetadata(String key);
  
  /**
   * Add new metadata to this event's metadata list.
   * @param m
   */
  void addMetadata(Metadata m);
  
  /**
   * Remove a specific metadata field from this event's metadata list.
   * @param m
   */
  void removeMetadata(Metadata m);

  /**
   * Update the event and persist it in the database
   * @param e
   *        Event to update this one with
   */
  void update(Event e);

  String toString();

  boolean equals(Object o);

  int hashCode();
    
  void initializeFromEvent(Event e);
  
  List<Event> createEventsFromRecurrence() throws ParseException, IncompleteDataException;

}
