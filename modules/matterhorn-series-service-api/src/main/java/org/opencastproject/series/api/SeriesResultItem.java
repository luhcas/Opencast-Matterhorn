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

import java.util.Collection;
import java.util.Date;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * An item found in series search. Part of {@link SeriesResult}.
 * 
 */
@XmlJavaTypeAdapter(SeriesResultItemImpl.Adapter.class)
public interface SeriesResultItem {

  /**
   * @return series ID
   */
  String getId();

  /**
   * @return series title
   */
  String getTitle();

  /**
   * @return subject(s)
   */
  Collection<String> getSubject();

  /**
   * @return creator(s)
   */
  Collection<String> getCreator();

  /**
   * @return publisher(s)
   */
  Collection<String> getPublisher();

  /**
   * @return contributor(s)
   */
  Collection<String> getContributor();

  /**
   * @return abstract(s)
   */
  Collection<String> getAbstract();

  /**
   * @return description(s)
   */
  Collection<String> getDescription();

  /**
   * @return created
   */
  Date getCreated();

  /**
   * @return available from
   */
  Date getAvailableFrom();

  /**
   * @return available to
   */
  Date getAvailableTo();

  /**
   * @return language(s)
   */
  Collection<String> getLanguage();

  /**
   * @return rights holder(s)
   */
  Collection<String> getRightsHolder();

  /**
   * @return spatial(s)
   */
  Collection<String> getSpatial();

  /**
   * @return temporal(s)
   */
  Collection<String> getTemporal();

  /**
   * @return is part of
   */
  Collection<String> getIsPartOf();

  /**
   * @return replaces
   */
  Collection<String> getReplaces();

  /**
   * @return type(s)
   */
  Collection<String> getType();

  /**
   * @return access rights
   */
  Collection<String> getAccessRights();

  /**
   * @return license(s)
   */
  Collection<String> getLicense();
}
