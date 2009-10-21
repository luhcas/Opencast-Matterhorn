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

import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResultBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The workflow definition for handling "distribute" operations
 */
public class DistributeWorkflowOperationHandler implements WorkflowOperationHandler {
  private static final Logger logger = LoggerFactory.getLogger(DistributeWorkflowOperationHandler.class);

  public WorkflowOperationResult run(final WorkflowInstance workflowInstance) throws WorkflowOperationException {
    logger.info("run() distribution workflow operation");
    if(workflowInstance.getProperties() != null && workflowInstance.getProperties().keySet().contains("itunes")) {
      logger.info("Distributing media to itunes for media package " + workflowInstance.getSourceMediaPackage().getIdentifier());
    }
    if(workflowInstance.getProperties() != null && workflowInstance.getProperties().keySet().contains("youtube")) {
      logger.info("Distributing media to youTube for media package " + workflowInstance.getSourceMediaPackage().getIdentifier());
    }
    if(workflowInstance.getProperties() == null || workflowInstance.getProperties().isEmpty()) {
      logger.info("This workflow contains no properties, so we can't distribute any media");
    }
    // TODO Add any distributed media to the media package
    return WorkflowOperationResultBuilder.build(workflowInstance.getSourceMediaPackage(), workflowInstance.getProperties(), false);
  }
}
