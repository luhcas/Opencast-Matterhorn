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
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
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
public class WorkflowRestEndpointTest {
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
  public void testStartAndRetrieveWorkflowInstance() throws Exception {
    // Start a workflow instance via the rest endpoint
    HttpPost postStart = new HttpPost(BASE_URL + "/workflow/rest/start");
    List<NameValuePair> formParams = new ArrayList<NameValuePair>();

    formParams.add(new BasicNameValuePair("definition", getSampleWorkflowDefinition()));
    formParams.add(new BasicNameValuePair("mediapackage", getSampleMediaPackage()));
    formParams.add(new BasicNameValuePair("properties", "this=that"));
    postStart.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));

    // Grab the new workflow instance from the response
    String postResponse = EntityUtils.toString(client.execute(postStart).getEntity());
    String id = getWorkflowInstanceId(postResponse);

    // Ensure we can retrieve the workflow instance from the rest endpoint
    HttpGet getWorkflowMethod = new HttpGet(BASE_URL + "/workflow/rest/instance/" + id + ".xml");
    String getResponse = EntityUtils.toString(client.execute(getWorkflowMethod).getEntity());
    Assert.assertEquals(id, getWorkflowInstanceId(getResponse));
    
    // Make sure we can retrieve it via json, too
    HttpGet getWorkflowJson = new HttpGet(BASE_URL + "/workflow/rest/instance/" + id + ".json");
    String jsonResponse = EntityUtils.toString(client.execute(getWorkflowJson).getEntity());
    JSONObject json = (JSONObject) JSONValue.parse(jsonResponse);
    Assert.assertEquals(id, json.get("workflow_id"));
  }

  protected String getWorkflowInstanceId(String xml) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(IOUtils.toInputStream(xml));
    return ((Element)XPathFactory.newInstance().newXPath().compile("/*").evaluate(doc, XPathConstants.NODE)).getAttribute("id");
  }

  protected String getSampleMediaPackage() throws Exception {
    String template = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("mediapackage-1.xml"));
    return template.replaceAll("@SAMPLES_URL@", BASE_URL + "/workflow/samples");
  }

  protected String getSampleWorkflowDefinition() throws Exception {
    return IOUtils.toString(getClass().getClassLoader().getResourceAsStream("workflow-definition-1.xml"));
  }
}
