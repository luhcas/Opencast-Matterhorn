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

import org.opencastproject.scheduler.api.Event;
import org.opencastproject.scheduler.api.Metadata;
import org.opencastproject.scheduler.api.RecurringEvent;
import org.opencastproject.scheduler.api.SchedulerFilter;
import org.opencastproject.scheduler.impl.EventImpl;
import org.opencastproject.scheduler.impl.IncompleteDataException;
import org.opencastproject.scheduler.impl.MetadataImpl;
import org.opencastproject.scheduler.impl.RecurringEventImpl;
import org.opencastproject.scheduler.impl.SchedulerFilterImpl;
import org.opencastproject.scheduler.impl.SchedulerServiceImpl;
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

import org.json.simple.JSONObject;

import java.io.StringReader;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
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
  private SchedulerServiceImpl service;
  
  protected String serverUrl = UrlSupport.DEFAULT_BASE_URL;
  
  /**
   * Method to set the service this REST endpoint uses
   * @param service
   */
  public void setService(SchedulerServiceImpl service) {
    this.service = service;
  }
  
  /**
   * Method to unset the service this REST endpoint uses
   * @param service
   */
  public void unsetService(SchedulerServiceImpl service) {
    this.service = null;
  }
  
  /**
   * The method that will be called, if the service will be activated
   * @param cc The ComponentContext of this service
   */
  public void activate(ComponentContext cc) {
    // Get the configured server URL
    if(cc == null) {
      serverUrl = UrlSupport.DEFAULT_BASE_URL;
    } else {
      String ccServerUrl = cc.getBundleContext().getProperty("org.opencastproject.server.url");
      logger.info("configured server url is {}", ccServerUrl);
      if(ccServerUrl == null) {
        serverUrl = UrlSupport.DEFAULT_BASE_URL;
      } else {
        serverUrl = ccServerUrl;
      }
    }
  }  
  
  /**
   * Get a specific scheduled event.
   * @param eventID The unique ID of the event.
   * @return event XML with the data of the event
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("event/{eventID}")
  public Response getSingleEvent(@PathParam("eventID") String eventID) {
    logger.debug("Single event Lookup: {}", eventID);
    try {
      Event event = service.getEvent(eventID);
      if (event == null) return Response.status(Status.BAD_REQUEST).build();
      return Response.ok(event).build();
    } catch (Exception e) {
      logger.warn("Single event Lookup failed: {}", eventID);
      return Response.status(Status.SERVICE_UNAVAILABLE).build();
    }
  }
  
  /**
   * add a recurring event.
   * @param event The recurring event to be added
   * @return event XML with the data of the stored recurring event
   */
  @PUT
  @Produces(MediaType.TEXT_XML)
  @Path("recurrence")
  public Response addRecurringEvent(@FormParam("recurringEvent") RecurringEventImpl event) {
    logger.debug("add Recurrent event: {}", event);
    try {
      if (event == null) return Response.status(Status.BAD_REQUEST).build();
      RecurringEvent result = service.addRecurringEvent(event);
      if (result == null) return Response.status(Status.BAD_REQUEST).build();
      return Response.ok(result).build();
    } catch (Exception e) {
      logger.warn("could not add recurring event: {}", event);
      return Response.status(Status.SERVICE_UNAVAILABLE).build();
    }
  }  
  
  /**
   * update a recurring event.
   * @param event The recurring event to be added
   * @return event XML with the data of the stored recurring event
   */
  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("recurrence")
  public Response updateRecurringEvent(@FormParam("recurringEvent") RecurringEventImpl event) {
    logger.debug("update Recurrent event: {}", event);
    try {
      if (event == null) return Response.status(Status.BAD_REQUEST).build();
      return Response.ok(service.updateRecurringEvent(event)).build();
    } catch (Exception e) {
      logger.warn("could not update recurring event {}: ", event, e.getMessage());
      return Response.status(Status.SERVICE_UNAVAILABLE).build();
    }
  }  
  
  /**
   * Get a specific scheduled recurring event.
   * @param eventID The unique ID of the event.
   * @return event XML with the data of the recurring event
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("recurrence/{recurringEventID}")
  public Response getRecurringEvent(@PathParam("recurringEventID") String eventID) {
    logger.debug("Recurrent event Lookup: {}", eventID);
    try {
      RecurringEvent event = service.getRecurringEvent(eventID);
      if (event == null) return Response.status(Status.BAD_REQUEST).build();
      return Response.ok(event).build();
    } catch (Exception e) {
      logger.warn("Recurrent event Lookup failed: {}", eventID);
      return Response.status(Status.SERVICE_UNAVAILABLE).build();
    }
  } 
  
  /**
   * Get list of events that belong to the recurring event.
   * @param eventID The unique ID of the RecurringEvent.
   * @return List of events XML with the data of the events that belong to the recurring event 
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("recurrence/{recurringEventID}/events")
  public Response getEventsFromRecurringEvent(@PathParam("recurringEventID") String eventID) {
    logger.debug("Getting events from recurrent event: {}", eventID);
    try {
      RecurringEvent event = service.getRecurringEvent(eventID);
      if (event == null) return Response.status(Status.BAD_REQUEST).build();
      return Response.ok((new GenericEntity<List<Event>> (event.getEvents()){})).build();
    } catch (Exception e) {
      logger.warn("Getting events from recurrent event failed: {}", eventID);
      return Response.status(Status.SERVICE_UNAVAILABLE).build();
    }
  } 
  
  /**
   * Delete a recurring event
   * @param eventID The unique ID of the RecurringEvent.
   * @return List of events XML with the data of the events that belong to the recurring event 
   */
  @DELETE
  @Produces(MediaType.TEXT_XML)
  @Path("recurrence/{recurringEventID}")
  public Response deleteRecurringEvent(@PathParam("recurringEventID") String eventID) {
    logger.debug("delete recurring event: {}", eventID);
    try {
      if (eventID == null) return Response.status(Status.BAD_REQUEST).build();
      return Response.ok(service.removeRecurringEvent(eventID)).build();
    } catch (Exception e) {
      logger.warn("removing events from recurrent event failed: {}", eventID);
      return Response.status(Status.SERVICE_UNAVAILABLE).build();
    }
  }  
  
  /**
   * Gets a XML with the Dublin Core metadata for the specified event. 
   * @param eventID The unique ID of the event. 
   * @return Dublin Core XML for the event
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("getDublinCoreMetadata/{eventID}")
  public Response getDublinCoreMetadata(@PathParam("eventID") String eventID) {
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
  public Response getCaptureAgentMetadata(@PathParam("eventID") String eventID) {
    String result = service.getCaptureAgentMetadata(eventID);
    if (result == null) return Response.status(Status.BAD_REQUEST).build();
    return Response.ok(result).build();
  }    
  

  /**
   * Stores a new event in the database. As a result the event will be returned, but some fields especially the event-id may have been updated. Within the metadata section it is possible to add any additional metadata as a key-value pair. The information will be stored even if the key is yet unknown.
   * @param e The SchedulerEvent that should be stored.
   * @return The same event with some updated fields.
   */
  @PUT
  @Produces(MediaType.TEXT_XML)
  @Path("event")
  public Response addEvent (@FormParam("event") EventImpl e) {
    logger.debug("addEvent(e): {}", e);
    if (e == null) {
      logger.error("Event that should be added is null");
      return Response.status(Status.BAD_REQUEST).build();
    }
    service.addEvent(e);
    logger.info("Adding event {} to scheduler",e.getEventId());
    return Response.ok(e).build();
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
  public Response removeEvent (@PathParam("eventID") String eventID) {
    return Response.ok(service.removeEvent(eventID)).build();
  }
  
  /**
   * 
   * Removes the specified event from the database. Returns true if the event was found and could be removed.
   * @param eventID The unique ID of the event.
   * @return true if the event was found and could be deleted.
   */
  @DELETE
  @Produces(MediaType.TEXT_PLAIN)
  @Path("event/{eventID}")
  public Response deleteEvent (@PathParam("eventID") String eventID) {
    return Response.ok(service.removeEvent(eventID)).build();
  }  

  
  /**
   * Updates an existing event in the database. The event-id has to be stored in the database already. Will return true, if the event was found and could be updated.
   * @param e The SchedulerEvent that should be updated 
   * @return true if the event was found and could be updated.
   */
  @POST
  @Produces(MediaType.TEXT_PLAIN)
  @Path("event")
  public Response updateEvent (@FormParam("event") EventImpl e) {
    if(service.updateEvent(e)) {
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
  @Path("events")
  public Response getEvents (@FormParam("filter") String filter) {
    if (filter == null) {
      logger.error("Filter is null");
      return Response.status(Status.BAD_REQUEST).build();
    }
    logger.debug("Filter: {} ", filter);
    SchedulerFilterJaxbImpl filterJaxB = null;
    try {
      JAXBContext jaxbContext = JAXBContext.newInstance(SchedulerFilterJaxbImpl.class);
      logger.debug("context created");
      Unmarshaller m = jaxbContext.createUnmarshaller();
      logger.debug("unmarshaler ready");
      filterJaxB =  (SchedulerFilterJaxbImpl) m.unmarshal(new StringReader(filter));      
      logger.debug("unmarshaler read");      
    } catch (JAXBException e) {
      logger.error(e.getMessage());
      e.printStackTrace();
    }      
    if (filterJaxB == null) {
      logger.error("FilterJaxB is null");
      return Response.status(Status.BAD_REQUEST).build();
    }
    logger.info("Filter events with "+filterJaxB.getFilter());
    List<Event> events = service.getEvents(filterJaxB.getFilter());
    if (events == null) return Response.status(Status.BAD_REQUEST).build();
    return Response.ok((new GenericEntity<List<Event>> (events){})).build();
  }
  
  /**
   * Looks for events that are conflicting with the given event, because they use the same recorder at the same time.
   * @param e The event that should be checked for conflicts
   * @return An XML with the list of conflicting events
   */
  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("events/conflict")
  public Response findConflictingEvents (@FormParam("event") EventImpl e) {
    if (e == null) {
      logger.error("event is null");
      return Response.status(Status.BAD_REQUEST).build();
    } 
    List<EventImpl> events = new LinkedList<EventImpl>();
    try {
      for(Event event : service.findConflictingEvents(e)){
        events.add((EventImpl)event);
      }
    } catch (Exception e1) {
      logger.error("Find Conflicting Failed: {}", e1);
      return Response.status(Status.BAD_REQUEST).build();
    }
    return Response.ok((new GenericEntity<List<EventImpl>> (events){})).build();
  }   
  
  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("recurrence/conflict")
  public Response findConflictingEvents (@FormParam("recurringEvent") RecurringEventImpl e) {
    if (e == null) {
      logger.error("event is null");
      return Response.status(Status.BAD_REQUEST).build();
    } 
    List<EventImpl> events = new LinkedList<EventImpl>();
    try {
      for(Event event : service.findConflictingEvents(e)){
        events.add((EventImpl)event);
      }
    } catch (IncompleteDataException e2) {
      logger.warn("Recurring event incomplete {}", e.toString());
      return Response.status(Status.BAD_REQUEST).build();
    } catch (Exception e1) {
      logger.error("Find Conflicting Failed: {}", e1);
      return Response.status(Status.BAD_REQUEST).build();
    }
    
    return Response.ok((new GenericEntity<List<EventImpl>> (events){})).build();
  }

  /**
   * Lists all events in the database, without any filter
   * @return XML with all events 
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("events/all")
  public Response allEvents () {
    List<Event> events = null;
    try {
      events = service.getEvents(null);
    } catch (Exception e) {
      return Response.status(Status.SERVICE_UNAVAILABLE).build();
    }
    if (events == null) return Response.status(Status.BAD_REQUEST).build();
    return Response.ok((new GenericEntity<List<Event>> (events){})).build();
  }     

  /**
   * Lists all Recurring events in the database
   * @return XML with all events 
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("recurrences/all")
  public Response allRecurringEvents () {
    List <RecurringEvent> events = null;
    try {
      events = service.getAllRecurringEvents();
    } catch (Exception e) {
      return Response.status(Status.SERVICE_UNAVAILABLE).build();
    }
    if (events == null) return Response.status(Status.BAD_REQUEST).build();
    return Response.ok((new GenericEntity<List<RecurringEvent>> (events){})).build();
  }
  
  /**
   * Lists all future events in the database, without any filter
   * @return XML with all events 
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("events/upcoming")
  public Response getUpcomingEvents () {
    SchedulerFilter filter = new SchedulerFilterImpl();
    filter.setStart(new Date(System.currentTimeMillis()));
     
    List<Event> events = service.getEvents(filter);
    if (events == null) return Response.status(Status.BAD_REQUEST).build();
    return Response.ok((new GenericEntity<List<Event>> (events){})).build();
  }    
  
  
  /**
   * Gets the iCalendar with all (even old) events for the specified capture agent id. 
   * @param captureAgentID The ID that specifies the capture agent.
   * @return an iCalendar
   */
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("getCalendarForCaptureAgent/{captureAgentID}")
  public Response getCalendarForCaptureAgent (@PathParam("captureAgentID") String captureAgentID) {
    String result = service.getCalendarForCaptureAgent(captureAgentID);
    if (result == null) return Response.status(Status.BAD_REQUEST).build();
    return Response.ok(result).build();
  }
  
  @SuppressWarnings("unchecked")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("uuid")
  public Response getUniqueId() {
    try {
      String id = UUID.randomUUID().toString();
      JSONObject j = new JSONObject();
      j.put("id", id);
      return Response.ok(j.toString()).build();
    } catch (Exception e) {
      logger.warn("could not create new seriesID");
      return Response.status(Status.SERVICE_UNAVAILABLE).build();
    }
  }
  
  /**
   * returns the REST documentation
   * @return the REST documentation, if available
   */
  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    if (docs == null) { docs = generateDocs(); }
    return docs;
  }
  
  protected String docs;
  private String[] notes = {
    "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
    "If the service is down or not working it will return a status 503, this means the the underlying service is not working and is either restarting or has failed",
    "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In other words, there is a bug! You should file an error report with your server logs from the time when the error occurred: <a href=\"https://issues.opencastproject.org\">Opencast Issue Tracker</a>", };
  
  /**
   * Generates the REST documentation
   * @return The HTML with the documentation
   */
  protected String generateDocs() {
    DocRestData data = new DocRestData("Scheduler", "Scheduler Service", "/scheduler/rest", notes);

    // abstract
    data.setAbstract("This service creates, edits and retrieves and helps manage scheduled capture events."); 

    
    // Scheduler addEvent 
    RestEndpoint addEventJPAEndpoint = new RestEndpoint("addEvent", RestEndpoint.Method.PUT, "/event", "Stores a new event in the database. As a result the event will be returned, but some fields especially the event-id may have been updated.\nWithin the metadata section it is possible to add any additional metadata as a key-value pair. The information will be stored even if the key is yet unknown.");
    addEventJPAEndpoint.addFormat(new Format("xml", null, null));
    addEventJPAEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, result returned"));
    addEventJPAEndpoint.addRequiredParam(new Param("event", Type.TEXT, generateEvent(), "The Event that should be stored."));
    addEventJPAEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, addEventJPAEndpoint);   
    
    // Scheduler addEvent 
    RestEndpoint addREventEndpoint = new RestEndpoint("addREvent", RestEndpoint.Method.PUT, "/recurrence", "Stores a new event in the database. As a result the event will be returned, but some fields especially the event-id may have been updated.\nWithin the metadata section it is possible to add any additional metadata as a key-value pair. The information will be stored even if the key is yet unknown.");
    addREventEndpoint.addFormat(new Format("xml", null, null));
    addREventEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, result returned"));
    addREventEndpoint.addRequiredParam(new Param("recurringEvent", Type.TEXT, generateRecurringEvent(), "The Recurring Event that should be stored."));
    addREventEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, addREventEndpoint);    
 
    // Scheduler updateEvent 
    RestEndpoint updateEventEndpoint = new RestEndpoint("updateEvent", RestEndpoint.Method.POST, "/event", "Updates an existing event in the database. The event-id has to be stored in the database already. Will return true, if the event was found and could be updated.");
    updateEventEndpoint.addFormat(new Format("boolean", null, null));
    updateEventEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, boolean returned"));
    updateEventEndpoint.addRequiredParam(new Param("event", Type.TEXT, generateEvent(), "The Event that should be updated."));
    updateEventEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, updateEventEndpoint);       
    
    // Scheduler updateRecurringEvent 
    RestEndpoint updateREventEndpoint = new RestEndpoint("updateREvent", RestEndpoint.Method.POST, "/recurrence", "Updates an existing recurrence event in the database. The recurringEventId has to be stored in the database already. Will return true, if the event was found and could be updated.");
    updateREventEndpoint.addFormat(new Format("boolean", null, null));
    updateREventEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, boolean returned"));
    updateREventEndpoint.addRequiredParam(new Param("recurringEvent", Type.TEXT, generateRecurringEvent(), "The RecurringEvent that should be updated."));
    updateREventEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, updateREventEndpoint);   
    
    // Scheduler removeEvent 
    RestEndpoint removeEventEndpoint = new RestEndpoint("removeEvent", RestEndpoint.Method.DELETE, "/event/{eventID}", "Removes the specified event from the database. Returns true if the event could be removed. It is also true if no event was found.");
    removeEventEndpoint.addFormat(new Format("boolean", null, null));
    removeEventEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, boolean returned"));
    removeEventEndpoint.addPathParam(new Param("eventID", Type.STRING, "EventID", "The unique ID of the event."));
    removeEventEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, removeEventEndpoint);     
    
    // Scheduler removeRecurringEvent 
    RestEndpoint removeREventEndpoint = new RestEndpoint("removeREvent", RestEndpoint.Method.DELETE, "/recurrence/{recurringEventID}", "Removes the specified recurringEvent from the database. Returns true if the event could be removed. It is also true if no event was found.");
    removeREventEndpoint.addFormat(new Format("boolean", null, null));
    removeREventEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, boolean returned"));
    removeREventEndpoint.addPathParam(new Param("recurringEventID", Type.STRING, "RecurrentEventID", "The unique ID of the event."));
    removeREventEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, removeREventEndpoint);     
    
    // Scheduler getEvent
    RestEndpoint getEventEndpoint = new RestEndpoint("getEvent", RestEndpoint.Method.GET, "/event/{eventID}", "Get a specific scheduled event.");
    getEventEndpoint.addFormat(new Format("xml", null, null));
    getEventEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, Event XML returned"));
    getEventEndpoint.addPathParam(new Param("eventID", Type.STRING, "c0e3d8a7-7ecc-479b-aee7-8da369e445f2", "The unique ID of the event."));
    getEventEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getEventEndpoint);  
    
    // Scheduler get recurring event
    RestEndpoint getREventEndpoint = new RestEndpoint("getREvent", RestEndpoint.Method.GET, "/recurrence/{recurringEventID}", "Get a specific scheduled recurrent event.");
    getREventEndpoint.addFormat(new Format("xml", null, null));
    getREventEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, Event XML returned"));
    getREventEndpoint.addPathParam(new Param("recurringEventID", Type.STRING, "Recurring Event ID", "The unique ID of the recurring event."));
    getREventEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getREventEndpoint);    
    
    // Scheduler getEventsFromRecurringEvent
    RestEndpoint getEventsFromRecurringEventEndpoint = new RestEndpoint("getEventsFromRecurringEvent", RestEndpoint.Method.GET, "/recurrence/{recurringEventID}/events", "returns scheduled events, that pass the filter.\nfilter: an xml definition of the filter. Tags that are not included will noct be filtered. Possible values for order by are title,creator,series,time-asc,time-desc,contributor,channel,location,device");
    getEventsFromRecurringEventEndpoint.addFormat(new Format("xml", null, null));
    getEventsFromRecurringEventEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, List of Events as XML returned"));
    getEventsFromRecurringEventEndpoint.addPathParam(new Param("recurringEventID", Type.STRING, "Recurring Event ID", "The unique ID of the recurring event."));
    getEventsFromRecurringEventEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getEventsFromRecurringEventEndpoint);
    
    // Scheduler getEvents
    RestEndpoint getEventsEndpoint = new RestEndpoint("events", RestEndpoint.Method.POST, "/events", "returns scheduled events, that pass the filter.\nfilter: an xml definition of the filter. Tags that are not included will noct be filtered. Possible values for order by are title,creator,series,time-asc,time-desc,contributor,channel,location,device");
    getEventsEndpoint.addFormat(new Format("xml", null, null));
    getEventsEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, List of Events as XML returned"));
    getEventsEndpoint.addOptionalParam(new Param("filter", Type.TEXT, generateSchedulerFilter(), "The SchedulerFilter that should be applied."));
    getEventsEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getEventsEndpoint);
    
    // Scheduler getEvents
    RestEndpoint getAllEventsEndpoint = new RestEndpoint("allEvents", RestEndpoint.Method.GET, "/events/all", "returns all scheduled events");
    getAllEventsEndpoint.addFormat(new Format("xml", null, null));
    getAllEventsEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, List of Events as XML returned"));
    getAllEventsEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getAllEventsEndpoint);  
    
    // Scheduler getAllRecurringEvents
    RestEndpoint getAllREventsEndpoint = new RestEndpoint("allRecurringEvents", RestEndpoint.Method.GET, "/recurrences/all", "returns all scheduled events");
    getAllREventsEndpoint.addFormat(new Format("xml", null, null));
    getAllREventsEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, List of Events as XML returned"));
    getAllREventsEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getAllREventsEndpoint);
    
    // Scheduler getUpcomingEvents 
    RestEndpoint getUpcomingEventsEndpoint = new RestEndpoint("upcomingEvents", RestEndpoint.Method.GET, "/events/upcoming", "returns all upcoming events. Returns true if the event was found and could be removed.");
    getUpcomingEventsEndpoint.addFormat(new Format("xml", null, null));
    getUpcomingEventsEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, List of Events as XML returned"));
    getUpcomingEventsEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getUpcomingEventsEndpoint);    
    
    // Scheduler findConflictingEvents 
    RestEndpoint findConflictingEventsEndpoint = new RestEndpoint("findConflictingEvents", RestEndpoint.Method.POST, "/events/conflict", "Looks for events that are conflicting with the given event, because they use the same recorder at the same time ");
    findConflictingEventsEndpoint.addFormat(new Format("xml", null, null));
    findConflictingEventsEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, List of Events as XML returned, or an empty list, if no conflicts were found"));
    findConflictingEventsEndpoint.addRequiredParam(new Param("event", Type.TEXT, generateEvent(), "The Event that should be checked for conflicts."));
    findConflictingEventsEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, findConflictingEventsEndpoint);  
    
    // Scheduler findConflictingREvents 
    RestEndpoint findConflictingREventsEndpoint = new RestEndpoint("findConflictingRecurringEvents", RestEndpoint.Method.POST, "/recurrence/conflict", "Looks for events that are conflicting with the given event, because they use the same recorder at the same time ");
    findConflictingREventsEndpoint.addFormat(new Format("xml", null, null));
    findConflictingREventsEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, List of Events as XML returned, or an empty list, if no conflicts were found"));
    findConflictingREventsEndpoint.addRequiredParam(new Param("recurringEvent", Type.TEXT, generateEvent(), "The recurring Event that should be checked for conflicts. "));
    findConflictingREventsEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, findConflictingREventsEndpoint);      

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
    RestEndpoint getCalendarForCaptureAgentEndpoint = new RestEndpoint("getCalendarForCaptureAgent", RestEndpoint.Method.GET, "/getCalendarForCaptureAgent/{captureAgentID}", "Gets the iCalendar with all upcoming events for the specified capture agent id. ");
    getCalendarForCaptureAgentEndpoint.addFormat(new Format("ics", "iCalendar", "http://tools.ietf.org/html/rfc2445"));
    getCalendarForCaptureAgentEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, iCalendar returned"));
    getCalendarForCaptureAgentEndpoint.addPathParam(new Param("captureAgentID", Type.STRING, "recorder", "The ID that specifies the capture agent."));
    getCalendarForCaptureAgentEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getCalendarForCaptureAgentEndpoint);    
    
    return DocUtil.generate(data);
  }

  /**
   * Creates an example XML of an Event for the documentation.
   * @return A XML with a Event
   */ 
  private String generateEvent() {
    Event e = new EventImpl();
    e.generateId();
    e.setRecurringEventId("demo-recurring-event");
    e.setPositionInRecurrence(0);
    LinkedList<Metadata> metadata = new LinkedList<Metadata>();
    metadata.add(new MetadataImpl("title", "demo title"));
    metadata.add(new MetadataImpl("location", "demo location"));
    metadata.add(new MetadataImpl("abstract", "demo abstract"));
    metadata.add(new MetadataImpl("creator", "demo creator"));
    metadata.add(new MetadataImpl("contributor", "demo contributor"));
    metadata.add(new MetadataImpl("time.start", "1317499200000"));
    metadata.add(new MetadataImpl("time.end", "1317507300000"));
    metadata.add(new MetadataImpl("device", "demo"));
    metadata.add(new MetadataImpl("resources", "vga, audio"));
    metadata.add(new MetadataImpl("series-id", "demo series"));
    e.setMetadata(metadata); 
    
    SchedulerBuilder builder = SchedulerBuilder.getInstance();
    try {
      
      String result = builder.marshallEvent((EventImpl)e);
      logger.info("Event: "+result);
      return result;
    } catch (Exception e1) {
      logger.warn("Could not marshall example event: {}", e1.getMessage());
      return null;
    }
  }
  
  /**
   * Creates an example XML of an RecurringEvent for the documentation.
   * @return A XML with a RecurringEvent
   */ 
  private String generateRecurringEvent() {
    RecurringEvent e = new RecurringEventImpl();
    e.generateId();
    e.setRecurringEventId("demo-recurring-event");
    LinkedList<Metadata> metadata = new LinkedList<Metadata>();
    metadata.add(new MetadataImpl("title", "demo title"));
    metadata.add(new MetadataImpl("location", "demo location"));
    metadata.add(new MetadataImpl("abstract", "demo abstract"));
    metadata.add(new MetadataImpl("creator", "demo creator"));
    metadata.add(new MetadataImpl("contributor", "demo contributor"));
    metadata.add(new MetadataImpl("recurrence.start", "1317499200000"));
    metadata.add(new MetadataImpl("recurrence.end", "1329350400000"));
    metadata.add(new MetadataImpl("recurrence.duration", "3600000"));
    metadata.add(new MetadataImpl("device", "demo"));
    metadata.add(new MetadataImpl("resources", "vga, audio"));
    metadata.add(new MetadataImpl("series-id", "demo series"));
    e.setMetadata(metadata); 
    
    e.setRecurrence("FREQ=WEEKLY;BYDAY=TU;BYHOUR=9;BYMINUTE=15");
    
    SchedulerBuilder builder = SchedulerBuilder.getInstance();
    try {
      
      String result = builder.marshallRecurringEvent((RecurringEventImpl)e);
      logger.info("Event: "+result);
      return result;
    } catch (Exception e1) {
      logger.warn("Could not marshall example event: {}", e1.getMessage());
      return null;
    }
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
