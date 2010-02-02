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
 * The Scheduler Filter is used to filter the events that would come back from an search operation on the database.
 *
 */
public interface SchedulerFilter {
  /**
   * Gets the event ID for which should be filtered (will probably deliver only one event).
   * @return The ID of the event
   */
  public String getEventIDFilter();
  
  /**
   * Sets the event ID for which should be filtered (will probably deliver only one event). Only a complete ID can be entered not a pattern.
   * @param eventID The ID of the event
   */
  public void setEventIDFilter(String eventID);
  
  /**
   * Gets the search Pattern for the device ID. Can be a part of the device ID.
   * @return The pattern for which the device ID should be filtered
   */
  public String getDeviceFilter();
  
  /**
   * Sets the search Pattern for the device ID. Can be a part of the device ID. Every device ID that has this pattern as a part will be returned.
   * @param devicePattern The pattern for which device ID should be filtered
   */
  public void setDeviceFilter(String devicePattern);
  
  /**
   * Gets the search Pattern for the title. Can be a part of the title.
   * @return The pattern for which the title should be filtered
   */
  public String getTitleFilter();
  
  /**
   * Sets the search pattern for the title. Can be a part of the title. Every title that has this pattern as a part will be returned.
   * @param titlePattern The pattern for which title should be filtered
   */
  public void setTitleFilter(String titlePattern);
  
  /**
   * Gets the search pattern for the creator. Can be a part of the creator.
   * @return The pattern for which the creator should be filtered
   */
  public String getCreatorFilter();
  
  /**
   * Sets the search pattern for the creator. Can be a part of the creator. Every creator that has this pattern as a part will be returned.
   * @param creatorPattern The pattern for which the creator should be filtered
   */
  public void setCreatorFilter(String creatorPattern);
  
  /**
   * Gets the search pattern for the abstract. Can be a part of the abstract.
   * @return The pattern for which the abstract should be filtered
   */
  public String getAbstractFilter();
  
  /**
   * Sets the search pattern for the abstract. Can be a part of the abstract. Every abstract that has this pattern as a part will be returned.
   * @param text The pattern for which the abstract should be filtered
   */
  public void setAbstractFilter(String text);
  
  /**
   * Gets the beginning of the period in which the event should be that will be returned
   * @return The beginning of the period
   */
  public Date getStart();
  
  /**
   * Sets the beginning of the period in which the event should be that will be returned
   * @param start The beginning of the period
   */
  public void setStart(Date start);
  
  /**
   * Gets the end of the period in which the event should be that will be returned
   * @return The end of the period
   */
  public Date getEnd();
  
  /**
   * Sets the end of the period in which the event should be that will be returned
   * @param end The end of the period
   */
  public void setEnd(Date end);
  
  /**
   * Gets the search pattern for the contributor. Can be a part of the contributor.
   * @return The pattern for which the contributor should be filtered
   */
  public String getContributorFilter();
  
  /**
   * Sets the search pattern for the contributor. Can be a part of the contributor. Every contributor that has this pattern as a part will be returned.
   * @param contributorPattern The pattern for which the contributor should be filtered
   */
  public void setContributorFilter(String contributorPattern);
  
  /**
   * Gets the ID of the series for which the event should be filtered. Has to be the complete ID 
   * @return The ID of the Series
   */
  public String getSeriesIDFilter();
  
  /**
   * Sets the ID of the series for which the event should be filtered. Has to be the complete ID 
   * @param seriesID The ID of the Series
   */
  public void setSeriesIDFilter(String seriesID);
  
  /**
   *  Gets the ID of the channel for which the event should be filtered. Has to be the complete ID
   * @return channelID of the channel
   */
  public String getChannelIDFilter();
  
  /**
   * Sets the ID of the channel for which the event should be filtered. Has to be the complete ID
   * @param channelID ChannelID of the channel
   */
  public void setChannelIDFilter(String channelID);
  
  /**
   * Gets the search pattern for the location. Can be a part of the location.
   * @return The pattern for which the location should be filtered
   */
  public String getLocationFilter();
  
  /**
   * Sets the search pattern for the location. Can be a part of the location. Every location that has this pattern as a part will be returned.
   * @param locationPattern The pattern for which the location should be filtered
   */
  public void setLocationFilter(String locationPattern);
  
  /**
   * Gets the search pattern for the resources. Can be a part of a single resources entry.
   * @return The pattern for which the resources should be filtered
   */
  public String getResourceFilter();
  
  /**
   * Sets the search pattern for the resources. Can be a part of the resources. Every resource that has this pattern as a part will be returned.
   * @param resourcePattern The pattern for which the resources should be filtered
   */
  public void setResourceFilter (String resourcePattern);
  
  /**
   * Gets the search pattern for the attendee. Can be a part of a single attendee entry.
   * @return The pattern for which the attendees should be filtered
   */
  public String getAttendeeFilter();
  
  /**
   * Sets the search pattern for the attendee. Can be a part of the attendees. Every attendee that has this pattern as a part will be returned.
   * @param attendeePattern The pattern for which the attendees should be filtered
   */
  public void setAttendeeFilter (String attendeePattern);
  
  /**
   * Sets the attribute by which the results should be ordered 
   * @param order eventID | seriesID | channelID | deviceID | location | creator | contributor | date
   */
  public boolean setOrderBy (String order);
  
  /**
   * Gets the attribute by which the results should be ordered 
   * @return eventID | seriesID | channelID | deviceID | location | creator | contributor | date
   */
  public String getOrderBy ();
}
