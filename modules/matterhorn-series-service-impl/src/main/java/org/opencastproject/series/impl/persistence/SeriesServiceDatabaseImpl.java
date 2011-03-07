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
package org.opencastproject.series.impl.persistence;

import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.series.impl.SeriesServiceDatabase;
import org.opencastproject.series.impl.SeriesServiceDatabaseException;
import org.opencastproject.util.NotFoundException;

import org.apache.commons.io.IOUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.persistence.spi.PersistenceProvider;

/**
 * Implements {@link SeriesServiceDatabase}. Defines permanent storage for series.
 */
public class SeriesServiceDatabaseImpl implements SeriesServiceDatabase {

  /** Logging utilities */
  private static final Logger logger = LoggerFactory.getLogger(SeriesServiceDatabaseImpl.class);

  /** Persistence provider set by OSGi */
  protected PersistenceProvider persistenceProvider;
  
  /** Persistence properties used to create {@link EntityManagerFactory} */
  protected Map<String, Object> persistenceProperties;

  /** Factory used to create {@link EntityManager}s for transactions */
  protected EntityManagerFactory emf;

  /** Dublin core service for serializing and deserializing Dublin cores */
  protected DublinCoreCatalogService dcService;

  /**
   * Creates {@link EntityManagerFactory} using persistence provider and properties passed via OSGi.
   * 
   * @param cc
   */
  public void activate(ComponentContext cc) {
    logger.info("Activating persistence manager for series");
    emf = persistenceProvider.createEntityManagerFactory("org.opencastproject.series.impl.persistence",
            persistenceProperties);
  }

  /**
   * Closes entity manager factory.
   * 
   * @param cc
   */
  public void deactivate(ComponentContext cc) {
    emf.close();
  }

  /**
   * OSGi callback to set persistence properties.
   * 
   * @param persistenceProperties
   *          persistence properties
   */
  public void setPersistenceProperties(Map<String, Object> persistenceProperties) {
    this.persistenceProperties = persistenceProperties;
  }

  /**
   * OSGi callback to set persistence provider.
   * 
   * @param persistenceProvider
   *          {@link PersistenceProvider} object
   */
  public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }

  /**
   * OSGi callback to set dublin core catalog service.
   * 
   * @param dcService
   *          {@link DublinCoreCatalogService} object
   */
  public void setDublinCoreService(DublinCoreCatalogService dcService) {
    this.dcService = dcService;
  }

  /**
   * Serializes Dublin core catalog and returns it as String.
   * 
   * @param dc
   *          {@link DublinCoreCatalog} to be serialized
   * @return String presenting serialized dublin core
   * @throws IOException
   *           if serialization fails
   */
  private String serializeDublinCore(DublinCoreCatalog dc) throws IOException {
    InputStream in = dcService.serialize(dc);

    StringWriter writer = new StringWriter();
    IOUtils.copy(in, writer, "UTF-8");

    return writer.toString();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.impl.SeriesServiceDatabase#deleteSeries(java.lang.String)
   */
  @Override
  public void deleteSeries(String seriesId) throws SeriesServiceDatabaseException, NotFoundException {
    EntityManager em = emf.createEntityManager();
    try {
      EntityTransaction tx = em.getTransaction();
      tx.begin();
      SeriesEntity entity = em.find(SeriesEntity.class, seriesId);
      if (entity == null) {
        throw new NotFoundException("Series with ID " + seriesId + " does not exist");
      }
      em.remove(entity);
      tx.commit();
    } catch (Exception e) {
      if (e instanceof NotFoundException) {
        throw (NotFoundException) e;
      }
      logger.error("Could not delete series: {}", e.getMessage());
      throw new SeriesServiceDatabaseException(e);
    } finally {
      em.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.impl.SeriesServiceDatabase#getAllSeries()
   */
  @SuppressWarnings("unchecked")
  @Override
  public DublinCoreCatalog[] getAllSeries() throws SeriesServiceDatabaseException {
    EntityManager em = emf.createEntityManager();
    Query query = em.createQuery("SELECT e FROM SeriesEntity e");
    List<SeriesEntity> seriesEntities = null;
    try {
      seriesEntities = (List<SeriesEntity>) query.getResultList();
    } catch (Exception e) {
      logger.error("Could not retrieve all series: {}", e.getMessage());
      throw new SeriesServiceDatabaseException(e);
    } finally {
      em.close();
    }
    DublinCoreCatalog[] catalogArray = new DublinCoreCatalog[seriesEntities.size()];
    for (int i = 0; i < seriesEntities.size(); i++) {
      try {
        String dcXML = seriesEntities.get(i).getDublinCoreXML();
        DublinCoreCatalog dc = dcService.load(new ByteArrayInputStream(dcXML.getBytes("UTF-8")));
        catalogArray[i] = dc;
      } catch (Exception e) {
        logger.error("Deserialization failed: {}", e.getMessage());
        throw new SeriesServiceDatabaseException(e);
      }
    }
    return catalogArray;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.impl.SeriesServiceDatabase#storeSeries(org.opencastproject.metadata.dublincore.
   * DublinCoreCatalog)
   */
  @Override
  public void storeSeries(DublinCoreCatalog dc) throws SeriesServiceDatabaseException {
    if (dc == null) {
      throw new SeriesServiceDatabaseException("Invalid value for Dublin core catalog: null");
    }
    String seriesId = dc.getFirst(DublinCore.PROPERTY_IDENTIFIER);
    String seriesXML;
    try {
      seriesXML = serializeDublinCore(dc);
    } catch (Exception e1) {
      logger.error("Could not serialize Dublin Core: {}", e1);
      throw new SeriesServiceDatabaseException(e1);
    }
    EntityManager em = emf.createEntityManager();
    try {
      EntityTransaction tx = em.getTransaction();
      tx.begin();
      SeriesEntity entity = em.find(SeriesEntity.class, seriesId);
      if (entity == null) {
        // no series stored, create new entity
        entity = new SeriesEntity();
        entity.setSeriesId(seriesId);
        entity.setSeries(seriesXML);
        em.persist(entity);
      } else {
        entity.setSeries(seriesXML);
        em.merge(entity);
      }
      tx.commit();
    } catch (Exception e) {
      logger.error("Could not update series: {}", e.getMessage());
      throw new SeriesServiceDatabaseException(e);
    } finally {
      em.close();
    }

  }

}
