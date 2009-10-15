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

import org.opencastproject.conductor.api.ConductorService;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowOperation;
import org.opencastproject.workflow.api.WorkflowService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the default implementation of the conductor service.
 */
public class ConductorServiceImpl implements ConductorService {

  protected WorkflowService workflowService;

  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  protected Map<String, WorkflowDefinition> defs;

  public void init() {
    defs = new HashMap<String, WorkflowDefinition>();
    List<WorkflowOperation> allOperations = workflowService.getWorkflowOperations();

    WorkflowDefinitionImpl def1 = new WorkflowDefinitionImpl();
    def1.setId("1");
    def1.setTitle("Transcode and Distribute");
    def1.setDescription("A simple workflow that transcodes the media into distribution formats, then sends the "
            + "resulting distribution files, along with their associated metadata, to the distribution channels");
    def1.setOperations(allOperations);
    defs.put(def1.getId(), def1);

    WorkflowDefinitionImpl def2 = new WorkflowDefinitionImpl();
    def2.setId("2");
    def2.setTitle("Distribute Only");
    def2.setDescription("A simple workflow that sends media and metadata directly to the distribution channels");
    List<WorkflowOperation> operations2 = new ArrayList<WorkflowOperation>();
    operations2.add(allOperations.get(1));
    def1.setOperations(operations2);
    defs.put(def2.getId(), def2);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.conductor.api.ConductorService#getWorkflowDefinitions()
   */
  public List<WorkflowDefinition> getWorkflowDefinitions() {
    List<WorkflowDefinition> defList = new ArrayList<WorkflowDefinition>(defs.size());
    defList.addAll(defs.values());
    return defList;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.conductor.api.ConductorService#getWorkflowDefinitionByName(java.lang.String)
   */
  public WorkflowDefinition getWorkflowDefinitionByName(String name) {
    return defs.get(name);
  }

}
