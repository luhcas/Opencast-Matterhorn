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

import org.opencastproject.feed.api.Content;
import org.opencastproject.feed.api.Enclosure;
import org.opencastproject.feed.api.Feed;
import org.opencastproject.feed.api.FeedEntry;
import org.opencastproject.feed.api.FeedExtension;
import org.opencastproject.feed.api.FeedGenerator;
import org.opencastproject.feed.api.Content.Mode;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchResultItem;
import org.opencastproject.search.api.SearchResultItem.SearchResultItemType;
import org.opencastproject.util.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class provides basic functionality for creating feeds and is used as the base implementation for the default
 * feed generators.
 */
public abstract class AbstractFeedGenerator implements FeedGenerator {

  /** A default value for limit */
  protected static final int DEFAULT_LIMIT = 10;

  /** Unlimited */
  protected static final int NO_LIMIT = Integer.MAX_VALUE;

  /** A default value for offset */
  protected static final int DEFAULT_OFFSET = 0;

  /** The date parser format **/
  protected static final String DATE_FORMAT = "dd.MM.yyyy HH:mm:ss";

  /** The default feed encoding */
  public static final String ENCODING = "UTF-8";

  /** Link to the user interface */
  protected String linkTemplate = null;

  /** The feed **/
  protected Feed feed = null;

  /** The feed homepage */
  protected String feedHome = null;

  /** Feed extensions */
  protected List<FeedExtension> extensions = null;

  /** Default format for rss feeds */
  protected MediaPackageElementFlavor rssTrackFlavor = null;

  /** the feed uri */
  protected String identifier = null;

  /** The feed name */
  protected String name = null;

  /** Url to the cover image */
  protected String cover = null;

  /** Copyright notice */
  protected String copyright = null;

  /** The feed description */
  protected String description = null;

  /** the logging facility provided by log4j */
  private final static Logger log_ = LoggerFactory.getLogger(AbstractFeedGenerator.class);

  /**
   * Creates a new abstract feed generator.
   * 
   * @param uri
   *          the feed identifier
   * @param feedHome
   *          the feed's home url
   * @param rssFlavor
   *          the flavor identifying rss tracks
   * @param entryLinkTemplate
   *          the link template
   */
  public AbstractFeedGenerator(String uri, String feedHome, MediaPackageElementFlavor rssFlavor,
          String entryLinkTemplate) {
    this.identifier = uri;
    this.feedHome = feedHome;
    this.rssTrackFlavor = rssFlavor;
    this.linkTemplate = entryLinkTemplate;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.feed.api.FeedGenerator#getFeedIdentifier()
   */
  public String getFeedIdentifier() {
    return identifier;
  }

  /**
   * Sets the feed name.
   * 
   * @param name
   *          the feed name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.feed.api.FeedGenerator#getName()
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the feed description.
   * 
   * @param description
   *          the feed description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.feed.api.FeedGenerator#getDescription()
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the copyright notice.
   * 
   * @param copyright
   *          the copyright notice
   */
  public void setCopyright(String copyright) {
    this.copyright = copyright;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.feed.api.FeedGenerator#getCopyright()
   */
  @Override
  public String getCopyright() {
    return copyright;
  }

  /**
   * Sets the url to the cover url.
   * 
   * @param cover
   *          the cover url
   */
  public void setCover(String cover) {
    this.cover = cover;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.feed.api.FeedGenerator#getCover()
   */
  @Override
  public String getCover() {
    return cover;
  }

  /**
   * Loads and returns the feed data.
   * 
   * @param type
   *          the requested feed type
   * @param query
   *          the query parameter
   * @param limit
   *          the number of entries
   * @param offset
   *          the starting entry
   * @return the feed data
   */
  protected abstract SearchResult loadFeedData(Feed.Type type, String query[], int limit, int offset);

  /**
   * Creates a new feed.
   * 
   * @param uri
   *          the feed identifier
   * @param title
   *          the feed title
   * @param description
   *          the feed description
   * @param link
   *          the link to the feed homepage
   * @return the new feed
   */
  protected Feed createFeed(String uri, Content title, Content description, String link) {
    return new FeedImpl(uri, title, description, link);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.feed.api.FeedGenerator#createFeed(org.opencastproject.feed.api.Feed.Type,
   *      java.lang.String[])
   */
  public Feed createFeed(Feed.Type type, String[] query) {
    SearchResult result = null;

    // Have the concrete implementation load the feed data
    try {
      result = loadFeedData(type, query, DEFAULT_LIMIT, DEFAULT_OFFSET);
    } catch (Exception e) {
      log_.error("Cannot retrieve solr result for feed '" + type.toString() + "' with query '" + query + "'.");
      return null;
    }

    // Create the feed
    feed = createFeed(getFeedIdentifier(), new ContentImpl(getName()), new ContentImpl(getDescription()), getFeedLink());
    feed.setEncoding(ENCODING);

    // TODO: Set feed icon and other metadata

    // Check if a default format has been specified
    // TODO: Parse flavor and set member variable rssTrackFlavor
    // String rssFlavor = query.length > 1 ? query[query.length - 1] : null;

    // Iterate over the feed data and create the entries
    for (SearchResultItem resultItem : result.getItems()) {
      try {
        extensions.clear();
        if (resultItem.getType().equals(SearchResultItemType.Series))
          addSeries(type, query, resultItem);
        else
           addEpisode(type, query, resultItem);
      } catch (Throwable t) {
        log_.error("Error creating entry with id " + resultItem.getId() + " for feed " + this + ": " + t.getMessage(),
                t);
      }
    }
    return feed;
  }

  /**
   * Adds series information to the feed.
   * 
   * @param type the feed type
   * @param query the query that results in the feed
   * @param resultItem the series item
   * @return the feed
   */
  protected Feed addSeries(Feed.Type type, String[] query, SearchResultItem resultItem) {
    Date d = resultItem.getDcCreated();

    if (!StringSupport.isEmpty(resultItem.getDcTitle()))
      feed.setTitle(resultItem.getDcTitle());

    if (!StringSupport.isEmpty(resultItem.getDcAbstract()))
      feed.setDescription(resultItem.getDcAbstract());

    if (!StringSupport.isEmpty(resultItem.getDcCreator()))
      feed.addAuthor(new PersonImpl(resultItem.getDcCreator()));

    if (!StringSupport.isEmpty(resultItem.getDcContributor()))
      feed.addContributor(new PersonImpl(resultItem.getDcContributor()));

    if (!StringSupport.isEmpty(resultItem.getDcAccessRights()))
      feed.setCopyright(resultItem.getDcAccessRights());

    if (!StringSupport.isEmpty(resultItem.getDcLanguage()))
      feed.setLanguage(resultItem.getDcLanguage());

    feed.setUri(resultItem.getId());
    feed.addLink(new LinkImpl(getLinkForEntry(type, resultItem)));

    if (d != null)
      feed.setPublishedDate(d);

    // TODO: Finish cover support
    // Set the cover image
    // String coverUrl = null;
    // DistributionFormat coverFormat = mgr.getMediaFormat(DistributionFormat.FEED_COVER_IMAGE);
    // if (!StringSupport.isEmpty(resultItem.getCover()))
    // coverUrl = resultItem.getCover();
    // else if (coverFormat != null)
    // coverUrl = MediaFormatSupport.getCoverImageUrl(coverFormat, resultItem);
    // if (coverUrl != null)
    // feed.setImage(new ImageImpl(coverUrl, resultItem.getDcTitle()));

    return feed;
  }
  
  /**
   * Adds episode information to the feed.
   * 
   * @param type the feed type
   * @param query the query that results in the feed
   * @param resultItem the episodes item
   * @return the feed
   */
  protected Feed addEpisode(Feed.Type type, String[] query, SearchResultItem resultItem) {
    Date d = resultItem.getDcCreated();
    String link = getLinkForEntry(type, resultItem);
    String title = resultItem.getDcTitle();
    FeedEntry entry = createEntry(title, link, resultItem.getId());

    //
    // Add extension modules (itunes, dc, doi)

    // iTunes extension

    ITunesFeedEntryExtension iTunesEntry = new ITunesFeedEntryExtension();
    iTunesEntry.setDuration(resultItem.getDcExtent());
    // Additional iTunes properties
    iTunesEntry.setBlocked(false);
    iTunesEntry.setExplicit(false);
    // TODO: Add iTunes keywords and subtitles
    // iTunesEntry.setKeywords(keywords);
    // iTunesEntry.setSubtitle(subtitle);

    // DC extension

    DublinCoreExtension dcExtension = new DublinCoreExtension();
    // Additional dublin core properties
    dcExtension.setTitle(title);
    dcExtension.setIdentifier(resultItem.getId());

    // Set contributor
    if (!StringSupport.isEmpty(resultItem.getDcContributor())) {
      for (String contributor : resultItem.getDcContributor().split(";;")) {
        entry.addContributor(new PersonImpl(contributor));
        dcExtension.addContributor(contributor);
      }
    }

    // Set creator
    if (!StringSupport.isEmpty(resultItem.getDcCreator())) {
      for (String creator : resultItem.getDcCreator().split(";;")) {
        if (iTunesEntry.getAuthor() == null)
          iTunesEntry.setAuthor(creator);
        entry.addAuthor(new PersonImpl(creator));
        dcExtension.addCreator(creator);
      }
    }

    // Set publisher
    if (!StringSupport.isEmpty(resultItem.getDcPublisher())) {
      dcExtension.addPublisher(resultItem.getDcPublisher());
    }

    // Set rights
    if (!StringSupport.isEmpty(resultItem.getDcAccessRights())) {
      dcExtension.setRights(resultItem.getDcAccessRights());
    }

    // Set abstract
    if (!StringSupport.isEmpty(resultItem.getDcAbstract())) {
      String summary = resultItem.getDcAbstract();
      entry.setDescription(new ContentImpl(summary));
      iTunesEntry.setSummary(summary);
      dcExtension.setDescription(summary);
    }

    // Set the language
    if (!StringSupport.isEmpty(resultItem.getDcLanguage())) {
      dcExtension.setLanguage(resultItem.getDcLanguage());
    }

    // Set the publication date
    if (d != null) {
      entry.setPublishedDate(d);
      dcExtension.setDate(d);
    }

    // TODO: Finish dc support

    // Set format
    // if (!StringSupport.isEmpty(resultItem.getMediaType())) {
    // dcExtension.setFormat(resultItem.getMediaType());
    // }

    // dcEntry.setCoverage(arg0);
    // dcEntry.setRelation(arg0);
    // dcEntry.setSource(arg0);
    // dcEntry.setSubject(arg0);

    // Add the enclosures
    addEnclosures(entry, type, resultItem);

    // Set the cover image
    // String coverUrl = null;
    // DistributionFormat coverFormat = mgr.getMediaFormat(DistributionFormat.FEED_COVER_IMAGE);
    // if (!StringSupport.isEmpty(resultItem.getCover()))
    // coverUrl = resultItem.getCover();
    // else if (coverFormat != null)
    // coverUrl = MediaFormatSupport.getCoverImageUrl(coverFormat, resultItem);
    // if (coverUrl != null && coverFormat != null)
    // setImage(entry, coverUrl);

    extensions.add(iTunesEntry);
    extensions.add(dcExtension);
    entry.setExtensions(getExtensions(resultItem));

    // Add entry to feed
    feed.addEntry(entry);  
  
    return feed;
  }

  /**
   * Returns a list of enabled extensions for the given result item.
   * 
   * @param item
   *          the result item
   * @return the feed extensions
   */
  protected List<FeedExtension> getExtensions(SearchResultItem item) {
    return extensions;
  }

  /**
   * Creates a new feed entry that can be added to the feed.
   * 
   * @param title
   *          the entry title
   * @param link
   *          link to the orginal resource
   * @param uri
   *          the entry uri
   * @return the feed
   */
  protected FeedEntry createEntry(String title, String link, String uri) {
    return createEntry(title, null, link, uri);
  }

  /**
   * Creates a new feed entry that can be added to the feed.
   * 
   * @param title
   *          the entry title
   * @param description
   *          the entry description
   * @param link
   *          link to the orginal resource
   * @param uri
   *          the entry uri
   * @return the feed
   */
  protected FeedEntry createEntry(String title, String description, String link, String uri) {
    if (feed == null)
      throw new IllegalStateException("Feed must be created prior to creating feed entries");
    FeedEntryImpl entry = new FeedEntryImpl(feed, title, description, new LinkImpl(link), uri);
    return entry;
  }

  /**
   * Adds the image as a content element to the feed entry.
   * 
   * @param entry
   *          the feed entry
   * @param imageUrl
   *          the image url
   * @return the image
   */
  protected Content setImage(FeedEntry entry, String imageUrl) {
    StringBuffer buf = new StringBuffer("<div xmlns=\"http://www.w3.org/1999/xhtml\">");
    buf.append("<img src=\"");
    buf.append(imageUrl);
    buf.append("\" />");
    buf.append("</div>");
    Content image = new ContentImpl(buf.toString(), "application/xhtml+xml", Mode.Xml);
    entry.addContent(image);
    return image;
  }

  /**
   * Adds the enclosures to the feed entry. In case of an rss feed, where only one enclosure is supported, either the
   * one identified by <code>defaultFormat</code> or the first one in the format list is chosen.
   * 
   * @param entry
   *          the feed entry
   * @param resultItem
   *          the result item from the solr index
   * @param defaultFormat
   *          default distribution format (rss feeds only)
   * @return the list of formats that have been added
   */
  protected List<Track> addEnclosures(FeedEntry entry, Feed.Type type, SearchResultItem resultItem)
          throws IllegalStateException {
    MediaPackage mediaPackage = resultItem.getMediaPackage();
    List<Track> enclosedFormats = new ArrayList<Track>();

    // Assemble formats to add
    Set<String> trackIds = getTracksForEntry(type, resultItem);

    // Did we find any distribution formats?
    if (trackIds.size() == 0) {
      log_.warn("No media formats found for feed entry {}", entry);
      return enclosedFormats;
    }

    // Create enclosures
    for (String trackId : trackIds) {
      Track track = mediaPackage.getTrack(trackId);
      String trackUrl = null;
      try {
        trackUrl = track.getURI().toURL().toExternalForm();
        String trackMimeType = track.getMimeType().toString();
        long trackLength = track.getSize();
        Enclosure enclosure = new EnclosureImpl(trackUrl, trackMimeType, trackLength);
        entry.addEnclosure(enclosure);
        enclosedFormats.add(track);
      } catch (MalformedURLException e) {
        log_.error("Error converting {} to string", trackUrl, e);
      }
    }

    return enclosedFormats;
  }

  /**
   * Returns the identifier of those tracks that are to be included as enclosures in the feed. Note that for a feed type
   * of {@link Feed.Type#RSS}, the list must exactly contain one single entry.
   * <p>
   * This default implementation will include the track identified by the flavor as specified in the constructor for rss
   * feeds and every distribution track for atom feeds.
   * 
   * @param type
   *          the feed type
   * @param resultItem
   *          the result item
   * @return the set of identifier
   */
  protected Set<String> getTracksForEntry(Feed.Type type, SearchResultItem resultItem) {
    Set<String> s = new HashSet<String>();
    MediaPackage mediaPackage = resultItem.getMediaPackage();

    if (type.equals(Feed.Type.RSS)) {
      if (rssTrackFlavor != null) {
        Track[] tracks = mediaPackage.getTracks(rssTrackFlavor);
        if (tracks.length == 1)
          s.add(tracks[0].getIdentifier());
        else if (tracks.length == 0)
          log_.warn("No distributed media matching {} found for rss feed entry", rssTrackFlavor);
        else {
          log_.warn("More than one distributed media matching {} for rss feed entry found", rssTrackFlavor);
        }
      }
    } else {
      for (Track t : mediaPackage.getTracks()) {
        // TODO: Select distribution tracks, not only derived ones
        if (t.getReference() != null) {
          s.add(t.getIdentifier());
        }
      }
    }
    return s;
  }

  /**
   * Sets the url to the feed's homepage.
   * 
   * @param url
   *          the homepage
   */
  public void setFeedLink(String url) {
    this.feedHome = url;
  }

  /**
   * Returns the url to the feed's homepage.
   * 
   * @return the feed home
   */
  public String getFeedLink() {
    return feedHome;
  }

  /**
   * Sets the entry's base url that will be used to form the episode link in the feeds. If the url contains a
   * placeholder in the form <code>{0}</code>, it will be replaced by the episode id.
   * 
   * @param url
   *          the url
   */
  public void setLinkTemplate(String url) {
    linkTemplate = url;
  }

  /**
   * Returns the link template to the default user interface.
   * 
   * @return the link to the ui
   */
  public String getLinkTemplate() {
    return linkTemplate;
  }

  /**
   * Generates a link for the current feed entry by using the entry identifier and the result of
   * {@link #getLinkTemplate()} to create the url. Overwrite this method to provide your own way of generating links to
   * feed entries.
   * 
   * @param type
   *          the feed type
   * @param solrResultItem
   *          solr search result for this feed entry
   * @return the link to the ui
   */
  protected String getLinkForEntry(Feed.Type type, SearchResultItem solrResultItem) {
    if (linkTemplate == null)
      throw new IllegalStateException("No template defined");
    return MessageFormat.format(linkTemplate, solrResultItem.getId());
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    if (this.getName() != null)
      return getName();
    return super.toString();
  }

}
