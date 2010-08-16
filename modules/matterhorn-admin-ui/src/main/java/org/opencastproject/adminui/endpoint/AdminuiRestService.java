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

import org.opencastproject.adminui.api.AdminRecording;
import org.opencastproject.adminui.api.AdminRecordingImpl;
import org.opencastproject.adminui.api.AdminRecordingList;
import org.opencastproject.adminui.api.AdminRecordingListImpl;
import org.opencastproject.adminui.api.AdminSeries;
import org.opencastproject.adminui.api.AdminSeriesImpl;
import org.opencastproject.adminui.api.AdminSeriesList;
import org.opencastproject.adminui.api.AdminSeriesListImpl;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.capture.admin.api.Recording;
import org.opencastproject.capture.admin.api.RecordingState;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.scheduler.api.SchedulerFilter;
import org.opencastproject.scheduler.impl.Event;
import org.opencastproject.scheduler.impl.SchedulerServiceImpl;
import org.opencastproject.series.api.Series;
import org.opencastproject.series.api.SeriesMetadata;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;
import org.opencastproject.util.doc.Status;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST endpoint for the Admin UI proxy service
 */
@Path("/")
public class AdminuiRestService {

  private final static int CAPTURE_AGENT_DELAY = 5000;
  private final static int CAPTURE_STATUS_TIMEOUT = 60 * 1000;
  private static final Logger logger = LoggerFactory.getLogger(AdminuiRestService.class);
  private SchedulerServiceImpl schedulerService;
  private SeriesService seriesService;
  private WorkflowService workflowService;
  private CaptureAgentStateService captureAdminService;

  public void setSchedulerService(SchedulerServiceImpl service) {
    logger.debug("binding SchedulerService");
    schedulerService = service;
  }

  public void unsetSchedulerService(SchedulerServiceImpl service) {
    logger.debug("unbinding SchedulerService");
    schedulerService = null;
  }

  public void setSeriesService(SeriesService service) {
    seriesService = service;
  }

  public void unsetSeriesService(SeriesService service) {
    logger.debug("unbinding SeriesService");
    seriesService = null;
  }

  public void setWorkflowService(WorkflowService service) {
    logger.debug("binding WorkflowService");
    workflowService = service;
  }

  public void unsetWorkflowService(WorkflowService service) {
    logger.debug("unbinding WorkflowService");
    workflowService = null;
  }

  public void setCaptureAdminService(CaptureAgentStateService service) {
    logger.debug("binding CaptureAgentStatusService");
    captureAdminService = service;
  }

  public void unsetCaptureAdminService(CaptureAgentStateService service) {
    logger.debug("unbinding CaptureAgentStatusService");
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
  public AdminRecordingListImpl getRecordings(@PathParam("state") String state,
          @QueryParam("pn") int pageNumber,
          @QueryParam("ps") int pageSize,
          @QueryParam("sb") String sortBy,
          @QueryParam("so") String sortOrder) {

    AdminRecordingListImpl out;
    try {
      AdminRecordingList.Order order = Enum.valueOf(AdminRecordingList.Order.class, sortOrder);
      AdminRecordingList.Field field = Enum.valueOf(AdminRecordingList.Field.class, sortBy);
      out = new AdminRecordingListImpl(field, order);
    } catch (Exception e) {
      out = new AdminRecordingListImpl();
    }
    boolean allRecordings = state.toUpperCase().equals("ALL");
    if ((state.toUpperCase().equals("UPCOMING")) || allRecordings) {
      out.addAll(addRecordingStatusForAll("upcoming", getUpcomingRecordings()));
    }
    if ((state.toUpperCase().equals("CAPTURING")) || allRecordings) {
      out.addAll(addRecordingStatusForAll("capturing", getCapturingRecordings()));
    }
    if ((state.toUpperCase().equals("PROCESSING")) || allRecordings) {
      out.addAll(addRecordingStatusForAll("processing", getRecordingsFromWorkflowService(WorkflowState.RUNNING)));
      out.addAll(addRecordingStatusForAll("processing", getRecordingsFromWorkflowService(WorkflowState.INSTANTIATED)));
    }
    if ((state.toUpperCase().equals("FINISHED")) || allRecordings) {
      out.addAll(addRecordingStatusForAll("finished", getRecordingsFromWorkflowService(WorkflowState.SUCCEEDED)));
    }
    if ((state.toUpperCase().equals("HOLD")) || allRecordings) {
      out.addAll(addRecordingStatusForAll("hold", getRecordingsFromWorkflowService(WorkflowState.PAUSED)));
    }
    if ((state.toUpperCase().equals("FAILED")) || allRecordings) {
      out.addAll(addRecordingStatusForAll("failed", getRecordingsFromWorkflowService(WorkflowState.FAILED)));
      out.addAll(addRecordingStatusForAll("failed", getRecordingsFromWorkflowService(WorkflowState.FAILING)));
      out.addAll(addRecordingStatusForAll("failed", getFailedCaptureJobs()));
    }
    if (pageNumber < 0) {
      pageNumber = 0;
    }
    AdminRecordingListImpl page = new AdminRecordingListImpl(out.sortBy, out.sortOrder);
    int first, last;
    if (out.size() <= pageSize) {
      first = 0;
      last = out.size();
    } else {
      first = pageNumber * pageSize;
      last = first + pageSize;
    }
    logger.debug("Returning results items " + first + " - " + (first + pageSize - 1));
    for (int i = first; i < last; i++) {
      try {
        page.add(out.get(i));
      } catch (IndexOutOfBoundsException e) {
        break;
      }
    }
    logger.debug("List: " + out.sortBy.toString() + " " + out.sortOrder.toString());
    return page;
  }

  /** Puts status in recordingStatus field of all items in list.
   *
   * @param status
   * @param in
   * @return
   */
  private LinkedList<AdminRecording> addRecordingStatusForAll(String status, Collection<AdminRecording> in) {
    Iterator<AdminRecording> i = in.iterator();
    LinkedList<AdminRecording> out = new LinkedList<AdminRecording>();
    while (i.hasNext()) {
      AdminRecording item = i.next();
      item.setRecordingStatus(status);
      out.add(item);
    }
    return out;
  }

  /**
   * returns a AdminRecordingList of recordings that are currently begin processed.
   * If the WorkflowService is not present an empty list is returned.
   * @return AdminRecordingList list of upcoming recordings
   */
  private LinkedList<AdminRecording> getRecordingsFromWorkflowService(WorkflowState state) {
    LinkedList<AdminRecording> out = new LinkedList<AdminRecording>();
    if (workflowService != null) {
      logger.debug("getting recordings from workflowService");
      WorkflowInstance[] workflows = workflowService.getWorkflowInstances(workflowService.newWorkflowQuery().withState(state).withCount(100000)).getItems();
      // next line is for debuging: return all workflowInstaces
      //WorkflowInstance[] workflows = workflowService.getWorkflowInstances(workflowService.newWorkflowQuery()).getItems();
      for (int i = 0; i < workflows.length; i++) {
        MediaPackage mediapackage = workflows[i].getMediaPackage();
        AdminRecording item = new AdminRecordingImpl();
        item.setId(workflows[i].getId());
        item.setItemType(AdminRecording.ItemType.WORKFLOW);
        item.setTitle(mediapackage.getTitle());
        item.setPresenter(joinStringArray(mediapackage.getCreators()));
        item.setSeriesTitle(getSeriesNameById(mediapackage.getSeries()));
        item.setSeriesId(mediapackage.getSeries());
        Date date = mediapackage.getDate();
        long duration = mediapackage.getDuration();
        if (date != null) {
          item.setStartTime(Long.toString(date.getTime()));
          item.setEndTime(Long.toString(date.getTime() + duration));
        } else {
          item.setStartTime("0");
          item.setEndTime("0");
        }
        // if there is a mediapackage zip, set it here
        MediaPackageElement[] zipArchives = mediapackage.getElementsByFlavor(MediaPackageElementFlavor.parseFlavor("archive/zip"));
        if (zipArchives.length > 0) {
          item.setZipUrl(zipArchives[0].getURI().toString());
        }
        item.setErrorMessages(workflows[i].getErrorMessages());
        item.setCaptureAgent(null); //FIXME get capture agent from where...?
        // TODO get distribution status #openquestion is there a way to find seriesList if a workflowOperation does distribution?
        WorkflowOperationInstance currentOperation = workflows[i].getCurrentOperation();
        if (currentOperation == null) {
          List<WorkflowOperationInstance> operationsList = workflows[i].getOperations();
          // Loop through the operations to find the last failed operation, and the current operation
          for (WorkflowOperationInstance op : operationsList) {
            if (op.getState().equals(WorkflowOperationInstance.OperationState.FAILED)) {
              item.setFailedOperation(op.getDescription());
            }
          }
          currentOperation = operationsList.get(operationsList.size() - 1);
        }
        if (currentOperation != null) {
          item.setProcessingStatus(currentOperation.getDescription());
        } else {
          item.setProcessingStatus("unknown");
        }
        // get Title and ActionTitle/ActionPanelURL from HoldOperation
        if (state == WorkflowState.PAUSED && currentOperation != null) {
          if (currentOperation.getState() == OperationState.PAUSED) {       // take only those WFInstances into account that have been paused by a HoldOperation
            item.setHoldOperationTitle(currentOperation.getDescription());
            item.setHoldActionTitle(currentOperation.getHoldActionTitle());
            URL holdUrl = currentOperation.getHoldStateUserInterfaceUrl();
            if (holdUrl != null) {
              item.setHoldActionPanelURL(holdUrl.toString());
            }
            out.add(item);
          }
        } else {
          out.add(item);
        }
        //logger.debug("Recording state: " + state.name() + " MediaPackage: " + mediapackage.getTitle() + " - " + joinStringArray(mediapackage.getCreators()) + " - " + mediapackage.getSeries());
      }
    } else {
      logger.warn("WorkflowService not present, returning empty list");
    }
    return out;
  }

  /**
   * Joins String array
   * 
   * @param values The array of values to concatenate
   * @return A formated string containing each of the values
   */
  private String joinStringArray(String[] values) {
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
    HashMap<String, Integer> out = new HashMap<String, Integer>();
    out.put("all", new Integer(0));           // init output data with zeros
    out.put("capturing", new Integer(0));
    out.put("processing", Integer.valueOf(0));
    out.put("inactive", Integer.valueOf(0));
    out.put("failed", Integer.valueOf(0));
    out.put("finished", Integer.valueOf(0));
    out.put("hold", Integer.valueOf(0));
    Integer total = 0;

    if (captureAdminService != null) {
      Map<String, Recording> recordings = new HashMap<String, Recording>(captureAdminService.getKnownRecordings());
      Iterator<String> i = recordings.keySet().iterator();
      int capturing = 0;
      while (i.hasNext()) {
        Recording r = recordings.get(i.next());
        if (r.getState().equals(RecordingState.CAPTURING)
                || r.getState().equals(RecordingState.CAPTURE_FINISHED)
                || r.getState().equals(RecordingState.MANIFEST)
                || r.getState().equals(RecordingState.UPLOADING)
                || r.getState().equals(RecordingState.COMPRESSING)) {   // FIXME also take into account RecordingState.UNKNOWN ??
          capturing++;
          total++;
        }
      }
      out.put("capturing", new Integer(capturing));
    } else {
      logger.warn("CaptureAdmin service not present, unable to retrieve capture statistics");
    }

    // get statistics from workflowService if present
    if (workflowService != null) {
      WorkflowQuery q = workflowService.newWorkflowQuery().withStartPage(0).withCount(100000);
      WorkflowInstance[] workflows = workflowService.getWorkflowInstances(q).getItems();
      int i = 0, processing = 0, inactive = 0, finished = 0, errors = 0, paused = 0;
      for (; i < workflows.length; i++) {
        switch (workflows[i].getState()) {
          case FAILED:
          case FAILING:
            errors++;
            total++;
            break;
          case INSTANTIATED:
          case RUNNING:
            processing++;
            total++;
            break;
          case PAUSED:
            if (workflows[i].getCurrentOperation().getState() == OperationState.PAUSED) {
              paused++;
              total++;
            }
            break;
          case STOPPED:
            break;
          case SUCCEEDED:
            finished++;
            total++;
            break;
        }
      }
      out.put("processing", Integer.valueOf(processing));
      out.put("inactive", Integer.valueOf(inactive));
      out.put("failed", Integer.valueOf(errors));
      out.put("finished", Integer.valueOf(finished));
      out.put("hold", Integer.valueOf(paused));
    } else {
      logger.warn("workflow service not present, unable to retrieve workflow statistics");
    }

    // get number of upcoming recordings if scheduler is present
    if (schedulerService != null) {
      int upcoming = 0;
      Event[] events = schedulerService.getUpcomingEvents();
      for (Event event : events) {
        if (System.currentTimeMillis() < event.getStartdate().getTime()) {
          upcoming++;
          total++;
        }
      }
      out.put("upcoming", new Integer(upcoming));
    } else {
      logger.warn("scheduler service not present, unable to retreive number of upcoming events");
    }

    // Add number of failed capture jobs to failed
    LinkedList<AdminRecording> failedCaptures = getFailedCaptureJobs();
    total += failedCaptures.size();
    out.put("failed", out.get("failed").intValue() + failedCaptures.size());

    out.put("all", new Integer(total));
    return out;
  }

  /** returns the list of capture jobs that haven either failed on the capture
   *  agent or that where never taken up by the capture agent. If either scheduler
   *  or capture admin service are not present, an empty list is returned.
   *
   * @return list of failed recordings
   */
  private LinkedList<AdminRecording> getFailedCaptureJobs() {
    LinkedList<AdminRecording> out = new LinkedList<AdminRecording>();
    if ((schedulerService != null) && (captureAdminService != null)) {
      SchedulerFilter filter = schedulerService.getNewSchedulerFilter();
      filter.setEnd(new Date(System.currentTimeMillis() + CAPTURE_AGENT_DELAY));
      Event[] events = schedulerService.getEvents(filter);
      for (Event event : events) {
        Recording recording = captureAdminService.getRecordingState(event.getEventId());
        if ((recording == null)
                || (recording.getState().equals(RecordingState.CAPTURE_ERROR))
                || (recording.getState().equals(RecordingState.COMPRESSING_ERROR))
                || (recording.getState().equals(RecordingState.MANIFEST_ERROR))
                || (recording.getState().equals(RecordingState.UPLOAD_ERROR))
                || ((new Date().getTime() - recording.getLastCheckinTime() > CAPTURE_STATUS_TIMEOUT)
                  && (recording.getState().equals(RecordingState.CAPTURING)
                      || recording.getState().equals(RecordingState.COMPRESSING)
                      || recording.getState().equals(RecordingState.MANIFEST)
                      || recording.getState().equals(RecordingState.UPLOADING)))
                ) {
          AdminRecordingImpl item = new AdminRecordingImpl();
          item.setItemType(AdminRecording.ItemType.SCHEDULER_EVENT);
          item.setId(event.getEventId());
          item.setTitle(event.getValue("title"));
          item.setPresenter(event.getValue("creator"));
          item.setSeriesTitle(getSeriesNameFromEvent(event));
          item.setStartTime(Long.toString(event.getStartdate().getTime()));
          item.setEndTime(Long.toString(event.getEnddate().getTime()));
          if (recording != null) {
            item.setProcessingStatus("Failed during capture" /*recording.getState()*/);
          } else {
            item.setProcessingStatus("Failed to start capture");
          }
          out.add(item);
        }
      }
    } else {
      logger.warn("Either Scheduler or CaptureAdmin service not present, unable to generate list of failed capture jobs");
    }
    return out;
  }

  /**
   * returns a AdminRecordingList of upcoming events. If the schedulerService
   * is not present an empty list is returned.
   * @return AdminRecordingList list of upcoming recordings
   */
  private LinkedList<AdminRecording> getUpcomingRecordings() {
    LinkedList<AdminRecording> out = new LinkedList<AdminRecording>();
    if (schedulerService != null) {
      logger.debug("getting upcoming recordings from scheduler");
      Event[] events = schedulerService.getUpcomingEvents();
      for (int i = 0; i < events.length; i++) {
        if (System.currentTimeMillis() < events[i].getStartdate().getTime()) {
          AdminRecording item = new AdminRecordingImpl();
          item.setId(events[i].getEventId());
          item.setItemType(AdminRecording.ItemType.SCHEDULER_EVENT);
          item.setTitle(events[i].getValue("title"));
          item.setPresenter(events[i].getValue("creator"));
          item.setSeriesTitle(getSeriesNameFromEvent(events[i]));
          item.setStartTime(Long.toString(events[i].getStartdate().getTime()));
          item.setEndTime(Long.toString(events[i].getEnddate().getTime()));
          item.setCaptureAgent(events[i].getValue("device"));
          item.setProcessingStatus("Scheduled");
          item.setDistributionStatus("not distributed");
          out.add(item);
        }
      }
    } else {
      logger.warn("scheduler not present, returning empty list");
    }
    return out;
  }

  private LinkedList<AdminRecording> getCapturingRecordings() {
    LinkedList<AdminRecording> out = new LinkedList<AdminRecording>();
    if (schedulerService != null && captureAdminService != null && seriesService != null) {
      //logger.debug("getting capturing recordings from scheduler");
      Map<String, Recording> recordings = captureAdminService.getKnownRecordings();
      for (Entry<String, Recording> recording : recordings.entrySet()) {
        Event event = schedulerService.getEvent(recording.getKey());
        try {
          Recording r = recording.getValue();
          if (r != null) {
            String state = r.getState();
            if (state != null
                    && state.equals(RecordingState.CAPTURING)
                    || state.equals(RecordingState.CAPTURE_FINISHED)
                    || state.equals(RecordingState.COMPRESSING)
                    || state.equals(RecordingState.MANIFEST)
                    || state.equals(RecordingState.UPLOADING)) {
              AdminRecording item = new AdminRecordingImpl();
              item.setId(event.getEventId());
              item.setItemType(AdminRecording.ItemType.SCHEDULER_EVENT);
              item.setTitle(event.getValue("title"));
              item.setPresenter(event.getValue("creator"));
              item.setSeriesTitle(getSeriesNameFromEvent(event));
              item.setStartTime(Long.toString(event.getStartdate().getTime()));
              item.setEndTime(Long.toString(event.getEnddate().getTime()));
              item.setCaptureAgent(event.getValue("device"));
              item.setProcessingStatus(r.getState());
              out.add(item);
            } else {
              logger.warn("Could not get state from recording: " + r.getID());
            }
          }
        } catch (Exception e) {
          logger.error("Exception while preparing list of capturing events: " + e.getMessage());
          e.printStackTrace();
        }
      }
    } else {
      logger.warn("scheduler or capture admin service not present, returning empty list");
    }
    return out;
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("series")
  public AdminSeriesListImpl getSeries(@QueryParam("pn") int pageNumber,
          @QueryParam("ps") int pageSize,
          @QueryParam("sb") String sortBy,
          @QueryParam("so") String sortOrder) {
    logger.debug("PageSize, PageNumber: {},{}", pageSize, pageNumber);
    AdminSeriesListImpl seriesList;
    try {
      AdminSeriesList.Order order = Enum.valueOf(AdminSeriesList.Order.class, sortOrder);
      AdminSeriesList.Field field = Enum.valueOf(AdminSeriesList.Field.class, sortBy);
      seriesList = new AdminSeriesListImpl(field, order);
    } catch (Exception e) {
      seriesList = new AdminSeriesListImpl();
    }
    List<Series> allSeries = seriesService.getAllSeries();
    for (Series s : allSeries) {
      AdminSeries series = new AdminSeriesImpl();
      series.setId(s.getSeriesId());
      List<SeriesMetadata> seriesMetadata = s.getMetadata();
      for (SeriesMetadata metadata : seriesMetadata) {
        if (metadata.getKey().equals("title")) {
          series.setTitle(metadata.getValue());
        } else if (metadata.getKey().equals("creator")) {
          series.setCreator(metadata.getValue());
        } else if (metadata.getKey().equals("contributor")) {
          series.setContributor(metadata.getValue());
        }
      }
      logger.debug("Found series {}", series.getTitle());
      seriesList.add(series);
    }
    AdminSeriesListImpl page = new AdminSeriesListImpl(seriesList.sortBy, seriesList.sortOrder);
    if (pageNumber < 0) {
      pageNumber = 0;
    }
    if (pageSize == 0) {
      pageSize = seriesList.size();
    }
    int first, last;
    if (seriesList.size() <= pageSize) {
      first = 0;
      last = seriesList.size();
    } else {
      first = pageNumber * pageSize;
      last = first + pageSize;
    }
    logger.debug("Returning results items " + first + " - " + (first + pageSize - 1));
    for (int i = first; i < last; i++) {
      try {
        page.add(seriesList.get(i));
      } catch (IndexOutOfBoundsException e) {
        break;
      }
    }
    logger.debug("List: " + page.sortBy.toString() + " " + page.sortOrder.toString());
    return page;
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

  public String getSeriesNameFromEvent(Event event) {
    String seriesId = event.getValue("seriesId");
    if (!seriesId.isEmpty()) {
      return getSeriesNameById(seriesId);
    }
    return "";
  }

  public String getSeriesNameById(String seriesId) {
    String seriesName = null;
    if (seriesId != null && !seriesId.isEmpty()) {
      Series series = seriesService.getSeries(seriesId);
      if (series != null) {
        seriesName = series.getFromMetadata("title");
      }
    }
    if (seriesName != null) {
      return seriesName;
    }
    return "";
  }

  public AdminuiRestService() {
  }
}
