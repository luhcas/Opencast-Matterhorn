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
package org.opencastproject.series.impl.solr;

import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.series.api.SeriesQuery;
import org.opencastproject.series.api.SeriesResult;
import org.opencastproject.series.api.SeriesResultItem;
import org.opencastproject.util.PathSupport;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;

/**
 * Tests indexing: indexing, removing, retrieving, merging and searching.
 * 
 */
public class SeriesServiceSolrTest {

  private SeriesServiceSolrIndex index;
  private DublinCoreCatalogService dcService;
  private DublinCoreCatalog testCatalog;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    index = new SeriesServiceSolrIndex();
    index.solrRoot = PathSupport.concat("target", Long.toString(System.currentTimeMillis()));
    dcService = new DublinCoreCatalogService();
    index.setDublinCoreService(dcService);
    index.activate();

    InputStream in = null;
    try {
      in = getClass().getResourceAsStream("/dublincore.xml");
      testCatalog = dcService.load(in);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  @Test
  public void testIndexing() throws Exception {
    index.index(testCatalog);
    Assert.assertTrue("Index should contain one instance", index.count() == 1);
  }

  @Test
  public void testDeletion() throws Exception {
    index.index(testCatalog);
    index.delete(testCatalog.getFirst(DublinCore.PROPERTY_IDENTIFIER));
    Assert.assertTrue("Index should be empty", index.count() == 0);
  }

  @Test
  public void testMergingAndRetrieving() throws Exception {
    DublinCoreCatalog secondCatalog = dcService.newInstance();
    secondCatalog.add(DublinCore.PROPERTY_IDENTIFIER, testCatalog.getFirst(DublinCore.PROPERTY_IDENTIFIER));
    secondCatalog.add(DublinCore.PROPERTY_TITLE, "Test Title");

    index.index(testCatalog);
    index.index(secondCatalog);
    Assert.assertTrue("Index should contain one instance", index.count() == 1);

    DublinCoreCatalog returnedCatalog = index.get(testCatalog.getFirst(DublinCore.PROPERTY_IDENTIFIER));
    Assert.assertTrue("Unexpected Dublin Core", returnedCatalog.getFirst(DublinCore.PROPERTY_TITLE)
            .equals("Test Title"));
  }

  @Test
  public void testSearchingByTitleAndFullText() throws Exception {
    DublinCoreCatalog firstCatalog = dcService.newInstance();
    firstCatalog.add(DublinCore.PROPERTY_IDENTIFIER, "10.0000/1");
    firstCatalog.add(DublinCore.PROPERTY_TITLE, "Cats and Dogs");
    firstCatalog.add(DublinCore.PROPERTY_DESCRIPTION, "This lecture tries to give an explanation...");

    DublinCoreCatalog secondCatalog = dcService.newInstance();
    secondCatalog.add(DublinCore.PROPERTY_IDENTIFIER, "10.0000/2");
    secondCatalog.add(DublinCore.PROPERTY_TITLE, "Nature of Dogs");
    secondCatalog.add(DublinCore.PROPERTY_DESCRIPTION, "Why do dogs chase cats?");

    index.index(firstCatalog);
    index.index(secondCatalog);

    SeriesQuery q = new SeriesQuery().setSeriesTitle("cat");
    SeriesResult result = index.search(q);
    Assert.assertTrue("Only one title contains 'cat'", result.getSize() == 1);

    q = new SeriesQuery().setSeriesTitle("dog");
    result = index.search(q);
    Assert.assertTrue("Both titles contains 'dog'", result.getSize() == 2);

    q = new SeriesQuery().setText("cat");
    result = index.search(q);
    Assert.assertTrue("Both Dublin Cores contains 'cat'", result.getSize() == 2);
  }

  @Test
  public void testCreatedRangedTest() throws Exception {
    DublinCoreCatalog firstCatalog = dcService.newInstance();
    firstCatalog.add(DublinCore.PROPERTY_IDENTIFIER, "10.0000/1");
    firstCatalog.add(DublinCore.PROPERTY_TITLE, "Cats and Dogs");
    firstCatalog.add(DublinCore.PROPERTY_CREATED, "2007-05-03");

    DublinCoreCatalog secondCatalog = dcService.newInstance();
    secondCatalog.add(DublinCore.PROPERTY_IDENTIFIER, "10.0000/2");
    secondCatalog.add(DublinCore.PROPERTY_TITLE, "Nature of Dogs");
    secondCatalog.add(DublinCore.PROPERTY_CREATED, "2007-05-05");

    DublinCoreCatalog thirdCatalog = dcService.newInstance();
    thirdCatalog.add(DublinCore.PROPERTY_IDENTIFIER, "10.0000/3");
    thirdCatalog.add(DublinCore.PROPERTY_TITLE, "Nature");
    thirdCatalog.add(DublinCore.PROPERTY_CREATED, "2007-05-07");

    index.index(firstCatalog);
    index.index(secondCatalog);
    index.index(thirdCatalog);

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    SeriesQuery q = new SeriesQuery().setCreatedFrom(sdf.parse("2007-05-02")).setCreatedTo(sdf.parse("2007-05-06"));
    SeriesResult result = index.search(q);
    Assert.assertTrue("Two series satisfy time range", result.getSize() == 2);
  }

  @Test
  public void testMultivaluedFields() throws Exception {
    DublinCoreCatalog catalog = dcService.newInstance();
    catalog.add(DublinCore.PROPERTY_IDENTIFIER, "10.0000/1");
    catalog.add(DublinCore.PROPERTY_TITLE, "Cats and Dogs");
    catalog.add(DublinCore.PROPERTY_CONTRIBUTOR, "John Smith");
    catalog.add(DublinCore.PROPERTY_CONTRIBUTOR, "John Doe");
    Assert.assertTrue(catalog.get(DublinCore.PROPERTY_CONTRIBUTOR).size() == 2);

    index.index(catalog);

    SeriesQuery q = new SeriesQuery().setSeriesId(catalog.getFirst(DublinCore.PROPERTY_IDENTIFIER));
    SeriesResult result = index.search(q);
    SeriesResultItem resultingCatalog = result.getItems()[0];
    Assert.assertTrue(resultingCatalog.getContributor().size() == 2);
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
    index.deactivate();
    FileUtils.deleteDirectory(new File(index.solrRoot));
    index = null;
  }

}
