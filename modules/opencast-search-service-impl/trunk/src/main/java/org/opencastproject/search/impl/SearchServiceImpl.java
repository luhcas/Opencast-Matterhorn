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

package org.opencastproject.search.impl;

import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.search.api.SearchException;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.search.impl.solr.SolrConnection;
import org.opencastproject.search.impl.solr.SolrIndexManager;
import org.opencastproject.search.impl.solr.SolrRequester;
import org.opencastproject.util.PathSupport;

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
  private static final Logger log_ = LoggerFactory.getLogger(SearchServiceImpl.class);

  /** Connection to the solr database */
  private SolrConnection solrConnection = null;

  /** Solr query execution */
  private SolrRequester solrRequester = null;

  /** Manager for the solr search index */
  private SolrIndexManager solrIndexManager = null;

  /** The solr root directory */
  private String solrRoot = null;

  /**
   * Creates a search service that puts solr into the given root directory. If the directory doesn't exist, it will be
   * created by the service.
   * 
   * @param solrRoot
   *          the solr root directory
   */
  public SearchServiceImpl(String solrRoot) {
    this.solrRoot = solrRoot;
  }

  /**
   * Creates a search service that places solr into a subdirectory of <code>java.io.tmpdir</code> called
   * <code>opencast/searchindex</code>.
   */
  public SearchServiceImpl() {
    this(System.getProperty("java.io.tmpdir") + File.separator + "opencast" + File.separator + "searchindex");
  }

  /**
   * Prepares the solr environment.
   * 
   * @param solrRoot
   *          the solr root directory
   */
  private void setupSolr(String solrRoot) {
    try {
      log_.info("Setting up solr search index at {}", solrRoot);
      File solrConfigDir = new File(solrRoot, "conf");

      // Create the config directory
      if (solrConfigDir.exists()) {
        log_.info("solr search index found at {}", solrConfigDir);
      } else {
        log_.info("solr config directory doesn't exist.  Creating {}", solrConfigDir);
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
      solrConnection = new SolrConnection(solrRoot, PathSupport.concat(solrRoot, "data"));
      solrRequester = new SolrRequester(solrConnection);
      solrIndexManager = new SolrIndexManager(solrConnection);
    } catch (IOException e) {
      throw new RuntimeException("Error setting up solr index at " + solrRoot, e);
    }
  }

  private void copyClasspathResourceToFile(String classpath, File dir) {
    InputStream in = SearchServiceImpl.class.getResourceAsStream(classpath);
    try {
      File file = new File(dir, FilenameUtils.getName(classpath));
      log_.debug("copying inputstream " + in + " to file to " + file);
      IOUtils.copy(in, new FileOutputStream(file));
    } catch (IOException e) {
      throw new RuntimeException("Error copying solr classpath resource to the filesystem", e);
    }
  }

  /**
   * Service activator, called via declarative services configuration.
   * 
   * @param componentContext
   *          the component context
   */
  public void activate(ComponentContext componentContext) {
    setupSolr(solrRoot);
  }

  public void deactivate() {
    try {
      solrConnection.destroy();
    } catch (Throwable t) {
      log_.error("Error closing the solr connection");
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.api.SearchService#getEpisodeAndSeriesById(java.lang.String)
   */
  public SearchResult getEpisodeAndSeriesById(String seriesId) throws SearchException {
    try {
      log_.debug("Searching index for episodes and series details of series " + seriesId);
      return solrRequester.getEpisodeAndSeriesById(seriesId);
    } catch (SolrServerException e) {
      throw new SearchException(e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.api.SearchService#getEpisodeById(java.lang.String)
   */
  public SearchResult getEpisodeById(String episodeId) throws SearchException {
    try {
      log_.debug("Searching index for episode " + episodeId);
      return solrRequester.getEpisodeById(episodeId);
    } catch (SolrServerException e) {
      throw new SearchException(e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.api.SearchService#getEpisodesAndSeriesByText(java.lang.String, int, int)
   */
  public SearchResult getEpisodesAndSeriesByText(String text, int offset, int limit) throws SearchException {
    try {
      log_.debug("Searching index for episodes and series matching '" + text + "'");
      return solrRequester.getEpisodesAndSeriesByText(text, offset, limit);
    } catch (SolrServerException e) {
      throw new SearchException(e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.api.SearchService#getEpisodesByDate(int, int)
   */
  public SearchResult getEpisodesByDate(int offset, int limit) throws SearchException {
    try {
      log_.debug("Asking index for episodes by date");
      return solrRequester.getEpisodesByDate(offset, limit);
    } catch (SolrServerException e) {
      throw new SearchException(e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.api.SearchService#getEpisodesBySeries(java.lang.String)
   */
  public SearchResult getEpisodesBySeries(String seriesId) throws SearchException {
    try {
      log_.debug("Searching index for episodes in series " + seriesId);
      return solrRequester.getEpisodesBySeries(seriesId);
    } catch (SolrServerException e) {
      throw new SearchException(e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.api.SearchService#getEpisodesByText(java.lang.String, int, int)
   */
  public SearchResult getEpisodesByText(String text, int offset, int limit) throws SearchException {
    try {
      return solrRequester.getEpisodesByText(text, offset, limit);
    } catch (SolrServerException e) {
      throw new SearchException(e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.api.SearchService#getSeriesByDate(int, int)
   */
  public SearchResult getSeriesByDate(int limit, int offset) throws SearchException {
    try {
      log_.debug("Asking index for series by date");
      return solrRequester.getSeriesByDate(limit, offset);
    } catch (SolrServerException e) {
      throw new SearchException(e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.api.SearchService#getSeriesById(java.lang.String)
   */
  public SearchResult getSeriesById(String seriesId) throws SearchException {
    try {
      log_.debug("Searching index for series " + seriesId);
      return solrRequester.getSeriesById(seriesId);
    } catch (SolrServerException e) {
      throw new SearchException(e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.api.SearchService#getSeriesByText(java.lang.String, int, int)
   */
  public SearchResult getSeriesByText(String text, int offset, int limit) throws SearchException {
    try {
      log_.debug("Searching index for series matching '" + text + "'");
      return solrRequester.getSeriesByText(text, offset, limit);
    } catch (SolrServerException e) {
      throw new SearchException(e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.api.SearchService#add(org.opencastproject.media.mediapackage.MediaPackage)
   */
  public void add(MediaPackage mediaPackage) throws SearchException {
    try {
      log_.info("Adding mediapackage {} to search index", mediaPackage.getIdentifier());
      solrIndexManager.add(mediaPackage);
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
      log_.info("Removing mediapackage {} from search index", mediaPackageId);
      solrIndexManager.delete(mediaPackageId);
    } catch (SolrServerException e) {
      throw new SearchException(e);
    }
  }

}
