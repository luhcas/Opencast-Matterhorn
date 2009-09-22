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
import org.apache.solr.client.solrj.SolrServerException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

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
   * Creates a search service that places solr into a subdirectory of 
   * <code>java.io.tmpdir</code> called <code>opencast/searchindex</code>.
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
      log_.info("Setting up solr search index at " + solrRoot);
      File solrRootDir = new File(solrRoot);

      // Create the root directory
      if (!solrRootDir.exists()) {
        FileUtils.forceMkdir(solrRootDir);
      }

      // Make sure there is a configuration in place
      File solrConfig = new File(solrRoot, "conf");
      if (!solrConfig.exists()) {
        URL defaultSolrConfig = SearchServiceImpl.class.getResource("/solr/conf");
        try {
          FileUtils.copyDirectoryToDirectory(new File(defaultSolrConfig.toURI()), solrRootDir);
        } catch (URISyntaxException e) {
          throw new RuntimeException("Error creating the solr configuration directory", e);
        }
      }

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
   * @param seriesId
   * @return
   * @throws SearchException
   * @see org.opencastproject.search.impl.solr.SolrRequester#getEpisodesAndSeriesById(java.lang.String)
   */
  public SearchResult getEpisodeAndSeriesById(String seriesId) throws SearchException {
    try {
      return solrRequester.getEpisodeAndSeriesById(seriesId);
    } catch (SolrServerException e) {
      throw new SearchException(e);
    }
  }

  /**
   * @param episodeId
   * @return
   * @throws SearchException
   * @see org.opencastproject.search.impl.solr.SolrRequester#getEpisodeById(java.lang.String)
   */
  public SearchResult getEpisodeById(String episodeId) throws SearchException {
    try {
      return solrRequester.getEpisodeById(episodeId);
    } catch (SolrServerException e) {
      throw new SearchException(e);
    }
  }

  /**
   * @param text
   * @param offset
   * @param limit
   * @return
   * @throws SearchException
   * @see org.opencastproject.search.impl.solr.SolrRequester#getEpisodesAndSeriesByText(java.lang.String, int, int)
   */
  public SearchResult getEpisodesAndSeriesByText(String text, int offset, int limit) throws SearchException {
    try {
      return solrRequester.getEpisodesAndSeriesByText(text, offset, limit);
    } catch (SolrServerException e) {
      throw new SearchException(e);
    }
  }

  /**
   * @param limit
   * @param offset
   * @return
   * @throws SearchException
   * @see org.opencastproject.search.impl.solr.SolrRequester#getEpisodesByDate(int, int)
   */
  public SearchResult getEpisodesByDate(int offset, int limit) throws SearchException {
    try {
      return solrRequester.getEpisodesByDate(offset, limit);
    } catch (SolrServerException e) {
      throw new SearchException(e);
    }
  }

  /**
   * @param seriesId
   * @return
   * @throws SearchException
   * @see org.opencastproject.search.impl.solr.SolrRequester#getEpisodesBySeries(java.lang.String)
   */
  public SearchResult getEpisodesBySeries(String seriesId) throws SearchException {
    try {
      return solrRequester.getEpisodesBySeries(seriesId);
    } catch (SolrServerException e) {
      throw new SearchException(e);
    }
  }

  /**
   * @param text
   * @param offset
   * @param limit
   * @return
   * @throws SearchException
   * @see org.opencastproject.search.impl.solr.SolrRequester#getEpisodesByText(java.lang.String, int, int)
   */
  public SearchResult getEpisodesByText(String text, int offset, int limit) throws SearchException {
    try {
      return solrRequester.getEpisodesByText(text, offset, limit);
    } catch (SolrServerException e) {
      throw new SearchException(e);
    }
  }

  /**
   * @param limit
   * @param offset
   * @return
   * @throws SearchException
   * @see org.opencastproject.search.impl.solr.SolrRequester#getSeriesByDate(int, int)
   */
  public SearchResult getSeriesByDate(int limit, int offset) throws SearchException {
    try {
      return solrRequester.getSeriesByDate(limit, offset);
    } catch (SolrServerException e) {
      throw new SearchException(e);
    }
  }

  /**
   * @param seriesId
   * @param limit
   * @param offset
   * @return
   * @throws SearchException
   * @see org.opencastproject.search.impl.solr.SolrRequester#getSeriesById(java.lang.String, int, int)
   */
  public SearchResult getSeriesById(String seriesId) throws SearchException {
    try {
      return solrRequester.getSeriesById(seriesId);
    } catch (SolrServerException e) {
      throw new SearchException(e);
    }
  }

  /**
   * @param text
   * @param offset
   * @param limit
   * @return
   * @throws SearchException
   * @see org.opencastproject.search.impl.solr.SolrRequester#getSeriesByText(java.lang.String, int, int)
   */
  public SearchResult getSeriesByText(String text, int offset, int limit) throws SearchException {
    try {
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
      solrIndexManager.delete(mediaPackageId);
    } catch (SolrServerException e) {
      throw new SearchException(e);
    }
  }

}