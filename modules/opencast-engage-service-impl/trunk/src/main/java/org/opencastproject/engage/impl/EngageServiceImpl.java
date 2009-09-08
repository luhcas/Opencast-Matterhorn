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

import org.opencastproject.engage.api.EngageService;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;

/**
 * FIXME -- Add javadocs
 */
public class EngageServiceImpl implements EngageService, ManagedService {
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(EngageServiceImpl.class);
  
  public static String engagePath = "/tmp/matterhorn/workingfilerepo/engage";
  public static String templatePath = "/player/templates";
  public static String playerTemplate = "player-download.html.tmpl";
  public static String mediaList = "availablemedia.html.tmpl";
  
  @SuppressWarnings("unchecked")
  public void updated(Dictionary props) throws ConfigurationException {
    // Update any configuration properties here
  }

  /** 
   * Returns an HTML page with a player that plays filename.
   * @param filename
   * @return HTML page
   */
  @SuppressWarnings("unchecked")
  public String deliverPlayer(String filename, String mediaHost) {
    if (!checkFilename(filename)) {
      throw new RuntimeException("File not found " + filename);
    }
    String template = loadTemplate(playerTemplate);
    HashMap<String,String> map = new HashMap();
    map.put("videoURL", "http://" + mediaHost + "/files/engage/" + filename);
    return doReplacements(template, map);
  }

  /**
   * Returns an HTML page that lists all available mediafiles.
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
    HashMap<String,String> map = new HashMap();
    map.put("fileList", sb.toString());
    return doReplacements(template, map);
  }

  /**
   * Returns true if filename if valid and file exists, false otherwise.
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
   * @param subject
   * @param replacementMap
   * @return String with replacements
   */
  private String doReplacements(String subject, HashMap<String,String> replacementMap) {
    Iterator<String> i = replacementMap.keySet().iterator();
    String out = new String(subject);   // jus for the case that replacementMap is empty
    while (i.hasNext()) {
      String currentKey = i.next();
      out = subject.replaceAll("<:"+currentKey+":>", (String)replacementMap.get(currentKey));
    }
    return out;
  }
  
  /**
   * Returns a String with the content of the reuqested resource.
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
      while ((line=in.readLine()) != null) {
        sb.append(line);
      }
      in.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return sb.toString();
  }
}

