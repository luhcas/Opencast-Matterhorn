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
package org.opencastproject.usertracking.endpoint;

import org.opencastproject.rest.RestConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.usertracking.api.UserTrackingException;
import org.opencastproject.usertracking.api.UserTrackingService;
import org.opencastproject.usertracking.impl.UserActionImpl;
import org.opencastproject.usertracking.impl.UserActionListImpl;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.Param.Type;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * REST Endpoint for User Tracking Service
 */
@Path("/")
public class UserTrackingRestService {

  private static final Logger logger = LoggerFactory.getLogger(UserTrackingRestService.class);

  private UserTrackingService usertrackingService;

  protected SecurityService securityService;

  protected String serverUrl = UrlSupport.DEFAULT_BASE_URL;

  protected String serviceUrl = "/usertracking"; // set this to the default value initially

  /**
   * Method to set the service this REST endpoint uses
   * 
   * @param service
   */
  public void setService(UserTrackingService service) {
    this.usertrackingService = service;
  }

  /**
   * Sets the security service
   * 
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * The method that is called, when the service is activated
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
      logger.info("configured server url is {}", ccServerUrl);
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
   * @return XML with all footprints
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("actions.xml")
  public UserActionListImpl getUserActionsAsXml(@QueryParam("id") String id, @QueryParam("type") String type,
          @QueryParam("day") String day, @QueryParam("limit") int limit, @QueryParam("offset") int offset) {

    // Are the values of offset and limit valid?
    if (offset < 0 || limit < 0)
      throw new WebApplicationException(Status.BAD_REQUEST);

    // Set default value of limit (max result value)
    if (limit == 0)
      limit = 10;
    try {
      if (!StringUtils.isEmpty(id) && !StringUtils.isEmpty(type))
        return (UserActionListImpl) usertrackingService.getUserActionsByTypeAndMediapackageId(type, id, offset, limit);
      else if (!StringUtils.isEmpty(type) && !StringUtils.isEmpty(day))
        return (UserActionListImpl) usertrackingService.getUserActionsByTypeAndDay(type, day, offset, limit);
      else if (!StringUtils.isEmpty(type))
        return (UserActionListImpl) usertrackingService.getUserActionsByType(type, offset, limit);
      else if (!StringUtils.isEmpty(day))
        return (UserActionListImpl) usertrackingService.getUserActionsByDay(day, offset, limit);
      else
        return (UserActionListImpl) usertrackingService.getUserActions(offset, limit);
    } catch (UserTrackingException e) {
      throw new WebApplicationException(e);
    }
  }

  /**
   * @return JSON with all footprints
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("actions.json")
  public UserActionListImpl getUserActionsAsJson(@QueryParam("id") String id, @QueryParam("type") String type,
          @QueryParam("day") String day, @QueryParam("limit") int limit, @QueryParam("offset") int offset) {
    return getUserActionsAsXml(id, type, day, limit, offset); // same logic, different @Produces annotation
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("stats.xml")
  public StatsImpl statsAsXml(@QueryParam("id") String mediapackageId) {
    StatsImpl s = new StatsImpl();
    s.setMediapackageId(mediapackageId);
    try {
      s.setViews(usertrackingService.getViews(mediapackageId));
    } catch (UserTrackingException e) {
      throw new WebApplicationException(e);
    }
    return s;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("stats.json")
  public StatsImpl statsAsJson(@QueryParam("id") String mediapackageId) {
    return statsAsXml(mediapackageId); // same logic, different @Produces annotation
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("report.xml")
  public ReportImpl reportAsXml(@QueryParam("from") String from, @QueryParam("to") String to,
          @QueryParam("offset") int offset, @QueryParam("limit") int limit) {

    // Are the values of offset and limit valid?
    if (offset < 0 || limit < 0)
      throw new WebApplicationException(Status.BAD_REQUEST);

    // Set default value of limit (max result value)
    if (limit == 0)
      limit = 10;

    try {
      if (from == null && to == null)
        return (ReportImpl) usertrackingService.getReport(offset, limit);
      else
        return (ReportImpl) usertrackingService.getReport(from, to, offset, limit);
    } catch (UserTrackingException e) {
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("report.json")
  public ReportImpl reportAsJson(@QueryParam("from") String from, @QueryParam("to") String to,
          @QueryParam("offset") int offset, @QueryParam("limit") int limit) {
    return reportAsXml(from, to, offset, limit); // same logic, different @Produces annotation
  }

  @PUT
  @Path("/")
  @Produces(MediaType.TEXT_XML)
  public Response add(@FormParam("id") String mediapackageId, @FormParam("in") String inString,
          @FormParam("out") String outString, @FormParam("type") String type, @Context HttpServletRequest request) {
    String sessionId = request.getSession().getId();
    String userId = securityService.getUser().getUserName();

    // Parse the in and out strings, which might be empty (hence, we can't let jax-rs handle them properly)
    if (StringUtils.isEmpty(inString)) {
      throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("in must be a non null integer")
              .build());
    }
    Integer in = null;
    try {
      in = Integer.parseInt(StringUtils.trim(inString));
    } catch (NumberFormatException e) {
      throw new WebApplicationException(e);
    }

    Integer out = null;
    if (StringUtils.isEmpty(outString)) {
      out = in;
    } else {
      try {
        out = Integer.parseInt(StringUtils.trim(outString));
      } catch (NumberFormatException e) {
        throw new WebApplicationException(e);
      }
    }

    UserActionImpl a = new UserActionImpl();
    a.setMediapackageId(mediapackageId);
    a.setUserId(userId);
    a.setSessionId(sessionId);
    a.setInpoint(in);
    a.setOutpoint(out);
    a.setType(type);

    try {
      a = (UserActionImpl) usertrackingService.addUserAction(a);
    } catch (UserTrackingException e) {
      throw new WebApplicationException(e);
    }

    URI uri;
    try {
      uri = new URI(UrlSupport.concat(new String[] { serverUrl, serviceUrl, "action", a.getId().toString(), ".xml" }));
    } catch (URISyntaxException e) {
      throw new WebApplicationException(e);
    }
    return Response.created(uri).entity(a).build();
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("action/{id}.xml")
  public UserActionImpl getActionAsXml(@PathParam("id") String actionId) {
    Long id = null;
    try {
      id = Long.parseLong(actionId);
    } catch (NumberFormatException e) {
      throw new WebApplicationException(e);
    }
    try {
      return (UserActionImpl) usertrackingService.getUserAction(id);
    } catch (UserTrackingException e) {
      throw new WebApplicationException(e);
    } catch (NotFoundException e) {
      return null;
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("action/{id}.json")
  public UserActionImpl getActionAsJson(@PathParam("id") String actionId) {
    return getActionAsXml(actionId);
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("footprint.xml")
  public FootprintsListImpl getFootprintAsXml(@QueryParam("id") String mediapackageId) {
    String userId = securityService.getUser().getUserName();

    // Is the mediapackageId passed
    if (mediapackageId == null)
      throw new WebApplicationException(Status.BAD_REQUEST);

    try {
      return (FootprintsListImpl) usertrackingService.getFootprints(mediapackageId, userId);
    } catch (UserTrackingException e) {
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("footprint.json")
  public FootprintsListImpl getFootprintAsJson(@QueryParam("id") String mediapackageId) {
    return getFootprintAsXml(mediapackageId); // this is the same logic... it's just annotated differently
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
   * @param
   * @return The HTML with the documentation
   */
  protected String generateDocs(String serviceUrl) {
    DocRestData data = new DocRestData("UserTracking", "User Tracking Service", serviceUrl, notes);

    // abstract
    data.setAbstract("This service is used for tracking user interaction creates, edits and retrieves user actions and viewing statistics.");

    // stats
    RestEndpoint statsEndpoint = new RestEndpoint("stats", RestEndpoint.Method.GET, "/stats.{format}",
            "Get the statistics for an episode");
    statsEndpoint.addFormat(new Format("XML", null, null));
    statsEndpoint.addFormat(new Format("JSON", null, null));
    statsEndpoint.addStatus(org.opencastproject.util.doc.Status.ok("The statistics, expressed as xml or json"));
    statsEndpoint.addPathParam(new Param("format", Type.STRING, "json", "The output format, xml or json"));
    statsEndpoint.addOptionalParam(new Param("id", Type.STRING, null,
            "The ID of the single episode to return the statistics for, if it exists"));
    statsEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, statsEndpoint);

    // add
    RestEndpoint addEndpoint = new RestEndpoint("add", RestEndpoint.Method.PUT, "/", "Record a user action");
    addEndpoint.addFormat(new Format("XML", null, null));
    addEndpoint.addStatus(org.opencastproject.util.doc.Status.created("The user action, expressed as xml, is returned"
            + " in the response body, and the URL to the user action is returned in the 'Location' header."));
    addEndpoint.addRequiredParam(new Param("id", Type.STRING, null, "The ID of the single episode"));
    addEndpoint.addRequiredParam(new Param("in", Type.STRING, null, "The inpoint of the user action"));
    addEndpoint.addOptionalParam(new Param("out", Type.STRING, null, "The outpoint of the user action"));
    addEndpoint.addRequiredParam(new Param("type", Type.STRING, null, "The type of user action"));
    addEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, addEndpoint);

    // footprint
    RestEndpoint footprintEndpoint = new RestEndpoint("footprint", RestEndpoint.Method.GET, "/footprint.{format}",
            "Get footprints");
    footprintEndpoint.addFormat(new Format("XML", null, null));
    footprintEndpoint.addFormat(new Format("JSON", null, null));
    footprintEndpoint.addStatus(org.opencastproject.util.doc.Status.ok("The footprints, expressed as xml or json"));
    footprintEndpoint.addPathParam(new Param("format", Type.STRING, "json", "The output format, xml or json"));
    footprintEndpoint.addOptionalParam(new Param("id", Type.STRING, null, "The mediapackage ID"));
    footprintEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, footprintEndpoint);

    // user action
    RestEndpoint userActionsEndpoint = new RestEndpoint("actions", RestEndpoint.Method.GET, "/actions.{format}",
            "Get user actions by type and day");
    userActionsEndpoint.addFormat(new Format("XML", null, null));
    userActionsEndpoint.addFormat(new Format("JSON", null, null));
    userActionsEndpoint.addStatus(org.opencastproject.util.doc.Status.ok("The user actions, expressed as xml or json"));
    userActionsEndpoint.addPathParam(new Param("format", Type.STRING, "json", "The output format, xml or json"));
    userActionsEndpoint.addOptionalParam(new Param("type", Type.STRING, null, "The type of the user action"));
    userActionsEndpoint.addOptionalParam(new Param("day", Type.STRING, null, "The day of creation (format: YYYYMMDD)"));
    userActionsEndpoint.addOptionalParam(new Param("limit", Type.STRING, "0",
            "The maximum number of items to return per page"));
    userActionsEndpoint.addOptionalParam(new Param("offset", Type.STRING, "0", "The page number"));
    userActionsEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, userActionsEndpoint);

    return DocUtil.generate(data);
  }

}
