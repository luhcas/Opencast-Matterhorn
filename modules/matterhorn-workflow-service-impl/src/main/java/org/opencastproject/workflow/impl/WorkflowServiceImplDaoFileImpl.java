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

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.solr.SolrServerFactory;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PathSupport;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowSet;
import org.opencastproject.workflow.api.WorkflowSetImpl;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * Provides data access to the workflow service through file storage in the workspace, indexed via solr.
 */
public class WorkflowServiceImplDaoFileImpl implements WorkflowServiceImplDao {
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(WorkflowServiceImplDaoFileImpl.class);

  /** Configuration key for a remote solr server */
  public static final String CONFIG_SOLR_URL = "org.opencastproject.workflow.solr.url";

  /** Configuration key for an embedded solr configuration and data directory */
  public static final String CONFIG_SOLR_ROOT = "org.opencastproject.workflow.solr.dir";

  /** Connection to the solr server. Solr is used to search for workflows. The workflow data are stored as xml files. */
  private SolrServer solrServer = null;

  /** The root directory to use for solr config and data files */
  protected String solrRoot = null;

  /** The URL to connect to a remote solr server */
  protected URL solrServerUrl = null;

  /** The collection in the working file repository to store workflow xml files */
  protected static final String COLLECTION_ID = "workflows";

  /** The key in solr documents representing the workflow's current operation */
  protected static final String OPERATION_KEY = "operation";

  /** The key in solr documents representing the workflow's series */
  protected static final String SERIES_KEY = "series";

  /** The key in solr documents representing the workflow's ID */
  protected static final String ID_KEY = "id";

  /** The key in solr documents representing the workflow's current state */
  private static final String STATE_KEY = "state";

  /** The key in solr documents representing the workflow as xml */
  private static final String XML_KEY = "xml";

  /** The key in solr documents representing the workflow's contributors */
  private static final String CONTRIBUTOR_KEY = "contributor";

  /** The key in solr documents representing the workflow's mediapackage language */
  private static final String LANGUAGE_KEY = "language";

  /** The key in solr documents representing the workflow's mediapackage license */
  private static final String LICENSE_KEY = "license";

  /** The key in solr documents representing the workflow's mediapackage title */
  private static final String TITLE_KEY = "title";

  /** The key in solr documents representing the workflow's mediapackage identifier */
  private static final String MEDIAPACKAGE_KEY = "mp";

  /** The key in solr documents representing the workflow's mediapackage creators */
  private static final String CREATOR_KEY = "creator";

  /** The key in solr documents representing the workflow's mediapackage subjects */
  private static final String SUBJECT_KEY = "subject";

  /** The key in solr documents representing the full text index */
  private static final String FULLTEXT_KEY = "fulltext";

  /** The workspace used to store the workflow xml files. These files are the "source of authority" for workflow data */
  protected Workspace workspace;

  /** Sets the workspace */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  public void activate(ComponentContext cc) {
    String solrServerUrlConfig = StringUtils.trimToNull(cc.getBundleContext().getProperty(CONFIG_SOLR_URL));
    if (solrServerUrlConfig != null) {
      try {
        solrServerUrl = new URL(solrServerUrlConfig);
      } catch (MalformedURLException e) {
        throw new IllegalStateException("Unable to connect to solr at " + solrServerUrlConfig, e);
      }
    } else if (cc.getBundleContext().getProperty(CONFIG_SOLR_ROOT) != null) {
      solrRoot = cc.getBundleContext().getProperty(CONFIG_SOLR_ROOT);
    } else {
      String storageDir = cc.getBundleContext().getProperty("org.opencastproject.storage.dir");
      if (storageDir == null)
        throw new IllegalStateException("Storage dir must be set (org.opencastproject.storage.dir)");
      solrRoot = PathSupport.concat(storageDir, "workflow");
    }
    activate();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#activate()
   */
  @Override
  public void activate() {
    // Set up the solr server
    if (solrServerUrl != null) {
      solrServer = SolrServerFactory.newRemoteInstance(solrServerUrl);
    } else {
      try {
        setupSolr(new File(solrRoot));
      } catch (IOException e) {
        throw new IllegalStateException("Unable to connect to solr at " + solrRoot, e);
      } catch (SolrServerException e) {
        throw new IllegalStateException("Unable to connect to solr at " + solrRoot, e);
      }
    }

    // If the solr is empty, add all of the workflows found in the COLLECTION_ID collection
    long instances = 0;
    try {
      instances = countWorkflowInstances();
    } catch (WorkflowDatabaseException e) {
      throw new IllegalStateException(e);
    }
    if (instances == 0) {
      // this may be a new index, so get all of the existing workflows and index them
      WorkflowBuilder builder = WorkflowBuilder.getInstance();
      URI[] uris = null;
      try {
        uris = workspace.getCollectionContents(COLLECTION_ID);
      } catch (IOException e) {
        throw new IllegalStateException("Error accessing workspace collection '" + COLLECTION_ID + "'", e);
      }
      if (uris.length > 0) {
        logger.info("The workflow search index is empty.  Populating it now with {} workflows.", uris.length);
      }
      for (URI uri : uris) {
        InputStream in = null;
        try {
          File file = workspace.get(uri);
          in = new FileInputStream(file);
          WorkflowInstance instance = builder.parseWorkflowInstance(in);
          index(instance);
        } catch (Exception e) {
          logger.warn("unable to parse workflow instance", e);
          throw new IllegalStateException(e);
        } finally {
          IOUtils.closeQuietly(in);
        }
      }
      if (uris.length > 0) {
        logger.info("Finished populating the workflow search index with {} workflows.", uris.length);
      }
    }
  }

  /**
   * Prepares the embedded solr environment.
   * 
   * @param solrRoot
   *          the solr root directory
   */
  protected void setupSolr(File solrRoot) throws IOException, SolrServerException {
    logger.info("Setting up solr search index at {}", solrRoot);
    File solrConfigDir = new File(solrRoot, "conf");

    // Create the config directory
    if (solrConfigDir.exists()) {
      logger.info("solr search index found at {}", solrConfigDir);
    } else {
      logger.info("solr config directory doesn't exist.  Creating {}", solrConfigDir);
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
    File solrDataDir = new File(solrRoot, "data");
    if (!solrDataDir.exists()) {
      FileUtils.forceMkdir(solrDataDir);
    }

    // Test for the existence of the index. Note that an empty index directory will prevent solr from
    // completing normal setup.
    File solrIndexDir = new File(solrDataDir, "index");
    if (solrIndexDir.exists() && solrIndexDir.list().length == 0) {
      FileUtils.deleteDirectory(solrIndexDir);
    }

    solrServer = SolrServerFactory.newEmbeddedInstance(solrRoot, solrDataDir);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#deactivate()
   */
  @Override
  public void deactivate() {
    SolrServerFactory.shutdown(solrServer);
  }

  // TODO: generalize this method
  private void copyClasspathResourceToFile(String classpath, File dir) {
    InputStream in = null;
    FileOutputStream fos = null;
    try {
      in = WorkflowServiceImplDaoFileImpl.class.getResourceAsStream(classpath);
      File file = new File(dir, FilenameUtils.getName(classpath));
      logger.debug("copying " + classpath + " to " + file);
      fos = new FileOutputStream(file);
      IOUtils.copy(in, fos);
    } catch (IOException e) {
      throw new RuntimeException("Error copying solr classpath resource to the filesystem", e);
    } finally {
      IOUtils.closeQuietly(in);
      IOUtils.closeQuietly(fos);
    }
  }

  public synchronized void index(WorkflowInstance instance) throws WorkflowDatabaseException {
    try {
      SolrInputDocument doc = createDocument(instance);
      solrServer.add(doc);
      solrServer.commit();
    } catch (Exception e) {
      throw new WorkflowDatabaseException("unable to index workflow", e);
    }
  }

  protected SolrInputDocument createDocument(WorkflowInstance instance) throws Exception {
    SolrInputDocument doc = new SolrInputDocument();
    doc.addField(ID_KEY, instance.getId());
    doc.addField(STATE_KEY, instance.getState().toString());

    String xml = WorkflowBuilder.getInstance().toXml(instance);
    doc.addField(XML_KEY, xml);
    
    WorkflowOperationInstance op = instance.getCurrentOperation();
    if (op != null)
      doc.addField(OPERATION_KEY, op.getId());
    MediaPackage mp = instance.getMediaPackage();
    doc.addField(MEDIAPACKAGE_KEY, mp.getIdentifier().toString());
    if (mp.getSeries() != null) {
      doc.addField(SERIES_KEY, mp.getSeries());
    }
    if (mp.getTitle() != null) {
      doc.addField(TITLE_KEY, mp.getTitle());
    }
    if (mp.getLicense() != null) {
      doc.addField(LICENSE_KEY, mp.getLicense());
    }
    if (mp.getLanguage() != null) {
      doc.addField(LANGUAGE_KEY, mp.getLanguage());
    }
    if (mp.getContributors() != null && mp.getContributors().length > 0) {
      for (String contributor : mp.getContributors()) {
        doc.addField(CONTRIBUTOR_KEY, contributor);
      }
    }
    if (mp.getCreators() != null && mp.getCreators().length > 0) {
      for (String creator : mp.getCreators()) {
        doc.addField(CREATOR_KEY, creator);
      }
    }
    if (mp.getSubjects() != null && mp.getSubjects().length > 0) {
      for (String subject : mp.getSubjects()) {
        doc.addField(SUBJECT_KEY, subject);
      }
    }
    return doc;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#countWorkflowInstances()
   */
  @Override
  public long countWorkflowInstances() throws WorkflowDatabaseException {
    SolrQuery query = new SolrQuery("*:*");
    try {
      QueryResponse response = solrServer.query(query);
      return response.getResults().getNumFound();
    } catch (SolrServerException e) {
      throw new WorkflowDatabaseException(e);
    }

  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#getWorkflowById(java.lang.String)
   */
  @Override
  public WorkflowInstance getWorkflowById(String workflowId) throws WorkflowDatabaseException, NotFoundException {
    try {
      QueryResponse response = solrServer.query(new SolrQuery(ID_KEY + ":" + workflowId));
      if(response.getResults().size() == 0) {
        throw new NotFoundException("Unable to find a workflow with id=" + workflowId);
      } else {
        String xml = (String)response.getResults().get(0).get(XML_KEY);
        try {
          return WorkflowBuilder.getInstance().parseWorkflowInstance(xml);
        } catch (Exception e) {
          throw new IllegalStateException("can not parse workflow xml", e);
        }
      }
    } catch (SolrServerException e) {
      throw new WorkflowDatabaseException(e);
    }
  }

  /**
   * Gets the URI for a workflow instance stored in the workspace.
   * 
   * @param workflowId
   *          The workflow identifier
   * @return the URI to the workflow. Note that there may not be a file at this URI. If so, calling
   *         {@link Workspace#get(URI)} will throw a {@link NotFoundException}
   */
  private URI getWorkflowFileUri(String workflowId) {
    return workspace.getCollectionURI(COLLECTION_ID, getFilename(workflowId));
  }

  private StringBuilder append(StringBuilder sb, String key, String value) {
    if(sb.length() > 0) {
      sb.append(" AND ");
    }
    sb.append(key);
    sb.append(":");
    sb.append(ClientUtils.escapeQueryChars(value));
    return sb;
  }
  
  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#getWorkflowInstances(org.opencastproject.workflow.api.WorkflowQuery)
   */
  @Override
  public WorkflowSet getWorkflowInstances(WorkflowQuery query) throws WorkflowDatabaseException {
    int count = query.getCount() > 0 ? (int) query.getCount() : 20; // default to 20 items if not specified
    int startPage = query.getStartPage() > 0 ? (int) query.getStartPage() : 0; // default to page zero

    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setRows(count);
    solrQuery.setStart(startPage * count);

    StringBuilder sb = new StringBuilder();
    if (StringUtils.isNotBlank(query.getMediaPackage())) {
      append(sb, MEDIAPACKAGE_KEY, query.getMediaPackage());
    }
    if (StringUtils.isNotBlank(query.getSeries())) {
      append(sb, SERIES_KEY, query.getSeries());
    }
    if (StringUtils.isNotBlank(query.getCurrentOperation())) {
      append(sb, OPERATION_KEY, query.getCurrentOperation());
    }
    if (query.getState() != null) {
      append(sb, STATE_KEY, query.getState().toString());
    }
    if (StringUtils.isNotBlank(query.getText())) {
      append(sb, FULLTEXT_KEY, query.getText());
    }

    // If we're looking for anything, set the query to a wildcard search
    if(sb.length() == 0) {
      sb.append("*:*");
    }
    
    solrQuery.setQuery(sb.toString());
    long totalHits;
    long time = System.currentTimeMillis();
    WorkflowSetImpl set = null;
    try {
      QueryResponse response = solrServer.query(solrQuery);
      SolrDocumentList items = response.getResults();
      time = System.currentTimeMillis() - time;
      totalHits = items.getNumFound();

      // Iterate through the results
      set = new WorkflowSetImpl();
      set.setPageSize(count);
      set.setTotalCount(totalHits);
      set.setStartPage(query.getStartPage());
      set.setSearchTime(time);

      for (SolrDocument doc : items) {
        String xml = (String)doc.get(XML_KEY);
        try {
          set.addItem(WorkflowBuilder.getInstance().parseWorkflowInstance(xml));
        } catch (Exception e) {
          throw new IllegalStateException("can not parse workflow xml", e);
        }
      }
    } catch (Exception e) {
      throw new WorkflowDatabaseException(e);
    }
    return set;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#remove(java.lang.String)
   */
  @Override
  public void remove(String id) throws WorkflowDatabaseException, NotFoundException {
    try {
      solrServer.deleteById(id);
      solrServer.commit();
      URI uri = getWorkflowFileUri(id);
      workspace.delete(uri);
    } catch (SolrServerException e) {
      throw new WorkflowDatabaseException(e);
    } catch (IOException e) {
      throw new WorkflowDatabaseException(e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.impl.WorkflowServiceImplDao#update(org.opencastproject.workflow.api.WorkflowInstance)
   */
  @Override
  public void update(WorkflowInstance instance) throws WorkflowDatabaseException {
    String xml;
    try {
      xml = WorkflowBuilder.getInstance().toXml(instance);
    } catch (Exception e) {
      throw new WorkflowDatabaseException(e);
    }
    try {
      workspace.putInCollection(COLLECTION_ID, getFilename(instance.getId()), IOUtils.toInputStream(xml, "UTF-8"));
      index(instance);
    } catch (IOException e) {
      throw new WorkflowDatabaseException(e);
    }
  }

  /**
   * Generates a filename based on the workflow ID
   * 
   * @param workflowId
   * @return
   */
  private String getFilename(String workflowId) {
    return workflowId + ".xml";
  }
}
