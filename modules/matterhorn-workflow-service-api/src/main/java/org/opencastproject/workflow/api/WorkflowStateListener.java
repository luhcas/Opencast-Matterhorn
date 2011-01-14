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

  /** The workflow instance identifiers to monitor */
  protected final Set<Long> workflowInstanceIds;

  /** The states that this listener respond to with a notify() */
  protected final Set<WorkflowState> notifyStates;

  /** The number of state change events that have triggered a notify() */
  protected int counter;

  /**
   * Constructs a workflow listener that notifies for any state change to any workflow instance.
   */
  public WorkflowStateListener() {
    workflowInstanceIds = Collections.unmodifiableSet(new HashSet<Long>());
    notifyStates = Collections.unmodifiableSet(new HashSet<WorkflowState>());
  }

  /**
   * Constructs a workflow listener that notifies for any state change to a single workflow instance.
   * 
   * @param workflowInstanceId
   *          the workflow identifier
   */
  public WorkflowStateListener(Long workflowInstanceId) {
    Set<Long> ids = new HashSet<Long>();
    if (workflowInstanceId != null) {
      ids.add(workflowInstanceId);
    }
    workflowInstanceIds = Collections.unmodifiableSet(ids);
    notifyStates = Collections.unmodifiableSet(new HashSet<WorkflowState>());
  }

  /**
   * Constructs a workflow listener for a single workflow instance. The listener may be configured to be notified on
   * state changes.
   * 
   * @param workflowInstanceId
   *          the workflow identifier
   * @param notifyState
   *          the workflow state change that should trigger this listener. If null, any state change will trigger the
   *          listener.
   */
  public WorkflowStateListener(Long workflowInstanceId, WorkflowState notifyState) {
    Set<Long> ids = new HashSet<Long>();
    if (workflowInstanceId != null) {
      ids.add(workflowInstanceId);
    }
    workflowInstanceIds = Collections.unmodifiableSet(ids);

    if (notifyState == null) {
      notifyStates = Collections.unmodifiableSet(new HashSet<WorkflowState>());
    } else {
      Set<WorkflowState> set = new HashSet<WorkflowState>();
      set.add(notifyState);
      notifyStates = Collections.unmodifiableSet(set);
    }
  }

  /**
   * Constructs a workflow listener for all workflow instances. The listener may be configured to be notified on a set
   * of specific state changes.
   * 
   * @param notifyStates
   *          the workflow state changes that should trigger this listener. If null, any state change will trigger the
   *          listener.
   */
  public WorkflowStateListener(Set<WorkflowState> notifyStates) {
    workflowInstanceIds = Collections.unmodifiableSet(new HashSet<Long>());

    if (notifyStates == null) {
      this.notifyStates = Collections.unmodifiableSet(new HashSet<WorkflowState>());
    } else {
      this.notifyStates = Collections.unmodifiableSet(notifyStates);
    }
  }

  /**
   * Constructs a workflow listener for all workflow instances. The listener may be configured to be notified on a of
   * specific state changes.
   * 
   * @param notifyState
   *          the workflow state change that should trigger this listener. If null, any state change will trigger the
   *          listener.
   */
  public WorkflowStateListener(WorkflowState notifyState) {
    workflowInstanceIds = Collections.unmodifiableSet(new HashSet<Long>());

    if (notifyState == null) {
      this.notifyStates = Collections.unmodifiableSet(new HashSet<WorkflowState>());
    } else {
      Set<WorkflowState> states = new HashSet<WorkflowInstance.WorkflowState>();
      states.add(notifyState);
      this.notifyStates = Collections.unmodifiableSet(states);
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
  public WorkflowStateListener(Long workflowInstanceId, Set<WorkflowState> notifyStates) {
    Set<Long> ids = new HashSet<Long>();
    if (workflowInstanceId != null) {
      ids.add(workflowInstanceId);
    }
    workflowInstanceIds = Collections.unmodifiableSet(ids);

    if (notifyStates == null) {
      this.notifyStates = Collections.unmodifiableSet(new HashSet<WorkflowState>());
    } else {
      this.notifyStates = Collections.unmodifiableSet(notifyStates);
    }
  }

  /**
   * Constructs a workflow listener for a set of workflow instances. The listener may be configured to be notified on a
   * set of specific state changes.
   * 
   * @param workflowInstanceIds
   *          the workflow identifiers
   * @param notifyStates
   *          the workflow state changes that should trigger this listener. If null, any state change will trigger the
   *          listener.
   */
  public WorkflowStateListener(Set<Long> workflowInstanceIds, Set<WorkflowState> notifyStates) {
    if (workflowInstanceIds == null) {
      workflowInstanceIds = new HashSet<Long>();
    }
    this.workflowInstanceIds = Collections.unmodifiableSet(workflowInstanceIds);
    if (notifyStates == null) {
      this.notifyStates = Collections.unmodifiableSet(new HashSet<WorkflowState>());
    } else {
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
    if (!workflowInstanceIds.isEmpty() && !workflowInstanceIds.contains(workflow.getId()))
      return;
    if (notifyStates != null) {
      WorkflowState currentState = workflow.getState();
      if (!notifyStates.contains(currentState)) {
        return;
      }
    }
    synchronized (this) {
      logger.debug("Workflow {} state updated to {}", workflow.getId(), workflow.getState());
      counter++;
      notifyAll();
    }
  }

  /**
   * Returns the number of state changes that this listener has observed without ignoring.
   * 
   * @return the counter
   */
  public int countStateChanges() {
    return counter;
  }
}
