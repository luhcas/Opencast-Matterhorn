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
package org.opencastproject.workflow.endpoint;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

/**
 * See this JAXB bug for the full explanation: https://jaxb.dev.java.net/issues/show_bug.cgi?id=223
 */
public class LocalHashMap {
  protected Map<String, String> map = new HashMap<String, String>();
  public Map<String, String> getMap() {return map;}
  
  public LocalHashMap(String in) {
    Properties properties = new Properties();
    try {
      properties.load(IOUtils.toInputStream(in));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    for(Entry<Object, Object> e : properties.entrySet()) {
      map.put((String)e.getKey(), (String)e.getValue());
    }
  }
}
