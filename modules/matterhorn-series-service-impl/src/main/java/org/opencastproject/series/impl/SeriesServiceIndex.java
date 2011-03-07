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
import org.opencastproject.series.api.SeriesQuery;
import org.opencastproject.series.api.SeriesResult;
import org.opencastproject.util.NotFoundException;

/**
 * Defines methods for indexing, retrieving and searching through index.
 * 
 */
public interface SeriesServiceIndex {
  /**
   * Performs any necessary setup and activates index.
   */
  void activate();

  /**
   * Deactivates index and performs any necessary cleanup.
   */
  void deactivate();

  /**
   * Index (new or existing) Dublin core representing series.
   * 
   * @param dublinCore
   *          {@link DublinCoreCatalog} representing series
   * @throws SeriesServiceDatabaseException
   *           if indexing fails
   */
  void index(DublinCoreCatalog dublinCore) throws SeriesServiceDatabaseException;

  /**
   * Removes series from index.
   * 
   * @param seriesID
   *          ID of the series to be removed
   * @throws SeriesServiceDatabaseException
   *           if removing fails
   */
  void delete(String seriesID) throws SeriesServiceDatabaseException;

  /**
   * Gets Dublin core representing series.
   * 
   * @param seriesID
   *          series to be retrieved
   * @return {@link DublinCoreCatalog} representing series
   * @throws SeriesServiceDatabaseException
   *           if retrieval fails
   * @throws NotFoundException
   *           if no such series exists
   */
  DublinCoreCatalog get(String seriesID) throws SeriesServiceDatabaseException, NotFoundException;

  /**
   * Search over indexed series with query.
   * 
   * @param query
   *          {@link SeriesQuery} object storing query parameters
   * @return {@link SeriesResult} with matching series
   * @throws SeriesServiceDatabaseException
   *           if query cannot be executed
   */
  SeriesResult search(SeriesQuery query) throws SeriesServiceDatabaseException;

  /**
   * Returns number of series in search index.
   * 
   * @return number of series in search index
   * @throws SeriesServiceDatabaseException
   *           if count cannot be retrieved
   */
  long count() throws SeriesServiceDatabaseException;
}