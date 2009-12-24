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

package org.opencastproject.feed.api;

import java.util.Locale;

/**
 * A <code>FeedGenerator</code> is able to create an xml feed of the requested type, based on a query string.
 * <p>
 * The implementation must either return a valid feed node or <code>null</code> if it cannot satisfy the query.
 */
public interface FeedGenerator {

  /**
   * Returns the feed identifier.
   * 
   * @return the feed identifier
   */
  String getFeedIdentifier();

  /**
   * Return the locale dependend feed name.
   * 
   * @return the feed name
   */
  String getFeedName(Locale locale);

  /**
   * Return the locale dependend feed description
   */
  String getFeedDescription(Locale locale);

  /**
   * Return the locale dependend feed link.
   * 
   * @return the feed link
   */
  String getFeedLink(Locale locale);

  /**
   * Returns <code>true</code> if the generator is able to satisfy the request for a feed described by the query. The
   * query consists of all the elements that are found in the request, separated by a slash.
   * 
   * @return <code>true</code> if the generator can handle the query
   */
  boolean accept(String[] query);

  /**
   * Returns <code>null</code> if the generator cannot deal with the request. Otherwise it must returns a valid xml
   * feed.
   * 
   * @param type
   *          the feed type
   * @param query
   *          the request
   * @param locale
   *          the request locale
   * @return the feed or <code>null</code>
   */
  Feed createFeed(Feed.Type type, String[] query, Locale locale);

  /**
   * Sets the entry's base url that will be used to form the episode link in the feeds. If the url contains a
   * placeholder in the form <code>{0}</code>, it will be replaced by the episode id.
   * 
   * @param url
   *          the url
   */
  void setLinkTemplate(String url);

}
