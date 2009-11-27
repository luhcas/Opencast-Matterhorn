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

import org.opencastproject.scheduler.api.SchedulerEvent;
import org.opencastproject.scheduler.api.SchedulerService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

/**
 * @see SchedulerWebService
 * TODO SOAP endpoint needs to be tested.
 */
@WebService()
public class SchedulerWebServiceImpl implements SchedulerWebService {
  private static final Logger logger = LoggerFactory.getLogger(SchedulerWebServiceImpl.class);
  
  private SchedulerService service;
  public void setService(SchedulerService service) {
    this.service = service;
  }

  public void unsetService(SchedulerService service) {
    this.service = null;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.endpoint.SchedulerWebService#addEvent(org.opencastproject.scheduler.api.SchedulerEvent)
   */
  @WebMethod()
  @WebResult(name="scheduler-event")
  public SchedulerEventJaxbImpl addEvent (@WebParam(name="scheduler-event") SchedulerEventJaxbImpl e) {
    return new SchedulerEventJaxbImpl (service.addEvent(e.getEvent()));
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.endpoint.SchedulerWebService#getCalendarForCaptureAgent(java.lang.String)
   */
  @WebMethod()
  @WebResult(name="url")
  public String getCalendarForCaptureAgent (@WebParam(name="captureAgentID")String captureAgentID) {
    return service.getCalendarForCaptureAgent(captureAgentID);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.endpoint.SchedulerWebService#getEvent(java.lang.String)
   */
  @WebMethod()
  @WebResult(name="scheduler-event")
  public SchedulerEventJaxbImpl getEvent (@WebParam(name="eventID")String eventID) {
    return new SchedulerEventJaxbImpl(service.getEvent(eventID));
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.endpoint.SchedulerWebService#getEvents(org.opencastproject.scheduler.api.SchedulerFilter)
   */
  @WebMethod()
  @WebResult(name="array")
  public SchedulerEventJaxbImpl [] getEvents (@WebParam(name="scheduler-filter")SchedulerFilterJaxbImpl filter) {
    SchedulerEvent [] events = service.getEvents(filter.getFilter());
    SchedulerEventJaxbImpl [] jaxbEvents = new SchedulerEventJaxbImpl [events.length];
    for (int i = 0; i < events.length; i++) jaxbEvents [i] = new SchedulerEventJaxbImpl(events[i]);
    return jaxbEvents;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.endpoint.SchedulerWebService#getEvents(org.opencastproject.scheduler.api.SchedulerFilter)
   */
  @WebMethod()
  @WebResult(name="array")
  public SchedulerEventJaxbImpl [] getUpcommingEvents () {
    SchedulerEvent [] events = service.getUpcommingEvents();
    SchedulerEventJaxbImpl [] jaxbEvents = new SchedulerEventJaxbImpl [events.length];
    for (int i = 0; i < events.length; i++) jaxbEvents [i] = new SchedulerEventJaxbImpl(events[i]);
    return jaxbEvents;
  }  

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.endpoint.SchedulerWebService#removeEvent(java.lang.String)
   */
  @WebMethod()
  @WebResult(name="boolean")
  public boolean removeEvent (@WebParam(name="eventID") String eventID) {
    return service.removeEvent(eventID);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.endpoint.SchedulerWebService#updateEvent(org.opencastproject.scheduler.api.SchedulerEvent)
   */
  @WebMethod()
  @WebResult(name="boolean")
  public boolean updateEvent (@WebParam(name="scheduler-event")SchedulerEventJaxbImpl e) {
    return service.updateEvent(e.getEvent());
  }
  
}
