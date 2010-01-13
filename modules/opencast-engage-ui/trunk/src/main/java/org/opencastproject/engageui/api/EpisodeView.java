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
package org.opencastproject.engageui.api;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * A class that represents the items that are on the browse media page
 * 
 */
@XmlJavaTypeAdapter(EpisodeViewImpl.Adapter.class)
public interface EpisodeView {

  /**
   * returns the title of the recording
   * 
   * @return String Title of recording
   */
  String getDcTitle();

  /**
   * sets the title
   * 
   * @param title
   */
  void setDcTitle(String dcTitle);

  /**
   * returns the contributor of the recording
   * 
   * @return String contributor
   */
  String getDcContributor();

  /**
   * sets the contributor
   * 
   * @param contributor
   */
  void setDcContributor(String dcContributor);

  /**
   * returns the abstract of the recording
   * 
   * @return String abstract
   */
  String getDcAbstract();

  /**
   * sets the abstract
   * 
   * @param abstract
   */
  void setDcAbstract(String dcAbstract);

  /**
   * returns the cover of the recording
   * 
   * @return String cover
   */
  String getCover();

  /**
   * sets the cover
   * 
   * @param cover
   */
  void setCover(String cover);

  /**
   * sets the mediaPackageID
   * 
   * @param mediaPackageID
   */
  void setURLEncodedMediaPackageId(String mediaPackageId);

  /**
   * returns the mediaPackageId of the episode
   * 
   * @return String mediaPackageId
   */
  String getURLEncodedMediaPackageId();

  /**
   * sets the creation date
   * 
   * @param creation
   *          date
   */
  void setDcCreated(String date);

  /**
   * returns the creation date of the episode
   * 
   * @return String creation date
   */
  String getDcCreated();

  /**
   * sets the rights holder
   * 
   * @param rights holder
   */
  void setDcRightsHolder(String dcRightsHolder);

  /**
   * returns the rights holder of the episode
   * 
   * @return String rights holder
   */
  String getDcRightsHolder();
  
  /**
   * sets the creator
   * 
   * @param creator
   */
  void setDcCreator(String dcCreator);

  /**
   * returns the creator of the episode
   * 
   * @return String creator
   */
  String getDcCreator();

}
