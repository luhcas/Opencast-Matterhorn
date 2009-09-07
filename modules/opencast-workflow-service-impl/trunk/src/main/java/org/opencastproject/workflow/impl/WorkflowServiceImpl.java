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
package org.opencastproject.workflow.impl;

import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowService;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * FIXME -- Add javadocs
 */
public class WorkflowServiceImpl implements WorkflowService, ManagedService {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowServiceImpl.class);
  
  Map<String, WorkflowDefinition> definitions;
  Map<String, WorkflowInstance> instances;
  
  public WorkflowServiceImpl() {
    definitions = new HashMap<String, WorkflowDefinition>();
    WorkflowDefinitionImpl def1 = new WorkflowDefinitionImpl();
    def1.setId("1");
    def1.setTitle("Transcode and Distribute");
    def1.setDescription("A simple workflow that transcodes the media into distribution formats, then sends the " +
        "resulting distribution files, along with their associated metadata, to the distribution channels");
    definitions.put(def1.getId(), def1);

    WorkflowDefinitionImpl def2 = new WorkflowDefinitionImpl();
    def2.setId("2");
    def2.setTitle("Distribute Only");
    def2.setDescription("A simple workflow that sends media and metadata directly to the distribution channels");
    definitions.put(def2.getId(), def2);
  }
  
  @SuppressWarnings("unchecked")
  public void updated(Dictionary props) throws ConfigurationException {
    // Update any configuration properties here
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowService#fetchAllWorkflowDefinitions()
   */
  public List<WorkflowDefinition> fetchAllWorkflowDefinitions() {
    List<WorkflowDefinition> list = new ArrayList<WorkflowDefinition>();
    for(Entry<String, WorkflowDefinition> entry : definitions.entrySet()) {
      list.add(entry.getValue());
    }
    Collections.sort(list, new Comparator<WorkflowDefinition>() {
      public int compare(WorkflowDefinition w1, WorkflowDefinition w2) {
        return w1.getTitle().compareTo(w2.getTitle());
      }
    });
    return list;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowService#fetchAllWorkflowInstances(java.lang.String)
   */
  public List<WorkflowInstance> fetchAllWorkflowInstances(String workflowDefinitionId) {
    List<WorkflowInstance> list = new ArrayList<WorkflowInstance>();
    for(Entry<String, WorkflowInstance> entry : instances.entrySet()) {
      list.add(entry.getValue());
    }
    Collections.sort(list, new Comparator<WorkflowInstance>() {
      public int compare(WorkflowInstance w1, WorkflowInstance w2) {
        return w1.getTitle().compareTo(w2.getTitle());
      }
    });
    return list;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowService#getWorkflowDefinition(java.lang.String)
   */
  public WorkflowDefinition getWorkflowDefinition(String id) {
    return definitions.get(id);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowService#getWorkflowInstance(java.lang.String)
   */
  public WorkflowInstance getWorkflowInstance(String id) {
    return instances.get(id);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowService#registerWorkflowDefinition(org.opencastproject.workflow.api.WorkflowDefinition)
   */
  public void registerWorkflowDefinition(WorkflowDefinition workflowDefinition) {
    if(definitions.containsKey(workflowDefinition.getId())) {
      throw new IllegalArgumentException("Workflow definition " + workflowDefinition.getId() + " already exists");
    }
    definitions.put(workflowDefinition.getId(), workflowDefinition);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowService#saveWorkflowInstance(org.opencastproject.workflow.api.WorkflowInstance)
   */
  public void saveWorkflowInstance(WorkflowInstance workflowInstance) {
    if(instances.put(workflowInstance.getId(), workflowInstance) == null) {
      logger.info("Updated existing workflow instance id=" + workflowInstance.getId());
    }
  }

}

