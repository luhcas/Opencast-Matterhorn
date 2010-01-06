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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

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
    // Start a workflow instance via the rest endpoint
    HttpPost postEncode = new HttpPost(BASE_URL + "/composer/rest/encode");
    List<NameValuePair> formParams = new ArrayList<NameValuePair>();
    formParams.add(new BasicNameValuePair("mediapackage", getSampleMediaPackage()));
    formParams.add(new BasicNameValuePair("audioSourceTrackId", "track-1"));
    formParams.add(new BasicNameValuePair("videoSourceTrackId", "track-2"));
    formParams.add(new BasicNameValuePair("targetTrackId", "track-3"));
    formParams.add(new BasicNameValuePair("profileId", "flash.http"));
    postEncode.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));

    // Grab the new track from the response
    String postResponse = EntityUtils.toString(client.execute(postEncode).getEntity());
    System.out.println("postResponse=" + postResponse);
    Assert.assertTrue(postResponse.contains("<ns2:receipt xmlns:ns2=\"http://composer.opencastproject.org/\""));
  }

  protected String getSampleMediaPackage() throws Exception {
    String template = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("mediapackage-2.xml"));
    return template.replaceAll("@SAMPLES_URL@", BASE_URL + "/workflow/samples");
  }
}
