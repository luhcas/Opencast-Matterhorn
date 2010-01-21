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
package org.opencastproject.engage.api;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * A class that represents the items that are on the browse media page
 *
 */
@XmlJavaTypeAdapter(EpisodeViewImpl.Adapter.class)
public interface EpisodeView {

  /**
   * returns the title of the recording
   * @return String Title of recording
   */ 
  String getTitle();

  /**
   * sets the title
   * @param title
   */
  void setTitle(String title);
  
  /**
   * sets the mediaPackageID
   * @param mediaPackageID
   */
  void setURLEncodedMediaPackageId(String mediaPackageId);

  /**
   * returns the mediaPackageId of the episode
   * @return String mediaPackageId
   */
  String getURLEncodedMediaPackageId();

}
