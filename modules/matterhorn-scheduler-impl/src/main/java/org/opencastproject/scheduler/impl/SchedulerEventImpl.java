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
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.UUID;

import org.opencastproject.scheduler.api.SchedulerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the Interface Scheduler event. This class is used to store event specific data.
 * This implementations has a slight dependency on SchedulerServiceImplDAO, because the length of the Strings is limited 
 * due to the dependencies of the database.
 */
public class SchedulerEventImpl implements SchedulerEvent {

  private static final Logger logger = LoggerFactory.getLogger(SchedulerEventImpl.class);
  
  LinkedList <String> attendees; // TODO: these look more like SortedSets than LinkedLists to me (jmh)
  LinkedList <String> resources;
  String id;
  Date start = new Date(0);
  Date end = new Date(0);
  Hashtable<String, String> metadata;
  
  int maxLengthKey = 0;
  int maxLengthValue = 0;
  
  public SchedulerEventImpl () {
    attendees = new LinkedList<String>();
    resources = new LinkedList<String>();
    metadata = new Hashtable<String, String>();
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#addAttendee(java.lang.String)
   */
  public void addAttendee(String attendee) {
    synchronized (attendees) {
      if (! attendees.contains(attendee)) attendees.add(attendee); // only save unique attendees  
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#addResource(java.lang.String)
   */
  public void addResource(String resource) {
    
    synchronized (resources) {
      if (! resources.contains(resource)) resources.add(resource); // only save unique resources
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#getAbstract()
   */
  public String getAbstract() {
    return metadata.get("abstract");
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
    return metadata.get("channel-id");
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#getContributor()
   */
  public String getContributor() {
    return metadata.get("contributor");
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#getCreator()
   */
  public String getCreator() {
    return metadata.get("creator");
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#getDevice()
   */
  public String getDevice() {
    return metadata.get("device");
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
    return metadata.get("location");
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
    return metadata.get("series-id");
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
    return metadata.get("title");
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#setAbstract(java.lang.String)
   */
  public void setAbstract(String text) {
    put("abstract", text);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#setAttendees(java.lang.String[])
   */
  public void setAttendees(String[] attendees) {
    this.attendees = new LinkedList<String>();
    for (int i = 0; i < attendees.length; i++) this.attendees.add(trimText(attendees[i], 255));
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#setChannelID(java.lang.String)
   */
  public void setChannelID(String channelID) {
    put("channel-id", channelID);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#setContributor(java.lang.String)
   */
  public void setContributor(String contributor) {
    put("contributor", contributor);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#setCreator(java.lang.String)
   */
  public void setCreator(String creator) {
    put("creator", creator);

  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#setDevice(java.lang.String)
   */
  public void setDevice(String device) {
    if (! attendees.contains(device)) addAttendee(device);
    put("device", device);

  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#setEnddate(java.util.Date)
   */
  public void setEnddate(Date end) throws IllegalArgumentException {
    logger.debug("Event {} set enddate {}", id, start.getTime());
    if (start.getTime() > 0 && end.getTime() > 0 && end.before(start)) throw new IllegalArgumentException ("End "+ end + " before start-date "+start);
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
    put("location", location);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#setResources(java.lang.String[])
   */
  public void setResources(String[] resources) {
    this.resources = new LinkedList<String>();
    for (int i = 0; i < resources.length ; i++) this.resources.add(trimText(resources[i],2048));
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#setSeriesID(java.lang.String)
   */
  public void setSeriesID(String seriesID) {
    put("series-id", seriesID);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#setStartdate(java.util.Date)
   */
  public void setStartdate(Date start) throws IllegalArgumentException{
    logger.debug("Event {} set startdate {}", id, start.getTime()); 
    if (end.getTime() > 0 && start.getTime() > 0 && end.before(start)) throw new IllegalArgumentException("Start "+start+" before end-date "+end);
    this.start = start;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#setTitle(java.lang.String)
   */
  public void setTitle(String title) {
    put("title", title);
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#valid()
   */  
  public boolean valid () {
    logger.debug("Title Valid?");
    if (metadata.get("title") == null) return false;
    logger.debug("Start time Valid?");
    if (start.getTime() == 0) return false;
    logger.debug("End time Valid?");
    if (end.getTime() == 0) return false;
    logger.debug("Start before End Valid?");
    if (end.before(start)) return false;
    logger.debug("Event is Valid!");
    return true;
  }
  
  public String toString () {
    // FIXME replace string concatenation with commons lang's ToStringBuilder
    String text = "ID: "+getID()+", start: "+getStartdate().toString()+", end: "+getEnddate().toString()+", creator: "+getCreator()+", title: "+getTitle()+
          ", abstract: "+getAbstract()+", device: "+getDevice()+", location: "+getLocation()+", series: "+getSeriesID()+
          ", channel: "+getChannelID()+", attendees: ";
    String [] att = getAttendees();
    if (att != null) for (int i=0; i<att.length; i++) text+= att[i]+", ";
    String [] res = getResources();
    text += " resources: ";
    if (res != null) for (int i=0; i<res.length; i++) text+= res[i]+", ";
    text += "metadata: ";      
    if (metadata != null) {
      String[] keys = metadata.keySet().toArray(new String[0]);
      for (int i = 0; i < keys.length; i++) text += keys[i]+"="+metadata.get(keys[i])+", "; 
    }
    return text;
    }
  
  /**
   * set a arbitrary key value pair of metadata. If this key-value-pair is not included in the mapping later on it will be ignored.
   * @param key the key for the metadata
   * @param value the value for the metadata
   */
  public void setMetadata (String key, String value) {
    put(key, value);
  }
  
  /**
   * Set a complet set of metadata
   * @param meta the Hasttable with the key value pairs
   */
  public void setMetadata (Hashtable <String, String> meta) {
    metadata = meta;
  }
  
  /**
   * get the a value for a specific key from the metadata
   * @param key 
   * @return the value for the given key
   */
  public String getMetadata(String key) {
    return metadata.get(key);
  }
  
  /**
   * get the complete set of Metadata from the event
   * @return The Hashtable with the metadata
   */
  public Hashtable<String, String> getMetadata () {
    return metadata;
  }
  
  /**
   * get all keys that the metadata contains
   * @return all keys from the metadata.
   */
  public String [] getMetadataKeys () {
    return metadata.keySet().toArray(new String [0]);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerEvent#createID()
   */
  public String createID() {
    return UUID.randomUUID().toString();
  }
  
  /**
   * Trims a String that must fit into a varchar(length) column
   * @param text the text that should be checked for its length
   * @param length the maximum length of the string
   * @return text text with the maximum character length specified in length
   */ 
  private String trimText (String text, int length) {
    if (text.length() > length) {
      logger.warn("Value for {} to long. Only {} characters allowed.", text, length);
      return text.substring(0, length-1);
    }
    return text;
  } 
  
  /**
   * Store data in the local Hashtable and make sure the key and value can be stored in database
   * This somehow has a dependency on the service current implementation, because keys can only be 255 chars and values 4096 chars   
   * @param key Keys longer than 255 chars will be shortened
   * @param value Values longer than 4096 chars will be shortened
   */
  private void put (String key, String value) {
    if (key == null || value == null ) {
      logger.error("Could not store value {} under key {}.",value,key);
      return;
    }
    metadata.put(trimText(key, 255), trimText(value, 4096));
  }

}
