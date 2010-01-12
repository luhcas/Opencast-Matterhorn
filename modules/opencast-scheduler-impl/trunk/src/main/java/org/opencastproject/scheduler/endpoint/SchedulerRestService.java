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
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;
import org.opencastproject.util.doc.Param.Type;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;

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
  
  /**
   * Method to set the service this REST endpoint uses
   * @param service
   */
  public void setService(SchedulerService service) {
    this.service = service;
  }
  
  /**
   * Method to unset the service this REST endpoint uses
   * @param service
   */
  public void unsetService(SchedulerService service) {
    this.service = null;
  }
  
  /**
   * The method tha will be called, if the service will be activated
   * @param cc The ComponentContext of this service
   */
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

    docs = generateDocs();
  }  
  
  /**
   * Get a specific scheduled event.
   * @param eventID The unique ID of the event.
   * @return SchedulerEvent XML with the data of the event
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("getEvent/{eventID}")
  public Response getEvent(@QueryParam("eventID") String eventID) {
    SchedulerEvent event = service.getEvent(eventID);
    if (event == null) return Response.status(Status.BAD_REQUEST).build();
    return Response.ok(new SchedulerEventJaxbImpl(event)).build();
  }
  
  /**
   * Gets a XML with the Dublin Core metadata for the specified event. 
   * @param eventID The unique ID of the event. 
   * @return Dublin Core XML for the event
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("getDublinCoreMetadata/{eventID}")
  public Response getDublinCoreMetadata(@QueryParam("eventID") String eventID) {
    String result = service.getDublinCoreMetadata(eventID);
    if (result == null) return Response.status(Status.BAD_REQUEST).build();
    return Response.ok(result).build();
  }  

  /**
   * Gets java Properties file with technical metadata for the specified event. 
   * @param eventID The unique ID of the event.
   * @return Java Properties File with the metadata for the event 
   */
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("getCaptureAgentMetadata/{eventID}")
  public Response getCaptureAgentMetadata(@QueryParam("eventID") String eventID) {
    String result = service.getCaptureAgentMetadata(eventID);
    if (result == null) return Response.status(Status.BAD_REQUEST).build();
    return Response.ok(result).build();
  }    
  
  /**
   * Stores a new event in the database. As a result the event will be returned, but some fields especially the event-id may have been updated. Within the metadata section it is possible to add any additional metadata as a key-value pair. The information will be stored even if the key is yet unknown.
   * @param e The SchedulerEvent that should be stored.
   * @return The same event with some updated fields.
   */
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
  
  /**
   * 
   * Removes the specified event from the database. Returns true if the event was found and could be removed.
   * @param eventID The unique ID of the event.
   * @return true if the event was found and could be deleted.
   */
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("removeEvent/{eventID}")
  public Response removeEvent (@QueryParam("eventID") String eventID) {
    return Response.ok(service.removeEvent(eventID)).build();
  }
  
  /**
   * Updates an existing event in the database. The event-id has to be stored in the database already. Will return true, if the event was found and could be updated.
   * @param e The SchedulerEvent that should be updated 
   * @return true if the event was found and could be updated.
   */
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
  
  /**
   * returns scheduled events, that pass the filter. filter: an xml definition of the filter. Tags that are not included will noct be filtered. Possible values for order by are title,creator,series,time-asc,time-desc,contributor,channel,location,device
   * @param filter exact id to search for pattern to search for pattern to search for A short description of the content of the lecture begin of the period of valid events end of the period of valid events pattern to search for ID of the series which will be filtered ID of the channel that will be filtered pattern to search for pattern to search for pattern to search for title|creator|series|time-asc|time-desc|contributor|channel|location|device">
   * @return List of SchedulerEvents as XML 
   */
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
   * Looks for events that are conflicting with the given event, because they use the same recorder at the same time.
   * @param e The event that should be checked for conflicts
   * @return An XML with the list of conflicting events
   */
  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("findConflictingEvents")
  public SchedulerEventJaxbImpl [] findConflictingEvents (@FormParam("event") SchedulerEventJaxbImpl e) {
    if (e == null) {
      logger.error("event is null");
      return new SchedulerEventJaxbImpl [0];
    }
    SchedulerEvent [] events = null; // FIXME -- compilation error below
//    SchedulerEvent [] events = service.findConflictingEvents(e.getEvent());
    if (events == null) return new SchedulerEventJaxbImpl [0];
    SchedulerEventJaxbImpl [] jaxbEvents = new SchedulerEventJaxbImpl [events.length];
    for (int i = 0; i < events.length; i++) jaxbEvents [i] = new SchedulerEventJaxbImpl(events[i]);
    return jaxbEvents;
  }  
  
  /**
   * Lists all events in the database, without any filter
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
  
  
  /**
   * Gets the iCalendar with all (even old) events for the specified capture agent id. 
   * @param captureAgentID The ID that specifies the capture agent.
   * @return an iCalendar
   */
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("getCalendarForCaptureAgent/{captureAgentID}")
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
   * Generates the REST documentation
   * @return The HTML with the documentation
   */
  protected String generateDocs() {
    DocRestData data = new DocRestData("Scheduler", "Scheduler Service", "/scheduler/rest", new String[] {"$Rev: 1505 $"});
    // Scheduler addEvent
    RestEndpoint addEventEndpoint = new RestEndpoint("addEvent", RestEndpoint.Method.POST, "/addEvent", "Stores a new event in the database. As a result the event will be returned, but some fields especially the event-id may have been updated.\nWithin the metadata section it is possible to add any additional metadata as a key-value pair. The information will be stored even if the key is yet unknown.");
    addEventEndpoint.addFormat(new Format("xml", null, null));
    addEventEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, result returned"));
    addEventEndpoint.addRequiredParam(new Param("event", Type.TEXT, generateSchedulerEvent(), "The SchedulerEvent that should be stored."));
    addEventEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, addEventEndpoint);
    
    // Scheduler updateEvent
    RestEndpoint updateEventEndpoint = new RestEndpoint("updateEvent", RestEndpoint.Method.POST, "/updateEvent", "Updates an existing event in the database. The event-id has to be stored in the database already. Will return true, if the event was found and could be updated.");
    updateEventEndpoint.addFormat(new Format("boolean", null, null));
    updateEventEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, boolean returned"));
    updateEventEndpoint.addRequiredParam(new Param("event", Type.TEXT, generateSchedulerEvent(), "The SchedulerEvent that should be updated."));
    updateEventEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, updateEventEndpoint);    
    
    // Scheduler removeEvent
    RestEndpoint removeEventEndpoint = new RestEndpoint("removeEvent", RestEndpoint.Method.GET, "/removeEvent/{eventID}", "Removes the specified event from the database. Returns true if the event was found and could be removed.");
    removeEventEndpoint.addFormat(new Format("boolean", null, null));
    removeEventEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, boolean returned"));
    removeEventEndpoint.addPathParam(new Param("eventID", Type.STRING, "c0e3d8a7-7ecc-479b-aee7-8da369e445f2", "The unique ID of the event."));
    removeEventEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, removeEventEndpoint);  
    
    // Scheduler getEvent
    RestEndpoint getEventEndpoint = new RestEndpoint("getEvent", RestEndpoint.Method.GET, "/getEvent/{eventID}", "Get a specific scheduled event.");
    getEventEndpoint.addFormat(new Format("xml", null, null));
    getEventEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, SchedulerEvent XML returned"));
    getEventEndpoint.addPathParam(new Param("eventID", Type.STRING, "c0e3d8a7-7ecc-479b-aee7-8da369e445f2", "The unique ID of the event."));
    getEventEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getEventEndpoint);
    
    // Scheduler getEvents
    RestEndpoint getEventsEndpoint = new RestEndpoint("getEvents", RestEndpoint.Method.POST, "/getEvents", "returns scheduled events, that pass the filter.\nfilter: an xml definition of the filter. Tags that are not included will noct be filtered. Possible values for order by are title,creator,series,time-asc,time-desc,contributor,channel,location,device");
    getEventsEndpoint.addFormat(new Format("xml", null, null));
    getEventsEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, List of SchedulerEvents as XML returned"));
    getEventsEndpoint.addOptionalParam(new Param("filter", Type.TEXT, generateSchedulerFilter(), "The SchedulerFilter that should be applied."));
    getEventsEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getEventsEndpoint);

    // Scheduler getUpcomingEvents 
    RestEndpoint getUpcomingEventsEndpoint = new RestEndpoint("getUpcomingEvents", RestEndpoint.Method.GET, "/getUpcomingEvents", "Removes the specified event from the database. Returns true if the event was found and could be removed.");
    getUpcomingEventsEndpoint.addFormat(new Format("xml", null, null));
    getUpcomingEventsEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, List of SchedulerEvents as XML returned"));
    getUpcomingEventsEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getUpcomingEventsEndpoint);
    
    // Scheduler findConflictingEvents 
    RestEndpoint findConflictingEventsEndpoint = new RestEndpoint("findConflictingEvents", RestEndpoint.Method.POST, "/findConflictingEvents", "Looks for events that are conflicting with the given event, because they use the same recorder at the same time ");
    findConflictingEventsEndpoint.addFormat(new Format("xml", null, null));
    findConflictingEventsEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, List of SchedulerEvents as XML returned"));
    findConflictingEventsEndpoint.addRequiredParam(new Param("event", Type.TEXT, generateSchedulerEvent(), "The SchedulerEvent that should be checked for conflicts."));
    findConflictingEventsEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, findConflictingEventsEndpoint);  

    // Scheduler getDublinCoreMetadata
    RestEndpoint getDublinCoreMetadataEndpoint = new RestEndpoint("getDublinCoreMetadata", RestEndpoint.Method.GET, "/getDublinCoreMetadata/{eventID}", "Gets a XML with the Dublin Core metadata for the specified event. ");
    getDublinCoreMetadataEndpoint.addFormat(new Format("xml", null, null));
    getDublinCoreMetadataEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, Dublin Core XML returned"));
    getDublinCoreMetadataEndpoint.addPathParam(new Param("eventID", Type.STRING, "c0e3d8a7-7ecc-479b-aee7-8da369e445f2", "The unique ID of the event."));
    getDublinCoreMetadataEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getDublinCoreMetadataEndpoint);    
    
    // Scheduler getCaptureAgentMetadata
    RestEndpoint getCaptureAgentMetadataEndpoint = new RestEndpoint("getCaptureAgentMetadata", RestEndpoint.Method.GET, "/getCaptureAgentMetadata/{eventID}", "Gets java Properties file with technical metadata for the specified event. ");
    getCaptureAgentMetadataEndpoint.addFormat(new Format("properties", "Java Properties", null));
    getCaptureAgentMetadataEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, Java Properties File returned"));
    getCaptureAgentMetadataEndpoint.addPathParam(new Param("eventID", Type.STRING, "c0e3d8a7-7ecc-479b-aee7-8da369e445f2", "The unique ID of the event."));
    getCaptureAgentMetadataEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getCaptureAgentMetadataEndpoint);  
    
    // Scheduler getCalendarForCaptureAgent
    RestEndpoint getCalendarForCaptureAgentEndpoint = new RestEndpoint("getCalendarForCaptureAgent", RestEndpoint.Method.GET, "/getCalendarForCaptureAgent/{captureAgentID}", "Gets the iCalendar with all (even old) events for the specified capture agent id. ");
    getCalendarForCaptureAgentEndpoint.addFormat(new Format("ics", "iCalendar", "http://tools.ietf.org/html/rfc2445"));
    getCalendarForCaptureAgentEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, iCalendar returned"));
    getCalendarForCaptureAgentEndpoint.addPathParam(new Param("captureAgentID", Type.STRING, "recorder", "The ID that specifies the capture agent."));
    getCalendarForCaptureAgentEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getCalendarForCaptureAgentEndpoint);    
    
    return DocUtil.generate(data);
  }

  /**
   * Creates an example XML of an SchedulerEvent for the documentation.
   * @return A XML with a SchedulerEvent
   */ 
  private String generateSchedulerEvent() {
    return "<ns1:SchedulerEvent xmlns:ns1=\"http://scheduler.opencastproject.org\">\n"+
           "<id>c0e3d8a7-7ecc-479b-aee7-8da369e445f2</id>\n"+
           "<metadata>\n"+
           "  <item key=\"channel-id\"><value>1</value></item>\n"+
           "  <item key=\"creator\"><value>lecturer</value></item>\n"+
           "  <item key=\"device\"><value>recorder</value></item>\n"+
           "  <item key=\"title\"><value>Test title</value></item>\n"+
           "  <item key=\"abstract\"><value>a description of the event</value></item>\n"+
           "  <item key=\"contributor\"><value>The contribution institution</value></item>\n"+
           "  <item key=\"series-id\"><value>c0e3d8a7-7ecc-479b-1234-8da369e445f2</value></item>\n"+
           "  <item key=\"location\"><value>my room</value></item>\n"+
           "</metadata>\n"+
           "<startdate>1262631892201</startdate>\n"+
           "<enddate>1262644114423</enddate>\n"+
           "<duration>12222222</duration>\n"+
           "<attendees>\n"+
           "  <attendee>recorder</attendee>\n"+
           "  <attendee>another attendee</attendee>\n"+
           "</attendees>\n"+
           "<resources>\n"+
           "  <resource>vga</resource>\n"+
           "  <resource>audio</resource>\n"+
           "</resources>\n"+
           "</ns1:SchedulerEvent>";
  }  
  
  
  /**
   * Creates an example XML of an SchedulerFilter for the documentation.
   * @return A XML with a SchedulerFilter
   */
  private String generateSchedulerFilter() {
     return "<ns2:SchedulerFilter xmlns:ns2=\"http://scheduler.opencastproject.org/\">\n"+
            " <event-id>exact id to search for</event-id>\n"+
            " <device>pattern to search for</device>\n"+
            " <title>pattern to search for</title>\n"+
            " <creator>pattern to search for</creator>\n"+
            " <abstract>A short description of the content of the lecture</abstract>\n" +
            " <startdate>begin of the period of valid events</startdate>\n" +
            " <enddate>end of the period of valid events</enddate>\n" +
            " <contributor>pattern to search for</contributor>\n" +
            " <series-id>ID of the series which will be filtered</series-id>\n" +
            " <channel-id>ID of the channel that will be filtered</channel-id>\n" +
            " <location>pattern to search for</location>\n" +
            " <attendee>pattern to search for</attendee>\n" +
            " <resource>pattern to search for</resource>\n" +
            " <order-by>title|creator|series|time-asc|time-desc|contributor|channel|location|device</order-by>\n" +
            "</ns2:SchedulerFilter>";
  }
  
}
