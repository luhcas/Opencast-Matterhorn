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

import org.opencastproject.media.mediapackage.Catalog;
import org.opencastproject.media.mediapackage.DublinCoreCatalog;
import org.opencastproject.media.mediapackage.EName;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageReferenceImpl;
import org.opencastproject.media.mediapackage.jaxb.MediapackageType;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowConfiguration;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowDefinitionList;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowInstanceListImpl;
import org.opencastproject.workflow.api.WorkflowOperationDefinition;
import org.opencastproject.workflow.api.WorkflowOperationDefinitionList;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstanceList;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowSet;
import org.opencastproject.workflow.api.WorkflowInstance.State;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * A REST endpoint for the {@link WorkflowService}
 */
@Path("/")
public class WorkflowRestService {
  private static final int DEFAULT_LIMIT = 20;
  private static final int MAX_LIMIT = 100;
  
  private static final Logger logger = LoggerFactory.getLogger(WorkflowRestService.class);
  private WorkflowService service;
  public void setService(WorkflowService service) {
    this.service = service;
  }

  public void unsetService(WorkflowService service) {
    this.service = null;
  }

  @GET
  @Path("definitions.{output:.*}")
  public Response getWorkflowDefinitions(@PathParam("output") String output) throws Exception {
    WorkflowDefinitionList list = service.listAvailableWorkflowDefinitions();
    if("json".equals(output)) {
      StringBuilder sb = new StringBuilder("{\"workflow_definitions\" : [\n");
      for(int i=0; i < list.size(); i++) {
        WorkflowDefinition definition = list.get(i);
        appendWorkflowDefinition(sb, definition);
        if(i < list.size() - 1) sb.append(",");
        sb.append("\n");
      }
      sb.append("]}");
      return Response.ok(sb.toString()).header("Content-Type", MediaType.APPLICATION_JSON).build();
    } else {
      return Response.ok(WorkflowBuilder.getInstance().toXml(list)).header("Content-Type", MediaType.TEXT_XML).build();
    }
  }

  /**
   * @param sb
   * @param definition
   */
  protected void appendWorkflowDefinition(StringBuilder sb, WorkflowDefinition definition) {
    sb.append(" { \"title\" : \"");
    sb.append(definition.getTitle());
    sb.append("\",\n");

    sb.append("   \"description\" : \"");
    sb.append(definition.getDescription());
    sb.append("\",\n");
    
    sb.append("   \"operations\" : [\n");

    WorkflowOperationDefinitionList operations = definition.getOperations();
    for(int i=0; i < operations.size(); i++) {
      WorkflowOperationDefinition op = operations.get(i);
      sb.append("    {\"name\" : \"");
      sb.append(op.getName());
      sb.append("\", \"description\" : \"");
      sb.append(op.getDescription());
      sb.append("\", \"exception_handler_workflow\" : ");
      String exceptionWorkflow = op.getExceptionHandlingWorkflow();
      if(exceptionWorkflow == null) {
        sb.append("null");
      } else {
        sb.append("\"");
        sb.append(exceptionWorkflow);
        sb.append("\"");
      }
      sb.append(", \"fail_on_error\" : ");
      sb.append(op.isFailWorkflowOnException());
      sb.append("}");
      if(i < operations.size() - 1) sb.append(",");
      sb.append("\n");
    }
    
    sb.append("   ]\n");
    sb.append(" }");
  }

  
  @GET
  @Path("instances.{output:.*}")
  public Response getWorkflows(
          @QueryParam("state") String state,
          @QueryParam("q") String text,
          @QueryParam("startPage") int offset,
          @QueryParam("count") int limit,
          @PathParam("output") String output) throws Exception {
    if(limit == 0 || limit > MAX_LIMIT) limit = DEFAULT_LIMIT;
    if(offset >0) offset--; // The service is zero based
    WorkflowSet set = null;
    if(text == null && state == null) {
      set = service.getWorkflowsByDate(offset, limit);
    } else if(text == null) {
      set = service.getWorkflowsInState(State.valueOf(state.toUpperCase()), offset, limit);
    } else if(state == null) {
      set = service.getWorkflowsByText(text, offset, limit);
    } else {
      set = service.getWorkflowsByTextAndState(State.valueOf(state.toUpperCase()), text, offset, limit);
    }
    
    if("json".equals(output)) {
      StringBuilder sb = new StringBuilder("{\"workflows\" : [\n");
      for(int i=0; i < set.getItems().length; i++) {
        WorkflowInstance workflow = set.getItems()[i];
        appendWorkflowInstance(sb, workflow, false);
        if(i < set.getItems().length - 1) sb.append(",");
        sb.append("\n");
      }
      sb.append("]}");
      return Response.ok(sb.toString()).header("Content-Type", MediaType.APPLICATION_JSON).build();
    } else {
      WorkflowInstanceListImpl list = new WorkflowInstanceListImpl();
      for(WorkflowInstance instance : set.getItems()) {
        list.getWorkflowInstance().add((WorkflowInstanceImpl)instance);
      }
      return Response.ok(WorkflowBuilder.getInstance().toXml(list))
        .header("Content-Type", MediaType.TEXT_XML).build();
    }
  }

  @GET
  @Path("instance/{id}.{output:.*}")
  public Response getWorkflow(
          @PathParam("id") String id,
          @PathParam("output") String output) throws Exception {
    WorkflowInstance instance = service.getWorkflowById(id);
    if("json".equals(output)) {
      StringBuilder sb = new StringBuilder();
      appendWorkflowInstance(sb, instance, true);
      return Response.ok(sb.toString()).header("Content-Type", MediaType.APPLICATION_JSON).build();
    } else {
      return Response.ok(WorkflowBuilder.getInstance().toXml(instance))
        .header("Content-Type", MediaType.TEXT_XML).build();
    }
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

  protected void appendWorkflowInstance(StringBuilder sb, WorkflowInstance workflow, boolean includeDublinCoreFields) {
    String mediaPackageTitle = getDublinCoreProperty(getDublinCore(workflow.getSourceMediaPackage()),
            DublinCoreCatalog.PROPERTY_TITLE);

    sb.append(" { \"workflow_id\" : \"");
    sb.append(workflow.getId());
    sb.append("\",\n");

    sb.append("   \"workflow_title\" : \"");
    sb.append(workflow.getTitle());
    sb.append("\",\n");
    
    sb.append("   \"workflow_current_operation\" : \"");
    sb.append(workflow.getCurrentOperation().getName());
    sb.append("\",\n");

    sb.append("   \"workflow_state\" : \"");
    sb.append(workflow.getState().name().toLowerCase());
    sb.append("\",\n");

    Set<WorkflowConfiguration> configs = workflow.getConfigurations();
    appendConfigs(sb, configs, true);
    sb.append("   \"workflow_operations\" : [\n");
    WorkflowOperationInstanceList operations = workflow.getWorkflowOperationInstanceList();
    for(int i=0; i < operations.size(); i++) {
      WorkflowOperationInstance op = operations.get(i);
      sb.append("     {\"name\" : \"");
      sb.append(op.getName());
      sb.append("\", \"description\" : \"");
      sb.append(op.getDescription());
      sb.append("\", \"state\" : \"");
      sb.append(op.getState().name().toLowerCase());
      sb.append("\"}");
      if(i < operations.size() - 1) sb.append(",");
      sb.append("\n");
    }
    sb.append("]\n");

    if(includeDublinCoreFields) {
      DublinCoreCatalog dc = getDublinCore(workflow.getSourceMediaPackage());
      List<EName> props = new ArrayList<EName>(dc.getProperties());
      for(int i=0; i<props.size(); i++) {
        sb.append(",\n");
        sb.append("   \"mediapackage_" + props.get(i).getLocalName().toLowerCase() + "\" : ");
        String value = dc.getFirst(props.get(i));
        if(value == null) {
          sb.append("null");
        } else {
          sb.append("\"");
          sb.append(value);
          sb.append("\"");
        }
      }
    } else {
      sb.append(",\n   \"mediapackage_title\" : \"");
      sb.append(mediaPackageTitle);
      sb.append("\"");
    }

    sb.append("\n }");
  }

  protected void appendConfigs(StringBuilder sb, Set<WorkflowConfiguration> configs, boolean moreElements) {
    if(configs != null) {
      sb.append("   \"configuration\" : [\n");
      for(Iterator<WorkflowConfiguration> iter = configs.iterator(); iter.hasNext();) {
        WorkflowConfiguration config = iter.next();
        sb.append("     {\"");
        sb.append(config.getKey());
        sb.append("\" : \"");
        sb.append(config.getValue());
        sb.append("\"}");
        if(iter.hasNext()) sb.append(",");
        sb.append("\n");
      }
      sb.append("   ]");
    }
    if(moreElements) sb.append(",\n");
  }

  protected DublinCoreCatalog getDublinCore(MediaPackage mediaPackage) {
    Catalog[] dcCatalogs = mediaPackage.getCatalogs(DublinCoreCatalog.FLAVOR, MediaPackageReferenceImpl.ANY_MEDIAPACKAGE);
    if(dcCatalogs.length == 0) return null;
    return (DublinCoreCatalog)dcCatalogs[0];
  }

  protected String getDublinCoreProperty(DublinCoreCatalog catalog, EName property) {
    if(catalog == null) return null;
    return catalog.getFirst(property, DublinCoreCatalog.LANGUAGE_ANY);
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
