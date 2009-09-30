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
package org.opencastproject.workflow.api;

import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.workflow.api.WorkflowInstance.State;

import java.util.List;
import java.util.Map;

/**
 * Manages {@link WorkflowDefinition}s and {@link WorkflowInstance}s.
 */
public interface WorkflowService {
  
  /**
   * Gets a {@link WorkflowInstace} by its ID.
   */
  WorkflowInstance getWorkflowInstance(String id);

  /**
   * Gets all known {@link WorkflowOperation}s that can be combined to create a {@link WorkflowInstance}.
   * @return The {@link List} of all known {@link WorkflowOperation}s
   */
  List<WorkflowOperation> getWorkflowOperations();
  
  /**
   * List all {@link WorkflowInstance}s that are currently in the given {@link State}.
   * 
   * TODO Implement paging
   * 
   * @return The list of {@link WorkflowInstance}s in this {@link State}.
   */
  List<WorkflowInstance> getWorkflowInstances(State state);

  /**
   * Creates a new workflow instance and starts the workflow.
   * 
   * @param workflowDefinitionId
   * @param mediaPackage
   * @param properties
   * @return The new workflow instance
   */
  WorkflowInstance start(WorkflowDefinition workflowDefinition, MediaPackage mediaPackage, Map<String, String> properties);

  /**
   * Stops a running workflow instance.
   * 
   * @param workflowInstanceId
   */
  void stop(String workflowInstanceId);

  /**
   * Temporarily suspends a started workflow instance.
   * 
   * @param workflowInstanceId
   */
  void suspend(String workflowInstanceId);

  /**
   * Resumes a suspended workflow instance.
   * 
   * @param workflowInstanceId
   */
  void resume(String workflowInstanceId);
  
  // TODO Add the findBy* methods once the search service is available.
  
}
