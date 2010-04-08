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


import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.property.RRule;

@Entity(name="recurringEvent")
@Table(name="MH_RECURRING_EVENT")
public class RecurringEvent {
  private static final Logger logger = LoggerFactory.getLogger(RecurringEvent.class);
  
  @Id 
  protected String rEventId;
  
  @Column(name="recurrence")
  protected String recurrence;
  
  @OneToMany (fetch=FetchType.EAGER, cascade=CascadeType.ALL)
  @MapKey(name="metadata")
  protected List<Metadata> metadata = new LinkedList<Metadata>();

  @OneToMany(cascade = CascadeType.ALL)
  @MapKey(name="generatedEvents")
  protected List<Event> generatedEvents = new LinkedList<Event>();

  public RecurringEvent () {
  }
  
  public RecurringEvent (String recurrence) {
    setRecurrence(recurrence);
  }

  public RecurringEvent (String recurrence, List<Metadata> metadata) {
    this(recurrence);
    setMetadata(metadata);
  }

  public RecurringEvent (String recurrence, String rEventId) {
    this(recurrence);
    setRecurringEventId(rEventId);
  }  
  
  public RecurringEvent (String recurrence, String rEventId, List<Metadata> metadata) {
    this(recurrence, metadata);
    setRecurringEventId(rEventId);
  }  
  
  public String getRecurringEventId() {
    return rEventId;
  }

  public void setRecurringEventId(String rEventId) {
    this.rEventId = rEventId;
  }

  public String getRecurrance() {
    return recurrence;
  }

  public void setRecurrence(String recurrence) {
    try {
      RRule rrule = new RRule(recurrence);
      rrule.validate();
    } catch (ParseException e) {
      logger.warn("Recurring event {}: Recurrance rule could not be parsed for {}", rEventId, recurrence);
      return;
    } catch (ValidationException e) {
      logger.warn("Recurring event {}: Recurrance rule could not be validated for {}", rEventId, recurrence);
      return; // TODO What should happen if recurrence is wrong?
    }
    this.recurrence = recurrence;
  }

  public List<Metadata> getMetadata() {
    return metadata;
  }

  public void setMetadata(List<Metadata> metadata) {
    this.metadata = metadata;
  }

  public List<Event> getGeneratedEvents() {
    return generatedEvents;
  }
  
  
  
}
