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

import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogImpl;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.userdirectory.jpa.JpaUser;
import org.opencastproject.userdirectory.jpa.JpaUserProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * A data loader to populate the series and JPA user provider with sample data.
 */
public class UserAndSeriesLoader {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(UserAndSeriesLoader.class);

  /** The series service */
  protected SeriesService seriesService = null;

  /** The JPA-based user provider, which includes an addUser() method */
  protected JpaUserProvider jpaUserProvider = null;

  /**
   * Callback on component activation.
   */
  protected void activate() {
    // Load 100 series
    for (int i = 0; i < 100; i++) {
      DublinCoreCatalog dc = DublinCoreCatalogImpl.newInstance();
      try {
        dc.set(DublinCore.PROPERTY_IDENTIFIER, "series_" + i);
        dc.set(DublinCore.PROPERTY_TITLE, "Series #" + i);
        dc.set(DublinCore.PROPERTY_CREATOR, "Creator #" + i);
        dc.set(DublinCore.PROPERTY_CONTRIBUTOR, "Contributor #" + i);
        seriesService.updateSeries(dc);
        logger.info("Added series {}", dc);
      } catch (SeriesException e) {
        logger.warn("Unable to create series {}", dc);
      }
    }

    // Load 1000 users, all with ROLE_USER and a role in the series
    for (int i = 0; i < 1000; i++) {
      Set<String> roleSet = new HashSet<String>();
      roleSet.add("ROLE_USER");
      roleSet.add("ROLE_SERIES_" + (i % 100));
      JpaUser user = new JpaUser("user" + i, "pass" + i, roleSet);
      try {
        jpaUserProvider.addUser(user);
        logger.info("Added {}", user);
      } catch (Exception e) {
        logger.warn("Can not add {}", user);
      }
    }

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
