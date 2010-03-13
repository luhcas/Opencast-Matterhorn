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

package org.opencastproject.feed.impl;

import org.opencastproject.feed.api.FeedGenerator;
import org.opencastproject.feed.api.Feed.Type;
import org.opencastproject.search.api.SearchResult;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;

/**
 * This feed generator creates a feed for the episodes returned by the query specified by the service property
 * <code>feed.query</code>. Additional arguments will be passed to the query by means of
 * {@link MessageFormat#format(String, Object...).
 * <p>
 * The service will answer requests matching the service property <code>feed.selector</code> as the first query argument
 * passed to the {@link #accept(String[])} method.
 */
public class CustomFeedService extends AbstractFeedService implements FeedGenerator {

  /** Logging facility */
  private static Logger log_ = LoggerFactory.getLogger(CustomFeedService.class);

  /** Property key for the query */
  private static final String PROP_QUERY = "feed.query";

  /** The solr query */
  private String solrQuery = null;

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.feed.api.FeedGenerator#accept(java.lang.String[])
   */
  public boolean accept(String[] query) {
    if (solrQuery == null) {
      log_.warn("{} denies to handle request for {} since query is still undefined", this, query);
      return false;
    }
    return super.accept(query);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.feed.impl.AbstractFeedGenerator#loadFeedData(org.opencastproject.feed.api.Feed.Type,
   *      java.lang.String[], int, int)
   */
  protected SearchResult loadFeedData(Type type, String query[], int limit, int offset) {
    try {
      String q = solrQuery;
      if (query != null && query.length > 1) {
        Object[] args = new Object[query.length - 1];
        for (int i=1; i<query.length; i++)
          args[i-1] = query[i];
        q = MessageFormat.format(solrQuery, args);
      }
      return searchService.getByQuery(q, limit, offset);
    } catch (Exception e) {
      log_.error("Cannot retrieve result for aggregated feed", e);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.feed.impl.AbstractFeedService#activate(org.osgi.service.component.ComponentContext)
   */
  @Override
  public void activate(ComponentContext context) throws Exception {
    super.activate(context);
    String query = (String) context.getProperties().get(PROP_QUERY);
    if (query != null && !"".equals(query)) {
      solrQuery = query;
      log_.debug("Configuring custom feed with query '{}'", query);
    }
  }

}
