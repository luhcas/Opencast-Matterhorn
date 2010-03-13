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
package org.opencastproject.ingestui.impl;

import org.opencastproject.ingestui.api.IngestuiService;
import org.opencastproject.ingestui.api.Metadata;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IngestuiServiceImplTest {
  private IngestuiService service = null;

  private Metadata testdata;

  /**
   * Sets up a test metadata object.
   * 
   */
  @Before
  public void setup() {
    service = new IngestuiServiceImpl();

    // create test data
    testdata = new MetadataImpl();
    testdata.setTitle("A Pirates Life");
    testdata.setSubmitter("MetadataTester");
    testdata.setPresenter("Long John Silva");
    testdata.setDescription("Yaaarrr");
    testdata.setLanguage("Pirateish");
    testdata.setDistSakai(true);
    testdata.setDistYouTube(true);
    testdata.setDistITunes(true);
  }

  /**
   * let the service save the save the metadata, then retreives it to another object and compares them.
   * 
   */
  @Test
  public void testLifecycle() {
    // store test data
    service.acceptMetadata("metadatatest", testdata);

    // retrieve test data
    Metadata retrievedData = service.getMetadata("metadatatest");

    // check if retrived metadata equals test data
    Assert.assertEquals(testdata.getFilename(), retrievedData.getFilename());
    Assert.assertEquals(testdata.getTitle(), retrievedData.getTitle());
    Assert.assertEquals(testdata.getSubmitter(), retrievedData.getSubmitter());
    Assert.assertEquals(testdata.getPresenter(), retrievedData.getPresenter());
    Assert.assertEquals(testdata.getDescription(), retrievedData.getDescription());
    Assert.assertEquals(testdata.getLanguage(), retrievedData.getLanguage());
    Assert.assertEquals(testdata.getDistSakai(), retrievedData.getDistSakai());
    Assert.assertEquals(testdata.getDistYouTube(), retrievedData.getDistYouTube());
    Assert.assertEquals(testdata.getDistITunes(), retrievedData.getDistITunes());
  }

  @After
  public void teardown() {
    service = null;
  }

}
