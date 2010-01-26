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
package org.opencastproject.capture.admin.endpoint;

import org.opencastproject.util.DocUtil;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;
import org.opencastproject.util.doc.Status;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.capture.admin.api.AgentStateUpdate;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.capture.admin.api.Recording;
import org.opencastproject.capture.admin.api.RecordingStateUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FIXME -- Add javadocs
 */
@Path("/")
public class CaptureAgentStateRestService {
  private static final Logger logger = LoggerFactory.getLogger(CaptureAgentStateRestService.class);
  private CaptureAgentStateService service;
  public void setService(CaptureAgentStateService service) {
    this.service = service;
  }

  public void unsetService(CaptureAgentStateService service) {
    this.service = null;
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("agents/{name}")
  public AgentStateUpdate getAgentState(@PathParam("name") String agentName) {
    return new AgentStateUpdate(service.getAgentState(agentName));
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("agents/{name}")
  public AgentStateUpdate setAgentState(@PathParam("name") String agentName, @FormParam("state") String state) {
    service.setAgentState(agentName, state);
    return new AgentStateUpdate(service.getAgentState(agentName));
    //return Response.ok(agentName + " set to " + state).build();
  }

  @DELETE
  @Path("agents/{name}")
  //TODO: removeAgent should return a boolean indicating if the agent existed.
  // This way this endpoint could return a 404 if the agent didn't exist, or OK otherwise
  public Response removeAgent(@FormParam("agentName") String agentName) {
    service.removeAgent(agentName);
    return Response.ok(agentName + " removed").build();
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("agents")
  public List<AgentStateUpdate> getKnownAgents() {
    LinkedList<AgentStateUpdate> update = new LinkedList<AgentStateUpdate>();
    Map<String, Agent> data = service.getKnownAgents();
    //Run through and build a map of updates (rather than states)
    for (Entry<String, Agent> e : data.entrySet()) {
      update.add(new AgentStateUpdate(e.getValue()));
    }
    return update;
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("recordings/{id}")
  public RecordingStateUpdate getRecordingState(@PathParam("id") String id) {
    return new RecordingStateUpdate(service.getRecordingState(id));
  }

  @POST
  @Path("recordings/{id}")
  // TODO: setRecordingState should return a boolean indicating if this recording existed or not
  // This way we could return a 404 if the recording doesn't exist, or OK otherwise
  public Response setRecordingState(@PathParam("id") String id, @FormParam("state") String state) {
    service.setRecordingState(id, state);
    return Response.ok(id + " set to " + state).build();
  }

  @DELETE
  @Path("recordings/{id}")
  // TODO: removeRecording should return a boolean indicating if this recording existed or not
  // This way we could return a 404 if the recording doesn't exist, or OK otherwise
  public Response removeRecording(@PathParam("id") String id) {
    service.removeRecording(id);
    return Response.ok(id + " removed").build();
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("recordings")
  public List<RecordingStateUpdate> getAllRecordings() {
    LinkedList<RecordingStateUpdate> update = new LinkedList<RecordingStateUpdate>();
    Map<String, Recording> data = service.getKnownRecordings();
    //Run through and build a map of updates (rather than states)
    for (Entry<String, Recording> e : data.entrySet()) {
      update.add(new RecordingStateUpdate(e.getValue()));
    }
    return update;
  }

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

  private String generateDocs() {
    DocRestData data = new DocRestData("captureadminservice", "Capture Admin Service", "/capture-admin/rest", notes);

    // getAgent
    RestEndpoint endpoint = new RestEndpoint("getAgent", RestEndpoint.Method.GET,
        "/agents/{name}",
        "Return the state of a given capture agent");
    endpoint.addPathParam(new Param("name", Param.Type.STRING, null,
        "The name of a given capture agent"));
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addStatus(Status.OK("{agentState}"));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, endpoint);

    // setAgent
    endpoint = new RestEndpoint("setAgent", RestEndpoint.Method.POST,
        "/agents/{name}",
        "Set the status of a given capture agent, registering it if it is new");
    endpoint.addPathParam(new Param("name", Param.Type.STRING, null,
        "The name of a given capture agent"));
    endpoint.addRequiredParam(new Param("state", Param.Type.STRING, null,
        "The state of the capture agent"));
    endpoint.addFormat(new Format("HTML", null, null));
    endpoint.addStatus(Status.OK("{agentName} set to {state}"));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // removeAgent
    endpoint = new RestEndpoint("removeAgent", RestEndpoint.Method.DELETE,
        "/agents/{name}",
        "Remove record of a given capture agent");
    endpoint.addPathParam(new Param("name", Param.Type.STRING, null,
        "The name of a given capture agent"));
    endpoint.addFormat(new Format("HTML", null, null));
    endpoint.addStatus(Status.OK("{agentName} removed"));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // getKnownAgents
    endpoint = new RestEndpoint("getKnownAgents", RestEndpoint.Method.GET,
        "/agents",
        "Return all registered capture agents and their state");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addStatus(Status.OK(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, endpoint);

    // getRecordingState
    endpoint = new RestEndpoint("getRecordingState", RestEndpoint.Method.GET,
        "/recordings/{id}",
        "Return the state of a given recording");
    endpoint.addPathParam(new Param("id", Param.Type.STRING, null,
        "The ID of a given recording"));
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addStatus(Status.OK(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, endpoint);

    // setRecordingState
    endpoint = new RestEndpoint("setRecordingState", RestEndpoint.Method.POST,
        "/recordings/{id}",
        "Set the status of a given recording, registering it if it is new");
    endpoint.addPathParam(new Param("id", Param.Type.STRING, null,
        "The ID of a given recording"));
    endpoint.addRequiredParam(new Param("state", Param.Type.STRING, null,
        "The state of the recording"));
    endpoint.addFormat(new Format("HTML", null, null));
    endpoint.addStatus(Status.OK("{id} set to {state}"));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // removeRecording
    endpoint = new RestEndpoint("removeRecording", RestEndpoint.Method.DELETE,
        "/recordings/{id}",
        "Remove record of a given recording");
    endpoint.addPathParam(new Param("id", Param.Type.STRING, null,
        "The ID of a given recording"));
    endpoint.addFormat(new Format("HTML", null, null));
    endpoint.addStatus(Status.OK("{id} removed"));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // getAllRecordings
    endpoint = new RestEndpoint("getAllRecordings", RestEndpoint.Method.GET,
        "/recordings",
        "Return all registered recordings and their state");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addStatus(Status.OK(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, endpoint);

    return DocUtil.generate(data);
  }

  public CaptureAgentStateRestService() {}
}
