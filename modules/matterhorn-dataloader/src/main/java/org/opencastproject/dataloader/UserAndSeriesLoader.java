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

import static org.opencastproject.security.api.SecurityConstants.DEFAULT_ORGANIZATION_ID;

import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogImpl;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.userdirectory.jpa.JpaUser;
import org.opencastproject.userdirectory.jpa.JpaUserAndRoleProvider;
import org.opencastproject.util.NotFoundException;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * A data loader to populate the series and JPA user provider with sample data.
 */
public class UserAndSeriesLoader {

  /** The logger */
  protected static final Logger logger = LoggerFactory.getLogger(UserAndSeriesLoader.class);

  /** The series service */
  protected SeriesService seriesService = null;

  /** The JPA-based user provider, which includes an addUser() method */
  protected JpaUserAndRoleProvider jpaUserProvider = null;

  /**
   * Callback on component activation.
   */
  protected void activate(ComponentContext cc) {

    // Get properties from the bundle context
    String adminUsername = StringUtils.trimToNull(cc.getBundleContext().getProperty(
            "org.opencastproject.security.demo.admin.user"));
    String adminUserPass = StringUtils.trimToNull(cc.getBundleContext().getProperty(
            "org.opencastproject.security.demo.admin.pass"));
    String adminUserRoles = StringUtils.trimToNull(cc.getBundleContext().getProperty(
            "org.opencastproject.security.demo.admin.roles"));
    String loadUsers = StringUtils.trimToNull(cc.getBundleContext().getProperty(
            "org.opencastproject.security.demo.loadusers"));

    // Load the admin user, if necessary
    if (jpaUserProvider.loadUser(adminUsername) == null && StringUtils.isNotBlank(adminUsername)
            && StringUtils.isNotBlank(adminUserPass) && StringUtils.isNotBlank(adminUserRoles)) {
      String[] roleArray = StringUtils.split(adminUserRoles, ',');
      Set<String> roles = new HashSet<String>();
      for (int i = 0; i < roleArray.length; i++)
        roles.add(StringUtils.trim(roleArray[i]));
      jpaUserProvider.addUser(new JpaUser(adminUsername, adminUserPass, DEFAULT_ORGANIZATION_ID, roles));
    }

    // Load the other users, if necessary
    if (Boolean.valueOf(loadUsers)) {
      // Load 100 series and 1000 users, but don't block activation
      new Loader().start();
    }
  }

  protected class Loader extends Thread {
    @Override
    public void run() {
      logger.info("Adding sample series...");
      for (int i = 0; i < 100; i++) {
        String seriesId = "series_" + i;
        DublinCoreCatalog dc = DublinCoreCatalogImpl.newInstance();
        AccessControlList acl = new AccessControlList(new AccessControlEntry("ROLE_SERIES_" + i, "read", true));
        try {
          dc.set(DublinCore.PROPERTY_IDENTIFIER, seriesId);
          dc.set(DublinCore.PROPERTY_TITLE, "Series #" + i);
          dc.set(DublinCore.PROPERTY_CREATOR, "Creator #" + i);
          dc.set(DublinCore.PROPERTY_CONTRIBUTOR, "Contributor #" + i);
          seriesService.updateSeries(dc);
          seriesService.updateAccessControl(seriesId, acl);
          logger.debug("Added series {}", dc);
        } catch (SeriesException e) {
          logger.warn("Unable to create series {}", dc);
        } catch (NotFoundException e) {
          logger.warn("Unable to find series {}", dc);
        }
      }

      // Load 1000 users, all with ROLE_USER and a role in the series
      logger.info("Adding sample users...");
      for (int i = 0; i < 1000; i++) {
        if (jpaUserProvider.loadUser("user" + i) == null) {
          Set<String> roleSet = new HashSet<String>();
          roleSet.add("ROLE_USER");
          roleSet.add("ROLE_SERIES_" + (i % 100));
          JpaUser user = new JpaUser("user" + i, "pass" + i, DEFAULT_ORGANIZATION_ID, roleSet);
          try {
            jpaUserProvider.addUser(user);
            logger.debug("Added {}", user);
          } catch (Exception e) {
            logger.warn("Can not add {}: {}", user, e);
          }
        }
      }
      logger.info("Finished loading sample series and users");
    }
  }

  /**
   * @param jpaUserProvider
   *          the jpaUserProvider to set
   */
  public void setJpaUserProvider(JpaUserAndRoleProvider jpaUserProvider) {
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
