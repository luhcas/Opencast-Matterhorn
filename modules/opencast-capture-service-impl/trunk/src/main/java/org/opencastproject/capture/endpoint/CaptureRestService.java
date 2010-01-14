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
package org.opencastproject.capture.endpoint;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.opencastproject.capture.api.CaptureAgent;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;
import org.opencastproject.util.doc.Param.Type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FIXME -- Add javadocs
 */
@Path("/")
public class CaptureRestService {
  private static final Logger logger = LoggerFactory.getLogger(CaptureRestService.class);
  private CaptureAgent service;
  protected final String docs;


  protected String generateDocs() {
    DocRestData data = new DocRestData("CaptureAgent", "Capture Agent", "/capture/rest", null);
    //// startCapture signatures
    // startCapture()
    RestEndpoint startNoParamEndpoint = new RestEndpoint("startNP", RestEndpoint.Method.GET, "/startCapture", "Starts a capture with the default parameters");
    startNoParamEndpoint.addFormat(new Format("String", "The recording ID for the capture started", null));
    startNoParamEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, results returned"));
    startNoParamEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("Couldn't start capture with default parameters"));
    startNoParamEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, startNoParamEndpoint);
    // startCapture(Properties)
    RestEndpoint startPropEndpoint = new RestEndpoint("startMP", RestEndpoint.Method.POST, "/startCapture", "Starts a capture with the default properties and a provided MediaPackage");
    startPropEndpoint.addFormat(new Format("String", "The recording ID for the capture started", null));
    startPropEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, results returned"));
    startPropEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("Couldn't start capture with provided parameters"));
    startPropEndpoint.addRequiredParam(new Param("config", Type.STRING, null, "The properties to set for this recording"));
    startPropEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, startPropEndpoint);

    //// stopCapture signatures
    // stopCapture()
    RestEndpoint stopNoParamEndpoint = new RestEndpoint("stopNP", RestEndpoint.Method.GET, "/stopCapture", "Stops the current capture");
    stopNoParamEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, recording properly stopped"));
    stopNoParamEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("Failed to stop the capture, or no current active capture"));
    stopNoParamEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, stopNoParamEndpoint);
    // stopCapture(recordingID)
    RestEndpoint stopIDEndpoint = new RestEndpoint("stopID", RestEndpoint.Method.POST, "/stopCapture", "Stops the current capture if its ID matches the argument");
    stopIDEndpoint.addRequiredParam(new Param("recordingID", Type.STRING, null, "The ID for the recording to stop"));
    stopIDEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("Current capture with the specified ID stopped succesfully"));
    // TODO: check if this can be returned
    //stopIDEndpoint.addStatus(org.opencastproject.util.doc.Status.NOT_FOUND("A workflow instance with this ID was not found"));
    stopIDEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("Failed to stop the capture, no current active capture, or no matching ID"));
    stopIDEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, stopIDEndpoint);

    //// ingest(recordingID)
    RestEndpoint ingestEndpoint = new RestEndpoint("ingest", RestEndpoint.Method.POST, "/ingest", "Ingests the specified capture");
    ingestEndpoint.addRequiredParam(new Param("recordingID", Type.STRING, null, "The ID for the recording to ingest"));
    ingestEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("Capture ingested succesfully"));
    ingestEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("Ingestion failed"));
    ingestEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, ingestEndpoint);

    return DocUtil.generate(data);
  }  

  public void setService(CaptureAgent service) {
    this.service = service;
  }

  public void unsetService(CaptureAgent service) {
    this.service = null;
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("startCapture")
  public Response startCapture() {
    String out;
    try {
      out = service.startCapture();
      return Response.ok("Start Capture OK. OUT: " + out).build();
    } catch (Exception e) {
      return Response.serverError().status(500).build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_PLAIN)
  @Path("startCapture")
  public Response startCapture(@FormParam("config") String config) {
    Properties configuration = new Properties();
    try {
      configuration.load(new ByteArrayInputStream(config.getBytes()));
    } catch (IOException e1) {
      logger.error("Unable to parse configuration string into valid capture config.  Continuing with default settings.");
    }

    String out;
    try {
      out = service.startCapture(configuration);
      if (out != null)
        return Response.ok("Started capture " + out).build();
      else
        return Response.serverError().build();
    } catch (Exception e) {
      return Response.serverError().status(500).build();
    }
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    return docs;
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("stopCapture")
  public Response stopCapture() {
    boolean out;
    try {
      out = service.stopCapture();
      if (out)
        return Response.ok("Stop Capture OK. OUT: " + out).build();
      else
        return Response.serverError().status(500).build();
    } catch (Exception e) {
      return Response.serverError().status(500).build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_PLAIN)
  @Path("stopCapture")
  public Response stopCapture(@FormParam("recordingID") String recordingID) {
    boolean out;
    try {
      out = service.stopCapture(recordingID);
      if (out)
        return Response.ok("Stopped Capture").build();
      else
        return Response.serverError().status(500).build();
    } catch (Exception e) {
      return Response.serverError().status(500).build();
    }
  }

  public CaptureRestService() {
    docs = generateDocs();
  }
}
