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
import org.opencastproject.util.UrlSupport;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowConfiguration;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowDefinitionList;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationDefinition;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstanceList;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowSet;
import org.opencastproject.workflow.api.WorkflowInstance.State;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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

  protected String docs = null;
  protected String serverUrl = UrlSupport.DEFAULT_BASE_URL;

  private WorkflowService service;
  public void setService(WorkflowService service) {
    this.service = service;
  }

  public void unsetService(WorkflowService service) {
    this.service = null;
  }

  public void activate(ComponentContext cc) {
    // Get the configured server URL
    if(cc == null) {
      serverUrl = UrlSupport.DEFAULT_BASE_URL;
    } else {
      String ccServerUrl = cc.getBundleContext().getProperty("serverUrl");
      logger.info("configured server url is " + ccServerUrl);
      if(ccServerUrl == null) {
        serverUrl = UrlSupport.DEFAULT_BASE_URL;
      } else {
        serverUrl = ccServerUrl;
      }
    }

    // Pre-load the documentation
    String docsFromClassloader = null;
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream("/html/index.html");
      docsFromClassloader = IOUtils.toString(in);
      docsFromClassloader = docsFromClassloader.replaceAll("@SERVER_URL@", serverUrl + "/workflow/rest");
      docsFromClassloader = docsFromClassloader.replaceAll("@SAMPLES_URL@", serverUrl + "/workflow/samples");
    } catch (IOException e) {
      logger.error("failed to read documentation", e);
      docsFromClassloader = "unable to load documentation for " + WorkflowRestService.class.getName();
    } finally {
      IOUtils.closeQuietly(in);
    }
    docs = docsFromClassloader;

  }
  
  @SuppressWarnings("unchecked")
  @GET
  @Path("definitions.{output:.*}")
  public Response getWorkflowDefinitions(@PathParam("output") String output) throws Exception {
    WorkflowDefinitionList list = service.listAvailableWorkflowDefinitions();
    if("json".equals(output)) {
      List<JSONObject> jsonDefs = new ArrayList<JSONObject>();
      for(WorkflowDefinition definition : list) {
        jsonDefs.add(getWorkflowDefinitionAsJson(definition));
      }
      JSONObject json = new JSONObject();
      json.put("workflow_definitions", jsonDefs);
      return Response.ok(json.toJSONString()).header("Content-Type", MediaType.APPLICATION_JSON).build();
    } else {
      return Response.ok(WorkflowBuilder.getInstance().toXml(list)).header("Content-Type", MediaType.TEXT_XML).build();
    }
  }

  /**
   * @param definition
   */
  @SuppressWarnings("unchecked")
  protected JSONObject getWorkflowDefinitionAsJson(WorkflowDefinition definition) {
    JSONObject json = new JSONObject();
    json.put("title", definition.getTitle());
    json.put("description", definition.getDescription());
    List<JSONObject> opList = new ArrayList<JSONObject>();
    for(WorkflowOperationDefinition operationDefinition : definition.getOperations()) {
      JSONObject op = new JSONObject();
      op.put("name",operationDefinition.getName());
      op.put("description",operationDefinition.getDescription());
      op.put("exception_handler_workflow",operationDefinition.getExceptionHandlingWorkflow());
      op.put("fail_on_error",operationDefinition.isFailWorkflowOnException());
      opList.add(op);
    }
    json.put("operations", opList);
    return json;
  }

// CHECKSTYLE:OFF (The number of method parameters is large because we need to handle many potential query parameters)
  @SuppressWarnings("unchecked")
  @GET
  @Path("instances.{output:.*}")
  public Response getWorkflows(
          @QueryParam("state") String state,
          @QueryParam("q") String text,
          @QueryParam("episode") String episodeId,
          @QueryParam("series") String seriesId,
          @QueryParam("mp") String mediapackageId,
          @QueryParam("op") String currentOperation,
          @QueryParam("startPage") int startPage,
          @QueryParam("count") int count,
          @PathParam("output") String output) throws Exception {
// CHECKSTYLE:ON
    if(count < 1 || count > MAX_LIMIT) count = DEFAULT_LIMIT;
    if(startPage == 0) startPage = 1;
    
    WorkflowQuery q = service.newWorkflowQuery();
    q.withCount(count);
    q.withStartPage(startPage);
    if(state != null) q.withState(State.valueOf(state.toUpperCase()));
    if(text != null) q.withText(text);
    if(episodeId != null) q.withEpisode(episodeId);
    if(seriesId != null) q.withSeries(seriesId);
    if(mediapackageId != null) q.withMediaPackage(mediapackageId);
    if(currentOperation != null) q.withCurrentOperation(currentOperation);
    WorkflowSet set = service.getWorkflowInstances(q);
    
    if("json".equals(output)) {
      JSONArray json = new JSONArray();
      for(WorkflowInstance workflow : set.getItems()) {
        json.add(getWorkflowInstanceAsJson(workflow, false));
      }
      return Response.ok(json.toJSONString()).header("Content-Type", MediaType.APPLICATION_JSON).build();
    } else {
      return Response.ok(WorkflowBuilder.getInstance().toXml(set)).header("Content-Type", MediaType.TEXT_XML).build();
    }
  }

  @GET
  @Path("instance/{id}.{output:.*}")
  public Response getWorkflow(
          @PathParam("id") String id,
          @PathParam("output") String output) throws Exception {
    WorkflowInstance instance = service.getWorkflowById(id);
    if("json".equals(output)) {
      return Response.ok(getWorkflowInstanceAsJson(instance, true).toString()).header("Content-Type", MediaType.APPLICATION_JSON).build();
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

  @SuppressWarnings("unchecked")
  protected JSONObject getWorkflowInstanceAsJson(WorkflowInstance workflow, boolean includeDublinCoreFields) throws Exception {
    String mediaPackageTitle = getDublinCoreProperty(getDublinCore(workflow.getSourceMediaPackage()),
            DublinCoreCatalog.PROPERTY_TITLE);

    JSONObject jsInstance = new JSONObject();
    jsInstance.put("workflow_id", workflow.getId());
    jsInstance.put("workflow_title", workflow.getTitle());
    WorkflowOperationInstance opInstance = workflow.getCurrentOperation();
    if(opInstance != null) {
      jsInstance.put("workflow_current_operation", opInstance.getName());
    }
    jsInstance.put("workflow_state", workflow.getState().name().toLowerCase());
    Set<WorkflowConfiguration> configs = workflow.getConfigurations();
    jsInstance.put("configuration", getConfigsAsJson(configs));
    WorkflowOperationInstanceList operations = workflow.getWorkflowOperationInstanceList();
    jsInstance.put("operations", getOperationsAsJson(operations));
    if(includeDublinCoreFields) {
      DublinCoreCatalog dc = getDublinCore(workflow.getCurrentMediaPackage());
      List<EName> props = new ArrayList<EName>(dc.getProperties());
      for(int i=0; i<props.size(); i++) {
        jsInstance.put("mediapackage_" + props.get(i).getLocalName().toLowerCase(), dc.getFirst(props.get(i)));
      }
    } else {
      jsInstance.put("mediapackage_title", mediaPackageTitle);
    }
    return jsInstance;
  }

  @SuppressWarnings("unchecked")
  protected JSONArray getOperationsAsJson(WorkflowOperationInstanceList operations) {
    JSONArray jsonArray = new JSONArray();
    for(WorkflowOperationInstance op : operations) {
      JSONObject jsOp = new JSONObject();
      jsOp.put("name", op.getName());
      jsOp.put("description", op.getDescription());
      jsOp.put("state", op.getState().name().toLowerCase());
      jsOp.put("configurations", getConfigsAsJson(op.getConfigurations()));
      jsonArray.add(jsOp);
    }
    return jsonArray;
  }

  @SuppressWarnings("unchecked")
  protected JSONArray getConfigsAsJson(Set<WorkflowConfiguration> configs) {
    JSONArray json = new JSONArray();
    if(configs != null) {
      for(WorkflowConfiguration config : configs) {
        JSONObject jsConfig = new JSONObject();
        jsConfig.put(config.getKey(), config.getValue());
        json.add(jsConfig);
      }
    }
    return json;
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
    if(docs == null) return "documentation not available";
    return docs;
  }
}
