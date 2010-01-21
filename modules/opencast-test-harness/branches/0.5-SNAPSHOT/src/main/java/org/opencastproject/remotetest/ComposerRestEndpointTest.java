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

import static org.opencastproject.remotetest.AllRemoteTests.BASE_URL;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

/**
 * Tests the functionality of a remote workflow service rest endpoint
 */
public class ComposerRestEndpointTest {
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
  public void testEncodeAudioAndVideoTracks() throws Exception {
    // Start an encoding job via the rest endpoint
    HttpPost postEncode = new HttpPost(BASE_URL + "/composer/rest/encode");
    List<NameValuePair> formParams = new ArrayList<NameValuePair>();
    formParams.add(new BasicNameValuePair("audioSourceTrackId", "track-1"));
    formParams.add(new BasicNameValuePair("videoSourceTrackId", "track-2"));
    formParams.add(new BasicNameValuePair("profileId", "flash.http"));
    formParams.add(new BasicNameValuePair("mediapackage", getSampleMediaPackage()));
    postEncode.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));

    // Grab the receipt from the response
    HttpResponse postResponse = client.execute(postEncode);
    Assert.assertEquals(200, postResponse.getStatusLine().getStatusCode());
    String postResponseXml = EntityUtils.toString(postResponse.getEntity());
    String receiptId = getReceiptId(postResponseXml);
    
    // Poll the service for the status of the receipt.
    String status = null;
    while(status == null || "RUNNING".equals(status)) {
      Thread.sleep(5000); // wait and try again
      HttpGet pollRequest = new HttpGet(BASE_URL + "/composer/rest/receipt/" + receiptId + ".xml");
      status = getRecepitStatus(client.execute(pollRequest));
      System.out.println("encoding job " + receiptId + " is " + status);
    }
    if( ! "FINISHED".equals(status)) {
      Assert.fail("receipt status terminated with status=" + status);
    }
  }

  @Test
  public void testImageExtraction() throws Exception {
    HttpPost postEncode = new HttpPost(BASE_URL + "/composer/rest/image");
    List<NameValuePair> formParams = new ArrayList<NameValuePair>();
    formParams.add(new BasicNameValuePair("sourceTrackId", "track-2"));
    formParams.add(new BasicNameValuePair("time", "1"));
    formParams.add(new BasicNameValuePair("profileId", "feed-image.http"));
    formParams.add(new BasicNameValuePair("mediapackage", getSampleMediaPackage()));
    postEncode.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));

    // Grab the attachment from the response
    HttpResponse postResponse = client.execute(postEncode);
    String postResponseXml = EntityUtils.toString(postResponse.getEntity());
    System.out.println(postResponseXml);
    Assert.assertEquals(200, postResponse.getStatusLine().getStatusCode());
  }

  protected String getSampleMediaPackage() throws Exception {
    String template = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("mediapackage-2.xml"));
    return template.replaceAll("@SAMPLES_URL@", BASE_URL + "/workflow/samples");
  }
  
  protected String getReceiptId(String xml) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(IOUtils.toInputStream(xml));
    return ((Element)XPathFactory.newInstance().newXPath().compile("/*").evaluate(doc, XPathConstants.NODE)).getAttribute("id");
  }

  protected String getRecepitStatus(HttpResponse pollResponse) throws Exception {
    String xml = EntityUtils.toString(pollResponse.getEntity());
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(IOUtils.toInputStream(xml));
    String status = ((Element)XPathFactory.newInstance().newXPath().compile("/*").evaluate(doc, XPathConstants.NODE)).getAttribute("status");
    return status;
  }

}
