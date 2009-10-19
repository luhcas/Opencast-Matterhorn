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

import org.opencastproject.scheduler.api.SchedulerEvent;
import org.opencastproject.scheduler.api.SchedulerFilter;
import org.opencastproject.scheduler.api.SchedulerService;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * FIXME -- Add javadocs
 */
public class SchedulerServiceImpl implements SchedulerService, ManagedService {
  private static final Logger logger = LoggerFactory.getLogger(SchedulerServiceImpl.class);
  
  HashMap<String, SchedulerEvent> events;
  
  public SchedulerServiceImpl() {
    events = new HashMap<String, SchedulerEvent>();
    
    //DEMO data for Stub
    SchedulerEvent event = new SchedulerEventImpl();
    event.addAttendee("recorder1");
    event.addAttendee("recorder2");
    event.addResource("lecturer, 640x480, 512K");
    event.addResource("audio, 44100kHz, 128K");
    event.setID("1");
    event.setDevice("recorder1");
    event.setTitle("Mattherhorn introduction");
    event.setCreator("Adam Hochmann");
    event.setAbstract("Mattherhorn is a development project from the OpenCast Community.\nThe first prototype will be available in January 2010.");
    event.setStartdate((new GregorianCalendar(2010,0,15,12,00)).getTime());
    event.setEnddate((new GregorianCalendar(2010,0,15,14,00)).getTime());
    event.setContributor("OpenCast Project");
    event.setSeriesID("19960401-080045-4000F192713-0052@host1.com");
    event.setChannelID("1");
    event.setLocation("room 101");
    
    addEvent(event);
    
    SchedulerEvent event2 = new SchedulerEventImpl();
    event2.addAttendee("recorder3");
    event2.addAttendee("recorder4");
    event2.addResource("lecturer, 640x480, 512K");
    event2.addResource("audio, 44100kHz, 128K");
    event2.setID("2");
    event2.setDevice("recorder3");
    event2.setTitle("Mattherhorn introduction");
    event2.setCreator("Adam Hochmann");
    event2.setAbstract("Mattherhorn is a development project from the OpenCast Community.\nThe first prototype will be available in January 2010.");
    event2.setStartdate((new GregorianCalendar(2010,0,15,12,00)).getTime());
    event2.setEnddate((new GregorianCalendar(2010,0,15,14,00)).getTime());
    event2.setContributor("OpenCast Project");
    event2.setSeriesID("19960401-080045-4000F192713-0052@host1.com");
    event2.setChannelID("1");
    event2.setLocation("room 206");
    
    addEvent(event2);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerService#addEvent(org.opencastproject.scheduler.api.SchedulerEvent)
   */
  public SchedulerEvent addEvent(SchedulerEvent e) {
    events.put(e.getID(), e);
    return e;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerService#getCalendarForCaptureAgent(java.lang.String)
   */
  public String getCalendarForCaptureAgent(String captureAgentID) {
    // TODO real URL
    return "https://wiki.opencastproject.org/confluence/download/attachments/7110717/MatterhornExample2.ics";
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerService#getEvent(java.lang.String)
   */
  public SchedulerEvent getEvent(String eventID) {
    return events.get(eventID);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerService#getEvents(org.opencastproject.scheduler.api.SchedulerFilter)
   */
  public SchedulerEvent[] getEvents(SchedulerFilter filter) {
    return events.values().toArray(new SchedulerEvent[0]);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerService#removeEvent(java.lang.String)
   */
  public boolean removeEvent(String eventID) {
    events.remove(eventID);
    return true;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerService#updateEvent(org.opencastproject.scheduler.api.SchedulerEvent)
   */
  public boolean updateEvent(SchedulerEvent e) {
    removeEvent(e.getID());
    addEvent(e);
    return true;
  }

  /**
   * {@inheritDoc}
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  public void updated(Dictionary properties) throws ConfigurationException {
    
  }

}

