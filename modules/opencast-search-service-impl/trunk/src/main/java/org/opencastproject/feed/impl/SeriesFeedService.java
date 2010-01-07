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

package org.opencastproject.feed.impl;

import org.opencastproject.feed.api.FeedGenerator;
import org.opencastproject.feed.api.Feed.Type;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.search.api.SearchResult;

/**
 * This feed generator implements a feed for series. The series argument is taken from the first url parameter after the
 * feed type and version, and {@link #accept(String[])} returns <code>true</code> if the search service returns a result
 * for that series identifier.
 */
public class SeriesFeedService extends AbstractFeedService implements FeedGenerator {

  /** The series identifier */
  protected ThreadLocal<String> series = new ThreadLocal<String>();

  /** The series data */
  protected ThreadLocal<SearchResult> seriesData = new ThreadLocal<SearchResult>();

  /**
   * Creates a new feed generator for series.
   * 
   * @param feedHome
   *          the feed's home url
   * @param rssFlavor
   *          the flavor identifying rss tracks
   * @param atomFlavor
   *          the flavors identifying tracks to be included in atom feeds
   * @param entryLinkTemplate
   *          the link template
   */
  public SeriesFeedService(String feedHome, MediaPackageElementFlavor rssFlavor, MediaPackageElementFlavor[] atomFlavors, String entryLinkTemplate) {
    super("series", feedHome, rssFlavor, atomFlavors, entryLinkTemplate);
    setName("Series");
  }

  /**
   * @see org.opencastproject.feed.api.FeedGenerator#accept(java.lang.String[])
   */
  public boolean accept(String[] query) {
    boolean generalChecksPassed = super.accept(query);
    if (!generalChecksPassed)
      return false;

    // Build the series id, first parameter is the selector. Note that if the series identifier
    // contained slashes (e. g. in the case of a handle or doi), we need to reassemble the
    // identifier
    StringBuffer id = new StringBuffer();
    int idparts = query.length - 1;
    for (int i = 1; i < idparts; i++) {
      if (id.length() > 0)
        id.append("/");
      id.append(query[i]);
    }

    try {
      // To check if we can accept the query it is enough to query for just one result
      SearchResult result = searchService.getEpisodeAndSeriesById(id.toString());
      if (result != null && result.size() > 0) {
        series.set(id.toString());
        seriesData.set(result);
        return true;
      }
    } catch (Exception e) {
      return false;
    }
    return false;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.feed.impl.AbstractFeedGenerator#getIdentifier()
   */
  public String getIdentifier() {
    return series.get();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.feed.impl.AbstractFeedGenerator#getName()
   */
  public String getName() {
    return seriesData.get().getItems()[0].getDcTitle();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.feed.impl.AbstractFeedGenerator#getDescription()
   */
  public String getDescription() {
    return seriesData.get().getItems()[0].getDcAbstract();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.feed.impl.AbstractFeedGenerator#loadFeedData(org.opencastproject.feed.api.Feed.Type,
   *      java.lang.String[], int, int)
   */
  protected SearchResult loadFeedData(Type type, String query[], int limit, int offset) {
    return seriesData.get();
  }

}
