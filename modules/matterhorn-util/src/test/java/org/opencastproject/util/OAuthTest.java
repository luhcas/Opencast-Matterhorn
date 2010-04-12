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
package org.opencastproject.util;

import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.net.HttpURLConnection;
import java.net.URL;

@Ignore
public class OAuthTest {
  HttpClient httpClient;
  CommonsHttpOAuthConsumer oauthConsumer;
  
  @Before
  public void setup() throws Exception {
    httpClient = new DefaultHttpClient();
    oauthConsumer = new CommonsHttpOAuthConsumer("org.opencastproject.oauth.trusted.consumer", "CHANGE_ME_secret_CHANGE_ME");
  }

  @After
  public void teardown() throws Exception {
    httpClient.getConnectionManager().shutdown();
  }
  
  @Test
  public void testSignedFetch() throws Exception {
    HttpGet get = new HttpGet("http://localhost:8080/welcome.html");
    oauthConsumer.sign(get);
    HttpResponse response = httpClient.execute(get);
    Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testUnSignedFetch() throws Exception {
    URL url = new URL("http://localhost:8080/welcome.html");
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setInstanceFollowRedirects(false);
    Assert.assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, conn.getResponseCode());
    conn.disconnect();
  }

}
