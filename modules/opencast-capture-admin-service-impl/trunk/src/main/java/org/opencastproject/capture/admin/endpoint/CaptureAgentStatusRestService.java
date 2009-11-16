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
import java.util.Map;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.opencastproject.capture.admin.api.CaptureAgentStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FIXME -- Add javadocs
 */
@Path("/")
public class CaptureAgentStatusRestService {
  private static final Logger logger = LoggerFactory.getLogger(CaptureAgentStatusRestService.class);
  private CaptureAgentStatusService service;
  public void setService(CaptureAgentStatusService service) {
    this.service = service;
  }

  public void unsetService(CaptureAgentStatusService service) {
    this.service = null;
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("GetAgentState")
  public String getAgentState(@QueryParam("agentName") String agentName) {
    return service.getAgentState(agentName);
  }

  @POST
  @Path("SetAgentState")
  public void setAgentState(@FormParam("agentName") String agentName, @FormParam("state") String state) {
    service.setAgentState(agentName, state);
  }

  @POST
  @Path("RemoveAgent")
  public void removeAgent(@FormParam("agentName") String agentName) {
    service.removeAgent(agentName);
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("GetKnownAgents")
  public Map<String, String> getKnownAgents() {
    return service.getKnownAgents();
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("GetRecordingState")
  public String getRecordingState(@QueryParam("id") String id) {
    return service.getRecordingState(id);
  }

  @POST
  @Path("SetRecordingState")
  public void setRecordingState(@FormParam("id") String id, @FormParam("state") String state) {
    service.setRecordingState(id, state);
  }

  @POST
  @Path("RemoveRecording")
  public void removeRecording(@FormParam("id") String id) {
    service.removeRecording(id);
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("GetAllRecordingStates")
  public Map<String, String> getAllRecordingStates() {
    return service.getAllRecordingStates();
  }
  
  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    return docs;
  }

  protected final String docs;
  
  public CaptureAgentStatusRestService() {
    String docsFromClassloader = null;
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream("/html/index.html");
      docsFromClassloader = IOUtils.toString(in);
    } catch (IOException e) {
      logger.error("failed to read documentation", e);
      docsFromClassloader = "unable to load documentation for " + CaptureAgentStatusRestService.class.getName();
    } finally {
      IOUtils.closeQuietly(in);
    }
    docs = docsFromClassloader;
  }
}
