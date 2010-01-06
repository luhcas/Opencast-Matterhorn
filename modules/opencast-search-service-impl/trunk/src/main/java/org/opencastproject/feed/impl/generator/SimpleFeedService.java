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

package org.opencastproject.feed.impl.generator;

import org.opencastproject.feed.api.Feed;
import org.opencastproject.feed.api.FeedGenerator;
import org.opencastproject.feed.impl.AbstractFeedService;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.search.impl.solr.SolrFields;
import org.opencastproject.util.StringSupport;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * This feed generator creates the feeds from the properties file <code>/WEB-INF/conf/feeds.properties</code>.
 */
public class SimpleFeedService extends AbstractFeedService implements FeedGenerator {

  /** the logging facility provided by log4j */
  protected final static Logger log_ = LoggerFactory.getLogger(SimpleFeedService.class);

  /** The defined feeds */
  protected static Map<String, FeedDefinition> feedDefinitions = null;

  /** The feed defintion */
  protected FeedDefinition feed = null;

  /** The search service */
  private SearchService searchService = null;

  /**
   * Creates a new aggregated feed.
   * 
   * @param uri
   *          the feed identifier
   * @param feedHome
   *          the feed's home url
   * @param rssFlavor
   *          the flavor identifying rss tracks
   * @param entryLinkTemplate
   *          the link template
   * @param propertiesFile
   *          path to the properties
   */
  public SimpleFeedService(String uri, String feedHome, MediaPackageElementFlavor rssFlavor, String entryLinkTemplate,
          String propertiesFile) {
    super(uri, propertiesFile, rssFlavor, entryLinkTemplate);
    init(propertiesFile);
  }

  /**
   * Callback used by the OSGi environment when this component is started.
   * 
   * @param context
   *          the osgi component context
   * @throws Exception
   *           if starting the component is resulting in an error
   */
  public void start(ComponentContext context) throws Exception {
    // TODO: Implement
  }

  /**
   * Callback used by the OSGi environment when this component is taken down.
   * 
   * @param context
   *          the osgi component context
   * @throws Exception
   *           if taking the component down is resulting in an error
   */
  public void stop(ComponentContext context) throws Exception {
    // TODO: Implement
  }

  /**
   * Sets the search service.
   * 
   * @param searchService
   *          the search service
   */
  public void setSearchService(SearchService searchService) {
    this.searchService = searchService;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.feed.impl.AbstractFeedService#getIdentifier()
   */
  public String getIdentifier() {
    return feed.getUri();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.feed.impl.AbstractFeedService#getName()
   */
  public String getName() {
    return feed.getName();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.feed.impl.AbstractFeedService#getDescription()
   */
  public String getDescription() {
    return feed.getDescription();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.feed.impl.AbstractFeedService#getFeedLink()
   */
  public String getFeedLink() {
    return feed.getLink();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.feed.api.FeedGenerator#getCopyright()
   */
  public String getCopyright() {
    return feed.getCopyright();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.feed.api.FeedGenerator#getCover()
   */
  @Override
  public String getCover() {
    return feed.getCover();
  }

  /**
   * @see AbstractFeedService.ethz.replay.core.store.feed.AbstractFeedGenerator#loadFeedData(org.opencastproject.feed.api.Feed.Type,
   *      java.lang.String[], int, int)
   */
  @Override
  protected SearchResult loadFeedData(Feed.Type type, String query[], int limit, int offset) {
    try {
      return searchService.getByQuery(feed.getQuery(), DEFAULT_LIMIT, DEFAULT_OFFSET);
    } catch (Exception e) {
      log_.error("Cannot retrieve solr result for feed 'recent episodes'");
      return null;
    }
  }

  /**
   * @see org.opencastproject.feed.api.FeedGenerator#accept(java.lang.String[])
   */
  public boolean accept(String[] query) {
    if (query == null || query.length == 0)
      return false;
    feed = feedDefinitions.get(query[0]);
    return feed != null;
  }

  /**
   * Tries to read the feed definitions from the properties file that must be located in /WEB-INF/conf/
   * 
   * @param propertiesFile
   *          name of the properties file
   */
  private void init(String propertiesFile) {
    File configFile = new File(propertiesFile);
    if (!configFile.exists())
      return;
    Properties properties = new Properties();
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(configFile);
      properties.load(fis);
      List<String> feedIds = extractFeedKeys(properties);
      feedDefinitions = new HashMap<String, FeedDefinition>();
      for (String feedId : feedIds) {
        FeedDefinition feedDefinition = null;
        String name = properties.getProperty(feedId + ".name");
        String description = properties.getProperty(feedId + ".description");
        String copyright = properties.getProperty(feedId + ".copyright");
        String url = properties.getProperty(feedId + ".url");
        String cover = properties.getProperty(feedId + ".cover");
        String query = properties.getProperty(feedId + ".query");
        String series = properties.getProperty(feedId + ".series");
        if (series != null && !StringSupport.isEmpty(series)) {
          StringTokenizer tok = new StringTokenizer(series, ",");
          List<String> seriesList = new ArrayList<String>();
          while (tok.hasMoreTokens()) {
            seriesList.add(tok.nextToken().trim());
          }
          feedDefinition = new FeedDefinition(feedId, name, description, copyright, url, cover, seriesList
                  .toArray(new String[seriesList.size()]));
        } else {
          feedDefinition = new FeedDefinition(feedId, name, description, copyright, url, cover, query);
        }
        feedDefinitions.put(feedId, feedDefinition);
      }
    } catch (FileNotFoundException e) {
      log_.error("Error loading feed configuration " + configFile, e);
    } catch (IOException e) {
      log_.error("Error loading feed configuration " + configFile, e);
    } finally {
      if (fis != null)
        try {
          fis.close();
        } catch (IOException e) {
        }
    }
  }

  /**
   * Extracts the feed identifiers from the properties file.
   * 
   * @param properties
   *          the properties
   * @return a list of feed identifier
   */
  private List<String> extractFeedKeys(Properties properties) {
    List<String> feeds = new ArrayList<String>();
    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
      String key = entry.getKey().toString();
      int separatorLocation = key.indexOf('.');
      if (separatorLocation > 0) {
        String feedId = key.substring(0, separatorLocation);
        if (!feeds.contains(feedId))
          feeds.add(feedId);
      }
    }
    return feeds;
  }

  /**
   * This class holds the configuration for a configured feed.
   */
  private class FeedDefinition {

    /** The feed identifier */
    private String identifier = null;

    /** The feed name */
    private String name = null;

    /** The feed description */
    private String description = null;

    /** The feed copyright */
    private String copyright = null;

    /** Url to the cover image */
    private String cover = null;

    /** Url to the content page */
    private String link = null;

    /** Array of series identifier */
    private String[] series = null;

    /** configured query expression */
    private String query = null;

    /** The solr query */
    private String solrQuery = null;

    /**
     * Creates the feed definition from a list of series identifier.
     * 
     * @param uri
     *          the feed identifier
     * @param name
     *          the feed name
     * @param description
     *          the feed description
     * @param copyright
     *          the feed copyright
     * @param link
     *          url to the feeds homepage
     * @param cover
     *          the cover url
     * @param series
     *          the series used to create the feed
     */
    FeedDefinition(String uri, String name, String description, String copyright, String link, String cover,
            String[] series) {
      this.identifier = uri;
      this.name = name;
      this.description = description;
      this.copyright = copyright;
      this.link = link;
      this.cover = cover;
      this.series = series;
    }

    /**
     * Creates the feed definition from a query expression.
     * 
     * @param uri
     *          the feed identifier
     * @param name
     *          the feed name
     * @param description
     *          the feed description
     * @param copyright
     *          the feed copyright
     * @param link
     *          url to the feeds homepage
     * @param cover
     *          the cover url
     * @param query
     *          the user defined query
     */
    FeedDefinition(String uri, String name, String description, String copyright, String link, String cover,
            String query) {
      this.identifier = uri;
      this.name = name;
      this.description = description;
      this.copyright = copyright;
      this.link = link;
      this.cover = cover;
      this.query = query;
    }

    /**
     * Returns the feed identifier.
     * 
     * @return the feed identifier
     */
    public String getUri() {
      return identifier;
    }

    /**
     * Returns the feed name.
     * 
     * @return the name
     */
    public String getName() {
      return name;
    }

    /**
     * Returns the feed description.
     * 
     * @return the description
     */
    public String getDescription() {
      return description;
    }

    /**
     * Returns the feed copyright.
     * 
     * @return the copyright notice
     */
    public String getCopyright() {
      return copyright;
    }

    /**
     * Returns the cover url.
     * 
     * @return the cover
     */
    public String getCover() {
      return cover;
    }

    /**
     * Returns the url to the feed homepage.
     * 
     * @return the link
     */
    public String getLink() {
      return link;
    }

    /**
     * Returns the solr query that is used to create the list of episode items for the feed.
     * 
     * @return the query
     */
    public String getQuery() {
      solrQuery = createQuery();
      return solrQuery;
    }

    /**
     * Creates the solr query used to lookup the episodes from the solr search index.
     * 
     * @return the solr search index
     */
    private String createQuery() {
      StringBuffer solrQuery = new StringBuffer();
      if (series != null && series.length > 0) {
        solrQuery.append(SolrFields.DC_IS_PART_OF);
        solrQuery.append(":(");
        for (int i = 0; i < series.length; i++) {
          solrQuery.append(series[i]);
          solrQuery.append(" ");
        }
        solrQuery.append(")");
        log_.trace("Feed request for series: '" + solrQuery + "''");
      } else if (query != null) {
        solrQuery.append(query);
        log_.trace("Feed request for custom solr query: '" + solrQuery + "'");
      } else {
        throw new IllegalStateException("Neither series nor query have been defined");
      }
      return solrQuery.toString();
    }

  }

}
