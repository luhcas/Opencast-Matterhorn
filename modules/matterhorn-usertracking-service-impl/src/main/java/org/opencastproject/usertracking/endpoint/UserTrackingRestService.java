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

import org.opencastproject.rest.RestPublisher;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.usertracking.api.Annotation;
import org.opencastproject.usertracking.api.Stats;
import org.opencastproject.usertracking.api.UserTrackingService;
import org.opencastproject.util.DocUtil;
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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
      String serviceUrl = (String) cc.getProperties().get(RestPublisher.SERVICE_PATH_PROPERTY);
      docs = generateDocs(serviceUrl);
    }
  }

  /**
   * @return XML with all footprints
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("annotations.xml")
  public Response getAnnotationsAsXml(@QueryParam("id") String id, @QueryParam("key") String key,
          @QueryParam("day") String day, @QueryParam("limit") int limit, @QueryParam("offset") int offset) {

    // Are the values of offset and limit valid?
    if (offset < 0 || limit < 0)
      return Response.status(Status.BAD_REQUEST).build();

    // Set default value of limit (max result value)
    if (limit == 0)
      limit = 10;

    if (!StringUtils.isEmpty(id) && !StringUtils.isEmpty(key))
      return Response.ok(usertrackingService.getAnnotationsByKeyAndMediapackageId(key, id, offset, limit)).build();
    else if (!StringUtils.isEmpty(key) && !StringUtils.isEmpty(day))
      return Response.ok(usertrackingService.getAnnotationsByKeyAndDay(key, day, offset, limit)).build();
    else if (!StringUtils.isEmpty(key))
      return Response.ok(usertrackingService.getAnnotationsByKey(key, offset, limit)).build();
    else if (!StringUtils.isEmpty(day))
      return Response.ok(usertrackingService.getAnnotationsByDay(day, offset, limit)).build();
    else
      return Response.ok(usertrackingService.getAnnotations(offset, limit)).build();
  }
  
  /**
   * @return JSON with all footprints
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("annotations.json")
  public Response getAnnotationsAsJson(@QueryParam("id") String id, @QueryParam("key") String key,
          @QueryParam("day") String day, @QueryParam("limit") int limit, @QueryParam("offset") int offset) {
    return getAnnotationsAsXml(id, key, day, limit, offset); // same logic, different @Produces annotation
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("stats.xml")
  public Response statsAsXml(@QueryParam("id") String mediapackageId) {
    Stats s = new StatsImpl();
    s.setMediapackageId(mediapackageId);
    s.setViews(usertrackingService.getViews(mediapackageId));

    return Response.ok(s).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("stats.json")
  public Response statsAsJson(@QueryParam("id") String mediapackageId) {
    return statsAsXml(mediapackageId); // same logic, different @Produces annotation
  }
  
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("report.xml")
  public Response reportAsXml(@QueryParam("from") String from, @QueryParam("to") String to,
          @QueryParam("offset") int offset, @QueryParam("limit") int limit) {

    // Are the values of offset and limit valid?
    if (offset < 0 || limit < 0)
      return Response.status(Status.BAD_REQUEST).build();

    // Set default value of limit (max result value)
    if (limit == 0)
      limit = 10;

    if (from == null && to == null)
      return Response.ok(usertrackingService.getReport(offset, limit)).build();
    else
      return Response.ok(usertrackingService.getReport(from, to, offset, limit)).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("report.json")
  public Response reportAsJson(@QueryParam("from") String from, @QueryParam("to") String to,
          @QueryParam("offset") int offset, @QueryParam("limit") int limit) {
    return reportAsXml(from, to, offset, limit); // same logic, different @Produces annotation
  }
  
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("add")
  public Response add(@QueryParam("id") String mediapackageId, @QueryParam("in") int inpoint,
          @QueryParam("out") int outpoint, @QueryParam("key") String key, @QueryParam("value") String value,
          @Context HttpServletRequest request) {

    String sessionId = request.getSession().getId();

    String userId = securityService.getUserName();

    Annotation a = new AnnotationImpl();
    a.setMediapackageId(mediapackageId);
    a.setUserId(userId);
    a.setSessionId(sessionId);
    a.setInpoint(inpoint);
    a.setOutpoint(outpoint);
    a.setKey(key);
    a.setValue(value);

    a = usertrackingService.addAnnotation(a);

    return Response.ok(a).build();
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("footprint.xml")
  public Response getFootprintAsXml(@QueryParam("id") String mediapackageId) {
    String userId = securityService.getUserName();
    
    // Is the mediapackageId passed
    if (mediapackageId == null)
      return Response.status(Status.BAD_REQUEST).build();

    return Response.ok(usertrackingService.getFootprints(mediapackageId, userId)).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("footprint.json")
  public Response getFootprintAsJson(@QueryParam("id") String mediapackageId) {
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
    data.setAbstract("This service is used for tracking user interaction creates, edits and retrieves annotations and viewing statistics.");

    // stats
    RestEndpoint statsEndpoint = new RestEndpoint("stats", RestEndpoint.Method.GET, "/stats.{format}",
            "Get the statistics for an episode");
    statsEndpoint.addFormat(new Format("XML", null, null));
    statsEndpoint.addFormat(new Format("JSON", null, null));
    statsEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("The statistics, expressed as xml or json"));
    statsEndpoint.addPathParam(new Param("format", Type.STRING, "json", "The output format, xml or json"));
    statsEndpoint.addOptionalParam(new Param("id", Type.STRING, null,
            "The ID of the single episode to return the statistics for, if it exists"));
    statsEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, statsEndpoint);

    // add
    RestEndpoint addEndpoint = new RestEndpoint("add", RestEndpoint.Method.GET, "/add",
            "Add an annotation on an episode");
    addEndpoint.addFormat(new Format("XML", null, null));
    addEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("The annotation, expressed as xml"));
    addEndpoint.addOptionalParam(new Param("id", Type.STRING, null, "The ID of the single episode"));
    addEndpoint.addOptionalParam(new Param("in", Type.STRING, null, "The inpoint of the annotation"));
    addEndpoint.addOptionalParam(new Param("out", Type.STRING, null, "The outpoint of the annotation"));
    addEndpoint.addOptionalParam(new Param("key", Type.STRING, null, "The key of the annotation"));
    addEndpoint.addOptionalParam(new Param("value", Type.STRING, null, "The value of the annotation"));
    addEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, addEndpoint);

    // footprint
    RestEndpoint footprintEndpoint = new RestEndpoint("footprint", RestEndpoint.Method.GET, "/footprint.{format}",
            "Get footprints");
    footprintEndpoint.addFormat(new Format("XML", null, null));
    footprintEndpoint.addFormat(new Format("JSON", null, null));
    footprintEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("The footprints, expressed as xml or json"));
    footprintEndpoint.addPathParam(new Param("format", Type.STRING, "json", "The output format, xml or json"));
    footprintEndpoint.addOptionalParam(new Param("id", Type.STRING, null, "The mediapackage ID"));
    footprintEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, footprintEndpoint);

    
    // annotation
    RestEndpoint annotationEndpoint = new RestEndpoint("annotation", RestEndpoint.Method.GET, "/annotations.{format}",
            "Get annotations by key and day");
    annotationEndpoint.addFormat(new Format("XML", null, null));
    annotationEndpoint.addFormat(new Format("JSON", null, null));
    annotationEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("The annotations, expressed as xml or json"));
    annotationEndpoint.addPathParam(new Param("format", Type.STRING, "json", "The output format, xml or json"));
    annotationEndpoint.addOptionalParam(new Param("key", Type.STRING, null, "The key of the annotation"));
    annotationEndpoint.addOptionalParam(new Param("day", Type.STRING, null, "The day of creation (format: YYYYMMDD)"));
    annotationEndpoint.addOptionalParam(new Param("limit", Type.STRING, "0",
            "The maximum number of items to return per page"));
    annotationEndpoint.addOptionalParam(new Param("offset", Type.STRING, "0", "The page number"));
    annotationEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, annotationEndpoint);

    return DocUtil.generate(data);
  }

}
