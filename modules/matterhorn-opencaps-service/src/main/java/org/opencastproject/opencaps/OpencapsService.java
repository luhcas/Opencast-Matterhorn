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
package org.opencastproject.opencaps;

import org.opencastproject.media.mediapackage.Catalog;
import org.opencastproject.media.mediapackage.CatalogImpl;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.workflow.api.ResumableWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowSet;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * The captions handler service <br/>
 * Search will provide means of searching for media packages without timed text catalogs <br/>
 * The consumer will present a list of those media packages <br/>
 * The consumer sends back timed text, which will be stored in the working file repository and added to the media
 * package <br/>
 * A new workflow (or the existing one with additional operations) will be kicked off (for redistribution including
 * captions)
 */
public class OpencapsService implements ResumableWorkflowOperationHandler {

  public static final String MEDIA_TAG = "captioning";
  public static final String OPERATION_NAME = "captions";
  public static final String FLAVOR_TYPE = "captions";
  public static final String TYPE_TIMETEXT = "TimeText";
  public static final String TYPE_DESCAUDIO = "DescriptiveAudio";
  public static final String REPUBLISH_WORKFLOW = "republish-after-captioning";
  public static final String REPUBLISH_WORKFLOW_DEF = "/workflow/republish-after-captioning.xml";
  public static final String WORKFLOW_DEF = "/workflow/caption-with-opencaps.xml";
  // FIXME: add a correct title for opencaps hold action
  public static final String HOLD_ACTION_TITLE = "Opencaps";

  private static final Logger logger = LoggerFactory.getLogger(OpencapsService.class);

  private Workspace workspace;

  private WorkflowService workflowService;

  private WorkflowDefinition workflowDefinition;

  public void activate(ComponentContext cc) {
    // Load the republish workflow definition, but don't register it with the workflow service
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream(REPUBLISH_WORKFLOW_DEF);
      workflowDefinition = WorkflowBuilder.getInstance().parseWorkflowDefinition(in);
    } catch (Exception e) {
      throw new IllegalStateException("unable to load workflow definition:", e);
    } finally {
      IOUtils.closeQuietly(in);
    }
    // Register the full opencaps workflow definition with the workflow service
    try {
      in = getClass().getResourceAsStream(WORKFLOW_DEF);
      WorkflowDefinition opencapsDefinition = WorkflowBuilder.getInstance().parseWorkflowDefinition(in);
      workflowService.registerWorkflowDefinition(opencapsDefinition);
    } catch(Exception e) {
      throw new IllegalStateException("unable to register workflow definition:", e);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  public void unsetWorkspace(Workspace workspace) {
    this.workspace = null;
  }

  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  public void unsetWorkflowService(WorkflowService workflowService) {
    this.workflowService = null;
  }

  public CaptionsResults getProcessedMedia(int start, int max) {
    List<OpencapsMediaItem> l = new ArrayList<OpencapsMediaItem>();
    if (start < 0) {
      start = 0;
    }
    WorkflowQuery q = workflowService.newWorkflowQuery();
    q.withState(WorkflowState.SUCCEEDED);
    q.withCount(max).withStartPage(start);
    WorkflowSet wfs = workflowService.getWorkflowInstances(q);
    int total = (int) wfs.size();
    WorkflowInstance[] workflows = wfs.getItems();
    for (WorkflowInstance workflow : workflows) {
      MediaPackage mp = workflow.getMediaPackage();
      l.add(new OpencapsMediaItem(workflow.getId(), mp));
    }
    return new CaptionsResults(l, start, max, total);
  }

  public CaptionsResults getHeldMedia(int start, int max) {
    List<OpencapsMediaItem> l = new ArrayList<OpencapsMediaItem>();
    if (start < 0) {
      start = 0;
    }
    WorkflowQuery q = workflowService.newWorkflowQuery();
    q.withState(WorkflowState.PAUSED).withCurrentOperation(OPERATION_NAME);
    q.withCount(max).withStartPage(start);
    WorkflowSet wfs = workflowService.getWorkflowInstances(q);
    int total = (int) wfs.size();
    WorkflowInstance[] workflows = wfs.getItems();
    for (WorkflowInstance workflow : workflows) {
      MediaPackage mp = workflow.getMediaPackage();
      l.add(new OpencapsMediaItem(workflow.getId(), mp));
    }
    return new CaptionsResults(l, start, max, total);
  }

  public OpencapsMediaItem updateCaptions(String workflowId, String captionType, InputStream data) {
    if (workflowId == null || "".equals(workflowId)) {
      throw new IllegalArgumentException("workflowId must be set");
    }
    if (captionType == null || "".equals(captionType)) {
      throw new IllegalArgumentException("captionType must be set");
    }
    if (data == null) {
      throw new IllegalArgumentException("data must be set");
    }
    // find the media package given a workflow
    WorkflowInstance workflow = workflowService.getWorkflowById(workflowId);
    OpencapsMediaItem cmi;
    if (workflow != null) {
      MediaPackage mp = workflow.getMediaPackage();
      // get the MP and update it
      MediaPackageElementFlavor flavor = new MediaPackageElementFlavor(FLAVOR_TYPE, captionType);
      Catalog captionsCatalog = CatalogImpl.newInstance();
      captionsCatalog.setFlavor(flavor);
      captionsCatalog.addTag("engage"); // TODO: Make this configurable or have it handled by the workflow
      mp.add(captionsCatalog);

      // Store the captions in the workspace
      URI uri = workspace.put(mp.getIdentifier().compact(), captionsCatalog.getIdentifier(), "captions.xml", data);
      captionsCatalog.setURI(uri);

      // Start the republish workflow if this media package has already been processed
      if (WorkflowInstance.WorkflowState.SUCCEEDED.equals(workflow.getState())) {
        workflowService.start(workflowDefinition, mp);
      } else {
        workflowService.update(workflow);
      }
      cmi = new OpencapsMediaItem(workflowId, mp);
    } else {
      throw new IllegalArgumentException("No workflow found with the given id: " + workflowId);
    }
    return cmi;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.opencaps.OpencapsService#getCaptionsMediaItem(java.lang.String)
   */
  public OpencapsMediaItem getCaptionsMediaItem(String workflowId) {
    if (workflowId == null || "".equals(workflowId)) {
      throw new IllegalArgumentException("workflowId must be set");
    }
    // find the media package given a workflow
    WorkflowInstance workflow = workflowService.getWorkflowById(workflowId);
    OpencapsMediaItem cmi;
    if (workflow != null) {
      MediaPackage mp = workflow.getMediaPackage();
      cmi = new OpencapsMediaItem(workflowId, mp);
    } else {
      cmi = null;
    }
    return cmi;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance)
   */
  public WorkflowOperationResult start(WorkflowInstance workflowInstance) throws WorkflowOperationException {
    MediaPackageElementFlavor flavor = new MediaPackageElementFlavor(FLAVOR_TYPE, "*");
    if (workflowInstance.getMediaPackage().getCatalogs(flavor).length > 0) {
      logger.info("Mediapackage {} already contains captions. Skipping caption hold state");
      return WorkflowBuilder.getInstance().buildWorkflowOperationResult(Action.CONTINUE);
    } else {
      logger.info("Waiting for captions to be provided");
      return WorkflowBuilder.getInstance().buildWorkflowOperationResult(Action.PAUSE);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#resume(org.opencastproject.workflow.api.WorkflowInstance,
   *      java.util.Map)
   */
  @Override
  public WorkflowOperationResult resume(WorkflowInstance workflowInstance) throws WorkflowOperationException {
    return WorkflowBuilder.getInstance().buildWorkflowOperationResult(Action.CONTINUE);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#destroy(org.opencastproject.workflow.api.WorkflowInstance)
   */
  @Override
  public void destroy(WorkflowInstance workflowInstance) throws WorkflowOperationException {
    logger.debug("cleaning up after handling a caption operation on {}", workflowInstance);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.ResumableWorkflowOperationHandler#getHoldStateUserInterfaceURL(WorkflowInstance)
   */
  @Override
  public URL getHoldStateUserInterfaceURL(WorkflowInstance workflow) throws WorkflowOperationException {
    String opencapsUrl = workflow.getCurrentOperation().getConfiguration("opencaps.url");
    if (opencapsUrl != null)
      try {
        opencapsUrl = opencapsUrl.replaceAll("\\{id\\}", workflow.getId());
        return new URL(opencapsUrl);
      } catch (MalformedURLException e) {
        throw new WorkflowOperationException("Can't create url to captions hold state ui: " + opencapsUrl, e);
      }
    else
      throw new WorkflowOperationException("No url to captions hold state specified");
  }
  
  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.ResumableWorkflowOperationHandler#getHoldActionTitle()
   */
  @Override
  public String getHoldActionTitle() {
    return HOLD_ACTION_TITLE;
  }

  /**
   * This will hold the results of a request for captionable media
   */
  public static class CaptionsResults {
    public int start;
    public int max;
    public int total;
    public List<OpencapsMediaItem> results;

    public CaptionsResults(List<OpencapsMediaItem> results, int start, int max, int total) {
      this.results = results;
      this.start = start;
      this.max = max;
      this.total = total;
    }
  }


}
