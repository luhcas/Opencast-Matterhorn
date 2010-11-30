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

import org.opencastproject.rest.RestPublisher;
import org.opencastproject.scheduler.api.Event;
import org.opencastproject.scheduler.api.Metadata;
import org.opencastproject.scheduler.api.SchedulerFilter;
import org.opencastproject.scheduler.impl.EventImpl;
import org.opencastproject.scheduler.impl.EventListImpl;
import org.opencastproject.scheduler.impl.MetadataImpl;
import org.opencastproject.scheduler.impl.SchedulerServiceImpl;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.Param.Type;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;


import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * REST Endpoint for Scheduler Service
 */
@Path("/")
public class SchedulerRestService {
  private static final Logger logger = LoggerFactory.getLogger(SchedulerRestService.class);
  private static final long ONE_HOUR_IN_MILLIS = 1000 * 60 * 60;
  private SchedulerServiceImpl service;

  protected String serverUrl = UrlSupport.DEFAULT_BASE_URL;
  protected String serviceUrl = null;

  /**
   * Method to set the service this REST endpoint uses
   * 
   * @param service
   */
  public void setService(SchedulerServiceImpl service) {
    this.service = service;
  }

  /**
   * Method to unset the service this REST endpoint uses
   * 
   * @param service
   */
  public void unsetService(SchedulerServiceImpl service) {
    this.service = null;
  }

  /**
   * The method that will be called, if the service will be activated
   * 
   * @param cc
   *          The ComponentContext of this service
   */
  public void activate(ComponentContext cc) {
    // Get the configured server URL
    if (cc == null) {
      serverUrl = UrlSupport.DEFAULT_BASE_URL;
    } else {
      String ccServerUrl = cc.getBundleContext().getProperty("org.opencastproject.server.url");
      logger.debug("configured server url is {}", ccServerUrl);
      if (ccServerUrl == null) {
        serverUrl = UrlSupport.DEFAULT_BASE_URL;
      } else {
        serverUrl = ccServerUrl;
      }
      serviceUrl = (String) cc.getProperties().get(RestPublisher.SERVICE_PATH_PROPERTY);
      docs = generateDocs(serviceUrl);
    }
  }

  /**
   * Get a specific scheduled event.
   * 
   * @param eventID
   *          The unique ID of the event.
   * @return event XML with the data of the event
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("{eventID}.xml")
  public Response getEventXml(@PathParam("eventID") Long eventId) {
    return getEvent(eventId);
  }

  /**
   * Get a specific scheduled event.
   * 
   * @param eventID
   *          The unique ID of the event.
   * @return event XML with the data of the event
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{eventID}.json")
  public Response getEventJson(@PathParam("eventID") Long eventId) {
    return getEvent(eventId);
  }

  private Response getEvent(Long eventId) {
    if (eventId == null) {
      return Response.status(Status.BAD_REQUEST).build();
    } else {
      try {
        Event e = service.getEvent(eventId);
        if (e != null) {
          return Response.ok(e).build();
        } else {
          return Response.status(Status.NOT_FOUND).build();
        }
      } catch (Exception e) {
        logger.warn("Error occured while looking for event '{}': {}", eventId, e);
        return Response.serverError().build();
      }
    }
  }

  /**
   * Gets a XML with the Dublin Core metadata for the specified event.
   * 
   * @param eventID
   *          The unique ID of the event.
   * @return Dublin Core XML for the event
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("{eventId}/dublincore")
  public Response getDublinCoreMetadata(@PathParam("eventId") Long eventId) {
    if (eventId == null) {
      return Response.status(Status.BAD_REQUEST).build();
    } else {
      try {
        String result = service.getDublinCoreMetadata(eventId);
        if (result != null) {
          return Response.ok(result).build();
        } else {
          return Response.status(Status.NOT_FOUND).build();
        }
      } catch (Exception e) {
        logger.warn("Unable to get dublincore Metadata for id '{}': {}", eventId, e);
        return Response.serverError().build();
      }
    }
  }

  /**
   * Gets java Properties file with technical metadata for the specified event.
   * 
   * @param eventID
   *          The unique ID of the event.
   * @return Java Properties File with the metadata for the event
   */
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("{eventId}/captureAgentMetadata")
  public Response getCaptureAgentMetadata(@PathParam("eventId") Long eventId) {
    if (eventId == null) {
      return Response.status(Status.BAD_REQUEST).build();
    } else {
      try {
        String result = service.getCaptureAgentMetadata(eventId);
        if (result != null) {
          return Response.ok(result).build();
        } else {
          return Response.status(Status.NOT_FOUND).build();
        }
      } catch (Exception e) {
        logger.warn("Unable to get capture agent metadata for id '{}': {}", eventId, e);
        return Response.serverError().build();
      }
    }
  }

  /**
   * Stores a new event in the database. As a result the event will be returned, but some fields especially the event-id
   * may have been updated. Within the metadata section it is possible to add any additional metadata as a key-value
   * pair. The information will be stored even if the key is yet unknown.
   * 
   * @param e
   *          The SchedulerEvent that should be stored.
   * @return The same event with some updated fields.
   */
  @PUT
  @Path("/")
  @Produces(MediaType.TEXT_XML)
  public Response addEvent(@FormParam("event") EventImpl event) {
    logger.debug("addEvent(e): {}", event);
    try {
      if (event.getRecurrencePattern() != null && !event.getRecurrencePattern().isEmpty()) {
        // try to create event and it's recurrences
        service.addRecurringEvent(event);
        return Response.status(Status.CREATED).build();
      } else {
        Event result = service.addEvent(event);
        if (result != null) { // TODO: addEvent never returns null. When it's updated to throw EntityExistsException
                              // Handle it though...
          return Response
                  .status(Status.CREATED)
                  .header("Location",
                          PathSupport.concat(new String[] { this.serverUrl, this.serviceUrl,
                                  result.getEventId() + ".xml" })).entity(result).build();
        } else {
          logger.error("Event that should be added is null");
          return Response.status(Status.NOT_FOUND).build();
        }
      }
    } catch (Exception e) {
      logger.warn("Unable to create new event", e);
      return Response.serverError().build();
    }
  }

  /**
   * 
   * Removes the specified event from the database. Returns true if the event was found and could be removed.
   * 
   * @param eventID
   *          The unique ID of the event.
   * @return true if the event was found and could be deleted.
   */
  @DELETE
  @Path("{eventId}")
  public Response deleteEvent(@PathParam("eventId") Long eventId) {
    if (eventId == null) {
      return Response.status(Status.BAD_REQUEST).build();
    } else {
      try {
        service.removeEvent(eventId);
        return Response.noContent().type("").build(); // remove content-type, no message-body.
      } catch(NotFoundException e) {
        return Response.status(Status.NOT_FOUND).build();
      } catch (Exception e) {
        logger.warn("Unable to delete event with id '{}': {}", eventId, e);
        return Response.serverError().build();
      }
    }
  }

  /**
   * Updates an existing event in the database. The event-id has to be stored in the database already. Will return true,
   * if the event was found and could be updated.
   * 
   * @param e
   *          The SchedulerEvent that should be updated
   * @return true if the event was found and could be updated.
   */
  @POST
  @Path("{eventId}")
  public Response updateEvent(@PathParam("eventId") String eventId, @FormParam("event") EventImpl event) {
    if (!eventId.isEmpty() && event != null) {
      try {
        service.updateEvent(event);
        return Response.noContent().type("").build(); // remove content-type, no message-body.
      } catch (NotFoundException e) {
        return Response.status(Status.NOT_FOUND).build();
      } catch(Exception e) {
        logger.warn("Unable to update event with id '{}': {}", eventId, e);
        return Response.serverError().build();
      }
    } else {
      return Response.status(Status.BAD_REQUEST).build();
    }
  }
  
  /**
   * Updates a list of existing event in the database. The event-id has to be stored in the database already.
   * 
   * @param e
   *          The SchedulerEvent that should be updated
   * @param eventIdList
   *        A JSON array of event ids.
   * @return true if the event was found and could be updated.
   */
  @SuppressWarnings("unchecked")
  @POST
  @Path("")
  public Response updateEvent(@FormParam("event") EventImpl event, @FormParam("idList") String idList) {
    JSONParser parser = new JSONParser();
    JSONArray ids = new JSONArray();
    try{
      ids = (JSONArray)parser.parse(idList);
    } catch (ParseException e) { 
    logger.warn("Unable to parse json id list: {}", e);
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (!ids.isEmpty() && event != null) {
      try {
        service.updateEvents(ids, event);
        return Response.noContent().type("").build(); // remove content-type, no message-body.
      } catch (NotFoundException nFEx) {
        return Response.status(Status.NOT_FOUND).build();
      } catch (Exception e) {
        logger.warn("Unable to update event with id '{}': {}", ids, e);
        return Response.serverError().build();
      }
    } else {
      return Response.status(Status.BAD_REQUEST).build();
    }
  }

  /**
   * returns scheduled events, that pass the filter. filter: an xml definition of the filter. Tags that are not included
   * will not be filtered. Possible values for order by are
   * title,creator,series,time-asc,time-desc,contributor,channel,location,device
   * 
   * @param filter
   *          exact id to search for pattern to search for pattern to search for A short description of the content of
   *          the lecture begin of the period of valid events end of the period of valid events pattern to search for ID
   *          of the series which will be filtered ID of the channel that will be filtered pattern to search for pattern
   *          to search for pattern to search for
   *          title|creator|series|time-asc|time-desc|contributor|channel|location|device">
   * @return List of SchedulerEvents as XML
   */ 
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("filter.xml")
  public Response filterEventsXml(@QueryParam("co") String contributor,
                                  @QueryParam("cr") String creator,
                                  @QueryParam("de") String device,
                                  @QueryParam("se") String series,
                                  @QueryParam("st") Long startDate,
                                  @QueryParam("ti") String title,
                                  @QueryParam("so") boolean isAsc) {
    return filterEvents(contributor, creator, device, series, startDate, title, isAsc);
  }
   
  /** 
  * returns scheduled events, that pass the filter. filter: an xml definition of the filter.
  * Tags that are not included will not be filtered. Possible values for order by are
  * title,creator,series,time-asc,time-desc,contributor,channel,location,device
  * @param filter
  *          exact id to search for pattern to search for pattern to search for A short description of the content of
  *          the lecture begin of the period of valid events end of the period of valid events pattern to search for ID
  *          of the series which will be filtered ID of the channel that will be filtered pattern to search for pattern
  *          to search for pattern to search for
  *          title|creator|series|time-asc|time-desc|contributor|channel|location|device">
  * @return List of SchedulerEvents as JSON
  */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("filter.json") 
  public Response filterEventsJson(@QueryParam("co") String contributor,
                                  @QueryParam("cr") String creator,
                                  @QueryParam("de") String device,
                                  @QueryParam("se") String series,
                                  @QueryParam("st") Long startDate,
                                  @QueryParam("ti") String title,
                                  @QueryParam("so") boolean isAsc) {
    return filterEvents(contributor, creator, device, series, startDate, title, isAsc);
  }

  private Response filterEvents(String contributor, String creator, String device, String series, Long startDate, String title, boolean isAsc) {
    /*if (filter != null) {
      try {
        logger.debug("Filter events with {}",filter);
        EventListImpl eventList = new EventListImpl(service.getEvents(filter));
        return Response.ok(eventList).type("").build();
      } catch (Exception e) { return
        Response.serverError().build();
      }
    } else {
      return Response.status(Status.BAD_REQUEST).build();
    }*/
    return Response.status(Status.BAD_REQUEST).build();
  }
   
  /**
   * Looks for events that are conflicting with the given event, because they use the same recorder at the same time.
   * 
   * @param e
   *          The event that should be checked for conflicts
   * @return An XML with the list of conflicting events
   */
  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("conflict.xml")
  public Response getConflictingEventsXml(@FormParam("event") EventImpl event) {
    return getConflictingEvents(event);
  }

  /**
   * Looks for events that are conflicting with the given event, because they use the same recorder at the same time.
   * 
   * @param e
   *          The event that should be checked for conflicts
   * @return An JSON with the list of conflicting events
   */
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("conflict.json")
  public Response getConflictingEventsJson(@FormParam("event") EventImpl event) {
    return getConflictingEvents(event);
  }

  private Response getConflictingEvents(EventImpl event) {
    if (event != null) {
      try {
        List<Event> events = service.findConflictingEvents(event);
        if (!events.isEmpty()) {
          EventListImpl eventList = new EventListImpl(events);
          return Response.ok(eventList).build();
        } else {
          return Response.noContent().type("").build();
        }
      } catch (Exception e) {
        logger.error("Unable to find conflicting events for {}: {}", event, e);
        return Response.serverError().build();
      }
    } else {
      return Response.status(Status.BAD_REQUEST).build();
    }
  }

  /**
   * Lists all events in the database, without any filter
   * 
   * @return XML with all events
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("all/events.xml")
  public Response getAllEventsXml() {
    return getAllEvents();
  }

  /**
   * Lists all events in the database, without any filter
   * 
   * @return JSON with all events
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("all/events.json")
  public Response getAllEventsJson() {
    return getAllEvents();
  }

  private Response getAllEvents() {
    try {
      EventListImpl eventList = new EventListImpl(service.getEvents(null));
      return Response.ok(eventList).build();
    } catch (Exception e) {
      logger.error("Unable to return all events: {}", e);
      return Response.serverError().build();
    }
  }

  /**
   * Lists all future events in the database, without any filter
   * 
   * @return XML with all events
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("upcoming.xml")
  public Response getUpcomingEventsXml() {
    return getUpcomingEvents();
  }

  /**
   * Lists all future events in the database, without any filter
   * 
   * @return JSON with all events
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("upcoming.json")
  public Response getUpcomingEventsJson() {
    return getUpcomingEvents();
  }

  private Response getUpcomingEvents() {
    SchedulerFilter filter = new SchedulerFilter();
    filter.withStart(new Date(System.currentTimeMillis()));
    try {
      EventListImpl eventList = new EventListImpl(service.getEvents(filter));
      return Response.ok(eventList).build();
    } catch (Exception e) {
      logger.error("Unable to return upcoming events: {}", e);
      return Response.serverError().build();
    }
  }

  /**
   * Gets the iCalendar with all (even old) events for the specified capture agent id.
   * 
   * @param captureAgentID
   *          The ID that specifies the capture agent.
   * @return an iCalendar
   */
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("{captureAgentID}/calendar")
  public Response getCalendarForCaptureAgent(@PathParam("captureAgentID") String captureAgentId) {
    if (!captureAgentId.isEmpty()) {
      try {
        String result = service.getCalendarForCaptureAgent(captureAgentId);
        if (!result.isEmpty()) {
          return Response.ok(result).build();
        } else {
          return Response.status(Status.NOT_FOUND).build();
        }
      } catch (Exception e) {
        logger.error("Unable to get calendar for capture agent '{}': {}", captureAgentId, e);
        return Response.serverError().build();
      }
    } else {
      return Response.status(Status.BAD_REQUEST).build();
    }
  }

  /**
   * returns the REST documentation
   * 
   * @return the REST documentation, if available
   */
  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    return docs;
  }

  protected String docs;
  private String[] notes = {
          "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
          "If the service is down or not working it will return a status 503, this means the the underlying service is not working and is either restarting or has failed",
          "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In other words, there is a bug! You should file an error report with your server logs from the time when the error occurred: <a href=\"https://issues.opencastproject.org\">Opencast Issue Tracker</a>", };

  /**
   * Generates the REST documentation
   * 
   * @return The HTML with the documentation
   */
  protected String generateDocs(String serviceUrl) {
    DocRestData data = new DocRestData("Scheduler", "Scheduler Service", serviceUrl, notes);

    // abstract
    data.setAbstract("This service creates, edits and retrieves and helps manage scheduled capture events.");

    // Scheduler addEvent
    RestEndpoint addEventEndpoint = new RestEndpoint("addEvent", RestEndpoint.Method.PUT, "/",
            "Stores a new event in the database.");
    addEventEndpoint.addStatus(org.opencastproject.util.doc.Status.CREATED("Event was successfully created."));
    addEventEndpoint
            .addRequiredParam(new Param("event", Type.TEXT, generateEvent(), "The Event that should be stored."));
    addEventEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, addEventEndpoint);

    // Scheduler updateEvent
    RestEndpoint updateEventEndpoint = new RestEndpoint("updateEvent", RestEndpoint.Method.POST, "/{eventId}",
            "Updates an existing event in the database. The event-id has to be stored in the database already.");
    updateEventEndpoint.addStatus(org.opencastproject.util.doc.Status.NO_CONTENT("Event successfully updated."));
    updateEventEndpoint.addStatus(org.opencastproject.util.doc.Status
            .BAD_REQUEST("Supplied eventId or Event are incorrect or missing."));
    updateEventEndpoint.addStatus(org.opencastproject.util.doc.Status
            .NOT_FOUND("An event matching the supplied eventId was not found."));
    updateEventEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("A server error occured."));
    updateEventEndpoint.addPathParam(new Param("eventId", Type.STRING, "", "The UUID of the event to update."));
    updateEventEndpoint.addRequiredParam(new Param("event", Type.TEXT, generateEvent(),
            "The Event that should be updated."));
    updateEventEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, updateEventEndpoint);

    // Scheduler removeEvent
    RestEndpoint removeEventEndpoint = new RestEndpoint("removeEvent", RestEndpoint.Method.DELETE, "/{eventId}",
            "Removes the specified event from the database.");
    removeEventEndpoint.addStatus(org.opencastproject.util.doc.Status.NO_CONTENT("Event successfully deleted."));
    removeEventEndpoint.addStatus(org.opencastproject.util.doc.Status
            .BAD_REQUEST("Supplied eventId is incorrect or missing."));
    removeEventEndpoint.addStatus(org.opencastproject.util.doc.Status
            .NOT_FOUND("An Event matching the supplied eventId was not found."));
    removeEventEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("A server error occured."));
    removeEventEndpoint.addPathParam(new Param("eventId", Type.STRING, "EventId", "The UUID of the event."));
    removeEventEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, removeEventEndpoint);

    // Scheduler getEvent
    RestEndpoint getEventEndpoint = new RestEndpoint("getEvent", RestEndpoint.Method.GET, "/{eventId}.{format}",
            "Get a specific scheduled event.");
    getEventEndpoint.addFormat(Format.xml("XML representation of the event."));
    getEventEndpoint.addFormat(Format.json("JSON representation of the event."));
    getEventEndpoint.setAutoPathFormat(true);
    getEventEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("XML or JSON representation of the Event."));
    getEventEndpoint.addStatus(org.opencastproject.util.doc.Status
            .BAD_REQUEST("Supplied eventId is incorrect or missing."));
    getEventEndpoint.addStatus(org.opencastproject.util.doc.Status
            .NOT_FOUND("An Event matching the supplied eventId was not found."));
    getEventEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("A server error occured."));
    getEventEndpoint.addPathParam(new Param("eventId", Type.STRING, "c0e3d8a7-7ecc-479b-aee7-8da369e445f2",
            "The UUID of the event."));
    getEventEndpoint.addPathParam(new Param("format", Type.STRING, "json",
            "The data format of the response, xml or json."));
    getEventEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getEventEndpoint);

    // Scheduler filterEventsEndpoint
    RestEndpoint filterEventsEndpoint = new RestEndpoint(
            "filterEvents",
            RestEndpoint.Method.POST,
            "/filter/events",
            "returns scheduled events, that pass the filter.\nfilter: an xml definition of the filter. Tags that are not included will noct be filtered. Possible values for order by are title,creator,series,time-asc,time-desc,contributor,channel,location,device");
    filterEventsEndpoint.addFormat(Format
            .xml("XML representation of a list of the events conforming to the supplied filter."));
    filterEventsEndpoint.addFormat(Format
            .json("JSON representation of a list of the event conforming to the supplied filter."));
    filterEventsEndpoint.setAutoPathFormat(true);
    filterEventsEndpoint.addStatus(org.opencastproject.util.doc.Status
            .OK("XML or JSON representation of a list of events belonging to a recurring event."));
    filterEventsEndpoint.addStatus(org.opencastproject.util.doc.Status
            .BAD_REQUEST("Supplied filter is incorrect or missing."));
    filterEventsEndpoint.addOptionalParam(new Param("filter", Type.TEXT, generateSchedulerFilter(),
            "The SchedulerFilter that should be applied."));
    filterEventsEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, filterEventsEndpoint);

    // Scheduler getEvents
    RestEndpoint getAllEventsEndpoint = new RestEndpoint("allEvents", RestEndpoint.Method.GET, "/all/events",
            "returns all scheduled events");
    getAllEventsEndpoint.addFormat(Format.xml("XML representation of a list of all events."));
    getAllEventsEndpoint.addFormat(Format.json("JSON representation of a list of all events."));
    getAllEventsEndpoint.setAutoPathFormat(true);
    getAllEventsEndpoint.addStatus(org.opencastproject.util.doc.Status
            .OK("XML or JSON representation of a list of all events."));
    getAllEventsEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("A server error occured."));
    getAllEventsEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getAllEventsEndpoint);

    // Scheduler getUpcomingEvents
    RestEndpoint getUpcomingEventsEndpoint = new RestEndpoint("upcomingEvents", RestEndpoint.Method.GET, "/upcoming",
            "returns all upcoming events. Returns true if the event was found and could be removed.");
    getUpcomingEventsEndpoint.addFormat(Format.xml("XML representation of a list of all events."));
    getUpcomingEventsEndpoint.addFormat(Format.json("JSON representation of a list of all events."));
    getUpcomingEventsEndpoint.setAutoPathFormat(true);
    getUpcomingEventsEndpoint.addStatus(org.opencastproject.util.doc.Status
            .OK("XML or JSON representation of a list of all upcoming events."));
    getUpcomingEventsEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("A server error occured."));
    getUpcomingEventsEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getUpcomingEventsEndpoint);

    // Scheduler findConflictingEvents
    RestEndpoint findConflictingEventsEndpoint = new RestEndpoint("findConflictingEvents", RestEndpoint.Method.POST,
            "/event/conflict",
            "Looks for events that are conflicting with the given event, because they use the same recorder at the same time ");
    findConflictingEventsEndpoint.addFormat(Format
            .xml("XML representation of a list of all events that conflict with supplied event."));
    findConflictingEventsEndpoint.addFormat(Format
            .json("JSON representation of a list of all events that conflict with supplied event."));
    findConflictingEventsEndpoint.setAutoPathFormat(true);
    findConflictingEventsEndpoint.addStatus(org.opencastproject.util.doc.Status
            .OK("XML or JSON representation of a list of all upcoming events."));
    findConflictingEventsEndpoint.addStatus(org.opencastproject.util.doc.Status
            .BAD_REQUEST("Supplied Event is invalid or missing."));
    findConflictingEventsEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("A server error occured."));
    findConflictingEventsEndpoint.addRequiredParam(new Param("event", Type.TEXT, generateEvent(),
            "The Event that should be checked for conflicts."));
    findConflictingEventsEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, findConflictingEventsEndpoint);

    // Scheduler findConflictingREvents
    RestEndpoint findConflictingREventsEndpoint = new RestEndpoint("findConflictingRecurringEvents",
            RestEndpoint.Method.POST, "/recurring/conflict",
            "Looks for events that are conflicting with the given event, because they use the same recorder at the same time ");
    findConflictingREventsEndpoint.addFormat(Format
            .xml("XML representation of a list of all events that conflict with supplied recurring event."));
    findConflictingREventsEndpoint.addFormat(Format
            .json("JSON representation of a list of all events that conflict with supplied recurring event."));
    findConflictingREventsEndpoint.setAutoPathFormat(true);
    findConflictingREventsEndpoint.addStatus(org.opencastproject.util.doc.Status
            .OK("XML or JSON representation of a list of all upcoming events."));
    findConflictingREventsEndpoint.addStatus(org.opencastproject.util.doc.Status
            .BAD_REQUEST("Supplied RecurringEvent is invalid or missing."));
    findConflictingREventsEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("A server error occured."));
    findConflictingREventsEndpoint.addRequiredParam(new Param("recurringEvent", Type.TEXT, generateEvent(),
            "The RecurringEvent that should be checked for conflicts. "));
    findConflictingREventsEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, findConflictingREventsEndpoint);

    // Scheduler getDublinCoreMetadata
    RestEndpoint getDublinCoreMetadataEndpoint = new RestEndpoint("getDublinCoreMetadata", RestEndpoint.Method.GET,
            "/event/{eventId}/dublincore", "Gets a XML with the Dublin Core metadata for the specified event. ");
    getDublinCoreMetadataEndpoint.addFormat(Format.xml("Dublincore metadata for the supplied eventId."));
    getDublinCoreMetadataEndpoint.addStatus(org.opencastproject.util.doc.Status
            .OK("Dublinecore XML document containing the event's metadata."));
    getDublinCoreMetadataEndpoint.addStatus(org.opencastproject.util.doc.Status
            .BAD_REQUEST("Supplied eventId is invalid or missing."));
    getDublinCoreMetadataEndpoint.addStatus(org.opencastproject.util.doc.Status
            .NOT_FOUND("No Event matching the supplied eventId was found."));
    getDublinCoreMetadataEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("A server error occured."));
    getDublinCoreMetadataEndpoint.addPathParam(new Param("eventId", Type.STRING,
            "c0e3d8a7-7ecc-479b-aee7-8da369e445f2", "The unique ID of the event."));
    getDublinCoreMetadataEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getDublinCoreMetadataEndpoint);

    // Scheduler getCaptureAgentMetadata
    RestEndpoint getCaptureAgentMetadataEndpoint = new RestEndpoint("getCaptureAgentMetadata", RestEndpoint.Method.GET,
            "/event/{eventId}/captureAgentMetadata",
            "Gets java Properties file with technical metadata for the specified event. ");
    getCaptureAgentMetadataEndpoint.addFormat(new Format("properties",
            "Java Properties files that is needed by the capture agent.", null));
    getCaptureAgentMetadataEndpoint.addStatus(org.opencastproject.util.doc.Status
            .OK("Java Properties file for the event."));
    getCaptureAgentMetadataEndpoint.addStatus(org.opencastproject.util.doc.Status
            .BAD_REQUEST("Supplied eventId is invalid or missing."));
    getCaptureAgentMetadataEndpoint.addStatus(org.opencastproject.util.doc.Status
            .NOT_FOUND("No Event matching the supplied eventId was found."));
    getCaptureAgentMetadataEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("A server error occured."));
    getCaptureAgentMetadataEndpoint.addPathParam(new Param("eventId", Type.STRING,
            "c0e3d8a7-7ecc-479b-aee7-8da369e445f2", "The unique ID of the event."));
    getCaptureAgentMetadataEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getCaptureAgentMetadataEndpoint);

    // Scheduler getCalendarForCaptureAgent
    RestEndpoint getCalendarForCaptureAgentEndpoint = new RestEndpoint("getCalendarForCaptureAgent",
            RestEndpoint.Method.GET, "/{captureAgentId}/calendar",
            "Gets the iCalendar with all upcoming events for the specified capture agent id. ");
    getCalendarForCaptureAgentEndpoint.addFormat(new Format("ics", "iCalendar", "http://tools.ietf.org/html/rfc2445"));
    getCalendarForCaptureAgentEndpoint.addStatus(org.opencastproject.util.doc.Status
            .OK("iCalendar file containing the scheduled Events for the capture agent."));
    getCaptureAgentMetadataEndpoint.addStatus(org.opencastproject.util.doc.Status
            .BAD_REQUEST("Supplied captureAgentId is invalid or missing."));
    getCaptureAgentMetadataEndpoint.addStatus(org.opencastproject.util.doc.Status
            .NOT_FOUND("No capture agent matching the supplied catureAgentId was found."));
    getCaptureAgentMetadataEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("A server error occured."));
    getCalendarForCaptureAgentEndpoint.addPathParam(new Param("captureAgentId", Type.STRING, "recorder",
            "The ID that specifies the capture agent."));
    getCalendarForCaptureAgentEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getCalendarForCaptureAgentEndpoint);

    return DocUtil.generate(data);
  }

  /**
   * Creates an example XML of an Event for the documentation.
   * 
   * @return A XML with a Event
   */
  private String generateEvent() {
    Event e = new EventImpl();
    e.setStartDate(new Date(System.currentTimeMillis() + ONE_HOUR_IN_MILLIS));
    e.setEndDate(new Date(e.getStartDate().getTime() + ONE_HOUR_IN_MILLIS));
    e.setContributor("demo contributor");
    e.setCreator("demo creator");
    e.setDescription("demo description");
    e.setDevice("demo");
    e.setLanguage("en");
    e.setLicense("creative commons");
    e.setSeriesId("demo series");
    e.setResources("vga, audio");
    e.setTitle("demo title");
    LinkedList<Metadata> metadata = new LinkedList<Metadata>();
    metadata.add(new MetadataImpl(e, "location", "demo location"));
    metadata.add(new MetadataImpl(e, "abstract", "demo abstract"));
    e.setMetadataList(metadata);

    SchedulerBuilder builder = SchedulerBuilder.getInstance();
    try {

      String result = builder.marshallEvent((EventImpl) e);
      logger.debug("Event: " + result);
      return result;
    } catch (Exception e1) {
      logger.warn("Could not marshall example event: {}", e1.getMessage());
      return null;
    }
  }

  /**
   * Creates an example XML of an SchedulerFilter for the documentation.
   * 
   * @return A XML with a SchedulerFilter
   */
  private String generateSchedulerFilter() {
    return "<ns2:SchedulerFilter xmlns:ns2=\"http://scheduler.opencastproject.org/\">\n"
            + " <event-id>exact id to search for</event-id>\n" + " <device>pattern to search for</device>\n"
            + " <title>pattern to search for</title>\n" + " <creator>pattern to search for</creator>\n"
            + " <abstract>A short description of the content of the lecture</abstract>\n"
            + " <startdate>begin of the period of valid events</startdate>\n"
            + " <enddate>end of the period of valid events</enddate>\n"
            + " <contributor>pattern to search for</contributor>\n"
            + " <series-id>ID of the series which will be filtered</series-id>\n"
            + " <channel-id>ID of the channel that will be filtered</channel-id>\n"
            + " <location>pattern to search for</location>\n" + " <attendee>pattern to search for</attendee>\n"
            + " <resource>pattern to search for</resource>\n"
            + " <order-by>title|creator|series|time-asc|time-desc|contributor|channel|location|device</order-by>\n"
            + "</ns2:SchedulerFilter>";
  }

}
