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
package org.opencastproject.remotetest;

import junit.framework.Assert;

import static org.opencastproject.remotetest.AllRemoteTests.BASE_URL;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * Tests the functionality of the Engage Rest proxy
 * 
 */
public class EngageRestProxyTest {
  HttpClient client;

  @Before
  public void setup() throws Exception {
    client = new DefaultHttpClient();
  }

  @After
  public void teardown() throws Exception {
    client.getConnectionManager().shutdown();
  }

  @Test
  public void testDocs() throws Exception {
    HttpGet get = new HttpGet(BASE_URL + "/engageui/rest/docs");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testWadl() throws Exception {
    HttpGet get = new HttpGet(BASE_URL + "/engageui/rest/?_wadl");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testEpisode() throws Exception {
    HttpGet get = new HttpGet(BASE_URL + "/engageui/rest/episode?episodeId=ENGAGE/1");
    HttpResponse response = client.execute(get);
    // Ensure we get a 200 OK
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }
  
  @Test
  public void testSearch() throws Exception {
    HttpGet get = new HttpGet(BASE_URL + "/engageui/rest/search");
    HttpResponse response = client.execute(get);
    // Ensure we get a 200 OK
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }
}
