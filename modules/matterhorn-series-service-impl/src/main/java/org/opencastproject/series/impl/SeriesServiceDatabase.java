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
import org.opencastproject.util.NotFoundException;

/**
 * API that defines persistent storage of series.
 * 
 */
public interface SeriesServiceDatabase {

  /**
   * Store (or update) series.
   * 
   * @param dc
   *          {@link DublinCoreCatalog} representing series
   * @throws SeriesServiceDatabaseException
   *           if exception occurs
   */
  void storeSeries(DublinCoreCatalog dc) throws SeriesServiceDatabaseException;

  /**
   * Removes series from persistent storage.
   * 
   * @param seriesId
   *          ID of the series to be removed
   * @throws SeriesServiceDatabaseException
   *           if exception occurs
   * @throws NotFoundException
   *           if series with specified ID is not found
   */
  void deleteSeries(String seriesId) throws SeriesServiceDatabaseException, NotFoundException;

  /**
   * Returns all series in persistent storage.
   * 
   * @return {@link DublinCoreCatalog} array representing stored series
   * @throws SeriesServiceDatabaseException
   *           if exception occurs
   */
  DublinCoreCatalog[] getAllSeries() throws SeriesServiceDatabaseException;
}
