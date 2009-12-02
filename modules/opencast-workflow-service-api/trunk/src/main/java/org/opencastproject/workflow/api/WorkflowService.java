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

import java.util.Map;

/**
 * Manages {@link WorkflowDefinition}s and {@link WorkflowInstance}s.
 */
public interface WorkflowService {

  /**
   * Returns the {@link WorkflowDefinition} identified by <code>name</code> or <code>null</code> if no such definition
   * was found.
   * 
   * @param name
   *          the workflow definition name
   * @return the workflow
   */
  WorkflowDefinition getWorkflowDefinitionByName(String name);

  /**
   * Gets a {@link WorkflowInstace} by its ID.
   */
  WorkflowInstance getWorkflowById(String workflowId);

//  /**
//   * Gets {@link WorkflowInstace}s associated with a media package ID.
//   */
//  WorkflowSet getWorkflowsByMediaPackage(String mediaPackageId);
//
//  /**
//   * List all {@link WorkflowInstance}s that are currently in the given {@link State}.
//   * 
//   * TODO Implement paging
//   * 
//   * @return The list of {@link WorkflowInstance}s in this {@link State}.
//   */
//  WorkflowSet getWorkflowsInState(State state, int offset, int limit);
//
//  /**
//   * Returns the {@link WorkflowInstance}s ordered by date (descending).
//   * 
//   * @param offset
//   *          starting position of the result set
//   * @param limit
//   *          the maximum size of the result set
//   * @return the resultset
//   * @throws WorkflowDatabaseException
//   *           if the lookup fails
//   */
//  WorkflowSet getWorkflowsByDate(int offset, int limit) throws WorkflowDatabaseException;
//
//  /**
//   * Returns the {@link WorkflowInstance}s deal with the specified episode.
//   * 
//   * @param episodeId
//   *          the episode identifier
//   * @return the resultset
//   * @throws WorkflowDatabaseException
//   *           if the lookup fails
//   */
//  WorkflowSet getWorkflowsByEpisode(String episodeId) throws WorkflowDatabaseException;
//
//  /**
//   * Returns the {@link WorkflowInstance}s deal with the specified series.
//   * 
//   * @param seriesId
//   *          the the series identifier
//   * @return the resultset
//   * @throws WorkflowDatabaseException
//   *           if the lookup fails
//   */
//  WorkflowSet getWorkflowsBySeries(String seriesId) throws WorkflowDatabaseException;
//
//  /**
//   * Returns the {@link WorkflowInstance}s that deal with episodes matching the specified search terms.
//   * 
//   * @param text
//   *          the search term
//   * @param offset
//   *          starting position of the result set
//   * @param limit
//   *          the maximum size of the result set
//   * @return the resultset
//   * @throws WorkflowDatabaseException
//   *           if the lookup fails
//   */
//  WorkflowSet getWorkflowsByText(String text, int offset, int limit) throws WorkflowDatabaseException;
//
//  /**
//   * Returns the {@link WorkflowInstance}s that deal with episodes matching the specified search terms and are in the
//   * specified state.
//   * 
//   * @param state
//   *          the workflow state
//   * @param text
//   *          the search term
//   * @param offset
//   *          starting position of the result set
//   * @param limit
//   *          the maximum size of the result set
//   * @return the resultset
//   * @throws WorkflowDatabaseException
//   *           if the lookup fails
//   */
//  WorkflowSet getWorkflowsByTextAndState(State state, String text, int offset, int limit) throws WorkflowDatabaseException;

  /**
   * Finds workflow instances based on the specified query.
   * @param query The query parameters
   * @return The {@link WorkflowSet} containing the workflow instances matching the query parameters
   */
  WorkflowSet getWorkflowInstances(WorkflowQuery query);
  
  /**
   * Constructs a new {@link WorkflowQuery}
   * @return The {@link WorkflowQuery}
   */
  WorkflowQuery newWorkflowQuery();
  
  /**
   * Creates a new workflow instance and starts the workflow.
   * 
   * @param workflowDefinitionId
   * @param mediaPackage
   * @param properties
   * @return The new workflow instance
   */
  WorkflowInstance start(WorkflowDefinition workflowDefinition, MediaPackage mediaPackage,
          Map<String, String> properties);

  /**
   * Gets the total number of workflows that have been created to date.
   * 
   * @return The number of workflow instances, regardless of their state
   */
  long countWorkflowInstances();
  
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

  /**
   * Updates the given workflow instance with regard to the media package, the properties and the operations involved.
   * 
   * @param workflowInstance
   *          the workflow instance
   */
  void update(WorkflowInstance workflowInstance);

  /**
   * Gets the list of available workflow definitions. In order to be "available", a workflow definition must be
   * registered and must have registered workflow operation handlers for each of the workflow definition's operations.
   * 
   * @return The list of currently available workflow definitions, sorted by title
   */
  WorkflowDefinitionList listAvailableWorkflowDefinitions();

  /**
   * Whether a workflow definition may be run at this moment. Every {@link WorkflowOperationDefinition} returned by
   * {@link WorkflowDefinition#getOperations()} must be registered as a {@link WorkflowOperationHandler} for this
   * {@link WorkflowDefinition} to be runnable.
   * 
   * @param workflowDefinition
   *          The workflow definition to inspect for runnability
   * @return Whether this workflow may be run
   */
  boolean isRunnable(WorkflowDefinition workflowDefinition);

  /**
   * Registers a new {@link WorkflowDefinition}
   * 
   * @param definition
   *          The definition to register
   */
  void registerWorkflowDefinition(WorkflowDefinition definition);

  /**
   * Removes the {@link WorkflowDefinition} specified by this title from the list of available
   * {@link WorkflowDefinition}s.
   * 
   * @param title
   *          The title of the {@link WorkflowDefinition} to unregister
   */
  void unregisterWorkflowDefinition(String title);
}
