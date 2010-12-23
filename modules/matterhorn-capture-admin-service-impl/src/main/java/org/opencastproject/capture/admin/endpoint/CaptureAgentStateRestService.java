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
package org.opencastproject.capture.admin.endpoint;

import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.capture.admin.api.AgentStateUpdate;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.capture.admin.api.Recording;
import org.opencastproject.capture.admin.api.RecordingStateUpdate;
import org.opencastproject.rest.RestPublisher;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;
import org.opencastproject.util.doc.Status;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * The REST endpoint for the capture agent service on the capture device
 */
@Path("/")
public class CaptureAgentStateRestService {

  private static final Logger logger = LoggerFactory.getLogger(CaptureAgentStateRestService.class);

  private CaptureAgentStateService service;

  /**
   * Callback from OSGi that is called when this service is activated.
   * 
   * @param cc
   *          OSGi component context
   */
  public void activate(ComponentContext cc) {
    String serviceUrl = (String) cc.getProperties().get(RestPublisher.SERVICE_PATH_PROPERTY);
    docs = generateDocs(serviceUrl);
  }

  public void setService(CaptureAgentStateService service) {
    this.service = service;
  }

  public void unsetService(CaptureAgentStateService service) {
    this.service = null;
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("agents/{name}")
  public Response getAgentState(@PathParam("name") String agentName) {
    if (service == null) {
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE).build();
    }
    Agent ret = service.getAgentState(agentName);

    if (ret != null) {
      logger.debug("Returning agent state for {}", agentName);
      return Response.ok(new AgentStateUpdate(ret)).build();
    } else {
      logger.debug("No such agent name: {}", agentName);
      return Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).build();
    }

  }

  @POST
  // @Produces(MediaType.TEXT_XML)
  @Path("agents/{name}")
  // Todo: Capture agent may send an optional FormParam containing it's configured address.
  // If this exists don't use request.getRemoteHost() for the URL
  public Response setAgentState(@Context HttpServletRequest request, @FormParam("address") String address,
          @PathParam("name") String agentName, @FormParam("state") String state) {
    if (service == null) {
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE).build();
    }
    logger.debug("Agents URL: {}", address);

    int result = service.setAgentState(agentName, state);
    String captureAgentAddress = "";
    if (address != null && !address.isEmpty()) {
      captureAgentAddress = address;
    } else {
      captureAgentAddress = request.getRemoteHost();
    }
    if (!service.setAgentUrl(agentName, captureAgentAddress)) {
      return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
    switch (result) {
    case CaptureAgentStateService.OK:
      logger.debug("{}'s state successfully set to {}", agentName, state);
      return Response.ok(agentName + " set to " + state).build();
    case CaptureAgentStateService.BAD_PARAMETER:
      logger.debug("{} is not a valid state", state);
      return Response.status(javax.ws.rs.core.Response.Status.BAD_REQUEST).build();
    case CaptureAgentStateService.NO_SUCH_AGENT:
      logger.debug("The agent {} is not registered in the system");
      return Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).build();
    default:
      logger.error("Unexpected server error in setAgent endpoint");
      return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @DELETE
  @Path("agents/{name}")
  public Response removeAgent(@PathParam("name") String agentName) {
    if (service == null) {
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE).build();
    }

    int result = service.removeAgent(agentName);

    switch (result) {
    case CaptureAgentStateService.OK:
      logger.debug("The agent {} was successfully removed", agentName);
      return Response.ok(agentName + " removed").build();
    case CaptureAgentStateService.NO_SUCH_AGENT:
      logger.debug("The agent {} is not registered in the system", agentName);
      return Response.status(Response.Status.NOT_FOUND).build();
    default:
      logger.error("Unexpected server error in removeAgent endpoint");
      return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("agents")
  public List<AgentStateUpdate> getKnownAgents() {
    logger.debug("Returning list of known agents...");
    LinkedList<AgentStateUpdate> update = new LinkedList<AgentStateUpdate>();
    if (service != null) {
      Map<String, Agent> data = service.getKnownAgents();
      logger.debug("Agents: {}", data);
      // Run through and build a map of updates (rather than states)
      for (Entry<String, Agent> e : data.entrySet()) {
        AgentStateUpdate updateItem = new AgentStateUpdate(e.getValue());
        Properties props = service.getAgentCapabilities(updateItem.getName());
        Iterator<String> propKeys = props.stringPropertyNames().iterator();
        Set<String> devices = new HashSet<String>();
        while (propKeys.hasNext()) {
          String key = propKeys.next();
          String[] parts = key.split("\\.");
          if ((parts[1].equalsIgnoreCase("device")) && (!parts[2].equalsIgnoreCase("timezone"))) {
            // FIXME not nice the whole thing here
            devices.add(parts[2]);
          }
        }
        Iterator<String> deviceIter = devices.iterator();
        while (deviceIter.hasNext()) {
          updateItem.addCapability(deviceIter.next());
        }
        update.add(updateItem);
      }
    } else {
      logger.info("Service was null for getKnownAgents");
    }
    return update;
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("agents/{name}/capabilities")
  public Response getCapabilities(@PathParam("name") String agentName) {
    if (service == null) {
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE).build();
    }

    Properties props = service.getAgentCapabilities(agentName);
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    try {
      props.storeToXML(os, "Capabilities for the agent " + agentName);
      logger.debug("Returning capabilities for the agent {}", agentName);

      return Response.ok(os.toString()).build();

    } catch (IOException e) {
      logger.error("An IOException occurred when serializing the agent capabilities");
      return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR).build();
    } catch (NullPointerException e) {
      logger.debug("The agent {} is not registered in the system", agentName);
      return Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).build();
    }
  }

  @POST
  // @Consumes(MediaType.TEXT_XML)
  @Produces(MediaType.TEXT_XML)
  @Path("agents/{name}/capabilities")
  public Response setCapabilities(@PathParam("name") String agentName, InputStream reqBody) {
    if (service == null) {
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE).build();
    }

    Properties caps = new Properties();
    try {
      caps.loadFromXML(reqBody);
      int result = service.setAgentCapabilities(agentName, caps);

      // Prepares the value to return
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      caps.storeToXML(buffer, "Capabilities for the agent " + agentName);

      switch (result) {
      case CaptureAgentStateService.OK:
        logger.debug("{}'s capabilities updated", agentName);
        return Response.ok(buffer.toString()).build();
      case CaptureAgentStateService.BAD_PARAMETER:
        logger.debug("The agent name cannot be blank or null");
        return Response.status(javax.ws.rs.core.Response.Status.BAD_REQUEST).build();
      default:
        logger.error("Unexpected server error in setCapabilities endpoint");
        return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR).build();
      }
    } catch (IOException e) {
      logger.debug("Unexpected I/O Exception when unmarshalling the capabilities: {}", e.getMessage());
      return Response.status(javax.ws.rs.core.Response.Status.BAD_REQUEST).build();
    }
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("recordings/{id}")
  public Response getRecordingState(@PathParam("id") String id) {
    if (service == null) {
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE).build();
    }

    Recording rec = service.getRecordingState(id);

    if (rec != null) {
      logger.debug("Submitting state for recording {}", id);
      return Response.ok(new RecordingStateUpdate(rec)).build();
    } else {
      logger.debug("No such recording: {}", id);
      return Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).build();
    }
  }

  @POST
  @Path("recordings/{id}")
  public Response setRecordingState(@PathParam("id") String id, @FormParam("state") String state) {
    if (service == null) {
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE).build();
    }

    if (service.setRecordingState(id, state)) {
      return Response.ok(id + " set to " + state).build();
    } else {
      return Response.serverError().status(Response.Status.BAD_REQUEST).build();
    }
  }

  @DELETE
  @Path("recordings/{id}")
  public Response removeRecording(@PathParam("id") String id) {
    if (service == null) {
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE).build();
    }

    if (service.removeRecording(id)) {
      return Response.ok(id + " removed").build();
    } else {
      return Response.serverError().status(Response.Status.BAD_REQUEST).build();
    }
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("recordings")
  public List<RecordingStateUpdate> getAllRecordings() {
    LinkedList<RecordingStateUpdate> update = new LinkedList<RecordingStateUpdate>();
    if (service != null) {
      Map<String, Recording> data = service.getKnownRecordings();
      // Run through and build a map of updates (rather than states)
      for (Entry<String, Recording> e : data.entrySet()) {
        update.add(new RecordingStateUpdate(e.getValue()));
      }
    } else {
      logger.info("Service was null for getAllRecordings");
    }
    return update;
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    return docs;
  }

  // Checkstyle:OFF
  protected String docs;
  private String[] notes = {
          "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
          "If the service is down or not working it will return a status 503, this means the the underlying service is not working and is either restarting or has failed",
          "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In other words, there is a bug! You should file an error report with your server logs from the time when the error occurred: <a href=\"https://issues.opencastproject.org\">Opencast Issue Tracker</a>", };

  private String generateDocs(String serviceUrl) {
    DocRestData data = new DocRestData("captureadminservice", "Capture Admin Service", serviceUrl, notes);

    // abstract
    data.setAbstract("This service is a registry of capture agents and their recordings. Please see the <a href='http://wiki.opencastproject.org/confluence/display/open/Capture+Admin+Service'>service contract</a> for further information.");

    // getAgent
    RestEndpoint endpoint = new RestEndpoint("getAgent", RestEndpoint.Method.GET, "/agents/{name}",
            "Return the state of a given capture agent");
    endpoint.addPathParam(new Param("name", Param.Type.STRING, null, "The name of a given capture agent"));
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addStatus(Status.ok("{agentState}"));
    endpoint.addStatus(Status.notFound("The agent {agentName} does not exist"));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, endpoint);

    // setAgent
    endpoint = new RestEndpoint("setAgentState", RestEndpoint.Method.POST, "/agents/{name}",
            "Set the status of a given capture agent");
    endpoint.addPathParam(new Param("name", Param.Type.STRING, null, "The name of a given capture agent"));
    endpoint.addRequiredParam(new Param("state", Param.Type.STRING, null, "The state of the capture agent"));
    endpoint.addFormat(new Format("HTML", null, null));
    endpoint.addStatus(Status.ok("{agentName} set to {state}"));
    endpoint.addStatus(Status.badRequest("{state} is null or empty"));
    endpoint.addStatus(Status.notFound("The agent {agentName} does not exist"));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // removeAgent
    endpoint = new RestEndpoint("removeAgent", RestEndpoint.Method.DELETE, "/agents/{name}",
            "Remove record of a given capture agent");
    endpoint.addPathParam(new Param("name", Param.Type.STRING, null, "The name of a given capture agent"));
    endpoint.addFormat(new Format("HTML", null, null));
    endpoint.addStatus(Status.ok("{agentName} removed"));
    endpoint.addStatus(Status.notFound("The agent {agentname} does not exist"));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // getCapabilities
    endpoint = new RestEndpoint("getAgentCapabilities", RestEndpoint.Method.GET, "/agents/{name}/capabilities",
            "Return the capabilities of a given capture agent");
    endpoint.addPathParam(new Param("name", Param.Type.STRING, null, "The name of a given capture agent"));
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addStatus(Status.ok("An XML representation of the agent capabilities"));
    endpoint.addStatus(Status.notFound("The agent {name} does not exist in the system"));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, endpoint);

    // setCapabilities
    endpoint = new RestEndpoint("setAgentStateCapabilities", RestEndpoint.Method.POST, "/agents/{name}/capabilities",
            "Set the capabilities of a given capture agent, registering it if it does not exist");
    endpoint.addPathParam(new Param("name", Param.Type.STRING, null, "The name of a given capture agent"));
    endpoint.addBodyParam(false, null,
            "An XML representation of the capabilities, as specified in http://java.sun.com/dtd/properties.dtd "
                    + "(friendly names as keys, device locations as their corresponding values)");
    endpoint.addFormat(new Format(Format.XML, "The capabilities that have just been set in the agent",
            "http://java.sun.com/dtd/properties.dtd"));
    endpoint.addStatus(Status.ok("{agentName} set to {state}"));
    endpoint.addStatus(Status.badRequest("The capabilities format is incorrect OR the agent name is blank or null"));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // getKnownAgents
    endpoint = new RestEndpoint("getKnownAgents", RestEndpoint.Method.GET, "/agents",
            "Return all registered capture agents and their state");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addStatus(Status.ok(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, endpoint);

    // getRecordingState
    endpoint = new RestEndpoint("getRecordingState", RestEndpoint.Method.GET, "/recordings/{id}",
            "Return the state of a given recording");
    endpoint.addPathParam(new Param("id", Param.Type.STRING, null, "The ID of a given recording"));
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addStatus(Status.ok(null));
    endpoint.addStatus(Status.notFound("The recording with the specified ID does not exist"));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, endpoint);

    // setRecordingState
    endpoint = new RestEndpoint("setRecordingState", RestEndpoint.Method.POST, "/recordings/{id}",
            "Set the status of a given recording, registering it if it is new");
    endpoint.addPathParam(new Param("id", Param.Type.STRING, null, "The ID of a given recording"));
    endpoint.addRequiredParam(new Param("state", Param.Type.STRING, null, "The state of the recording"));
    endpoint.addFormat(new Format("HTML", null, null));
    endpoint.addStatus(Status.ok("{id} set to {state}"));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // removeRecording
    endpoint = new RestEndpoint("removeRecording", RestEndpoint.Method.DELETE, "/recordings/{id}",
            "Remove record of a given recording");
    endpoint.addPathParam(new Param("id", Param.Type.STRING, null, "The ID of a given recording"));
    endpoint.addFormat(new Format("HTML", null, null));
    endpoint.addStatus(Status.ok("{id} removed"));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // getAllRecordings
    endpoint = new RestEndpoint("getAllRecordings", RestEndpoint.Method.GET, "/recordings",
            "Return all registered recordings and their state");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addStatus(Status.ok(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, endpoint);

    return DocUtil.generate(data);
  }

  // Checkstyle:ON

  public CaptureAgentStateRestService() {
  }
}
