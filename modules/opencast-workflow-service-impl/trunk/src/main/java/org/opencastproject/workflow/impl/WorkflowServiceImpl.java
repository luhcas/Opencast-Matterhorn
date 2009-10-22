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
import org.opencastproject.util.PathSupport;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowDefinitionList;
import org.opencastproject.workflow.api.WorkflowDefinitionListImpl;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationDefinition;
import org.opencastproject.workflow.api.WorkflowOperationDefinitionListImpl;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowSet;
import org.opencastproject.workflow.api.WorkflowInstance.State;
import org.opencastproject.workflow.impl.solr.SolrConnection;
import org.opencastproject.workflow.impl.solr.SolrIndexManager;
import org.opencastproject.workflow.impl.solr.SolrRequester;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.core.SolrCore;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Implements {@link WorkflowService} with in-memory data structures to hold {@link WorkflowOperation}s and
 * {@link WorkflowInstance}s. {@link WorkflowOperationHandler}s are looked up in the OSGi service registry based on the
 * "opencast.workflow.operation" property. If the {@link WorkflowOperationHandler}'s "opencast.workflow.operation"
 * service registration property matches {@link WorkflowOperation#getName()}, then the factory returns a
 * {@link WorkflowOperationRunner} to handle that operation. This allows for custom runners to be added or
 * modified without affecting the workflow service itself.
 */
public class WorkflowServiceImpl implements WorkflowService, ManagedService {

  /** Logging facility */
  private static final Logger log_ = LoggerFactory.getLogger(WorkflowServiceImpl.class);

  /** The service registration property we use to identify which workflow operation a {@link WorkflowOperationHandler} should handle. */
  protected static final String WORKFLOW_OPERATION_PROPERTY = "workflow.operation";

  /** The service registration property we use to identify registered {@link WorkflowDefinition}s. */
  protected static final String WORKFLOW_DEFINITION_PROPERTY = "workflow.definition.title";

  /** The collection of workflow definition registrations */
  protected Map<String, ServiceRegistration> workflowDefinitionRegistrations = new HashMap<String, ServiceRegistration>();

  /** Connection to the solr database */
  private SolrConnection solrConnection = null;

  /** Solr query execution */
  private SolrRequester solrRequester = null;

  /** Manager for the solr search index */
  private SolrIndexManager solrIndexManager = null;
  
  /** The solr root directory */
  private String solrRoot = null;

  /** A collection of the running workflow threads */
  protected Map<String, Thread> threadMap = new HashMap<String, Thread>();

  /** The OSGi component context, which we use to do service lookups */
  protected ComponentContext componentContext;

  /**
   * Creates a solr service that puts its data into the given root directory. If the directory doesn't exist, it will be
   * created.
   * 
   * @param solrRoot
   *          the solr root directory
   */
  public WorkflowServiceImpl(String solrRoot) {
    this.solrRoot = solrRoot;
  }

  /**
   * Creates a new workflow service instance.
   */
  public WorkflowServiceImpl() {
    this(System.getProperty("java.io.tmpdir") + File.separator + "opencast" + File.separator + "workflows");
  }

  /**
   * Activate this service implementation via the OSGI service component runtime
   * 
   * @param componentContext The component context that's instantiating this service.
   */
  public void activate(ComponentContext componentContext) {
    this.componentContext = componentContext;
    setupSolr(solrRoot);
  }
  
  /**
   * Deactivate this service.
   */
  public void deactivate() {
    try {
      solrConnection.destroy();
    } catch (Throwable t) {
      log_.error("Error closing the solr connection");
    }
  }

  /**
   * {@inheritDoc}
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  @SuppressWarnings("unchecked")
  public void updated(Dictionary props) throws ConfigurationException {
    // Update any configuration properties here
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowService#listAvailableWorkflowDefinitions()
   */
  public WorkflowDefinitionList listAvailableWorkflowDefinitions() {
    WorkflowDefinitionList list = new WorkflowDefinitionListImpl();
    for(Object registeredDefinition : componentContext.locateServices("WORKFLOW_DEFINITIONS")) {
      list.add((WorkflowDefinition)registeredDefinition);
    }
    return list;
  }

  /**
   * {@inheritDoc}
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
   * Tests the workflow definition for its runnability. This method is a helper for {@link #isRunnable(WorkflowDefinition)}
   * that is suited for recursive calling.
   * 
   * @param workflowDefinition the definition to test
   * @param availableOperations
   * list of currently available operation handlers
   * @param checkedWorkflows
   * list of checked workflows, used to avoid circular checking
   * @return <code>true</code> if all bits and pieces used for executing <code>workflowDefinition</code> are in place
   */
  private boolean isRunnable(WorkflowDefinition workflowDefinition, List<String> availableOperations, List<WorkflowDefinition> checkedWorkflows) {
    if (checkedWorkflows.contains(workflowDefinition))
      return true;

    // Test availability of operation handler and catch workflows
    for(WorkflowOperationDefinition op : workflowDefinition.getOperations()) { 
      if(! availableOperations.contains(op.getName())) {
        log_.info(workflowDefinition + " is not runnable due to missing operation " + op);
        return false;
      }
      String catchWorkflow = op.getExceptionHandlingWorkflow(); 
      if (catchWorkflow != null) {
        WorkflowDefinition catchWorkflowDefinition = getWorkflowDefinitionByName(catchWorkflow);
        if (catchWorkflowDefinition == null) {
          log_.info(workflowDefinition + " is not runnable due to missing catch workflow " + catchWorkflow + " on operation " + op);
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
   * Lists the names of each workflow operation.  Operation names are availalbe for use if there is a registered
   * {@link WorkflowOperationHandler} with an equal {@link WorkflowServiceImpl#WORKFLOW_OPERATION_PROPERTY} property.
   * 
   * @return The {@link List} of available workflow operation names
   */
  protected List<String> listAvailableOperationNames() {
    try {
      List<String> list = new ArrayList<String>();
      for(ServiceReference ref :
        componentContext.getBundleContext().getAllServiceReferences(WorkflowOperationHandler.class.getName(), null)) {
        list.add((String)ref.getProperty(WORKFLOW_OPERATION_PROPERTY));
      }
      return list;
    } catch (InvalidSyntaxException e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowService#registerWorkflowDefinition(org.opencastproject.workflow.api.WorkflowDefinition)
   */
  public void registerWorkflowDefinition(WorkflowDefinition workflow) {
    Hashtable<String, String> props = new Hashtable<String, String>();
    props.put(WORKFLOW_DEFINITION_PROPERTY, workflow.getTitle());
    ServiceRegistration reg = componentContext.getBundleContext().registerService(WorkflowDefinition.class.getName(), workflow, props);
    workflowDefinitionRegistrations.put(workflow.getTitle(), reg);
  }

  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowService#unregisterWorkflowDefinition(org.opencastproject.workflow.api.WorkflowDefinition)
   */
  public void unregisterWorkflowDefinition(String workflowTitle) {
    ServiceRegistration reg = workflowDefinitionRegistrations.get(workflowTitle);
    if(reg == null) {throw new IllegalArgumentException("No workflow registered with title " + workflowTitle);}
    workflowDefinitionRegistrations.remove(workflowTitle);
    reg.unregister();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#getWorkflowById(java.lang.String)
   */
  public WorkflowInstance getWorkflowById(String id) {
    try {
      WorkflowSet set = solrRequester.getWorkflowById(id);
      if(set.getItems().length == 0) return null;
      return set.getItems()[0];
    } catch (SolrServerException e) {
      throw new WorkflowDatabaseException(e);
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowService#getWorkflowsByEpisode(java.lang.String)
   */
  public WorkflowSet getWorkflowsByEpisode(String episodeId) throws WorkflowDatabaseException {
    try {
      return solrRequester.getWorkflowsByEpisodeId(episodeId);
    } catch (SolrServerException e) {
      throw new WorkflowDatabaseException(e);
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowService#getWorkflowsByMediaPackage(java.lang.String)
   */
  public WorkflowSet getWorkflowsByMediaPackage(String mediaPackageId) {
    try {
      return solrRequester.getWorkflowsByMediaPackageId(mediaPackageId);
    } catch (SolrServerException e) {
      throw new WorkflowDatabaseException(e);
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowService#getWorkflowsInState(org.opencastproject.workflow.api.WorkflowInstance.State)
   */
  public WorkflowSet getWorkflowsInState(State state, int offset, int limit) {
    try {
      return solrRequester.getWorkflowsInState(state.name(), offset, limit);
    } catch (SolrServerException e) {
      throw new WorkflowDatabaseException(e);
    }
  }
  
  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#start(org.opencastproject.workflow.api.WorkflowDefinition,
   *      org.opencastproject.media.mediapackage.MediaPackage)
   */
  public WorkflowInstance start(WorkflowDefinition workflowDefinition, MediaPackage mediaPackage,
          Map<String, String> properties) {
    WorkflowInstanceImpl workflowInstance = new WorkflowInstanceImpl(workflowDefinition);
    workflowInstance.setId(UUID.randomUUID().toString());
    try {
      workflowInstance.setSourceMediaPackageType(MediapackageType.fromXml(mediaPackage.toXml()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    HashMap<String, String> map = new HashMap<String, String>();
    if(properties != null) map.putAll(properties);
    workflowInstance.setProperties(map);
    workflowInstance.setState(WorkflowInstance.State.RUNNING.name());
    update(workflowInstance);
    run(workflowInstance);
    return workflowInstance;
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
    ServiceReference[] serviceRefs = null;
    try {
      serviceRefs = componentContext.getBundleContext().getAllServiceReferences(WorkflowOperationHandler.class.getName(), null);
    } catch (InvalidSyntaxException e) {
      throw new RuntimeException(e);
    }
    if (serviceRefs == null || serviceRefs.length == 0) {
      log_.info("No Workflow Operation Handlers registered");
      return null;
    } else {
      for(ServiceReference serviceRef : serviceRefs) {
        String operationProperty = (String)serviceRef.getProperty(WORKFLOW_OPERATION_PROPERTY);
        if (operationProperty != null && operationProperty.equals(operation.getName())) {
          handlerList.add((WorkflowOperationHandler)componentContext.getBundleContext().getService(serviceRef));
        }
      }
    }
    
    // Select one of the possibly multiple operation handlers.  TODO Allow for a pluggable strategy for this mechanism
    if (handlerList.size() > 0) {
      int index = (int)Math.round((handlerList.size() - 1) * Math.random());
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
            wfi.getWorkflowOperationInstanceList().add(opInstance);
            update(wfi); // Update the workflow instance, since it has a new operation instance
            try {
              opInstance.setResult(operationHandler.run(wfi));
              opInstance.setState(State.SUCCEEDED.name());
            } catch(WorkflowOperationException e) {
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
                  log_.warn("Unable to execute catch workflow " + catchWorkflowName + " for operation " + operationDefinition);
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
                  log_.warn("Unable to execute catch workflow " + catchWorkflowName + " for operation " + operationDefinition);
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
            runFromOperation = -1;
          }
        }

        // All of the workflow operation definitions have run, so set the state and save it.
        if(failed) {
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
   * Returns the workflow identified by <code>name</code> or <code>null</code>
   * if no such workflow was found.
   * 
   * @param name the workflow definition name
   * @return the workflow
   */
  public WorkflowDefinition getWorkflowDefinitionByName(String name) {
    String filter = getWorkflowDefinitionFilter(name);
    try {
      return (WorkflowDefinition)componentContext.getBundleContext().getServiceReferences(WorkflowDefinition.class.getName(), filter)[0];
    } catch (InvalidSyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Constructs a filter for finding registered workflow definitions by name
   * @param name The name of the workflow definition
   * @return The filter string
   */
  protected String getWorkflowDefinitionFilter(String name) {
    StringBuilder sb = new StringBuilder("(");
    sb.append(WORKFLOW_DEFINITION_PROPERTY);
    sb.append("=");
    sb.append(name);
    sb.append(")");
    return sb.toString();
  }
  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#stop(java.lang.String)
   */
  public void stop(String workflowInstanceId) {
    WorkflowInstanceImpl instance = (WorkflowInstanceImpl)getWorkflowById(workflowInstanceId);
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
    WorkflowInstanceImpl instance = (WorkflowInstanceImpl)getWorkflowById(workflowInstanceId);
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
    WorkflowInstanceImpl workflowInstance = (WorkflowInstanceImpl)getWorkflowById(workflowInstanceId);
    run(workflowInstance);
    workflowInstance.setState(State.RUNNING.name());
    update(workflowInstance);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#update(org.opencastproject.workflow.api.WorkflowInstance)
   */
  public void update(WorkflowInstance workflowInstance) {
    addToDatabase(workflowInstance);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowService#getWorkflowsByDate(int, int)
   */
  public WorkflowSet getWorkflowsByDate(int offset, int limit) throws WorkflowDatabaseException {
    try {
      return solrRequester.getWorkflowsByDate(offset, limit);
    } catch (SolrServerException e) {
      throw new WorkflowDatabaseException(e);
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowService#getWorkflowsBySeries(java.lang.String)
   */
  public WorkflowSet getWorkflowsBySeries(String seriesId) throws WorkflowDatabaseException {
    try {
      return solrRequester.getWorkflowsBySeries(seriesId);
    } catch (SolrServerException e) {
      throw new WorkflowDatabaseException(e);
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowService#getWorkflowsByText(java.lang.String, int, int)
   */
  public WorkflowSet getWorkflowsByText(String text, int offset, int limit) throws WorkflowDatabaseException {
    try {
      return solrRequester.getWorkflowsByText(text, offset, limit);
    } catch (SolrServerException e) {
      throw new WorkflowDatabaseException(e);
    }
  }

  /**
   * Adds the workflow instance to the workflow database. If the workflow is already known to the database, then the
   * corresponding entry is updated with the current list of operations, the current operation and the current set of
   * properties.
   * 
   * @param workflow
   *          the workflow to add
   * @throws WorkflowDatabaseException
   *           if the workflow could not be added to the database
   */
  protected void addToDatabase(WorkflowInstance workflow) throws WorkflowDatabaseException {
    try {
      solrIndexManager.add(workflow);
    } catch (SolrServerException e) {
      throw new WorkflowDatabaseException(e);
    }
  }

  /**
   * Removes the workflow instance from the database.
   * 
   * @param workflowId
   *          workflow identifier
   * @throws WorkflowDatabaseException
   *           if the workflow could not be removed
   */
  protected void removeFromDatabase(String workflowId) throws WorkflowDatabaseException {
    try {
      solrIndexManager.delete(workflowId);
    } catch (SolrServerException e) {
      throw new WorkflowDatabaseException(e);
    }
  }

  /**
   * Prepares the solr environment.
   * 
   * @param solrRoot
   *          the solr root directory
   */
  private void setupSolr(String solrRoot) {
    try {
      log_.info("Setting up solr search index at " + solrRoot);
      File solrConfigDir = new File(solrRoot, "conf");

      // Create the config directory
      if (solrConfigDir.exists()) {
        log_.info("solr search index found at " + solrConfigDir);
      } else {
        log_.info("solr config directory doesn't exist.  Creating " + solrConfigDir);
        FileUtils.forceMkdir(solrConfigDir);
      }

      // Make sure there is a configuration in place
      copyClasspathResourceToFile("/solr/conf/protwords.txt", solrConfigDir);
      copyClasspathResourceToFile("/solr/conf/schema.xml", solrConfigDir);
      copyClasspathResourceToFile("/solr/conf/scripts.conf", solrConfigDir);
      copyClasspathResourceToFile("/solr/conf/solrconfig.xml", solrConfigDir);
      copyClasspathResourceToFile("/solr/conf/stopwords.txt", solrConfigDir);
      copyClasspathResourceToFile("/solr/conf/synonyms.txt", solrConfigDir);

      // Test for the existence of a data directory
      File solrData = new File(solrRoot, "data");
      if (!solrData.exists()) {
        FileUtils.forceMkdir(solrData);
      }

      SolrCore.log.getParent().setLevel(Level.WARNING);
      solrConnection = new SolrConnection(solrRoot, PathSupport.concat(solrRoot, "data"));
      solrRequester = new SolrRequester(solrConnection);
      solrIndexManager = new SolrIndexManager(solrConnection);
    } catch (IOException e) {
      throw new RuntimeException("Error setting up solr index at " + solrRoot, e);
    }
  }

  private void copyClasspathResourceToFile(String classpath, File dir) {
    InputStream in = WorkflowServiceImpl.class.getResourceAsStream(classpath);
    try {
      File file = new File(dir, FilenameUtils.getName(classpath));
      log_.info("copying inputstream " + in + " to file to " + file);
      IOUtils.copy(in, new FileOutputStream(file));
    } catch (IOException e) {
      throw new RuntimeException("Error copying solr classpath resource to the filesystem", e);
    }
  }
}
