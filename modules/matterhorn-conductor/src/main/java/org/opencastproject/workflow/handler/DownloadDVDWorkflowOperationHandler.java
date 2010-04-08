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
package org.opencastproject.workflow.handler;

import org.opencastproject.workflow.api.AbstractResumableWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Operation that holds for download of DVD image
 */
public class DownloadDVDWorkflowOperationHandler extends AbstractResumableWorkflowOperationHandler {

  private static final Logger logger = LoggerFactory.getLogger(DownloadDVDWorkflowOperationHandler.class);

  /** Path to the hold ui resources */
  private static final String HOLD_UI_PATH = "/operation/ui/download-dvd/index.html";

  public void setHttpService(HttpService service) {
    super.httpService = service;
  }
  
  public void setHttpContext(HttpContext httpContext) {
    super.httpContext = httpContext;
  }

  public void activate(ComponentContext cc) {
    super.activate(cc);
    registerHoldStateUserInterface(HOLD_UI_PATH);
    logger.info("Registering download-DVD hold state ui as {}", getHoldStateUserInterfaceURL());
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance) throws WorkflowOperationException {
    logger.info("Holding for download of DVD image...");
    return WorkflowBuilder.getInstance().buildWorkflowOperationResult(Action.PAUSE);
  }
}
