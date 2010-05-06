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
package org.opencastproject.workflow;

import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowService;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

/**
 * This is the default implementation of the conductor service.
 */
public class WorkflowDefinitionLoader {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowDefinitionLoader.class);

  private WorkflowService workflowService = null;

  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  public void activate(ComponentContext componentContext) {
    logger.info("init() loading default workflow definitions");
    try {
      InputStream errorHandler = WorkflowDefinitionLoader.class.getClassLoader().getResourceAsStream(
              "/OSGI-INF/workflows/default-error-handler.xml");
      workflowService.registerWorkflowDefinition(WorkflowBuilder.getInstance().parseWorkflowDefinition(errorHandler));

      InputStream composeDistPublish = WorkflowDefinitionLoader.class.getClassLoader().getResourceAsStream(
              "/OSGI-INF/workflows/compose-distribute-publish.xml");
      workflowService.registerWorkflowDefinition(WorkflowBuilder.getInstance().parseWorkflowDefinition(
              composeDistPublish));

      //InputStream reviewComposeDistPublish = WorkflowDefinitionLoader.class.getClassLoader().getResourceAsStream(
      //        "/OSGI-INF/workflows/review-compose-distribute-publish.xml");
      InputStream reviewComposeDistPublish = WorkflowDefinitionLoader.class.getClassLoader().getResourceAsStream(
              "/OSGI-INF/workflows/create-dvd.xml");

      workflowService.registerWorkflowDefinition(WorkflowBuilder.getInstance().parseWorkflowDefinition(
              reviewComposeDistPublish));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
