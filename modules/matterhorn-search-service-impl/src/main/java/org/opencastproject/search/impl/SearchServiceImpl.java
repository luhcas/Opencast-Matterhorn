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
import org.opencastproject.remote.api.RemoteServiceManager;
import org.opencastproject.search.api.SearchException;
import org.opencastproject.search.api.SearchQuery;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.search.impl.solr.SolrConnection;
import org.opencastproject.search.impl.solr.SolrIndexManager;
import org.opencastproject.search.impl.solr.SolrRequester;
import org.opencastproject.util.PathSupport;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.core.SolrCore;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

/**
 * A Solr-based {@link SearchService} implementation.
 */
public class SearchServiceImpl implements SearchService {

  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);

  public static final String CONFIG_SOLR_ROOT = "org.opencastproject.search.searchindexdir";

  /** Connection to the solr database */
  private SolrConnection solrConnection = null;

  /** Solr query execution */
  private SolrRequester solrRequester = null;

  /** Manager for the solr search index */
  private SolrIndexManager solrIndexManager = null;

  /** The solr root directory */
  String solrRoot = null;

  private DublinCoreCatalogService dcService;

  private Mpeg7CatalogService mpeg7Service;

  /** The local workspace */
  private Workspace workspace;

  /** The registry of remote services */
  protected RemoteServiceManager remoteServiceManager;

  public void setRemoteServiceManager(RemoteServiceManager remoteServiceManager) {
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
   * Service activator, called via declarative services configuration.
   * 
   * @param cc
   *          the component context
   */
  public void activate(ComponentContext cc) {
    if (cc.getBundleContext().getProperty(CONFIG_SOLR_ROOT) != null) {
      this.solrRoot = cc.getBundleContext().getProperty(CONFIG_SOLR_ROOT);
    } else {
      String storageDir = cc.getBundleContext().getProperty("org.opencastproject.storage.dir");
      if (storageDir == null)
        throw new IllegalStateException("Storage dir must be set (org.opencastproject.storage.dir)");
      this.solrRoot = PathSupport.concat(storageDir, "searchindex");
    }
    logger.info("DEFAULT " + CONFIG_SOLR_ROOT + ": " + this.solrRoot);
    setupSolr(this.solrRoot);
  }

  public void deactivate() {
    try {
      solrConnection.destroy();
      // Needs some time to close properly
      // FIXME Not ideal solution
      Thread.sleep(3000);
    } catch (Throwable t) {
      logger.error("Error closing the solr connection");
    }
  }

  /**
   * Prepares the solr environment.
   * 
   * @param solrRoot
   *          the solr root directory
   */
  protected void setupSolr(String solrRoot) {
    try {
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

      SolrCore.log.getParent().setLevel(Level.WARNING);
      solrConnection = new SolrConnection(solrRoot, PathSupport.concat(solrRoot,"data"));
      solrRequester = new SolrRequester(solrConnection);
      solrIndexManager = new SolrIndexManager(solrConnection, workspace);
      solrIndexManager.setDcService(dcService);
      solrIndexManager.setMpeg7Service(mpeg7Service);
      // On windows, the solr index needs some time to setup
      if (System.getProperty("os.name").toLowerCase().indexOf("windows") > -1)
        Thread.sleep(3000);
    } catch (IOException e) {
      throw new RuntimeException("Error setting up solr index at " + solrRoot, e);
    } catch (InterruptedException e) {
      logger.error("Interupted while setting up solr index");
    }
  }

  private void copyClasspathResourceToFile(String classpath, File dir) {
    InputStream in = SearchServiceImpl.class.getResourceAsStream(classpath);
    try {
      File file = new File(dir, FilenameUtils.getName(classpath));
      logger.debug("copying inputstream " + in + " to file to " + file);
      IOUtils.copy(in, new FileOutputStream(file));
    } catch (IOException e) {
      throw new RuntimeException("Error copying solr classpath resource to the filesystem", e);
    }
  }

//  /**
//   * {@inheritDoc}
//   * 
//   * @see org.opencastproject.search.api.SearchService#getEpisodeAndSeriesById(java.lang.String)
//   */
//  public SearchResult getEpisodeAndSeriesById(String seriesId) throws SearchException {
//    try {
//      logger.debug("Searching index for episodes and series details of series " + seriesId);
//      return solrRequester.getEpisodeAndSeriesById(seriesId);
//    } catch (SolrServerException e) {
//      throw new SearchException(e);
//    }
//  }

//  /**
//   * {@inheritDoc}
//   * 
//   * @see org.opencastproject.search.api.SearchService#getEpisodeById(java.lang.String)
//   */
//  public SearchResult getEpisodeById(String episodeId) throws SearchException {
//    try {
//      logger.debug("Searching index for episode " + episodeId);
//      return solrRequester.getEpisodeById(episodeId);
//    } catch (SolrServerException e) {
//      throw new SearchException(e);
//    }
//  }
//
//  /**
//   * {@inheritDoc}
//   * 
//   * @see org.opencastproject.search.api.SearchService#getEpisodesAndSeriesByText(java.lang.String, int, int)
//   */
//  public SearchResult getEpisodesAndSeriesByText(String text, int limit, int offset) throws SearchException {
//    try {
//      logger.debug("Searching index for episodes and series matching '" + text + "'");
//      return solrRequester.getEpisodesAndSeriesByText(text, limit, offset);
//    } catch (SolrServerException e) {
//      throw new SearchException(e);
//    }
//  }
//
//  /**
//   * {@inheritDoc}
//   * 
//   * @see org.opencastproject.search.api.SearchService#getEpisodesByDate(int, int)
//   */
//  public SearchResult getEpisodesByDate(int limit, int offset) throws SearchException {
//    try {
//      logger.debug("Asking index for episodes by date");
//      return solrRequester.getEpisodesByDate(limit, offset);
//    } catch (SolrServerException e) {
//      throw new SearchException(e);
//    }
//  }
//
//  /**
//   * {@inheritDoc}
//   * 
//   * @see org.opencastproject.search.api.SearchService#getEpisodesBySeries(java.lang.String)
//   */
//  public SearchResult getEpisodesBySeries(String seriesId) throws SearchException {
//    try {
//      logger.debug("Searching index for episodes in series " + seriesId);
//      return solrRequester.getEpisodesBySeries(seriesId);
//    } catch (SolrServerException e) {
//      throw new SearchException(e);
//    }
//  }
//
//  /**
//   * {@inheritDoc}
//   * 
//   * @see org.opencastproject.search.api.SearchService#getSeriesByDate(int, int)
//   */
//  public SearchResult getSeriesByDate(int limit, int offset) throws SearchException {
//    try {
//      logger.debug("Asking index for series by date");
//      return solrRequester.getSeriesByDate(limit, offset);
//    } catch (SolrServerException e) {
//      throw new SearchException(e);
//    }
//  }
//
//  /**
//   * {@inheritDoc}
//   * 
//   * @see org.opencastproject.search.api.SearchService#getSeriesById(java.lang.String)
//   */
//  public SearchResult getSeriesById(String seriesId) throws SearchException {
//    try {
//      logger.debug("Searching index for series " + seriesId);
//      return solrRequester.getSeriesById(seriesId);
//    } catch (SolrServerException e) {
//      throw new SearchException(e);
//    }
//  }
//
//  /**
//   * {@inheritDoc}
//   * 
//   * @see org.opencastproject.search.api.SearchService#getSeriesByText(java.lang.String, int, int)
//   */
//  public SearchResult getSeriesByText(String text, int limit, int offset) throws SearchException {
//    try {
//      logger.debug("Searching index for series matching '" + text + "'");
//      return solrRequester.getSeriesByText(text, limit, offset);
//    } catch (SolrServerException e) {
//      throw new SearchException(e);
//    }
//  }

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
    if(mediaPackage == null) {
      throw new IllegalArgumentException("Unable to add a null mediapackage");
    }
    try {
      logger.debug("Attempting to add mediapackage {} to search index", mediaPackage.getIdentifier());
      if(solrIndexManager.add(mediaPackage)) {
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
