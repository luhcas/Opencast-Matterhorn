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
package org.opencastproject.feedback.endpoint;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringUtils;
import org.opencastproject.feedback.api.Annotation;
import org.opencastproject.feedback.api.FeedbackService;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.doc.DocRestData;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST Endpoint for Feedback Service
 */
@Path("/")
public class FeedbackRestService {

  private static final Logger logger = LoggerFactory.getLogger(FeedbackRestService.class);

  private FeedbackService feedbackService;

  protected String serverUrl = UrlSupport.DEFAULT_BASE_URL;

  /**
   * Method to set the service this REST endpoint uses
   * 
   * @param service
   */
  public void setService(FeedbackService service) {
    this.feedbackService = service;
  }

  /**
   * Method to unset the service this REST endpoint uses
   * 
   * @param service
   */
  public void unsetService(FeedbackService service) {
    this.feedbackService = null;
  }

  /**
   * The method tha will be called, if the service will be activated
   * 
   * @param cc
   *          The ComponentContext of this service
   */
  public void activate(ComponentContext cc) {
    // Get the configured server URL
    if (cc == null) {
      serverUrl = UrlSupport.DEFAULT_BASE_URL;
    } else {
      String ccServerUrl = cc.getBundleContext().getProperty("serverUrl");
      logger.info("configured server url is {}", ccServerUrl);
      if (ccServerUrl == null) {
        serverUrl = UrlSupport.DEFAULT_BASE_URL;
      } else {
        serverUrl = ccServerUrl;
      }
    }
  }

  /**
   * @return XML with all footprints
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("annotation")
  public Response getAnnotations(@QueryParam("key") String key, @QueryParam("day") String day,
          @QueryParam("limit") int limit, @QueryParam("offset") int offset) {

    // Are the values of offset and limit valid?
    if (offset < 0 || limit < 0)
      return Response.status(Status.BAD_REQUEST).build();

    // Set default value of limit (max result value)
    if (limit == 0)
      limit = 10;

    if (!StringUtils.isEmpty(key) && !StringUtils.isEmpty(day))
      return Response.ok(feedbackService.getAnnotationsByKeyAndDay(key, day, offset, limit)).build();
    else if (!StringUtils.isEmpty(key))
      return Response.ok(feedbackService.getAnnotationsByKey(key, offset, limit)).build();
    else if (!StringUtils.isEmpty(day))
      return Response.ok(feedbackService.getAnnotationsByDay(day, offset, limit)).build();
    else
      return Response.ok(feedbackService.getAnnotations(offset, limit)).build();

  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("add")
  public Response add(@QueryParam("id") String mediapackageId, @QueryParam("session") int sessionId,
          @QueryParam("in") int inpoint, @QueryParam("out") int outpoint, @QueryParam("key") String key,
          @QueryParam("value") String value) {

    Annotation a = new AnnotationImpl();
    a.setMediapackageId(mediapackageId);
    a.setSessionId(sessionId);
    a.setInpoint(inpoint);
    a.setOutpoint(outpoint);
    a.setKey(key);
    a.setValue(value);

    a = feedbackService.addAnnotation(a);

    return Response.ok(a).build();
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
    if (docs == null) {
      docs = generateDocs();
    }
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
  protected String generateDocs() {
    DocRestData data = new DocRestData("Feedback", "Feedback Service", "/feedback/rest", notes);

    // abstract
    data.setAbstract("This service creates, edits and retrieves annotations.");

    return DocUtil.generate(data);
  }

}
