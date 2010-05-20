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
package org.opencastproject.series.impl;

import java.util.Dictionary;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.persistence.spi.PersistenceProvider;

import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.series.api.Series;
import org.opencastproject.series.api.SeriesService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Comment me!
 *
 */
public class SeriesServiceImpl implements SeriesService {

  /** 
   * Properties that are updated by ManagedService updated method
   */
  @SuppressWarnings("unchecked")
  protected Dictionary properties;
  
  /**
   * The component context that is passed when activate is called
   */
  protected ComponentContext componentContext; 
  
  private static final Logger logger = LoggerFactory.getLogger(SeriesServiceImpl.class);
  
  protected PersistenceProvider persistenceProvider;
  protected Map<String, Object> persistenceProperties;
  protected EntityManagerFactory emf = null;
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.series.api.SeriesService#addSeries(org.opencastproject.series.api.Series)
   */
  @Override
  public boolean addSeries(Series s) {
    s = makeIdUnique(s);
    
    EntityManager em = emf.createEntityManager();
    try {
      EntityTransaction tx = em.getTransaction();
      tx.begin();
      em.persist(s);
      tx.commit();
    } finally {
      em.close();
    } 
    // TODO Auto-generated method stub
    return false;
  }

  protected Series makeIdUnique (Series series) {
    EntityManager em = emf.createEntityManager();
    if (series.getSeriesId() == null) series.generateSeriesId();
    try {
      Series found = em.find(Series.class, series.getSeriesId());
      while (found != null) {
        series.generateSeriesId();
        found = em.find(Series.class, series.getSeriesId());
      }
    } finally {
      em.close();
    }
    return series;
  }  
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.series.api.SeriesService#getAllSeries()
   */
  @Override
  public List<Series> getAllSeries() {
    EntityManager em = emf.createEntityManager();
    Query query = em.createQuery("SELECT e FROM Series e");
    List<Series> series = null;
    try {
      series = (List<Series>) query.getResultList();
    } finally {
      em.close();
    }
    return series; 
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.series.api.SeriesService#getDublinCore(java.lang.String)
   */
  @Override
  public DublinCore getDublinCore(String seriesID) {
    return getSeries(seriesID).getDublinCore();
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.series.api.SeriesService#getSeries(java.lang.String)
   */
  @Override
  public Series getSeries(String seriesID) {
    logger.debug("loading series with the ID {}", seriesID);
    if (seriesID == null || emf == null) {
      logger.warn("could not find event {}. Null Pointer exeption");
      return null;
    }
    EntityManager em = emf.createEntityManager();
    Series s = null;
    try {
       s = em.find(Series.class, seriesID);
    } finally {
      em.close();
    }
    return s;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.series.api.SeriesService#newSeriesID()
   */
  @Override
  public String newSeriesID() {
    Series s = new Series();
    return s.generateSeriesId();
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.series.api.SeriesService#removeSeries(java.lang.String)
   */
  @Override
  public boolean removeSeries(String seriesID) {
    logger.debug("Removing event with the ID {}", seriesID);
    Series s;
    EntityManager em = emf.createEntityManager();
    try {
      em.getTransaction().begin();
      s = em.find(Series.class, seriesID);
      if (s == null) return false; // Event not in database
      em.remove(s);
      em.getTransaction().commit();
    } finally {
      em.close();
    }
    return true; 
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.series.api.SeriesService#updateSeries(org.opencastproject.series.api.Series)
   */
  @Override
  public boolean updateSeries(Series s) {
    EntityManager em = emf.createEntityManager();
    try {
      em.getTransaction().begin();
      Series storedSeries = em.find(Series.class, s.getSeriesId()); 
      if (storedSeries == null) return false; //nothing found to update
      storedSeries.setMetadata(s.getMetadata());
      em.merge(storedSeries);
      em.getTransaction().commit();
    } catch (Exception e1) {
      logger.warn("Could not update series {}. Reason: {}",s.getSeriesId(),e1.getMessage());
      return false;
    } finally {
      em.close();
    }
    return true;
  }
  
  public void activate (ComponentContext cc) {
    logger.info("SeriesService activated.");
    if (cc == null) {
      logger.warn("Could not activate because of missing ComponentContext");
      return;
    }
    this.componentContext = cc;
    
    emf = persistenceProvider.createEntityManagerFactory("org.opencastproject.series.api", persistenceProperties);
  }
  
  public void destroy() {
    emf.close();
  }   

}
