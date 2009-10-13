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

import java.util.Date;

/**
 * TODO: Comment me!
 *
 */
public interface SchedulerEvent {
  /**
   * Gets the event ID of this object 
   * @return the event ID
   */
  public String getID();
  
  /**
   * Sets the event ID of this object
   * @param id the event ID
   */
  public void setID(String id);
  
  /**
   * Gets the ID of the capture agent that should be used to record this event
   * @return the ID of the capture Agent
   */
  public String getDevice();
  
  /**
   * Sets the ID of the capture agent 
   * @param device The ID of the capture agent 
   */
  public void setDevice(String device);
  
  /**
   * Gets the title of the event
   * @return The title of the event
   */
  public String getTitle();
  
  /**
   * Sets the title of the event
   * @param title The title of the event
   */
  public void setTitle(String title);
  
  /**
   * Gets the creator (lecturer) of the event
   * @return The creator of the event
   */
  public String getCreator();
  
  /**
   * Sets the creator (lecturer) of the event.
   * @param creator The creator of the event.
   */
  public void setCreator(String creator);
  
  /**
   * Gets a short description of the event
   * @return A short description of the event
   */
  public String getAbstract();
  
  /** 
   * Sets a short description of the event
   * @param text A short description of the event
   */
  public void setAbstract(String text);
  
  /**
   * Gets the time and date when the recording of the event should be started.
   * @return The date when the recording should be started 
   */
  public Date getStartdate();
  
  /**
   * Sets the time and date when the recording of the event should be started
   * @param start The Time and Date when the recording should be started
   */
  public void setStartdate(Date start);
  
  /**
   * Gets the time and date when the recording of the event should be stoped.
   * @return The date when the recording should be stoped 
   */
  public Date getEnddate();
  
  /**
   * Sets the time and date when the recording of the event should be stoped
   * @param end The Time and Date when the recording should be stoped
   */
  public void setEnddate(Date end);
  
  /**
   * Gets the contributor (i.e. the department for which the lecture is given) of the event  
   * @return the contributor of the event
   */
  public String getContributor();
  
  /**
   * Sets the contributor (i.e. the department for which the lecture is given) of the event
   * @param contributor The contributor of the event
   */
  public void setContributor(String contributor);
  
  /**
   * Gets the series ID of the series that this events belongs to
   * @return the ID of the series
   */
  public String getSeriesID();
  
  /**
   * Sets the series ID of the series that this events belongs to
   * @param seriesID the ID of the series
   */
  public void setSeriesID(String seriesID);
  
  /**
   * Gets the iCalendar source ID that this event comes from 
   * @return The ID of the iCalendar source
   */
  public String getChannelID();
  
  /**
   * Sets the iCalendar source ID that this event comes from
   * @param channelID The ID of the iCalendar source
   */
  public void setChannelID(String channelID);
  
  /**
   * Gets the location (room) where the event will happen
   * @return The location of the event
   */
  public String getLocation();
  
  /**
   * Sets the location (room) where the event will happen
   * @param location The location of the event
   */
  public void setLocation(String location);
  
  /**
   * Gets a list of the technical settings for the capture agent
   * @return A list of settings for the capture agent 
   */
  public String [] getResources();
  
  /**
   * Adds a single technical setting to the list of settings
   * @param resource The technical setting
   */
  public void addResource (String resource);
  
  /**
   * Replaces the list of technical settings for the capture agent
   * @param resources The list of technical settings for the capture agent
   */
  public void setResources (String [] resources);
  
  /**
   * Gets a list af the attendees of the event. The capture agent ID could/should be part of this list of attendees 
   * @return the list of attendees
   */
  public String [] getAttendees();
  
  /**
   * Adds an additional attendee or an capture agent ID to the list of attendees. 
   * @param attendee One Attendee
   */
  public void addAttendee (String attendee);
  
  /**
   * Replaces the list of attendees. 
   * @param attendees The new list of attendees
   */
  public void setAttendees (String [] attendees);
}
