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
import org.opencastproject.workflow.api.WorkflowOperationDefinition;
import org.opencastproject.workflow.api.WorkflowOperationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The workflow definition for handling "compose" operations
 */
public class ComposeWorkflowOperationDefinition implements WorkflowOperationDefinition {
  private static final Logger logger = LoggerFactory.getLogger(ComposeWorkflowOperationDefinition.class);
  protected String name = "compose";
  protected String description = "Compose new media file(s)"; // TODO i18n

  public MediaPackage run(WorkflowInstance workflowInstance) throws WorkflowOperationException {
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

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowOperationDefinition#getDescription()
   */
  public String getDescription() {
    return description;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowOperationDefinition#getName()
   */
  public String getName() {
    return name;
  }

  public String getExceptionHandlingWorkflow() {
    throw new UnsupportedOperationException("This should be called from the instance-level workflow operation");
  }

  public boolean isFailWorkflowOnException() {
    throw new UnsupportedOperationException("This should be called from the instance-level workflow operation");
  }
}
