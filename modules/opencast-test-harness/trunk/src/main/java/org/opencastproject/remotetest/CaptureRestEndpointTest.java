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

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/** 
 * Test the functionality of the capture endpoints. Does not assume capture
 * devices are connected, so only mock captures are tested
 */
public class CaptureRestEndpointTest {
  
  private HttpClient httpClient;
  
  @Before
  public void setup() throws Exception {
    httpClient = new DefaultHttpClient();
  }
  
  @After
  public void tearDown() throws Exception {
    httpClient.getConnectionManager().shutdown();
  }
  
  @Test
  public void testStartCaptureGet() throws Exception {
    
    HttpGet getStart = new HttpGet(BASE_URL + "/capture/rest/startCapture");
    int getResponse = httpClient.execute(getStart).getStatusLine().getStatusCode();
    Assert.assertTrue(getResponse == HttpStatus.SC_OK || getResponse == HttpStatus.SC_INTERNAL_SERVER_ERROR);
  }
  
  @Test
  public void testStopCaptureGet() throws Exception {

    HttpGet get = new HttpGet(BASE_URL + "/capture/rest/stopCapture");
    int getResponse = httpClient.execute(get).getStatusLine().getStatusCode();
    Assert.assertTrue(getResponse == HttpStatus.SC_OK || getResponse == HttpStatus.SC_INTERNAL_SERVER_ERROR);

  }
  

  
  @Test
  public void testStartCapturePost() throws Exception {
    // Test Properties from resources
    String props = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("capture.properties"));
    
    HttpPost postStart = new HttpPost(BASE_URL + "/capture/rest/startCapture");
    List<NameValuePair> formParams = new ArrayList<NameValuePair>();
    formParams.add(new BasicNameValuePair("config", props));
    postStart.setEntity(new UrlEncodedFormEntity(formParams));
    int postResponse = httpClient.execute(postStart).getStatusLine().getStatusCode();
    Assert.assertTrue(postResponse == HttpStatus.SC_OK || postResponse == HttpStatus.SC_INTERNAL_SERVER_ERROR);
  }

  @Test
  public void testStopCapturePost() throws Exception {
    
    HttpPost postStop = new HttpPost(BASE_URL + "/capture/rest/stopCapture");
    List<NameValuePair> formParams = new ArrayList<NameValuePair>();
    formParams.add(new BasicNameValuePair("recordingID", "none"));
    postStop.setEntity(new UrlEncodedFormEntity(formParams));
    
    int postResponse = httpClient.execute(postStop).getStatusLine().getStatusCode();
    Assert.assertTrue(postResponse == HttpStatus.SC_OK || postResponse == HttpStatus.SC_INTERNAL_SERVER_ERROR);
  }
  
  @Test
  public void printTestingInfo() {
    System.out.println("\n\nInstructions to test capture agent\n==================================");
    System.out.println("The capture agents can be tested by setting the appropriate\n" +
        "properties for the JVM. They properties right now are: testHauppauge, " +
        "testEpiphan, testBt878, testAlsa.\n" +
        "Each property should be assigned to its location instead of setting a boolean.\n" +
        "An example test could be: mvn test -DargLine=\"-DtestHauppauge=/dev/video0 -DtestAlsa=hw:0\"\n\n");
  }
  

}
