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
package org.opencastproject.scheduler.impl.jpa;

import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.opencastproject.scheduler.api.SchedulerEvent;
import org.opencastproject.scheduler.impl.SchedulerEventImpl;

/**
 * An Event has a unique ID, a relation to the recurring event from which it was created and a set of metadata.
 * Even the start- and end-time is stored in the set of metadata, with the keys "time.start" and "time.end" as long value converted to string.
 * Resources and Attendees are store in the metadata too, as 
 */
@Entity(name="Event")
@Table(name="MH_EVENT")
public class Event {

  @Id
  protected String eventId;
  
  @ManyToOne (fetch=FetchType.EAGER, cascade=CascadeType.ALL)
  @MapKey(name="recurringEvent")
  protected RecurringEvent recurringEvent;
  
  @OneToMany (fetch=FetchType.EAGER, cascade=CascadeType.ALL)
  @MapKey(name="metadata")
  protected List<Metadata> metadata = new LinkedList<Metadata>();

  @Transient
  protected Hashtable<String, String> metadataTable;
  
  public String getEventId() {
    return eventId;
  }

  public void setEventId(String eventId) {
    this.eventId = eventId;
  }
  
  public String generateId() {
    return eventId = UUID.randomUUID().toString();
  }

  public RecurringEvent getRecurringEvent() {
    return recurringEvent;
  }

  public void setRecurringEvent(RecurringEvent recurringEvent) {
    this.recurringEvent = recurringEvent;
  }

  public List<Metadata> getMetadata() {
    return metadata;
  }

  public void setMetadata(List<Metadata> metadata) {
    this.metadata = metadata;
  }
  
  protected void buildMetadataTable () {
    metadataTable = new Hashtable<String, String>(); // Buffer metadata in Hashtable for quick 
    if (recurringEvent != null) 
      for (Metadata data : recurringEvent.getMetadata()) metadataTable.put(data.key, data.value); // Inherit values
    for (Metadata data : metadata) metadataTable.put(data.key, data.value); // Overwrite with event specific data
  }
  
  public String getValue (String key) {
   if (metadataTable == null) buildMetadataTable();
   return metadataTable.get(key);
   
  }
  
  public Set<String> getKeySet () {
    if (metadataTable == null) buildMetadataTable();
    return metadataTable.keySet();
  }
  
  public SchedulerEvent toSchedulerEvent () {
    SchedulerEventImpl e = new SchedulerEventImpl();
    if (metadataTable == null) buildMetadataTable();
    e.setMetadata(metadataTable);
    e.setID(eventId);
    e.setStartdate(new Date(Long.parseLong(metadataTable.get("time.start")))); //TODO will not work for generated events
    e.setEnddate(new Date(Long.parseLong(metadataTable.get("time.end"))));
    StringTokenizer attendees =  new StringTokenizer(metadataTable.get("attendees"),",");
    while (attendees.hasMoreTokens()) e.addAttendee(attendees.nextToken());
    StringTokenizer resources =  new StringTokenizer(metadataTable.get("resources"),",");
    while (resources.hasMoreTokens()) e.addResource(resources.nextToken());  
    return e;
  }
  
}
