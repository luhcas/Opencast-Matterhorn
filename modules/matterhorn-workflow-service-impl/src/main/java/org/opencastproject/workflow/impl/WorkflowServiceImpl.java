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

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageMetadata;
import org.opencastproject.metadata.api.MediaPackageMetadataService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.ResumableWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowException;
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

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
public class WorkflowServiceImpl implements WorkflowService, ManagedService {

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(WorkflowServiceImpl.class);

  /** The number of threads to start with in the workflow instance thread pool */
  protected static final int DEFAULT_THREADS = 1;

  /** The configuration key that defines the number of threads to use in the workflow instance thread pool */
  protected static final String WORKFLOW_THREADS_CONFIGURATION = "org.opencastproject.concurrent.jobs";

  /** The pattern used by workfow operation configuration keys **/
  public static final Pattern PROPERTY_PATTERN = Pattern.compile("\\$\\{.+?\\}");

  /** TODO: Remove references to the component context once felix scr 1.2 becomes available */
  protected ComponentContext componentContext = null;

  /** The collection of workflow definitions */
  protected Map<String, WorkflowDefinition> workflowDefinitions = new HashMap<String, WorkflowDefinition>();

  /** The metadata services */
  private SortedSet<MediaPackageMetadataService> metadataServices;

  /** The data access object responsible for storing and retrieving workflow instances */
  protected WorkflowServiceImplDao dao;

  /** The list of workflow listeners */
  private List<WorkflowListener> listeners = new CopyOnWriteArrayList<WorkflowListener>();

  /** The thread pool */
  protected ThreadPoolExecutor executorService;

  /** The thread pool to use for firing listeners */
  protected ThreadPoolExecutor listenerExecutorService;

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  @Override
  public void updated(@SuppressWarnings("rawtypes") Dictionary properties) throws ConfigurationException {
    String threadPoolConfig = (String) properties.get(WORKFLOW_THREADS_CONFIGURATION);
    if (threadPoolConfig != null) {
      try {
        int threads = Integer.parseInt(threadPoolConfig);
        if (this.executorService == null) {
          logger.debug("Creating a new thread pool of size {}", threads);
          this.executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(threads);
        } else {
          logger.debug("Resetting thread pool size to {}", threads);
          this.executorService.setCorePoolSize(threads);
        }
      } catch (NumberFormatException e) {
        logger.warn(e.getMessage());
      }
    }
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
   * Sets the DAO implementation to use in this service.
   * 
   * @param dao
   *          The dao to use for persistence
   */
  public void setDao(WorkflowServiceImplDao dao) {
    this.dao = dao;
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
   * Activate this service implementation via the OSGI service component runtime
   */
  public void activate(ComponentContext componentContext) {
    this.componentContext = componentContext;
    logger.debug("Creating a new thread pool with default size of {}", DEFAULT_THREADS);
    executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(DEFAULT_THREADS);
    listenerExecutorService = (ThreadPoolExecutor) Executors.newCachedThreadPool();
  }

  /**
   * Deactivate this service.
   */
  public void deactivate() {
    if (executorService != null && !executorService.isShutdown()) {
      executorService.shutdown();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#addWorkflowListener(org.opencastproject.workflow.api.WorkflowListener)
   */
  @Override
  public void addWorkflowListener(WorkflowListener listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Listener must not be null");
    }
    listeners.add(listener);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#removeWorkflowListener(org.opencastproject.workflow.api.WorkflowListener)
   */
  @Override
  public void removeWorkflowListener(WorkflowListener listener) {
    if (listener != null) {
      listeners.remove(listener);
    }
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
    return dao.getWorkflowById(id);
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
    WorkflowInstanceImpl workflowInstance = new WorkflowInstanceImpl(workflowDefinition, mediaPackage,
            parentWorkflowId, properties);
    workflowInstance = updateConfiguration(workflowInstance, properties);

    try {
      // Before we persist this, extract the metadata
      populateMediaPackageMetadata(workflowInstance.getMediaPackage());
      update(workflowInstance);

      // Have the job executed
      logger.info("Starting a new workflow: {}", workflowInstance);
      run(workflowInstance);

      return workflowInstance;
    } catch (Throwable t) {
      try {
        workflowInstance.setState(WorkflowState.FAILED);
        update(workflowInstance);
      } catch (Exception failureToFail) {
        logger.warn("Unable to update workflow to failed state", failureToFail);
      }
      throw new WorkflowDatabaseException(t);
    }
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

  protected WorkflowInstanceImpl updateConfiguration(WorkflowInstanceImpl instance, Map<String, String> properties) {
    if (properties == null)
      return instance;
    try {
      String xml = replaceVariables(WorkflowParser.toXml(instance), properties);
      WorkflowInstanceImpl workflow = (WorkflowInstanceImpl) WorkflowParser.parseWorkflowInstance(xml);
      workflow.init(); // needed to keep the current operation setting intact
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

  protected void run(final WorkflowInstanceImpl wfi) throws WorkflowDatabaseException, WorkflowParsingException {
    run(wfi, null);
  }

  protected void run(final WorkflowInstanceImpl wfi, final Map<String, String> properties)
          throws WorkflowDatabaseException, WorkflowParsingException {
    wfi.setState(WorkflowInstance.WorkflowState.RUNNING);
    WorkflowOperationInstance operation = wfi.getCurrentOperation();
    if (operation == null) {
      operation = wfi.next();
    }
    update(wfi);
    WorkflowOperationHandler operationHandler = selectOperationHandler(operation);
    executorService.execute(new WorkflowOperationWorker(operationHandler, wfi, properties, this));
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
    instance.setState(WorkflowState.STOPPED);
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
    instance.setState(WorkflowState.PAUSED);
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
    WorkflowInstanceImpl workflowInstance = updateConfiguration(getWorkflowById(workflowInstanceId), properties);

    // Update the workflow instance
    workflowInstance.setState(WorkflowInstance.WorkflowState.RUNNING);
    update(workflowInstance);

    // Continue running the workflow
    run(workflowInstance, properties);

    // Return the latest (running) version of the workflow instance
    return workflowInstance;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#update(org.opencastproject.workflow.api.WorkflowInstance)
   */
  public void update(WorkflowInstance workflowInstance) throws WorkflowDatabaseException, WorkflowParsingException {
    WorkflowInstance databaseInstance = null;
    try {
      databaseInstance = dao.getWorkflowById(workflowInstance.getId());
    } catch (NotFoundException e) {
      // That's fine, it's a new workflow instance
    }
    dao.update(workflowInstance);
    WorkflowInstance clone = WorkflowParser.parseWorkflowInstance(WorkflowParser.toXml(workflowInstance));
    fireListeners(databaseInstance, clone);
  }

  /**
   * Removes a workflow instance.
   * 
   * @param id
   *          The id of the workflow instance to remove
   */
  public void removeFromDatabase(long id) throws WorkflowDatabaseException, NotFoundException {
    dao.remove(id);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#countWorkflowInstances()
   */
  @Override
  public long countWorkflowInstances() throws WorkflowDatabaseException {
    return dao.countWorkflowInstances(null, null);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#countWorkflowInstances(org.opencastproject.workflow.api.WorkflowInstance.WorkflowState,
   *      java.lang.String)
   */
  @Override
  public long countWorkflowInstances(WorkflowState state, String operation) throws WorkflowDatabaseException {
    return dao.countWorkflowInstances(state, operation);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#getStatistics()
   */
  @Override
  public WorkflowStatistics getStatistics() throws WorkflowDatabaseException {
    return dao.getStatistics();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#getWorkflowInstances(org.opencastproject.workflow.api.WorkflowQuery)
   */
  public WorkflowSet getWorkflowInstances(WorkflowQuery query) throws WorkflowDatabaseException {
    return dao.getWorkflowInstances(query);
  }

  /**
   * Callback for workflow operations that were throwing an exception. This implementation assumes that the operation
   * worker has already adjusted the current operation's state appropriately.
   * 
   * @param workflow
   *          the workflow instance
   * @param e
   *          the exception
   */
  void handleOperationException(WorkflowInstanceImpl workflow, Exception e) throws WorkflowDatabaseException {
    // Add the exception's localized message to the workflow instance
    workflow.addErrorMessage(e.getLocalizedMessage());

    WorkflowOperationInstance currentOperation = workflow.getCurrentOperation();
    if (currentOperation.isFailWorkflowOnException()) {
      String errorDefId = currentOperation.getExceptionHandlingWorkflow();

      // Is there an error handler for this operation
      if (errorDefId != null) {
        int currentOperationPosition = workflow.getOperations().indexOf(currentOperation);
        List<WorkflowOperationInstance> operations = new ArrayList<WorkflowOperationInstance>();
        operations.addAll(workflow.getOperations().subList(0, currentOperationPosition + 1));

        WorkflowDefinition errorDef = null;
        try {
          errorDef = getWorkflowDefinitionById(errorDefId);
        } catch (NotFoundException notFoundException) {
          throw new IllegalStateException("Unable to find the error workflow definition '" + errorDefId + "'");
        }
        for (WorkflowOperationDefinition def : errorDef.getOperations()) {
          operations.add(new WorkflowOperationInstanceImpl(def));
        }
        workflow.setOperations(operations);
      }
    }

    currentOperation.setState(OperationState.FAILED);
    try {
      update(workflow);
      handleOperationResult(workflow, new WorkflowOperationResultImpl(workflow.getMediaPackage(), null,
              Action.CONTINUE, 0));
    } catch (WorkflowException workflowException) {
      // There is nothing we can do at this point.
      logger.error("Unable to save the workflow instance state to the database.", workflowException);
    }
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
  void handleOperationResult(WorkflowInstanceImpl workflow, WorkflowOperationResult result)
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

    // We are asked to pause operation and workflow
    if (Action.PAUSE.equals(action)) {
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
      workflow.setState(WorkflowState.PAUSED);
      update(workflow);
      return;
    }

    // Somebody might have set the workflow to "paused" from the outside, so take a look a the database first
    WorkflowState dbWorkflowState = null;
    try {
      dbWorkflowState = getWorkflowById(workflow.getId()).getState();
      workflow.setState(dbWorkflowState);
    } catch (WorkflowDatabaseException e) {
      throw new IllegalStateException("The workflow with ID " + workflow.getId()
              + " can not be accessed in the database");
    } catch (NotFoundException e) {
      throw new IllegalStateException("The workflow with ID " + workflow.getId() + " can not be found in the database");
    }

    // If the workflow was paused or stopped while the operation was still working, accept the updated mediapackage
    // and properties, but do not continue on.
    if (WorkflowState.PAUSED.equals(dbWorkflowState) || WorkflowState.STOPPED.equals(dbWorkflowState)) {
      update(workflow);
      return;
    }

    // Move on to the next workflow operation
    WorkflowOperationInstance nextOperation = workflow.next(); // Be careful... this increments the current operation

    // Is the workflow done?
    if (nextOperation == null) {
      workflow.setState(WorkflowState.SUCCEEDED);
      for (WorkflowOperationInstance op : workflow.getOperations()) {
        if (op.getState().equals(WorkflowOperationInstance.OperationState.FAILED)) {
          if (op.isFailWorkflowOnException()) {
            workflow.setState(WorkflowState.FAILED);
            break;
          }
        }
      }
      update(workflow);
    } else {
      this.run(workflow);
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
}
