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

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.util.NotFoundException;

import java.util.List;
import java.util.Map;

/**
 * Manages {@link WorkflowDefinition}s and {@link WorkflowInstance}s.
 */
public interface WorkflowService {
  /**
   * The service registration property we use to identify which workflow operation a {@link WorkflowOperationHandler}
   * should handle.
   */
  final String WORKFLOW_OPERATION_PROPERTY = "workflow.operation";

  /**
   * The job identifier for running workflow instances.
   */
  final String JOB_TYPE = "org.opencastproject.composer";

  /**
   * Returns the {@link WorkflowDefinition} identified by <code>name</code> or <code>null</code> if no such definition
   * was found.
   * 
   * @param id
   *          the workflow definition id
   * @return the workflow
   * @throws WorkflowDatabaseException
   *           if there is a problem accessing the workflow definition
   * @throws NotFoundException
   *           if there is no registered workflow definition with this identifier
   * 
   */
  WorkflowDefinition getWorkflowDefinitionById(String id) throws WorkflowDatabaseException, NotFoundException;

  /**
   * Gets a {@link WorkflowInstance} by its ID.
   * 
   * @return the workflow instance
   * @throws WorkflowDatabaseException
   *           if there is a problem accessing the workflow instance from persistence
   * @throws NotFoundException
   *           if there is no workflow instance with this identifier
   */
  WorkflowInstance getWorkflowById(String workflowId) throws WorkflowDatabaseException, NotFoundException;

  /**
   * Finds workflow instances based on the specified query.
   * 
   * @param query
   *          The query parameters
   * @return The {@link WorkflowSet} containing the workflow instances matching the query parameters
   * @throws WorkflowDatabaseException
   *           if there is a problem accessing the workflow instances from persistence
   */
  WorkflowSet getWorkflowInstances(WorkflowQuery query) throws WorkflowDatabaseException;

  /**
   * Creates a new workflow instance and starts the workflow.
   * 
   * @param workflowDefinition
   *          the workflow definition
   * @param mediaPackage
   *          the mediapackage to process
   * @param properties
   *          any properties to apply to the workflow definition
   * @return The new workflow instance
   * @throws WorkflowDatabaseException
   *           if there is a problem storing the workflow instance in persistence
   */
  WorkflowInstance start(WorkflowDefinition workflowDefinition, MediaPackage mediaPackage,
          Map<String, String> properties) throws WorkflowDatabaseException;

  /**
   * Creates a new workflow instance and starts the workflow.
   * 
   * @param workflowDefinition
   *          the workflow definition
   * @param mediaPackage
   *          the mediapackage to process
   * @param parentWorkflowId
   *          An existing workflow to associate with the new workflow instance
   * @param properties
   *          any properties to apply to the workflow definition
   * @return The new workflow instance
   * @throws NotFoundException
   *           if the parent workflow does not exist
   * @throws WorkflowDatabaseException
   *           if there is a problem storing the workflow instance in persistence
   */
  WorkflowInstance start(WorkflowDefinition workflowDefinition, MediaPackage mediaPackage, String parentWorkflowId,
          Map<String, String> properties) throws WorkflowDatabaseException, NotFoundException;

  /**
   * Creates a new workflow instance and starts the workflow.
   * 
   * @param workflowDefinition
   *          the workflow definition
   * @param mediaPackage
   *          the mediapackage to process
   * @return The new workflow instance
   * @throws WorkflowDatabaseException
   *           if there is a problem storing the workflow instance in persistence
   */
  WorkflowInstance start(WorkflowDefinition workflowDefinition, MediaPackage mediaPackage)
          throws WorkflowDatabaseException;

  /**
   * Creates a new workflow instance using the default workflow definition (which can be configured through the use of a
   * {@link WorkflowSelectionStrategy}), and starts the workflow. The supplied properties are applied to the workflow
   * instance immediately.
   * 
   * @param mediaPackage
   *          the mediapackage to process
   * @param properties
   *          any properties to apply to the workflow definition
   * @return The new workflow instance
   * @throws WorkflowDatabaseException
   *           if there is a problem storing the workflow instance in persistence
   */
  WorkflowInstance start(MediaPackage mediaPackage, Map<String, String> properties) throws WorkflowDatabaseException;

  /**
   * Creates a new workflow instance using the default workflow definition (which can be configured through the use of a
   * {@link WorkflowSelectionStrategy}), and starts the workflow.
   * 
   * @param mediaPackage
   *          the mediapackage to process
   * @return The new workflow instance
   * @throws WorkflowDatabaseException
   *           if there is a problem storing the workflow instance in persistence
   */
  WorkflowInstance start(MediaPackage mediaPackage) throws WorkflowDatabaseException;

  /**
   * Gets the total number of workflows that have been created to date.
   * 
   * @return The number of workflow instances, regardless of their state
   * @throws WorkflowDatabaseException
   *           if there is a problem accessing the workflow instances in persistence
   */
  long countWorkflowInstances() throws WorkflowDatabaseException;

  /**
   * Stops a running workflow instance.
   * 
   * @param workflowInstanceId
   *          the workflow instance identifier
   * @throws NotFoundException
   *           if no running workflow with this identifier exists
   * @throws WorkflowDatabaseException
   *           if there is a problem accessing the workflow instance in persistence
   */
  void stop(String workflowInstanceId) throws WorkflowDatabaseException, NotFoundException;

  /**
   * Temporarily suspends a started workflow instance.
   * 
   * @param workflowInstanceId
   *          the workflow instance identifier
   * @throws NotFoundException
   *           if no running workflow with this identifier exists
   * @throws WorkflowDatabaseException
   *           if there is a problem accessing the workflow instance in persistence
   */
  void suspend(String workflowInstanceId) throws WorkflowDatabaseException, NotFoundException;

  /**
   * Resumes a suspended workflow instance.
   * 
   * @param workflowInstanceId
   *          the workflow instance identifier
   * @throws NotFoundException
   *           if no paused workflow with this identifier exists
   * @throws WorkflowDatabaseException
   *           if there is a problem accessing the workflow instance in persistence
   */
  void resume(String workflowInstanceId) throws NotFoundException, WorkflowDatabaseException;

  /**
   * Resumes a suspended workflow instance, applying new properties to the workflow.
   * 
   * @param workflowInstanceId
   *          the workflow to resume
   * @param properties
   *          the properties to apply to the resumed workflow
   * @throws NotFoundException
   *           if no paused workflow with this identifier exists
   * @throws WorkflowDatabaseException
   *           if there is a problem accessing the workflow instance in persistence
   */
  void resume(String workflowInstanceId, Map<String, String> properties) throws NotFoundException,
          WorkflowDatabaseException;

  /**
   * Updates the given workflow instance with regard to the media package, the properties and the operations involved.
   * 
   * @param workflowInstance
   *          the workflow instance
   * @throws WorkflowDatabaseException
   *           if there is a problem storing the workflow instance in persistence
   */
  void update(WorkflowInstance workflowInstance) throws WorkflowDatabaseException;

  /**
   * Gets the list of available workflow definitions. In order to be "available", a workflow definition must be
   * registered and must have registered workflow operation handlers for each of the workflow definition's operations.
   * 
   * @return The list of currently available workflow definitions, sorted by title
   * @throws WorkflowDatabaseException
   *           if there is a problem storing the registered workflow definitions
   */
  List<WorkflowDefinition> listAvailableWorkflowDefinitions() throws WorkflowDatabaseException;
}
