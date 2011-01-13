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
package org.opencastproject.ingest.impl;

import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowListener;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A simple workflow listener implementation suitable for monitoring a workflow's state changes.
 */
public class WorkflowStateListener implements WorkflowListener {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(WorkflowStateListener.class);

  /** The workflow instance identifier */
  protected final long workflowInstanceId;

  /** Whether to notify for all state changes */
  protected final Boolean notifyForAllStateChanges;

  /** The states that this listener respond to with a notify() */
  protected final Set<WorkflowState> notifyStates;

  /** The set of unique workflow operations that the workflow has observed */
  protected Set<WorkflowOperationInstance> uniqueOperations = new HashSet<WorkflowOperationInstance>();

  /**
   * Constructs a workflow listener that notifies for any state or operation change to a single workflow instance.
   * 
   * @param workflowInstanceId
   *          the workflow identifier
   */
  public WorkflowStateListener(long workflowInstanceId) {
    this.workflowInstanceId = workflowInstanceId;
    notifyForAllStateChanges = true;
    notifyStates = null;
  }

  /**
   * Constructs a workflow listener for a single workflow instance. The listener may be configured to be notified on
   * state change.
   * 
   * @param workflowInstanceId
   *          the workflow identifier
   * @param notifyState
   *          the workflow state change that should trigger this listener. If null, any state change will trigger the
   *          listener.
   */
  public WorkflowStateListener(long workflowInstanceId, WorkflowState notifyState) {
    this.workflowInstanceId = workflowInstanceId;

    if (notifyState == null) {
      notifyForAllStateChanges = true;
      notifyStates = null;
    } else {
      notifyForAllStateChanges = false;
      Set<WorkflowState> set = new HashSet<WorkflowState>();
      set.add(notifyState);
      notifyStates = Collections.unmodifiableSet(set);
    }
  }

  /**
   * Constructs a workflow listener for a single workflow instance. The listener may be configured to be notified on a
   * set of specific state changes.
   * 
   * @param workflowInstanceId
   *          the workflow identifier
   * @param notifyStates
   *          the workflow state changes that should trigger this listener. If null, any state change will trigger the
   *          listener.
   */
  public WorkflowStateListener(long workflowInstanceId, Set<WorkflowState> notifyStates) {
    this.workflowInstanceId = workflowInstanceId;
    
    if (notifyStates == null || notifyStates.isEmpty()) {
      notifyForAllStateChanges = true;
      this.notifyStates = null;
    } else {
      notifyForAllStateChanges = false;
      this.notifyStates = Collections.unmodifiableSet(notifyStates);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowListener#operationChanged(org.opencastproject.workflow.api.WorkflowInstance)
   */
  @Override
  public void operationChanged(WorkflowInstance workflow) {
    logger.debug("No-op");
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowListener#stateChanged(org.opencastproject.workflow.api.WorkflowInstance)
   */
  @Override
  public void stateChanged(WorkflowInstance workflow) {
    if (workflow.getId() != workflowInstanceId)
      return;
    WorkflowOperationInstance currentOperation = workflow.getCurrentOperation();
    if (!notifyForAllStateChanges && notifyStates != null) {
      WorkflowState currentState = workflow.getState();
      if (!notifyStates.contains(currentState)) {
        return;
      }
    }
    synchronized (this) {
      logger.debug("Workflow {} state updated to {}", workflow.getId(), workflow.getState());
      if (currentOperation != null) {
        uniqueOperations.add(currentOperation);
      }
      notifyAll();
    }
  }

  /**
   * Returns the set of unique operations observed by this listener.
   * 
   * @return the uniqueOperations
   */
  public Set<WorkflowOperationInstance> getUniqueOperations() {
    synchronized (this) {
      return uniqueOperations;
    }
  }

  /**
   * Clears the stateChanges and operationChanges counts.
   */
  public void clear() {
    synchronized (this) {
      uniqueOperations.clear();
    }
  }
}
