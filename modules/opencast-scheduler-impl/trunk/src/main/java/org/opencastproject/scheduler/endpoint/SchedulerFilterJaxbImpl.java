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
package org.opencastproject.scheduler.endpoint;

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.opencastproject.scheduler.api.SchedulerFilter;
import org.opencastproject.scheduler.impl.SchedulerFilterImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Comment me!
 *
 */

@XmlType(name="scheduler-filter", namespace="http://scheduler.opencastproject.org/")
@XmlRootElement(name="scheduler-filter", namespace="http://scheduler.opencastproject.org/")
@XmlAccessorType(XmlAccessType.FIELD)
public class SchedulerFilterJaxbImpl {
  private static final Logger logger = LoggerFactory.getLogger(SchedulerFilterJaxbImpl.class);
  
  @XmlElement(name="event-id")
  String eventID;
  @XmlElement(name="device")
  String device;
  @XmlElement(name="title")
  String title;
  @XmlElement(name="creator")
  String creator;
  @XmlElement(name="abstract")
  String abstr;
  @XmlElement(name="startdate")
  Date start;
  @XmlElement(name="enddate")
  Date end;
  @XmlElement(name="contributor")
  String contributor;
  @XmlElement(name="series-id")
  String seriesID;
  @XmlElement(name="channel-id")
  String channelID;
  @XmlElement(name="location")
  String location;
  @XmlElement(name="attendee")
  String attendee;
  @XmlElement(name="resource")
  String resource;
  @XmlElement(name="order-by")
  String orderBy;
  
  public SchedulerFilterJaxbImpl() {}
  public SchedulerFilterJaxbImpl(String tmp) {}
  public SchedulerFilterJaxbImpl(SchedulerFilter filter) {
    logger.info("Creating a " + SchedulerFilterJaxbImpl.class.getName() + " from " + filter);
    eventID = filter.getEventIDFilter();
    device = filter.getDeviceFilter();
    title = filter.getTitleFilter();
    creator = filter.getCreatorFilter();
    abstr = filter.getAbstractFilter();
    start = filter.getStart();
    end = filter.getEnd();
    contributor = filter.getContributorFilter();
    seriesID = filter.getSeriesIDFilter();
    channelID = filter.getChannelIDFilter();
    location = filter.getLocationFilter();
    attendee = filter.getAttendeeFilter();
    resource = filter.getResourceFilter();
    orderBy = filter.getOrderBy();
  }
  
  @XmlTransient
  public SchedulerFilter getFilter() {
    SchedulerFilterImpl filter = new SchedulerFilterImpl();
    filter.setEventIDFilter(eventID);
    filter.setDeviceFilter(device);
    filter.setTitleFilter(title);
    filter.setCreatorFilter(creator);
    filter.setAbstractFilter(abstr);
    filter.setStart(start);
    filter.setEnd(end);
    filter.setContributorFilter(contributor);
    filter.setSeriesIDFilter(seriesID);
    filter.setChannelIDFilter(channelID);
    filter.setLocationFilter(location);
    filter.setAttendeeFilter(attendee);
    filter.setResourceFilter(resource);
    filter.setOrderBy(orderBy);
    return filter;
  }  
  
  
}