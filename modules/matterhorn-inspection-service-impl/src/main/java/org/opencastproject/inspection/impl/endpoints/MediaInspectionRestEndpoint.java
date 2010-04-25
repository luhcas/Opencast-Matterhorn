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
package org.opencastproject.inspection.impl.endpoints;

import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.receipt.api.Receipt;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;
import org.opencastproject.util.doc.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * A service endpoint to expose the {@link MediaInspectionService} via REST.
 */
@Path("/")
public class MediaInspectionRestEndpoint {
  private static final Logger logger = LoggerFactory.getLogger(MediaInspectionRestEndpoint.class);
  
  protected MediaInspectionService service;
  public void setService(MediaInspectionService service) {
    this.service = service;
  }
  public void unsetService(MediaInspectionService service) {
    this.service = null;
  }

  public MediaInspectionRestEndpoint() {
    docs = generateDocs();
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  public Response getTrack(@QueryParam("url") URI url) {
    checkNotNull(service);
    try {
      Receipt r = service.inspect(url, false);
      return Response.ok(r).build();
    } catch (Exception e) {
      logger.info(e.getMessage());
      return Response.serverError().status(400).build();
    }
  }
  
  @GET
  @Path("/receipt/{id}.xml")
  @Produces(MediaType.TEXT_XML)
  public Response getReceipt(@PathParam("id") String id) {
    checkNotNull(service);
    try {
      Receipt r = service.getReceipt(id);
      return Response.ok(r).build();
    } catch (Exception e) {
      logger.info(e.getMessage());
      return Response.serverError().status(400).build();
    }
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    return docs;
  }

  protected final String docs;
  private String[] notes = {
          "All paths above are relative to the REST endpoint base (something like http://your.server/inspection/rest)",
          "If the service is down or not working it will return a status 503, this means the underlying service is not working and is either restarting or has failed",
          "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In other words, there is a bug! You should file an error report with your server logs from the time when the error occurred: <a href=\"https://issues.opencastproject.org\">Opencast Issue Tracker</a>",
          "Here is a sample video for testing: <a href=\"./?url=http://source.opencastproject.org/svn/modules/opencast-media/trunk/src/test/resources/aonly.mov\">analyze sample video</a>" };

  private String generateDocs() {
    DocRestData data = new DocRestData("inspection", "Media inspection", "/inspection/rest", notes);
    // abstract
    data.setAbstract("This service extracts technical metadata from media files.");
    // getTrack
    RestEndpoint endpoint = new RestEndpoint("getTrack", RestEndpoint.Method.GET, "/",
            "Analyze a given media file, returning a receipt to check on the status and outcome of the job");
    endpoint.addOptionalParam(new Param("url", Param.Type.STRING, null, "Location of the media file"));
    endpoint.addFormat(Format.xml());
    endpoint.addStatus(Status.OK("XML encoded receipt is returned"));
    endpoint.addStatus(new Status(400, "Problem retrieving media file or invalid media file or URL"));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, endpoint);

    // getReceipt
    RestEndpoint receiptEndpoint = new RestEndpoint("getReceipt", RestEndpoint.Method.GET, "/receipt/{id}.xml",
            "Check on the status of an inspection receipt");
    receiptEndpoint.addPathParam(new Param("id", Param.Type.STRING, null, "ID of the receipt"));
    receiptEndpoint.addFormat(Format.xml());
    receiptEndpoint.addStatus(Status.OK("XML encoded receipt is returned"));
    receiptEndpoint.addStatus(new Status(400, "Problem retrieving receipt"));
    receiptEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, receiptEndpoint);
    
    return DocUtil.generate(data);
  }

  /**
   * Checks if the service or services are available,
   * if not it handles it by returning a 503 with a message
   * @param services an array of services to check
   */
  protected void checkNotNull(Object... services) {
    if (services != null) {
      for (Object object : services) {
        if (object == null) {
          throw new javax.ws.rs.WebApplicationException(javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE);
        }
      }
    }
  }

}
