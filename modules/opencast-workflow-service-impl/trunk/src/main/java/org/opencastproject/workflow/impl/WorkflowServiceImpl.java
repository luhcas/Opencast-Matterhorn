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
import org.opencastproject.workflow.api.WorkflowSet;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowDefinitionJaxbImpl;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstanceJaxbImpl;
import org.opencastproject.workflow.api.WorkflowOperation;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationImpl;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowInstance.State;
import org.opencastproject.workflow.impl.solr.SolrConnection;
import org.opencastproject.workflow.impl.solr.SolrIndexManager;
import org.opencastproject.workflow.impl.solr.SolrRequester;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
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
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

/**
 * Implements {@link WorkflowService} with in-memory data structures to hold {@link WorkflowOperation}s and
 * {@link WorkflowInstance}s. {@link WorkflowOperationHandler}s are looked up in the OSGi service registry based on the
 * "opencast.workflow.operation" property. If the {@link WorkflowOperationHandler}'s "opencast.workflow.operation"
 * service registration property matches {@link WorkflowOperation#getName()}, then the factory returns a
 * {@link Runnable} to handle that operation. This allows for custom runnables to be added or modified without affecting
 * the workflow service itself.
 */
public class WorkflowServiceImpl implements WorkflowService, ManagedService {

  /** Logging facility */
  private static final Logger log_ = LoggerFactory.getLogger(WorkflowServiceImpl.class);

  /** The workflow instances */
  Map<String, WorkflowInstance> instances = null;

  /** Known workflow operations */
  List<WorkflowOperation> operations = null;

  /** Connection to the solr database */
  private SolrConnection solrConnection = null;

  /** Solr query execution */
  private SolrRequester solrRequester = null;

  /** Manager for the solr search index */
  private SolrIndexManager solrIndexManager = null;

  /** The solr root directory */
  private String solrRoot = null;

  /**
   * Creates a solr service that puts its data into the given root directory. If the directory doesn't exist, it will be
   * created.
   * 
   * @param solrRoot
   *          the solr root directory
   */
  public WorkflowServiceImpl(String solrRoot) {
    this.solrRoot = solrRoot;
    instances = new HashMap<String, WorkflowInstance>();
    operations = new ArrayList<WorkflowOperation>();
    loadOperations();
  }

  /**
   * Creates a new workflow service instance.
   */
  public WorkflowServiceImpl() {
    this(System.getProperty("java.io.tmpdir") + File.separator + "opencast" + File.separator + "workflows");
  }

  @SuppressWarnings("unchecked")
  public void updated(Dictionary props) throws ConfigurationException {
    // Update any configuration properties here
  }

  protected void loadOperations() {
    WorkflowOperationImpl compose = new WorkflowOperationImpl("compose", "Composes new media tracks", true);
    WorkflowOperationImpl distribute = new WorkflowOperationImpl("distribute",
            "Distributes media tracks to distribution channels", true);
    operations.add(compose);
    operations.add(distribute);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#getWorkflowInstance(java.lang.String)
   */
  public WorkflowInstance getWorkflowInstance(String id) {
    WorkflowInstance instance = instances.get(id);
    if (instance == null) {
      log_.warn("no workflow instance found with id=" + id);
    }
    return instance;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#start(org.opencastproject.workflow.api.WorkflowDefinition,
   *      org.opencastproject.media.mediapackage.MediaPackage)
   */
  public WorkflowInstance start(WorkflowDefinition workflowDefinition, MediaPackage mediaPackage,
          Map<String, String> properties) {
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
    setupSolr(solrRoot);
  }

  public void deactivate() {
    try {
      solrConnection.destroy();
    } catch (Throwable t) {
      log_.error("Error closing the solr connection");
    }
  }

  protected void run(final WorkflowInstance wfi) {
    Thread t = new Thread(new Runnable() {
      public void run() {
        // Get all of the runnable workflow services available for each operation (in the order of operations)
        for (WorkflowOperation operation : wfi.getWorkflowDefinition().getOperations()) {
          ServiceReference[] serviceRefs = null;
          try {
            serviceRefs = componentContext.getBundleContext().getAllServiceReferences(
                    WorkflowOperationHandler.class.getName(),
                    "(opencast.workflow.operation=" + operation.getName() + ")");
          } catch (InvalidSyntaxException e) {
            throw new RuntimeException(e);
          }
          if (serviceRefs == null) {
            log_.info("No WorkflowRunners registered for operation " + operation);
          } else {
            for (ServiceReference serviceRef : serviceRefs) {
              WorkflowOperationHandler runner = (WorkflowOperationHandler) componentContext.getBundleContext()
                      .getService(serviceRef);
              // TODO: Set current operation on wfi
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
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#stop(java.lang.String)
   */
  public void stop(String workflowInstanceId) {
    WorkflowInstanceJaxbImpl instance = (WorkflowInstanceJaxbImpl) getWorkflowInstance(workflowInstanceId);
    Thread t = threadMap.get(workflowInstanceId);
    if (t != null) {
      t.interrupt();
    }
    instance.setState(State.STOPPED.name());
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#suspend(java.lang.String)
   */
  public void suspend(String workflowInstanceId) {
    WorkflowInstanceJaxbImpl instance = (WorkflowInstanceJaxbImpl) getWorkflowInstance(workflowInstanceId);
    Thread t = threadMap.get(workflowInstanceId);
    if (t != null) {
      t.interrupt();
    }
    instance.setState(State.PAUSED.name());
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#resume(java.lang.String)
   */
  public void resume(String workflowInstanceId) {
    WorkflowInstanceJaxbImpl workflowInstance = (WorkflowInstanceJaxbImpl) getWorkflowInstance(workflowInstanceId);
    run(workflowInstance);
    workflowInstance.setState(State.RUNNING.name());
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#update(org.opencastproject.workflow.api.WorkflowInstance)
   */
  public void update(WorkflowInstance workflowInstance) {
    // TODO: Update the workflow instance
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#getWorkflowInstances(org.opencastproject.workflow.api.WorkflowInstance.State)
   */
  public List<WorkflowInstance> getWorkflowInstances(State state) {
    List<WorkflowInstance> instancesInState = new ArrayList<WorkflowInstance>();
    for (Entry<String, WorkflowInstance> entry : instances.entrySet()) {
      if (entry.getValue().getState().equals(state)) {
        instancesInState.add(entry.getValue());
      }
    }
    return instancesInState;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#getWorkflowOperations()
   */
  public List<WorkflowOperation> getWorkflowOperations() {
    return Collections.unmodifiableList(operations);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowService#getWorkflowsById(java.lang.String)
   */
  public WorkflowSet getWorkflowsById(String episodeId) throws WorkflowDatabaseException {
    try {
      return solrRequester.getWorkflowsById(episodeId);
    } catch (SolrServerException e) {
      throw new WorkflowDatabaseException(e);
    }
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
   * @see org.opencastproject.workflow.api.WorkflowService#getWorkflowsByEpisode(java.lang.String)
   */
  public WorkflowSet getWorkflowsByEpisode(String episodeId) throws WorkflowDatabaseException {
    try {
      return solrRequester.getWorkflowsByEpisode(episodeId);
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