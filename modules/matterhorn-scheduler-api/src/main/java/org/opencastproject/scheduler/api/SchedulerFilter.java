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

/**
 * The Scheduler Filter is used to filter the events that would come back from an search operation on the database.
 * 
 */
public class SchedulerFilter {

  protected String creatorPattern;
  protected String contributorPattern;
  protected String devicePattern;
  protected String seriesPattern;
  protected String seriesId;
  protected String titlePattern;
  protected String order;
  protected boolean betweenStartAndStop = false;
  protected boolean isAscending = true;
  protected boolean includeCurrent = false;
  protected Date start;
  protected Date stop;

  /**
   * Gets the search Pattern for the device ID. Can be a part of the device ID.
   * 
   * @return The pattern for which the device ID should be filtered
   */
  public String getDeviceFilter() {
    return devicePattern;
  }

  /**
   * Sets the search Pattern for the device ID. Can be a part of the device ID. Every device ID that has this pattern as
   * a part will be returned.
   * 
   * @param devicePattern
   *          The pattern for which device ID should be filtered
   */
  public SchedulerFilter withDeviceFilter(String devicePattern) {
    this.devicePattern = devicePattern;
    return this;
  }

  public String getCreatorFilter() {
    return creatorPattern;
  }

  /**
   * Sets the search pattern for the creator string.
   * @param creatorPattern
   * @return SchedulerFilter
   */
  public SchedulerFilter withCreatorFilter(String creatorPattern) {
    this.creatorPattern = creatorPattern;
    return this;
  }
  
  public String getContributorFilter() {
    return contributorPattern;
  }
  
  /**
   * Sets the search pattern for the contributor string.
   * @param contributorPattern
   * @return SchedulerFilter
   */
  public SchedulerFilter withContributorFilter(String contributorPattern) {
    this.contributorPattern = contributorPattern;
    return this;
  }

  /**
   * Gets the search Pattern for the title. Can be a part of the title.
   * 
   * @return The pattern for which the title should be filtered
   */
  public String getTitleFilter() {
    return titlePattern;
  }

  /**
   * Sets the search pattern for the title. Can be a part of the title. Every title that has this pattern as a part will
   * be returned.
   * 
   * @param titlePattern
   *          The pattern for which title should be filtered
   */
  public SchedulerFilter withTitleFilter(String titlePattern) {
    this.titlePattern = titlePattern;
    return this;
  }

  public String getSeriesFilter() {
    return seriesPattern;
  }

  /**
   * Sets the search pattern for the series title. Can be a part of the series title. Every series title that has a part
   * of this pattern will be returned.
   * 
   * @param seriesPattern
   *          The pattern for which series title should be filtered.
   */
  public SchedulerFilter withSeriesFilter(String seriesPattern) {
    this.seriesPattern = seriesPattern;
    return this;
  }
  
  public SchedulerFilter isPartOf(String seriesId) {
    this.seriesId = seriesId;
    return this;
  }
  
  public String getSeriesId() {
    return this.seriesId;
  }

  /**
   * Gets the beginning of the period in which the event should be that will be returned
   * 
   * @return The beginning of the period
   */
  public Date getStart() {
    return start;
  }

  /**
   * Sets the beginning of the period in which the event should be that will be returned
   * 
   * @param start
   *          The beginning of the period
   */
  public SchedulerFilter withStart(Date start) {
    this.start = start;
    return this;
  }

  /**
   * Gets the end of the period in which the event should be that will be returned
   * 
   * @return The end of the period
   */
  public Date getStop() {
    return stop;
  }

  /**
   * Sets the end of the period in which the event should be that will be returned
   * 
   * @param end
   *          The end of the period
   */
  public SchedulerFilter withStop(Date stop) {
    this.stop = stop;
    return this;
  }
  
  /**
   * Sets the start and end periods between which events should be returned.
   * Only events that start and stop in this period will be returned.
   * 
   * @param start
   * @param stop
   * @return SchedulerFilter
   */
  
  public SchedulerFilter between(Date start, Date stop) {
    this.start = start;
    this.stop = stop;
    this.betweenStartAndStop = true;
    return this;
  }
  
  public SchedulerFilter withCurrentAndUpcoming() {
   this.includeCurrent = true; 
   return this;
  }
  
  public boolean getCurrentAndUpcoming() {
    return this.includeCurrent;
  }

  /**
   * Sets the attribute by which the results should be ordered (ascending)
   * 
   * @param order
   *          eventID | seriesID | channelID | deviceID | location | creator | contributor | date
   */
  public SchedulerFilter withOrder(String order) {
    return withOrderAscending(order, true);
  }

  /**
   * Sets the attribute by which the results should be ordered and in what direction.
   * 
   * @param order
   *          eventID | seriesID | channelID | deviceID | location | creator | contributor | date
   * @param isAsc
   *          True if order should be ascending, false if descending.
   */
  public SchedulerFilter withOrderAscending(String order, boolean isAsc) {
    this.order = order;
    this.isAscending = isAsc;
    return this;
  }

  /**
   * Gets the attribute by which the results should be ordered
   * 
   * @return eventID | seriesID | channelID | deviceID | location | creator | contributor | date
   */
  public String getOrder() {
    return order;
  }

  public boolean isOrderAscending() {
    return isAscending;
  }
}
