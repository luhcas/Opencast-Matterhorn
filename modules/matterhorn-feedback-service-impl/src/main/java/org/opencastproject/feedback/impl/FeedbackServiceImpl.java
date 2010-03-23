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
package org.opencastproject.feedback.impl;

import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.spi.PersistenceProvider;

import org.opencastproject.feedback.api.Annotation;
import org.opencastproject.feedback.api.AnnotationList;
import org.opencastproject.feedback.api.FeedbackService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of org.opencastproject.feedback.api.FeedbackService
 * 
 * @see org.opencastproject.feedback.api.FeedbackService
 */
public class FeedbackServiceImpl implements FeedbackService {

  private static final Logger logger = LoggerFactory.getLogger(FeedbackServiceImpl.class);

  /**
   * The component context that is passed when activate is called
   */
  protected ComponentContext componentContext;

  /** The entity manager used for persisting Java objects. */
  protected EntityManager em = null;

  /**
   * @param persistenceProvider
   *          the persistenceProvider to set
   */
  public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }

  @SuppressWarnings("unchecked")
  protected Map persistenceProperties;

  /**
   * @param persistenceProperties
   *          the persistenceProperties to set
   */
  @SuppressWarnings("unchecked")
  public void setPersistenceProperties(Map persistenceProperties) {
    this.persistenceProperties = persistenceProperties;
  }

  /** The factory used to generate the entity manager */
  protected EntityManagerFactory emf = null;

  /**
   * The JPA provider
   */
  protected PersistenceProvider persistenceProvider;

  /**
   * This method will be called, when the bundle gets unloaded from OSGI
   */
  public void deactivate() {
  }

  /**
   * This method will be called, when the bundle gets loaded from OSGI
   * 
   * @param componentContext
   *          The ComponetnContext of the OSGI bundle
   */
  public void activate(ComponentContext componentContext) {
    logger.info("activation started.");
    if (componentContext == null) {
      logger.error("Could not activate because of missing ComponentContext");
      return;
    }
    this.componentContext = componentContext;
    emf = persistenceProvider.createEntityManagerFactory("org.opencastproject.feedback", persistenceProperties);
    em = emf.createEntityManager();
  }

  public void destroy() {
    em.close();
    emf.close();
  }

  public Annotation addAnnotation(Annotation a) {

    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      em.persist(a);
      tx.commit();
      return a;
    } finally {
      if (tx.isActive()) {
        tx.rollback();
      }
    }

  }

  public AnnotationList getAnnotationsByKey(String key, int offset, int limit) {
    return null;
  }
}
