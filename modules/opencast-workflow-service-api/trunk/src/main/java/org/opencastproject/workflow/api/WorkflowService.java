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

import java.util.List;

/**
 * Manages {@link WorkflowDefinition}s and {@link WorkflowInstance}s.
 */
public interface WorkflowService {

  /**
   * Gets a {@link WorkflowDefinition} by its ID.
   */
  WorkflowDefinition getWorkflowDefinition(String id);

  /**
   * Register a {@link WorkflowDefinition}.
   * 
   * @param id The ID of the entity
   * @param entity The entity to save
   */
  void registerWorkflowDefinition(WorkflowDefinition workflowDefinition);

  /**
   * @return ALl registered {@link WorkflowDefinition}s
   */
  List<WorkflowDefinition> fetchAllWorkflowDefinitions();
  
  /**
   * Gets a {@link WorkflowInstace} by its ID.
   */
  WorkflowInstance getWorkflowInstance(String id);
  
  /**
   * For a given workflow definition ID, list all associated {@link WorkflowInstance}s.
   * 
   * TODO Implement paging
   * 
   * @param workflowDefinitionId
   * @return
   */
  List<WorkflowInstance> fetchAllWorkflowInstances(String workflowDefinitionId);

  /**
   * Creates a new workflow instance and starts the workflow.
   * 
   * @param workflowDefinitionId
   * @param mediaPackage
   * @return The new workflow instance
   */
  WorkflowInstance start(WorkflowDefinition workflowDefinition, MediaPackage mediaPackage);

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

