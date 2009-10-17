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
package org.opencastproject.workflow.endpoint;

import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.jaxb.MediapackageType;
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowInstanceListImpl;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowInstance.State;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * A REST endpoint for the {@link WorkflowService}
 */
@Path("/")
public class WorkflowRestService {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowRestService.class);
  private WorkflowService service;
  public void setService(WorkflowService service) {
    this.service = service;
  }

  public void unsetService(WorkflowService service) {
    this.service = null;
  }

  @GET
  @Path("instances/{state}")
  @Produces(MediaType.TEXT_XML)
  public WorkflowInstanceListImpl fetchAllJaxbWorkflowInstances(@PathParam("state") String state) throws Exception {
    WorkflowInstanceListImpl list = new WorkflowInstanceListImpl();
    for(WorkflowInstance instance : service.getWorkflowInstances(State.valueOf(state))) {
      list.getWorkflowInstance().add((WorkflowInstanceImpl)instance);
    }
    return list;
  }

  @GET
  @Path("instance/{id}")
  @Produces(MediaType.TEXT_XML)
  public WorkflowInstanceImpl getJaxbWorkflowInstance(@PathParam("id") String id) throws Exception {
    return (WorkflowInstanceImpl)service.getWorkflowInstance(id);
  }

  @POST
  @Path("start")
  @Produces(MediaType.TEXT_XML)
  public WorkflowInstanceImpl start(
          @FormParam("definition") WorkflowDefinitionImpl workflowDefinition,
          @FormParam("mediapackage") MediapackageType mediaPackage,
          @FormParam("properties") LocalHashMap localMap) {
    Map<String, String> properties = localMap.getMap();
    try {
      InputStream in = IOUtils.toInputStream(mediaPackage.toXml());
      MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().loadFromManifest(in);
      WorkflowInstance instance = service.start(workflowDefinition, mp, properties);
      return (WorkflowInstanceImpl)instance;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  // FIXME Using GET for testing purposes only.
  
  @GET
  @Path("stop/{id}")
  @Produces(MediaType.TEXT_PLAIN)
  public Response stop(@PathParam("id") String workflowInstanceId) {
    service.stop(workflowInstanceId);
    return Response.ok("stopped " + workflowInstanceId).build();
  }

  // FIXME Using GET for testing purposes only.

  @GET
  @Path("suspend/{id}")
  @Produces(MediaType.TEXT_PLAIN)
  public Response suspend(@PathParam("id") String workflowInstanceId) {
    service.suspend(workflowInstanceId);
    return Response.ok("suspended " + workflowInstanceId).build();
  }

  // FIXME Using GET for testing purposes only.

  @GET
  @Path("resume/{id}")
  @Produces(MediaType.TEXT_PLAIN)
  public Response resume(@PathParam("id") String workflowInstanceId) {
    service.resume(workflowInstanceId);
    return Response.ok("resumed " + workflowInstanceId).build();
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    return docs;
  }

  protected final String docs;
  
  public WorkflowRestService() {
    // Pre-load the documentation
    String docsFromClassloader = null;
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream("/html/index.html");
      docsFromClassloader = IOUtils.toString(in);
    } catch (IOException e) {
      logger.error("failed to read documentation", e);
      docsFromClassloader = "unable to load documentation for " + WorkflowRestService.class.getName();
    } finally {
      IOUtils.closeQuietly(in);
    }
    docs = docsFromClassloader;
  }
}
