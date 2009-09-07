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

import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowService;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.Consumes;
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
  @Path("defs")
  @Produces(MediaType.TEXT_XML)
  public WorkflowDefinitionJaxbImplList fetchAllJaxbWorkflowDefinitions() {
    WorkflowDefinitionJaxbImplList list = new WorkflowDefinitionJaxbImplList();
    for(WorkflowDefinition def : service.fetchAllWorkflowDefinitions()) {
      list.getWorkflowDefinition().add(new WorkflowDefinitionJaxbImpl(def));
    }
    return list;
  }

  @GET
  @Path("def/{id}")
  @Produces(MediaType.TEXT_XML)
  public WorkflowDefinitionJaxbImpl getJaxbWorkflowDefinition(@PathParam("id") String id) {
    WorkflowDefinition entity = service.getWorkflowDefinition(id);
    return new WorkflowDefinitionJaxbImpl(entity);
  }

  @POST
  @Path("def/{id}")
  @Consumes(MediaType.TEXT_XML)
  public Response registerJaxbWorkflowDefinition(WorkflowDefinitionJaxbImpl workflowDefinition) {
    service.registerWorkflowDefinition(workflowDefinition.getEntity());
    return Response.ok(workflowDefinition).build();
  }

  @GET
  @Path("instances/{id}")
  @Produces(MediaType.TEXT_XML)
  public WorkflowInstanceJaxbImplList fetchAllJaxbWorkflowInstances(@PathParam("id") String workflowDefinitionId) {
    WorkflowInstanceJaxbImplList list = new WorkflowInstanceJaxbImplList();
    for(WorkflowInstance instance : service.fetchAllWorkflowInstances(workflowDefinitionId)) {
      list.getWorkflowInstance().add(new WorkflowInstanceJaxbImpl(instance));
    }
    return list;
  }

  @GET
  @Path("instance/{id}")
  @Produces(MediaType.TEXT_XML)
  public WorkflowInstanceJaxbImpl getJaxbWorkflowInstance(@PathParam("id") String id) {
    return new WorkflowInstanceJaxbImpl(service.getWorkflowInstance(id));
  }

  @POST
  @Path("instance/{id}")
  @Produces(MediaType.TEXT_XML)
  public Response saveJaxbWorkflowInstance(WorkflowInstanceJaxbImpl workflowInstance) {
    service.saveWorkflowInstance(workflowInstance.getEntity());
    return Response.ok(workflowInstance).build();
  }

  
  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    return docs;
  }

  protected final String docs;
  
  public WorkflowRestService() {
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
