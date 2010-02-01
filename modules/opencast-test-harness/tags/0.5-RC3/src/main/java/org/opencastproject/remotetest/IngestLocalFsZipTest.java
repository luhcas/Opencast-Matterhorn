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

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Posts a zip file from the local filesystem to the ingest service.  Because this test makes assumptions about the
 * local filesystem (like your're on OSX, and your name is Josh, and there'a s file named media.zip on your desktop),
 * this test is not part of the {@link AllRemoteTests} test suite.
 */
public class IngestLocalFsZipTest {
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
  public void testIngestZip() throws Exception {
    File f = new File("/Users/josh/Desktop/media.zip");
    long length = f.length();
    InputStream in = new FileInputStream(f);
    HttpPost post = new HttpPost(BASE_URL + "/ingest/rest/addZippedMediaPackage");
    post.setEntity(new InputStreamEntity(in, length));
    HttpResponse response = client.execute(post);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    in.close();
  }
}
