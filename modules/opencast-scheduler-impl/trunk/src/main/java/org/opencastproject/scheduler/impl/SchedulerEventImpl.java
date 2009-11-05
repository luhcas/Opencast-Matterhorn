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
import java.util.LinkedList;

import org.opencastproject.scheduler.api.SchedulerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Comment me!
 *
 */
public class SchedulerEventImpl implements SchedulerEvent {

  private static final Logger logger = LoggerFactory.getLogger(SchedulerEventImpl.class);
  LinkedList <String> attendees;
  LinkedList <String> resources;
  String id;
  String deviceID;
  String title;
  String creator;
  String abstr;
  Date start = new Date();
  Date end = new Date();
  String contributor;
  String seriesID;
  String channelID;
  String location;
  
  public SchedulerEventImpl () {
    attendees = new LinkedList<String>();
    resources = new LinkedList<String>();
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#addAttendee(java.lang.String)
   */
  public void addAttendee(String attendee) {
    if (! attendees.contains(attendee)) attendees.add(attendee); // only save unique attendees
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#addResource(java.lang.String)
   */
  public void addResource(String resource) {
    if (! resources.contains(resource)) resources.add(resource); // only save unique resources
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#getAbstract()
   */
  public String getAbstract() {
    return abstr;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#getAttendees()
   */
  public String[] getAttendees() {
    return attendees.toArray(new String [0]);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#getChannelID()
   */
  public String getChannelID() {
    return channelID;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#getContributor()
   */
  public String getContributor() {
    return contributor;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#getCreator()
   */
  public String getCreator() {
    return creator;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#getDevice()
   */
  public String getDevice() {
    return deviceID;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#getEnddate()
   */
  public Date getEnddate() {
    return end;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#getID()
   */
  public String getID() {
    return id;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#getLocation()
   */
  public String getLocation() {
    return location;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#getResources()
   */
  public String[] getResources() {
    return resources.toArray(new String [0]);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#getSeriesID()
   */
  public String getSeriesID() {
    return seriesID;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#getStartdate()
   */
  public Date getStartdate() {
    return start;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#getTitle()
   */
  public String getTitle() {
    return title;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#setAbstract(java.lang.String)
   */
  public void setAbstract(String text) {
    abstr=text;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#setAttendees(java.lang.String[])
   */
  public void setAttendees(String[] attendees) {
    this.attendees = new LinkedList<String>();
    for (int i = 0; i < attendees.length; i++) this.attendees.add(attendees[i]);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#setChannelID(java.lang.String)
   */
  public void setChannelID(String channelID) {
    this.channelID = channelID;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#setContributor(java.lang.String)
   */
  public void setContributor(String contributor) {
    this.contributor = contributor;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#setCreator(java.lang.String)
   */
  public void setCreator(String creator) {
    this.creator = creator;

  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#setDevice(java.lang.String)
   */
  public void setDevice(String device) {
    if (! attendees.contains(device)) addAttendee(device);
    this.deviceID = device;

  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#setEnddate(java.util.Date)
   */
  public void setEnddate(Date end) throws IllegalArgumentException {
    logger.debug("Event "+id+" set enddate "+start.getTime());
    if (start != null && end != null && end.before(start)) throw new IllegalArgumentException ("End "+ end + " before start-date "+start);
    this.end = end;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#setID(java.lang.String)
   */
  public void setID(String id) {
    this.id = id;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#setLocation(java.lang.String)
   */
  public void setLocation(String location) {
    this.location = location;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#setResources(java.lang.String[])
   */
  public void setResources(String[] resources) {
    this.resources = new LinkedList<String>();
    for (int i = 0; i < resources.length ; i++) this.resources.add(resources[i]);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#setSeriesID(java.lang.String)
   */
  public void setSeriesID(String seriesID) {
    this.seriesID = seriesID;

  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#setStartdate(java.util.Date)
   */
  public void setStartdate(Date start) throws IllegalArgumentException{
    logger.debug("Event "+id+" set startdate "+start.getTime()); 
    if (end != null && start != null && end.before(start)) throw new IllegalArgumentException("Start "+start+" before End-date "+end);
    this.start = start;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#setTitle(java.lang.String)
   */
  public void setTitle(String title) {
    this.title = title;

  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#valid()
   */  
  public boolean valid () {
    if (title == null) return false;
    if (start == null) return false;
    if (end == null) return false;
    if (end.before(start)) return false;
    return true;
  }
  
  public String toString () {
    String text = "ID: "+getID()+", start: "+getStartdate().toString()+", end: "+getEnddate().toString()+", creator: "+getCreator()+", title: "+getTitle()+
          ", abstract: "+getAbstract()+", device: "+getDevice()+", location: "+getLocation()+", series: "+getSeriesID()+
          ", channel: "+getChannelID()+", attendees: ";
    String [] att = getAttendees();
    if (att != null) for (int i=0; i<att.length; i++) text+= att[i]+", ";
    String [] res = getResources();
    text += "resources: ";
    if (res != null) for (int i=0; i<res.length; i++) text+= res[i]+", ";
    return text;
    }

}
