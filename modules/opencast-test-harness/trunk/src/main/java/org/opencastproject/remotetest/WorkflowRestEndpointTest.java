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

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
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
  protected String baseUrl;
  
  public WorkflowRestEndpointTest(String baseUrl) {
    this.baseUrl = baseUrl;
    try {
      testStartAndRetrieveWorkflowInstance();
      System.out.println(this + " passed");
    } catch(Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  public void testStartAndRetrieveWorkflowInstance() throws Exception {
    HttpClient client = new DefaultHttpClient();
    
    // Start a workflow instance via the rest endpoint
    HttpPost postStart = new HttpPost(baseUrl + "/workflow/rest/start");
    List<NameValuePair> formParams = new ArrayList<NameValuePair>();

    formParams.add(new BasicNameValuePair("definition", getSampleWorkflowDefinition()));
    formParams.add(new BasicNameValuePair("mediapackage", getSampleMediaPackage()));
    formParams.add(new BasicNameValuePair("properties", "this=that"));
    postStart.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));

    // Grab the new workflow instance from the response
    String postResponse = client.execute(postStart, new ResponseHandler<String>() {
      public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
        HttpEntity entity = response.getEntity();
        return EntityUtils.toString(entity);
      }
    });
    
    String id = getWorkflowInstanceId(postResponse);
    System.out.println("Started workflow instance " + id);

    // Ensure we can retrieve the workflow instance from the rest endpoint
    HttpGet getWorkflowMethod = new HttpGet(baseUrl + "/workflow/rest/instance/" + id + ".xml");
    String getResponse = client.execute(getWorkflowMethod, new ResponseHandler<String>() {
      public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
        return EntityUtils.toString(response.getEntity());}
      });
    System.out.println("Retrieved workflow instance " + getWorkflowInstanceId(getResponse));
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
    return template.replaceAll("@SAMPLES_URL@", baseUrl + "/workflow/samples");
  }

  protected String getSampleWorkflowDefinition() throws Exception {
    return IOUtils.toString(getClass().getClassLoader().getResourceAsStream("workflow-definition-1.xml"));
  }

}
