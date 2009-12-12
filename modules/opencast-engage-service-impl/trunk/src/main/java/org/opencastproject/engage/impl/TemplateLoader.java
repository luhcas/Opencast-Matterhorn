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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;

public class TemplateLoader {
  private String templatePath;
  
  public TemplateLoader(String templatePath){
    this.templatePath = templatePath;
  }
  
  /**
   * Returns a String with the content of the reuqested resource.
   * 
   * @param templateName
   * @return String
   */
  public String loadTemplate(String templateName) {
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
   * Replaces occurences of keys in String with values.
   * 
   * @param subject
   * @param replacementMap
   * @return String with replacements
   */
  public String doReplacements(String subject, HashMap<String, String> replacementMap) {
    Iterator<String> i = replacementMap.keySet().iterator();
    String out = subject; // just for the case that replacementMap is empty
    while (i.hasNext()) {
      String currentKey = i.next();
      out = out.replaceAll("<:" + currentKey + ":>", (String) replacementMap.get(currentKey)); // FIXME use entrySet()
    }
    return out;
  }
}
