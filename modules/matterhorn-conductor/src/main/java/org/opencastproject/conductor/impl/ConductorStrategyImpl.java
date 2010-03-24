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
package org.opencastproject.conductor.impl;

import org.opencastproject.conductor.api.ConductorStrategy;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowService;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConductorStrategyImpl implements ConductorStrategy {
  private WorkflowService workflowService = null;

  private static final Logger logger = LoggerFactory.getLogger(ConductorStrategyImpl.class);
  public static final String CONFIG_DEFAULT_WORKFLOW = "conductor.strategy.defaultworkflow";

  private String defaultWorkflowId = null;

  @Override
  public WorkflowDefinition getWorkflow(String workFlowDefinitionId) {
    WorkflowDefinition wd = null;
    if (workflowService.DEFAULT_WORKFLOW_ID == workFlowDefinitionId) {
      if (defaultWorkflowId == null) {
        logger.error("Default workflow requested but nothing is specified in the config!");
        throw(new RuntimeException());
      }
    } else {
      wd = workflowService.getWorkflowDefinitionById(defaultWorkflowId);
    }
    wd = workflowService.getWorkflowDefinitionById(workFlowDefinitionId);
    logger.info("selected workflow \"{}\"", wd.getId());
    return wd;
    
  }

  // activator
  public void activate(ComponentContext componentContext) {
    try {
      defaultWorkflowId = (String) componentContext.getBundleContext().getProperty(CONFIG_DEFAULT_WORKFLOW);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  // ------- OSGi services -------
  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

}
