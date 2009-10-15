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
package org.opencastproject.conductor.impl;

import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationRunner;
import org.opencastproject.workflow.api.WorkflowOperationRunnerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Comment me!
 *
 */
public class ComposeWorkflowOperationRunnerFactory implements WorkflowOperationRunnerFactory {
  private static final Logger logger = LoggerFactory.getLogger(ComposeWorkflowOperationRunnerFactory.class);


  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowOperationRunnerFactory#getRunner(org.opencastproject.workflow.api.WorkflowInstance)
   */
  public WorkflowOperationRunner getRunner() {
    return new WorkflowOperationRunner() {
      
      public MediaPackage run(WorkflowInstance workflowInstance) {
        logger.info("run() compose workflow operation");
        if(workflowInstance.getProperties() != null && workflowInstance.getProperties().keySet().contains("flash")) {
          logger.info("Composing flash media for media package " + workflowInstance.getSourceMediaPackage().getIdentifier());
        } else {
          logger.info("Skipping flash media composition for media package " + workflowInstance.getSourceMediaPackage().getIdentifier());
        }
        if(workflowInstance.getProperties() == null || workflowInstance.getProperties().isEmpty()) {
          logger.info("This workflow contains no properties, so we can't compose any media");
        }
        // TODO Add new media track to the media package
        return workflowInstance.getSourceMediaPackage();
      }
    };
  }
}
