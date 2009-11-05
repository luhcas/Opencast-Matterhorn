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
import java.util.LinkedList;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.opencastproject.scheduler.api.SchedulerEvent;
import org.opencastproject.scheduler.impl.SchedulerEventImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Comment me!
 *
 */
@XmlType(name="scheduler-event", namespace="http://scheduler.opencastproject.org")
@XmlRootElement(name="scheduler-event", namespace="http://scheduler.opencastproject.org")
@XmlAccessorType(XmlAccessType.FIELD)
public class SchedulerEventJaxbImpl {
  private static final Logger logger = LoggerFactory.getLogger(SchedulerEventJaxbImpl.class);
  
  @XmlID
  String id;
  @XmlElement(name="device-id")
  String deviceID;
  @XmlElement(name="title")
  String title;
  @XmlElement(name="creator")
  String creator;
  @XmlElement(name="abstract")
  String abstr;
  @XmlElement(name="startdate")
  long start;
  @XmlElement(name="enddate")
  long end;
  @XmlElement(name="contributor")
  String contributor;
  @XmlElement(name="series-id")
  String seriesID;
  @XmlElement(name="channel-id")
  String channelID;
  @XmlElement(name="location")
  String location;
  @XmlElement(name="attendees")
  LinkedList <String> attendees;
  @XmlElement(name="resources")
  LinkedList <String> resources;  
  
  public SchedulerEventJaxbImpl() {}
  public SchedulerEventJaxbImpl(SchedulerEvent event) {
    logger.debug("Creating a " + SchedulerEventJaxbImpl.class.getName() + " from " + event);
    id = event.getID();
    deviceID = event.getDevice();
    title = event.getTitle();
    creator = event.getCreator();
    abstr = event.getAbstract();
    start = event.getStartdate().getTime();
    end = event.getEnddate().getTime();
    contributor = event.getContributor();
    seriesID = event.getSeriesID();
    channelID = event.getChannelID();
    location = event.getLocation();
    
    String [] att = event.getAttendees();
    attendees = new LinkedList ();
    for (int i = 0; i < att.length; i++) attendees.add(att[i]);
    
    String [] res = event.getResources();
    resources = new LinkedList ();
    for (int i = 0; i < res.length; i++) resources.add(res[i]);
    
  }
  
  @XmlTransient
  public SchedulerEvent getEvent() {
    SchedulerEventImpl event = new SchedulerEventImpl();
    event.setID(id);
    event.setDevice(deviceID);
    event.setTitle(title);
    event.setCreator(creator);
    event.setAbstract(abstr);
    event.setStartdate(new Date(start));
    event.setEnddate(new Date(end));
    event.setContributor(contributor);
    event.setSeriesID(seriesID);
    event.setChannelID(channelID);
    event.setLocation(location);
    event.setResources(resources.toArray(new String [0]));
    event.setAttendees(attendees.toArray(new String [0]));
    return event;
  }
  
  /**
   * valueOf function is called by JAXB to bind values. This function calls the ScheduleEvent factory.
   *
   *  @param    xml string representation of an event.
   *  @return   instantiated event SchdeulerEventJaxbImpl.
   */
  public static SchedulerEventJaxbImpl valueOf(String xmlString) throws Exception {
    return (SchedulerEventJaxbImpl) SchedulerBuilder.getInstance().parseSchedulerEventJaxbImpl(xmlString);
  }
}
