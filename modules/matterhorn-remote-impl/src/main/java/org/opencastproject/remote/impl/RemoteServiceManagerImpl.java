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

import org.opencastproject.remote.api.Job;
import org.opencastproject.remote.api.RemoteServiceManager;
import org.opencastproject.remote.api.ServiceRegistration;
import org.opencastproject.remote.api.ServiceStatistics;
import org.opencastproject.remote.api.Job.Status;
import org.opencastproject.util.UrlSupport;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
  protected static List<Status> JOB_STATUSES_INFLUINCING_LOAD_BALANCING;

  static {
    JOB_STATUSES_INFLUINCING_LOAD_BALANCING = new ArrayList<Status>();
    JOB_STATUSES_INFLUINCING_LOAD_BALANCING.add(Status.QUEUED);
    JOB_STATUSES_INFLUINCING_LOAD_BALANCING.add(Status.RUNNING);
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
   * @see org.opencastproject.remote.api.RemoteServiceManager#parseJob(java.io.InputStream)
   */
  @Override
  public Job parseJob(InputStream in) {
    try {
      return JobBuilder.getInstance().parseJob(in);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.remote.api.RemoteServiceManager#parseJob(java.lang.String)
   */
  @Override
  public Job parseJob(String xml) {
    try {
      return JobBuilder.getInstance().parseJob(xml);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.remote.api.RemoteServiceManager#createJob(java.lang.String)
   */
  @Override
  public Job createJob(String type) {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      ServiceRegistrationImpl serviceRegistration = getServiceRegistration(em, type, this.hostName);
      Job job = new JobImpl(Status.QUEUED, serviceRegistration);
      em.persist(job);
      tx.commit();
      return job;
    } catch (RollbackException e) {
      tx.rollback();
      throw e;
    } finally {
      em.close();
    }
  }

  /**
   * Creates a job for a specific service registration.
   * 
   * @return the new job
   */
  JobImpl createJob(ServiceRegistrationImpl serviceRegistration) {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      JobImpl job = new JobImpl(serviceRegistration);
      em.persist(job);
      tx.commit();
      return job;
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
   * @see org.opencastproject.remote.api.RemoteServiceManager#getJob(java.lang.String)
   */
  @Override
  public Job getJob(String id) {
    EntityManager em = emf.createEntityManager();
    try {
      Job job = em.find(JobImpl.class, id);
      if (job != null) {
        // JPA's caches can be out of date if external changes (e.g. another node in the cluster) have been made to
        // this row in the database
        em.refresh(job);
      }
      return job;
    } finally {
      em.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.remote.api.RemoteServiceManager#updateJob(org.opencastproject.remote.api.Job)
   */
  @Override
  public void updateJob(Job job) {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      JobImpl fromDb;
      try {
        fromDb = em.find(JobImpl.class, job.getId());
      } catch (NoResultException e) {
        throw new IllegalArgumentException("job " + job + " is not a persistent object.", e);
      }
      update(fromDb, (JobImpl) job);
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
   * Sets the queue and runtimes and other elements of a persistent job based on a job that's been modified in memory.
   * Times on both the objects must be modified, since the in-memory job must not be stale.
   * 
   * @param fromDb
   *          The job from the database
   * @param job
   *          The in-memory job
   */
  private void update(JobImpl fromDb, JobImpl job) {
    Date now = new Date();
    Status status = job.getStatus();
    fromDb.setElement(job.getElement());
    fromDb.setStatus(job.getStatus());
    if (Status.QUEUED.equals(status)) {
      job.dateCreated = now;
      fromDb.dateCreated = now;
    } else if (Status.RUNNING.equals(status)) {
      job.dateStarted = now;
      job.queueTime = now.getTime() - fromDb.dateCreated.getTime();
      fromDb.dateStarted = now;
      fromDb.queueTime = now.getTime() - fromDb.dateCreated.getTime();
    } else if (Status.FAILED.equals(status)) {
      job.dateCompleted = now;
      job.runTime = now.getTime() - fromDb.dateStarted.getTime();
      fromDb.dateCompleted = now;
      fromDb.runTime = now.getTime() - fromDb.dateStarted.getTime();
    } else if (Status.FINISHED.equals(status)) {
      job.dateCompleted = now;
      job.runTime = now.getTime() - fromDb.dateStarted.getTime();
      fromDb.dateCompleted = now;
      fromDb.runTime = now.getTime() - fromDb.dateStarted.getTime();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.remote.api.RemoteServiceManager#registerService(java.lang.String, java.lang.String)
   */
  @Override
  public ServiceRegistration registerService(String jobType, String baseUrl) {
    return setOnlineStatus(jobType, baseUrl, true);
  }

  protected ServiceRegistrationImpl getServiceRegistration(EntityManager em, String jobType, String baseUrl) {
    try {
      Query q = em.createQuery("SELECT rh from ServiceRegistration rh where rh.host = :host and rh.jobType = :jobType");
      q.setParameter("host", baseUrl);
      q.setParameter("jobType", jobType);
      return (ServiceRegistrationImpl) q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * Sets the online status of a service registration.
   * 
   * @param jobType
   *          The job type
   * @param baseUrl
   *          the host URL
   * @param online
   *          whether the service is online or off
   * @return the service registration
   */
  protected ServiceRegistration setOnlineStatus(String jobType, String baseUrl, boolean online) {
    if (StringUtils.trimToNull(jobType) == null || StringUtils.trimToNull(baseUrl) == null) {
      throw new IllegalArgumentException("job and baseUrl must not be empty or null");
    }
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      ServiceRegistrationImpl registration = getServiceRegistration(em, jobType, baseUrl);
      if (registration == null) {
        registration = new ServiceRegistrationImpl(baseUrl, jobType);
        em.persist(registration);
      } else {
        registration.setOnline(online);
        em.merge(registration);
      }
      tx.commit();
      return registration;
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
   * @see org.opencastproject.remote.api.RemoteServiceManager#unRegisterService(java.lang.String, java.lang.String)
   */
  @Override
  public void unRegisterService(String jobType, String baseUrl) {
    setOnlineStatus(jobType, baseUrl, false);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.remote.api.RemoteServiceManager#setMaintenanceStatus(java.lang.String, java.lang.String,
   *      boolean)
   */
  @Override
  public void setMaintenanceStatus(String jobType, String baseUrl, boolean maintenance) throws IllegalStateException {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      Query q = em.createQuery("Select rh from ServiceRegistration rh where rh.host = :host and rh.jobType = :jobType");
      q.setParameter("host", baseUrl);
      q.setParameter("jobType", jobType);
      ServiceRegistrationImpl rh = null;
      try {
        rh = (ServiceRegistrationImpl) q.getSingleResult();
      } catch (NoResultException e) {
        throw new IllegalStateException("Can not set maintenance mode on a service that has not been registered");
      }
      rh.setMaintenanceMode(maintenance);
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
   * {@inheritDoc}
   * 
   * @see org.opencastproject.remote.api.RemoteServiceManager#getServiceRegistrations()
   */
  @SuppressWarnings("unchecked")
  @Override
  public List<ServiceRegistration> getServiceRegistrations() {
    EntityManager em = emf.createEntityManager();
    try {
      return em.createQuery("SELECT rh FROM ServiceRegistration rh").getResultList();
    } finally {
      em.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.remote.api.RemoteServiceManager#count(java.lang.String,
   *      org.opencastproject.remote.api.Job.Status)
   */
  @Override
  public long count(String type, Status status) {
    EntityManager em = emf.createEntityManager();
    try {
      Query query = em
              .createQuery("SELECT COUNT(j) FROM Job j where j.status = :status and j.serviceRegistration.jobType = :type");
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
   *      org.opencastproject.remote.api.Job.Status, java.lang.String)
   */
  @Override
  public long count(String type, Status status, String host) {
    EntityManager em = emf.createEntityManager();
    try {
      Query query = em
              .createQuery("SELECT COUNT(j) FROM Job j where j.status = :status and j.serviceRegistration.jobType = :type and j.serviceRegistration.host = :host");
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
   * @see org.opencastproject.remote.api.RemoteServiceManager#getServiceStatistics()
   */
  @SuppressWarnings("unchecked")
  @Override
  public List<ServiceStatistics> getServiceStatistics() {
    EntityManager em = emf.createEntityManager();
    try {
      Query query = em.createNamedQuery("ServiceRegistration.statistics");
      Map<ServiceRegistration, ServiceStatisticsImpl> statsMap = new HashMap<ServiceRegistration, ServiceStatisticsImpl>();
      List queryResults = query.getResultList();
      for (Object result : queryResults) {
        Object[] oa = (Object[]) result;
        ServiceRegistrationImpl serviceRegistration = ((ServiceRegistrationImpl) oa[0]);
        Status status = ((Status) oa[1]);
        Number count = (Number) oa[2];
        Number meanQueueTime = (Number) oa[3];
        Number meanRunTime = (Number) oa[4];

        // The statistics query returns a cartesian product, so we need to iterate over them to build up the objects
        ServiceStatisticsImpl stats = statsMap.get(serviceRegistration);
        if (stats == null) {
          stats = new ServiceStatisticsImpl(serviceRegistration);
          statsMap.put(serviceRegistration, stats);
        }
        // the status will be null if there are no jobs at all associated with this service registration
        if (status != null) {
          switch (status) {
          case RUNNING:
            stats.runningJobs = count.intValue();
            break;
          case QUEUED:
            stats.queuedJobs = count.intValue();
            break;
          case FINISHED:
            stats.meanRunTime = meanRunTime.longValue();
            stats.meanQueueTime = meanQueueTime.longValue();
            break;
          }
        }
      }
      List<ServiceStatistics> stats = new ArrayList<ServiceStatistics>(statsMap.values());
      Collections.sort(stats, new Comparator<ServiceStatistics>() {
        @Override
        public int compare(ServiceStatistics o1, ServiceStatistics o2) {
          ServiceRegistration reg1 = o1.getServiceRegistration();
          ServiceRegistration reg2 = o2.getServiceRegistration();
          int typeComparison = reg1.getJobType().compareTo(reg2.getJobType());
          return typeComparison == 0 ? reg1.getHost().compareTo(reg2.getHost()) : typeComparison;
        }
      });
      return stats;
    } finally {
      em.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.remote.api.RemoteServiceManager#getActiveHosts(java.lang.String)
   */
  @Override
  public List<String> getActiveHosts(String jobType) {
    List<HostHolder> hostHolders = new ArrayList<HostHolder>();
    for (ServiceStatistics serviceStats : getServiceStatistics()) {
      ServiceRegistration registration = serviceStats.getServiceRegistration();
      if (registration.isInMaintenanceMode() || !registration.isOnline() || !jobType.equals(registration.getJobType())) {
        continue;
      }
      hostHolders.add(new HostHolder(registration.getHost(), serviceStats.getQueuedJobs()
              + serviceStats.getRunningJobs()));
    }
    Collections.sort(hostHolders);
    List<String> hosts = new ArrayList<String>();
    for (HostHolder hostHolder : hostHolders) {
      hosts.add(hostHolder.host);
    }
    return hosts;
  }

  /**
   * A job-count comparable holder for hosts.
   */
  class HostHolder implements Comparable<HostHolder> {
    String host;
    Integer count;

    HostHolder(String host, Integer count) {
      this.host = host;
      this.count = count;
    }

    @Override
    public int compareTo(HostHolder o) {
      return this.count.compareTo(o.count);
    }
  }
}
