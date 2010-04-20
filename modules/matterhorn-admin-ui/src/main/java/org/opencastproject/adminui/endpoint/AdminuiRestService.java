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
package org.opencastproject.adminui.endpoint;

import org.opencastproject.adminui.api.RecordingDataView;
import org.opencastproject.adminui.api.RecordingDataViewImpl;
import org.opencastproject.adminui.api.RecordingDataViewList;
import org.opencastproject.adminui.api.RecordingDataViewListImpl;
import org.opencastproject.capture.admin.api.AgentState;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.capture.admin.api.Recording;
import org.opencastproject.capture.admin.api.RecordingState;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.scheduler.api.SchedulerEvent;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;
import org.opencastproject.util.doc.Status;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;

/**
 * REST endpoint for the Admin UI proxy service
 */
@Path("/")
public class AdminuiRestService {

  private static final Logger logger = LoggerFactory.getLogger(AdminuiRestService.class);
  private SchedulerService schedulerService;
  private WorkflowService workflowService;
  private CaptureAgentStateService captureAdminService;

  public void setSchedulerService(SchedulerService service) {
    logger.info("binding SchedulerService");
    schedulerService = service;
  }

  public void unsetSchedulerService(SchedulerService service) {
    logger.info("unbinding SchedulerService");
    schedulerService = null;
  }

  public void setWorkflowService(WorkflowService service) {
    logger.info("binding WorkflowService");
    workflowService = service;
  }

  public void unsetWorkflowService(WorkflowService service) {
    logger.info("unbinding WorkflowService");
    workflowService = null;
  }

  public void setCaptureAdminService(CaptureAgentStateService service) {
    logger.info("binding CaptureAgentStatusService");
    captureAdminService = service;
  }

  public void unsetCaptureAdminService(CaptureAgentStateService service) {
    logger.info("unbinding CaptureAgentStatusService");
    captureAdminService = null;
  }

  /**
   * Returns a list of recordings in a certain state.
   * @param state state according to which the recordings should filtered
   * @return recordings list of recordings in specified state
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("recordings/{state}")
  public RecordingDataViewListImpl getRecordings(@PathParam("state") String state) {
    RecordingDataViewListImpl out = new RecordingDataViewListImpl();
    // This returnes an empty list as long as there's no recordings - all screen (to speed things up)
    /*if (state.toUpperCase().equals("ALL")) {
      return out;
    }*/
    if ((state.toUpperCase().equals("UPCOMING")) || (state.toUpperCase().equals("ALL"))) {
      out.addAll(getUpcomingRecordings());
    }
    if ((state.toUpperCase().equals("CAPTURING")) || (state.toUpperCase().equals("ALL"))) {
      out.addAll(getCapturingRecordings());
    }
    if ((state.toUpperCase().equals("PROCESSING")) || (state.toUpperCase().equals("ALL"))) {
      out.addAll(getRecordingsFromWorkflowService(WorkflowState.RUNNING));
    }
    if ((state.toUpperCase().equals("FINISHED")) || (state.toUpperCase().equals("ALL"))) {
      out.addAll(getRecordingsFromWorkflowService(WorkflowState.SUCCEEDED));
    }
    if ((state.toUpperCase().equals("HOLD")) || (state.toUpperCase().equals("ALL"))) {
      out.addAll(getRecordingsFromWorkflowService(WorkflowState.PAUSED));
    }
    if ((state.toUpperCase().equals("FAILED")) || (state.toUpperCase().equals("ALL"))) {
      out.addAll(getRecordingsFromWorkflowService(WorkflowState.FAILED));
      out.addAll(getRecordingsFromWorkflowService(WorkflowState.FAILING));
    }
    return out;
  }

  /**
   * returns a RecordingDataViewList of recordings that are currently begin processed. 
   * If the WorkflowService is not present an empty list is returned.
   * @return RecordingDataViewList list of upcoming recordings
   */
  private RecordingDataViewList getRecordingsFromWorkflowService(WorkflowState state) {
    RecordingDataViewList out = new RecordingDataViewListImpl();
    if (workflowService != null) {
      logger.info("getting recordings from workflowService");
      WorkflowInstance[] workflows = workflowService.getWorkflowInstances(workflowService.newWorkflowQuery().withState(state)).getItems();
      // next line is for debuging: return all workflowInstaces
      //WorkflowInstance[] workflows = workflowService.getWorkflowInstances(workflowService.newWorkflowQuery()).getItems();
      for (int i = 0; i < workflows.length; i++) {
        MediaPackage mediapackage = workflows[i].getMediaPackage();
        RecordingDataView item = new RecordingDataViewImpl();
        item.setId(workflows[i].getId());
        item.setTitle(mediapackage.getTitle());
        //item.setPresenter(formatMultipleString(mediapackage.getContributors()));
        item.setPresenter(formatMultipleString(mediapackage.getCreators()));
        item.setSeriesTitle(mediapackage.getSeriesTitle());
        item.setSeriesId(mediapackage.getSeries());
        item.setSeriesTitle(mediapackage.getSeriesTitle());
        Date date = mediapackage.getDate();
        if (date != null) {
          //item.setStartTime(sdf.format(date));
          item.setStartTime(Long.toString(date.getTime()));
        }
        item.setCaptureAgent(null); //FIXME get capture agent from where...?
        WorkflowOperationInstance operation = null;
        ListIterator<WorkflowOperationInstance> instances = workflows[i].getOperations().listIterator();
        StringBuffer sb = new StringBuffer();
        while (instances.hasNext()) {
          operation = instances.next();
          sb.append(operation.getState().toString() + ": " + operation.getId() + ";");
        }
        item.setProcessingStatus(sb.toString());
        /*if (operation != null) {
        item.setProcessingStatus(operation.getState().toString() + " : " + operation.getName());
        } else {
        logger.warn("Could not get any WorkflowOperationInstance from WorkflowInstance.");
        }*/
        // TODO get distribution status #openquestion is there a way to find out if a workflowOperation does distribution?

        // get Title and ActionTitle/ActionPanelURL from HoldOperation
        if (state == WorkflowState.PAUSED) {
          WorkflowOperationInstance instance = workflows[i].getCurrentOperation();
          if (instance.getState() == OperationState.PAUSED) {       // take only those WFInstances into account that have been paused by a HoldOperation
            item.setHoldOperationTitle(instance.getDescription());
            item.setHoldActionTitle(instance.getHoldActionTitle());
            item.setHoldActionPanelURL(instance.getHoldStateUserInterfaceUrl().toString());
            out.add(item);
          }
        } else {
          out.add(item);
        }
      }
    } else {
      logger.warn("WorkflowService not present, returning empty list");
    }
    return out;
  }

  /**
   * Formats an array of values to a single string for display
   * 
   * @param values The array of values to concatenate
   * @return A formated string containing each of the values
   */
  private String formatMultipleString(String[] values) {
    if (values == null || values.length == 0) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < values.length; i++) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append(values[i]);
    }
    return sb.toString();
  }

  /**
   * Returns simple statistics about "recordings" in the system
   * @return simple statistics about "recordings" in the system
   */
  @SuppressWarnings("unchecked")
  @GET
  @Path("countRecordings")
  public Response countRecordings() {
    HashMap<String, Integer> stats = getRecordingsStatistic();
    Iterator<Entry<String, Integer>> i = stats.entrySet().iterator();
    JSONObject out = new JSONObject();
    while (i.hasNext()) {
      Entry<String, Integer> ent = i.next();
      out.put(ent.getKey(), ent.getValue());
    }
    return Response.ok(out.toJSONString()).header("Content-Type", MediaType.APPLICATION_JSON).build();
  }

  /**
   * returns a statistic about number and state of recordings in the system
   * @return statistic about number and state of recordings in the system
   */
  public HashMap<String, Integer> getRecordingsStatistic() {
    String logMessage = "got statistics from: ";
    HashMap<String, Integer> out = new HashMap<String, Integer>();
    Integer total = new Integer(0);

    // get number of upcoming recordings if scheduler is present
    if (schedulerService != null) {
      SchedulerEvent[] events = schedulerService.getUpcomingEvents();
      out.put("upcoming", new Integer(events.length));
      logMessage += "scheduler-service";
      total += events.length;
    } else {
      logger.warn("scheduler service not present, unable to retreive number of upcoming events");
    }

    // get statistics from capture admin if present
    if (captureAdminService != null) {
      Map<String, Recording> recordings = captureAdminService.getKnownRecordings();
      Iterator<String> i = recordings.keySet().iterator();
      int capturing = 0;
      while (i.hasNext()) {
        if (recordings.get(i.next()).getState().equals(AgentState.CAPTURING)) {
          capturing++;
        }
      }
      out.put("capturing", new Integer(capturing));
      total += capturing;
      logMessage += " capture-admin-service";
    } else {
      logger.warn("CaptureAdmin service not present, unable to retrieve capture statistics");
    }

    // get statistics from workflowService if present
    if (workflowService != null) {
      WorkflowInstance[] workflows = workflowService.getWorkflowInstances(workflowService.newWorkflowQuery()).getItems();
      int i = 0, processing = 0, inactive = 0, finished = 0, errors = 0, paused = 0;
      for (; i < workflows.length; i++) {
        switch (workflows[i].getState()) {
          case FAILED:
          case FAILING:
            errors++;
            break;
          case INSTANTIATED:
          case RUNNING:
            processing++;
            break;
          case PAUSED:
            if (workflows[i].getCurrentOperation().getState() == OperationState.PAUSED) {
              paused++;
            }
            break;
          case STOPPED:
            inactive++;
            break;
          case SUCCEEDED:
            finished++;
        }
      }
      out.put("processing", Integer.valueOf(processing));
      out.put("inactive", Integer.valueOf(inactive));
      out.put("errors", Integer.valueOf(errors));
      out.put("finished", Integer.valueOf(finished));
      out.put("hold", Integer.valueOf(paused));
      total += i;
      logMessage += " workflow-service";
    } else {
      logger.warn("workflow service not present, unable to retrieve workflow statistics");
    }
    out.put("total", total);
    //logger.info(logMessage);
    return out;
  }

  /**
   * returns a RecordingDataViewList of upcoming events. If the schedulerService
   * is not present an empty list is returned.
   * @return RecordingDataViewList list of upcoming recordings
   */
  private RecordingDataViewList getUpcomingRecordings() {
    RecordingDataViewList out = new RecordingDataViewListImpl();
    if (schedulerService != null) {
      logger.info("getting upcoming recordings from scheduler");
      SchedulerEvent[] events = schedulerService.getUpcomingEvents();
      for (int i = 0; i < events.length; i++) {
        RecordingDataView item = new RecordingDataViewImpl();
        item.setId(events[i].getID());
        item.setTitle(events[i].getTitle());
        item.setPresenter(events[i].getCreator());
        item.setSeriesTitle(events[i].getSeriesID());    // actually it's the series title
        // FIXME Add the series ID too
        item.setStartTime(Long.toString(events[i].getStartdate().getTime()));
        item.setEndTime(Long.toString(events[i].getEnddate().getTime()));
        item.setCaptureAgent(events[i].getDevice());
        item.setProcessingStatus("scheduled");
        item.setDistributionStatus("not distributed");
        out.add(item);
      }
    } else {
      logger.warn("scheduler not present, returning empty list");
    }
    return out;
  }

  private RecordingDataViewList getCapturingRecordings() {
    RecordingDataViewList out = new RecordingDataViewListImpl();
    if (schedulerService != null && captureAdminService != null) {
      logger.info("getting capturing recordings from scheduler");
      SchedulerEvent[] events = schedulerService.getCapturingEvents();
      for (int i = 0; i < events.length; i++) {
        RecordingDataView item = new RecordingDataViewImpl();
        item.setId(events[i].getID());
        item.setTitle(events[i].getTitle());
        item.setPresenter(events[i].getCreator());
        item.setSeriesTitle(events[i].getSeriesID());    // actually it's the series title
        // FIXME Add the series ID too
        item.setStartTime(Long.toString(events[i].getStartdate().getTime()));
        item.setEndTime(Long.toString(events[i].getEnddate().getTime()));
        item.setCaptureAgent(events[i].getDevice());
        Recording r = captureAdminService.getRecordingState(events[i].getID());
        String recordingState = RecordingState.UNKNOWN;
        if (r != null) {
          recordingState = r.getState();
        }
        item.setRecordingStatus(recordingState);
        item.setProcessingStatus("scheduled");
        item.setDistributionStatus("not distributed");
        out.add(item);
      }
    } else {
      logger.warn("scheduler or capture admin service not present, returning empty list");
    }
    return out;
  }

  /**
   * @return documentation for this endpoint
   */
  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    if (docs == null) {
      docs = generateDocs();
    }
    return docs;
  }
  protected String docs;
  private String[] notes = {
    "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
    "If the service is down or not working it will return a status 503, this means the the underlying service is not working and is either restarting or has failed",
    "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In other words, there is a bug! You should file an error report with your server logs from the time when the error occurred: <a href=\"https://issues.opencastproject.org\">Opencast Issue Tracker</a>",};

  private String generateDocs() {
    DocRestData data = new DocRestData("adminuiservice", "Admin UI Service", "/admin/rest", notes);

    // abstract
    data.setAbstract("This service reports the number and state of available recordings. It is designed to support the Admin UI.");

    // getRecordings
    RestEndpoint endpoint = new RestEndpoint("getAgent", RestEndpoint.Method.GET,
            "/recordings/{state}",
            "Return all recordings with a given state");
    endpoint.addPathParam(new Param("state", Param.Type.STRING, null,
            "The state of the recordings"));
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addStatus(Status.OK(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, endpoint);

    // countRecordings
    endpoint = new RestEndpoint("countRecordings", RestEndpoint.Method.GET,
            "/countRecordings",
            "Return number of recordings that match each possible state");
    endpoint.addFormat(new Format("JSON", null, null));
    endpoint.addStatus(Status.OK(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, endpoint);

    return DocUtil.generate(data);
  }

  public AdminuiRestService() {
  }
}
