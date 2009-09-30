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
import org.opencastproject.media.mediapackage.jaxb.MediapackageType;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowDefinitionJaxbImpl;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstanceJaxbImpl;
import org.opencastproject.workflow.api.WorkflowOperation;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationImpl;
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
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

/**
 * Implements {@link WorkflowService} with in-memory data structures to hold {@link WorkflowOperation}s and
 * {@link WorkflowInstance}s.  {@link WorkflowOperationHandler}s are looked up in the OSGi service registry based on the
 * "opencast.workflow.operation" property.  If the {@link WorkflowOperationHandler}'s "opencast.workflow.operation"
 * service registration property matches {@link WorkflowOperation#getName()}, then the factory returns a
 * {@link Runnable} to handle that operation.  This allows for custom runnables to be added or modified without
 * affecting the workflow service itself.
 */
public class WorkflowServiceImpl implements WorkflowService, ManagedService {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowServiceImpl.class);
  
  Map<String, WorkflowInstance> instances;
  List<WorkflowOperation> operations;

  public WorkflowServiceImpl() {
    instances = new HashMap<String, WorkflowInstance>();
    operations = new ArrayList<WorkflowOperation>();
    loadOperations();
  }
  
  @SuppressWarnings("unchecked")
  public void updated(Dictionary props) throws ConfigurationException {
    // Update any configuration properties here
  }

  protected void loadOperations() {
    WorkflowOperationImpl compose = new WorkflowOperationImpl("compose", "Composes new media tracks", true);
    WorkflowOperationImpl distribute = new WorkflowOperationImpl("distribute", "Distributes media tracks to distribution channels", true);
    operations.add(compose);
    operations.add(distribute);
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
   * @see org.opencastproject.workflow.api.WorkflowService#start(org.opencastproject.workflow.api.WorkflowDefinition, org.opencastproject.media.mediapackage.MediaPackage)
   */
  public WorkflowInstance start(WorkflowDefinition workflowDefinition, MediaPackage mediaPackage, Map<String, String> properties) {
    WorkflowInstanceJaxbImpl workflowInstance = new WorkflowInstanceJaxbImpl();
    WorkflowDefinitionJaxbImpl def = (WorkflowDefinitionJaxbImpl) workflowDefinition;
    workflowInstance.setId(UUID.randomUUID().toString());
    workflowInstance.setTitle(workflowDefinition.getTitle() + " Instance " + workflowInstance.getId());
    workflowInstance.setDescription(workflowInstance.getTitle());
    workflowInstance.setWorkflowDefinition(def);
    try {
      workflowInstance.setMediaPackageType(MediapackageType.fromXml(mediaPackage.toXml()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    workflowInstance.setProperties(new HashMap<String, String>(properties));
    workflowInstance.setState(WorkflowInstance.State.RUNNING.name());
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
        for(WorkflowOperation operation : wfi.getWorkflowDefinition().getOperations()) {
          ServiceReference[] serviceRefs = null;
          try {
            serviceRefs = componentContext.getBundleContext().getAllServiceReferences(WorkflowOperationHandler.class.getName(),
                    "(opencast.workflow.operation=" + operation.getName() + ")");
          } catch (InvalidSyntaxException e) {
            throw new RuntimeException(e);
          }
          if (serviceRefs == null) {
            logger.info("No WorkflowRunners registered for operation " + operation);
          } else {
            for(ServiceReference serviceRef : serviceRefs) {
              WorkflowOperationHandler runner = (WorkflowOperationHandler)componentContext.getBundleContext().getService(serviceRef);
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
    WorkflowInstanceJaxbImpl instance = (WorkflowInstanceJaxbImpl)getWorkflowInstance(workflowInstanceId);
    Thread t = threadMap.get(workflowInstanceId);
    if(t != null) {
      t.interrupt();
    }
    instance.setState(State.STOPPED.name());
}

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowService#suspend(java.lang.String)
   */
  public void suspend(String workflowInstanceId) {
    WorkflowInstanceJaxbImpl instance = (WorkflowInstanceJaxbImpl)getWorkflowInstance(workflowInstanceId);
    Thread t = threadMap.get(workflowInstanceId);
    if(t != null) {
      t.interrupt();
    }
    instance.setState(State.PAUSED.name());
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowService#resume(java.lang.String)
   */
  public void resume(String workflowInstanceId) {
    WorkflowInstanceJaxbImpl workflowInstance = (WorkflowInstanceJaxbImpl)getWorkflowInstance(workflowInstanceId);
    run(workflowInstance);
    workflowInstance.setState(State.RUNNING.name());
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowService#getWorkflowInstances(org.opencastproject.workflow.api.WorkflowInstance.State)
   */
  public List<WorkflowInstance> getWorkflowInstances(State state) {
    List<WorkflowInstance> instancesInState = new ArrayList<WorkflowInstance>();
    for(Entry<String, WorkflowInstance> entry : instances.entrySet()) {
      if(entry.getValue().getState().equals(state)) {
        instancesInState.add(entry.getValue());
      }
    }
    return instancesInState;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowService#getWorkflowOperations()
   */
  public List<WorkflowOperation> getWorkflowOperations() {
    return Collections.unmodifiableList(operations);
  }
}
