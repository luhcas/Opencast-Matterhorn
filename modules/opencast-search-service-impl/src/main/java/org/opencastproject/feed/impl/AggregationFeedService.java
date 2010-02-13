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
import org.opencastproject.search.impl.solr.SolrFields;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This feed generator creates a feed for the latest episodes across a set of series as specified by the service
 * property <code>feed.series</code>.
 * <p>
 * The service will answer requests matching the service property <code>feed.selector</code> as the first query argument
 * passed to the {@link #accept(String[])} method.
 */
public class AggregationFeedService extends AbstractFeedService implements FeedGenerator {

  /** Logging facility */
  private static Logger log_ = LoggerFactory.getLogger(AggregationFeedService.class);

  /** Property key for the series */
  private static final String PROP_SERIES = "feed.series";

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
      return searchService.getByQuery(solrQuery, limit, offset);
    } catch (Exception e) {
      log_.error("Cannot retrieve result for aggregated feed", e);
      return null;
    }
  }

  /**
   * Sets the series that are to be aggregated when creating the feed.
   * 
   * @param series
   *          the series identifier
   */
  public void setSeries(String[] series) {
    if (series == null || series.length == 0)
      throw new IllegalArgumentException("Series cannot be null or empty");

    // Create the solr query for the series
    StringBuffer q = new StringBuffer();
    q.append(SolrFields.DC_IS_PART_OF);
    q.append(":(");
    for (String s : series) {
      q.append(s).append(" ");
    }
    q.append(")");

    // Store the query
    solrQuery = q.toString();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.feed.impl.AbstractFeedService#activate(org.osgi.service.component.ComponentContext)
   */
  @Override
  public void activate(ComponentContext context) throws Exception {
    super.activate(context);
    String series = (String) context.getProperties().get(PROP_SERIES);
    if (series != null && !"".equals(series)) {
      setSeries(series.split("\\W"));
      log_.debug("Configuring aggregation feed with series {}", series);
    }
  }

}
