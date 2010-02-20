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

import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;

/**
 * A query used for finding workflow instances.  See {@link WorkflowService#getWorkflowInstances(WorkflowQuery)}
 */
public interface WorkflowQuery {

  /** Include a limit for the number of items to return in the result */
  WorkflowQuery withCount(long count);

  /** Include a paging offset for the items returned */
  WorkflowQuery withStartPage(long startPage);

  /** Limit results to workflow instances matching a free text search */
  WorkflowQuery withText(String text);

  /** Limit results to workflow instances in a specific state */
  WorkflowQuery withState(WorkflowState state);

  /** Limit results to workflow instances for a specific episode */
  WorkflowQuery withEpisode(String episodeId);

  /** Limit results to workflow instances for a specific series */
  WorkflowQuery withSeries(String seriesId);

  /** Limit results to workflow instances for a specific media package */
  WorkflowQuery withMediaPackage(String mediaPackageId);

  /** Limit results to workflow instances that are currently handling the specified operation */
  WorkflowQuery withCurrentOperation(String currentOperation);

  /**
   * Add the existence (or not) of a media package element of this type and flavor
   * @param elementType The type of element (e.g. track, catalog, attachment)
   * @param elementFlavor The element flavor (e.g. metadata/dublincore)
   * @param exists Whether the query should look for workflows where this element exists, or is absent
   * @return The updated WorkflowQuery
   */
  WorkflowQuery withElement(String elementType, String elementFlavor, boolean exists);

}
