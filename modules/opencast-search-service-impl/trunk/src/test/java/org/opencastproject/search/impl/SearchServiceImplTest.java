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

package org.opencastproject.search.impl;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;

import org.opencastproject.media.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilder;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchResultItem;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Tests the functionality  of the search service.
 */
public class SearchServiceImplTest {

  /** The search service */
  private SearchServiceImpl service = null;

  /** The solr root directory */
  private String solrRoot = "target" + File.separator + "opencast" + File.separator + "searchindex";
    
  @Before
  public void setup() {
    service = new SearchServiceImpl(solrRoot);
    service.activate(null);
  }

  @After
  public void teardown() {
    try {
      FileUtils.deleteDirectory(new File(solrRoot));
    } catch (IOException e) {
      fail("Error on cleanup");
    }
  }
  
  /**
   * Test wether an empty search index will work.
   */
  @Test
  public void testEmptySearchIndex() {
    SearchResult result = service.getEpisodeAndSeriesById("foo");
    assertEquals(0, result.size());
  }
  
  /**
   * Ads a simple media package that has a dublin core for the episode only.
   */
  @Test
  public void testAddSimpleMediaPackage() {
    MediaPackageBuilderFactory builderFactory = MediaPackageBuilderFactory.newInstance();
    MediaPackageBuilder mediaPackageBuilder = builderFactory.newMediaPackageBuilder();
    URL rootUrl = SearchServiceImplTest.class.getResource("/");
    mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(rootUrl));
    
    // Load the simple media package
    MediaPackage mediaPackage = null;
    try {
      InputStream is = SearchServiceImplTest.class.getResourceAsStream("/manifest-simple.xml");
      mediaPackage = mediaPackageBuilder.loadFromManifest(is);
    } catch (MediaPackageException e) {
      fail("Error loading simple media package");
    }

    // Add the media package to the search index
    service.add(mediaPackage);
    
    // Make sure it's properly indexed and returned
    assertEquals(1, service.getEpisodeById("10.0000/1").size());
    assertEquals(1, service.getEpisodesByDate(Integer.MAX_VALUE, 0).size());
    
    // Test for various fields
    SearchResult result = service.getEpisodeById("10.0000/1");
    assertEquals(1, result.getTotalSize());
    SearchResultItem resultItem = result.getItems()[0];
    assertNotNull(resultItem.getMediaPackage());
    assertEquals(1, resultItem.getMediaPackage().getCatalogs().length);
  }

  /**
   * Ads a simple media package that has a dublin core for the episode only.
   */
  @Test
  public void testAddFullMediaPackage() {
    MediaPackageBuilderFactory builderFactory = MediaPackageBuilderFactory.newInstance();
    MediaPackageBuilder mediaPackageBuilder = builderFactory.newMediaPackageBuilder();
    URL rootUrl = SearchServiceImplTest.class.getResource("/");
    mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(rootUrl));
    
    // Load the simple media package
    MediaPackage mediaPackage = null;
    try {
      InputStream is = SearchServiceImplTest.class.getResourceAsStream("/manifest-full.xml");
      mediaPackage = mediaPackageBuilder.loadFromManifest(is);
    } catch (MediaPackageException e) {
      fail("Error loading full media package");
    }

    // Add the media package to the search index
    service.add(mediaPackage);
    
    // Make sure it's properly indexed and returned
    assertEquals(1, service.getEpisodeById("10.0000/2").size());
    assertEquals(1, service.getEpisodesByDate(Integer.MAX_VALUE, 0).size());
  }
  
  /**
   * Test removal from the search index.
   */
  @Test
  public void testDeleteMediaPackage() {
    MediaPackageBuilderFactory builderFactory = MediaPackageBuilderFactory.newInstance();
    MediaPackageBuilder mediaPackageBuilder = builderFactory.newMediaPackageBuilder();
    URL rootUrl = SearchServiceImplTest.class.getResource("/");
    mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(rootUrl));
    
    // Load the simple media package
    MediaPackage mediaPackage = null;
    try {
      InputStream is = SearchServiceImplTest.class.getResourceAsStream("/manifest-simple.xml");
      mediaPackage = mediaPackageBuilder.loadFromManifest(is);
    } catch (MediaPackageException e) {
      fail("Error loading simple media package");
    }

    // Add the media package to the search index
    service.add(mediaPackage);
    service.delete(mediaPackage.getIdentifier().toString());
    assertEquals(0, service.getEpisodeById("10.0000/1").size());
    assertEquals(0, service.getEpisodesByDate(Integer.MAX_VALUE, 0).size());
  }

}