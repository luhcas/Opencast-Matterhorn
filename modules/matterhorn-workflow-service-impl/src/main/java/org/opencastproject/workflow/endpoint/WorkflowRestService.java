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
package org.opencastproject.workflow.endpoint;

import org.opencastproject.mediapackage.MediaPackageImpl;
import org.opencastproject.rest.RestPublisher;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.Param.Type;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;
import org.opencastproject.workflow.api.Configurable;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationDefinition;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowSet;
import org.opencastproject.workflow.api.WorkflowStatistics;
import org.opencastproject.workflow.impl.WorkflowServiceImpl;
import org.opencastproject.workflow.impl.WorkflowServiceImpl.HandlerRegistration;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * A REST endpoint for the {@link WorkflowService}
 */
@Path("/")
public class WorkflowRestService {
  /** The default number of results returned */
  private static final int DEFAULT_LIMIT = 20;

  /** The maximum number of results returned */
  private static final int MAX_LIMIT = 100;

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(WorkflowRestService.class);

  /** The documentation for this rest endpoint */
  protected String docs = null;

  /** The default server URL */
  protected String serverUrl = UrlSupport.DEFAULT_BASE_URL;

  /** The default service URL */
  protected String serviceUrl = serverUrl + "/workflow/rest";

  /** The workflow service instance */
  private WorkflowService service;

  /**
   * Sets the workflow service
   * 
   * @param service
   *          the workflow service instance
   */
  public void setService(WorkflowService service) {
    this.service = service;
  }

  /**
   * OSGI callback for component activation
   * 
   * @param cc
   *          the OSGI declarative services component context
   */
  public void activate(ComponentContext cc) {
    // Get the configured server URL
    if (cc == null) {
      serverUrl = UrlSupport.DEFAULT_BASE_URL;
    } else {
      String ccServerUrl = cc.getBundleContext().getProperty("org.opencastproject.server.url");
      logger.info("configured server url is {}", ccServerUrl);
      if (ccServerUrl == null) {
        serverUrl = UrlSupport.DEFAULT_BASE_URL;
      } else {
        serverUrl = ccServerUrl;
      }
      serviceUrl = (String) cc.getProperties().get(RestPublisher.SERVICE_PATH_PROPERTY);
      docs = generateDocs();
    }
  }

  /**
   * Generates the REST documentation for this service.
   * 
   * @return the REST documentation
   */
  protected String generateDocs() {
    DocRestData data = new DocRestData("Workflow", "Workflow Service", serviceUrl, new String[] { "$Rev$" });

    // abstract
    data.setAbstract("This service lists available workflows and starts, stops, suspends and resumes workflow instances.");
    // Workflow Definitions
    RestEndpoint defsEndpoint = new RestEndpoint("defs", RestEndpoint.Method.GET, "/definitions.{format}",
            "List all available workflow definitions");
    defsEndpoint.addFormat(new Format("xml", null, null));
    defsEndpoint.addFormat(new Format("json", null, null));
    defsEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("Valid request, results returned"));
    defsEndpoint.addPathParam(new Param("format", Type.STRING, "xml", "The format of the results: xml or json"));
    defsEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, defsEndpoint);

    // Workflow Definition by id
    RestEndpoint defEndpoint = new RestEndpoint("defs", RestEndpoint.Method.GET, "/definition/{id}.{format}",
            "List all available workflow definitions");
    defEndpoint.addFormat(new Format("xml", null, null));
    defEndpoint.addFormat(new Format("json", null, null));
    defEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("Valid request, results returned"));
    defEndpoint.addPathParam(new Param("id", Type.STRING, "full", "The workflow definition identifier"));
    defEndpoint.addPathParam(new Param("format", Type.STRING, "xml", "The format of the results: xml or json"));
    defEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, defEndpoint);

    // Workflow Instances
    RestEndpoint instancesEndpoint = new RestEndpoint("instances", RestEndpoint.Method.GET, "/instances.{format}",
            "List all workflow instances matching the query parameters");
    instancesEndpoint.addFormat(new Format("xml", null, null));
    instancesEndpoint.addFormat(new Format("json", null, null));
    instancesEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("Valid request, results returned"));
    instancesEndpoint.addPathParam(new Param("format", Type.STRING, "xml", "The format of the results: xml or json"));
    instancesEndpoint.addOptionalParam(new Param("state", Type.STRING, "succeeded", "Filter results by state"));
    instancesEndpoint.addOptionalParam(new Param("q", Type.STRING, "climate",
            "Filter results by string in metadata catalog"));
    instancesEndpoint.addOptionalParam(new Param("series", Type.STRING, null, "Filter results by series ID"));
    instancesEndpoint.addOptionalParam(new Param("mp", Type.STRING, null, "Filter results by media package ID"));
    instancesEndpoint.addOptionalParam(new Param("op", Type.STRING, "inspect", "Filter results by current operation"));
    instancesEndpoint.addOptionalParam(new Param("count", Type.STRING, "20", "Results per page (max 100)"));
    instancesEndpoint.addOptionalParam(new Param("startPage", Type.STRING, "0", "Page offset"));
    instancesEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, instancesEndpoint);

    // Workflow Instance
    RestEndpoint instanceEndpoint = new RestEndpoint("instance", RestEndpoint.Method.GET, "/instance/{id}.{format}",
            "Get a specific workflow instance");
    instanceEndpoint.addFormat(new Format("xml", null, null));
    instanceEndpoint.addFormat(new Format("json", null, null));
    instanceEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("Valid request, results returned"));
    instanceEndpoint.addStatus(org.opencastproject.util.doc.Status
            .NOT_FOUND("A workflow instance with this ID was not found"));
    instanceEndpoint.addPathParam(new Param("id", Type.STRING, null, "The ID of the workflow instance"));
    instanceEndpoint.addPathParam(new Param("format", Type.STRING, "xml", "The format of the results: xml or json"));
    instanceEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, instanceEndpoint);

    // Workflow statistics
    RestEndpoint statisticsEndpoint = new RestEndpoint("stats", RestEndpoint.Method.GET, "/statistics.{format}",
            "Get workflow instance statistics");
    statisticsEndpoint.addFormat(new Format("xml", null, null));
    statisticsEndpoint.addFormat(new Format("json", null, null));
    statisticsEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("Valid request, results returned"));
    statisticsEndpoint.addPathParam(new Param("format", Type.STRING, "xml", "The format of the results: xml or json"));
    statisticsEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, statisticsEndpoint);

    // Operation Handlers
    RestEndpoint handlersEndpoint = new RestEndpoint("handlers", RestEndpoint.Method.GET, "/handlers.json",
            "List all registered workflow operation handlers (implementations)");
    handlersEndpoint.addFormat(new Format("json", null, null));
    handlersEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("Valid request, results returned"));
    handlersEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, handlersEndpoint);

    // Workflow Configuration Panel
    RestEndpoint configPanelEndpoint = new RestEndpoint("configuration_panel", RestEndpoint.Method.GET,
            "/configurationPanel", "Get configuration panel for a specific workflow");
    configPanelEndpoint.addFormat(new Format("html", null, null));
    configPanelEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("Valid request, results returned"));
    configPanelEndpoint.addStatus(org.opencastproject.util.doc.Status
            .NOT_FOUND("A workflow definition with this ID was not found"));
    configPanelEndpoint.addOptionalParam(new Param("definitionId", Type.STRING, "full-review",
            "ID of workflow definition"));
    configPanelEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, configPanelEndpoint);

    // Start a new Workflow Instance
    RestEndpoint startEndpoint = new RestEndpoint("start", RestEndpoint.Method.POST, "/start",
            "Start a new workflow instance");
    startEndpoint.addFormat(new Format("xml", null, null));
    startEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, workflow running or queued"));
    startEndpoint.addRequiredParam(new Param("mediapackage", Type.TEXT, generateMediaPackage(),
            "The media package upon which to perform the workflow"));
    String sampleDefinition = null;
    try {
      sampleDefinition = generateWorkflowDefinition();
    } catch (IOException e) {
      logger.warn("Unable to find the sample workflow definition.  The rest docs will not include a sample by default",
              e);
    }
    startEndpoint.addRequiredParam(new Param("definition", Type.TEXT, sampleDefinition, "The workflow definition"));
    startEndpoint.addOptionalParam(new Param("parent", Type.STRING, null, "An optional parent workflow instance"));
    startEndpoint.addRequiredParam(new Param("properties", Type.TEXT, "dvd.format=pal",
            "Configuration properties for this workflow instance"));
    startEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, startEndpoint);

    // Stop a Workflow Instance
    RestEndpoint stopEndpoint = new RestEndpoint("stop", RestEndpoint.Method.POST, "/stop",
            "Stop a running workflow instance");
    stopEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("the stopped workflow"));
    stopEndpoint.addStatus(org.opencastproject.util.doc.Status
            .NOT_FOUND("A workflow instance with this ID was not found"));
    stopEndpoint.addRequiredParam(new Param("id", Type.STRING, null, "The ID of the workflow instance"));
    stopEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, stopEndpoint);

    // Suspend a Workflow Instance
    RestEndpoint suspendEndpoint = new RestEndpoint("suspend", RestEndpoint.Method.POST, "/suspend",
            "Suspend a running workflow instance");
    suspendEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("the suspended workflow"));
    suspendEndpoint.addStatus(org.opencastproject.util.doc.Status
            .NOT_FOUND("A workflow instance with this ID was not found"));
    suspendEndpoint.addRequiredParam(new Param("id", Type.STRING, null, "The ID of the workflow instance"));
    suspendEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, suspendEndpoint);

    // Resume a Workflow Instance
    RestEndpoint resumeAndReplaceEndpoint = new RestEndpoint("replaceAndresume", RestEndpoint.Method.POST,
            "/replaceAndresume", "Resume a suspended workflow instance, replacing the mediapackage");
    resumeAndReplaceEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("the resumed workflow"));
    resumeAndReplaceEndpoint.addStatus(org.opencastproject.util.doc.Status
            .NOT_FOUND("A workflow instance with this ID was not found"));
    resumeAndReplaceEndpoint.addRequiredParam(new Param("id", Type.STRING, null, "The ID of the workflow instance"));
    resumeAndReplaceEndpoint.addOptionalParam(new Param("mediapackage", Type.TEXT, "mediapackage",
            "The updated mediapackage for this workflow instance"));
    resumeAndReplaceEndpoint.addOptionalParam(new Param("properties", Type.TEXT, "",
            "The properties to set for this workflow instance"));
    resumeAndReplaceEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, resumeAndReplaceEndpoint);

    RestEndpoint resumeEndpoint = new RestEndpoint("resume", RestEndpoint.Method.POST, "/resume",
            "Resume a suspended workflow instance");
    resumeEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("the resumed workflow"));
    resumeEndpoint.addStatus(org.opencastproject.util.doc.Status
            .NOT_FOUND("A workflow instance with this ID was not found"));
    resumeEndpoint.addRequiredParam(new Param("id", Type.STRING, null, "The ID of the workflow instance"));
    resumeEndpoint.addOptionalParam(new Param("properties", Type.TEXT, "",
            "The properties to set for this workflow instance"));
    resumeEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, resumeEndpoint);

    // Update a Workflow Instance
    RestEndpoint updateEndpoint = new RestEndpoint("update", RestEndpoint.Method.POST, "/update",
            "Update a workflow instance.  This is typically used by remote workflow service implementations only.");
    updateEndpoint.addStatus(org.opencastproject.util.doc.Status.NO_CONTENT("Workflow updated"));
    updateEndpoint.addRequiredParam(new Param("workflow", Type.TEXT, null, "The workflow instance as xml"));
    updateEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, updateEndpoint);

    return DocUtil.generate(data);
  }

  protected String generateMediaPackage() {
    String samplesUrl = serverUrl + "/workflow/samples";

    return "<ns2:mediapackage xmlns:ns2=\"http://mediapackage.opencastproject.org\" start=\"2007-12-05T13:40:00\" duration=\"1004400000\">\n"
            + "  <media>\n"
            + "    <track id=\"track-1\" type=\"presenter/source\">\n"
            + "      <mimetype>audio/mp3</mimetype>\n" + "      <url>"
            + samplesUrl
            + "/audio.mp3</url>\n"
            + "      <checksum type=\"md5\">950f9fa49caa8f1c5bbc36892f6fd062</checksum>\n"
            + "      <duration>10472</duration>\n"
            + "      <audio>\n"
            + "        <channels>2</channels>\n"
            + "        <bitdepth>0</bitdepth>\n"
            + "        <bitrate>128004.0</bitrate>\n"
            + "        <samplingrate>44100</samplingrate>\n"
            + "      </audio>\n"
            + "    </track>\n"
            + "    <track id=\"track-2\" type=\"presenter/source\">\n"
            + "      <mimetype>video/quicktime</mimetype>\n"
            + "      <url>"
            + samplesUrl
            + "/camera.mpg</url>\n"
            + "      <checksum type=\"md5\">43b7d843b02c4a429b2f547a4f230d31</checksum>\n"
            + "      <duration>14546</duration>\n"
            + "      <video>\n"
            + "        <device type=\"UFG03\" version=\"30112007\" vendor=\"Unigraf\" />\n"
            + "        <encoder type=\"H.264\" version=\"7.4\" vendor=\"Apple Inc\" />\n"
            + "        <resolution>640x480</resolution>\n"
            + "        <scanType type=\"progressive\" />\n"
            + "        <bitrate>540520</bitrate>\n"
            + "        <frameRate>2</frameRate>\n"
            + "      </video>\n"
            + "    </track>\n"
            + "  </media>\n"
            + "  <metadata>\n"
            + "    <catalog id=\"catalog-1\" type=\"dublincore/episode\">\n"
            + "      <mimetype>text/xml</mimetype>\n"
            + "      <url>"
            + samplesUrl
            + "/dc-1.xml</url>\n"
            + "      <checksum type=\"md5\">20e466615251074e127a1627fd0dae3e</checksum>\n"
            + "    </catalog>\n"
            + "  </metadata>\n" + "</ns2:mediapackage>";
  }

  protected String generateWorkflowDefinition() throws IOException {
    InputStream is = null;
    try {
      is = getClass().getResourceAsStream("/sample/compose-distribute-publish.xml");
      return IOUtils.toString(is, "UTF-8");
    } finally {
      IOUtils.closeQuietly(is);
    }
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("/count")
  public Response getCount(@QueryParam("state") WorkflowInstance.WorkflowState state,
          @QueryParam("operation") String operation) {
    try {
      Long count = service.countWorkflowInstances(state, operation);
      return Response.ok(count).build();
    } catch (WorkflowDatabaseException e) {
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("/statistics.xml")
  public Response getStatisticsAsXml() {
    try {
      WorkflowStatistics statistics = service.getStatistics();
      return Response.ok(statistics).build();
    } catch (WorkflowDatabaseException e) {
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/statistics.json")
  public Response getStatisticsAsJson() {
    return getStatisticsAsXml();
  }

  @SuppressWarnings("unchecked")
  @GET
  @Path("definitions.{output:.*}")
  public Response getWorkflowDefinitions(@PathParam("output") String output) throws Exception {
    List<WorkflowDefinition> list = service.listAvailableWorkflowDefinitions();
    if ("json".equals(output)) {
      List<JSONObject> jsonDefs = new ArrayList<JSONObject>();
      for (WorkflowDefinition definition : list) {
        jsonDefs.add(getWorkflowDefinitionAsJson(definition));
      }
      JSONObject json = new JSONObject();
      json.put("workflow_definitions", jsonDefs);
      return Response.ok(json.toJSONString()).header("Content-Type", MediaType.APPLICATION_JSON).build();
    } else {
      return Response.ok(WorkflowBuilder.getInstance().toXml(list)).header("Content-Type", MediaType.TEXT_XML).build();
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("definition/{id}.json")
  public Response getWorkflowDefinitionAsJson(@PathParam("id") String workflowDefinitionId) throws Exception {
    WorkflowDefinition def = null;
    try {
      def = service.getWorkflowDefinitionById(workflowDefinitionId);
      return Response.ok(def).build();
    } catch (NotFoundException e) {
      return Response.status(Status.NOT_FOUND).build();
    }
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("definition/{id}.xml")
  public Response getWorkflowDefinitionAsXml(@PathParam("id") String workflowDefinitionId) throws Exception {
    return getWorkflowDefinitionAsJson(workflowDefinitionId);
  }

  /**
   * Returns the workflow configuration panel HTML snippet for the workflow definition specified by
   * 
   * @param definitionId
   * @return config panel HTML snippet
   */
  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("configurationPanel")
  public Response getConfigurationPanel(@QueryParam("definitionId") String definitionId) {
    try {
      WorkflowDefinition def = service.getWorkflowDefinitionById(definitionId);
      if (def != null) {
        String out = def.getConfigurationPanel();
        return Response.ok(out).build();
      } else {
        return Response.serverError().status(Status.NOT_FOUND).build();
      }
    } catch (Exception e) {
      logger.error(e.getMessage());
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * @param definition
   */
  @SuppressWarnings("unchecked")
  protected JSONObject getWorkflowDefinitionAsJson(WorkflowDefinition definition) {
    JSONObject json = new JSONObject();
    json.put("id", definition.getId());
    json.put("title", definition.getTitle());
    json.put("description", definition.getDescription());
    List<JSONObject> opList = new ArrayList<JSONObject>();
    for (WorkflowOperationDefinition operationDefinition : definition.getOperations()) {
      JSONObject op = new JSONObject();
      op.put("name", operationDefinition.getId());
      op.put("description", operationDefinition.getDescription());
      op.put("exception_handler_workflow", operationDefinition.getExceptionHandlingWorkflow());
      op.put("fail_on_error", operationDefinition.isFailWorkflowOnException());
      opList.add(op);
    }
    json.put("operations", opList);
    return json;
  }

  // CHECKSTYLE:OFF (The number of method parameters is large because we need to handle many potential query parameters)
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("instances.xml")
  public Response getWorkflowsAsXml(@QueryParam("state") String state, @QueryParam("q") String text,
          @QueryParam("series") String seriesId, @QueryParam("mp") String mediapackageId,
          @QueryParam("op") String currentOperation, @QueryParam("startPage") int startPage,
          @QueryParam("count") int count) throws Exception {
    // CHECKSTYLE:ON
    if (count < 1 || count > MAX_LIMIT)
      count = DEFAULT_LIMIT;
    WorkflowQuery q = new WorkflowQuery();
    q.withCount(count);
    q.withStartPage(startPage);
    if (state != null)
      q.withState(WorkflowState.valueOf(state.toUpperCase()));
    if (text != null)
      q.withText(text);
    if (seriesId != null)
      q.withSeries(seriesId);
    if (mediapackageId != null)
      q.withMediaPackage(mediapackageId);
    if (currentOperation != null)
      q.withCurrentOperation(currentOperation);
    WorkflowSet set = service.getWorkflowInstances(q);
    return Response.ok(set).build();
  }

  // CHECKSTYLE:OFF (The number of method parameters is large because we need to handle many potential query parameters)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("instances.json")
  public Response getWorkflowsAsJson(@QueryParam("state") String state, @QueryParam("q") String text,
          @QueryParam("series") String seriesId, @QueryParam("mp") String mediapackageId,
          @QueryParam("op") String currentOperation, @QueryParam("startPage") int startPage,
          @QueryParam("count") int count) throws Exception {
    // CHECKSTYLE:ON
    return getWorkflowsAsXml(state, text, seriesId, mediapackageId, currentOperation, startPage, count);
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("instance/{id}.xml")
  public Response getWorkflowAsXml(@PathParam("id") long id) {
    WorkflowInstance instance;
    try {
      instance = service.getWorkflowById(id);
    } catch (WorkflowDatabaseException e) {
      throw new WebApplicationException(e);
    } catch (NotFoundException e) {
      return Response.status(Status.NOT_FOUND).entity("Workflow instance " + id + " does not exist").build();
    }
    return Response.ok(instance).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("instance/{id}.json")
  public Response getWorkflowAsJson(@PathParam("id") long id) {
    return getWorkflowAsXml(id);
  }

  @POST
  @Path("start")
  @Produces(MediaType.TEXT_XML)
  public WorkflowInstanceImpl start(@FormParam("definition") WorkflowDefinitionImpl workflowDefinition,
          @FormParam("mediapackage") MediaPackageImpl mp, @FormParam("parent") String parentWorkflowId,
          @FormParam("properties") LocalHashMap localMap) {
    Map<String, String> properties = localMap.getMap();
    Long parentIdAsLong = null;
    if (parentWorkflowId != null) {
      try {
        parentIdAsLong = Long.parseLong(parentWorkflowId);
      } catch (NumberFormatException e) {
        throw new WebApplicationException(e);
      }
    }
    try {
      return (WorkflowInstanceImpl) service.start(workflowDefinition, mp, parentIdAsLong, properties);
    } catch (WorkflowDatabaseException e) {
      throw new WebApplicationException(e);
    } catch (NotFoundException e) {
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Path("stop")
  @Produces(MediaType.TEXT_XML)
  public Response stop(@FormParam("id") long workflowInstanceId) {
    try {
      WorkflowInstance workflow = service.stop(workflowInstanceId);
      return Response.ok(workflow).build();
    } catch (NotFoundException e) {
      return Response.status(Status.NOT_FOUND).build();
    } catch (WorkflowDatabaseException e) {
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Path("suspend")
  @Produces(MediaType.TEXT_XML)
  public Response suspend(@FormParam("id") long workflowInstanceId) {
    try {
      WorkflowInstance workflow = service.suspend(workflowInstanceId);
      return Response.ok(workflow).build();
    } catch (NotFoundException e) {
      return Response.status(Status.NOT_FOUND).build();
    } catch (WorkflowDatabaseException e) {
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Path("resume")
  @Produces(MediaType.TEXT_XML)
  public Response resume(@FormParam("id") long workflowInstanceId, @FormParam("properties") LocalHashMap properties) {
    Map<String, String> map;
    if (properties == null) {
      map = new HashMap<String, String>();
    } else {
      map = properties.getMap();
    }
    try {
      WorkflowInstance workflow = service.resume(workflowInstanceId, map);
      return Response.ok(workflow).build();
    } catch (NotFoundException e) {
      return Response.status(Status.NOT_FOUND).build();
    } catch (WorkflowDatabaseException e) {
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Path("replaceAndresume")
  @Produces(MediaType.TEXT_XML)
  public Response resume(@FormParam("id") long workflowInstanceId,
          @FormParam("mediapackage") MediaPackageImpl mediaPackage, @FormParam("properties") LocalHashMap properties) {
    Map<String, String> map;
    if (properties == null) {
      map = new HashMap<String, String>();
    } else {
      map = properties.getMap();
    }
    try {
      WorkflowInstance workflow = service.getWorkflowById(workflowInstanceId);
      if (mediaPackage != null) {
        workflow.setMediaPackage(mediaPackage);
        service.update(workflow);
      }
      service.resume(workflowInstanceId, map);
      return Response.ok(workflow).build();
    } catch (WorkflowDatabaseException e) {
      throw new WebApplicationException(e);
    } catch (NotFoundException e) {
      return Response.status(Status.NOT_FOUND).build();
    }
  }

  @POST
  @Path("update")
  public Response update(@FormParam("workflow") WorkflowInstance workflowInstance) {
    try {
      service.update(workflowInstance);
      return Response.noContent().build();
    } catch (WorkflowDatabaseException e) {
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("handlers.json")
  @SuppressWarnings("unchecked")
  public Response getOperationHandlers() {
    JSONArray jsonArray = new JSONArray();
    for (HandlerRegistration reg : ((WorkflowServiceImpl) service).getRegisteredHandlers()) {
      WorkflowOperationHandler handler = reg.getHandler();
      JSONObject jsonHandler = new JSONObject();
      jsonHandler.put("id", handler.getId());
      jsonHandler.put("description", handler.getDescription());
      JSONObject jsonConfigOptions = new JSONObject();
      for (Entry<String, String> configEntry : handler.getConfigurationOptions().entrySet()) {
        jsonConfigOptions.put(configEntry.getKey(), configEntry.getValue());
      }
      jsonHandler.put("options", jsonConfigOptions);
      jsonArray.add(jsonHandler);
    }
    return Response.ok(jsonArray.toJSONString()).header("Content-Type", MediaType.APPLICATION_JSON).build();
  }

  @PUT
  @Path("/definition")
  public Response registerWorkflowDefinition(@FormParam("workflowDefinition") WorkflowDefinitionImpl workflowDefinition) {
    if (workflowDefinition == null) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    try {
      service.getWorkflowDefinitionById(workflowDefinition.getId());
      return Response.status(Status.PRECONDITION_FAILED).build(); // the workflow definition should be unregistered
    } catch (NotFoundException notFoundException) {
      try {
        service.registerWorkflowDefinition(workflowDefinition);
        return Response
                .created(
                        new URI(UrlSupport.concat(new String[] { serverUrl, "definition",
                                workflowDefinition.getId() + ".xml" }))).build();
      } catch (WorkflowDatabaseException e) {
        return Response.status(Status.INTERNAL_SERVER_ERROR).build();
      } catch (URISyntaxException e) {
        throw new IllegalStateException("Unable to generate a URI for workflow definitions", e);
      }
    } catch (WorkflowDatabaseException e) {
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }
  
  @DELETE
  @Path("/definition/{id}")
  public Response unregisterWorkflowDefinition(@PathParam("id") String workflowDefinitionId) {
    try {
      service.unregisterWorkflowDefinition(workflowDefinitionId);
       return Response.status(Status.NO_CONTENT).build();
    } catch (NotFoundException e) {
      return Response.status(Status.NOT_FOUND).build();
    } catch (WorkflowDatabaseException e) {
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @SuppressWarnings("unchecked")
  protected JSONArray getOperationsAsJson(List<WorkflowOperationInstance> operations) {
    JSONArray jsonArray = new JSONArray();
    for (WorkflowOperationInstance op : operations) {
      JSONObject jsOp = new JSONObject();
      jsOp.put("name", op.getId());
      jsOp.put("description", op.getDescription());
      jsOp.put("state", op.getState().name().toLowerCase());
      jsOp.put("configurations", getConfigsAsJson(op));
      jsonArray.add(jsOp);
    }
    return jsonArray;
  }

  @SuppressWarnings("unchecked")
  protected JSONArray getConfigsAsJson(Configurable entity) {
    JSONArray json = new JSONArray();
    Set<String> keys = entity.getConfigurationKeys();
    if (keys != null) {
      for (String key : keys) {
        JSONObject jsConfig = new JSONObject();
        jsConfig.put(key, entity.getConfiguration(key));
        json.add(jsConfig);
      }
    }
    return json;
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    return docs;
  }
}
