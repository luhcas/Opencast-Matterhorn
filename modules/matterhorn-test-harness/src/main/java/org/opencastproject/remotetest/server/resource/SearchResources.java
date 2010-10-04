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
package org.opencastproject.remotetest.server.resource;

import org.opencastproject.remotetest.Main;

import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

/**
 * Search REST resources
 */

public class SearchResources {

  private static final String getServiceUrl() {
    return Main.getBaseUrl() + "/search/rest/";
  }
  
  public static HttpResponse add(String mediapackage) throws Exception {
    HttpPost post = new HttpPost(getServiceUrl() + "add");
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("mediapackage", mediapackage));
    post.setEntity(new UrlEncodedFormEntity(params));
    return Main.getClient().execute(post);
  }
  
  // TODO add remaining query parameters (episode and series)
  public static HttpResponse episode(String id) throws Exception {
    return Main.getClient().execute(new HttpGet(getServiceUrl() + "episode?id=" + id));
  }
  
  public static HttpResponse all(String q) throws Exception {
    return Main.getClient().execute(new HttpGet(getServiceUrl() + "episode?q=" + q));
  }
  
  public static HttpResponse series(String id) throws Exception {
    return Main.getClient().execute(new HttpGet(getServiceUrl() + "series?id=" + id));
  }
}
