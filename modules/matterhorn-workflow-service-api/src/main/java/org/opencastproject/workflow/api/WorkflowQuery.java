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
  long getCount();

  /** Include a paging offset for the items returned */
  WorkflowQuery withStartPage(long startPage);
  long getStartPage();
  
  /** Limit results to workflow instances matching a free text search */
  WorkflowQuery withText(String text);
  String getText();

  /** Limit results to workflow instances in a specific state */
  WorkflowQuery withState(WorkflowState state);
  WorkflowState getState();

  /** Limit results to workflow instances for a specific series */
  WorkflowQuery withSeries(String seriesId);
  String getSeries();

  /** Limit results to workflow instances for a specific media package */
  WorkflowQuery withMediaPackage(String mediaPackageId);
  String getMediaPackage();

  /** Limit results to workflow instances that are currently handling the specified operation */
  WorkflowQuery withCurrentOperation(String currentOperation);
  String getCurrentOperation();
}
