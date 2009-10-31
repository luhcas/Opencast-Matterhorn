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

import org.opencastproject.util.PathSupport;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowInstance;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

/**
 * Solr based implementation of the workflow service's DAO.
 */
public class WorkflowServiceImplDaoSolrImpl implements WorkflowServiceImplDao {
  private static final Logger log_ = LoggerFactory.getLogger(WorkflowServiceImplDaoSolrImpl.class);
  
  /** Connection to the solr database */
  private SolrConnection solrConnection = null;

  /** Solr query execution */
  private SolrRequester solrRequester = null;

  /** Manager for the solr search index */
  private SolrIndexManager solrIndexManager = null;
  
  /** The solr root directory */
  private String solrRoot = null;

  /**
   * Constructs the DAO with the default settings.
   */
  public WorkflowServiceImplDaoSolrImpl() {
    this(System.getProperty("java.io.tmpdir") + File.separator + "opencast" + File.separator + "workflows");
  }

  public WorkflowServiceImplDaoSolrImpl(String solrRoot) {
    this.solrRoot = solrRoot;
  }


  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#update(org.opencastproject.workflow.api.WorkflowInstance)
   */
  public void update(WorkflowInstance workflow) {
    log_.info("adding " + workflow + " to solr");
    try {
      solrIndexManager.add(workflow);
//      try {Thread.sleep(3000);} catch (InterruptedException e) {}
    } catch (SolrServerException e) {
      throw new WorkflowDatabaseException(e);
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#remove(java.lang.String)
   */
  public void remove(String id) {
    try {
      solrIndexManager.delete(id);
    } catch (SolrServerException e) {
      throw new WorkflowDatabaseException(e);
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#getWorkflowById(java.lang.String)
   */
  public WorkflowInstance getWorkflowById(String id) {
    try {
      WorkflowSet set = solrRequester.getWorkflowById(id);
      if(set.getItems().length == 0) return null;
      if(set.getItems().length > 1) throw new IllegalStateException("Somehow, there are " + set.getItems().length +
              " workflow instances with an id=" + id);
      return set.getItems()[0];
    } catch (SolrServerException e) {
      throw new WorkflowDatabaseException(e);
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#getWorkflowsByDate(int, int)
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
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#getWorkflowsByEpisode(java.lang.String)
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
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#getWorkflowsByMediaPackage(java.lang.String)
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
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#getWorkflowsBySeries(java.lang.String)
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
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#getWorkflowsByText(java.lang.String, int, int)
   */
  public WorkflowSet getWorkflowsByText(String text, int offset, int limit) throws WorkflowDatabaseException {
    try {
      return solrRequester.getWorkflowsByText(text, offset, limit);
    } catch (SolrServerException e) {
      throw new WorkflowDatabaseException(e);
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#getWorkflowsInState(org.opencastproject.workflow.api.WorkflowInstance.State, int, int)
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
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#activate()
   */
  public void activate(File storageDir) {
    try {
      log_.info("Setting up solr search index at " + solrRoot);
      File solrConfigDir = new File(storageDir, "conf");

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

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#deactivate()
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
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#countWorkflowInstances()
   */
  public long countWorkflowInstances() {
    // TODO Auto-generated method stub
    return 0;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#getWorkflowsByTextAndState(org.opencastproject.workflow.api.WorkflowInstance.State, java.lang.String, int, int)
   */
  public WorkflowSet getWorkflowsByTextAndState(State state, String text, int offset, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

}
