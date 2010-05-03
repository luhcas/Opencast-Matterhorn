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
package org.opencastproject.workflow.api;


import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;

/**
 * Abstract base implementation for an operation handler, which implements a simple start operation that returns a
 * {@link WorkflowOperationResult} with the current mediapackage and {@link Action#CONTINUE}.
 */
public abstract class AbstractWorkflowOperationHandler implements WorkflowOperationHandler {

  /** The ID of this operation handler */
  protected String id;

  /** The description of what this handler actually does */
  protected String description;

  /**
   * Activates this component with its properties once all of the collaborating services have been set
   * @param cc The component's context, containing the properties used for configuration
   */
  protected void activate(ComponentContext cc) {
    this.id = (String)cc.getProperties().get(WorkflowService.WORKFLOW_OPERATION_PROPERTY);
    this.description = (String)cc.getProperties().get(Constants.SERVICE_DESCRIPTION);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance) throws WorkflowOperationException {
    return WorkflowBuilder.getInstance().buildWorkflowOperationResult(workflowInstance.getMediaPackage(), Action.CONTINUE);
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#destroy(org.opencastproject.workflow.api.WorkflowInstance)
   */
  @Override
  public void destroy(WorkflowInstance workflowInstance) throws WorkflowOperationException {}
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#getId()
   */
  @Override
  public String getId() {
    return id;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#getDescription()
   */
  @Override
  public String getDescription() {
    return description;
  }
  

  /**
   * {@inheritDoc}
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "'" + getId() + "' Operation Handler";
  }
  
}
