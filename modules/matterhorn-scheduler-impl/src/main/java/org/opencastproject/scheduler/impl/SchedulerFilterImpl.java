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
package org.opencastproject.scheduler.impl;

import java.util.Date;


import org.opencastproject.scheduler.api.SchedulerFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the SchedulerFilter interface
 *
 */
public class SchedulerFilterImpl implements SchedulerFilter {
  
  private static final Logger logger = LoggerFactory.getLogger(SchedulerFilterImpl.class);

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
  String orderBySQL;
  
  public SchedulerFilterImpl () {
    
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#getAbstractFilter()
   */
  @Override
  public String getAbstractFilter() {
    return abstr;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#getAttendeeFilter()
   */
  @Override
  public String getAttendeeFilter() {
    return attendee;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#getChannelIDFilter()
   */
  @Override
  public String getChannelIDFilter() {
    return channelID;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#getContributorFilter()
   */
  @Override
  public String getContributorFilter() {
    return contributor;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#getCreatorFilter()
   */
  @Override
  public String getCreatorFilter() {
    return creator;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#getDeviceFilter()
   */
  @Override
  public String getDeviceFilter() {
    return device;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#getEnd()
   */
  @Override
  public Date getEnd() {
    return end;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#getEventIDFilter()
   */
  @Override
  public String getEventIDFilter() {
    return eventID;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#getLocationFilter()
   */
  @Override
  public String getLocationFilter() {
    return location;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#getResourceFilter()
   */
  @Override
  public String getResourceFilter() {
    return resource;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#getSeriesIDFilter()
   */
  @Override
  public String getSeriesIDFilter() {
    return seriesID;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#getStart()
   */
  @Override
  public Date getStart() {
    return start;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#getTitleFilter()
   */
  @Override
  public String getTitleFilter() {
    return title;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#setAbstractFilter(java.lang.String)
   */
  @Override
  public void setAbstractFilter(String text) {
    this.abstr = text;

  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#setAttendeeFilter(java.lang.String)
   */
  @Override
  public void setAttendeeFilter(String attendeePattern) {
    attendee = attendeePattern;

  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#setChannelIDFilter(java.lang.String)
   */
  @Override
  public void setChannelIDFilter(String channelID) {
    this.channelID = channelID;

  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#setContributorFilter(java.lang.String)
   */
  @Override
  public void setContributorFilter(String contributorPattern) {
    contributor = contributorPattern;

  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#setCreatorFilter(java.lang.String)
   */
  @Override
  public void setCreatorFilter(String creatorPattern) {
    creator = creatorPattern;

  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#setDeviceFilter(java.lang.String)
   */
  @Override
  public void setDeviceFilter(String devicePattern) {
    device = devicePattern;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#setEnd(java.util.Date)
   */
  @Override
  public void setEnd(Date end) {
    this.end = end;

  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#setEventIDFilter(java.lang.String)
   */
  @Override
  public void setEventIDFilter(String eventID) {
    this.eventID = eventID;

  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#setLocationFilter(java.lang.String)
   */
  @Override
  public void setLocationFilter(String locationPattern) {
    location = locationPattern;

  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#setResourceFilter(java.lang.String)
   */
  @Override
  public void setResourceFilter(String resourcePattern) {
    resource = resourcePattern;

  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#setSeriesIDFilter(java.lang.String)
   */
  @Override
  public void setSeriesIDFilter(String seriesID) {
    this.seriesID = seriesID;

  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#setStart(java.util.Date)
   */
  @Override
  public void setStart(Date start) {
    this.start = start;

  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#setTitleFilter(java.lang.String)
   */
  @Override
  public void setTitleFilter(String titlePattern) {
    title = titlePattern;

  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#getOrderBy()
   */
  @Override
  public String getOrderBy() {
    return orderBy;
  }
  
  public String getOrderBySQL() {
    return orderBySQL;
  }  

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerFilter#setOrderBy(java.lang.String)
   */
  @Override
  public boolean setOrderBy(String order) throws IllegalArgumentException {
    if (order == null) {
      logger.error ("Order is null");
      return false;
    }
    if (! (order.equalsIgnoreCase("title") || order.equalsIgnoreCase("creator") || order.equalsIgnoreCase("series") || 
        order.equalsIgnoreCase("time-asc") || order.equalsIgnoreCase("time-desc") || order.equalsIgnoreCase("contributor") || 
        order.equalsIgnoreCase("channel") || order.equalsIgnoreCase("location") || order.equalsIgnoreCase("device"))) 
          throw new IllegalArgumentException("No valid value for order: "+order);
    orderBy = order;
    orderBySQL = order;
    if (order.equals("series")) orderBySQL = "seriesid";
    if (order.equals("device")) orderBySQL = "deviceid";
    if (order.equals("channel")) orderBySQL = "channelid";
    if (order.equals("time-asc")) orderBySQL = "startdate ASC";
    if (order.equals("time-desc")) orderBySQL = "startdate DESC";
    return true;
  }
  
  /**
   * 
   * {@inheritDoc}
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString () {
    String result = "SchedulerFilter = ";
    if (attendee != null) result += "attendee pattern = "+attendee+", ";
    if (resource != null) result += "resource pattern = "+resource+", ";
    if (device != null) result += "device pattern = "+device+", ";
    if (title != null) result += "title pattern = "+title+", ";
    if (creator != null) result += "creator pattern = "+creator+", ";
    if (abstr != null) result += "abstract pattern = "+abstr+", ";
    if (contributor != null) result += "contributor pattern = "+contributor+", ";
    if (seriesID != null) result += "seriesID = "+seriesID+", ";
    if (channelID != null) result += "channelID pattern = "+channelID+", ";
    if (location != null) result += "location pattern = "+location+", ";
    if (eventID != null) result += "eventID = "+eventID+", ";
    if (start != null) result += "start of period = "+start+", ";
    if (end != null) result += "end of period = "+end+", ";
    if (orderBy != null) result += "order by "+orderBy;
    return result;
  }

}
