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
package org.opencastproject.scheduler.endpoint;

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.opencastproject.scheduler.api.SchedulerFilter;
import org.opencastproject.scheduler.impl.SchedulerFilterImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JaxB Implementation of SchedulerFilter
 *
 */

@XmlRootElement(name="schedulerFilter")
@XmlAccessorType(XmlAccessType.FIELD)
public class SchedulerFilterJaxbImpl {
  private static final Logger logger = LoggerFactory.getLogger(SchedulerFilterJaxbImpl.class);
  
  @XmlElement(name="eventId")
  String eventID;
  @XmlElement(name="device")
  String device;
  @XmlElement(name="title")
  String title;
  @XmlElement(name="creator")
  String creator;
  @XmlElement(name="abstract")
  String abstr;
  @XmlElement(name="startDate")
  long start;
  @XmlElement(name="endDate")
  long end;
  @XmlElement(name="contributor")
  String contributor;
  @XmlElement(name="seriesId")
  String seriesID;
  @XmlElement(name="channelId")
  String channelID;
  @XmlElement(name="location")
  String location;
  @XmlElement(name="attendee")
  String attendee;
  @XmlElement(name="resource")
  String resource;
  @XmlElement(name="orderBy")
  String orderBy;
  
  /**
   * Default constructor without any import.
   */
  public SchedulerFilterJaxbImpl() {}
  
  /**
   * TODO don't know where this comes from
   * @param tmp
   */
  public SchedulerFilterJaxbImpl(String tmp) {}
  
  /**
   * Constructs a JaxB representations of a SchedulerFilter
   * @param filter
   */
  public SchedulerFilterJaxbImpl(SchedulerFilter filter) {
    logger.info("Creating a {} from {}", SchedulerFilterJaxbImpl.class.getName(), filter);
    eventID = filter.getEventIDFilter();
    device = filter.getDeviceFilter();
    title = filter.getTitleFilter();
    creator = filter.getCreatorFilter();
    abstr = filter.getAbstractFilter();
    if (filter.getStart() != null) start = filter.getStart().getTime();
    if (filter.getEnd() != null)end = filter.getEnd().getTime();
    contributor = filter.getContributorFilter();
    seriesID = filter.getSeriesIDFilter();
    channelID = filter.getChannelIDFilter();
    location = filter.getLocationFilter();
    attendee = filter.getAttendeeFilter();
    resource = filter.getResourceFilter();
    orderBy = filter.getOrderBy();
    logger.info("Filter created");
  }
  
  /**
   * Converts the JaxB representation of the SchedulerFilter into a regular SchedulerFilter
   * @return the Scheduler Filter represented by this object
   */
  @XmlTransient
  public SchedulerFilter getFilter() {
    SchedulerFilterImpl filter = new SchedulerFilterImpl();
    if (eventID != null) filter.setEventIDFilter(eventID);
    if (device != null) filter.setDeviceFilter(device);
    if (title != null) filter.setTitleFilter(title);
    if (creator != null) filter.setCreatorFilter(creator);
    if (abstr != null) filter.setAbstractFilter(abstr);
    if (start > 0) filter.setStart(new Date (start));
    if (end > start) filter.setEnd(new Date(end));
    if (contributor != null) filter.setContributorFilter(contributor);
    if (seriesID != null) filter.setSeriesIDFilter(seriesID);
    if (channelID != null) filter.setChannelIDFilter(channelID);
    if (channelID != null) filter.setLocationFilter(location);
    if (attendee != null) filter.setAttendeeFilter(attendee);
    if (resource != null) filter.setResourceFilter(resource);
    if (orderBy != null) filter.setOrderBy(orderBy);
    return filter;
  }  
 
  /**
   * valueOf function is called by JAXB to bind values. This function calls the Scheduler factory.
   *
   *  @param    xmlString string representation of an event.
   *  @return   instantiated event SchdeulerFilterJaxbImpl.
   */
  public static SchedulerFilterJaxbImpl valueOf(String xmlString) throws Exception {
    return SchedulerBuilder.getInstance().parseSchedulerFilterJaxbImpl(xmlString);
  }
  
}
