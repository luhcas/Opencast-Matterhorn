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
package org.opencastproject.workflow.impl;

import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowStatistics;

import java.util.Map;

/**
 * Default implementation of the {@link WorkflowStatistics} interface.
 */
public class WorkflowStatisticsImpl implements WorkflowStatistics {

  long total = 0;
  
  long instantiated = 0;

  long running = 0;
  
  long paused = 0;

  long stopped = 0;

  long succeeded = 0;

  long failed = 0;

  long failing = 0;

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowStatistics#getCountByStates()
   */
  @Override
  public Map<WorkflowState, Long> getCountByStates() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowStatistics#getCountByWorkflowDefinition()
   */
  @Override
  public Map<String, Map<String, Long>> getCountByWorkflowDefinition() {
    // TODO Auto-generated method stub
    return null;
  }

}
