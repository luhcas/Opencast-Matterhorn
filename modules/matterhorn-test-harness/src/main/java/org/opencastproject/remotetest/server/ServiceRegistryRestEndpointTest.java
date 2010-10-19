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
package org.opencastproject.remotetest.server;

import static org.opencastproject.remotetest.Main.BASE_URL;
import static org.opencastproject.remotetest.Main.PASSWORD;
import static org.opencastproject.remotetest.Main.USERNAME;

import java.io.InputStream;
import java.net.URLEncoder;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;

import junit.framework.Assert;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opencastproject.remotetest.security.TrustedHttpClientImpl;
import org.opencastproject.remotetest.util.Utils;
import org.w3c.dom.Document;

/**
 * Tests the working file repository's rest endpoint
 */
public class ServiceRegistryRestEndpointTest {
  String serviceUrl = null;
  
  String remoteHost = null;
  TrustedHttpClientImpl client;

  @Before
  public void setup() throws Exception {
    remoteHost = URLEncoder.encode(BASE_URL, "UTF-8");
    serviceUrl = BASE_URL + "/services/rest";
    client = new TrustedHttpClientImpl(USERNAME, PASSWORD);
  }

  @After
  public void teardown() throws Exception {
  }

  @Test
  public void testGetServiceRegistrations() throws Exception {
    // Get a known service registration as xml
    HttpGet get = new HttpGet(serviceUrl + "/services.xml?serviceType=org.opencastproject.composer&host=" + remoteHost);
    HttpResponse response = client.execute(get);
    Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
    InputStream in = response.getEntity().getContent();
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
    String typeFromResponse = (String)Utils.xPath(doc, "//type", XPathConstants.STRING);
    Assert.assertEquals("org.opencastproject.composer", typeFromResponse);
    String hostFromResponse = (String)Utils.xPath(doc, "//host", XPathConstants.STRING);
    Assert.assertEquals(BASE_URL, hostFromResponse);
    
    // Get a registration that is known to not exist, and ensure we get a 404
    get = new HttpGet(serviceUrl + "/services.xml?serviceType=foo&host=" + remoteHost);
    response = client.execute(get);
    Assert.assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
    
    // Get all services on a single host
    get = new HttpGet(serviceUrl + "/services.xml?host=" + remoteHost);
    response = client.execute(get);
    Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
    in = response.getEntity().getContent();
    doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
    int serviceCount = ((Number)Utils.xPath(doc, "count(//service)", XPathConstants.NUMBER)).intValue();
    Assert.assertTrue(serviceCount > 0);

    // Get all services of a single type
    get = new HttpGet(serviceUrl + "/services.xml?serviceType=org.opencastproject.composer");
    response = client.execute(get);
    Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
    in = response.getEntity().getContent();
    doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
    serviceCount = ((Number)Utils.xPath(doc, "count(//service)", XPathConstants.NUMBER)).intValue();
    Assert.assertEquals(1, serviceCount);
    
    // Get statistics
    get = new HttpGet(serviceUrl + "/statistics.xml");
    response = client.execute(get);
    Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
    in = response.getEntity().getContent();
    doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
    serviceCount = ((Number)Utils.xPath(doc, "count(//service)", XPathConstants.NUMBER)).intValue();
    Assert.assertTrue(serviceCount > 0);
  }
}
