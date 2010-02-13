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
import org.opencastproject.search.api.SearchResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This feed generator creates a feed for the latest episodes across all series.
 */
public class LatestFeedService extends AbstractFeedService implements FeedGenerator {

  /** Logging facility */
  private static Logger log_ = LoggerFactory.getLogger(LatestFeedService.class);

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.feed.impl.AbstractFeedGenerator#loadFeedData(org.opencastproject.feed.api.Feed.Type,
   *      java.lang.String[], int, int)
   */
  protected SearchResult loadFeedData(Type type, String query[], int limit, int offset) {
    try {
      return searchService.getEpisodesByDate(limit, offset);
    } catch (Exception e) {
      log_.error("Cannot retrieve result for feed 'recent episodes'", e);
      return null;
    }
  }

}
