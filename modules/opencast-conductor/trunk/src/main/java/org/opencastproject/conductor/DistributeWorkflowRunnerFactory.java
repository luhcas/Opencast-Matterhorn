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
package org.opencastproject.conductor;

import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowRunnerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Comment me!
 *
 */
public class DistributeWorkflowRunnerFactory implements WorkflowRunnerFactory {
  private static final Logger logger = LoggerFactory.getLogger(DistributeWorkflowRunnerFactory.class);

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowRunner#getRunnable(org.opencastproject.workflow.api.WorkflowInstance)
   */
  public Runnable getRunnable(final WorkflowInstance workflowInstance) {
    return new Runnable() {
      public void run() {
        logger.info("run() distribution workflow operation");
        if(workflowInstance.getProperties() != null && workflowInstance.getProperties().keySet().contains("itunes")) {
          logger.info("Distributing media to itunes for media package " + workflowInstance.getMediaPackage().getIdentifier());
        }
        if(workflowInstance.getProperties() != null && workflowInstance.getProperties().keySet().contains("youtube")) {
          logger.info("Distributing media to youTube for media package " + workflowInstance.getMediaPackage().getIdentifier());
        }
        if(workflowInstance.getProperties() == null || workflowInstance.getProperties().isEmpty()) {
          logger.info("This workflow contains no properties, so we can't distribute any media");
        }
      }
    };
  }
}
