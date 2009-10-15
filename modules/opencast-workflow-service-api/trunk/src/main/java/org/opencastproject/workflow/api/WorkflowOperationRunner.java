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

/**
 * Contains the logic for handling a {@link WorkflowOperationDefinition}.  WorkflowOperationRunners are not guaranteed to be
 * thread safe.
 */
public interface WorkflowOperationRunner {

  /**
   * Runs a single operation within a given {@link WorkflowInstance}
   * 
   * @param workflowInstance The workflow instance 
   * @return The media package, including any changes made to the media package as a result of this workflow
   * operation's execution.
   */
  public MediaPackage run(WorkflowInstance workflowInstance);
}
