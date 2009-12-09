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
import org.opencastproject.workflow.api.WorkflowConfiguration;
import org.opencastproject.workflow.api.WorkflowConfigurationImpl;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowDefinitionList;
import org.opencastproject.workflow.api.WorkflowDefinitionListImpl;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationConfigurations;
import org.opencastproject.workflow.api.WorkflowOperationConfigurationsImpl;
import org.opencastproject.workflow.api.WorkflowOperationDefinition;
import org.opencastproject.workflow.api.WorkflowOperationDefinitionListImpl;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowSet;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements {@link WorkflowService} with in-memory data structures to hold {@link WorkflowOperation}s and
 * {@link WorkflowInstance}s. {@link WorkflowOperationHandler}s are looked up in the OSGi service registry based on the
 * "opencast.workflow.operation" property. If the {@link WorkflowOperationHandler}'s "opencast.workflow.operation"
 * service registration property matches {@link WorkflowOperation#getName()}, then the factory returns a
 * {@link WorkflowOperationRunner} to handle that operation. This allows for custom runners to be added or modified
 * without affecting the workflow service itself.
 */
public class WorkflowServiceImpl implements WorkflowService, ManagedService {

  /** Logging facility */
  private static final Logger log_ = LoggerFactory.getLogger(WorkflowServiceImpl.class);

  /**
   * The service registration property we use to identify which workflow operation a {@link WorkflowOperationHandler}
   * should handle.
   */
  protected static final String WORKFLOW_OPERATION_PROPERTY = "workflow.operation";

  /** TODO: Remove references to the component context once felix scr 1.2 becomes available */
  protected ComponentContext componentContext = null;

  /** The collection of workflow definitions */
  protected Map<String, WorkflowDefinition> workflowDefinitions = new HashMap<String, WorkflowDefinition>();

  /**
   * A tuple of a workflow operation handler and the name of the operation it handles
   */
  class HandlerRegistration {
    HandlerRegistration(String operationName, WorkflowOperationHandler handler) {
      this.operationName = operationName;
      this.handler = handler;
    }
    WorkflowOperationHandler handler;
    String operationName;
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + getOuterType().hashCode();
      result = prime * result + ((handler == null) ? 0 : handler.hashCode());
      result = prime * result + ((operationName == null) ? 0 : operationName.hashCode());
      return result;
    }
    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (!(obj instanceof HandlerRegistration)) {
        return false;
      }
      HandlerRegistration other = (HandlerRegistration) obj;
      if (!getOuterType().equals(other.getOuterType())) {
        return false;
      }
      if (handler == null) {
        if (other.handler != null) {
          return false;
        }
      } else if (!handler.equals(other.handler)) {
        return false;
      }
      if (operationName == null) {
        if (other.operationName != null) {
          return false;
        }
      } else if (!operationName.equals(other.operationName)) {
        return false;
      }
      return true;
    }
    private WorkflowServiceImpl getOuterType() {
      return WorkflowServiceImpl.this;
    }
  }
  
  /** The data access object responsible for storing and retrieving workflow instances */
  protected WorkflowServiceImplDao dao;
  
  /** A collection of the running workflow threads */
  protected Map<String, Thread> threadMap = new ConcurrentHashMap<String, Thread>();

  /**
   * Sets the DAO implementation to use in this service.
   * @param dao The dao to use for persistence
   */
  public void setDao(WorkflowServiceImplDao dao) {
    this.dao = dao;
  }

  /**
   * Activate this service implementation via the OSGI service component runtime
   */
  public void activate(ComponentContext componentContext) {
    this.componentContext = componentContext;
  }

  /**
   * Deactivate this service.
   */
  public void deactivate() {
  }
  
  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  @SuppressWarnings("unchecked")
  public void updated(Dictionary props) throws ConfigurationException {
    // Update any configuration properties here
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#listAvailableWorkflowDefinitions()
   */
  public WorkflowDefinitionList listAvailableWorkflowDefinitions() {
    WorkflowDefinitionList list = new WorkflowDefinitionListImpl();
    for (Entry<String, WorkflowDefinition> entry : workflowDefinitions.entrySet()) {
      list.add((WorkflowDefinition) entry.getValue());
    }
    Collections.sort(list, new Comparator<WorkflowDefinition>() {
      public int compare(WorkflowDefinition o1, WorkflowDefinition o2) {
        return o1.getTitle().compareTo(o2.getTitle());
      }
    });
    return list;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#isRunnable(org.opencastproject.workflow.api.WorkflowDefinition)
   */
  public boolean isRunnable(WorkflowDefinition workflowDefinition) {
    List<String> availableOperations = listAvailableOperationNames();
    List<WorkflowDefinition> checkedWorkflows = new ArrayList<WorkflowDefinition>();
    boolean runnable = isRunnable(workflowDefinition, availableOperations, checkedWorkflows);
    int wfCount = checkedWorkflows.size() - 1;
    if (runnable)
      log_.info("Workflow " + workflowDefinition + ", containing " + wfCount + " derived workflows is runnable");
    else
      log_.warn("Workflow " + workflowDefinition + ", containing " + wfCount + " derived workflows is not runnable");
    return runnable;
  }

  /**
   * Tests the workflow definition for its runnability. This method is a helper for
   * {@link #isRunnable(WorkflowDefinition)} that is suited for recursive calling.
   * 
   * @param workflowDefinition
   *          the definition to test
   * @param availableOperations
   *          list of currently available operation handlers
   * @param checkedWorkflows
   *          list of checked workflows, used to avoid circular checking
   * @return <code>true</code> if all bits and pieces used for executing <code>workflowDefinition</code> are in place
   */
  private boolean isRunnable(WorkflowDefinition workflowDefinition, List<String> availableOperations,
          List<WorkflowDefinition> checkedWorkflows) {
    if (checkedWorkflows.contains(workflowDefinition))
      return true;

    // Test availability of operation handler and catch workflows
    for (WorkflowOperationDefinition op : workflowDefinition.getOperations()) {
      if (!availableOperations.contains(op.getName())) {
        log_.info(workflowDefinition + " is not runnable due to missing operation " + op);
        return false;
      }
      String catchWorkflow = op.getExceptionHandlingWorkflow();
      if (catchWorkflow != null) {
        WorkflowDefinition catchWorkflowDefinition = getWorkflowDefinitionByName(catchWorkflow);
        if (catchWorkflowDefinition == null) {
          log_.info(workflowDefinition + " is not runnable due to missing catch workflow " + catchWorkflow
                  + " on operation " + op);
          return false;
        }
        if (!isRunnable(catchWorkflowDefinition, availableOperations, checkedWorkflows))
          return false;
      }
    }

    // Add the workflow to the list of checked workflows
    if (!checkedWorkflows.contains(workflowDefinition))
      checkedWorkflows.add(workflowDefinition);
    return true;
  }

  /**
   * Gets the currently registered workflow operation handlers.
   * 
   * @return All currently registered handlers
   */
  protected Set<HandlerRegistration> getRegisteredHandlers() {
    Set<HandlerRegistration> set = new HashSet<HandlerRegistration>();
    ServiceReference[] refs;
    try {
      refs = componentContext.getBundleContext().getServiceReferences(
              WorkflowOperationHandler.class.getName(), null);
    } catch (InvalidSyntaxException e) {
      throw new RuntimeException(e);
    }
    for (ServiceReference ref : refs) {
      WorkflowOperationHandler handler = (WorkflowOperationHandler)componentContext.getBundleContext().getService(ref);
      set.add(new HandlerRegistration((String)ref.getProperty(WORKFLOW_OPERATION_PROPERTY), handler));
    }
    return set;
  }

  /**
   * Lists the names of each workflow operation. Operation names are availalbe for use if there is a registered
   * {@link WorkflowOperationHandler} with an equal {@link WorkflowServiceImpl#WORKFLOW_OPERATION_PROPERTY} property.
   * 
   * @return The {@link List} of available workflow operation names
   */
  protected List<String> listAvailableOperationNames() {
    List<String> list = new ArrayList<String>();
    for(HandlerRegistration reg : getRegisteredHandlers()) {
      list.add(reg.operationName);
    }
    return list;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#registerWorkflowDefinition(org.opencastproject.workflow.api.WorkflowDefinition)
   */
  public void registerWorkflowDefinition(WorkflowDefinition workflow) {
    workflowDefinitions.put(workflow.getTitle(), workflow);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#unregisterWorkflowDefinition(org.opencastproject.workflow.api.WorkflowDefinition)
   */
  public void unregisterWorkflowDefinition(String workflowTitle) {
    workflowDefinitions.remove(workflowTitle);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#getWorkflowById(java.lang.String)
   */
  public WorkflowInstance getWorkflowById(String id) {
    return dao.getWorkflowById(id);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#start(org.opencastproject.workflow.api.WorkflowDefinition,
   *      org.opencastproject.media.mediapackage.MediaPackage)
   */
  public WorkflowInstance start(WorkflowDefinition workflowDefinition, MediaPackage mediaPackage,
          Map<String, String> properties) {
    //Set<WorkflowConfiguration> configurations = new HashSet<WorkflowConfiguration>();
    //if (properties != null) {
    //  for(Entry<String, String> entry : properties.entrySet()) {
    //    configurations.add(new WorkflowConfigurationImpl(entry.getKey(), entry.getValue()));
    //  }
    //}
    
    Set<WorkflowOperationConfigurations> configurations = new HashSet<WorkflowOperationConfigurations>();
    if(properties != null){
      configurations = proccessProperties(properties);
    }
    
    String id = UUID.randomUUID().toString();
    log_.info("Starting a new workflow instance with ID=" + id);
    WorkflowInstanceImpl workflowInstance = new WorkflowInstanceImpl(workflowDefinition);
    workflowInstance.setId(id);
    try {
      workflowInstance.setSourceMediaPackageType(MediapackageType.fromXml(mediaPackage.toXml()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    workflowInstance.setConfigurations(configurations);
    workflowInstance.setState(WorkflowInstance.State.RUNNING.name());
    dao.update(workflowInstance);
    run(workflowInstance);
    return workflowInstance;
  }
  
  /**
   * Divides properties based to prefix to local (specific for workflow operation) or global
   * 
   * @param properties
   * @return
   */
  protected Set<WorkflowOperationConfigurations> proccessProperties(Map<String, String> properties){
    
    HashMap<String, Set<WorkflowConfiguration>> configurations = new HashMap<String, Set<WorkflowConfiguration>>();
    configurations.put("global", new HashSet<WorkflowConfiguration>());
    
    for(Entry<String, String> entry : properties.entrySet()){
      if(entry.getKey().contains("/")){
        String[] key = entry.getKey().split("/", 2);
        if(!configurations.containsKey(key[0])){
          configurations.put(key[0], new HashSet<WorkflowConfiguration>());
        }
        configurations.get(key[0]).add(new WorkflowConfigurationImpl(key[1], entry.getValue()));
      } else {
        configurations.get("global").add(new WorkflowConfigurationImpl(entry.getKey(), entry.getValue()));
      }
    }
    
    Set<WorkflowOperationConfigurations> confs = new HashSet<WorkflowOperationConfigurations>();
    for (String key : configurations.keySet()){
      confs.add(new WorkflowOperationConfigurationsImpl(key, configurations.get(key)));
    }
    
    return confs;
  }

  /**
   * Does a lookup of available operation handlers for the given workflow operation.
   * 
   * @param operation
   *          the operation definition
   * @return the handler or <code>null</code>
   */
  protected WorkflowOperationHandler selectOperationHandler(WorkflowOperationDefinition operation) {
    List<WorkflowOperationHandler> handlerList = new ArrayList<WorkflowOperationHandler>();
    for (HandlerRegistration handlerReg : getRegisteredHandlers()) {
      if (handlerReg.operationName != null && handlerReg.operationName.equals(operation.getName())) {
        handlerList.add((WorkflowOperationHandler) handlerReg.handler);
      }
    }

    // Select one of the possibly multiple operation handlers. TODO Allow for a pluggable strategy for this mechanism
    if (handlerList.size() > 0) {
      int index = (int) Math.round((handlerList.size() - 1) * Math.random());
      return handlerList.get(index);
    }

    log_.info("No workflow operation handlers found for operation " + operation.getName());
    return null;
  }

  protected void run(final WorkflowInstanceImpl wfi) {
    Thread t = new Thread(new Runnable() {
      public void run() {
        boolean failed = false;
        int runFromOperation = 0;
        List<WorkflowOperationDefinition> operationDefinitions = wfi.getWorkflowOperationDefinitionList();
        while (runFromOperation >= 0 && runFromOperation < operationDefinitions.size()) {
          // Get all of the available handlers for each operation (in the order of operations)
          operationDefinitions = wfi.getWorkflowOperationDefinitionList();
          for (int i = runFromOperation; i < operationDefinitions.size(); i++) {
            WorkflowOperationDefinition operationDefinition = operationDefinitions.get(i);
            WorkflowOperationHandler operationHandler = selectOperationHandler(operationDefinition);
            // If there is no handler for the operation, mark this workflow as failed
            if (operationHandler == null) {
              log_.warn("No handler available to execute operation " + operationDefinition);
              failed = true;
              break;
            }
            // Add an operation instance with the resulting media package to the workflow instance
            WorkflowOperationInstanceImpl opInstance = new WorkflowOperationInstanceImpl(operationDefinition);
            // Merge in local configurations
            for(WorkflowConfiguration config : wfi.getLocalConfigurations(opInstance.getName())){
              opInstance.setConfiguration(config.getKey(), config.getValue());
            }
            
            // Merge in the workflow instance's configurations, which override any operation-specific config
            for(WorkflowConfiguration config : wfi.getConfigurations()) {
              opInstance.setConfiguration(config.getKey(), config.getValue());
            }
            wfi.getWorkflowOperationInstanceList().add(opInstance);
            update(wfi); // Update the workflow instance, since it has a new operation instance
            try {
              opInstance.setResult(operationHandler.run(wfi));
              opInstance.setState(State.SUCCEEDED.name());
            } catch (WorkflowOperationException e) {
              log_.warn("Operation " + operationDefinition + " failed:" + e.getMessage());
              // If the operation is set to fail on error, set the workflow to "failed" and run the exception handling
              // workflow operations
              if (operationDefinition.isFailWorkflowOnException()) {
                wfi.setState(State.FAILING.name());
                failed = true;

                // Replace the next operations with those from the failure handling workflow
                String catchWorkflowName = operationDefinition.getExceptionHandlingWorkflow();
                WorkflowDefinition catchWorkflowDefinition = getWorkflowDefinitionByName(catchWorkflowName);
                if (catchWorkflowDefinition == null) {
                  log_.warn("Unable to execute catch workflow " + catchWorkflowName + " for operation "
                          + operationDefinition);
                  return;
                }

                // Find the position of the current operation and compile the new operation list
                int operationIndex = wfi.getWorkflowOperationInstanceList().size();
                WorkflowOperationDefinitionListImpl updatedOperationDefinitions = new WorkflowOperationDefinitionListImpl();
                for (int j = 0; j < operationIndex; j++)
                  updatedOperationDefinitions.add(operationDefinitions.get(j));
                updatedOperationDefinitions.addAll(catchWorkflowDefinition.getOperations());
                wfi.setWorkflowOperationDefinitionList(updatedOperationDefinitions);
                runFromOperation = operationIndex;
                break;
              } else {
                // Replace the next operations with those from the failure handling workflow
                String catchWorkflowName = operationDefinition.getExceptionHandlingWorkflow();
                WorkflowDefinition catchWorkflowDefinition = getWorkflowDefinitionByName(catchWorkflowName);
                if (catchWorkflowDefinition == null) {
                  log_.warn("Unable to execute catch workflow " + catchWorkflowName + " for operation "
                          + operationDefinition);
                  return;
                }

                // Find the position of the current operation and compile the new operation list
                int operationIndex = wfi.getWorkflowOperationInstanceList().size();
                WorkflowOperationDefinitionListImpl updatedOperationDefinitions = new WorkflowOperationDefinitionListImpl();
                for (int j = 0; j < operationIndex; j++)
                  updatedOperationDefinitions.add(operationDefinitions.get(j));
                updatedOperationDefinitions.addAll(catchWorkflowDefinition.getOperations());
                for (int j = operationIndex; j < operationDefinitions.size(); j++)
                  updatedOperationDefinitions.add(operationDefinitions.get(j));
                wfi.setWorkflowOperationDefinitionList(updatedOperationDefinitions);
                runFromOperation = operationIndex;
                break;
              }
            }
            update(wfi); // Update the workflow instance again, since its new operation instance has completed
            if (opInstance.getResult().isWait()) { // suspend the workflow if the operation handler asked us to do so
              suspend(wfi.getId());
            }
            runFromOperation = -1;
          }
        }

        // All of the workflow operation definitions have run, so set the state and save it.
        if (failed) {
          wfi.setState(State.FAILED.name());
        } else {
          wfi.setState(State.SUCCEEDED.name());
        }
        update(wfi);
      }
    });
    threadMap.put(wfi.getId(), t);
    t.start();
  }

  /**
   * Returns the workflow identified by <code>name</code> or <code>null</code> if no such workflow was found.
   * 
   * @param name
   *          the workflow definition name
   * @return the workflow
   */
  public WorkflowDefinition getWorkflowDefinitionByName(String name) {
    return workflowDefinitions.get(name);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#stop(java.lang.String)
   */
  public void stop(String workflowInstanceId) {
    WorkflowInstanceImpl instance = (WorkflowInstanceImpl) getWorkflowById(workflowInstanceId);
    Thread t = threadMap.get(workflowInstanceId);
    if (t != null) {
      t.interrupt();
    }
    instance.setState(State.STOPPED.name());
    update(instance);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#suspend(java.lang.String)
   */
  public void suspend(String workflowInstanceId) {
    WorkflowInstanceImpl instance = (WorkflowInstanceImpl) getWorkflowById(workflowInstanceId);
    Thread t = threadMap.get(workflowInstanceId);
    if (t != null) {
      t.interrupt();
    }
    instance.setState(State.PAUSED.name());
    update(instance);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#resume(java.lang.String)
   */
  public void resume(String workflowInstanceId) {
    WorkflowInstanceImpl workflowInstance = (WorkflowInstanceImpl) getWorkflowById(workflowInstanceId);
    run(workflowInstance);
    workflowInstance.setState(State.RUNNING.name());

    // TODO restart the workflow from the operation where it was paused

    update(workflowInstance);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#update(org.opencastproject.workflow.api.WorkflowInstance)
   */
  public void update(WorkflowInstance workflowInstance) {
    dao.update(workflowInstance);
  }

  /**
   * Removes a workflow instance.
   * 
   * @param id
   *          The id of the workflow instance to remove
   */
  public void removeFromDatabase(String id) {
    dao.remove(id);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowService#countWorkflowInstances()
   */
  public long countWorkflowInstances() {
    return dao.countWorkflowInstances();
  }

  public WorkflowSet getWorkflowInstances(WorkflowQuery query) {
    return dao.getWorkflowInstances(query);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowService#newWorkflowQuery()
   */
  public WorkflowQuery newWorkflowQuery() {
    return new WorkflowQueryImpl();
  }
}
