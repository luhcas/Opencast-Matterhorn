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

import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowSet;
import org.opencastproject.workflow.api.WorkflowInstance.State;

import java.io.File;

/**
 * Provides persistence services to the workflow service implementation.
 */
public interface WorkflowServiceImplDao {
  /**
   * Activate this DAO using the given filestorage directory.  It is the DAO implementation's responsibility to create
   * this directory if necessary.
   */
  void activate(File storageDirectory);
  
  /**
   * Deactivate this DAO
   */
  void deactivate();
  
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
   * Gets {@link WorkflowInstace}s associated with a media package ID.
   */
  WorkflowSet getWorkflowsByMediaPackage(String mediaPackageId);

  /**
   * List all {@link WorkflowInstance}s that are currently in the given {@link State}.
   * 
   * TODO Implement paging
   * 
   * @return The list of {@link WorkflowInstance}s in this {@link State}.
   */
  WorkflowSet getWorkflowsInState(State state, int offset, int limit);

  /**
   * Returns the {@link WorkflowInstance}s ordered by date (descending).
   * 
   * @param offset
   *          starting position of the result set
   * @param limit
   *          the maximum size of the result set
   * @return the resultset
   * @throws WorkflowDatabaseException
   *           if the lookup fails
   */
  WorkflowSet getWorkflowsByDate(int offset, int limit) throws WorkflowDatabaseException;

  /**
   * Returns the {@link WorkflowInstance}s deal with the specified episode.
   * 
   * @param episodeId
   *          the episode identifier
   * @return the resultset
   * @throws WorkflowDatabaseException
   *           if the lookup fails
   */
  WorkflowSet getWorkflowsByEpisode(String episodeId) throws WorkflowDatabaseException;

  /**
   * Returns the {@link WorkflowInstance}s deal with the specified series.
   * 
   * @param seriesId
   *          the the series identifier
   * @return the resultset
   * @throws WorkflowDatabaseException
   *           if the lookup fails
   */
  WorkflowSet getWorkflowsBySeries(String seriesId) throws WorkflowDatabaseException;

  /**
   * Returns the {@link WorkflowInstance}s that deal with episodes matching the specified search terms.
   * 
   * @param offset
   *          starting position of the result set
   * @param limit
   *          the maximum size of the result set
   * @return the resultset
   * @throws WorkflowDatabaseException
   *           if the lookup fails
   */
  WorkflowSet getWorkflowsByText(String text, int offset, int limit) throws WorkflowDatabaseException;

  /**
   * Gets the total number of workflows that have been created to date.
   * 
   * @return The number of workflow instances, regardless of their state
   */
  long countWorkflowInstances();

}
