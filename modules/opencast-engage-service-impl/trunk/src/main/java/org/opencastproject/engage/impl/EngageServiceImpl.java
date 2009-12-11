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
import java.net.MalformedURLException;
import java.util.Dictionary;
import java.util.HashMap;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.opencastproject.engage.api.EngageService;
import org.opencastproject.media.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilder;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageException;
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
  public static String playerTemplate = "player.tmpl.html";
  public static String browseTemplate = "search/browse.tmpl.html";
  public static String searchHeaderTemplate = "search/oc-search-header.tmpl.html";
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

    addToSearchService(path, "manifest-demo01.xml", "dublincore-demo01.xml");
    addToSearchService(path, "manifest-demo02.xml", "dublincore-demo02.xml");
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
    searchService.delete("10.0000/01");
    searchService.delete("10.0000/02");
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
  public String deliverPlayer(String mediaPackageIdBase64Encoded) {

    // Variables
    String mediaPackageId;

    // Base64 Decode media package id
    mediaPackageId = new String(Base64.decodeBase64(mediaPackageIdBase64Encoded.getBytes()));

    StringBuilder sb = new StringBuilder();

    SearchResultItem[] result = searchService.getEpisodeById(mediaPackageId).getItems();
    String title = "no title";
    String playerTrackUrl = null;
    // String mediaPackageId;
    for (int i = 0; i < result.length; i++) {
      title = result[i].getDcTitle();
      // mediaPackageId = result[i].getId();
      playerTrackUrl = result[i].getMediaPackage().getTrack("track-1").getURI().toString();
    }

    String searchHeaderTmpl = tmplLoader.loadTemplate(searchHeaderTemplate);
    String template = tmplLoader.loadTemplate(playerTemplate);
    HashMap<String, String> map = new HashMap<String, String>();
    if (playerTrackUrl != null) {
      map.put("videoURL", playerTrackUrl);
      map.put("oc-episode-title", title);
      map.put("oc-engage-search-header", searchHeaderTmpl);
    }

    sb.append(tmplLoader.doReplacements(template, map));

    return sb.toString();
  }

  public String deliverBrowsePage() {
    // Variables
    String browsePage = tmplLoader.loadTemplate(browseTemplate);
    String searchResultItemTmpl = tmplLoader.loadTemplate(searchResultItemTemplate);

    // Get the search result from the search service
    SearchResultItem[] result = searchService.getEpisodesByDate(0, 30).getItems();

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

    // Variables
    String title;
    String mediaPackageId;
    HashMap<String, String> map;

    // Get the media package id
    mediaPackageId = s.getId();

    // Base64 Encode media package id
    String mediaPackageIdEncoded = new String(Base64.encodeBase64(mediaPackageId.getBytes()));

    // Get the media package title
    title = s.getDcTitle();

    // Create a map to put into the strings that will be replaced inside of the template
    map = new HashMap<String, String>();
    map.put("oc-search-result-item-title", "<a href=\"watch/" + mediaPackageIdEncoded + "\">" + title + "</a>" + "\n");

    return tmplLoader.doReplacements(searchResultItemTmpl, map);

  }
}
