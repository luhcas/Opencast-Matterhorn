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

package org.opencastproject.series.impl;

import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.series.api.Series;
import org.opencastproject.series.api.SeriesMetadata;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.persistence.jpa.PersistenceProvider;
import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.sql.DataSource;

public class SeriesServiceImplTest {

  SeriesServiceImpl service;

  SeriesImpl series;

  private DataSource datasource;

  private static final String storageRoot = "target" + File.separator + "service-test-db";

  private DataSource connectToDatabase(File storageDirectory) {
    JdbcConnectionPool cp = JdbcConnectionPool.create("jdbc:h2:" + storageDirectory + ";LOCK_MODE=1;MVCC=TRUE", "sa",
            "sa");
    return cp;
  }
  
  @Before
  public void setup() {
    File storageDir = new File(storageRoot);
    storageDir.mkdirs();
    datasource = connectToDatabase(new File(storageDir, "db"));

    // Collect the persistence properties
    Map<String, Object> props = new HashMap<String, Object>();
    props.put("javax.persistence.nonJtaDataSource", datasource);
    props.put("eclipselink.ddl-generation", "create-tables");
    props.put("eclipselink.ddl-generation.output-mode", "database");

    service = new SeriesServiceImpl();

    service.setPersistenceProvider(new PersistenceProvider());
    service.setPersistenceProperties(props);

    service.activate(null);

    series = new SeriesImpl();
    series.setSeriesId("10.0000/5819"); // see dublincore.xml in src/test/resources

    LinkedList<SeriesMetadata> metadata = new LinkedList<SeriesMetadata>();

    metadata.add(new SeriesMetadataImpl(series, "title", "demo title"));
    metadata.add(new SeriesMetadataImpl(series, "license", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "valid", "" + System.currentTimeMillis()));
    metadata.add(new SeriesMetadataImpl(series, "publisher", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "creator", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "subject", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "temporal", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "audience", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "spatial", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "rightsHolder", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "extent", "3600000"));
    metadata.add(new SeriesMetadataImpl(series, "created", "" + System.currentTimeMillis()));
    metadata.add(new SeriesMetadataImpl(series, "language", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "identifier", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "isReplacedBy", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "type", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "available", "" + System.currentTimeMillis()));
    metadata.add(new SeriesMetadataImpl(series, "modified", "" + System.currentTimeMillis()));
    metadata.add(new SeriesMetadataImpl(series, "replaces", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "contributor", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "description", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "issued", "" + System.currentTimeMillis()));
    series.setMetadata(metadata);
  }

  @After
  public void teardown() throws Exception {
    service.deactivate(null);
    service = null;
    FileUtils.deleteDirectory(new File(storageRoot));
  }

  @Test
  public void testSeriesService() throws Exception {
    Assert.assertNotNull(series.getSeriesId());
    Assert.assertNotNull(series.getMetadata());
    Assert.assertNotNull(series.getDublinCore());
    Assert.assertTrue(series.valid());

    service.addSeries(series);
    Series loaded = service.getSeries(series.getSeriesId());
    Assert.assertNotNull(loaded);
    Assert.assertNotNull(loaded.getMetadata());
  }

  @Test
  public void testDublinCoreParsing() throws Exception {
    // Load the DC catalog
    DublinCoreCatalogService dcService = new DublinCoreCatalogService();
    DublinCoreCatalog dc = null;
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream("/dublincore.xml");
      dc = dcService.load(in);
    } finally {
      IOUtils.closeQuietly(in);
    }

    // Update the series
    series.updateMetadata(dc);

    // Ensure that the in-memory series has been updated to reflect the xml catalog's values
    Assert.assertEquals(dc.getFirst(DublinCore.PROPERTY_DESCRIPTION), series
            .getFromMetadata(DublinCore.PROPERTY_DESCRIPTION.getLocalName()));
  }

}
