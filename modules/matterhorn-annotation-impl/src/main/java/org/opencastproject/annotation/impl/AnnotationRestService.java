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
package org.opencastproject.annotation.impl;

import org.opencastproject.annotation.api.Annotation;
import org.opencastproject.annotation.api.AnnotationService;
import org.opencastproject.rest.RestConstants;
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
 * The REST endpoint for the annotation service.
 */
@Path("/")
public class AnnotationRestService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(AnnotationRestService.class);

  /** The annotation service */
  private AnnotationService annotationService;

  /** This server's base URL */
  protected String serverUrl = UrlSupport.DEFAULT_BASE_URL;

  /** The REST endpoint's base URL */
  // this is the default value, which may be overridden in the OSGI service registration
  protected String serviceUrl = "/annotation";

  /**
   * Method to set the service this REST endpoint uses
   * 
   * @param service
   *          the annotation service implementation
   */
  public void setService(AnnotationService service) {
    this.annotationService = service;
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
  @Path("annotations.xml")
  public Response getAnnotationsAsXml(@QueryParam("episode") String id, @QueryParam("type") String type,
          @QueryParam("day") String day, @QueryParam("limit") int limit, @QueryParam("offset") int offset) {

    // Are the values of offset and limit valid?
    if (offset < 0 || limit < 0)
      return Response.status(Status.BAD_REQUEST).build();

    // Set default value of limit (max result value)
    if (limit == 0)
      limit = 10;

    if (!StringUtils.isEmpty(id) && !StringUtils.isEmpty(type))
      return Response.ok(annotationService.getAnnotationsByTypeAndMediapackageId(type, id, offset, limit)).build();
    else if (!StringUtils.isEmpty(type) && !StringUtils.isEmpty(day))
      return Response.ok(annotationService.getAnnotationsByTypeAndDay(type, day, offset, limit)).build();
    else if (!StringUtils.isEmpty(type))
      return Response.ok(annotationService.getAnnotationsByType(type, offset, limit)).build();
    else if (!StringUtils.isEmpty(day))
      return Response.ok(annotationService.getAnnotationsByDay(day, offset, limit)).build();
    else
      return Response.ok(annotationService.getAnnotations(offset, limit)).build();
  }

  /**
   * @return JSON with all footprints
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("annotations.json")
  public Response getAnnotationsAsJson(@QueryParam("episode") String id, @QueryParam("type") String type,
          @QueryParam("day") String day, @QueryParam("limit") int limit, @QueryParam("offset") int offset) {
    return getAnnotationsAsXml(id, type, day, limit, offset); // same logic, different @Produces annotation
  }

  @PUT
  @Path("")
  @Produces(MediaType.TEXT_XML)
  public Response add(@FormParam("episode") String mediapackageId, @FormParam("in") int inpoint,
          @FormParam("out") int outpoint, @FormParam("type") String type, @FormParam("value") String value,
          @Context HttpServletRequest request) {
    String sessionId = request.getSession().getId();
    Annotation a = new AnnotationImpl();
    a.setMediapackageId(mediapackageId);
    a.setSessionId(sessionId);
    a.setInpoint(inpoint);
    a.setOutpoint(outpoint);
    a.setType(type);
    a.setValue(value);
    a = annotationService.addAnnotation(a);
    URI uri;
    try {
      uri = new URI(
              UrlSupport.concat(new String[] { serverUrl, serviceUrl, Long.toString(a.getAnnotationId()), ".xml" }));
    } catch (URISyntaxException e) {
      throw new WebApplicationException(e);
    }
    return Response.created(uri).entity(a).build();
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("/{id}.xml")
  public AnnotationImpl getAnnotationAsXml(@PathParam("id") String idAsString) throws NotFoundException {
    Long id = null;
    try {
      id = Long.parseLong(idAsString);
    } catch (NumberFormatException e) {
      throw new WebApplicationException(e, Status.BAD_REQUEST);
    }
    return (AnnotationImpl) annotationService.getAnnotation(id);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/{id}.xml")
  public AnnotationImpl getAnnotationAsJson(@PathParam("id") String idAsString) throws NotFoundException {
    return getAnnotationAsXml(idAsString);
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

  /**
   * Generates the REST documentation
   * 
   * @param
   * @return The HTML with the documentation
   */
  protected String generateDocs(String serviceUrl) {
    DocRestData data = new DocRestData("annotation", "Annotation Service", serviceUrl, null);

    // abstract
    data.setAbstract("This service is used for managing user generated annotations.");

    // add
    RestEndpoint addEndpoint = new RestEndpoint("add", RestEndpoint.Method.PUT, "/", "Add an annotation on an episode");
    addEndpoint.addFormat(new Format("XML", null, null));
    addEndpoint.addStatus(org.opencastproject.util.doc.Status.created("The URL to this annotation is returned in the "
            + "Location header, and the annotation itelf is returned in the response body."));
    addEndpoint.addRequiredParam(new Param("episode", Type.STRING, null, "The ID of the episode"));
    addEndpoint.addRequiredParam(new Param("type", Type.STRING, null, "The type of annotation"));
    addEndpoint.addRequiredParam(new Param("in", Type.STRING, null, "The time, or inpoint, of the annotation"));
    addEndpoint.addOptionalParam(new Param("out", Type.STRING, null, "The optional outpoint of the annotation"));
    addEndpoint.addRequiredParam(new Param("value", Type.TEXT, null, "The value of the annotation"));
    addEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, addEndpoint);

    // annotation
    RestEndpoint annotationEndpoint = new RestEndpoint("annotation", RestEndpoint.Method.GET, "/annotations.{format}",
            "Get annotations by key and day");
    annotationEndpoint.addFormat(new Format("XML", null, null));
    annotationEndpoint.addFormat(new Format("JSON", null, null));
    annotationEndpoint.addStatus(org.opencastproject.util.doc.Status.ok("The annotations, expressed as xml or json"));
    annotationEndpoint.addPathParam(new Param("format", Type.STRING, "json", "The output format, xml or json"));
    annotationEndpoint
            .addOptionalParam(new Param("episode", Type.STRING, null, "The mediapackage (episode) identifier"));
    annotationEndpoint.addOptionalParam(new Param("type", Type.STRING, null, "The type of annotation"));
    annotationEndpoint.addOptionalParam(new Param("day", Type.STRING, null, "The day of creation (format: YYYYMMDD)"));
    annotationEndpoint.addOptionalParam(new Param("limit", Type.STRING, "0",
            "The maximum number of items to return per page"));
    annotationEndpoint.addOptionalParam(new Param("offset", Type.STRING, "0", "The page number"));
    annotationEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, annotationEndpoint);

    return DocUtil.generate(data);
  }

}
