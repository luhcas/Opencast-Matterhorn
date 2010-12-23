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

package org.opencastproject.search.api;

import org.opencastproject.mediapackage.MediaPackage;

/**
 * Provides search capabilities, possibly to the engage tools, possibly to other services.
 * 
 * TODO: Improve documentation
 */
public interface SearchService {
  /**
   * Identifier for service registration and location
   */
  String JOB_TYPE = "org.opencastproject.search";

  /**
   * Adds the media package to the search index.
   * 
   * @param mediaPackage
   *          the media package
   * @throws SearchException
   *           if an error occurs while adding the media package
   */
  void add(MediaPackage mediaPackage) throws SearchException;

  /**
   * Removes the media package identified by <code>mediaPackageId</code> from the search index.
   * 
   * @param mediaPackageId
   *          id of the media package to remove
   * @throws SearchException
   *           if an error occurs while removing the media package
   */
  void delete(String mediaPackageId) throws SearchException;

  /**
   * Clears the search index.
   * 
   * @throws SearchException
   *           if an error occurs while clearing the index
   */
  void clear() throws SearchException;

  /**
   * Find search results based on the specified query object
   * 
   * @param q
   *          The {@link SearchQuery} containing the details of the desired results
   * @return The search result
   * @throws SearchException
   *           if an error occurs while searching for media packages
   */
  SearchResult getByQuery(SearchQuery q) throws SearchException;

  /**
   * Sends a query to the search service. Depending on the service implementation, the query might be an sql statement a
   * solr query or something similar. In the future, a higher level query language might be a better solution.
   * 
   * @param query
   *          the search query
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @return the search result
   * @throws SearchException
   */
  SearchResult getByQuery(String query, int limit, int offset) throws SearchException;
}
