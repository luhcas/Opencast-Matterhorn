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

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Dictionary;

/**
 * Implementation of org.opencastproject.scheduler.api.SchedulerService
 * @see org.opencastproject.scheduler.api.SchedulerService
 */
public abstract class SchedulerServiceImpl implements SchedulerService, ManagedService {
  private static final Logger logger = LoggerFactory.getLogger(SchedulerServiceImpl.class);
 
  /** 
   * Properties that are updated by ManagedService updated method
   */
  protected Dictionary properties;
  
  /**
   * The component context that is passed when activate is called
   */
  protected ComponentContext componentContext;  
  
  DublinCoreGenerator dcGenerator;
  CaptureAgentMetadataGenerator caGenerator;
 
  /**
   * Sets a DublinCoreGenerator
   * @param dcGenerator The DublinCoreGenerator that should be used
   */
  public void setDublinCoreGenerator(DublinCoreGenerator dcGenerator) {
    this.dcGenerator = dcGenerator;
  }

  /**
   * Sets the CaptureAgentMetadataGenerator 
   * @param caGenerator The CaptureAgentMetadataGenerator that should be used
   */
  public void setCaptureAgentMetadataGenerator(CaptureAgentMetadataGenerator caGenerator) {
    this.caGenerator = caGenerator;
  }
  
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
    SchedulerFilter filter = getFilterForCaptureAgent (captureAgentID); 
    CalendarGenerator cal = new CalendarGenerator(dcGenerator, caGenerator);
    SchedulerEvent[] events = getEvents(filter);
    for (int i = 0; i < events.length; i++) cal.addEvent(events[i]);
    return cal.getCalendar().toString(); // CalendarOutputter performance sucks (jmh)
  }

  /**
   * resolves the appropriate Filter for the Capture Agent 
   * @param captureAgentID The ID as provided by the capture agent 
   * @return the Filter for this capture Agent.
   */
  private SchedulerFilter getFilterForCaptureAgent(String captureAgentID) {
    SchedulerFilter filter = new SchedulerFilterImpl();
    filter.setDeviceFilter(captureAgentID);
    filter.setOrderBy("time-desc");
    filter.setStart(new Date(System.currentTimeMillis()));
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
   * This method will be called, when the bundle gets unloaded from OSGI 
   */
  public void deactivate ()  {

  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerService#getUpcomingEvents()
   */
  public SchedulerEvent [] getUpcomingEvents() {
    SchedulerFilter filter = new SchedulerFilterImpl();
    filter.setStart(new Date(System.currentTimeMillis()));
    return getEvents(filter);
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerService#getRecordingEvents()
   */
  public SchedulerEvent [] getCapturingEvents() {
    return getCapturingEvents();
  }
  
  /**
   * {@inheritDoc}
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  public void updated(Dictionary properties) throws ConfigurationException {
    this.properties = properties;
  }  
  
  /**
   * This method will be called, when the bundle gets loaded from OSGI
   * @param componentContext The ComponetnContext of the OSGI bundle
   */
  public void activate(ComponentContext componentContext) {
    logger.info("SchedulerService activated.");
    if (componentContext == null) {
      logger.error("Could not activate because of missing ComponentContext");
      return;
    }
    this.componentContext = componentContext;
    URL dcMappingURL = componentContext.getBundleContext().getBundle().getResource("config/dublincoremapping.properties");
    logger.debug("Using Dublin Core Mapping from {}.",dcMappingURL);
    try {
      if (dcMappingURL != null)  {
        URLConnection con = dcMappingURL.openConnection();
        dcGenerator = new DublinCoreGenerator(con.getInputStream());
      }
    } catch (IOException e) {
      logger.error("Could not open URL connection to Dublin Core Mapping File after activation");
    }
    
    URL caMappingURL = componentContext.getBundleContext().getBundle().getResource("config/captureagentmetadatamapping.properties");
    logger.debug("Using Capture Agent Metadata Mapping from {}.", caMappingURL);
    try {
      if (caMappingURL != null) {
        URLConnection con = caMappingURL.openConnection();
        caGenerator = new CaptureAgentMetadataGenerator(con.getInputStream());
      }
    } catch (IOException e) {
      logger.error("Could not open URL connection to Capture Agent Metadata Mapping File after activation");
    }    
  }  
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerService#getDublinCoreMetadata(java.lang.String)
   */
  public String getDublinCoreMetadata (String eventID) {
    SchedulerEvent event = getEvent(eventID);
    if (dcGenerator == null){
      logger.error("Dublin Core generator not initialized");
      return null;
    }
    return dcGenerator.generateAsString(event);
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerService#getDublinCoreMetadata(java.lang.String)
   */
  public String getCaptureAgentMetadata (String eventID) {
    SchedulerEvent event = getEvent(eventID);
    if (caGenerator == null){
      logger.error("Capture Agent Metadata generator not initialized");
      return null;
    }
    return caGenerator.generateAsString(event);
  }  
  
}
