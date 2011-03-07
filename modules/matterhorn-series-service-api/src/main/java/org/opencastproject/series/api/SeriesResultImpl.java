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

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Implements {@link SeriesResult}.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "series-results", namespace = "http://series.opencastproject.org/")
public class SeriesResultImpl implements SeriesResult {

  /** Result set */
  @XmlElement(name = "result-item")
  private List<SeriesResultItem> resultSet;
  /** Result offset */
  @XmlAttribute(name = "startPage")
  private long startPage;
  /** Maximum result returned */
  @XmlAttribute(name = "count")
  private long pageSize;
  /** Search time in milliseconds */
  @XmlAttribute(name = "searchTime")
  private long searchTime;
  /** Total number of results corresponding query */
  @XmlAttribute(name = "totalCount")
  private long numberOfItems;

  /** Needed for JAXB */
  public SeriesResultImpl() {
  }

  /**
   * Add search result to the set
   * 
   * @param item
   *          {@link SeriesResultItem} representing one matching item
   */
  public void addItem(SeriesResultItem item) {
    if (item == null) {
      throw new IllegalArgumentException("Parameter item cannot be null");
    }
    if (resultSet == null) {
      resultSet = new LinkedList<SeriesResultItem>();
    }
    resultSet.add(item);
  }

  /**
   * Set result offset
   * 
   * @param startPage
   */
  public void setStartPage(long startPage) {
    this.startPage = startPage;
  }

  /**
   * Set result page size
   * 
   * @param pageSize
   */
  public void setPageSize(long pageSize) {
    this.pageSize = pageSize;
  }

  /**
   * Set search time in milliseconds
   * 
   * @param searchTime
   */
  public void setSearchTime(long searchTime) {
    this.searchTime = searchTime;
  }

  /**
   * Set number of items corresponding query
   * 
   * @param numberOfItems
   */
  public void setNumberOfItems(long numberOfItems) {
    this.numberOfItems = numberOfItems;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesQueryResult#getNumberOfItems()
   */
  @Override
  public long getNumberOfItems() {
    return numberOfItems;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesQueryResult#getSearchTime()
   */
  @Override
  public long getSearchTime() {
    return searchTime;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesQueryResult#getStartPage()
   */
  @Override
  public long getStartPage() {
    return startPage;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesQueryResult#getPageSize()
   */
  @Override
  public long getPageSize() {
    return pageSize;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesQueryResult#getItems()
   */
  @Override
  public SeriesResultItem[] getItems() {
    return resultSet == null ? new SeriesResultItem[0] : resultSet.toArray(new SeriesResultItem[resultSet.size()]);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesQueryResult#getSize()
   */
  @Override
  public long getSize() {
    return resultSet == null ? 0 : resultSet.size();
  }

  static class Adapter extends XmlAdapter<SeriesResultImpl, SeriesResult> {

    /*
     * (non-Javadoc)
     * 
     * @see javax.xml.bind.annotation.adapters.XmlAdapter#unmarshal(java.lang.Object)
     */
    @Override
    public SeriesResult unmarshal(SeriesResultImpl v) throws Exception {
      return v;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.xml.bind.annotation.adapters.XmlAdapter#marshal(java.lang.Object)
     */
    @Override
    public SeriesResultImpl marshal(SeriesResult v) throws Exception {
      return (SeriesResultImpl) v;
    }

  }
}
