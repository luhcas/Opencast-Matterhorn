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

import org.opencastproject.feed.api.Feed.Type;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchService;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

/**
 * Convenience implementation that is intended to serve as a base implementation for feed generator services. It handles
 * service activation, reads a default set of properties (see below) and can be configured to track the opencast
 * {@link SearchService} by using {@link #setSearchService(SearchService)}.
 * <p>
 * By using this implementation as the basis for feed services, only the two methods {@link #accept(String[])} and
 * {@link #loadFeedData(Type, String[], int, int)} need to be implemented by subclasses.
 * <p>
 * In the {@link #activate(ComponentContext)} method, the following properties are being read from the component
 * properties:
 * <ul>
 * <li><code>feed.uri</code> - the feed uri</li>
 * <li><code>feed.selector</code> the pattern that is used to determine if the feed implementation wants to handle a
 * request, e. g. the selector {{latest}} in {{http://<servername>/feeds/atom/0.3/latest}} maps the latest feed handler
 * to urls containing that selector</li>
 * <li><code>feed.name</code> - name of this feed</li>
 * <li><code>feed.description</code> - an abstract of this feed</li>
 * <li><code>feed.copyright</code> - the feed copyright note</li>
 * <li><code>feed.home</code> - url of the feed's home page</li>
 * <li><code>feed.cover</code> - url of the feed's cover image</li>
 * <li><code>feed.entry</code> - template to create a link to a feed entry</li>
 * <li><code>feed.rssflavor</code> - flavor identifying rss feed media package elements</li>
 * <li><code>feed.atomflavors</code> - comma separated list of flavors identifying atom feed media package elements</li>
 * </ul>
 */
public abstract class AbstractFeedService extends AbstractFeedGenerator {

  /** Logging facility */
  private static Logger log_ = LoggerFactory.getLogger(AbstractFeedService.class);

  /** Property key for the feed uri */
  public static final String PROP_URI = "feed.uri";

  /** Property key for the feed selector pattern */
  public static final String PROP_SELECTOR = "feed.selector";

  /** Property key for the feed name */
  public static final String PROP_NAME = "feed.name";

  /** Property key for the feed description */
  public static final String PROP_DESCRIPTION = "feed.description";

  /** Property key for the feed copyright note */
  public static final String PROP_COPYRIGHT = "feed.copyright";

  /** Property key for the feed home url */
  public static final String PROP_HOME = "feed.home";

  /** Property key for the feed cover url */
  public static final String PROP_COVER = "feed.cover";

  /** Property key for the feed entry link template */
  public static final String PROP_ENTRY = "feed.entry";

  /** Property key for the feed rss media element flavor */
  public static final String PROP_RSSFLAVOR = "feed.rssflavor";

  /** Property key for the feed atom media element flavor */
  public static final String PROP_ATOMFLAVORS = "feed.atomflavors";

  /** The selector used to match urls */
  protected String selector = null;

  /** The search service */
  protected SearchService searchService = null;

  /**
   * Creates a new abstract feed generator.
   * <p>
   * <b>Note:</b> Subclasses using this constructor need to set required member variables prior to calling
   * {@link #createFeed(org.opencastproject.feed.api.Feed.Type, String[])} for the first time.
   */
  protected AbstractFeedService() {
    super();
  }

  /**
   * Creates a new abstract feed generator.
   * 
   * @param uri
   *          the feed identifier
   * @param feedHome
   *          the feed's home url
   * @param rssFlavor
   *          the flavor identifying rss tracks
   * @param atomFlavor
   *          the flavors identifying tracks to be included in atom feeds
   * @param entryLinkTemplate
   *          the link template
   */
  public AbstractFeedService(String uri, String feedHome, MediaPackageElementFlavor rssFlavor,
          MediaPackageElementFlavor[] atomFlavors, String entryLinkTemplate) {
    super(uri, feedHome, rssFlavor, atomFlavors, entryLinkTemplate);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.feed.api.FeedGenerator#accept(java.lang.String[])
   */
  public boolean accept(String[] query) {
    if (searchService == null) {
      log_.warn("{} denies to handle request for {} due to missing search service", this, query);
      return false;
    } else if (selector == null) {
      log_.warn("{} denies to handle request for {} since no selector is defined", this);
      return false;
    } else if (query.length == 0) {
      log_.debug("{} denies to handle unknown request", this);
      return false;
    } else if (!query[0].toLowerCase().equals(selector)) {
      log_.debug("{} denies to handle request for {}", this, query);
      return false;
    }
    log_.debug("{} accepts to handle request for {}", this, query);
    return true;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.feed.impl.AbstractFeedGenerator#loadFeedData(org.opencastproject.feed.api.Feed.Type,
   *      java.lang.String[], int, int)
   */
  protected abstract SearchResult loadFeedData(Type type, String[] query, int limit, int offset);

  /**
   * Callback used by the OSGi environment when this component is started. The method tries to read the following
   * properties from the service's component context:
   * <ul>
   * <li><code>feed.uri</code> - the feed uri</li>
   * <li><code>feed.selector</code> the pattern that is used to determine if the feed implementation wants to handle a
   * request, e. g. the selector {{latest}} in {{http://<servername>/feeds/atom/0.3/latest}} maps the latest feed
   * handler to urls containing that selector</li>
   * <li><code>feed.name</code> - name of this feed</li>
   * <li><code>feed.description</code> - an abstract of this feed</li>
   * <li><code>feed.copyright</code> - the feed copyright note</li>
   * <li><code>feed.home</code> - url of the feed's home page</li>
   * <li><code>feed.cover</code> - url of the feed's cover image</li>
   * <li><code>feed.entry</code> - template to create a link to a feed entry</li>
   * <li><code>feed.rssflavor</code> - media package flavor identifying rss feed media package elements</li>
   * <li><code>feed.atomflavors</code> - comma separated list of flavors identifying atom feed media package elements</li>
   * </ul>
   * 
   * @param context
   *          the osgi component context
   * @throws Exception
   *           if starting the component is resulting in an error
   */
  public void activate(ComponentContext context) throws Exception {
    Dictionary<?, ?> properties = context.getProperties();
    uri = (String) properties.get(PROP_URI);
    selector = (String) properties.get(PROP_SELECTOR);
    name = (String) properties.get(PROP_NAME);
    description = (String) properties.get(PROP_DESCRIPTION);
    copyright = (String) properties.get(PROP_COPYRIGHT);
    home = (String) properties.get(PROP_HOME);
    cover = (String) properties.get(PROP_COVER);
    linkTemplate = (String) properties.get(PROP_ENTRY);
    String rssFlavor = (String) properties.get(PROP_RSSFLAVOR);
    if (rssFlavor != null)
      rssTrackFlavor = MediaPackageElementFlavor.parseFlavor(rssFlavor);
    String atomFlavors = (String) properties.get(PROP_ATOMFLAVORS);
    if (atomFlavors != null) {
      String[] flavors = atomFlavors.split(",; ");
      for (String f : flavors)
        addAtomTrackFlavor(MediaPackageElementFlavor.parseFlavor(f));
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

  /**
   * Returns the search service.
   * 
   * @return the search services
   */
  protected SearchService getSearchService() {
    return searchService;
  }

}
