/**
 *  Copyright 2009 The Regents of the University of California
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

package org.opencastproject.workflow.api;

/**
 * A single result of searching.
 */
public interface WorkflowSet {

  /**
   * The search item list
   * 
   * @return Item list.
   */
  public abstract WorkflowInstance[] getItems();

  /**
   * Get the user query.
   * 
   * @return The user query.
   */
  public abstract String getQuery();

  /**
   * Get the total number of items found.
   * 
   * @return The number.
   */
  public abstract long size();

  /**
   * Get the offset.
   * 
   * @return The offset.
   */
  public abstract long getOffset();

  /**
   * Get the limit.
   * 
   * @return The limit.
   */
  public abstract long getLimit();

  /**
   * Get the search time.
   * 
   * @return The time in ms.
   */
  public abstract long getSearchTime();

  /**
   * Get the page of the current result.
   * 
   * @return The page.
   */
  public abstract long getPage();

}
