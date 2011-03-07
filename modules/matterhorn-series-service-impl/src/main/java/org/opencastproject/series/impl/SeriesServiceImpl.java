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

import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesQuery;
import org.opencastproject.series.api.SeriesResult;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.series.impl.solr.SeriesServiceSolrIndex;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PathSupport;

import org.osgi.framework.ServiceException;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Implements {@link SeriesService}. Uses {@link SeriesServiceDatabase} for permanent storage and
 * {@link SeriesServiceIndex} for searching.
 * 
 */
public class SeriesServiceImpl implements SeriesService, ManagedService {

  /** Logging utility */
  private static final Logger logger = LoggerFactory.getLogger(SeriesServiceImpl.class);

  /** Index for searching */
  protected SeriesServiceIndex index;
  
  /** Persistent storage */
  protected SeriesServiceDatabase persistence;
  
  /** Whether indexing is synchronous or asynchronous */
  protected boolean synchronousIndexing;
  
  /** Executor used for asynchronous indexing */
  protected ExecutorService indexingExecutor;

  /**
   * OSGi callback for setting index.
   * 
   * @param index
   */
  public void setIndex(SeriesServiceIndex index) {
    this.index = index;
  }

  /**
   * OSGi callback for setting persistance.
   * 
   * @param persistence
   */
  public void setPersistence(SeriesServiceDatabase persistence) {
    this.persistence = persistence;
  }

  /**
   * Activates Series Service. Checks whether we are using synchronous or asynchronous indexing. If asynchronous is
   * used, Executor service is set. If index is empty, persistent storage is queried if it contains any series. If that
   * is the case, series are retrieved and indexed.
   * 
   * @param cc
   *          ComponentContext
   * @throws Exception
   */
  public void activate(ComponentContext cc) throws Exception {
    logger.info("Activating Series Service");

    if (cc == null) {
      this.index = new SeriesServiceSolrIndex(PathSupport.concat(System.getProperty("java.io.tmpdir"), "series"));
      ((SeriesServiceSolrIndex)index).setDublinCoreService(new DublinCoreCatalogService());
      this.index.activate();
      this.synchronousIndexing = true;
    } else {
      Object syncIndexingConfig = cc.getProperties().get("synchronousIndexing");
      if ((syncIndexingConfig != null) && ((syncIndexingConfig instanceof Boolean))) {
        this.synchronousIndexing = ((Boolean) syncIndexingConfig).booleanValue();
      }
    }
    if (this.synchronousIndexing) {
      logger.debug("Series will be added to the search index synchronously");
    } else {
      logger.debug("Series will be added to the search index asynchronously");
      this.indexingExecutor = Executors.newSingleThreadExecutor();
    }

    long instancesInSolr = 0L;
    try {
      instancesInSolr = this.index.count();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    if (instancesInSolr == 0L) {
      try {
        DublinCoreCatalog[] databaseSeries = this.persistence.getAllSeries();
        if (databaseSeries.length != 0) {
          logger.info("The series index is empty. Populating it now with {} series",
                  Integer.valueOf(databaseSeries.length));
          for (DublinCoreCatalog dc : databaseSeries) {
            this.index.index(dc);
          }
          logger.info("Finished populating series search index");
        }
      } catch (Exception e) {
        logger.warn("Unable to index series instances: {}", e);
        throw new ServiceException(e.getMessage());
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  public void updated(@SuppressWarnings("rawtypes") Dictionary properties) throws ConfigurationException {
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.opencastproject.series.api.SeriesService#updateSeries(org.opencastproject.metadata.dublincore.DublinCoreCatalog
   * )
   */
  public void updateSeries(final DublinCoreCatalog dc) throws SeriesException {
    try {
      this.persistence.storeSeries(dc);
    } catch (SeriesServiceDatabaseException e1) {
      logger.error("Could not store series {}: {}", dc, e1);
      throw new SeriesException(e1);
    }

    if (this.synchronousIndexing)
      try {
        this.index.index(dc);
      } catch (SeriesServiceDatabaseException e) {
        logger.warn("Unable to index {}: {}", dc, e);
      }
    else
      this.indexingExecutor.submit(new Runnable() {
        public void run() {
          try {
            index.index(dc);
          } catch (SeriesServiceDatabaseException e) {
            SeriesServiceImpl.logger.warn("Unable to index {}: {}", dc, e);
          }
        }
      });
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesService#deleteSeries(java.lang.String)
   */
  public void deleteSeries(final String seriesID) throws SeriesException, NotFoundException {
    try {
      this.persistence.deleteSeries(seriesID);
    } catch (SeriesServiceDatabaseException e1) {
      logger.error("Could not delete series with id {} from persistence storage", seriesID);
      throw new SeriesException(e1);
    }

    if (this.synchronousIndexing)
      try {
        this.index.delete(seriesID);
      } catch (SeriesServiceDatabaseException e) {
        logger.warn("Unable to delete series with id {}: {}", seriesID, e);
      }
    else
      this.indexingExecutor.submit(new Runnable() {
        public void run() {
          try {
            index.delete(seriesID);
          } catch (SeriesServiceDatabaseException e) {
            SeriesServiceImpl.logger.warn("Unable to delete series with id {}: {}", seriesID, e);
          }
        }
      });
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesService#getSeries(org.opencastproject.series.api.SeriesQuery)
   */
  public SeriesResult getSeries(SeriesQuery query) throws SeriesException {
    try {
      return this.index.search(query);
    } catch (SeriesServiceDatabaseException e) {
      logger.error("Failed to execute search query: {}", e);
      throw new SeriesException(e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesService#getSeries(java.lang.String)
   */
  public DublinCoreCatalog getSeries(String seriesID) throws SeriesException, NotFoundException {
    try {
      return this.index.get(seriesID);
    } catch (SeriesServiceDatabaseException e) {
      logger.error("Exception occured while retrieving series: {}", e);
      throw new SeriesException(e);
    }
  }
}
