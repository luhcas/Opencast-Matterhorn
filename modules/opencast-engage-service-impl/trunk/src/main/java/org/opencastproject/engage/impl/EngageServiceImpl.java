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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;

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
  public static String playerTemplate = "player-jquery.html.tmpl";
  public static String mediaList = "availablemedia.html.tmpl";

  private SearchService searchService;

  @SuppressWarnings("unchecked")
  public void updated(Dictionary props) throws ConfigurationException {
    // Update any configuration properties here
  }

  /**
   * Returns an HTML page with a player that plays filename.
   * 
   * @param filename
   * @return HTML page
   */
  @SuppressWarnings("unchecked")
  public String deliverPlayer(String filename, String mediaHost) {
    if (!checkFilename(filename)) {
      throw new RuntimeException("File not found " + filename);
    }
    String template = loadTemplate(playerTemplate);
    HashMap<String, String> map = new HashMap();
    map.put("videoURL", "http://" + mediaHost + "/files/engage/" + filename);
    return doReplacements(template, map);
  }
  /**
   * Returns an HTML page that lists all available mediafiles.
   * 
   * @return HTML page
   */
  @SuppressWarnings("unchecked")
  public String listRecordings() {
    StringBuilder sb = new StringBuilder();
    File dir = new File(engagePath);
    File[] files = dir.listFiles();
    for (int i = 0; i < files.length; i++) {
      sb.append("<a href=\"/engage/play/" + files[i].getName() + "\">" + files[i].getName() + "</a><br>\n");
    }
    String template = loadTemplate(mediaList);
    HashMap<String, String> map = new HashMap();
    map.put("fileList", sb.toString());
    return doReplacements(template, map);
  }

  /**
   * Returns true if filename if valid and file exists, false otherwise.
   * 
   * @param filename
   * @return
   */
  private boolean checkFilename(String filename) {
    // FIXME do regexp check if filename is valid (secure)
    File file = new File(engagePath + File.separator + filename);
    return file.exists();
  }

  /**
   * Replaces occurences of keys in String with values.
   * 
   * @param subject
   * @param replacementMap
   * @return String with replacements
   */
  private String doReplacements(String subject, HashMap<String, String> replacementMap) {
    Iterator<String> i = replacementMap.keySet().iterator();
    String out = new String(subject); // jus for the case that replacementMap is empty
    while (i.hasNext()) {
      String currentKey = i.next();
      out = subject.replaceAll("<:" + currentKey + ":>", (String) replacementMap.get(currentKey));
    }
    return out;
  }

  /**
   * Returns a String with the content of the requested resource.
   * 
   * @param templateName
   * @return String
   */
  private String loadTemplate(String templateName) {
    InputStream resc = this.getClass().getResourceAsStream(templatePath + "/" + templateName);
    if (resc == null) {
      throw new RuntimeException("Unable the read template " + templatePath + "/" + templateName);
    }
    BufferedReader in = new BufferedReader(new InputStreamReader(resc));
    StringBuilder sb = new StringBuilder();
    String line = null;
    try {
      while ((line = in.readLine()) != null) {
        sb.append(line);
      }
      in.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return sb.toString();
  }

  /** 
   * Returns an HTML page with list of all available media packages in the search index.
   * @param limit
   * @param offset
   * @return HTML page
   */
  public String getEpisodesByDate(int limit, int offset) {
    StringBuilder sb = new StringBuilder();
    SearchResultItem[] result = searchService.getEpisodesByDate(0, offset).getItems();
    
    sb.append("<html>");
    
    sb.append("<h1>List of available Episodes</h1>");
    
    
    String title;
 //   String playerTrackUrl;
    String mediaPackageId;
    for(int i=0; i<result.length; i++)
    {
      title = result[i].getDcTitle();
      mediaPackageId = result[i].getId();
 //   playerTrackUrl = result[i].getMediaPackage().getTrack("track-1").getURL().toExternalForm();
      sb.append("<a href=\"../watch/"+mediaPackageId+"\">"+ title +"</a>"+ "<br>");
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
    String path = File.separator + "demo-data" + File.separator;
    
    addToSearchService(path, "manifest-demo1.xml", "dublincore-demo1.xml");
    addToSearchService(path, "manifest-demo2.xml", "dublincore-demo2.xml");
    addToSearchService(path, "manifest-demo3.xml", "dublincore-demo3.xml");
  }

  private void addToSearchService(String path, String manifestFile, String dublincoreFile)
  {
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
   * @param episodeId
   * @return HTML page
   */
  public String deliverPlayer(String episodeId) {
    StringBuilder sb = new StringBuilder();
    sb.append("<html>");
    
    sb.append("MediaPackage ID: "+episodeId+ "<br>");
    
    SearchResultItem[] result = searchService.getEpisodeById(episodeId).getItems();
    String title = null;
    String playerTrackUrl = null;
 //   String mediaPackageId;
    for(int i=0; i<result.length; i++)
    {
      title = result[i].getDcTitle();
  //    mediaPackageId = result[i].getId();
      playerTrackUrl = result[i].getMediaPackage().getTrack("track-1").getURL().toExternalForm();
    }
    
    if(title != null)
      sb.append("Title: "+title+ "<br>");
    
    String template = loadTemplate(playerTemplate);
    HashMap<String, String> map = new HashMap();
    if(playerTrackUrl != null)
    {
      map.put("videoURL", playerTrackUrl);
      sb.append("track-1: "+playerTrackUrl+ "<br>");
    }
    
   
    sb.append(doReplacements(template, map));
    
    sb.append("</html>");
    
    return sb.toString();
  }
}
