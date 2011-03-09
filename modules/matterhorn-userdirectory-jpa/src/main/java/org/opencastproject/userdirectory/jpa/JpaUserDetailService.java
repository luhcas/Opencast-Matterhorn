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
package org.opencastproject.userdirectory.jpa;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.spi.PersistenceProvider;

/**
 * Manages and locates users using JPA.
 */
public class JpaUserDetailService implements UserDetailsService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(JpaUserDetailService.class);

  /** The JPA provider */
  protected PersistenceProvider persistenceProvider;

  /**
   * @param persistenceProvider
   *          the persistenceProvider to set
   */
  public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }

  @SuppressWarnings("rawtypes")
  protected Map persistenceProperties;

  /**
   * @param persistenceProperties
   *          the persistenceProperties to set
   */
  @SuppressWarnings("rawtypes")
  public void setPersistenceProperties(Map persistenceProperties) {
    this.persistenceProperties = persistenceProperties;
  }

  /** The factory used to generate the entity manager */
  protected EntityManagerFactory emf = null;

  public void activate(ComponentContext cc) {
    logger.debug("activate");

    // Set up persistence
    emf = persistenceProvider.createEntityManagerFactory("org.opencastproject.userdirectory", persistenceProperties);

    // Ensure that the inter-server remoting account exists
    if (cc != null) {

      // TODO: replace this with proper account provisioning

      Set<GrantedAuthority> authorities = new HashSet<GrantedAuthority>();
      authorities.add(new GrantedAuthorityImpl("ROLE_ADMIN"));
      authorities.add(new GrantedAuthorityImpl("ROLE_USER"));

      String username = cc.getBundleContext().getProperty("org.opencastproject.security.digest.user");
      String pass = cc.getBundleContext().getProperty("org.opencastproject.security.digest.pass");

      try {
        loadUserByUsername(username);
      } catch (UsernameNotFoundException e) {
        JpaUser systemAccount = new JpaUser(username, pass, true, true, true, true, authorities);
        addUser(systemAccount);
      }

      try {
        loadUserByUsername("admin");
      } catch (UsernameNotFoundException e) {
        JpaUser adminUser = new JpaUser("admin", "opencast", true, true, true, true, authorities);
        logger.warn("Automatically adding the 'admin' user for development purposes (needs to be removed)");
        addUser(adminUser);
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.springframework.security.core.userdetails.UserDetailsService#loadUserByUsername(java.lang.String)
   */
  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessException {
    EntityManager em = emf.createEntityManager();
    try {
      JpaUser user = em.find(JpaUser.class, username);
      if (user == null) {
        throw new UsernameNotFoundException(username);
      } else {
        return user;
      }
    } finally {
      em.close();
    }
  }

  /**
   * A utility class to load the user directory.
   * 
   * @param user
   *          the user object
   */
  public void addUser(JpaUser user) {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = null;
    try {
      tx = em.getTransaction();
      tx.begin();
      em.persist(user);
      tx.commit();
    } finally {
      if (tx.isActive()) {
        tx.rollback();
      }
      em.close();
    }
  }

}
