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
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowDefinitionList;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowInstanceListImpl;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowSet;
import org.opencastproject.workflow.api.WorkflowInstance.State;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
  private static final Logger logger = LoggerFactory.getLogger(WorkflowRestService.class);
  private WorkflowService service;
  public void setService(WorkflowService service) {
    this.service = service;
  }

  public void unsetService(WorkflowService service) {
    this.service = null;
  }

  @GET
  @Path("definitions")
  @Produces(MediaType.TEXT_XML)
  public String getWorkflowDefinitions() {
    // FIXME: For some reason, we can't return the object here.
    // "[JAXRSUtils] WARN - WebApplicationException has been caught : no cause is available"
    WorkflowDefinitionList list = service.listAvailableWorkflowDefinitions();
    try {
      return WorkflowBuilder.getInstance().toXml(list);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @GET
  @Path("instances/{state}")
  @Produces(MediaType.TEXT_XML)
  public WorkflowInstanceListImpl getWorkflowsInState(
          @PathParam("state") String state,
          @QueryParam("startPage") int offset,
          @QueryParam("count") int limit) throws Exception {
    WorkflowInstanceListImpl list = new WorkflowInstanceListImpl();
    for(WorkflowInstance instance : service.getWorkflowsInState(State.valueOf(state.toUpperCase()), offset, limit).getItems()) {
      list.getWorkflowInstance().add((WorkflowInstanceImpl)instance);
    }
    return list;
  }

  @GET
  @Path("instances")
  @Produces(MediaType.TEXT_XML)
  public WorkflowInstanceListImpl getWorkflowsByText(
          @QueryParam("q") String text,
          @QueryParam("startPage") int offset,
          @QueryParam("count") int limit) throws Exception {
    WorkflowInstanceListImpl list = new WorkflowInstanceListImpl();
    for(WorkflowInstance instance : service.getWorkflowsByText(text, offset, limit).getItems()) {
      list.getWorkflowInstance().add((WorkflowInstanceImpl)instance);
    }
    return list;
  }

  @GET
  @Path("instance/{id}")
  @Produces(MediaType.TEXT_XML)
  public WorkflowInstanceImpl getWorkflowById(@PathParam("id") String id) throws Exception {
    return (WorkflowInstanceImpl)service.getWorkflowById(id);
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
  @Path("allinstances")
  @Produces(MediaType.APPLICATION_JSON)
  public String getWorkflowsAsJson(@QueryParam("offset") int offset, @QueryParam("limit") int limit) {
    WorkflowSet set = service.getWorkflowsByDate(offset, limit);
    StringBuilder sb = new StringBuilder("{\"workflows\" : [\n");
    for(int i=0; i < set.getItems().length; i++) {
      WorkflowInstance workflow = set.getItems()[i];
      appendWorkflow(sb, workflow, false);
      if(i < set.getItems().length - 1) sb.append(",");
      sb.append("\n");
    }
    sb.append("]}");
    return sb.toString();
  }

  @GET
  @Path("instanceinfo/{workflowId}")
  @Produces(MediaType.APPLICATION_JSON)
  public String getWorkflowAsJson(@PathParam("workflowId") String workflowId) {
    WorkflowInstance workflow = service.getWorkflowById(workflowId);
    StringBuilder sb = new StringBuilder();
    appendWorkflow(sb, workflow, true);
    return sb.toString();
  }

  protected void appendWorkflow(StringBuilder sb, WorkflowInstance workflow, boolean includeDublinCoreFields) {
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
    sb.append("\"");

    if(includeDublinCoreFields) {
      DublinCoreCatalog dc = getDublinCore(workflow.getSourceMediaPackage());
      List<EName> props = new ArrayList<EName>(dc.getProperties());
      for(int i=0; i<props.size(); i++) {
        sb.append(",\n");
        sb.append("   \"mediapackage_" + props.get(i).getLocalName().toLowerCase() + "\" : \"");
        sb.append(dc.getFirst(props.get(i)));
        sb.append("\"");
      }
    } else {
      sb.append(",\n   \"mediapackage_title\" : \"");
      sb.append(mediaPackageTitle);
      sb.append("\"");
    }

    sb.append("\n }");
  }

  private DublinCoreCatalog getDublinCore(MediaPackage mediaPackage) {
    Catalog[] dcCatalogs = mediaPackage.getCatalogs(DublinCoreCatalog.FLAVOR, MediaPackageReferenceImpl.ANY_MEDIAPACKAGE);
    if(dcCatalogs.length == 0) return null;
    return (DublinCoreCatalog)dcCatalogs[0];
  }

  private String getDublinCoreProperty(DublinCoreCatalog catalog, EName property) {
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
