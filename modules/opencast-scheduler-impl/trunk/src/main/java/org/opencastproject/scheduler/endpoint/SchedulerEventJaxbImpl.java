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
import java.util.Hashtable;
import java.util.LinkedList;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.opencastproject.scheduler.api.SchedulerEvent;
import org.opencastproject.scheduler.impl.SchedulerEventImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Comment me!
 *
 */
@XmlType(name="SchedulerEvent", namespace="http://scheduler.opencastproject.org")
@XmlRootElement(name="SchedulerEvent", namespace="http://scheduler.opencastproject.org")
@XmlAccessorType(XmlAccessType.FIELD)
public class SchedulerEventJaxbImpl {
  private static final Logger logger = LoggerFactory.getLogger(SchedulerEventJaxbImpl.class);
  
  @XmlID
  String id;
  @XmlJavaTypeAdapter(value=HashtableAdapter.class)
  Hashtable<String, String> metadata;
  @XmlElement(name="startdate")
  long start;
  @XmlElement(name="enddate")
  long end;
  /**
   * Duration can be alternative to enddate. If duration is given enddate will be ignored.
   */
  @XmlElement(name="duration") 
  long duration;
  @XmlElementWrapper(name="attendees")
  @XmlElement(name="attendee")
  LinkedList <String> attendees;
  @XmlElementWrapper(name="resources")
  @XmlElement(name="resource")
  LinkedList <String> resources;  
  
  public SchedulerEventJaxbImpl() {
    metadata = new Hashtable<String, String>();
  }
  public SchedulerEventJaxbImpl(SchedulerEvent event) {
    logger.info("Creating a " + SchedulerEventJaxbImpl.class.getName() + " from " + event);
    id = event.getID();
    metadata = event.getMetadata();
    start = event.getStartdate().getTime();
    end = event.getEnddate().getTime();
    duration = end-start;
    
    String [] att = event.getAttendees();
    attendees = new LinkedList <String>();
    for (int i = 0; i < att.length; i++) attendees.add(att[i]);
    
    String [] res = event.getResources();
    resources = new LinkedList <String>();
    for (int i = 0; i < res.length; i++) resources.add(res[i]);
    
  }
  
  @XmlTransient
  public SchedulerEvent getEvent() {
    SchedulerEventImpl event = new SchedulerEventImpl();
    if(id != ""){
      event.setID(id);
    }else{
      event.setID(event.createID());
    }
    event.setMetadata(metadata);
    event.setStartdate(new Date(start));
    if (duration > 0 )
      event.setStartdate(new Date(start+duration));
    else event.setEnddate(new Date(end));
    event.setResources(resources.toArray(new String [0]));
    event.setAttendees(attendees.toArray(new String [0]));
    logger.info("Event created "+event.toString());
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
