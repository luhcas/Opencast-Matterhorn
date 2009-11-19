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
import org.opencastproject.scheduler.impl.SchedulerEventImpl;
import org.opencastproject.scheduler.impl.SchedulerServiceImpl;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * FIXME -- Add javadocs
 */
@Path("/")
public class SchedulerRestService {
  private static final Logger logger = LoggerFactory.getLogger(SchedulerRestService.class);
  private SchedulerService service;
  public void setService(SchedulerService service) {
    this.service = service;
  }

  public void unsetService(SchedulerService service) {
    this.service = null;
  }
  
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("getEvent")
  public SchedulerEventJaxbImpl getEvent(@QueryParam("eventID") String eventID) {
    SchedulerEvent event = service.getEvent(eventID);
    return new SchedulerEventJaxbImpl(event);
  }
  
  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("addEvent")
  public SchedulerEventJaxbImpl addEvent (@FormParam("event") SchedulerEventJaxbImpl e) {
    SchedulerEvent i = e.getEvent();
    SchedulerEvent j = service.addEvent(i);
    return new SchedulerEventJaxbImpl(j);
  }
  
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("removeEvent")
  public Response removeEvent (@QueryParam("eventID") String eventID) {
    return Response.ok(service.removeEvent(eventID)).build();
  }
  
  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("updateEvent")
  public Response updateEvent (@FormParam("event") SchedulerEventJaxbImpl e) {
    service.updateEvent(e.getEvent());
    return Response.ok(service.updateEvent(e.getEvent())).build();
  }
  
  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("getEvents")
  public SchedulerEventJaxbImpl [] getEvents (@FormParam("filter") SchedulerFilterJaxbImpl filter) {
    SchedulerEvent [] events = service.getEvents(filter.getFilter());
    SchedulerEventJaxbImpl [] jaxbEvents = new SchedulerEventJaxbImpl [events.length];
    for (int i = 0; i < events.length; i++) jaxbEvents [i] = new SchedulerEventJaxbImpl(events[i]);
    return jaxbEvents;
  }
  
  /**
   * Lists all events in the database, without any filter
   * TODO only a stub for debugging, because there is no UI to schow these at the moment
   * @return XML with all events 
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("getEvents")
  public SchedulerEventJaxbImpl [] getEvents () {
    SchedulerEvent [] events = service.getEvents(null);
    SchedulerEventJaxbImpl [] jaxbEvents = new SchedulerEventJaxbImpl [events.length];
    for (int i = 0; i < events.length; i++) jaxbEvents [i] = new SchedulerEventJaxbImpl(events[i]);
    return jaxbEvents;
  }  
  
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("getCalendarForCaptureAgent")
  public String getCalendarForCaptureAgent (@QueryParam("captureAgentID") String captureAgentID) {
    return service.getCalendarForCaptureAgent(captureAgentID);
  }
  
  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    return docs;
  }
  
  
  /**
   * TODO only for debugging. Checks if tables are present and creates a new event starting now.
   * @return Status information 
   */
  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("info")
  public String info() {
    logger.debug("getting status info for scheduling service");
    SchedulerEventImpl event = new SchedulerEventImpl();
    event.setTitle("test-info");
    event.setDevice("rec19");
    event.addAttendee("rec19");
    event.addAttendee("Ruediger Rolf");
    event.addResource("vga");
    event.addResource("audio");
    event.setChannelID("1");
    event.setCreator("Adam Hochman");
    long time1 = System.currentTimeMillis();
    long time2 = time1+1111111;
    Date date1 = new Date(time1);
    Date date2 = new Date(time2);
    logger.debug("Info times "+time1+" = "+date1.getTime()+", "+time2+" = "+ date2.getTime());
    event.setStartdate(date1);
    event.setEnddate(date2);
    SchedulerEvent event2 = service.addEvent(event);
    String answer = "Database available: "+ ((SchedulerServiceImpl) service).dbCheck() +", "+
    "demo-data inserted, ID="+event2.getID();
    return answer;
  }

  protected final String docs;
  
  public SchedulerRestService() {
    String docsFromClassloader = null;
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream("/html/index.html");
      docsFromClassloader = IOUtils.toString(in);
    } catch (IOException e) {
      logger.error("failed to read documentation", e);
      docsFromClassloader = "unable to load documentation for " + SchedulerRestService.class.getName();
    } finally {
      IOUtils.closeQuietly(in);
    }
    docs = docsFromClassloader;
  }
}
