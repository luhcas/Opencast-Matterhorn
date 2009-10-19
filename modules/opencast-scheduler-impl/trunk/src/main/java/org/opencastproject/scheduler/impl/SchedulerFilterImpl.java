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
package org.opencastproject.scheduler.impl;

import java.util.Date;


import org.opencastproject.scheduler.api.SchedulerFilter;

/**
 * TODO: Comment me!
 *
 */
public class SchedulerFilterImpl implements SchedulerFilter {

  String attendee;
  String resource;
  String device;
  String title;
  String creator;
  String abstr;
  Date start;
  Date end;
  String contributor;
  String seriesID;
  String channelID;
  String location;  
  String eventID;
  String orderBy;
  
  public SchedulerFilterImpl () {
    
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#getAbstractFilter()
   */
  public String getAbstractFilter() {
    return abstr;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#getAttendeeFilter()
   */
  public String getAttendeeFilter() {
    return attendee;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#getChannelIDFilter()
   */
  public String getChannelIDFilter() {
    return channelID;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#getContributorFilter()
   */
  public String getContributorFilter() {
    return contributor;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#getCreatorFilter()
   */
  public String getCreatorFilter() {
    return creator;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#getDeviceFilter()
   */
  public String getDeviceFilter() {
    return device;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#getEnd()
   */
  public Date getEnd() {
    return end;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#getEventIDFilter()
   */
  public String getEventIDFilter() {
    return eventID;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#getLocationFilter()
   */
  public String getLocationFilter() {
    return location;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#getResourceFilter()
   */
  public String getResourceFilter() {
    return resource;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#getSeriesIDFilter()
   */
  public String getSeriesIDFilter() {
    return seriesID;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#getStart()
   */
  public Date getStart() {
    return start;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#getTitleFilter()
   */
  public String getTitleFilter() {
    return title;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#setAbstractFilter(java.lang.String)
   */
  public void setAbstractFilter(String text) {
    this.abstr = text;

  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#setAttendeeFilter(java.lang.String)
   */
  public void setAttendeeFilter(String attendeePattern) {
    attendee = attendeePattern;

  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#setChannelIDFilter(java.lang.String)
   */
  public void setChannelIDFilter(String channelID) {
    this.channelID = channelID;

  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#setContributorFilter(java.lang.String)
   */
  public void setContributorFilter(String contributorPattern) {
    contributor = contributorPattern;

  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#setCreatorFilter(java.lang.String)
   */
  public void setCreatorFilter(String creatorPattern) {
    creator = creatorPattern;

  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#setDeviceFilter(java.lang.String)
   */
  public void setDeviceFilter(String devicePattern) {
    device = devicePattern;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#setEnd(java.util.Date)
   */
  public void setEnd(Date end) {
    this.end = end;

  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#setEventIDFilter(java.lang.String)
   */
  public void setEventIDFilter(String eventID) {
    this.eventID = eventID;

  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#setLocationFilter(java.lang.String)
   */
  public void setLocationFilter(String locationPattern) {
    location = locationPattern;

  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#setResourceFilter(java.lang.String)
   */
  public void setResourceFilter(String resourcePattern) {
    resource = resourcePattern;

  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#setSeriesIDFilter(java.lang.String)
   */
  public void setSeriesIDFilter(String seriesID) {
    this.seriesID = seriesID;

  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#setStart(java.util.Date)
   */
  public void setStart(Date start) {
    this.start = start;

  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#setTitleFilter(java.lang.String)
   */
  public void setTitleFilter(String titlePattern) {
    title = titlePattern;

  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#getOrderBy()
   */
  public String getOrderBy() {
    return orderBy;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#setOrderBy(java.lang.String)
   */
  public boolean setOrderBy(String order) {
    orderBy = order;
    return true;
  }

}
