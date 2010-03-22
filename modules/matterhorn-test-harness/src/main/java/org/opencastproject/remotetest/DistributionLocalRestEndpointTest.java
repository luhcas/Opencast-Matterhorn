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
package org.opencastproject.remotetest;

import org.opencastproject.integrationtest.AuthenticationSupport;

import junit.framework.Assert;

import static org.opencastproject.remotetest.AllRemoteTests.BASE_URL;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the functionality of the local distribution service rest endpoint
 */
public class DistributionLocalRestEndpointTest {
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
    HttpGet get = new HttpGet(BASE_URL + "/distribution/local/rest/docs");
    AuthenticationSupport.addAuthentication(get);
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testWadl() throws Exception {
    HttpGet get = new HttpGet(BASE_URL + "/distribution/local/rest/?_wadl");
    AuthenticationSupport.addAuthentication(get);
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testDistribute() throws Exception {
    HttpPost post = new HttpPost(BASE_URL + "/distribution/local/rest/");
    AuthenticationSupport.addAuthentication(post);
    List<NameValuePair> formParams = new ArrayList<NameValuePair>();

    formParams.add(new BasicNameValuePair("mediapackage", getSampleMediaPackage()));
    formParams.add(new BasicNameValuePair("elementId", "track-1"));
    formParams.add(new BasicNameValuePair("elementId", "catalog-1"));
    post.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));

    // Ensure we get a 200 OK (we don't really care about the body of the result)
    HttpResponse response = client.execute(post);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    
    // Ideally, we would check to see that the files have been copied to the right location, too, but we can't assume
    // that the server is local.  So we'll just have to trust that our unit tests cover that.
  }

  protected String getSampleMediaPackage() throws Exception {
    String template = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("mediapackage-1.xml"));
    return template.replaceAll("@SAMPLES_URL@", BASE_URL + "/workflow/samples");
  }
}
