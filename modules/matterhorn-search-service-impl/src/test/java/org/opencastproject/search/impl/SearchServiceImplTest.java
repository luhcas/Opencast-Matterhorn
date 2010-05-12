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

package org.opencastproject.search.impl;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;

import org.opencastproject.media.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilder;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
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
    service.setDublincoreService(new DublinCoreCatalogService());
    service.activate(null);
  }

  @After
  public void teardown() {
    service.deactivate();
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
    SearchResult result = service.getByQuery(new SearchQueryImpl().withId("foo"));
    assertEquals(0, result.size());
  }
  
  /**
   * Ads a simple media package that has a dublin core for the episode only.
   */
  @Test
  public void testAddSimpleMediaPackage() throws Exception {
    MediaPackageBuilderFactory builderFactory = MediaPackageBuilderFactory.newInstance();
    MediaPackageBuilder mediaPackageBuilder = builderFactory.newMediaPackageBuilder();
    URL rootUrl = SearchServiceImplTest.class.getResource("/");
    mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(rootUrl));
    
    // Load the simple media package
    MediaPackage mediaPackage = null;
    InputStream is = SearchServiceImplTest.class.getResourceAsStream("/manifest-simple.xml");
    mediaPackage = mediaPackageBuilder.loadFromXml(is);

    // Add the media package to the search index
    service.add(mediaPackage);
    
    // Make sure it's properly indexed and returned
    SearchQueryImpl q = new SearchQueryImpl();
    q.includeEpisodes(true);
    q.includeSeries(false);
    q.withId("10.0000/1");
    assertEquals(1, service.getByQuery(q).size());
    
    q = new SearchQueryImpl();
    q.includeEpisodes(true);
    q.includeSeries(false);
    
    assertEquals(1, service.getByQuery(q).size());
    
    // Test for various fields
    q = new SearchQueryImpl();
    q.includeEpisodes(true);
    q.includeSeries(false);
    q.withId("10.0000/1");
    SearchResult result = service.getByQuery(q);
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
      mediaPackage = mediaPackageBuilder.loadFromXml(is);
    } catch (MediaPackageException e) {
      fail("Error loading full media package");
    }

    // Add the media package to the search index
    service.add(mediaPackage);
    
    // Make sure it's properly indexed and returned
    SearchQueryImpl q = new SearchQueryImpl();
    q.includeEpisodes(true);
    q.includeSeries(false);
    q.withId("10.0000/2");
    assertEquals(1, service.getByQuery(q).size());
    q.withId(null); // Clear the ID requirement
    assertEquals(1, service.getByQuery(q).size());
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
      mediaPackage = mediaPackageBuilder.loadFromXml(is);
    } catch (MediaPackageException e) {
      fail("Error loading simple media package");
    }

    // Add the media package to the search index
    service.add(mediaPackage);
    service.delete(mediaPackage.getIdentifier().toString());

    SearchQueryImpl q = new SearchQueryImpl();
    q.includeEpisodes(true);
    q.includeSeries(false);
    q.withId("10.0000/1");
    assertEquals(0, service.getByQuery(q).size());
    q.withId(null); // Clear the ID requirement
    assertEquals(0, service.getByQuery(q).size());
  }

}