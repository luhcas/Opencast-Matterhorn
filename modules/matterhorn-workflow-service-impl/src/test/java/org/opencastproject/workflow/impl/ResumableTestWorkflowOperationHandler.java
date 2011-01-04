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
package org.opencastproject.workflow.impl;

import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.ResumableWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import java.net.URL;
import java.util.Map;

/**
 * Implementation to be used by test cases.
 */
public class ResumableTestWorkflowOperationHandler extends AbstractWorkflowOperationHandler implements
        ResumableWorkflowOperationHandler {

  protected Action startAction = null;

  protected Action resumeAction = null;

  /** True if the start() method has been called */
  private boolean started = false;

  /** True if the resume() method has been called */
  private boolean resumed = false;

  /**
   * Creates a new workflow operation handler that will pause on <code>start()</code> and continue on
   * <code>resume()</code>.
   */
  public ResumableTestWorkflowOperationHandler() {
    this("test", Action.PAUSE, Action.CONTINUE);
  }

  /**
   * Creates a new workflow operation handler that will return <code>startAction</code> on <code>start()</code> and
   * <code>resumeAction</code> on <code>resume()</code>.
   */
  public ResumableTestWorkflowOperationHandler(Action startAction, Action resumeAction) {
    this("test", startAction, resumeAction);
  }

  /**
   * Creates a new workflow operation handler that will return <code>startAction</code> on <code>start()</code> and
   * <code>resumeAction</code> on <code>resume()</code>.
   */
  public ResumableTestWorkflowOperationHandler(String id, Action startAction, Action resumeAction) {
    super.id = id;
    this.startAction = startAction;
    this.resumeAction = resumeAction;
  }

  /**
   * Sets the action that is returned on {@link #start(WorkflowInstance)}.
   * 
   * @param startAction
   *          the start action
   */
  void setStartAction(Action startAction) {
    this.startAction = startAction;
  }

  /**
   * Sets the action that is returned on {@link #resume(WorkflowInstance, Map)}.
   * 
   * @param resumeAction
   *          the resume action
   */
  void setResumeAction(Action resumeAction) {
    this.resumeAction = resumeAction;
  }

  /**
   * Returns <code>true</code> if the resume() method has been called.
   * 
   * @return <code>true</code> if the handler was resumed
   */
  public boolean isResumed() {
    return resumed;
  }

  /**
   * Returns <code>true</code> if the start() method has been called.
   * 
   * @return <code>true</code> if the handler was started
   */
  public boolean isStarted() {
    return started;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.AbstractWorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance) throws WorkflowOperationException {
    started = true;
    return createResult(startAction);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.ResumableWorkflowOperationHandler#resume(org.opencastproject.workflow.api.WorkflowInstance,
   *      java.util.Map)
   */
  @Override
  public WorkflowOperationResult resume(WorkflowInstance workflowInstance, Map<String, String> properties)
          throws WorkflowOperationException {
    resumed = true;
    return createResult(resumeAction);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.ResumableWorkflowOperationHandler#getHoldStateUserInterfaceURL(org.opencastproject.workflow.api.WorkflowInstance)
   */
  @Override
  public URL getHoldStateUserInterfaceURL(WorkflowInstance workflowInstance) throws WorkflowOperationException {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.ResumableWorkflowOperationHandler#getHoldActionTitle()
   */
  @Override
  public String getHoldActionTitle() {
    return "Test action";
  }

}
