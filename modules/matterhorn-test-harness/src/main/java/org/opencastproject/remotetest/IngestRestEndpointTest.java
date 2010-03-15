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
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.JUnit4;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Posts a zip file to the ingest service
 */
public class IngestRestEndpointTest {
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
  public void testIngestThinClient() throws Exception {
    // create emptiy MediaPackage
    HttpGet get = new HttpGet(BASE_URL + "/ingest/rest/createMediaPackage");
    HttpResponse response = client.execute(get);
    HttpEntity entity = response.getEntity();
    String mp = "";
    if (entity != null) {
      InputStream instream = entity.getContent();
      mp = convertStreamToString(instream);
      // System.out.println(mp);
    }
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());

    // Grow media package
    mp = postCall("/ingest/rest/addTrack", "av.mov", "track/presenter", mp);
    mp = postCall("/ingest/rest/addCatalog", "dublincore.xml", "metadata/dublincore", mp);
    mp = postCall("/ingest/rest/addAttachment", "cover.png", "cover/source", mp);

    // Ingest the new grown media package
    mp = postCall("/ingest/rest/ingest", "", "", mp);
  }

  protected String postCall(String method, String mediaFile, String flavor, String mediaPackage)
          throws ClientProtocolException, IOException {
    HttpPost post = new HttpPost(BASE_URL + method);
    List<NameValuePair> formParams = new ArrayList<NameValuePair>();
    formParams = new ArrayList<NameValuePair>();
    formParams.add(new BasicNameValuePair("url", getClass().getClassLoader().getResource(mediaFile).toString()));
    formParams.add(new BasicNameValuePair("flavor", flavor));
    formParams.add(new BasicNameValuePair("mediaPackage", mediaPackage));
    post.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));
    HttpResponse response = client.execute(post);
    HttpEntity entity = response.getEntity();
    String mp = "";
    if (entity != null) {
      InputStream instream = entity.getContent();
      mp = convertStreamToString(instream);
      // System.out.println(mp);
    }
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    return mp;
  }

  protected String convertStreamToString(InputStream is) throws IOException {
    if (is != null) {
      StringBuilder sb = new StringBuilder();
      String line;
      try {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        while ((line = reader.readLine()) != null) {
          sb.append(line).append("\n");
        }
      } finally {
        is.close();
      }
      return sb.toString();
    } else {
      return "";
    }
  }
}
