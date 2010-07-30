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
package org.opencastproject.remote.impl;

import org.opencastproject.remote.api.Receipt;
import org.opencastproject.remote.api.RemoteServiceManager;
import org.opencastproject.remote.api.ServiceRegistration;
import org.opencastproject.remote.api.Receipt.Status;
import org.opencastproject.util.UrlSupport;

import org.apache.commons.collections.MapIterator;
import org.apache.commons.collections.bidimap.TreeBidiMap;
import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.RollbackException;
import javax.persistence.spi.PersistenceProvider;

/**
 * JPA implementation of the {@link RemoteServiceManager}
 */
public class RemoteServiceManagerImpl implements RemoteServiceManager {
  private static final Logger logger = LoggerFactory.getLogger(RemoteServiceManagerImpl.class);

  /**
   * A static list of statuses that influence how load balancing is calculated
   */
  protected static List<Status> STATUSES_INFLUINCING_LOAD_BALANCING;

  static {
    STATUSES_INFLUINCING_LOAD_BALANCING = new ArrayList<Status>();
    STATUSES_INFLUINCING_LOAD_BALANCING.add(Status.QUEUED);
    STATUSES_INFLUINCING_LOAD_BALANCING.add(Status.RUNNING);
  }

  /** The JPA provider */
  protected PersistenceProvider persistenceProvider;

  /** This host's base URL */
  protected String hostName;

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

  public void activate(ComponentContext cc) {
    logger.debug("activate");
    emf = persistenceProvider.createEntityManagerFactory("org.opencastproject.remote", persistenceProperties);
    if (cc == null || cc.getBundleContext().getProperty("org.opencastproject.server.url") == null) {
      hostName = UrlSupport.DEFAULT_BASE_URL;
    } else {
      hostName = cc.getBundleContext().getProperty("org.opencastproject.server.url");
    }
  }

  public void deactivate() {
    logger.debug("deactivate");
    emf.close();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.remote.api.RemoteServiceManager#parseReceipt(java.io.InputStream)
   */
  @Override
  public Receipt parseReceipt(InputStream in) {
    try {
      return ReceiptBuilder.getInstance().parseReceipt(in);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.remote.api.RemoteServiceManager#parseReceipt(java.lang.String)
   */
  @Override
  public Receipt parseReceipt(String xml) {
    try {
      return ReceiptBuilder.getInstance().parseReceipt(xml);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.remote.api.RemoteServiceManager#count(java.lang.String,
   *      org.opencastproject.remote.api.Receipt.Status)
   */
  @Override
  public long count(String type, Status status) {
    EntityManager em = emf.createEntityManager();
    try {
      Query query = em.createQuery("SELECT COUNT(r) FROM Receipt r where r.status = :status and r.type = :type");
      query.setParameter("status", status);
      query.setParameter("type", type);
      Number countResult = (Number) query.getSingleResult();
      return countResult.longValue();
    } finally {
      em.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.remote.api.RemoteServiceManager#count(java.lang.String,
   *      org.opencastproject.remote.api.Receipt.Status, java.lang.String)
   */
  @Override
  public long count(String type, Status status, String host) {
    EntityManager em = emf.createEntityManager();
    try {
      Query query = em
              .createQuery("SELECT COUNT(r) FROM Receipt r where r.status = :status and r.type = :type and r.host = :host");
      query.setParameter("status", status);
      query.setParameter("type", type);
      query.setParameter("host", host);
      Number countResult = (Number) query.getSingleResult();
      return countResult.longValue();
    } finally {
      em.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.remote.api.RemoteServiceManager#createReceipt(java.lang.String)
   */
  @Override
  public Receipt createReceipt(String type) {
    String id = UUID.randomUUID().toString();
    Receipt receipt = new ReceiptImpl(id, Status.QUEUED, type, hostName, null);
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      em.persist(receipt);
      tx.commit();
      return receipt;
    } catch (RollbackException e) {
      tx.rollback();
      throw e;
    } finally {
      em.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.remote.api.RemoteServiceManager#getReceipt(java.lang.String)
   */
  @Override
  public Receipt getReceipt(String id) {
    EntityManager em = emf.createEntityManager();
    try {
      Receipt receipt = em.find(ReceiptImpl.class, id);
      if (receipt != null) {
        // JPA's caches can be out of date if external changes (e.g. another node in the cluster) have been made to
        // this row in the database
        em.refresh(receipt);
      }
      return receipt;
    } finally {
      em.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.remote.api.RemoteServiceManager#updateReceipt(org.opencastproject.remote.api.Receipt)
   */
  @Override
  public void updateReceipt(Receipt receipt) {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      Receipt fromDb;
      try {
        fromDb = em.find(ReceiptImpl.class, receipt.getId());
      } catch (NoResultException e) {
        throw new IllegalArgumentException("receipt " + receipt + " is not a persistent object.", e);
      }
      fromDb.setElement(receipt.getElement());
      fromDb.setStatus(receipt.getStatus());
      fromDb.setType(receipt.getType());
      fromDb.setHost(receipt.getHost());
      tx.commit();
    } catch (RollbackException e) {
      if (tx.isActive())
        tx.rollback();
      throw e;
    } finally {
      em.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.remote.api.RemoteServiceManager#registerService(java.lang.String, java.lang.String)
   */
  @Override
  public void registerService(String jobType, String baseUrl) {
    if (StringUtils.trimToNull(jobType) == null || StringUtils.trimToNull(baseUrl) == null) {
      throw new IllegalArgumentException("receiptType and baseUrl must not be empty or null");
    }
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      ServiceRegistrationImpl existingRegistration = getServiceRegistration(em, jobType, baseUrl);
      if (existingRegistration == null) {
        ServiceRegistrationImpl rh = new ServiceRegistrationImpl(baseUrl, jobType, false);
        em.persist(rh);
      } else {
        logger.warn("An existing service registration exists for {}@{}.  Perhaps there was an unclean shutdown?",
                jobType, baseUrl);
        if (existingRegistration.isInMaintenanceMode()) {
          existingRegistration.setInMaintenanceMode(false);
          em.merge(existingRegistration);
        }
      }
      tx.commit();
    } catch (RollbackException e) {
      tx.rollback();
      throw e;
    } finally {
      em.close();
    }
  }

  protected ServiceRegistrationImpl getServiceRegistration(EntityManager em, String jobType, String baseUrl) {
    try {
      Query q = em
              .createQuery("SELECT rh from ServiceRegistrationImpl rh where rh.host = :host and rh.receiptType = :jobType");
      q.setParameter("host", baseUrl);
      q.setParameter("jobType", jobType);
      return (ServiceRegistrationImpl) q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.remote.api.RemoteServiceManager#unRegisterService(java.lang.String, java.lang.String)
   */
  @Override
  public void unRegisterService(String receiptType, String baseUrl) {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      Query q = em
              .createQuery("DELETE from ServiceRegistrationImpl rh where rh.host = :host and rh.receiptType = :jobType");
      q.setParameter("host", baseUrl);
      q.setParameter("jobType", receiptType);
      q.executeUpdate();
      tx.commit();
    } catch (RollbackException e) {
      if (tx.isActive())
        tx.rollback();
      throw e;
    } finally {
      em.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.remote.api.RemoteServiceManager#setMaintenanceMode(java.lang.String, java.lang.String,
   *      boolean)
   */
  @Override
  public void setMaintenanceMode(String jobType, String baseUrl, boolean maintenanceMode) throws IllegalStateException {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      Query q = em
              .createQuery("Select rh from ServiceRegistrationImpl rh where rh.host = :host and rh.receiptType = :jobType");
      q.setParameter("host", baseUrl);
      q.setParameter("jobType", jobType);
      ServiceRegistrationImpl rh = null;
      try {
        rh = (ServiceRegistrationImpl) q.getSingleResult();
      } catch (NoResultException e) {
        throw new IllegalStateException("Can not set maintenance mode on a service that has not been registered");
      }
      rh.setInMaintenanceMode(maintenanceMode);
      em.merge(rh);
      tx.commit();
    } catch (RollbackException e) {
      if (tx.isActive())
        tx.rollback();
      throw e;
    } finally {
      em.close();
    }
  }

  /**
   * Get the lists of hosts that can handle this jobType
   * 
   * @param jobType
   *          The job type
   * @return the list of hosts that can handle this kind of job
   */
  @SuppressWarnings("unchecked")
  protected List<String> getHosts(String jobType) {
    EntityManager em = emf.createEntityManager();
    try {
      Query query = em
              .createQuery("SELECT DISTINCT rh.host FROM ServiceRegistrationImpl rh where rh.receiptType = :jobType and rh.inMaintenanceMode = false");
      query.setParameter("jobType", jobType);
      return query.getResultList();
    } finally {
      em.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.remote.api.RemoteServiceManager#getRemoteHosts(java.lang.String)
   */
  @Override
  public List<String> getRemoteHosts(String jobType) {
    EntityManager em = emf.createEntityManager();
    // Get a list of hosts, sorted by the number of running jobs
    List<String> hosts = getHosts(jobType);
    TreeBidiMap runningJobsMap = new TreeBidiMap();
    try {
      Query query = em
              .createQuery("SELECT r.host, COUNT(r) FROM Receipt r where r.status in :statuses and r.type = :jobType group by r.host");
      query.setParameter("statuses", STATUSES_INFLUINCING_LOAD_BALANCING);
      query.setParameter("jobType", jobType);
      for (Object result : query.getResultList()) {
        Object[] oa = (Object[]) result;
        runningJobsMap.put((String) oa[0], (Long) oa[1]);
      }
      // if a host wasn't returned because it doesn't have any jobs, add it here with a count of zero
      for (String host : hosts) {
        // There seems to be a bug in the TreeBidiMap.containsKey() method, so work around it
        if (runningJobsMap.get(host) == null) {
          runningJobsMap.put(host, 0L);
        }
      }

      // now build a list of hosts, sorted by the job count
      LinkedList<String> sortedRemoteHosts = new LinkedList<String>();
      MapIterator iter = runningJobsMap.inverseOrderedBidiMap().orderedMapIterator();
      while (iter.hasNext()) {
        iter.next();
        sortedRemoteHosts.add((String) iter.getValue());
      }
      return sortedRemoteHosts;
    } finally {
      em.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.remote.api.RemoteServiceManager#getServiceRegistrations()
   */
  @SuppressWarnings("unchecked")
  @Override
  public List<ServiceRegistration> getServiceRegistrations() {
    EntityManager em = emf.createEntityManager();
    try {
      return em.createQuery("SELECT rh FROM ServiceRegistrationImpl rh").getResultList();
    } finally {
      em.close();
    }
  }
}
