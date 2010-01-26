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
import org.opencastproject.media.mediapackage.MediaPackageImpl;
import org.opencastproject.media.mediapackage.MediaPackageReferenceImpl;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;
import org.opencastproject.util.doc.Param.Type;
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
import javax.ws.rs.core.Response.Status;

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
      logger.info("configured server url is {}", ccServerUrl);
      if(ccServerUrl == null) {
        serverUrl = UrlSupport.DEFAULT_BASE_URL;
      } else {
        serverUrl = ccServerUrl;
      }
    }
    docs = generateDocs();
  }

  protected String generateDocs() {
    DocRestData data = new DocRestData("Workflow", "Workflow Service", "/workflow/rest", new String[] {"$Rev$"});
    // Workflow Definitions
    RestEndpoint defsEndpoint = new RestEndpoint("defs", RestEndpoint.Method.GET, "/definitions.{format}", "Returns all available workflow definitions");
    defsEndpoint.addFormat(new Format("xml", null, null));
    defsEndpoint.addFormat(new Format("json", null, null));
    defsEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, results returned"));
    defsEndpoint.addPathParam(new Param("format", Type.STRING, "xml", "The format of the results, xml or json"));
    defsEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, defsEndpoint);
    
    // Workflow Instances
    RestEndpoint instancesEndpoint = new RestEndpoint("instances", RestEndpoint.Method.GET, "/instances.{format}", "Returns workflow instances matching the query parameters");
    instancesEndpoint.addFormat(new Format("xml", null, null));
    instancesEndpoint.addFormat(new Format("json", null, null));
    instancesEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, results returned"));
    instancesEndpoint.addPathParam(new Param("format", Type.STRING, "xml", "The format of the results, xml or json"));
    instancesEndpoint.addOptionalParam(new Param("state", Type.STRING, "succeeded", "Limit the resulting workflow instances to those currently in this state"));
    instancesEndpoint.addOptionalParam(new Param("q", Type.STRING, "climate", "Limit the resulting workflow instances to those with this string somewhere in its metadata catalogs"));
    instancesEndpoint.addOptionalParam(new Param("count", Type.STRING, "20", "The number of results to return per page (max is 100)"));
    instancesEndpoint.addOptionalParam(new Param("startPage", Type.STRING, "1", "The page of results to display"));
    instancesEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, instancesEndpoint);

    // Workflow Instance
    RestEndpoint instanceEndpoint = new RestEndpoint("instance", RestEndpoint.Method.GET, "/instance/{id}.{format}", "Returns a single workflow instance");
    instanceEndpoint.addFormat(new Format("xml", null, null));
    instanceEndpoint.addFormat(new Format("json", null, null));
    instanceEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, results returned"));
    instanceEndpoint.addStatus(org.opencastproject.util.doc.Status.NOT_FOUND("A workflow instance with this ID was not found"));
    instanceEndpoint.addPathParam(new Param("id", Type.STRING, null, "The ID of the workflow instance"));
    instanceEndpoint.addPathParam(new Param("format", Type.STRING, "xml", "The format of the results, xml or json"));
    instanceEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, instanceEndpoint);

    // Start a new Workflow Instance
    RestEndpoint startEndpoint = new RestEndpoint("start", RestEndpoint.Method.POST, "/start", "Start a new workflow instance");
    startEndpoint.addFormat(new Format("xml", null, null));
    startEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, workflow running or queued"));
    startEndpoint.addRequiredParam(new Param("mediapackage", Type.TEXT, generateMediaPackage(), "The media package upon which to perform the workflow"));
    startEndpoint.addRequiredParam(new Param("definition", Type.TEXT, generateWorkflowDefinition(), "The workflow definition"));
    startEndpoint.addRequiredParam(new Param("properties", Type.TEXT, "encode=true\nflash.http=true\nyouTube=true\nitunes=false", "The properties to set for this workflow instance"));
    startEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, startEndpoint);

    // Stop a Workflow Instance
    RestEndpoint stopEndpoint = new RestEndpoint("stop", RestEndpoint.Method.GET, "/stop/{id}", "Stop a running workflow instance (currently a get, but should probably be a POST or even DELETE?)");
    stopEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, workflow stopped"));
    stopEndpoint.addStatus(org.opencastproject.util.doc.Status.NOT_FOUND("A workflow instance with this ID was not found"));
    stopEndpoint.addPathParam(new Param("id", Type.STRING, null, "The ID of the workflow instance"));
    stopEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, stopEndpoint);

    // Suspend a Workflow Instance
    RestEndpoint suspendEndpoint = new RestEndpoint("suspend", RestEndpoint.Method.GET, "/suspend/{id}", "Suspends a running workflow instance (currently a get, but should probably be a POST)");
    suspendEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, workflow suspended"));
    suspendEndpoint.addStatus(org.opencastproject.util.doc.Status.NOT_FOUND("A workflow instance with this ID was not found"));
    suspendEndpoint.addPathParam(new Param("id", Type.STRING, null, "The ID of the workflow instance"));
    suspendEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, suspendEndpoint);

    // Resume a Workflow Instance
    RestEndpoint resumeEndpoint = new RestEndpoint("resume", RestEndpoint.Method.GET, "/resume/{id}", "Resumes a suspended workflow instance (currently a get, but should probably be a POST)");
    resumeEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, suspended workflow has now resumed"));
    resumeEndpoint.addStatus(org.opencastproject.util.doc.Status.NOT_FOUND("A suspended workflow instance with this ID was not found"));
    resumeEndpoint.addPathParam(new Param("id", Type.STRING, null, "The ID of the workflow instance"));
    resumeEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, resumeEndpoint);

    return DocUtil.generate(data);
  }
  
  protected String generateMediaPackage() {
    String samplesUrl = serverUrl + "/workflow/samples";
    
    return "<ns2:mediapackage xmlns:ns2=\"http://mediapackage.opencastproject.org\" start=\"2007-12-05T13:40:00\" duration=\"1004400000\">\n" +
    "  <media>\n" +
    "    <track id=\"track-1\" type=\"presenter/source\">\n" +
    "      <mimetype>audio/mp3</mimetype>\n" +
    "      <url>" + samplesUrl + "/audio.mp3</url>\n" +
    "      <checksum type=\"md5\">950f9fa49caa8f1c5bbc36892f6fd062</checksum>\n" +
    "      <duration>10472</duration>\n" +
    "      <audio>\n" +
    "        <channels>2</channels>\n" +
    "        <bitdepth>0</bitdepth>\n" +
    "        <bitrate>128004.0</bitrate>\n" +
    "        <samplingrate>44100</samplingrate>\n" +
    "      </audio>\n" +
    "    </track>\n" +
    "    <track id=\"track-2\" type=\"presenter/source\">\n" +
    "      <mimetype>video/quicktime</mimetype>\n" +
    "      <url>" + samplesUrl + "/camera.mpg</url>\n" +
    "      <checksum type=\"md5\">43b7d843b02c4a429b2f547a4f230d31</checksum>\n" +
    "      <duration>14546</duration>\n" +
    "      <video>\n" +
    "        <device type=\"UFG03\" version=\"30112007\" vendor=\"Unigraf\" />\n" +
    "        <encoder type=\"H.264\" version=\"7.4\" vendor=\"Apple Inc\" />\n" +
    "        <resolution>640x480</resolution>\n" +
    "        <scanType type=\"progressive\" />\n" +
    "        <bitrate>540520</bitrate>\n" +
    "        <frameRate>2</frameRate>\n" +
    "      </video>\n" +
    "    </track>\n" +
    "  </media>\n" +
    "  <metadata>\n" +
    "    <catalog id=\"catalog-1\" type=\"metadata/dublincore\">\n" +
    "      <mimetype>text/xml</mimetype>\n" +
    "      <url>" + samplesUrl + "/dc-1.xml</url>\n" +
    "      <checksum type=\"md5\">20e466615251074e127a1627fd0dae3e</checksum>\n" +
    "    </catalog>\n" +
    "  </metadata>\n" +
    "</ns2:mediapackage>";
  }
  
  protected String generateWorkflowDefinition() {
    try {
      return IOUtils.toString(getClass().getResourceAsStream("/sample/compose-distribute-publish.xml"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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
    if(instance == null) return Response.status(Status.NOT_FOUND).entity("Workflow instance " + id + " does not exist").build();
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
          @FormParam("mediapackage") MediaPackageImpl mp,
          @FormParam("properties") LocalHashMap localMap) {
    Map<String, String> properties = localMap.getMap();
    return (WorkflowInstanceImpl)service.start(workflowDefinition, mp, properties);
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
    MediaPackage mp = workflow.getCurrentMediaPackage();
    DublinCoreCatalog dc = getDublinCore(mp);
    String mediaPackageTitle = null;
    if(dc == null) {
      mediaPackageTitle = mp.getIdentifier() + "(unknown)";
    } else {
      mediaPackageTitle = getDublinCoreProperty(dc, DublinCoreCatalog.PROPERTY_TITLE);
    }

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
    if(includeDublinCoreFields && dc != null) {
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
