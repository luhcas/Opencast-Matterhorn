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
import org.opencastproject.util.UrlSupport;
import org.osgi.service.component.ComponentContext;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Date;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

/**
 * REST Endpoint for Scheduler Service
 */
@Path("/")
public class SchedulerRestService {
  private static final Logger logger = LoggerFactory.getLogger(SchedulerRestService.class);
  private SchedulerService service;
  
  protected String docs = null;
  protected String serverUrl = UrlSupport.DEFAULT_BASE_URL;
  
  public void setService(SchedulerService service) {
    this.service = service;
  }

  public void unsetService(SchedulerService service) {
    this.service = null;
  }
  
  public void activate(ComponentContext cc) {
    // Get the configured server URL
    if(cc == null) {
      serverUrl = UrlSupport.DEFAULT_BASE_URL;
    } else {
      String ccServerUrl = cc.getBundleContext().getProperty("serverUrl");
      logger.info("configured server url is {}", ccServerUrl);
      if(ccServerUrl == null) {
        serverUrl = UrlSupport.DEFAULT_BASE_URL;
      } else {
        serverUrl = ccServerUrl;
      }
    }

    // Pre-load the documentation
    String docsFromClassloader = null;
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream("/html/index.html");
      docsFromClassloader = IOUtils.toString(in);
      docsFromClassloader = docsFromClassloader.replaceAll("@SERVER_URL@", serverUrl + "/scheduler/rest");
    } catch (IOException e) {
      logger.error("failed to read documentation", e);
      docsFromClassloader = "unable to load documentation for " + SchedulerRestService.class.getName();
    } finally {
      IOUtils.closeQuietly(in);
    }
    docs = docsFromClassloader;

  }  
  
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("getEvent")
  public Response getEvent(@QueryParam("eventID") String eventID) {
    SchedulerEvent event = service.getEvent(eventID);
    if (event == null) return Response.status(Status.BAD_REQUEST).build();
    return Response.ok(new SchedulerEventJaxbImpl(event)).build();
  }
  
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("getDublinCoreMetadata")
  public Response getDublinCoreMetadata(@QueryParam("eventID") String eventID) {
    String result = service.getDublinCoreMetadata(eventID);
    if (result == null) return Response.status(Status.BAD_REQUEST).build();
    return Response.ok(result).build();
  }  

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("getCaptureAgentMetadata")
  public Response getCaptureAgentMetadata(@QueryParam("eventID") String eventID) {
    String result = service.getCaptureAgentMetadata(eventID);
    if (result == null) return Response.status(Status.BAD_REQUEST).build();
    return Response.ok(result).build();
  }    
  
  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("addEvent")
  public Response addEvent (@FormParam("event") SchedulerEventJaxbImpl e) {
    if (e == null) {
      logger.error("Event that should be added is null");
      return Response.status(Status.BAD_REQUEST).build();
    }
    SchedulerEvent i = e.getEvent();
    if(i == null){
      logger.info("Event was null.");
    }
    SchedulerEvent j = service.addEvent(i);
    logger.info("Adding event "+j.getID()+" to scheduler");
    return Response.ok(new SchedulerEventJaxbImpl(j)).build();
  }
  
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("removeEvent")
  public Response removeEvent (@QueryParam("eventID") String eventID) {
    return Response.ok(service.removeEvent(eventID)).build();
  }
  
  @POST
  @Produces(MediaType.TEXT_PLAIN)
  @Path("updateEvent")
  public Response updateEvent (@FormParam("event") SchedulerEventJaxbImpl e) {
    if(service.updateEvent(e.getEvent())) {
      return Response.ok(true).build();
    } else {
      return Response.serverError().build();
    }
  }
  
  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("getEvents")
  public SchedulerEventJaxbImpl [] getEvents (@FormParam("filter") String filter) {
    if (filter == null) {
      logger.error("Filter is null");
      return new SchedulerEventJaxbImpl [0];
    }
    logger.info("Filter: "+ filter);
    SchedulerFilterJaxbImpl filterJaxB = null;
    try {
      JAXBContext jaxbContext = JAXBContext.newInstance(SchedulerFilterJaxbImpl.class);
      logger.info("context created");
      Unmarshaller m = jaxbContext.createUnmarshaller();
      logger.info("unmarshaler ready");
      filterJaxB =  (SchedulerFilterJaxbImpl) m.unmarshal(new StringReader(filter));      
      logger.info("unmarshaler read");      
    } catch (JAXBException e) {
      logger.error(e.getMessage());
      e.printStackTrace();
    }      
    if (filterJaxB == null) {
      logger.error("FilterJaxB is null");
      return new SchedulerEventJaxbImpl [0];
    }
    logger.info("Filter events with "+filterJaxB.getFilter());
    SchedulerEvent [] events = service.getEvents(filterJaxB.getFilter());
    if (events == null) return new SchedulerEventJaxbImpl [0];
    SchedulerEventJaxbImpl [] jaxbEvents = new SchedulerEventJaxbImpl [events.length];
    for (int i = 0; i < events.length; i++) jaxbEvents [i] = new SchedulerEventJaxbImpl(events[i]);
    return jaxbEvents;
  }
  
  /**
   * Lists all events in the database, without any filter
   * TODO only a stub for debugging, because there is no UI to show these at the moment
   * @return XML with all events 
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("getEvents")
  public SchedulerEventJaxbImpl [] getEvents () {
    SchedulerEvent [] events = service.getEvents(null);
    SchedulerEventJaxbImpl [] jaxbEvents = new SchedulerEventJaxbImpl [events.length];
    for (int i = 0; i < events.length; i++) { 
      jaxbEvents [i] = new SchedulerEventJaxbImpl(events[i]);
      logger.info("JaxB version of event "+events[i]+ "created");
    }
    return jaxbEvents;
  }  
  
  /**
   * Lists all future events in the database, without any filter
   * @return XML with all events 
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("getUpcomingEvents")
  public SchedulerEventJaxbImpl [] getUpcomingEvents () {
    SchedulerEvent [] events = service.getUpcomingEvents();
    SchedulerEventJaxbImpl [] jaxbEvents = new SchedulerEventJaxbImpl [events.length];
    for (int i = 0; i < events.length; i++) jaxbEvents [i] = new SchedulerEventJaxbImpl(events[i]);
    return jaxbEvents;
  }   
  
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("getCalendarForCaptureAgent")
  public Response getCalendarForCaptureAgent (@QueryParam("captureAgentID") String captureAgentID) {
    String result = service.getCalendarForCaptureAgent(captureAgentID);
    if (result == null) return Response.status(Status.BAD_REQUEST).build();
    return Response.ok(result).build();
  }
  
  /**
   * returns the REST documentation
   * @return the REST documentation, if available
   */
  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    if (docs == null) return "No documetation available.";
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
    long time1 = System.currentTimeMillis()+11111111;
    long time2 = time1 + 11111111 + 1111111;
    Date date1 = new Date(time1);
    Date date2 = new Date(time2);
    logger.debug("Info times "+time1+" = "+date1.getTime()+", "+time2+" = "+ date2.getTime());
    event.setStartdate(date1);
    event.setEnddate(date2);
    SchedulerEvent event2 = service.addEvent(event);
    String answer = "demo-data inserted, ID="+event2.getID();
    return answer;
  }
  
  
/*    @GET
  @Produces(MediaType.TEXT_XML)
  @Path("getFilter")
 public Response getFilter () {
   SchedulerFilter filter = new SchedulerFilterImpl();
   filter.setStart(new Date(System.currentTimeMillis()));
   filter.setEnd(new Date(System.currentTimeMillis()+2222222));
   filter.setOrderBy("time-desc");
   filter.setAbstractFilter("text");
   filter.setAttendeeFilter("attendee");
   filter.setTitleFilter("title");
   filter.setChannelIDFilter("1");
   filter.setContributorFilter("contributor");
   filter.setCreatorFilter("creator");
   filter.setDeviceFilter("rekorder");
   filter.setEventIDFilter("event-id");
   filter.setLocationFilter("room");
   filter.setResourceFilter("resource");
   filter.setSeriesIDFilter("series");
   SchedulerFilterJaxbImpl filterxml = new SchedulerFilterJaxbImpl(filter);
   logger.info("Filter ready");
   JAXBContext jaxbContext;
   String result = "";
  try {
    jaxbContext = JAXBContext.newInstance(SchedulerFilterJaxbImpl.class);
    logger.info("context created");
    Marshaller m = jaxbContext.createMarshaller();
    logger.info("marshaler ready");
    StringWriter write = new StringWriter();
    m.marshal(filterxml, write);
    logger.info("MArshaler written");
    result = write.toString();
  } catch (JAXBException e) {
    logger.error(e.getMessage());
  }  
   return Response.ok(result).build();
 }
*/  
}
