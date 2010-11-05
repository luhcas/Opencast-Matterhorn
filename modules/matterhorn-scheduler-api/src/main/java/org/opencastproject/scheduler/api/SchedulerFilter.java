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

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;
/**
 * The Scheduler Filter is used to filter the events that would come back from an search operation on the database.
 * 
 */
public class SchedulerFilter {

  protected String devicePattern;
  protected String titlePattern;
  protected String creatorPattern;
  protected String seriesPattern;
  protected String order;
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

  public SchedulerFilter withCreatorFilter(String creatorPattern) {
    this.creatorPattern = creatorPattern;
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
   * Sets the search pattern for the series title. Can be a part of the series title. Every series title that has
   * a part of this pattern will be returned.
   *
   * @param seriesPattern
   *        The pattern for which series title should be filtered.
   */
  public SchedulerFilter withSeriesFilter(String seriesPattern) {
    this.seriesPattern = seriesPattern;
    return this;
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
   * Sets the attribute by which the results should be ordered
   * 
   * @param order
   *          eventID | seriesID | channelID | deviceID | location | creator | contributor | date
   */
  public SchedulerFilter withOrder(String order) {
    this.order = order;
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

}
