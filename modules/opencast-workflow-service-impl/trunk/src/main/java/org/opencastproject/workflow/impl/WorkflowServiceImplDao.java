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
package org.opencastproject.workflow.impl;

import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowSet;

/**
 * Provides persistence services to the workflow service implementation.
 */
public interface WorkflowServiceImplDao {
  
  /**
   * Update the workflow instance, or add it to persistence if it is not already stored.
   * @param instance The workflow instance to store
   */
  void update(WorkflowInstance instance);
  
  /**
   * Remove the workflow instance with this id.
   * @param id The workflow instance id
   */
  void remove(String id);

  /**
   * Gets a {@link WorkflowInstace} by its ID.
   */
  WorkflowInstance getWorkflowById(String workflowId);

  /**
   * Gets the total number of workflows that have been created to date.
   * 
   * @return The number of workflow instances, regardless of their state
   */
  long countWorkflowInstances();

  /**
   * @param query
   * @return
   */
  WorkflowSet getWorkflowInstances(WorkflowQuery query);

}
