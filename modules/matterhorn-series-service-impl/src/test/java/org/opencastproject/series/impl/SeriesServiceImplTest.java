package org.opencastproject.series.impl;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.sql.DataSource;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.eclipse.persistence.jpa.PersistenceProvider;
import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opencastproject.series.api.Series;
import org.opencastproject.series.api.SeriesMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeriesServiceImplTest {
  private static final Logger logger = LoggerFactory.getLogger(SeriesServiceImplTest.class);
  
  SeriesServiceImpl service;
  
  Series series;
  
  private DataSource datasource;
  
  private static final String storageRoot = "target" + File.separator + "service-test-db";
  private static final String resourcesRoot = "src" + File.separator + "main" + File.separator + "resources";
  
  
  private DataSource connectToDatabase(File storageDirectory) {
    if (storageDirectory == null) {
      storageDirectory = new File(File.separator + "tmp" + File.separator +"opencast" + File.separator + "scheduler-db");
    }
      JdbcConnectionPool cp = JdbcConnectionPool.create("jdbc:h2:" + storageDirectory + ";LOCK_MODE=1;MVCC=TRUE", "sa", "sa");
    return cp;
  } 
  
  @Before
  public void setup () {
    // Clean up database
    try { 
      FileUtils.deleteDirectory(new File(storageRoot));
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    }    
    datasource = connectToDatabase(new File(storageRoot));
    
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
    
    LinkedList<SeriesMetadata> metadata = new LinkedList<SeriesMetadata>();
    
    metadata.add(new SeriesMetadataImpl(series, "title", "demo title"));
    metadata.add(new SeriesMetadataImpl(series, "license", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "valid", ""+System.currentTimeMillis()));
    metadata.add(new SeriesMetadataImpl(series, "publisher", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "creator", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "subject", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "temporal", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "audience", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "spatial", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "rightsHolder", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "extent", "3600000"));
    metadata.add(new SeriesMetadataImpl(series, "created", ""+System.currentTimeMillis()));
    metadata.add(new SeriesMetadataImpl(series, "language", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "identifier", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "isReplacedBy", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "type", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "available", ""+System.currentTimeMillis()));
    metadata.add(new SeriesMetadataImpl(series, "modified", ""+System.currentTimeMillis()));
    metadata.add(new SeriesMetadataImpl(series, "replaces", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "contributor", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "description", "demo"));
    metadata.add(new SeriesMetadataImpl(series, "issued", ""+System.currentTimeMillis()));
    
    series.setMetadata(metadata);
    
    
  }
  
  @After
  public void teardown () {
    service.deactivate(null);
    service = null;
    
    
  }
  
  @Test
  public void testSeriesService () {
    
    String id = service.newSeriesID();
    Assert.assertNotNull(id);
    
    series.setSeriesId(id);
    Assert.assertNotNull(series);
    Assert.assertNotNull(series.getSeriesId());
    Assert.assertNotNull(series.getMetadata());
    Assert.assertNotNull(series.getDublinCore());
    Assert.assertTrue(series.valid());
    
    boolean added = service.addSeries(series);
    Assert.assertTrue(added);
    
    Series loaded = service.getSeries(series.getSeriesId());
    Assert.assertNotNull(loaded);
    Assert.assertNotNull(loaded.getMetadata());
    
    
  }
  
  
  
  
}
