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

import java.util.List;

/**
 * FIXME -- Add javadocs
 */
public interface SeriesService {

  /**
   * Adds a new Series
   * 
   * @param s
   *          The series that should be stored
   * @throws IllegalArgumentException
   *           if a series already exists with this identifier
   */
  void addSeries(Series s) throws IllegalArgumentException;

  /**
   * Removes the series with the given seriesID
   * 
   * @param seriesID
   *          The ID of the series that should be removed
   * @throws NotFoundException
   *           if the series doesn't exist
   */
  void removeSeries(String seriesID) throws NotFoundException, SeriesException;

  /**
   * updates an series in the database
   * 
   * @param s
   *          The series that should be updated. A series with the given ID has to be in the database already!
   * @throws NotFoundException
   *           if the series doesn't exist
   */
  void updateSeries(Series s) throws NotFoundException, SeriesException;

  /**
   * returns the series with the provided ID
   * 
   * @param seriesID
   *          The ID of the requested series
   * @return The requested series
   */
  Series getSeries(String seriesID) throws NotFoundException;

  /**
   * returns all series as a List
   * 
   * @return A List with the series'
   */
  List<Series> getAllSeries();

  /**
   * returns the Dublin Core metadata set for the series specified by the ID
   * 
   * @param seriesID
   *          The ID of the demanded series
   * @return A dublin Core Element
   */
  DublinCoreCatalog getDublinCore(String seriesID) throws NotFoundException;

  /**
   * Adds or updates an existing series based on a dublin core catalog.
   * 
   * @param dcCatalog
   *          The dublin core metadata catalog
   * @return The updated series
   */
  Series addOrUpdate(DublinCoreCatalog dcCatalog) throws SeriesException;

  /**
   * Searches for all series' that fit into a certain pattern
   * 
   * @param pattern
   *          a part the value of a metadata field
   * @return a List of all series that match that pattern
   */
  List<Series> searchSeries(String pattern) throws NotFoundException;
  
}
