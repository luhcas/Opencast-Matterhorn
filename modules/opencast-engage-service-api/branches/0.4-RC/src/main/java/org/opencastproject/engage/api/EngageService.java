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

/**
 * FIXME -- Add javadocs
 */
public interface EngageService {

  /** 
   * Returns an HTML page with a player that plays filename.
   * @param filename
   * @deprecated
   * @return HTML page
   */
  //String deliverPlayer(String filename, String mediaHost);
  
  /**
   * Returns an HTML page that lists all available mediafiles.
   * @deprecated
   * @return HTML page
   */
   //String listRecordings();
  
  /** 
   * Returns an HTML page with list of all available media packages in the search index.
   * @param videoUrl
   * @return HTML page
   */
  String getEpisodesByDate(int limit, int offset);
  
  /** 
   * Returns an HTML page with a player that plays the mediaPackageId.
   * @param episodeId
   * @return HTML page
   */
  String deliverPlayer(String episodeId);
  
  /** 
   * Returns an HTML page with the latest available episodes
   * @return HTML page
   */
  String deliverBrowsePage();
  
}
