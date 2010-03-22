/**
 *  Copyright 2009, 2010 The Regents of the University of California
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
package org.opencastproject.integrationtest;

import org.apache.http.message.AbstractHttpMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Adds authentication headers to http request.
 */
public class AuthenticationSupport {
  public static Map<String, String> getHeaders() {
    Map<String, String> map = new HashMap<String, String>();
    map.put("Authorization", "Basic YWRtaW46b3BlbmNhc3Q=");
    return map;
  }
  
  public static void addAuthentication(AbstractHttpMessage message) {
    for(Entry<String, String> header : AuthenticationSupport.getHeaders().entrySet()) {
      message.addHeader(header.getKey(), header.getValue());
    }
  }
}
