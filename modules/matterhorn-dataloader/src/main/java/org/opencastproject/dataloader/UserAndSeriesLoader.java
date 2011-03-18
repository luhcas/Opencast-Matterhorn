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
package org.opencastproject.dataloader;

import org.opencastproject.series.api.SeriesService;
import org.opencastproject.userdirectory.jpa.JpaUserProvider;

/**
 * A data loader to populate the series and JPA user provider with sample data.
 */
public class UserAndSeriesLoader {

  /** The series service */
  protected SeriesService seriesService = null;

  /** The JPA-based user provider, which includes an addUser() method */
  protected JpaUserProvider jpaUserProvider = null;

  /**
   * Callback on component activation.
   */
  protected void activate() {
    
  }

  /**
   * @param jpaUserProvider
   *          the jpaUserProvider to set
   */
  public void setJpaUserProvider(JpaUserProvider jpaUserProvider) {
    this.jpaUserProvider = jpaUserProvider;
  }

  /**
   * @param seriesService
   *          the seriesService to set
   */
  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

}
