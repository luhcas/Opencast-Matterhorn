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
import org.opencastproject.metadata.dublincore.DublinCoreCatalogList;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesQuery;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.NotFoundException;

import org.apache.commons.lang.StringUtils;
import org.opencastproject.util.RequireUtil;
import org.osgi.framework.ServiceException;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.List;

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

    long instancesInSolr = 0L;
    try {
      instancesInSolr = this.index.count();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    if (instancesInSolr == 0L) {
      try {
        DublinCoreCatalog[] databaseSeries = persistence.getAllSeries();
        if (databaseSeries.length != 0) {
          logger.info("The series index is empty. Populating it now with {} series",
                  Integer.valueOf(databaseSeries.length));
          for (DublinCoreCatalog series : databaseSeries) {
            index.index(series);
            String id = series.getFirst(DublinCore.PROPERTY_IDENTIFIER);
            AccessControlList acl = persistence.getAccessControlList(id);
            if (acl != null) {
              index.index(id, acl);
            }
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
  @Override
  public void updated(@SuppressWarnings("unchecked") Dictionary properties) throws ConfigurationException {
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.opencastproject.series.api.SeriesService#updateSeries(org.opencastproject.metadata.dublincore.DublinCoreCatalog
   * )
   */
  @Override
  public boolean updateSeries(final DublinCoreCatalog dc) throws SeriesException {
    if (dc == null) {
      throw new IllegalArgumentException("DC argument for updating series must not be null");
    }

    boolean updated;
    try {
      updated = persistence.storeSeries(dc);
    } catch (SeriesServiceDatabaseException e1) {
      logger.error("Could not store series {}: {}", dc, e1);
      throw new SeriesException(e1);
    }

    try {
      index.index(dc);
    } catch (SeriesServiceDatabaseException e) {
      logger.error("Unable to index series {}: {}", dc.getFirst(DublinCore.PROPERTY_IDENTIFIER), e.getMessage());
      throw new SeriesException(e);
    }

    return updated;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesService#updateAccessControl(java.lang.String,
   * org.opencastproject.security.api.AccessControlList)
   */
  @Override
  public boolean updateAccessControl(final String seriesID, final AccessControlList accessControl)
          throws NotFoundException, SeriesException {
    if (StringUtils.isEmpty(seriesID)) {
      throw new IllegalArgumentException("Series ID parameter must not be null or empty.");
    }
    if (accessControl == null) {
      throw new IllegalArgumentException("ACL parameter must not be null");
    }

    boolean updated;
    // try updating it in persistence first - not found is thrown if it doesn't exist
    try {
      updated = persistence.storeSeriesAccessControl(seriesID, accessControl);
    } catch (SeriesServiceDatabaseException e) {
      logger.error("Could not update series {} with access control rules: {}", seriesID, e.getMessage());
      throw new SeriesException(e);
    }

    try {
      index.index(seriesID, accessControl);
    } catch (Exception e) {
      logger.error("Could not update series {} with access control rules: {}", seriesID, e.getMessage());
      throw new SeriesException(e);
    }

    return updated;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesService#deleteSeries(java.lang.String)
   */
  @Override
  public void deleteSeries(final String seriesID) throws SeriesException, NotFoundException {
    try {
      this.persistence.deleteSeries(seriesID);
    } catch (SeriesServiceDatabaseException e1) {
      logger.error("Could not delete series with id {} from persistence storage", seriesID);
      throw new SeriesException(e1);
    }

    try {
      index.delete(seriesID);
    } catch (SeriesServiceDatabaseException e) {
      logger.error("Unable to delete series with id {}: {}", seriesID, e.getMessage());
      throw new SeriesException(e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesService#getSeries(org.opencastproject.series.api.SeriesQuery)
   */
  @Override
  public DublinCoreCatalogList getSeries(SeriesQuery query) throws SeriesException {
    try {
      List<DublinCoreCatalog> result = index.search(query);
      DublinCoreCatalogList dcList = new DublinCoreCatalogList();
      dcList.setCatalogList(result);
      return dcList;
    } catch (SeriesServiceDatabaseException e) {
      logger.error("Failed to execute search query: {}", e.getMessage());
      throw new SeriesException(e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesService#getSeries(java.lang.String)
   */
  @Override
  public DublinCoreCatalog getSeries(String seriesID) throws SeriesException, NotFoundException {
    try {
      return this.index.getDublinCore(RequireUtil.notNull(seriesID, "seriesID"));
    } catch (SeriesServiceDatabaseException e) {
      logger.error("Exception occured while retrieving series {}: {}", seriesID, e.getMessage());
      throw new SeriesException(e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesService#getSeriesAccessControl(java.lang.String)
   */
  @Override
  public AccessControlList getSeriesAccessControl(String seriesID) throws NotFoundException, SeriesException {
    try {
      return index.getAccessControl(RequireUtil.notNull(seriesID, "seriesID"));
    } catch (SeriesServiceDatabaseException e) {
      logger.error("Exception occurred while retrieving access control rules for series {}: {}", seriesID,
              e.getMessage());
      throw new SeriesException(e);
    }
  }
}
