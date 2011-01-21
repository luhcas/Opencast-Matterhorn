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
package org.opencastproject.workflow.impl;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.opencastproject.workflow.api.WorkflowInstance.WorkflowState.FAILED;
import static org.opencastproject.workflow.api.WorkflowInstance.WorkflowState.FAILING;
import static org.opencastproject.workflow.api.WorkflowInstance.WorkflowState.INSTANTIATED;
import static org.opencastproject.workflow.api.WorkflowInstance.WorkflowState.PAUSED;
import static org.opencastproject.workflow.api.WorkflowInstance.WorkflowState.RUNNING;
import static org.opencastproject.workflow.api.WorkflowInstance.WorkflowState.STOPPED;
import static org.opencastproject.workflow.api.WorkflowInstance.WorkflowState.SUCCEEDED;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageMetadata;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.metadata.api.MediaPackageMetadataService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.ResumableWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowListener;
import org.opencastproject.workflow.api.WorkflowOperationDefinition;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workflow.api.WorkflowOperationResultImpl;
import org.opencastproject.workflow.api.WorkflowParser;
import org.opencastproject.workflow.api.WorkflowParsingException;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowSet;
import org.opencastproject.workflow.api.WorkflowStatistics;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implements WorkflowService with in-memory data structures to hold WorkflowOperations and WorkflowInstances.
 * WorkflowOperationHandlers are looked up in the OSGi service registry based on the "workflow.operation" property. If
 * the WorkflowOperationHandler's "workflow.operation" service registration property matches
 * WorkflowOperation.getName(), then the factory returns a WorkflowOperationRunner to handle that operation. This allows
 * for custom runners to be added or modified without affecting the workflow service itself.
 */
public class WorkflowServiceImpl implements WorkflowService, JobProducer {

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(WorkflowServiceImpl.class);

  /** List of available operations on jobs */
  private enum Operation {
    START_WORKFLOW, RESUME, START_OPERATION
  };

  /** The pattern used by workfow operation configuration keys **/
  public static final Pattern PROPERTY_PATTERN = Pattern.compile("\\$\\{.+?\\}");

  /** Constant value indicating a <code>null</code> parent id */
  private static final String NULL_PARENT_ID = "-";

  /** TODO: Remove references to the component context once felix scr 1.2 becomes available */
  protected ComponentContext componentContext = null;

  /** The collection of workflow definitions */
  protected Map<String, WorkflowDefinition> workflowDefinitions = new HashMap<String, WorkflowDefinition>();

  /** The metadata services */
  private SortedSet<MediaPackageMetadataService> metadataServices;

  /** The data access object responsible for storing and retrieving workflow instances */
  protected WorkflowServiceIndex index;

  /** The list of workflow listeners */
  private List<WorkflowListener> listeners = new CopyOnWriteArrayList<WorkflowListener>();

  /** The thread pool to use for firing listeners */
  protected ThreadPoolExecutor listenerExecutorService;

  /** The service registry */
  protected ServiceRegistry serviceRegistry = null;

  /**
   * Constructs a new workflow service impl, with a priority-sorted map of metadata services
   */
  public WorkflowServiceImpl() {
    metadataServices = new TreeSet<MediaPackageMetadataService>(new Comparator<MediaPackageMetadataService>() {
      @Override
      public int compare(MediaPackageMetadataService o1, MediaPackageMetadataService o2) {
        return o1.getPriority() - o2.getPriority();
      }
    });
  }

  /**
   * Activate this service implementation via the OSGI service component runtime.
   * 
   * @param componentContext
   *          the component context
   */
  public void activate(ComponentContext componentContext) {
    this.componentContext = componentContext;
    listenerExecutorService = (ThreadPoolExecutor) Executors.newCachedThreadPool();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#addWorkflowListener(org.opencastproject.workflow.api.WorkflowListener)
   */
  @Override
  public void addWorkflowListener(WorkflowListener listener) {
    listeners.add(listener);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#removeWorkflowListener(org.opencastproject.workflow.api.WorkflowListener)
   */
  @Override
  public void removeWorkflowListener(WorkflowListener listener) {
    listeners.remove(listener);
  }

  /**
   * Fires the workflow listeners on workflow updates.
   */
  protected void fireListeners(final WorkflowInstance oldWorkflowInstance, final WorkflowInstance newWorkflowInstance)
          throws WorkflowParsingException {
    for (final WorkflowListener listener : listeners) {
      if (oldWorkflowInstance == null || !oldWorkflowInstance.getState().equals(newWorkflowInstance.getState())) {
        Runnable runnable = new Runnable() {
          @Override
          public void run() {
            listener.stateChanged(newWorkflowInstance);
          }
        };
        listenerExecutorService.execute(runnable);
      }

      if (newWorkflowInstance.getCurrentOperation() != null) {
        if (oldWorkflowInstance == null || oldWorkflowInstance.getCurrentOperation() == null
                || !oldWorkflowInstance.getCurrentOperation().equals(newWorkflowInstance.getCurrentOperation())) {
          Runnable runnable = new Runnable() {
            @Override
            public void run() {
              listener.operationChanged(newWorkflowInstance);
            }
          };
          listenerExecutorService.execute(runnable);
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#listAvailableWorkflowDefinitions()
   */
  public List<WorkflowDefinition> listAvailableWorkflowDefinitions() {
    List<WorkflowDefinition> list = new ArrayList<WorkflowDefinition>();
    for (Entry<String, WorkflowDefinition> entry : workflowDefinitions.entrySet()) {
      list.add((WorkflowDefinition) entry.getValue());
    }
    Collections.sort(list, new Comparator<WorkflowDefinition>() {
      public int compare(WorkflowDefinition o1, WorkflowDefinition o2) {
        return o1.getId().compareTo(o2.getId());
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
      logger.info("Workflow {}, containing {} derived workflows, is runnable", workflowDefinition, wfCount);
    else
      logger.warn("Workflow {}, containing {} derived workflows, is not runnable", workflowDefinition, wfCount);
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
      if (!availableOperations.contains(op.getId())) {
        logger.info("{} is not runnable due to missing operation {}", workflowDefinition, op);
        return false;
      }
      String catchWorkflow = op.getExceptionHandlingWorkflow();
      if (catchWorkflow != null) {
        WorkflowDefinition catchWorkflowDefinition;
        try {
          catchWorkflowDefinition = getWorkflowDefinitionById(catchWorkflow);
        } catch (NotFoundException e) {
          logger.info("{} is not runnable due to missing catch workflow {} on operation {}", new Object[] {
                  workflowDefinition, catchWorkflow, op });
          return false;
        } catch (WorkflowDatabaseException e) {
          logger.info("{} is not runnable because we can not load the catch workflow {} on operation {}", new Object[] {
                  workflowDefinition, catchWorkflow, op });
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
  public Set<HandlerRegistration> getRegisteredHandlers() {
    Set<HandlerRegistration> set = new HashSet<HandlerRegistration>();
    ServiceReference[] refs;
    try {
      refs = componentContext.getBundleContext().getServiceReferences(WorkflowOperationHandler.class.getName(), null);
    } catch (InvalidSyntaxException e) {
      throw new IllegalStateException(e);
    }
    for (ServiceReference ref : refs) {
      WorkflowOperationHandler handler = (WorkflowOperationHandler) componentContext.getBundleContext().getService(ref);
      set.add(new HandlerRegistration((String) ref.getProperty(WORKFLOW_OPERATION_PROPERTY), handler));
    }
    return set;
  }

  protected WorkflowOperationHandler getWorkflowOperationHandler(String operationId) {
    for (HandlerRegistration reg : getRegisteredHandlers()) {
      if (reg.operationName.equals(operationId))
        return reg.handler;
    }
    return null;
  }

  /**
   * Lists the names of each workflow operation. Operation names are availalbe for use if there is a registered
   * {@link WorkflowOperationHandler} with an equal {@link WorkflowServiceImpl#WORKFLOW_OPERATION_PROPERTY} property.
   * 
   * @return The {@link List} of available workflow operation names
   */
  protected List<String> listAvailableOperationNames() {
    List<String> list = new ArrayList<String>();
    for (HandlerRegistration reg : getRegisteredHandlers()) {
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
    if (workflow == null || workflow.getId() == null) {
      throw new IllegalArgumentException("Workflow must not be null, and must contain an ID");
    }
    String id = workflow.getId();
    if (workflowDefinitions.containsKey(id)) {
      throw new IllegalStateException("A workflow definition with ID '" + id + "' is already registered.");
    }
    workflowDefinitions.put(id, workflow);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#unregisterWorkflowDefinition(java.lang.String)
   */
  public void unregisterWorkflowDefinition(String workflowDefinitionId) {
    workflowDefinitions.remove(workflowDefinitionId);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#getWorkflowById(long)
   */
  public WorkflowInstanceImpl getWorkflowById(long id) throws WorkflowDatabaseException, NotFoundException {
    try {
      Job job = serviceRegistry.getJob(id);
      if (Status.DELETED.equals(job.getStatus()))
        throw new NotFoundException("Workflow '" + id + "' has been deleted");
      return WorkflowParser.parseWorkflowInstance(job.getPayload());
    } catch (WorkflowParsingException e) {
      throw new IllegalStateException("The workflow job payload is malformed");
    } catch (ServiceRegistryException e) {
      throw new IllegalStateException("Error loading workflow job from the service registry");
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#start(org.opencastproject.workflow.api.WorkflowDefinition,
   *      org.opencastproject.mediapackage.MediaPackage)
   */
  @Override
  public WorkflowInstance start(WorkflowDefinition workflowDefinition, MediaPackage mediaPackage)
          throws WorkflowDatabaseException, WorkflowParsingException {
    if (workflowDefinition == null)
      throw new IllegalArgumentException("workflow definition must not be null");
    if (mediaPackage == null)
      throw new IllegalArgumentException("mediapackage must not be null");
    Map<String, String> properties = new HashMap<String, String>();
    return start(workflowDefinition, mediaPackage, properties);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#start(org.opencastproject.workflow.api.WorkflowDefinition,
   *      org.opencastproject.mediapackage.MediaPackage)
   */
  public WorkflowInstance start(WorkflowDefinition workflowDefinition, MediaPackage mediaPackage,
          Map<String, String> properties) throws WorkflowDatabaseException, WorkflowParsingException {
    try {
      return start(workflowDefinition, mediaPackage, null, properties);
    } catch (NotFoundException e) {
      // should never happen
      throw new IllegalStateException("a null workflow ID caused a NotFoundException.  This is a programming error.");
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#start(org.opencastproject.workflow.api.WorkflowDefinition,
   *      org.opencastproject.mediapackage.MediaPackage, Long, java.util.Map)
   */
  @Override
  public WorkflowInstance start(WorkflowDefinition workflowDefinition, MediaPackage mediaPackage,
          Long parentWorkflowId, Map<String, String> properties) throws WorkflowDatabaseException,
          WorkflowParsingException, NotFoundException {

    if (workflowDefinition == null)
      throw new IllegalArgumentException("workflow definition must not be null");
    if (mediaPackage == null)
      throw new IllegalArgumentException("mediapackage must not be null");
    if (parentWorkflowId != null && getWorkflowById(parentWorkflowId) == null)
      throw new IllegalArgumentException("Parent workflow " + parentWorkflowId + " not found");

    // Create and configure the workflow instance
    WorkflowInstance workflowInstance = new WorkflowInstanceImpl(workflowDefinition, mediaPackage, parentWorkflowId,
            properties);
    workflowInstance = updateConfiguration(workflowInstance, properties);

    try {

      // Before we persist this, extract the metadata
      populateMediaPackageMetadata(workflowInstance.getMediaPackage());

      // Create a new job for this workflow instance
      String workflowDefinitionXml = WorkflowParser.toXml(workflowDefinition);
      String workflowInstanceXml = WorkflowParser.toXml(workflowInstance);
      String mediaPackageXml = MediaPackageParser.getAsXml(mediaPackage);

      List<String> arguments = new ArrayList<String>();
      arguments.add(workflowDefinitionXml);
      arguments.add(mediaPackageXml);
      if (parentWorkflowId != null || properties != null) {
        String parentWorkflowIdString = (parentWorkflowId != null) ? parentWorkflowId.toString() : NULL_PARENT_ID;
        arguments.add(parentWorkflowIdString);
      }
      if (properties != null) {
        arguments.add(mapToString(properties));
      }

      Job job = serviceRegistry.createJob(JOB_TYPE, Operation.START_WORKFLOW.toString(), arguments,
              workflowInstanceXml, false);

      // Have the workflow take on the job's identity
      workflowInstance.setId(job.getId());

      // Add the workflow to the search index and have the job enqueued for dispatch.
      update(workflowInstance);

      return workflowInstance;
    } catch (Throwable t) {
      try {
        workflowInstance.setState(FAILED);
        update(workflowInstance);
      } catch (Exception failureToFail) {
        logger.warn("Unable to update workflow to failed state", failureToFail);
      }
      throw new WorkflowDatabaseException(t);
    }
  }

  protected WorkflowInstance updateConfiguration(WorkflowInstance instance, Map<String, String> properties) {
    if (properties == null)
      return instance;
    try {
      String xml = replaceVariables(WorkflowParser.toXml(instance), properties);
      WorkflowInstanceImpl workflow = (WorkflowInstanceImpl) WorkflowParser.parseWorkflowInstance(xml);
      return workflow;
    } catch (Exception e) {
      throw new IllegalStateException("Unable to replace workflow instance variables", e);
    }
  }

  /**
   * Replaces all occurrances of <code>${.*+}</code> with the property in the provided map, or if not available in the
   * map, from the bundle context properties, if available.
   * 
   * @param source
   *          The source string
   * @param properties
   *          The map of properties to replace
   * @return The resulting string
   */
  protected String replaceVariables(String source, Map<String, String> properties) {
    Matcher matcher = PROPERTY_PATTERN.matcher(source);
    StringBuilder result = new StringBuilder();
    int cursor = 0;
    boolean matchFound = matcher.find();
    if (!matchFound)
      return source;
    while (matchFound) {
      int matchStart = matcher.start();
      int matchEnd = matcher.end();
      result.append(source.substring(cursor, matchStart)); // add the content before the match
      String key = source.substring(matchStart + 2, matchEnd - 1);
      String systemProperty = componentContext == null ? null : componentContext.getBundleContext().getProperty(key);
      String providedProperty = properties.get(key);
      if (isNotBlank(providedProperty)) {
        result.append(providedProperty);
      } else if (isNotBlank(systemProperty)) {
        result.append(systemProperty);
      } else {
        result.append(source.substring(matchStart, matchEnd)); // retain the original matched value
      }
      cursor = matchEnd;
      matchFound = matcher.find();
      if (!matchFound)
        result.append(source.substring(matchEnd, source.length()));
    }
    return result.toString();
  }

  /**
   * Does a lookup of available operation handlers for the given workflow operation.
   * 
   * @param operation
   *          the operation definition
   * @return the handler or <code>null</code>
   */
  protected WorkflowOperationHandler selectOperationHandler(WorkflowOperationInstance operation) {
    List<WorkflowOperationHandler> handlerList = new ArrayList<WorkflowOperationHandler>();
    for (HandlerRegistration handlerReg : getRegisteredHandlers()) {
      if (handlerReg.operationName != null && handlerReg.operationName.equals(operation.getId())) {
        handlerList.add(handlerReg.handler);
      }
    }
    if (handlerList.size() > 1) {
      throw new IllegalStateException("Multiple operation handlers found for operation '" + operation.getId() + "'");
    } else if (handlerList.size() == 1) {
      return handlerList.get(0);
    }
    logger.warn("No workflow operation handlers found for operation '{}'", operation.getId());
    return null;
  }

  /**
   * Executes the workflow.
   * 
   * @param workflow
   *          the workflow instance
   * @throws WorkflowDatabaseException
   *           if the workflow instance can't be updated in the database
   * @throws WorkflowParsingException
   *           if the workflow instance can't be parsed
   */
  protected Job runWorkflow(WorkflowInstance workflow) throws WorkflowDatabaseException, WorkflowParsingException {
    if (!INSTANTIATED.equals(workflow.getState()))
      throw new IllegalStateException("Cannot start a workflow in state '" + workflow.getState() + "'");

    // If this is a new workflow, move to the first operation
    workflow.setState(RUNNING);
    update(workflow);

    WorkflowOperationInstance operation = workflow.getCurrentOperation();

    if (operation == null)
      throw new IllegalStateException("Cannot start a workflow without operations");

    if (operation.getPosition() != 0)
      throw new IllegalStateException("Current operation expected to be first");

    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.START_OPERATION.toString(),
              Arrays.asList(Long.toString(workflow.getId())));
    } catch (ServiceRegistryException e) {
      throw new WorkflowDatabaseException(e);
    }

  }

  /**
   * Executes the workflow's current operation.
   * 
   * @param workflow
   *          the workflow
   * @throws WorkflowDatabaseException
   *           if the workflow can't be updated in the database
   * @throws WorkflowParsingException
   *           if the workflow can't be parsed
   */
  protected void runWorkflowOperation(WorkflowInstance workflow) throws WorkflowDatabaseException,
          WorkflowParsingException {
    WorkflowOperationInstance operation = workflow.getCurrentOperation();
    if (operation == null)
      throw new IllegalStateException("No operation to run, workflow is " + workflow.getState());

    WorkflowOperationWorker worker = new WorkflowOperationWorker(workflow, this);
    WorkflowState currentState = workflow.getState();

    // Execute the operation handler
    WorkflowOperationHandler operationHandler = selectOperationHandler(operation);
    worker.setHandler(operationHandler);
    worker.execute();

    // Move on to the next workflow operation
    if (!PAUSED.equals(workflow.getState())) {
      operation = workflow.next();
    }

    // Is the workflow done?
    if (operation == null) {

      // If we are in failing mode, we were simply working off an error handling workflow
      if (FAILING.equals(workflow.getState())) {
        workflow.setState(FAILED);
      }

      // Otherwise, let's make sure we didn't miss any failed operation, since the workflow state could have been
      // switched to paused while processing the error handling workflow extension
      else if (!FAILED.equals(workflow.getState())) {
        workflow.setState(SUCCEEDED);
        for (WorkflowOperationInstance op : workflow.getOperations()) {
          if (op.getState().equals(WorkflowOperationInstance.OperationState.FAILED)) {
            if (op.isFailWorkflowOnException()) {
              workflow.setState(FAILED);
              break;
            }
          }
        }
      }

      // Save the updated workflow to the database
      update(workflow);

    } else {

      // Somebody might have set the workflow to "paused" from the outside, so take a look a the database first
      WorkflowState dbWorkflowState = null;
      try {
        dbWorkflowState = getWorkflowById(workflow.getId()).getState();
      } catch (WorkflowDatabaseException e) {
        throw new IllegalStateException("The workflow with ID " + workflow.getId()
                + " can not be accessed in the database");
      } catch (NotFoundException e) {
        throw new IllegalStateException("The workflow with ID " + workflow.getId()
                + " can not be found in the database");
      }

      // If somebody changed the workflow state from the outside, that state should take precedence
      if (!dbWorkflowState.equals(currentState)) {
        logger.info("Workflow state for {} was changed to '{}' from the outside", workflow, dbWorkflowState);
        workflow.setState(dbWorkflowState);
      }

      // Save the updated workflow to the database
      update(workflow);

      switch (workflow.getState()) {
        case FAILED:
          break;
        case FAILING:
        case RUNNING:
          try {
            serviceRegistry.createJob(JOB_TYPE, Operation.START_OPERATION.toString(),
                    Arrays.asList(Long.toString(workflow.getId())));
          } catch (ServiceRegistryException e) {
            throw new WorkflowDatabaseException(e);
          }
          break;
        case PAUSED:
        case STOPPED:
        case SUCCEEDED:
          break;
        case INSTANTIATED:
          throw new IllegalStateException("Impossible workflow state found during processing");
      }

    }

  }

  /**
   * Returns the workflow identified by <code>id</code> or <code>null</code> if no such definition was found.
   * 
   * @param id
   *          the workflow definition id
   * @return the workflow
   */
  public WorkflowDefinition getWorkflowDefinitionById(String id) throws NotFoundException, WorkflowDatabaseException {
    WorkflowDefinition def = workflowDefinitions.get(id);
    if (def == null)
      throw new NotFoundException("Workflow definition '" + id + "' not found");
    return def;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#stop(long)
   */
  public WorkflowInstance stop(long workflowInstanceId) throws WorkflowDatabaseException, WorkflowParsingException,
          NotFoundException {
    WorkflowInstanceImpl instance = (WorkflowInstanceImpl) getWorkflowById(workflowInstanceId);
    instance.setState(STOPPED);
    update(instance);
    return instance;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#suspend(long)
   */
  public WorkflowInstance suspend(long workflowInstanceId) throws WorkflowDatabaseException, WorkflowParsingException,
          NotFoundException {
    WorkflowInstanceImpl instance = (WorkflowInstanceImpl) getWorkflowById(workflowInstanceId);
    instance.setState(PAUSED);
    update(instance);
    return instance;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#resume(long)
   */
  @Override
  public WorkflowInstance resume(long id) throws WorkflowDatabaseException, WorkflowParsingException, NotFoundException {
    return resume(id, new HashMap<String, String>());
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#resume(long)
   */
  public WorkflowInstance resume(long workflowInstanceId, Map<String, String> properties)
          throws WorkflowDatabaseException, WorkflowParsingException, NotFoundException {

    WorkflowInstance workflowInstance = getWorkflowById(workflowInstanceId);
    workflowInstance = updateConfiguration(workflowInstance, properties);
    update(workflowInstance);

    // Set the job to queued, so it gets picked up again
    Job job;
    try {
      job = serviceRegistry.getJob(workflowInstanceId);
      job.setStatus(Status.QUEUED);
      job.setOperation(Operation.RESUME.toString());
      serviceRegistry.updateJob(job);
    } catch (ServiceRegistryException e) {
      throw new WorkflowDatabaseException(e);
    }

    return workflowInstance;
  }

  /**
   * Resumes a suspended workflow instance, applying new properties to the workflow.
   * 
   * @param workflowInstance
   *          the workflow to resume
   * @param properties
   *          the properties to apply to the resumed workflow
   * @return the workflow instance
   * @throws NotFoundException
   *           if no paused workflow with this identifier exists
   * @throws WorkflowDatabaseException
   *           if there is a problem accessing the workflow instance in persistence
   * @throws WorkflowParsingException
   *           if there is a problem parsing the workflow instance from persistence
   */
  protected WorkflowInstance resume(WorkflowInstance workflowInstance, Map<String, String> properties)
          throws WorkflowDatabaseException, WorkflowParsingException, NotFoundException {
    workflowInstance = updateConfiguration(workflowInstance, properties);
    workflowInstance.setState(RUNNING);
    runWorkflowOperation(workflowInstance);
    return workflowInstance;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#update(org.opencastproject.workflow.api.WorkflowInstance)
   */
  public void update(WorkflowInstance workflowInstance) throws WorkflowDatabaseException {
    WorkflowInstance originalWorkflowInstance = null;
    try {
      originalWorkflowInstance = getWorkflowById(workflowInstance.getId());
    } catch (NotFoundException e) {
      // That's fine, it's a new workflow instance
    }

    // Synchronize the job status with the workflow
    WorkflowState workflowState = workflowInstance.getState();
    String xml;
    try {
      xml = WorkflowParser.toXml(workflowInstance);
    } catch (Exception e) {
      // Can't happen, since we are converting from an in-memory object
      throw new IllegalStateException("In-memory workflow instance could not be serialized", e);
    }

    Job job = null;
    try {
      job = serviceRegistry.getJob(workflowInstance.getId());
      job.setPayload(xml);

      // Synchronize workflow and job state
      switch (workflowState) {
        case FAILED:
          job.setStatus(Status.FAILED);
          break;
        case FAILING:
          break;
        case INSTANTIATED:
          job.setStatus(Status.QUEUED);
          break;
        case PAUSED:
          job.setStatus(Status.PAUSED);
          break;
        case RUNNING:
          job.setStatus(Status.RUNNING);
          break;
        case STOPPED:
          job.setStatus(Status.DELETED);
          break;
        case SUCCEEDED:
          job.setStatus(Status.FINISHED);
          break;
        default:
          throw new IllegalStateException("Found a workflow state that is not handled");
      }

      // Update the search index
      index.update(workflowInstance);

      // Update the service registry
      serviceRegistry.updateJob(job);

    } catch (ServiceRegistryException e) {
      throw new WorkflowDatabaseException(e);
    } catch (NotFoundException e) {
      // this should never happen, since we create the job if it doesn't already exist
      throw new WorkflowDatabaseException(e);
    }

    try {
      WorkflowInstance clone = WorkflowParser.parseWorkflowInstance(WorkflowParser.toXml(workflowInstance));
      fireListeners(originalWorkflowInstance, clone);
    } catch (Exception e) {
      // Can't happen, since we are converting from an in-memory object
      throw new IllegalStateException("In-memory workflow instance could not be serialized", e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#countWorkflowInstances()
   */
  @Override
  public long countWorkflowInstances() throws WorkflowDatabaseException {
    return index.countWorkflowInstances(null, null);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#countWorkflowInstances(org.opencastproject.workflow.api.WorkflowInstance.WorkflowState,
   *      java.lang.String)
   */
  @Override
  public long countWorkflowInstances(WorkflowState state, String operation) throws WorkflowDatabaseException {
    return index.countWorkflowInstances(state, operation);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#getStatistics()
   */
  @Override
  public WorkflowStatistics getStatistics() throws WorkflowDatabaseException {
    return index.getStatistics();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#getWorkflowInstances(org.opencastproject.workflow.api.WorkflowQuery)
   */
  public WorkflowSet getWorkflowInstances(WorkflowQuery query) throws WorkflowDatabaseException {
    return index.getWorkflowInstances(query);
  }

  /**
   * Callback for workflow operations that were throwing an exception. This implementation assumes that the operation
   * worker has already adjusted the current operation's state appropriately.
   * 
   * @param workflow
   *          the workflow instance
   * @param e
   *          the exception
   * @throws WorkflowParsingException
   */
  void handleOperationException(WorkflowInstance workflow, Exception e) throws WorkflowDatabaseException,
          WorkflowParsingException {
    // Add the exception's localized message to the workflow instance
    workflow.addErrorMessage(e.getLocalizedMessage());

    WorkflowOperationInstance currentOperation = workflow.getCurrentOperation();
    String errorDefId = currentOperation.getExceptionHandlingWorkflow();

    // Adjust the workflow state according to the setting on the operation
    if (currentOperation.isFailWorkflowOnException()) {
      if (StringUtils.isBlank(errorDefId)) {
        workflow.setState(FAILED);
      } else {
        workflow.setState(FAILING);

        // Remove the rest of the original workflow
        int currentOperationPosition = workflow.getOperations().indexOf(currentOperation);
        List<WorkflowOperationInstance> operations = new ArrayList<WorkflowOperationInstance>();
        operations.addAll(workflow.getOperations().subList(0, currentOperationPosition + 1));
        workflow.setOperations(operations);

        // Append the operations
        WorkflowDefinition errorDef = null;
        try {
          errorDef = getWorkflowDefinitionById(errorDefId);
          workflow.extend(errorDef);
        } catch (NotFoundException notFoundException) {
          throw new IllegalStateException("Unable to find the error workflow definition '" + errorDefId + "'");
        }
      }
    }

    // Fail the current operation
    currentOperation.setState(OperationState.FAILED);

    update(workflow);
  }

  /**
   * Callback for workflow operation handlers that executed and finished without exception. This implementation assumes
   * that the operation worker has already adjusted the current operation's state appropriately.
   * 
   * @param workflow
   *          the workflow instance
   * @param result
   *          the workflow operation result
   * @throws WorkflowDatabaseException
   *           if updating the workflow fails
   */
  void handleOperationResult(WorkflowInstance workflow, WorkflowOperationResult result)
          throws WorkflowDatabaseException, WorkflowParsingException {

    // Get the operation and its handler
    WorkflowOperationInstanceImpl currentOperation = (WorkflowOperationInstanceImpl) workflow.getCurrentOperation();
    WorkflowOperationHandler handler = getWorkflowOperationHandler(currentOperation.getId());

    // Create an operation result for the lazy or else update the workflow's media package
    if (result == null) {
      logger.warn("Handling a null operation result for workflow {} in operation {}", workflow.getId(),
              currentOperation.getId());
      result = new WorkflowOperationResultImpl(workflow.getMediaPackage(), null, Action.CONTINUE, 0);
    } else {
      MediaPackage mp = result.getMediaPackage();
      if (mp != null) {
        workflow.setMediaPackage(mp);
      }
    }

    // The action to take
    Action action = result.getAction();

    // Update the workflow configuration
    workflow = updateConfiguration(workflow, result.getProperties());

    // Adjust workflow statistics
    currentOperation.setTimeInQueue(result.getTimeInQueue());

    // Adjust the operation state
    switch (action) {
      case CONTINUE:
        currentOperation.setState(OperationState.SUCCEEDED);
        break;
      case PAUSE:
        if (!(handler instanceof ResumableWorkflowOperationHandler)) {
          throw new IllegalStateException("Operation " + currentOperation.getId() + " is not resumable");
        }

        // Set abortable and continuable to default values
        currentOperation.setContinuable(result.allowsContinue());
        currentOperation.setAbortable(result.allowsAbort());

        ResumableWorkflowOperationHandler resumableHandler = (ResumableWorkflowOperationHandler) handler;
        try {
          URL url = resumableHandler.getHoldStateUserInterfaceURL(workflow);
          if (url != null) {
            String holdActionTitle = resumableHandler.getHoldActionTitle();
            ((WorkflowOperationInstanceImpl) currentOperation).setHoldActionTitle(holdActionTitle);
            ((WorkflowOperationInstanceImpl) currentOperation).setHoldStateUserInterfaceUrl(url);
          }
        } catch (WorkflowOperationException e) {
          logger.warn("unable to replace workflow ID in the hold state URL", e);
        }

        workflow.setState(PAUSED);
        currentOperation.setState(OperationState.PAUSED);
        break;
      case SKIP:
        currentOperation.setState(OperationState.SKIPPED);
        break;
      default:
        throw new IllegalStateException("Unknown action '" + action + "' returned");
    }

    update(workflow);
  }

  /**
   * Reads the available metadata from the dublin core catalog (if there is one).
   * 
   * @param mp
   *          the media package
   */
  protected void populateMediaPackageMetadata(MediaPackage mp) {
    if (metadataServices.size() == 0) {
      logger.warn("No metadata services are registered, so no mediapackage metadata can be extracted from catalogs");
      return;
    }
    for (MediaPackageMetadataService metadataService : metadataServices) {
      MediaPackageMetadata metadata = metadataService.getMetadata(mp);
      if (metadata != null) {

        // Series identifier
        if (isBlank(mp.getSeries()) && isNotBlank(metadata.getSeriesIdentifier())) {
          mp.setSeries(metadata.getSeriesIdentifier());
        }

        // Series title
        if (isBlank(mp.getSeriesTitle()) && isNotBlank(metadata.getSeriesTitle())) {
          mp.setSeriesTitle(metadata.getSeriesTitle());
        }

        // Episode title
        if (isBlank(mp.getTitle()) && isNotBlank(metadata.getTitle())) {
          mp.setTitle(metadata.getTitle());
        }

        // Episode date
        if (mp.getDate().getTime() == 0 && metadata.getDate() != null) {
          mp.setDate(metadata.getDate());
        }

        // Episode subjects
        if (mp.getSubjects().length == 0 && metadata.getSubjects().length > 0) {
          for (String subject : metadata.getSubjects()) {
            mp.addSubject(subject);
          }
        }

        // Episode contributers
        if (mp.getContributors().length == 0 && metadata.getContributors().length > 0) {
          for (String contributor : metadata.getContributors()) {
            mp.addContributor(contributor);
          }
        }

        // Episode creators
        if (mp.getCreators().length == 0 && metadata.getCreators().length > 0) {
          for (String creator : metadata.getCreators()) {
            mp.addCreator(creator);
          }
        }

        // Episode license
        if (isBlank(mp.getLicense()) && isNotBlank(metadata.getLicense())) {
          mp.setLicense(metadata.getLicense());
        }

        // Episode language
        if (isBlank(mp.getLanguage()) && isNotBlank(metadata.getLanguage())) {
          mp.setLanguage(metadata.getLanguage());
        }

      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JobProducer#getJobType()
   */
  @Override
  public String getJobType() {
    return JOB_TYPE;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JobProducer#acceptJob(org.opencastproject.job.api.Job, java.lang.String,
   *      java.util.List)
   */
  @Override
  public void acceptJob(Job job, String operation, List<String> arguments) throws ServiceRegistryException {
    Operation op = null;
    WorkflowInstance workflowInstance = null;
    try {
      try {
        op = Operation.valueOf(operation);
        switch (op) {
          case START_WORKFLOW:
            workflowInstance = WorkflowParser.parseWorkflowInstance(job.getPayload());
            logger.info("Starting new workflow {}", workflowInstance);
            runWorkflow(workflowInstance);
            break;
          case RESUME:
            workflowInstance = WorkflowParser.parseWorkflowInstance(job.getPayload());
            workflowInstance = getWorkflowById(workflowInstance.getId());
            Map<String, String> properties = null;
            if (arguments.size() > 1)
              stringToMap(arguments.get(1));
            logger.info("Resuming workflow {}", workflowInstance);
            resume(workflowInstance, properties);
            break;
          case START_OPERATION:
            workflowInstance = getWorkflowById(Long.parseLong(arguments.get(0)));
            logger.info("Starting workflow operation {}", workflowInstance.getCurrentOperation());
            runWorkflowOperation(workflowInstance);
            break;
          default:
            throw new IllegalStateException("Don't know how to handle operation '" + operation + "'");
        }
      } catch (IllegalArgumentException e) {
        throw new ServiceRegistryException("This service can't handle operations of type '" + op + "'", e);
      } catch (IndexOutOfBoundsException e) {
        throw new ServiceRegistryException("This argument list for operation '" + op + "' does not meet expectations",
                e);
      }
    } catch (Exception e) {
      try {
        if (workflowInstance != null) {
          workflowInstance.setState(FAILED);
          update(workflowInstance);
        } else {
          logger.warn("Unable to parse workflow instance", e);
        }
      } catch (WorkflowDatabaseException e1) {
        throw new ServiceRegistryException(e1);
      }
      if (e instanceof ServiceRegistryException)
        throw (ServiceRegistryException) e;
      throw new ServiceRegistryException("Error handling operation '" + op + "'", e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JobProducer#countJobs(org.opencastproject.job.api.Job.Status)
   */
  @Override
  public long countJobs(Status status) throws ServiceRegistryException {
    return serviceRegistry.count(JOB_TYPE, status);
  }

  /**
   * Converts a Map<String, String> to s key=value\n string, suitable for the properties form parameter expected by the
   * workflow rest endpoint.
   * 
   * @param props
   *          The map of strings
   * @return the string representation
   */
  private String mapToString(Map<String, String> props) {
    if (props == null)
      return null;
    StringBuilder sb = new StringBuilder();
    for (Entry<String, String> entry : props.entrySet()) {
      sb.append(entry.getKey());
      sb.append("=");
      sb.append(entry.getValue());
      sb.append("\n");
    }
    return sb.toString();
  }

  /**
   * Creates a map from a serialized version that was created using {@link #mapToString(Map)}.
   * 
   * @param string
   *          the serialized map
   * @return the map or <code>null</code> if <code>string</code> was null in the first place
   * @throws IOException
   *           if reading the map fails
   */
  private Map<String, String> stringToMap(String string) throws IOException {
    if (string == null)
      return null;
    Map<String, String> map = new HashMap<String, String>();
    Properties properties = new Properties();
    properties.load(IOUtils.toInputStream(string));
    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
      map.put((String) entry.getKey(), (String) entry.getValue());
    }
    return map;
  }

  /**
   * Callback for the OSGi environment to register with the <code>ServiceRegistry</code>.
   * 
   * @param registry
   *          the service registry
   */
  void setServiceRegistry(ServiceRegistry registry) {
    this.serviceRegistry = registry;
  }

  /**
   * Sets the DAO implementation to use in this service.
   * 
   * @param dao
   *          The dao to use for persistence
   */
  void setDao(WorkflowServiceIndex dao) {
    this.index = dao;
  }

  /**
   * Callback to set the metadata service
   * 
   * @param service
   *          the metadata service
   */
  void addMetadataService(MediaPackageMetadataService service) {
    metadataServices.add(service);
  }

  /**
   * Callback to remove a mediapackage metadata service.
   * 
   * @param service
   *          the mediapackage metadata service to remove
   */
  void removeMetadataService(MediaPackageMetadataService service) {
    metadataServices.remove(service);
  }

  /**
   * A tuple of a workflow operation handler and the name of the operation it handles
   */
  public static class HandlerRegistration {

    private WorkflowOperationHandler handler;
    private String operationName;

    public HandlerRegistration(String operationName, WorkflowOperationHandler handler) {
      if (operationName == null)
        throw new IllegalArgumentException("Operation name cannot be null");
      if (handler == null)
        throw new IllegalArgumentException("Handler cannot be null");
      this.operationName = operationName;
      this.handler = handler;
    }

    public WorkflowOperationHandler getHandler() {
      return handler;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + handler.hashCode();
      result = prime * result + operationName.hashCode();
      return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      HandlerRegistration other = (HandlerRegistration) obj;
      if (!handler.equals(other.handler))
        return false;
      if (!operationName.equals(other.operationName))
        return false;
      return true;
    }
  }

}
