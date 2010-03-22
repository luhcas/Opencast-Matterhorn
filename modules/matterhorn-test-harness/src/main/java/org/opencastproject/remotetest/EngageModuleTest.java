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

import static org.opencastproject.remotetest.AllRemoteTests.BASE_URL;

import org.opencastproject.demo.DemodataLoader;
import org.opencastproject.integrationtest.AuthenticationSupport;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

/**
 * Tests the functionality of the Engage module Tests if all media player components are available
 * 
 * This needs to be improved by String[] EngageGFXuri = { "/engage-hybrid-player/icons/cc_off.png",
 * "/engage-hybrid-player/icons/cc_on.png", .... }
 * 
 * String[] EngageJSuri = { ... }
 * 
 * to remove many Testcases
 * 
 * The DefaultHttpClient needs to be threadsafe - included in org.apache.httpcomponents version 4-1alpha
 * 
 */
public class EngageModuleTest {
  HttpClient client;

  private static DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
  private static XPathFactory factory = XPathFactory.newInstance();

  public static String ENGAGE_BASE_URL = BASE_URL + "/engage/ui";

  private void clearSearchIndex() throws Exception {
    HttpClient client = new DefaultHttpClient();
    HttpPost post = new HttpPost(BASE_URL + "/search/rest/clear");

    List<NameValuePair> formParams = new ArrayList<NameValuePair>();
    post.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));

    client.execute(post);
  }

  @Before
  public void setup() throws Exception {
    client = new DefaultHttpClient();

    domFactory = DocumentBuilderFactory.newInstance();
    domFactory.setNamespaceAware(false); // don't forget this!
  }

  @After
  public void teardown() throws Exception {
    client.getConnectionManager().shutdown();
  }

  @Test
  public void testContainsEngageTrack() throws Exception {
    // Clear the search index. Remove all media packages.
    clearSearchIndex();

    // Load the data
    String[] args = { "-n", "1" };
    DemodataLoader.main(args);

    // Ensure that the data loading finishes successfully
    int attempts = 0;
    boolean success = false;
    while (true) {
      if (++attempts == 20)
        Assert.fail("search rest endpoint test has hung");

      HttpClient client = new DefaultHttpClient();
      HttpGet get = new HttpGet(BASE_URL + "/search/rest/episode?limit=1&offset=0");
      String getResponse = EntityUtils.toString(client.execute(get).getEntity());

      DocumentBuilder builder = domFactory.newDocumentBuilder();
      Document doc = builder.parse(IOUtils.toInputStream(getResponse));

      XPath xpath = factory.newXPath();

      // Test if the media package contains a track with the tag 'engage'
      XPathExpression expr = xpath.compile("/search-results/result/mediapackage/media/track/tags/tag[.='engage']");

      NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

      success = (nodes.getLength() == 1);

      if (success) {
        Assert.assertTrue(true);  // FIXME this isn't necessary
        break;
      }
      Thread.sleep(2000);
    }
  }

  @Test
  public void testJQuery() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/jquery/jquery/jquery-1.3.2.js");
    AuthenticationSupport.addAuthentication(get);
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testJQueryXSLT() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/jquery/plugins/jquery.xslt.js");
    AuthenticationSupport.addAuthentication(get);
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testEngageUI() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/js/engage-ui.js");
    AuthenticationSupport.addAuthentication(get);
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }


  @Test
  public void testFABridge() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/bridge/lib/FABridge.js");
    AuthenticationSupport.addAuthentication(get);
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testVideodisplay() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/bridge/Videodisplay.js");
    AuthenticationSupport.addAuthentication(get);
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testjARIA() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/jquery/js/jARIA.js");
    AuthenticationSupport.addAuthentication(get);
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testFluid() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/fluid/js/Fluid.js");
    AuthenticationSupport.addAuthentication(get);
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testInlineEdit() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/fluid/js/InlineEdit.js");
    AuthenticationSupport.addAuthentication(get);
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testJQueryCore() throws Exception {
    HttpGet get = new HttpGet(ENGAGE_BASE_URL + "/engage-hybrid-player/jquery/ui/ui.core.js");
    AuthenticationSupport.addAuthentication(get);
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }
}
