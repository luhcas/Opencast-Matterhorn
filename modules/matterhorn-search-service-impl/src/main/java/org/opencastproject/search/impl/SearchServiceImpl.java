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

package org.opencastproject.search.impl;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogService;
import org.opencastproject.search.api.SearchException;
import org.opencastproject.search.api.SearchQuery;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.search.impl.solr.EmbeddedSolrServerWrapper;
import org.opencastproject.search.impl.solr.SolrIndexManager;
import org.opencastproject.search.impl.solr.SolrRequester;
import org.opencastproject.search.impl.solr.SolrServerFactory;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.PathSupport;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * A Solr-based {@link SearchService} implementation.
 */
public class SearchServiceImpl implements SearchService {

  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);

  /** Configuration key for a remote solr server */
  public static final String CONFIG_SOLR_URL = "org.opencastproject.search.solr.url";

  /** Configuration key for an embedded solr configuration and data directory */
  public static final String CONFIG_SOLR_ROOT = "org.opencastproject.search.solr.dir";

  /** Connection to the solr database */
  private SolrServer solrServer = null;

  /** Solr query execution */
  private SolrRequester solrRequester = null;

  /** Manager for the solr search index */
  private SolrIndexManager solrIndexManager = null;

  private DublinCoreCatalogService dcService;

  private Mpeg7CatalogService mpeg7Service;

  /** The local workspace */
  private Workspace workspace;

  /** The registry of remote services */
  protected ServiceRegistry remoteServiceManager;

  public void setRemoteServiceManager(ServiceRegistry remoteServiceManager) {
    this.remoteServiceManager = remoteServiceManager;
  }

  public void setDublincoreService(DublinCoreCatalogService dcService) {
    this.dcService = dcService;
    if (solrIndexManager != null)
      solrIndexManager.setDcService(dcService); // In case the dc service is updated
  }

  public void setMpeg7Service(Mpeg7CatalogService mpeg7Service) {
    this.mpeg7Service = mpeg7Service;
    if (solrIndexManager != null)
      solrIndexManager.setMpeg7Service(mpeg7Service); // In case the dc service is updated
  }

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
    if (solrIndexManager != null)
      solrIndexManager.setWorkspace(workspace); // In case the workspace is updated
  }

  /**
   * Service activator, called via declarative services configuration. If the solr server url is configured, we try to
   * connect to it. If not, the solr data directory with an embedded Solr server is used.
   * 
   * @param cc
   *          the component context
   */
  public void activate(ComponentContext cc) throws IllegalStateException {
    String solrRoot = null;
    String solrServerUrlConfig = StringUtils.trimToNull(cc.getBundleContext().getProperty(CONFIG_SOLR_URL));
    URL solrServerUrl = null;

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
      solrRoot = PathSupport.concat(storageDir, "searchindex");
    }

    try {
      if (solrServerUrlConfig != null)
        setupSolr(solrServerUrl);
      else
        setupSolr(new File(solrRoot));
    } catch (IOException e) {
      throw new IllegalStateException("Unable to connect to solr at " + solrRoot, e);
    } catch (SolrServerException e) {
      throw new IllegalStateException("Unable to connect to solr at " + solrRoot, e);
    }
  }

  public void deactivate() {
    if (solrServer instanceof EmbeddedSolrServerWrapper) {
      ((EmbeddedSolrServerWrapper) solrServer).shutdown();
    }
  }

  /**
   * Prepares the solr environment.
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
    solrRequester = new SolrRequester(solrServer);
    solrIndexManager = new SolrIndexManager(solrServer, workspace);
    solrIndexManager.setDcService(dcService);
    solrIndexManager.setMpeg7Service(mpeg7Service);
  }

  /**
   * Connects to a remote solr server.
   * 
   * @param url
   *          the url of the remote solr server
   */
  protected void setupSolr(URL url) throws IOException, SolrServerException {
    logger.info("Connecting to solr search index at {}", url);
    solrServer = SolrServerFactory.newRemoteInstance(url);
    solrRequester = new SolrRequester(solrServer);
    solrIndexManager = new SolrIndexManager(solrServer, workspace);
    solrIndexManager.setDcService(dcService);
    solrIndexManager.setMpeg7Service(mpeg7Service);
  }

  private void copyClasspathResourceToFile(String classpath, File dir) {
    InputStream in = null;
    FileOutputStream fos = null;
    try {
      in = SearchServiceImpl.class.getResourceAsStream(classpath);
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

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.api.SearchService#getByQuery(java.lang.String, int, int)
   */
  public SearchResult getByQuery(String query, int limit, int offset) throws SearchException {
    try {
      logger.debug("Searching index using custom query '" + query + "'");
      return solrRequester.getByQuery(query, limit, offset);
    } catch (SolrServerException e) {
      throw new SearchException(e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.api.SearchService#add(org.opencastproject.mediapackage.MediaPackage)
   */
  public void add(MediaPackage mediaPackage) throws SearchException, IllegalArgumentException {
    if (mediaPackage == null) {
      throw new IllegalArgumentException("Unable to add a null mediapackage");
    }
    try {
      logger.debug("Attempting to add mediapackage {} to search index", mediaPackage.getIdentifier());
      if (solrIndexManager.add(mediaPackage)) {
        logger.info("Added mediapackage {} to the search index", mediaPackage.getIdentifier());
      } else {
        logger.warn("Failed to add mediapackage {} to the search index", mediaPackage.getIdentifier());
      }
    } catch (SolrServerException e) {
      throw new SearchException(e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.api.SearchService#delete(java.lang.String)
   */
  public void delete(String mediaPackageId) throws SearchException {
    try {
      logger.info("Removing mediapackage {} from search index", mediaPackageId);
      solrIndexManager.delete(mediaPackageId);
    } catch (SolrServerException e) {
      throw new SearchException(e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.api.SearchService#clear()
   */
  @Override
  public void clear() throws SearchException {
    try {
      logger.info("Clearing the search index");
      solrIndexManager.clear();
    } catch (SolrServerException e) {
      throw new SearchException(e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.api.SearchService#getByQuery(org.opencastproject.search.api.SearchQuery)
   */
  public SearchResult getByQuery(SearchQuery q) throws SearchException {
    try {
      logger.debug("Searching index using query object '" + q + "'");
      return solrRequester.getByQuery(q);
    } catch (SolrServerException e) {
      throw new SearchException(e);
    }
  }

}
