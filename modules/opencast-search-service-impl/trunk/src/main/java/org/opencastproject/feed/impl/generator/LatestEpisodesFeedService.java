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
import org.opencastproject.feed.impl.AbstractFeedService;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This feed generator creates a feed for the latest episodes across all series.
 */
public class LatestEpisodesFeedService extends AbstractFeedService implements FeedGenerator {

  /** Logging facility */
  private static Logger log_ = LoggerFactory.getLogger(LatestEpisodesFeedService.class);

  /** The feed type */
  private static final String QUERY = "recent";
  
  /** The search service */
  private SearchService searchService = null;

  /**
   * Creates a new feed generator that produces a feed containing the latest episodes over all series.
   * 
   * @param uri
   *          the feed identifier
   * @param feedHome
   *          link to the feed's homepage
   * @param rssFlavor
   *          flavor identifying the track for rss feeds
   * @param entryLinkTemplate
   *          template for link generation
   */
  public LatestEpisodesFeedService(String uri, String feedHome, MediaPackageElementFlavor rssFlavor,
          String entryLinkTemplate) {
    super(QUERY, feedHome, rssFlavor, entryLinkTemplate);
    setName("Recent episodes");
  }

  /**
   * @see org.opencastproject.feed.api.FeedGenerator#accept(java.lang.String[])
   */
  public boolean accept(String[] query) {
    return searchService != null && query.length > 0 && QUERY.equals(query[0].toLowerCase());
  }

  /**
   * Sets the search service.
   * 
   * @param searchService the search service
   */
  public void setSearchService(SearchService searchService) {
    this.searchService = searchService;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.feed.impl.AbstractFeedService#loadFeedData(org.opencastproject.feed.api.Feed.Type,
   *      java.lang.String[], int, int)
   */
  protected SearchResult loadFeedData(Type type, String query[], int limit, int offset) {
    try {
      return searchService.getEpisodesByDate(DEFAULT_LIMIT, DEFAULT_OFFSET);
    } catch (Exception e) {
      log_.error("Cannot retrieve solr result for feed 'recent episodes'");
      return null;
    }
  }

}
