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
import java.util.List;
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
import org.opencastproject.feedback.api.Footprint;
import org.opencastproject.feedback.api.FootprintList;
import org.opencastproject.feedback.api.Report;
import org.opencastproject.feedback.api.ReportItem;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.feedback.endpoint.AnnotationListImpl;
import org.opencastproject.feedback.endpoint.FootprintImpl;
import org.opencastproject.feedback.endpoint.FootprintsListImpl;
import org.opencastproject.feedback.endpoint.ReportImpl;
import org.opencastproject.feedback.endpoint.ReportItemImpl;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of org.opencastproject.feedback.api.FeedbackService
 * 
 * @see org.opencastproject.feedback.api.FeedbackService
 */
public class FeedbackServiceImpl implements FeedbackService {

  public static final String FOOTPRINT_KEY = "FOOTPRINT";

  private static final Logger logger = LoggerFactory.getLogger(FeedbackServiceImpl.class);

  private SearchService searchService;

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

  public int getViews(String mediapackageId) {
    Query q = em.createNamedQuery("countSessionsOfMediapackage");
    q.setParameter("mediapackageId", mediapackageId);
    return ((Long) q.getSingleResult()).intValue();
  }

  @SuppressWarnings("unchecked")
  public Annotation addAnnotation(Annotation a) {
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      Query q = em.createNamedQuery("findLastAnnotationsOfSession");
      q.setMaxResults(1);
      q.setParameter("sessionId", a.getSessionId());
      Collection<Annotation> annotations = q.getResultList();

      if (annotations.size() >= 1) {
        Annotation last = annotations.iterator().next();
        if (last.getMediapackageId().equals(a.getMediapackageId()) && last.getKey().equals(a.getKey())
                && last.getOutpoint() == a.getInpoint()) {
          last.setOutpoint(a.getOutpoint());
          a = last;
        } else {
          em.persist(a);
        }

      } else {
        em.persist(a);
      }
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

  public AnnotationList getAnnotationsByKeyAndMediapackageId(String key, String mediapackageId, int offset, int limit) {
    AnnotationList result = new AnnotationListImpl();

    result.setTotal(getTotal(key, mediapackageId));
    result.setOffset(offset);
    result.setLimit(limit);

    Query q = em.createNamedQuery("findAnnotationsByKeyAndMediapackageId");
    q.setParameter("key", key);
    q.setParameter("mediapackageId", mediapackageId);
    q.setFirstResult(offset);
    q.setMaxResults(limit);
    Collection<Annotation> annotations = q.getResultList();

    for (Annotation a : annotations) {
      result.add(a);
    }

    return result;
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

  private int getTotal(String key, String mediapackageId) {

    Query q = em.createNamedQuery("findTotalByKeyAndMediapackageId");
    q.setParameter("key", key);
    q.setParameter("mediapackageId", mediapackageId);
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
  
  private int getDistinctEpisodeIdTotal(Calendar calBegin, Calendar calEnd) {
    Query q = em.createNamedQuery("findDistinctEpisodeIdTotalByIntervall");
    q.setParameter("begin", calBegin, TemporalType.TIMESTAMP);
    q.setParameter("end", calEnd, TemporalType.TIMESTAMP);
    return ((Long) q.getSingleResult()).intValue();
  }

  public Report getReport(int offset, int limit) {
    Report report = new ReportImpl();
    report.setLimit(limit);
    report.setOffset(offset);

    Query q = em.createNamedQuery("countSessionsGroupByMediapackage");
    q.setFirstResult(offset);
    q.setMaxResults(limit);

    List<Object[]> result = q.getResultList();
    ReportItem item;

    for (Object[] a : result) {
      item = new ReportItemImpl();
      item.setEpisodeId((String) a[0]);
      item.setViews((Long) a[1]);
      item.setPlayed((Long) a[2]);
      report.add(item);
    }

    return report;
  }

  public Report getReport(String from, String to, int offset, int limit) {
    Report report = new ReportImpl();
    report.setLimit(limit);
    report.setOffset(offset);

    int year = Integer.parseInt(from.substring(0, 4));
    int month = Integer.parseInt(from.substring(4, 6)) - 1;
    int date = Integer.parseInt(from.substring(6, 8));
    Calendar calBegin = new GregorianCalendar();
    calBegin.set(year, month, date, 0, 0);

    year = Integer.parseInt(to.substring(0, 4));
    month = Integer.parseInt(to.substring(4, 6)) - 1;
    date = Integer.parseInt(to.substring(6, 8));
    Calendar calEnd = new GregorianCalendar();
    calEnd.set(year, month, date, 23, 59);

    report.setTotal(getDistinctEpisodeIdTotal(calBegin, calEnd));
    Query q = em.createNamedQuery("countSessionsGroupByMediapackageByIntervall");
    q.setParameter("begin", calBegin, TemporalType.TIMESTAMP);
    q.setParameter("end", calEnd, TemporalType.TIMESTAMP);
    q.setFirstResult(offset);
    q.setMaxResults(limit);

    List<Object[]> result = q.getResultList();
    ReportItem item;

    for (Object[] a : result) {
      item = new ReportItemImpl();
      item.setEpisodeId((String) a[0]);
      item.setViews((Long) a[1]);
      item.setPlayed((Long) a[2]);
      report.add(item);
    }

    return report;
  }

  public FootprintList getFootprints(String mediapackageId, String userId) {
    FootprintList result = new FootprintsListImpl();

    Query q = em.createNamedQuery("findAnnotationsByKeyAndMediapackageIdOrderByOutpointDESC");
    q.setParameter("key", FOOTPRINT_KEY);
    q.setParameter("mediapackageId", mediapackageId);
    Collection<Annotation> annotations = q.getResultList();

    int[] resultArray = new int[1];
    boolean first = true;

    for (Annotation a : annotations) {
      if (first) {
        // Get one more item than the known outpoint to append a footprint of 0 views at the end of the result set
        resultArray = new int[a.getOutpoint() + 1];
        first = false;
      }
      for (int i = a.getInpoint(); i < a.getOutpoint(); i++) {
        resultArray[i]++;
      }
    }

    FootprintList list = new FootprintsListImpl();
    int current, last = -1;
    int lastPositionAdded = -1;
    for (int i = 0; i < resultArray.length; i++) {
      current = resultArray[i];
      if (last != current) {
        Footprint footprint = new FootprintImpl();
        footprint.setPosition(i);
        footprint.setViews(current);
        list.add(footprint);
        lastPositionAdded = i;
      }
      last = current;
    }

    return list;
  }

  public void setSearchService(SearchService searchService) {
    this.searchService = searchService;
  }
}
