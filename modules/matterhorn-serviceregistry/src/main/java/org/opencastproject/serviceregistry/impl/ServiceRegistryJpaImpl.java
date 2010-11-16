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
package org.opencastproject.serviceregistry.impl;

import static org.apache.commons.lang.StringUtils.isBlank;

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

import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.rest.RestPublisher;
import org.opencastproject.serviceregistry.api.JaxbServiceStatistics;
import org.opencastproject.serviceregistry.api.ServiceRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.serviceregistry.api.ServiceStatistics;
import org.opencastproject.serviceregistry.api.ServiceUnavailableException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JPA implementation of the {@link ServiceRegistry}
 */
public class ServiceRegistryJpaImpl implements ServiceRegistry {

  private static final Logger logger = LoggerFactory.getLogger(ServiceRegistryJpaImpl.class);

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

  protected RestServiceTracker tracker = null;

  public void activate(ComponentContext cc) {
    logger.debug("activate");
    emf = persistenceProvider.createEntityManagerFactory("org.opencastproject.serviceregistry", persistenceProperties);
    if (cc == null || cc.getBundleContext().getProperty("org.opencastproject.server.url") == null) {
      hostName = UrlSupport.DEFAULT_BASE_URL;
    } else {
      hostName = cc.getBundleContext().getProperty("org.opencastproject.server.url");
    }
    if (cc != null) {
      try {
        tracker = new RestServiceTracker(cc.getBundleContext());
        tracker.open();
      } catch (InvalidSyntaxException e) {
        logger.error("Invlid filter syntax: {}", e);
        throw new IllegalStateException(e);
      }
    }
  }

  public void deactivate() {
    logger.debug("deactivate");
    if (tracker != null) {
      tracker.close();
    }
    if (emf != null) {
      emf.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String)
   */
  @Override
  public Job createJob(String type) throws ServiceUnavailableException, ServiceRegistryException {
    return createJob(type, this.hostName, false);
  }

  /**
   * Creates a job on a remote host.
   */
  public Job createJob(String type, String host, boolean start) throws ServiceUnavailableException, ServiceRegistryException {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      ServiceRegistrationJpaImpl serviceRegistration = getServiceRegistration(em, type, host);
      if (serviceRegistration == null) {
        throw new ServiceUnavailableException("No service registration exists for type '" + type + "' on host '" + host
                + "'");
      }
      Status status = start ? Status.RUNNING : Status.QUEUED;
      JobJpaImpl job = new JobJpaImpl(status, serviceRegistration);;
      serviceRegistration.jobs.add(job);
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
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, boolean)
   */
  @Override
  public Job createJob(String type, boolean start) throws ServiceUnavailableException, ServiceRegistryException {
    return createJob(type, this.hostName, start);
  }
  
  /**
   * Creates a job for a specific service registration.
   * 
   * @return the new job
   */
  JobJpaImpl createJob(ServiceRegistrationJpaImpl serviceRegistration) {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      JobJpaImpl job = new JobJpaImpl(serviceRegistration);
      serviceRegistration.jobs.add(job);
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
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getJob(long)
   */
  @Override
  public Job getJob(long id) throws NotFoundException, ServiceRegistryException {
    EntityManager em = emf.createEntityManager();
    try {
      Job job = em.find(JobJpaImpl.class, id);
      if (job == null)
        throw new NotFoundException("Job " + id + " not found");
      // JPA's caches can be out of date if external changes (e.g. another node in the cluster) have been made to
      // this row in the database
      em.refresh(job);
      return job;
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    } finally {
      em.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#updateJob(org.opencastproject.job.api.Job)
   */
  @Override
  public void updateJob(Job job) throws ServiceRegistryException {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      JobJpaImpl fromDb;
      try {
        fromDb = em.find(JobJpaImpl.class, job.getId());
      } catch (NoResultException e) {
        throw new NotFoundException("job " + job + " is not a persistent object.", e);
      }
      update(fromDb, (JaxbJob) job);
      tx.commit();
    } catch (Exception e) {
      if (tx.isActive())
        tx.rollback();
      throw new ServiceRegistryException(e);
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
  private void update(JobJpaImpl fromDb, JaxbJob job) {
    Date now = new Date();
    Status status = job.getStatus();
    fromDb.setElement(job.getElement());
    fromDb.setStatus(job.getStatus());
    if (Status.QUEUED.equals(status)) {
      job.setDateCreated(now);
      fromDb.setDateCreated(now);
    } else if (Status.RUNNING.equals(status)) {
      job.setDateStarted(now);
      job.setQueueTime(now.getTime() - job.getDateCreated().getTime());
      fromDb.setDateStarted(now);
      fromDb.setQueueTime(now.getTime() - job.getDateCreated().getTime());
    } else if (Status.FAILED.equals(status)) {
      // failed jobs may not have even started properly
      if (job.getDateStarted() != null) {
        job.setDateCompleted(now);
        job.setRunTime(now.getTime() - job.getDateStarted().getTime());
        fromDb.setDateCompleted(now);
        fromDb.setRunTime(now.getTime() - job.getDateStarted().getTime());
      }
    } else if (Status.FINISHED.equals(status)) {
      if (job.getDateStarted() == null)
        throw new IllegalStateException("Job " + job + " was never started");
      job.setDateCompleted(now);
      job.setRunTime(now.getTime() - job.getDateStarted().getTime());
      fromDb.setDateCompleted(now);
      fromDb.setRunTime(now.getTime() - job.getDateStarted().getTime());
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#registerService(java.lang.String, java.lang.String,
   *      java.lang.String)
   */
  @Override
  public ServiceRegistration registerService(String serviceType, String baseUrl, String path) throws ServiceRegistryException {
    return setOnlineStatus(serviceType, baseUrl, path, true, false);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#registerService(java.lang.String, java.lang.String,
   *      java.lang.String, boolean)
   */
  @Override
  public ServiceRegistration registerService(String serviceType, String baseUrl, String path, boolean jobProducer) throws ServiceRegistryException {
    return setOnlineStatus(serviceType, baseUrl, path, true, jobProducer);
  }

  protected ServiceRegistrationJpaImpl getServiceRegistration(EntityManager em, String serviceType, String host) {
    try {
      Query q = em.createNamedQuery("ServiceRegistration.getRegistration");
      q.setParameter("serviceType", serviceType);
      q.setParameter("host", host);
      return (ServiceRegistrationJpaImpl) q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * Sets the online status of a service registration.
   * 
   * @param serviceType
   *          The job type
   * @param baseUrl
   *          the host URL
   * @param online
   *          whether the service is online or off
   * @param jobProducer
   *          whether this service produces jobs for long running operations
   * @return the service registration
   */
  protected ServiceRegistration setOnlineStatus(String serviceType, String baseUrl, String path, boolean online,
          Boolean jobProducer) throws ServiceRegistryException {
    if (isBlank(serviceType) || isBlank(baseUrl)) {
      throw new IllegalArgumentException("serviceType and baseUrl must not be blank");
    }
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      ServiceRegistrationJpaImpl registration = getServiceRegistration(em, serviceType, baseUrl);
      if (registration == null) {
        if (isBlank(path)) {
          // we can not create a new registration without a path
          throw new IllegalArgumentException("path must not be blank when registering new services");
        }
        if (jobProducer == null) { // if we are not provided a value, consider it to be false
          registration = new ServiceRegistrationJpaImpl(serviceType, baseUrl, path, false);
        } else {
          registration = new ServiceRegistrationJpaImpl(serviceType, baseUrl, path, jobProducer);
        }
        em.persist(registration);
      } else {
        registration.setOnline(online);
        if (jobProducer != null) { // if we are not provided a value, don't update the persistent value
          registration.setJobProducer(jobProducer);
        }
        em.merge(registration);
      }
      tx.commit();
      return registration;
    } catch (Exception e) {
      if (tx.isActive())
        tx.rollback();
      throw new ServiceRegistryException(e);
    } finally {
      em.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#unRegisterService(java.lang.String, java.lang.String)
   */
  @Override
  public void unRegisterService(String serviceType, String baseUrl) throws ServiceRegistryException {
    setOnlineStatus(serviceType, baseUrl, null, false, null);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#setMaintenanceStatus(java.lang.String,
   *      java.lang.String, boolean)
   */
  @Override
  public void setMaintenanceStatus(String serviceType, String baseUrl, boolean maintenance)
          throws IllegalStateException {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      ServiceRegistrationJpaImpl reg = getServiceRegistration(em, serviceType, baseUrl);
      if (reg == null) {
        throw new IllegalStateException("Can not set maintenance mode on a service that has not been registered");
      }
      reg.setInMaintenanceMode(maintenance);
      em.merge(reg);
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
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistrations()
   */
  @SuppressWarnings("unchecked")
  @Override
  public List<ServiceRegistration> getServiceRegistrations() {
    EntityManager em = emf.createEntityManager();
    try {
      return em.createNamedQuery("ServiceRegistration.getAll").getResultList();
    } finally {
      em.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#count(java.lang.String,
   *      org.opencastproject.job.api.Job.Status)
   */
  @Override
  public long count(String type, Status status) {
    EntityManager em = emf.createEntityManager();
    try {
      Query query = em.createNamedQuery("Job.count");
      query.setParameter("status", status);
      query.setParameter("serviceType", type);
      Number countResult = (Number) query.getSingleResult();
      return countResult.longValue();
    } finally {
      em.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#count(java.lang.String,
   *      org.opencastproject.job.api.Job.Status, java.lang.String)
   */
  @Override
  public long count(String type, Status status, String host) {
    EntityManager em = emf.createEntityManager();
    try {
      Query query = em.createNamedQuery("Job.countByHost");
      query.setParameter("status", status);
      query.setParameter("serviceType", type);
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
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceStatistics()
   */
  @Override
  public List<ServiceStatistics> getServiceStatistics() {
    EntityManager em = emf.createEntityManager();
    try {
      Query query = em.createNamedQuery("ServiceRegistration.statistics");
      Map<ServiceRegistration, JaxbServiceStatistics> statsMap = new HashMap<ServiceRegistration, JaxbServiceStatistics>();
      @SuppressWarnings("rawtypes")
      List queryResults = query.getResultList();
      for (Object result : queryResults) {
        Object[] oa = (Object[]) result;
        ServiceRegistrationJpaImpl serviceRegistration = ((ServiceRegistrationJpaImpl) oa[0]);
        Status status = ((Status) oa[1]);
        Number count = (Number) oa[2];
        Number meanQueueTime = (Number) oa[3];
        Number meanRunTime = (Number) oa[4];

        // The statistics query returns a cartesian product, so we need to iterate over them to build up the objects
        JaxbServiceStatistics stats = statsMap.get(serviceRegistration);
        if (stats == null) {
          stats = new JaxbServiceStatistics(serviceRegistration);
          statsMap.put(serviceRegistration, stats);
        }
        // the status will be null if there are no jobs at all associated with this service registration
        if (status != null) {
          switch (status) {
          case RUNNING:
            stats.setRunningJobs(count.intValue());
            break;
          case QUEUED:
            stats.setQueuedJobs(count.intValue());
            break;
          case FINISHED:
            stats.setMeanRunTime(meanRunTime.longValue());
            stats.setMeanQueueTime(meanQueueTime.longValue());
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
          int typeComparison = reg1.getServiceType().compareTo(reg2.getServiceType());
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
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistrationsByLoad(java.lang.String)
   */
  @Override
  public List<ServiceRegistration> getServiceRegistrationsByLoad(String serviceType) {
    List<ServiceRegistration> registrations = new ArrayList<ServiceRegistration>();
    List<ServiceStatistics> stats = getServiceStatistics();
    Collections.sort(stats, new Comparator<ServiceStatistics>() {
      @Override
      public int compare(ServiceStatistics o1, ServiceStatistics o2) {
        return (o1.getQueuedJobs() + o1.getRunningJobs()) - (o2.getQueuedJobs() + o2.getRunningJobs());
      }
    });
    for (ServiceStatistics serviceStats : stats) {
      ServiceRegistration registration = serviceStats.getServiceRegistration();
      if (registration.isInMaintenanceMode() || !registration.isOnline()
              || !serviceType.equals(registration.getServiceType())) {
        continue;
      }
      registrations.add(serviceStats.getServiceRegistration());
    }
    return registrations;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistrationsByType(java.lang.String)
   */
  @SuppressWarnings("unchecked")
  @Override
  public List<ServiceRegistration> getServiceRegistrationsByType(String serviceType) throws ServiceRegistryException {
    EntityManager em = emf.createEntityManager();
    try {
      return em.createNamedQuery("ServiceRegistration.getByType").setParameter("serviceType", serviceType).getResultList();
    } finally {
      em.close();
    }
  }
  
  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistrationsByHost(java.lang.String)
   */
  @SuppressWarnings("unchecked")
  @Override
  public List<ServiceRegistration> getServiceRegistrationsByHost(String host) throws ServiceRegistryException {
    EntityManager em = emf.createEntityManager();
    try {
      return em.createNamedQuery("ServiceRegistration.getByHost").setParameter("host", host).getResultList();
    } finally {
      em.close();
    }
  }
  
  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistration(java.lang.String,
   *      java.lang.String)
   */
  @Override
  public ServiceRegistration getServiceRegistration(String serviceType, String host) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      return getServiceRegistration(em, serviceType, host);
    } finally {
      if (em != null) {
        em.close();
      }
    }
  }

  /**
   * A custom ServiceTracker that registers all locally published servlets so clients can find the most appropriate
   * service on the network to handle new jobs.
   */
  class RestServiceTracker extends ServiceTracker {

    RestServiceTracker(BundleContext bundleContext) throws InvalidSyntaxException {
      super(bundleContext, bundleContext.createFilter("(&(objectClass=javax.servlet.Servlet)("
              + RestPublisher.SERVICE_PATH_PROPERTY + "=*))"), null);
    }

    @Override
    public Object addingService(ServiceReference reference) {
      String serviceType = (String) reference.getProperty(RestPublisher.SERVICE_TYPE_PROPERTY);
      String servicePath = (String) reference.getProperty(RestPublisher.SERVICE_PATH_PROPERTY);
      boolean jobProducer = (Boolean) reference.getProperty(RestPublisher.SERVICE_JOBPRODUCER_PROPERTY);
      try {
        registerService(serviceType, hostName, servicePath, jobProducer);
      } catch (ServiceRegistryException e) {
        logger.warn("Unable to register job producer of type " + serviceType + " on host " + hostName);
      }
      return super.addingService(reference);
    }

    @Override
    public void removedService(ServiceReference reference, Object service) {
      String serviceType = (String) reference.getProperty(RestPublisher.SERVICE_TYPE_PROPERTY);
      try {
        unRegisterService(serviceType, hostName);
      } catch (ServiceRegistryException e) {
        logger.warn("Unable to unregister job producer of type " + serviceType + " on host " + hostName);
      }
      super.removedService(reference, service);
    }
  }
}
