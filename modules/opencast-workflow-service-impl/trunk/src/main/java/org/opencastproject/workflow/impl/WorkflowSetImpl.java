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

package org.opencastproject.workflow.impl;

import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * The search result represents a set of result items that has been compiled as a result for a search operation.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name="search-result", namespace="http://search.opencastproject.org/")
public class WorkflowSetImpl implements WorkflowSet {

  /** Logging facility */
  static Logger log_ = LoggerFactory.getLogger(WorkflowSetImpl.class);

  /** A list of search items. */
  @XmlElementWrapper(name="search-results")
  private List<WorkflowInstance> resultSet = null;

  /** The query that yielded the result set */
  @XmlElement
  private String query = null;

  /** The pagination offset. */
  @XmlAttribute
  private long offset = 0;

  /** The pagination limit. Default is 10. */
  @XmlAttribute
  private long limit = 10;

  /** The search time in milliseconds */
  @XmlAttribute
  private long searchTime = 0;

  /**
   * A no-arg constructor needed by JAXB
   */
  public WorkflowSetImpl() {}

  /**
   * Creates a new and empty search result.
   * 
   * @param query
   *          the query
   */
  public WorkflowSetImpl(String query) {
    if (query == null)
      throw new IllegalArgumentException("Quey cannot be null");
    this.query = query;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.WorkflowSet.impl.SearchResult#getItems()
   */
  public WorkflowInstance[] getItems() {
    return resultSet.toArray(new WorkflowInstance[resultSet.size()]);
  }

  /**
   * Adds an item to the result set.
   * 
   * @param item
   *          the item to add
   */
  public void addItem(WorkflowInstance item) {
    if (item == null)
      throw new IllegalArgumentException("Parameter item cannot be null");
    if (resultSet == null)
      resultSet = new ArrayList<WorkflowInstance>();
    resultSet.add((WorkflowInstance)item);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.WorkflowSet.impl.SearchResult#getQuery()
   */
  public String getQuery() {
    return query;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.WorkflowSet.impl.SearchResult#size()
   */
  public long size() {
    return resultSet != null ? resultSet.size() : 0;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.WorkflowSet.impl.SearchResult#getOffset()
   */
  public long getOffset() {
    return offset;
  }

  /**
   * Set the offset.
   * 
   * @param offset
   *          The offset.
   */
  public void setOffset(long offset) {
    this.offset = offset;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.WorkflowSet.impl.SearchResult#getLimit()
   */
  public long getLimit() {
    return limit;
  }

  /**
   * Set the limit.
   * 
   * @param limit
   *          The limit.
   */
  public void setLimit(long limit) {
    this.limit = limit;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.WorkflowSet.impl.SearchResult#getSearchTime()
   */
  public long getSearchTime() {
    return searchTime;
  }

  /**
   * Set the search time.
   * 
   * @param searchTime
   *          The time in ms.
   */
  public void setSearchTime(long searchTime) {
    this.searchTime = searchTime;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.WorkflowSet.impl.SearchResult#getPage()
   */
  public long getPage() {
    if (limit != 0)
      return offset / limit;
    return 0;
  }

}
