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

import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowRunnerFactory;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowInstance.State;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

/**
 * Implements {@link WorkflowService} with in-memory data structures to hold the {@link WorkflowDefinition}s and
 * {@link WorkflowInstance}s.  {@link WorkflowRunnerFactory}s are looked up in the OSGi service registry based on the
 * "opencast.workflow.operation" property.  If the {@link WorkflowRunnerFactory}'s "opencast.workflow.operation"
 * service registration property matches an operation from {@link WorkflowDefinition#getOperations()}, then the factory
 * returns a {@link Runnable} to handle that operation.  This allows for custom runnables to be added or modified
 * without affecting the workflow service itself.
 */
public class WorkflowServiceImpl implements WorkflowService, ManagedService {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowServiceImpl.class);
  
  Map<String, WorkflowDefinition> definitions;
  Map<String, WorkflowInstance> instances;

  public WorkflowServiceImpl() {
    definitions = new HashMap<String, WorkflowDefinition>();
    instances = new HashMap<String, WorkflowInstance>();
    loadSampleData();
  }
  
  @SuppressWarnings("unchecked")
  public void updated(Dictionary props) throws ConfigurationException {
    // Update any configuration properties here
  }

  protected void loadSampleData() {
    WorkflowDefinitionImpl def1 = new WorkflowDefinitionImpl();
    def1.setId("1");
    def1.setTitle("Transcode and Distribute");
    def1.setDescription("A simple workflow that transcodes the media into distribution formats, then sends the " +
        "resulting distribution files, along with their associated metadata, to the distribution channels");
    List<String> operations1 = new ArrayList<String>();
    operations1.add("compose");
    operations1.add("distribute");
    def1.setOperations(operations1);
    definitions.put(def1.getId(), def1);
  
    WorkflowDefinitionImpl def2 = new WorkflowDefinitionImpl();
    def2.setId("2");
    def2.setTitle("Distribute Only");
    def2.setDescription("A simple workflow that sends media and metadata directly to the distribution channels");
    List<String> operations2 = new ArrayList<String>();
    operations2.add("distribute");
    def2.setOperations(operations2);
    definitions.put(def2.getId(), def2);
    
    WorkflowInstanceImpl instance1 = new WorkflowInstanceImpl();
    instance1.setId("1");
    instance1.setTitle("Instance #1 of 'Transcode and Distribute'");
    instance1.setDescription("An instance of the transcode and distribute workflow definition");
    instance1.setWorkflowDefinition(def1);
    instance1.setState(State.PAUSED);
    Map<String, String> map1 = new HashMap<String, String>();
    map1.put("flash", "low-bandwidth");
    instance1.setProperties(map1);
    instances.put(instance1.getId(), instance1);
  
    WorkflowInstanceImpl instance2 = new WorkflowInstanceImpl();
    instance2.setId("2");
    instance2.setTitle("Instance #2 of 'Transcode and Distribute'");
    instance2.setDescription("Another instance of the transcode and distribute workflow definition");
    instance2.setWorkflowDefinition(def1);
    instance2.setState(State.RUNNING);
    Map<String, String> map2 = new HashMap<String, String>();
    map2.put("flash", "high-bandwidth");
    instance2.setProperties(map2);
    instances.put(instance2.getId(), instance2);
  
    WorkflowInstanceImpl instance3 = new WorkflowInstanceImpl();
    instance3.setId("3");
    instance3.setTitle("Instance #1 of 'Distribute Only'");
    instance3.setDescription("An instance of the distribute only workflow definition");
    instance3.setWorkflowDefinition(def2);
    instance3.setState(State.STOPPED);
    instances.put(instance3.getId(), instance3);
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
    WorkflowDefinition workflowDefinition = definitions.get(workflowDefinitionId);
    for(Entry<String, WorkflowInstance> entry : instances.entrySet()) {
      WorkflowInstance instance = entry.getValue();
      if(instance.getWorkflowDefinition().equals(workflowDefinition)) {
        list.add(instance);
      }
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
    WorkflowInstance instance = instances.get(id);
    if(instance == null) {
      logger.warn("no workflow instance found with id=" + id);
    }
    return instance;
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
   * @see org.opencastproject.workflow.api.WorkflowService#start(org.opencastproject.workflow.api.WorkflowDefinition, org.opencastproject.media.mediapackage.MediaPackage)
   */
  public WorkflowInstance start(WorkflowDefinition workflowDefinition, MediaPackage mediaPackage, Map<String, String> properties) {
    WorkflowInstanceImpl workflowInstance = new WorkflowInstanceImpl();
    workflowInstance.setId(UUID.randomUUID().toString());
    workflowInstance.setTitle(workflowDefinition.getTitle() + " Instance " + workflowInstance.getId());
    workflowInstance.setDescription(workflowInstance.getTitle());
    workflowInstance.setWorkflowDefinition(workflowDefinition);
    workflowInstance.setMediaPackage(mediaPackage);
    workflowInstance.setProperties(properties);
    workflowInstance.setState(WorkflowInstance.State.RUNNING);
    instances.put(workflowInstance.getId(), workflowInstance);
    run(workflowInstance);
    return workflowInstance;
  }

  protected Map<String, Thread> threadMap = new HashMap<String, Thread>();

  // TODO Remove OSGI dependency if possible
  ComponentContext componentContext;
  public void activate(ComponentContext componentContext) {
    this.componentContext = componentContext;
  }

  protected void run(final WorkflowInstance wfi) {
    Thread t = new Thread(new Runnable() {
      public void run() {
        // Get all of the runnable workflow services available for each operation (in the order of operations)
        for(String operation : wfi.getWorkflowDefinition().getOperations()) {
          ServiceReference[] serviceRefs = null;
          try {
            serviceRefs = componentContext.getBundleContext().getAllServiceReferences(WorkflowRunnerFactory.class.getName(),
                    "(opencast.workflow.operation=" + operation + ")");
          } catch (InvalidSyntaxException e) {
            throw new RuntimeException(e);
          }
          if (serviceRefs == null) {
            logger.info("No WorkflowRunners registered for operation " + operation);
          } else {
            for(ServiceReference serviceRef : serviceRefs) {
              WorkflowRunnerFactory runner = (WorkflowRunnerFactory)componentContext.getBundleContext().getService(serviceRef);
              runner.getRunnable(wfi).run(); // Do not spawn new threads for these runnables yet
            }
          }
        }
      }
    });
    threadMap.put(wfi.getId(), t);
    t.start();
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowService#stop(java.lang.String)
   */
  public void stop(String workflowInstanceId) {
    WorkflowInstanceImpl instance = (WorkflowInstanceImpl)getWorkflowInstance(workflowInstanceId);
    Thread t = threadMap.get(workflowInstanceId);
    if(t != null) {
      t.interrupt();
    }
    instance.setState(State.STOPPED);
}

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowService#suspend(java.lang.String)
   */
  public void suspend(String workflowInstanceId) {
    WorkflowInstanceImpl instance = (WorkflowInstanceImpl)getWorkflowInstance(workflowInstanceId);
    Thread t = threadMap.get(workflowInstanceId);
    if(t != null) {
      t.interrupt();
    }
    instance.setState(State.PAUSED);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowService#resume(java.lang.String)
   */
  public void resume(String workflowInstanceId) {
    WorkflowInstanceImpl workflowInstance = (WorkflowInstanceImpl)getWorkflowInstance(workflowInstanceId);
    run(workflowInstance);
    workflowInstance.setState(State.RUNNING);
  }
}
