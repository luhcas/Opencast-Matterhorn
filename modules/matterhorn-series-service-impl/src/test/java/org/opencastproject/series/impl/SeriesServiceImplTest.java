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

import com.mchange.v2.c3p0.ComboPooledDataSource;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.eclipse.persistence.jpa.PersistenceProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class SeriesServiceImplTest {

  SeriesServiceImpl service;

  SeriesImpl series;

  private ComboPooledDataSource pooledDataSource = null;

  @Before
  public void setup() throws Exception {
    pooledDataSource = new ComboPooledDataSource();
    pooledDataSource.setDriverClass("org.h2.Driver");
    pooledDataSource.setJdbcUrl("jdbc:h2:./target/db" + System.currentTimeMillis() + ";LOCK_MODE=1;MVCC=TRUE");
    pooledDataSource.setUser("sa");
    pooledDataSource.setPassword("sa");

    // Collect the persistence properties
    Map<String, Object> props = new HashMap<String, Object>();
    props.put("javax.persistence.nonJtaDataSource", pooledDataSource);
    props.put("eclipselink.ddl-generation", "create-tables");
    props.put("eclipselink.ddl-generation.output-mode", "database");

    service = new SeriesServiceImpl();

    service.setPersistenceProvider(new PersistenceProvider());
    service.setPersistenceProperties(props);

    service.activate(null);

    series = new SeriesImpl();
    series.setSeriesId("10.0000/5819"); // see dublincore.xml in src/test/resources
    series.setDescription("demo");
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
    metadata.add(new SeriesMetadataImpl(series, "issued", "" + System.currentTimeMillis()));
    series.setMetadata(metadata);
  }

  @After
  public void teardown() throws Exception {
    service.deactivate(null);
    service = null;
    pooledDataSource.close();
  }

  @Test
  public void testSeriesService() throws Exception {
    Assert.assertNotNull(series.getSeriesId());
    Assert.assertNotNull(series.getMetadata());
    Assert.assertNotNull(series.getDublinCore());

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
    Assert.assertEquals(dc.getFirst(DublinCore.PROPERTY_TITLE),
            series.getFromMetadata(DublinCore.PROPERTY_TITLE.getLocalName()));
  }

  @Test
  public void testSeriesBuilder() throws Exception {
    String seriesXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><series><description>Description</description><additionalMetadata><metadata><key>title</key><value>title</value></metadata></additionalMetadata></series>";
    SeriesBuilder builder = SeriesBuilder.getInstance();
    Series s = builder.parseSeriesImpl(seriesXml);
    Assert.assertNotNull(s.getMetadata());
    Assert.assertEquals("Description", s.getDescription());
    Assert.assertEquals("title", s.getFromMetadata("title"));
    String marshalledSeries = builder.marshallSeries(s);
    Assert.assertEquals(seriesXml, marshalledSeries);
  }

}
