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
package org.opencastproject.integrationtest;

import org.opencastproject.integrationtest.AbstractIntegrationTest;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.endpoint.WorkflowRestService;

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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests the workflow service in a running osgi container
 *
 */
@Ignore
public class WorkflowRestEndpointTest extends AbstractIntegrationTest {
  protected String baseUrl = null;
  
  @Before
  public void setup() throws Exception {
    String configuredUrl = super.bundleContext.getProperty("serverUrl");
    baseUrl = configuredUrl == null ? UrlSupport.DEFAULT_BASE_URL : configuredUrl;
    
    // Ensure the dependent services have started
    retrieveService(WorkflowService.class);
    retrieveService(WorkflowRestService.class);
//    retrieveService(ConductorService.class);
  }

  @Test
  public void testWorkflowRestEndpoint() throws Exception {
    HttpClient client = new DefaultHttpClient();
    
    // Start a workflow instance via the rest endpoint
    HttpPost postStart = new HttpPost(baseUrl + "/workflow/rest/start");
    List<NameValuePair> formParams = new ArrayList<NameValuePair>();

    formParams.add(new BasicNameValuePair("definition", getSampleWorkflowDefinition()));
    formParams.add(new BasicNameValuePair("mediapackage", getSampleMediaPackage()));
    UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formParams, "UTF-8");
    postStart.setEntity(entity);

    // Grab the new workflow instance from the response
    String postResponse = client.execute(postStart, new ResponseHandler<String>() {
      public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
        HttpEntity entity = response.getEntity();
        return EntityUtils.toString(entity);
      }
    });
    System.out.println("Started workflow instance " + postResponse);
    WorkflowInstance instanceFromPost = WorkflowBuilder.getInstance().parseWorkflowInstance(postResponse);

    // Ensure we can retrieve the workflow instance from the rest endpoint
    HttpGet getWorkflowMethod = new HttpGet(baseUrl + "/workflow/rest/instance/" + instanceFromPost.getId() + ".xml");
    String getResponse = client.execute(getWorkflowMethod, new ResponseHandler<String>() {
      public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
        return EntityUtils.toString(response.getEntity());}
      });
    WorkflowInstance instanceFromGet = WorkflowBuilder.getInstance().parseWorkflowInstance(getResponse);
    Assert.assertEquals(instanceFromPost.getId(), instanceFromGet.getId());
    System.out.println("Retrieved workflow " + getResponse);
  }

  protected String getSampleMediaPackage() {
    try {
      String template = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("/mediapackage-1.xml"));
      return template.replaceAll("@SAMPLES_URL@", baseUrl + "/workflow/samples");
    } catch (IOException e) {Assert.fail(); return null;}
  }

  protected String getSampleWorkflowDefinition() {
    try {
      return IOUtils.toString(getClass().getClassLoader().getResourceAsStream("/workflow-definition-1.xml"));
    } catch (IOException e) {Assert.fail(); return null;}
  }
}
