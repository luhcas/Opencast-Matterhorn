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

import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserProvider;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.spi.PersistenceProvider;

/**
 * Manages and locates users using JPA.
 */
public class JpaUserAndRoleProvider implements UserProvider, RoleProvider {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(JpaUserAndRoleProvider.class);

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

  /**
   * Callback for activation of this component.
   * 
   * @param cc
   *          the component context
   */
  public void activate(ComponentContext cc) {
    logger.debug("activate");

    // Set up persistence
    emf = persistenceProvider.createEntityManagerFactory("org.opencastproject.userdirectory", persistenceProperties);
  }

  /**
   * Callback for deactivation of this component.
   */
  public void deactivate() {
    if (emf != null && emf.isOpen()) {
      emf.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.UserProvider#loadUser(java.lang.String)
   */
  @Override
  public User loadUser(String userName) {
    EntityManager em = emf.createEntityManager();
    try {
      JpaUser user = em.find(JpaUser.class, userName);
      if (user == null) {
        return null;
      } else {
        Set<String> roles = user.getRoles();
        return new User(userName, user.getPassword(), user.getOrganization(), roles.toArray(new String[roles.size()]));
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

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.RoleDirectoryService#getRoles()
   */
  @Override
  public String[] getRoles() {
    EntityManager em = emf.createEntityManager();
    try {
      Query q = em.createNamedQuery("roles");
      @SuppressWarnings("unchecked")
      List<String> results = q.getResultList();
      return results.toArray(new String[results.size()]);
    } finally {
      em.close();
    }
  }

  /**
   * Creates or updates a mapping from an application role to a local role. If <code>localRole</code> is null, any
   * existing mapping will be removed.
   */
  public void setRoleMapping(String applicationRole, String localRole) {
    if (applicationRole == null)
      throw new IllegalArgumentException("applicationRole can not be null");
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    JpaRoleMapping mapping = null;
    try {
      tx.begin();
      Query q = em.createNamedQuery("role");
      q.setParameter("applicationRole", applicationRole);
      mapping = (JpaRoleMapping) q.getSingleResult();
      if (localRole == null) {
        em.remove(mapping);
      } else {
        mapping.setLocalRole(localRole);
        em.merge(mapping);
      }
    } catch (NoResultException e) {
      // there is no mapping for this application role
      mapping = new JpaRoleMapping(applicationRole, localRole);
      em.persist(mapping);
    } finally {
      tx.commit();
      em.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.RoleProvider#getLocalRole(java.lang.String)
   */
  @Override
  public String getLocalRole(String role) {
    EntityManager em = emf.createEntityManager();
    try {
      Query q = em.createNamedQuery("role");
      q.setParameter("applicationRole", role);
      JpaRoleMapping mapping = (JpaRoleMapping) q.getSingleResult();
      return mapping.getLocalRole();
    } catch (NoResultException e) {
      return null;
    } finally {
      em.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.UserProvider#getOrganization()
   */
  @Override
  public String getOrganization() {
    return SecurityConstants.DEFAULT_ORGANIZATION_ID;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return getClass().getName();
  }

}
