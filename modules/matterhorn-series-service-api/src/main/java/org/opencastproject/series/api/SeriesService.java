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
package org.opencastproject.series.api;

import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.util.NotFoundException;

/**
 * Series service API for creating, removing and searching over series.
 * 
 */
public interface SeriesService {

  /**
   * Adds or updates series
   * 
   * @param dc
   *          {@link DublinCoreCatalog} representing series
   * @throws SeriesException
   *           if adding or updating fails
   */
  void updateSeries(DublinCoreCatalog dc) throws SeriesException;

  /**
   * Removes series
   * 
   * @param seriesID
   *          ID of the series to be removed
   * @throws SeriesException
   *           if deleting fails
   * @throws NotFoundException
   *           if series with specified ID does not exist
   */
  void deleteSeries(String seriesID) throws SeriesException, NotFoundException;

  /**
   * Returns Dublin core representing series by series ID.
   * 
   * @param seriesID
   *          series to be retrieved
   * @return {@link DublinCoreCatalog} representing series
   * @throws SeriesException
   *           if retrieving fails
   */
  DublinCoreCatalog getSeries(String seriesID) throws SeriesException, NotFoundException;

  /**
   * Search over series
   * 
   * @param query
   *          {@link SeriesQuery} representing query
   * @return {@link SeriesResult} object that stores result of a query
   * @throws SeriesException
   *           if query could not be performed
   */
  SeriesResult getSeries(SeriesQuery query) throws SeriesException;
}
