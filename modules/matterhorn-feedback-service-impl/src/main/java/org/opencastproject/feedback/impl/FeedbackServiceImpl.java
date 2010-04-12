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

import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.spi.PersistenceProvider;

import org.opencastproject.feedback.api.Annotation;
import org.opencastproject.feedback.api.AnnotationList;
import org.opencastproject.feedback.api.FeedbackService;
import org.opencastproject.feedback.endpoint.AnnotationListImpl;
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

  @SuppressWarnings("unchecked")
  public AnnotationList getAnnotations(int offset, int limit) {
    AnnotationList result = new AnnotationListImpl();

    result.setTotal(getTotal());
    result.setOffset(offset);
    result.setLimit(limit);

    Query q = em.createNamedQuery("findAnnotations");
    q.setFirstResult(offset);
    q.setMaxResults(limit);
    Collection<Annotation> annotations = q.getResultList();

    for (Annotation a : annotations) {
      result.add(a);
    }

    return result;
  }

  private int getTotal() {
    Query q = em.createNamedQuery("findTotal");
    return ((Long) q.getSingleResult()).intValue();
  }

  @SuppressWarnings("unchecked")
  public AnnotationList getAnnotationsByKey(String key, int offset, int limit) {
    AnnotationList result = new AnnotationListImpl();

    result.setTotal(getTotal(key));
    result.setOffset(offset);
    result.setLimit(limit);

    Query q = em.createNamedQuery("findAnnotationsByKey");
    q.setParameter("key", key);
    q.setFirstResult(offset);
    q.setMaxResults(limit);
    Collection<Annotation> annotations = q.getResultList();

    for (Annotation a : annotations) {
      result.add(a);
    }

    return result;
  }

  private int getTotal(String key) {

    Query q = em.createNamedQuery("findTotalByKey");
    q.setParameter("key", key);
    return ((Long) q.getSingleResult()).intValue();
  }

  @SuppressWarnings("unchecked")
  public AnnotationList getAnnotationsByKeyAndDay(String key, String day, int offset, int limit) {
    AnnotationList result = new AnnotationListImpl();

    int year = Integer.parseInt(day.substring(0, 4));
    int month = Integer.parseInt(day.substring(4, 6)) - 1;
    int date = Integer.parseInt(day.substring(6, 8));

    Calendar calBegin = new GregorianCalendar();
    calBegin.set(year, month, date, 0, 0);
    Calendar calEnd = new GregorianCalendar();
    calEnd.set(year, month, date, 23, 59);

    result.setTotal(getTotal(key, calBegin, calEnd));
    result.setOffset(offset);
    result.setLimit(limit);

    Query q = em.createNamedQuery("findAnnotationsByKeyAndIntervall");
    q.setParameter("key", key);
    q.setParameter("begin", calBegin, TemporalType.TIMESTAMP);
    q.setParameter("end", calEnd, TemporalType.TIMESTAMP);
    q.setFirstResult(offset);
    q.setMaxResults(limit);
    Collection<Annotation> annotations = q.getResultList();

    for (Annotation a : annotations) {
      result.add(a);
    }

    return result;
  }

  private int getTotal(String key, Calendar calBegin, Calendar calEnd) {

    Query q = em.createNamedQuery("findTotalByKeyAndIntervall");
    q.setParameter("key", key);
    q.setParameter("begin", calBegin, TemporalType.TIMESTAMP);
    q.setParameter("end", calEnd, TemporalType.TIMESTAMP);
    return ((Long) q.getSingleResult()).intValue();
  }

  @SuppressWarnings("unchecked")
  public AnnotationList getAnnotationsByDay(String day, int offset, int limit) {
    AnnotationList result = new AnnotationListImpl();

    int year = Integer.parseInt(day.substring(0, 4));
    int month = Integer.parseInt(day.substring(4, 6)) - 1;
    int date = Integer.parseInt(day.substring(6, 8));

    Calendar calBegin = new GregorianCalendar();
    calBegin.set(year, month, date, 0, 0);
    Calendar calEnd = new GregorianCalendar();
    calEnd.set(year, month, date, 23, 59);

    result.setTotal(getTotal(calBegin, calEnd));
    result.setOffset(offset);
    result.setLimit(limit);

    Query q = em.createNamedQuery("findAnnotationsByIntervall");
    q.setParameter("begin", calBegin, TemporalType.TIMESTAMP);
    q.setParameter("end", calEnd, TemporalType.TIMESTAMP);
    q.setFirstResult(offset);
    q.setMaxResults(limit);
    Collection<Annotation> annotations = q.getResultList();

    for (Annotation a : annotations) {
      result.add(a);
    }

    return result;
  }

  private int getTotal(Calendar calBegin, Calendar calEnd) {

    Query q = em.createNamedQuery("findTotalByIntervall");
    q.setParameter("begin", calBegin, TemporalType.TIMESTAMP);
    q.setParameter("end", calEnd, TemporalType.TIMESTAMP);
    return ((Long) q.getSingleResult()).intValue();
  }

}
