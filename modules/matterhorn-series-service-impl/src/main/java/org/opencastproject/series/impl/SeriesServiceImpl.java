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

import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.series.api.Series;
import org.opencastproject.series.api.SeriesMetadata;
import org.opencastproject.series.api.SeriesService;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.persistence.spi.PersistenceProvider;

/**
 * TODO: Comment me!
 *
 */
public class SeriesServiceImpl implements SeriesService, ManagedService {

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
  
  public SeriesServiceImpl () {
    logger.info("Series Service instantiated");
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.series.api.SeriesService#addSeries(org.opencastproject.series.api.Series)
   */
  @Override
  public boolean addSeries(Series s) {
    if (s == null) return false;
    s = makeIdUnique(s);
    
    EntityManager em = emf.createEntityManager();
    try {
      EntityTransaction tx = em.getTransaction();
      tx.begin();
      em.persist(s);
      tx.commit();
    } catch (Exception e) {
      logger.warn("Problem to add series {}", s.getSeriesId());
      return false;
    }
    finally {
      em.close();
    } 
    return true;
  }

  protected Series makeIdUnique (Series series) {
    EntityManager em = emf.createEntityManager();
    if (series.getSeriesId() == null || series.getSeriesId().length() == 0) series.generateSeriesId();
    try {
      Series found = em.find(SeriesImpl.class, series.getSeriesId());
      while (found != null) {
        series.generateSeriesId();
        found = em.find(SeriesImpl.class, series.getSeriesId());
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
  @SuppressWarnings("unchecked")
  @Override
  public List<Series> getAllSeries() {
    EntityManager em = emf.createEntityManager();
    Query query = em.createQuery("SELECT e FROM SeriesImpl e");
    List<Series> series = null;
    try {
      series = (List<Series>) query.getResultList();
      logger.debug("Got all series: {}", series);
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
  public DublinCoreCatalog getDublinCore(String seriesID) {
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
      logger.warn("could not find series {}. Null Pointer exeption", seriesID);
      return null;
    }
    EntityManager em = emf.createEntityManager();
    Series s = null;
    try {
       s = em.find(SeriesImpl.class, seriesID);
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
    Series s = new SeriesImpl();
    return s.generateSeriesId();
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.series.api.SeriesService#removeSeries(java.lang.String)
   */
  @Override
  public boolean removeSeries(String seriesID) {
    logger.debug("Removing series with the ID {}", seriesID);
    Series s;
    EntityManager em = emf.createEntityManager();
    try {
      em.getTransaction().begin();
      s = em.find(SeriesImpl.class, seriesID);
      if (s == null) return false; // series not in database
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
      SeriesImpl storedSeries = em.find(SeriesImpl.class, s.getSeriesId()); 
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
    }
    else this.componentContext = cc;
    
    emf = persistenceProvider.createEntityManagerFactory("org.opencastproject.series.impl", persistenceProperties);
  }
  
  public void deactivate(ComponentContext cc) {
    emf.close();
  }   
  
  public Map<String, Object> getPersistenceProperties() {
    return persistenceProperties;
  }

  public void setPersistenceProperties(Map<String, Object> persistenceProperties) {
    this.persistenceProperties = persistenceProperties;
  }
  
  public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }
  
  public PersistenceProvider getPersistenceProvider() {
    return persistenceProvider;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void updated(Dictionary properties) throws ConfigurationException {
    this.properties = properties;
    
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<Series> searchSeries(String pattern) {
    EntityManager em = emf.createEntityManager();
    List<SeriesMetadataImpl> found = null;
    try {
      Query query = em.createQuery("SELECT o FROM SeriesMetadataImpl o WHERE o.value LIKE :keyword");
      query.setParameter("keyword", "%"+pattern+"%");
      found = (List<SeriesMetadataImpl>) query.getResultList();
      logger.debug("Found {} values containing {}.", found.size(), pattern);
    } catch (Exception e) {
      logger.warn("Could not search for pattern {}: {} ",pattern, e.getMessage());
      return null;
    } finally {
      em.close();
    } 
    
    HashSet<Series> series = new HashSet<Series>(); 
    for (SeriesMetadata m : found) {
      series.add(m.getSeries());
    }
    
    LinkedList<Series> result = new LinkedList<Series>(series);
    
    Collections.sort(result);
    
    return result;
    
    
  }

}
