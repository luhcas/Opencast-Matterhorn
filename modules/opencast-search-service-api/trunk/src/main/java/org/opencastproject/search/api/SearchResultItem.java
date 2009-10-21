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

package org.opencastproject.search.api;

import org.opencastproject.media.mediapackage.MediaPackage;

import java.util.Date;

/**
 * An item that was found as part of a search.  Typically a {@link SearchResultItem} will be included in a {@link SearchResult}
 */
public interface SearchResultItem {

  /**
   * A search result item can either represent an episode ({@link SearchResultItemType#AudioVisual}) or a series ({@link SearchResultItemType#Series})
   */
  public enum SearchResultItemType { AudioVisual, Series };
  
  /**
   * @return the id
   */
  public String getId();
  
  /**
   * Returns the media package that was used to create the entry in the search index.
   * 
   * @return the media package
   */
  public MediaPackage getMediaPackage();

  /**
   * @return the dcExtent
   */
  public long getDcExtent();

  /**
   * @return the dcTitle
   */
  public String getDcTitle();

  /**
   * @return the dcSubject
   */
  public String getDcSubject();

  /**
   * @return the dcCreator
   */
  public String getDcCreator();

  /**
   * @return the dcPublisher
   */
  public String getDcPublisher();

  /**
   * @return the dcContributor
   */
  public String getDcContributor();

  /**
   * @return the dcAbtract
   */
  public String getDcAbstract();

  /**
   * @return the dcCreated
   */
  public Date getDcCreated();

  /**
   * @return the dcAvailableFrom
   */
  public Date getDcAvailableFrom();

  /**
   * @return the dcAvailableTo
   */
  public Date getDcAvailableTo();

  /**
   * @return the dcLanguage
   */
  public String getDcLanguage();

  /**
   * @return the dcRightsHolder
   */
  public String getDcRightsHolder();

  /**
   * @return the dcSpatial
   */
  public String getDcSpatial();

  /**
   * @return the dcTemporal
   */
  public String getDcTemporal();

  /**
   * @return the dcIsPartOf
   */
  public String getDcIsPartOf();

  /**
   * @return the dcReplaces
   */
  public String getDcReplaces();

  /**
   * @return the dcType
   */
  public String getDcType();

  /**
   * @return the dcAccessRights
   */
  public String getDcAccessRights();

  /**
   * @return the dcLicense
   */
  public String getDcLicense();

  /**
   * @return the mediaType
   */
  public SearchResultItemType getType();

  /**
   * @return the keywords
   */
  public String[] getKeywords();

  /**
   * @return the cover
   */
  public String getCover();

  /**
   * @return the modified
   */
  public Date getModified();

  /**
   * @return the score
   */
  public double getScore();

  /**
   * Get the result item segment list.
   * 
   * @return The segment list.
   */
  public MediaSegment[] getSegments();

  /**
   * Get a certain segment by index.
   * 
   * @param number
   *          The segment number.
   * @return if segment is NIL returns null, else returns the segment
   */
  public MediaSegment getSegment(int number);

}
