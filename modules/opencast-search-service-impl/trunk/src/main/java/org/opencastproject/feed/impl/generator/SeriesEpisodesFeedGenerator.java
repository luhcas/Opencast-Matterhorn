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

import org.opencastproject.feed.api.FeedGenerator;
import org.opencastproject.feed.api.Feed.Type;
import org.opencastproject.feed.impl.AbstractFeedGenerator;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This feed generator implements a feed for series. The series argument is taken from the first url parameter after the
 * feed type and version, and <code>accept()</code> returns <code>true</code> if the search service returns a result for
 * that series.
 */
public class SeriesEpisodesFeedGenerator extends AbstractFeedGenerator implements FeedGenerator {

  /** the logging facility provided by log4j */
  static Logger log_ = LoggerFactory.getLogger(SeriesEpisodesFeedGenerator.class);

  /** The series identifier */
  protected String seriesId = null;

  /** The search service */
  private SearchService searchService = null;

  /**
   * Creates a new feed generator for series.
   * 
   * @param feedHome
   *          the feed's home url
   * @param rssFlavor
   *          the flavor identifying rss tracks
   * @param entryLinkTemplate
   *          the link template
   */
  public SeriesEpisodesFeedGenerator(String feedHome, MediaPackageElementFlavor rssFlavor, String entryLinkTemplate) {
    super("series", feedHome, rssFlavor, entryLinkTemplate);
    setName("Series");
  }

  /**
   * @see org.opencastproject.feed.api.FeedGenerator#accept(java.lang.String[])
   */
  public boolean accept(String[] query) {
    if (searchService == null || query.length == 0)
      return false;

    // Build the series id. Last parameter is expected to be the
    // format identifier:
    StringBuffer id = new StringBuffer();
    int idparts = query.length - 1;
    for (int i = 0; i < idparts; i++) {
      if (id.length() > 0)
        id.append("/");
      id.append(query[i]);
    }

    // TODO: Store seriesId in a ThreadLocal
    seriesId = id.toString();
    try {
      // To check if we can accept the query it is enough to query for just one result
      // TODO: Store result in a ThreadLocal and reuse in loadFeedData();
      SearchResult result = searchService.getEpisodeAndSeriesById(seriesId);
      return result != null && result.size() > 0;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.feed.impl.AbstractFeedGenerator#getFeedIdentifier()
   */
  public String getFeedIdentifier() {
    return seriesId;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.feed.impl.AbstractFeedGenerator#getName()
   */
  public String getName() {
    return seriesId;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.feed.impl.AbstractFeedGenerator#getDescription()
   */
  public String getDescription() {
    return seriesId;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.feed.impl.AbstractFeedGenerator#loadFeedData(org.opencastproject.feed.api.Feed.Type,
   *      java.lang.String[], int, int)
   */
  protected SearchResult loadFeedData(Type type, String query[], int limit, int offset) {
    try {
      return searchService.getEpisodeAndSeriesById(seriesId);
    } catch (Exception e) {
      log_.error("Cannot retrieve solr result for feed '" + type.toString() + "' with query '" + query + "'.");
      return null;
    }
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

}
