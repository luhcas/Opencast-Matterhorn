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

import org.opencastproject.rest.RestConstants;
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

import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
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
      serviceUrl = (String) cc.getProperties().get(RestConstants.SERVICE_PATH_PROPERTY);
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
  public Response getEventXml(@PathParam("eventID") Long eventId) throws NotFoundException {
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
  public Response getEventJson(@PathParam("eventID") Long eventId) throws NotFoundException {
    return getEvent(eventId);
  }

  private Response getEvent(Long eventId) throws NotFoundException {
    if (eventId == null)
      return Response.status(Status.BAD_REQUEST).build();
    Event e = service.getEvent(eventId);
    return Response.ok(e).build();
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
  public Response getDublinCoreMetadata(@PathParam("eventId") Long eventId) throws NotFoundException {
    if (eventId == null)
      return Response.status(Status.BAD_REQUEST).build();
    String result = service.getDublinCoreMetadata(eventId);
    return Response.ok(result).build();
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
  public Response getCaptureAgentMetadata(@PathParam("eventId") Long eventId) throws NotFoundException {
    if (eventId == null)
      return Response.status(Status.BAD_REQUEST).build();
    String result = service.getCaptureAgentMetadata(eventId);
    return Response.ok(result).build();
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
  public Response addEvent(@FormParam("event") EventImpl event) throws NotFoundException {
    logger.debug("addEvent(e): {}", event);
    try {
      if (StringUtils.isNotEmpty(event.getRecurrencePattern())) {
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
  public Response deleteEvent(@PathParam("eventId") Long eventId) throws NotFoundException {
    if (eventId == null) {
      return Response.status(Status.BAD_REQUEST).build();
    } else {
      try {
        service.removeEvent(eventId);
        return Response.noContent().type("").build(); // remove content-type, no message-body.
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
  public Response updateEvent(@PathParam("eventId") String eventId, @FormParam("event") EventImpl event)
          throws NotFoundException {
    if (!eventId.isEmpty() && event != null) {
      try {
        event.setEventId(Long.parseLong(eventId));
        service.updateEvent(event, true, true);
        return Response.noContent().type("").build(); // remove content-type, no message-body.
      } catch (Exception e) {
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
   *          A JSON array of event ids.
   * @return true if the event was found and could be updated.
   */
  @SuppressWarnings("unchecked")
  @POST
  @Path("")
  public Response updateEvent(@FormParam("event") EventImpl event, @FormParam("idList") String idList)
          throws NotFoundException {
    JSONParser parser = new JSONParser();
    JSONArray ids = new JSONArray();
    try {
      if (idList != null && !idList.isEmpty()) {
        ids = (JSONArray) parser.parse(idList);
      }
    } catch (ParseException e) {
      logger.warn("Unable to parse json id list: {}", e);
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (!ids.isEmpty() && event != null) {
      try {
        service.updateEvents(ids, event);
        return Response.noContent().type("").build(); // remove content-type, no message-body.
      } catch (Exception e) {
        logger.warn("Unable to update event with id '{}': {}", ids, e);
        return Response.serverError().build();
      }
    } else {
      return Response.status(Status.BAD_REQUEST).build();
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("filter")
  public Response filterEventsDefault(@QueryParam("contributor") String contributor,
          @QueryParam("creator") String creator, @QueryParam("device") String device,
          @QueryParam("series") String series, @QueryParam("start") Long startDate, @QueryParam("end") Long endDate,
          @QueryParam("title") String title, @QueryParam("order") boolean isAsc) {
    return filterEvents(contributor, creator, device, series, startDate, endDate, title, isAsc);
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
  public Response filterEventsXml(@QueryParam("contributor") String contributor, @QueryParam("creator") String creator,
          @QueryParam("device") String device, @QueryParam("series") String series,
          @QueryParam("start") Long startDate, @QueryParam("end") Long endDate, @QueryParam("title") String title,
          @QueryParam("order") boolean isAsc) {
    return filterEvents(contributor, creator, device, series, startDate, endDate, title, isAsc);
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
   * @return List of SchedulerEvents as JSON
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("filter.json")
  public Response filterEventsJson(@QueryParam("contributor") String contributor,
          @QueryParam("creator") String creator, @QueryParam("device") String device,
          @QueryParam("series") String series, @QueryParam("start") Long startDate, @QueryParam("end") Long endDate,
          @QueryParam("title") String title, @QueryParam("order") boolean isAsc) {
    return filterEvents(contributor, creator, device, series, startDate, endDate, title, isAsc);
  }

  private Response filterEvents(String contributor, String creator, String device, String series, Long startDate,
          Long endDate, String title, boolean isAsc) {
    SchedulerFilter filter = new SchedulerFilter().withCreatorFilter(creator).withDeviceFilter(device)
            .withSeriesFilter(series).withTitleFilter(title).withContributorFilter(contributor)
            .withOrderAscending("title", isAsc);

    if (startDate != null && endDate != null) {
      filter.between(new Date(startDate), new Date(endDate));
    } else if (startDate != null) {
      filter.withStart(new Date(startDate));
    } else if (endDate != null) {
      filter.withStop(new Date(endDate));
    }

    try {
      List<Event> events = service.getEvents(filter);
      EventListImpl eventList = new EventListImpl(events);
      return Response.ok(eventList).build();
    } catch (Exception e) {
      logger.error("Exception while filtering events: ", e);
      return Response.serverError().build();
    }
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
  public Response getConflictingEventsXml(@FormParam("device") String device, @FormParam("start") Long startDate,
          @FormParam("end") Long endDate, @FormParam("duration") Long duration, @FormParam("rrule") String rrule) {
    return getConflictingEvents(device, new Date(startDate), new Date(endDate), duration, rrule);
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
  public Response getConflictingEventsJson(@FormParam("device") String device, @FormParam("start") Long startDate,
          @FormParam("end") Long endDate, @FormParam("duration") Long duration, @FormParam("rrule") String rrule) {
    logger.debug("Checking for conflicts");
    return getConflictingEvents(device, new Date(startDate), new Date(endDate), duration, rrule);
  }

  private Response getConflictingEvents(String device, Date startDate, Date endDate, Long duration, String rrule) {
    if (StringUtils.isNotEmpty(device) && startDate != null && endDate != null && duration > 0) {
      try {
        List<Event> events = null;
        if (StringUtils.isNotEmpty(rrule)) {
          events = service.findConflictingEvents(device, rrule, startDate, endDate, duration);
        } else {
          events = service.findConflictingEvents(device, startDate, endDate);
        }
        if (!events.isEmpty()) {
          EventListImpl eventList = new EventListImpl(events);
          return Response.ok(eventList).build();
        } else {
          return Response.noContent().type("").build();
        }
      } catch (Exception e) {
        logger.error(
                "Unable to find conflicting events for " + device + ", " + startDate.toString() + ", "
                        + endDate.toString() + ", " + String.valueOf(duration) + ":", e);
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
  public Response getCalendarForCaptureAgent(@PathParam("captureAgentID") String captureAgentId,
          @Context HttpServletRequest request) throws NotFoundException {
    if (captureAgentId.isEmpty()) {
      return Response.status(Status.BAD_REQUEST).build();
    } else {
      try {
        // If the etag matches the if-not-modified header, return a 304
        Date lastModified = service.getScheduleLastModified(captureAgentId);
        if (lastModified == null) {
          lastModified = new Date();
        }
        String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
        if (StringUtils.isNotBlank(ifNoneMatch) && ifNoneMatch.equals("mod" + Long.toString(lastModified.getTime()))) {
          return Response.notModified("mod" + Long.toString(lastModified.getTime())).expires(null).build();
        }
        String result = service.getCalendarForCaptureAgent(captureAgentId);
        if (!result.isEmpty()) {
          return Response.ok(result).header(HttpHeaders.ETAG, "mod" + Long.toString(lastModified.getTime())).build();
        } else {
          throw new NotFoundException();
        }
      } catch (Exception e) {
        logger.error("Unable to get calendar for capture agent '{}': {}", captureAgentId, e);
        return Response.serverError().build();
      }
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
    addEventEndpoint.addStatus(org.opencastproject.util.doc.Status.created("Event was successfully created."));
    addEventEndpoint
            .addRequiredParam(new Param("event", Type.TEXT, generateEvent(), "The Event that should be stored."));
    addEventEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, addEventEndpoint);

    // Scheduler updateEvent
    RestEndpoint updateEventEndpoint = new RestEndpoint("updateEvent", RestEndpoint.Method.POST, "/{eventId}",
            "Updates an existing event in the database. The event-id has to be stored in the database already.");
    updateEventEndpoint.addStatus(org.opencastproject.util.doc.Status.noContent("Event successfully updated."));
    updateEventEndpoint.addStatus(org.opencastproject.util.doc.Status
            .badRequest("Supplied eventId or Event are incorrect or missing."));
    updateEventEndpoint.addStatus(org.opencastproject.util.doc.Status
            .notFound("An event matching the supplied eventId was not found."));
    updateEventEndpoint.addStatus(org.opencastproject.util.doc.Status.error("A server error occured."));
    updateEventEndpoint.addPathParam(new Param("eventId", Type.STRING, "", "The UUID of the event to update."));
    updateEventEndpoint.addRequiredParam(new Param("event", Type.TEXT, generateEvent(),
            "The Event that should be updated."));
    updateEventEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, updateEventEndpoint);

    // Scheduler removeEvent
    RestEndpoint removeEventEndpoint = new RestEndpoint("removeEvent", RestEndpoint.Method.DELETE, "/{eventId}",
            "Removes the specified event from the database.");
    removeEventEndpoint.addStatus(org.opencastproject.util.doc.Status.noContent("Event successfully deleted."));
    removeEventEndpoint.addStatus(org.opencastproject.util.doc.Status
            .badRequest("Supplied eventId is incorrect or missing."));
    removeEventEndpoint.addStatus(org.opencastproject.util.doc.Status
            .notFound("An Event matching the supplied eventId was not found."));
    removeEventEndpoint.addStatus(org.opencastproject.util.doc.Status.error("A server error occured."));
    removeEventEndpoint.addPathParam(new Param("eventId", Type.STRING, "EventId", "The UUID of the event."));
    removeEventEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, removeEventEndpoint);

    // Scheduler getEvent
    RestEndpoint getEventEndpoint = new RestEndpoint("getEvent", RestEndpoint.Method.GET, "/{eventId}.{format}",
            "Get a specific scheduled event.");
    getEventEndpoint.addFormat(Format.xml("XML representation of the event."));
    getEventEndpoint.addFormat(Format.json("JSON representation of the event."));
    // getEventEndpoint.setAutoPathFormat(true);
    getEventEndpoint.addStatus(org.opencastproject.util.doc.Status.ok("XML or JSON representation of the Event."));
    getEventEndpoint.addStatus(org.opencastproject.util.doc.Status
            .badRequest("Supplied eventId is incorrect or missing."));
    getEventEndpoint.addStatus(org.opencastproject.util.doc.Status
            .notFound("An Event matching the supplied eventId was not found."));
    getEventEndpoint.addStatus(org.opencastproject.util.doc.Status.error("A server error occured."));
    getEventEndpoint.addPathParam(new Param("eventId", Type.STRING, "c0e3d8a7-7ecc-479b-aee7-8da369e445f2",
            "The UUID of the event."));
    getEventEndpoint.addPathParam(new Param("format", Type.STRING, "json",
            "The data format of the response, xml or json."));
    getEventEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getEventEndpoint);

    // Scheduler filterEventsEndpoint
    RestEndpoint filterEventsEndpoint = new RestEndpoint("filterEvents", RestEndpoint.Method.GET, "/filter.{format}",
            "returns scheduled events, that pass the filter. All string fields are case-sensative.");
    filterEventsEndpoint.addFormat(Format
            .xml("XML representation of a list of the events conforming to the supplied filter."));
    filterEventsEndpoint.addFormat(Format
            .json("JSON representation of a list of the event conforming to the supplied filter."));
    // filterEventsEndpoint.setAutoPathFormat(true);
    filterEventsEndpoint.addStatus(org.opencastproject.util.doc.Status
            .ok("XML or JSON representation of a list of events belonging to a recurring event."));
    filterEventsEndpoint.addStatus(org.opencastproject.util.doc.Status
            .badRequest("Supplied filter is incorrect or missing."));
    // contributor, creator, device, series, startDate, endDate, title, isAsc
    filterEventsEndpoint.addPathParam(new Param("format", Type.STRING, "xml",
            "The data format of the response, xml or json."));
    filterEventsEndpoint.addOptionalParam(new Param("contributor", Type.STRING, "Joe Shmoe",
            "Pattern to search for in contributor"));
    filterEventsEndpoint.addOptionalParam(new Param("creator", Type.STRING, "Joe Shmoe",
            "Pattern to search for in creator"));
    filterEventsEndpoint.addOptionalParam(new Param("device", Type.STRING, "demo_capture_agent",
            "Pattern to search for in the device name"));
    filterEventsEndpoint.addOptionalParam(new Param("series", Type.STRING, "A Series",
            "Pattern to search for in the series"));
    filterEventsEndpoint.addOptionalParam(new Param("title", Type.STRING, "Katsudon",
            "Pattern to search for in the title"));
    filterEventsEndpoint.addOptionalParam(new Param("start", Type.STRING, String.valueOf(System.currentTimeMillis()),
            "Start date prior to which events will be filtered"));
    filterEventsEndpoint
            .addOptionalParam(new Param("end", Type.STRING,
                    String.valueOf(System.currentTimeMillis() + 60 * 60 * 1000),
                    "End date after which events will be filtered"));
    filterEventsEndpoint.addOptionalParam(new Param("order", Type.STRING, "true",
            "Sort events by title ascending (true) or decending (false)"));
    filterEventsEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, filterEventsEndpoint);

    // Scheduler getEvents
    RestEndpoint getAllEventsEndpoint = new RestEndpoint("allEvents", RestEndpoint.Method.GET, "/all/events",
            "returns all scheduled events");
    getAllEventsEndpoint.addFormat(Format.xml("XML representation of a list of all events."));
    getAllEventsEndpoint.addFormat(Format.json("JSON representation of a list of all events."));
    getAllEventsEndpoint.setAutoPathFormat(true);
    getAllEventsEndpoint.addStatus(org.opencastproject.util.doc.Status
            .ok("XML or JSON representation of a list of all events."));
    getAllEventsEndpoint.addStatus(org.opencastproject.util.doc.Status.error("A server error occured."));
    getAllEventsEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getAllEventsEndpoint);

    // Scheduler getUpcomingEvents
    RestEndpoint getUpcomingEventsEndpoint = new RestEndpoint("upcomingEvents", RestEndpoint.Method.GET, "/upcoming",
            "returns all upcoming events. Returns true if the event was found and could be removed.");
    getUpcomingEventsEndpoint.addFormat(Format.xml("XML representation of a list of all events."));
    getUpcomingEventsEndpoint.addFormat(Format.json("JSON representation of a list of all events."));
    getUpcomingEventsEndpoint.setAutoPathFormat(true);
    getUpcomingEventsEndpoint.addStatus(org.opencastproject.util.doc.Status
            .ok("XML or JSON representation of a list of all upcoming events."));
    getUpcomingEventsEndpoint.addStatus(org.opencastproject.util.doc.Status.error("A server error occured."));
    getUpcomingEventsEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getUpcomingEventsEndpoint);

    // Scheduler findConflictingEvents
    // RestEndpoint findConflictingEventsEndpoint = new RestEndpoint("findConflictingEvents", RestEndpoint.Method.POST,
    // "/conflict.{format}",
    // "Looks for events that are conflicting with the given event, because they use the same recorder at the same time ");
    // findConflictingEventsEndpoint.addFormat(Format
    // .xml("XML representation of a list of all events that conflict with supplied event."));
    // findConflictingEventsEndpoint.addFormat(Format
    // .json("JSON representation of a list of all events that conflict with supplied event."));
    // //findConflictingEventsEndpoint.setAutoPathFormat(true);
    // findConflictingEventsEndpoint.addStatus(org.opencastproject.util.doc.Status
    // .ok("XML or JSON representation of a list of all upcoming events."));
    // findConflictingEventsEndpoint.addStatus(org.opencastproject.util.doc.Status
    // .badRequest("Supplied Event is invalid or missing."));
    // findConflictingEventsEndpoint.addStatus(org.opencastproject.util.doc.Status.error("A server error occured."));
    // findConflictingEventsEndpoint.addPathParam(new Param("format", Type.STRING, "xml",
    // "The data format of the response, xml or json."));
    // findConflictingEventsEndpoint.addRequiredParam(new Param("event", Type.TEXT, generateEvent(),
    // "The Event that should be checked for conflicts."));
    // findConflictingEventsEndpoint.setTestForm(RestTestForm.auto());
    // data.addEndpoint(RestEndpoint.Type.READ, findConflictingEventsEndpoint);

    // Scheduler findConflictingREvents
    RestEndpoint findConflictingREventsEndpoint = new RestEndpoint("findConflictingRecurringEvents",
            RestEndpoint.Method.POST, "/conflict.{format}",
            "Looks for events that are conflicting with the given event, because they use the same recorder at the same time ");
    findConflictingREventsEndpoint.addFormat(Format
            .xml("XML representation of a list of all events that conflict with supplied recurring event."));
    findConflictingREventsEndpoint.addFormat(Format
            .json("JSON representation of a list of all events that conflict with supplied recurring event."));
    // findConflictingREventsEndpoint.setAutoPathFormat(true);
    findConflictingREventsEndpoint.addStatus(org.opencastproject.util.doc.Status
            .ok("XML or JSON representation of a list of all upcoming events."));
    findConflictingREventsEndpoint.addStatus(org.opencastproject.util.doc.Status
            .badRequest("Supplied RecurringEvent is invalid or missing."));
    findConflictingREventsEndpoint.addStatus(org.opencastproject.util.doc.Status.error("A server error occured."));
    findConflictingREventsEndpoint.addPathParam(new Param("format", Type.STRING, "xml",
            "The data format of the response, xml or json."));
    // findConflictingREventsEndpoint.addRequiredParam(new Param("recurringEvent", Type.TEXT, generateEvent(),
    // "The RecurringEvent that should be checked for conflicts. "));
    findConflictingREventsEndpoint
            .addRequiredParam(new Param("device", Type.STRING, "", "The device that is scheduled"));
    findConflictingREventsEndpoint.addRequiredParam(new Param("start", Type.STRING, "",
            "The start of the recurrence period"));
    findConflictingREventsEndpoint.addRequiredParam(new Param("end", Type.STRING, "",
            "The end of the recurrence period"));
    findConflictingREventsEndpoint.addRequiredParam(new Param("duration", Type.STRING, "",
            "The duration of the single event"));
    findConflictingREventsEndpoint.addRequiredParam(new Param("rrule", Type.STRING, "", "The recurrence rule"));
    findConflictingREventsEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, findConflictingREventsEndpoint);

    // Scheduler getDublinCoreMetadata
    RestEndpoint getDublinCoreMetadataEndpoint = new RestEndpoint("getDublinCoreMetadata", RestEndpoint.Method.GET,
            "/{eventId}/dublincore", "Gets a XML with the Dublin Core metadata for the specified event. ");
    getDublinCoreMetadataEndpoint.addFormat(Format.xml("Dublincore metadata for the supplied eventId."));
    getDublinCoreMetadataEndpoint.addStatus(org.opencastproject.util.doc.Status
            .ok("Dublinecore XML document containing the event's metadata."));
    getDublinCoreMetadataEndpoint.addStatus(org.opencastproject.util.doc.Status
            .badRequest("Supplied eventId is invalid or missing."));
    getDublinCoreMetadataEndpoint.addStatus(org.opencastproject.util.doc.Status
            .notFound("No Event matching the supplied eventId was found."));
    getDublinCoreMetadataEndpoint.addStatus(org.opencastproject.util.doc.Status.error("A server error occured."));
    getDublinCoreMetadataEndpoint.addPathParam(new Param("eventId", Type.STRING,
            "c0e3d8a7-7ecc-479b-aee7-8da369e445f2", "The unique ID of the event."));
    getDublinCoreMetadataEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getDublinCoreMetadataEndpoint);

    // Scheduler getCaptureAgentMetadata
    RestEndpoint getCaptureAgentMetadataEndpoint = new RestEndpoint("getCaptureAgentMetadata", RestEndpoint.Method.GET,
            "/{eventId}/captureAgentMetadata",
            "Gets java Properties file with technical metadata for the specified event. ");
    getCaptureAgentMetadataEndpoint.addFormat(new Format("properties",
            "Java Properties files that is needed by the capture agent.", null));
    getCaptureAgentMetadataEndpoint.addStatus(org.opencastproject.util.doc.Status
            .ok("Java Properties file for the event."));
    getCaptureAgentMetadataEndpoint.addStatus(org.opencastproject.util.doc.Status
            .badRequest("Supplied eventId is invalid or missing."));
    getCaptureAgentMetadataEndpoint.addStatus(org.opencastproject.util.doc.Status
            .notFound("No Event matching the supplied eventId was found."));
    getCaptureAgentMetadataEndpoint.addStatus(org.opencastproject.util.doc.Status.error("A server error occured."));
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
            .ok("iCalendar file containing the scheduled Events for the capture agent."));
    getCaptureAgentMetadataEndpoint.addStatus(org.opencastproject.util.doc.Status
            .badRequest("Supplied captureAgentId is invalid or missing."));
    getCaptureAgentMetadataEndpoint.addStatus(org.opencastproject.util.doc.Status
            .notFound("No capture agent matching the supplied catureAgentId was found."));
    getCaptureAgentMetadataEndpoint.addStatus(org.opencastproject.util.doc.Status.error("A server error occured."));
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
}
