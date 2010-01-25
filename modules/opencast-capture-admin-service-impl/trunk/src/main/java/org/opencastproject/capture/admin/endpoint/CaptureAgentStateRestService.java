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

import java.io.IOException;
import java.io.InputStream;
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

import org.apache.commons.io.IOUtils;
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
    return docs;
  }

  protected final String docs;

  public CaptureAgentStateRestService() {
    String docsFromClassloader = null;
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream("/html/index.html");
      docsFromClassloader = IOUtils.toString(in);
    } catch (IOException e) {
      logger.error("failed to read documentation", e);
      docsFromClassloader = "unable to load documentation for " + CaptureAgentStateRestService.class.getName();
    } finally {
      IOUtils.closeQuietly(in);
    }
    docs = docsFromClassloader;
  }
}
