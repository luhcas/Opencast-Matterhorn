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
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Dictionary;

/**
 * FIXME -- Add javadocs
 */
public abstract class SchedulerServiceImpl implements SchedulerService, ManagedService {
  private static final Logger logger = LoggerFactory.getLogger(SchedulerServiceImpl.class);
 
  /** 
   * Properties that are updated by ManagedService updated method
   */
  Dictionary properties;
 
  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerService#addEvent(org.opencastproject.scheduler.api.SchedulerEvent)
   */
  public abstract SchedulerEvent addEvent(SchedulerEvent e);
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerService#getCalendarForCaptureAgent(java.lang.String)
   */
  public String getCalendarForCaptureAgent(String captureAgentID) {
    // TODO real URL
    SchedulerFilter filter = getFilterForCaptureAgent (captureAgentID); 
    CalendarGenerator cal = new CalendarGenerator();
    SchedulerEvent[] events = getEvents(filter);
    for (int i = 0; i < events.length; i++) cal.addEvent(events[i]);
    return cal.getCalendar().toString();
  }

  /**
   * resolves the appropriate Filter for the Capture Agent 
   * @param captureAgentID The ID as provided by the capture agent 
   * @return the Filter for this capture Agent.
   */
  private SchedulerFilter getFilterForCaptureAgent(String captureAgentID) {
    SchedulerFilter filter = new SchedulerFilterImpl();
    filter.setDeviceFilter(captureAgentID);
    filter.setOrderBy("time-asc");
    return filter;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerService#getEvent(java.lang.String)
   */
  public abstract SchedulerEvent getEvent(String eventID);
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerService#getEvents(org.opencastproject.scheduler.api.SchedulerFilter)
   */
  public abstract SchedulerEvent[] getEvents(SchedulerFilter filter);
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerService#removeEvent(java.lang.String)
   */
  public abstract boolean removeEvent(String eventID);
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerService#updateEvent(org.opencastproject.scheduler.api.SchedulerEvent)
   */
  public abstract boolean updateEvent(SchedulerEvent e);
  
  
  /**
   * This method will be called, when the bundle gets loaded from OSGI
   * @param componentContext The ComponetnContext of the OSGI bundle
   */
  //todo: if this method is empty do we still need it?
  public void activate(ComponentContext componentContext) {
    
  }
  
  /**
   * This method will be called, when the bundle gets unloaded from OSGI 
   */
  //todo: if this method is empty do we still need it?
  public void deactivate ()  {

  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerService#getUpcommingEvents()
   */
  public SchedulerEvent [] getUpcommingEvents() {
    SchedulerFilter filter = new SchedulerFilterImpl();
    filter.setStart(new Date(System.currentTimeMillis()));
    return getEvents(filter);
  }
  
  /**
   * {@inheritDoc}
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  public void updated(Dictionary properties) throws ConfigurationException {
    this.properties = properties;
  }  
  
}
