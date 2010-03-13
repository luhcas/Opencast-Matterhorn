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



import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

/**
 * Interface for SOAP Webservice, currently not in use
 */
@WebService()
public interface SchedulerWebService {
  
  @WebMethod()
  @WebResult(name="scheduler-event")
  public SchedulerEventJaxbImpl addEvent (@WebParam(name="scheduler-event") SchedulerEventJaxbImpl e);
  
  @WebMethod()
  @WebResult(name="boolean")
  public boolean removeEvent (@WebParam(name="eventID") String eventID);
  
  @WebMethod()
  @WebResult(name="boolean")
  public boolean updateEvent (@WebParam(name="scheduler-event")SchedulerEventJaxbImpl e);
  
  @WebMethod()
  @WebResult(name="scheduler-event")
  public SchedulerEventJaxbImpl getEvent (@WebParam(name="eventID")String eventID);
  
  @WebMethod()
  @WebResult(name="array")
  public SchedulerEventJaxbImpl [] getEvents (@WebParam(name="scheduler-filter")SchedulerFilterJaxbImpl filter);
  
  @WebMethod()
  @WebResult(name="url")
  public String getCalendarForCaptureAgent (@WebParam(name="captureAgentID")String captureAgentID);
  
  @WebMethod()
  @WebResult(name="array")
  public SchedulerEventJaxbImpl [] getUpcomingEvents ();  
}
