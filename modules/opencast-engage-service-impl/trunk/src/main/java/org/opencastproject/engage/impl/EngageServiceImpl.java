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
package org.opencastproject.engage.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.Dictionary;
import java.util.HashMap;

import org.apache.commons.io.IOUtils;
import org.opencastproject.engage.api.EngageService;
import org.opencastproject.engage.api.EpisodeView;
import org.opencastproject.engage.api.EpisodeViewImpl;
import org.opencastproject.engage.api.EpisodeViewListImpl;
import org.opencastproject.engage.api.EpisodeViewListResultImpl;
import org.opencastproject.media.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilder;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchResultItem;
import org.opencastproject.search.api.SearchService;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An EngageService implementation.
 */
public class EngageServiceImpl implements EngageService, ManagedService {

  /** Log facility */
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(EngageServiceImpl.class);

  /** The java io root directory */
  private String rootDirectory = System.getProperty("java.io.tmpdir");

  public static String engagePath = System.getProperty("java.io.tmpdir") + File.separator + "opencast" + File.separator
          + "workingfilerepo" + File.separator + "engage";
  public static String templatePath = "/player/templates";
  public static String playerTemplate = "player-jquery.html.tmpl";
  public static String browseTemplate = "search/browse.tmpl.html";
  public static String searchResultItemTemplate = "search/oc-search-result-item.tmpl.html";

  private SearchService searchService;

  private TemplateLoader tmplLoader;

  @SuppressWarnings("unchecked")
  public void updated(Dictionary props) throws ConfigurationException {
    // Update any configuration properties here
  }

  /**
   * Returns an HTML page with list of all available media packages in the search index.
   * 
   * @param limit
   * @param offset
   * @return HTML page
   */
  public String getEpisodesByDate(int limit, int offset) {
    StringBuilder sb = new StringBuilder();
    SearchResultItem[] result = searchService.getEpisodesByDate(0, offset).getItems();

    sb.append("<html>");

    sb.append("<h1>List of available Episodes Test</h1>");

    String title;
    // String playerTrackUrl;
    String mediaPackageId;
    for (int i = 0; i < result.length; i++) {
      title = result[i].getDcTitle();
      mediaPackageId = result[i].getId();
      // playerTrackUrl = result[i].getMediaPackage().getTrack("track-1").getURL().toExternalForm();
      sb.append("<a href=\"../watch/" + mediaPackageId + "\">" + title + "</a>" + "<br>");
    }

    sb.append("</html>");

    return sb.toString();
  }

  /**
   * Service activator, called via declarative services configuration.
   * 
   * @param componentContext
   *          the component context
   */
  public void activate(ComponentContext componentContext) {
    tmplLoader = new TemplateLoader(templatePath);
    String path = File.separator + "demo-data" + File.separator;

    addToSearchService(path, "manifest-demo1.xml", "dublincore-demo1.xml");
    addToSearchService(path, "manifest-demo2.xml", "dublincore-demo2.xml");
    addToSearchService(path, "manifest-demo3.xml", "dublincore-demo3.xml");
  }

  private void addToSearchService(String path, String manifestFile, String dublincoreFile) {
    String manifest = path + manifestFile;
    String dublincore = path + dublincoreFile;

    InputStream streamManifest = this.getClass().getResourceAsStream(manifest);
    InputStream streamDublincore = this.getClass().getResourceAsStream(dublincore);

    copyToTmpDir(streamManifest, manifestFile);
    copyToTmpDir(streamDublincore, dublincoreFile);

    MediaPackageBuilderFactory builderFactory = MediaPackageBuilderFactory.newInstance();
    MediaPackageBuilder mediaPackageBuilder = builderFactory.newMediaPackageBuilder();
    try {
      mediaPackageBuilder
              .setSerializer(new DefaultMediaPackageSerializerImpl(new File(rootDirectory + File.separator)));

      // Load the simple media package
      MediaPackage mediaPackage = null;
      InputStream is = this.getClass().getResourceAsStream(manifest);
      mediaPackage = mediaPackageBuilder.loadFromManifest(is);

      System.out.println("Length: " + mediaPackage.getTracks().length);

      // Add the media package to the search index
      searchService.add(mediaPackage);

    } catch (MediaPackageException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (MalformedURLException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
  }

  private void copyToTmpDir(InputStream stream, String path) {
    File f = new File(rootDirectory + File.separator + path);

    try {
      FileOutputStream fos = new FileOutputStream(f);
      IOUtils.copy(stream, fos);
      IOUtils.closeQuietly(stream);
      IOUtils.closeQuietly(fos);

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Service deactivator, called via declarative services configuration.
   * 
   * @param componentContext
   *          the component context
   */
  public void deactivate(ComponentContext componentContext) {
    searchService.delete("10.0000/1");
    searchService.delete("10.0000/2");
    searchService.delete("10.0000/3");
  }

  /**
   * Set SearchService called via declarative services configuration.
   * 
   * @param SearchService
   *          the search service instance
   */
  public void setSearchService(SearchService service) {
    this.searchService = service;
  }

  /**
   * Returns an HTML page with a player that plays the mediaPackageId.
   * 
   * @param episodeId
   * @return HTML page
   */
  public String deliverPlayer(String episodeId) {
    StringBuilder sb = new StringBuilder();
    sb.append("<html>");

    sb.append("MediaPackage ID: " + episodeId + "<br>");

    SearchResultItem[] result = searchService.getEpisodeById(episodeId).getItems();
    String title = null;
    String playerTrackUrl = null;
    // String mediaPackageId;
    for (int i = 0; i < result.length; i++) {
      title = result[i].getDcTitle();
      // mediaPackageId = result[i].getId();
     // playerTrackUrl = result[i].getMediaPackage().getTrack("track-1").getURI().toString();
      playerTrackUrl = "http://vs1.rz.uni-osnabrueck.de/public/virtmm/opencast/car.flv";
    }

    if (title != null)
      sb.append("Title: " + title + "<br>");

    String template = tmplLoader.loadTemplate(playerTemplate);
    HashMap<String, String> map = new HashMap<String, String>();
    if (playerTrackUrl != null) {
      map.put("videoURL", playerTrackUrl);
      sb.append("track-1: " + playerTrackUrl + "<br>");
    }

    sb.append(tmplLoader.doReplacements(template, map));

    sb.append("</html>");

    return sb.toString();
  }

  public String deliverBrowsePage() {
    String browsePage = tmplLoader.loadTemplate(browseTemplate);
    String searchResultItemTmpl = tmplLoader.loadTemplate(searchResultItemTemplate);

    SearchResult searchResult = searchService.getEpisodesByDate(0, 10);

    SearchResultItem[] result = searchService.getEpisodesByDate(0, 10).getItems();

    StringBuilder searchResultItems = new StringBuilder();
    for (int i = 0; i < result.length; i++) {
      searchResultItems.append(fillSearchResultItem(searchResultItemTmpl, result[i]));
    }

    HashMap<String, String> map = new HashMap<String, String>();
    map.put("oc-search-result-items", searchResultItems.toString());
    map.put("oc-search-result-items-number", "" + result.length);
    map.put("oc-search-result-items-max", "" + result.length);

    return tmplLoader.doReplacements(browsePage, map);
  }

  private String fillSearchResultItem(String searchResultItemTmpl, SearchResultItem s) {
    String title = s.getDcTitle();
    // String mediaPackageId = s.getId();

    HashMap<String, String> map = new HashMap<String, String>();
    map.put("oc-search-result-item-title", title);

    return tmplLoader.doReplacements(searchResultItemTmpl, map);
  }

  /**
   * Returns a EpisodeViewList episodes. If the search service is not present an empty list is returned.
   * 
   * @return EpisodeViewList list of episodes
   */
  public EpisodeViewListResultImpl getEpisodesByDate(int page) {
    // Variables
    int pagemax, episodesMax, fromIndex, toIndex;
    String mediaPackageId, title, videoURL;
    EpisodeViewListResultImpl episodeViewListResult;
    EpisodeViewListImpl episodeViewList;
    EpisodeView episodeViewItem;

    // Creating a new EpisodeViewListResultImpl
    episodeViewListResult = new EpisodeViewListResultImpl();

    // Creating a new EpisodeViewListImpl
    episodeViewList = new EpisodeViewListImpl();

    // Adding episodeViewList to episodeViewListResult
    episodeViewListResult.setEpisodeViewList(episodeViewList);

    // Checking if the searchService is available
    if (searchService == null) {
      logger.warn("search service not present, returning empty list");
      return episodeViewListResult;
    }

    // Get episodes by date from the search service
    SearchResultItem[] searchResultItems = searchService.getEpisodesByDate(0, 52).getItems();

    // Set episodesMax
    episodesMax = searchResultItems.length;

    // Set the episodes max
    episodeViewListResult.setEpisodesMax(episodesMax);

    // Calculate the page maximum
    pagemax = searchResultItems.length / 10;

    if (searchResultItems.length % 10 != 0)
      pagemax = pagemax + 1;
    else if (pagemax == 0)
      pagemax = 1;

    // Set the page maximum
    episodeViewListResult.setPageMax(pagemax);

    // Calculate fromIndex
    fromIndex = (page * 10) - 10;

    // Set the fromIndex
    episodeViewListResult.setFromIndex(fromIndex + 1);

    // Calculate toIndex
    toIndex = Math.min(fromIndex + 10, searchResultItems.length);

    // Set the toIndex
    episodeViewListResult.setToIndex(toIndex);

    for (int i = fromIndex; i < toIndex; i++) {
      // Get the the sarchResultItem on position i in the array
      SearchResultItem searchResultItem = searchResultItems[i];

      // Getting fields from the searchResultItem
      mediaPackageId = searchResultItem.getId();
      title = searchResultItem.getDcTitle();
      //videoURL = searchResultItem.getMediaPackage().getTrack("track-1").getURI().toString();
      videoURL = "http://vs1.rz.uni-osnabrueck.de/public/virtmm/opencast/car.flv";

      try {
        // URLEncode the media package id
        mediaPackageId = URLEncoder.encode(mediaPackageId, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }

      // Setting fields to the episodeViewItem
      episodeViewItem = new EpisodeViewImpl();
      episodeViewItem.setTitle(title);
      episodeViewItem.setURLEncodedMediaPackageId(mediaPackageId);

      // Adding the episodeViewItem to the episodeViewList
      episodeViewList.add(episodeViewItem);
    }

    return episodeViewListResult;
  }

}
