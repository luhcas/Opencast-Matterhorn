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

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * API for series result. Stores results, search time and number of requested items and offset.
 */
@XmlJavaTypeAdapter(SeriesResultImpl.Adapter.class)
public interface SeriesResult {

  /**
   * Returns size of the result.
   * 
   * @return result size
   */
  long getSize();

  /**
   * Returns number off items corresponding query.
   * 
   * @return number of items
   */
  long getNumberOfItems();

  /**
   * Returns search time in milliseconds.
   * 
   * @return search time
   */
  long getSearchTime();

  /**
   * Returns result offset
   * 
   * @return start page
   */
  long getStartPage();

  /**
   * Returns page size
   * 
   * @return page size
   */
  long getPageSize();

  /**
   * Returns resulting items.
   * 
   * @return items
   */
  SeriesResultItem[] getItems();
}
