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

import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.job.api.JobParser;
import org.opencastproject.rest.RestConstants;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.serviceregistry.api.JaxbServiceStatistics;
import org.opencastproject.serviceregistry.api.ServiceRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.serviceregistry.api.ServiceStatistics;
import org.opencastproject.serviceregistry.api.ServiceUnavailableException;
import org.opencastproject.serviceregistry.api.SystemLoad;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
import javax.persistence.OptimisticLockException;
import javax.persistence.Query;
import javax.persistence.RollbackException;
import javax.persistence.spi.PersistenceProvider;

/**
 * JPA implementation of the {@link ServiceRegistry}
 */
public class ServiceRegistryJpaImpl implements ServiceRegistry {

  static final Logger logger = LoggerFactory.getLogger(ServiceRegistryJpaImpl.class);

  /** Configuration key for the maximum load */
  protected static final String OPT_MAXLOAD = "org.opencastproject.server.maxload";

  /** The http client to use when connecting to remote servers */
  protected TrustedHttpClient client = null;

  /** Default dispatcher timeout (1 second) */
  static final long DEFAULT_DISPATCHER_TIMEOUT = 1000;

  /** The job dispatching thread */
  protected JobDispatcher jobDispatcher = null;

  /**
   * A static list of statuses that influence how load balancing is calculated
   */
  protected static final List<Status> JOB_STATUSES_INFLUINCING_LOAD_BALANCING;

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

  /** The maximum number of parallel jobs */
  protected int maxJobs = 1;

  public void activate(ComponentContext cc) {
    logger.debug("activate");

    // Set up persistence
    emf = persistenceProvider.createEntityManagerFactory("org.opencastproject.serviceregistry", persistenceProperties);

    // Find this host's url
    if (cc == null || StringUtils.isBlank(cc.getBundleContext().getProperty("org.opencastproject.server.url"))) {
      hostName = UrlSupport.DEFAULT_BASE_URL;
    } else {
      hostName = cc.getBundleContext().getProperty("org.opencastproject.server.url");
    }

    // Register this host
    try {
      if (cc == null || StringUtils.isBlank(cc.getBundleContext().getProperty(OPT_MAXLOAD))) {
        maxJobs = Runtime.getRuntime().availableProcessors();
      } else {
        try {
          maxJobs = Integer.parseInt(cc.getBundleContext().getProperty(OPT_MAXLOAD));
        } catch (NumberFormatException e) {
          maxJobs = Runtime.getRuntime().availableProcessors();
          logger.warn("Configuration key '{}' is not an integer. Falling back to the number of cores ({})",
                  OPT_MAXLOAD, maxJobs);
        }
      }
      registerHost(hostName, maxJobs);
    } catch (ServiceRegistryException e) {
      throw new IllegalStateException("Unable to register host " + hostName + " in the service registry", e);
    }

    // Track any services from this host that need to be added to the service registry
    if (cc != null) {
      try {
        tracker = new RestServiceTracker(cc.getBundleContext());
        tracker.open(true);
      } catch (InvalidSyntaxException e) {
        logger.error("Invlid filter syntax: {}", e);
        throw new IllegalStateException(e);
      }
    }

    // Instantiate and start the job dispatcher
    jobDispatcher = new JobDispatcher();
    jobDispatcher.setTimeout(DEFAULT_DISPATCHER_TIMEOUT);
    jobDispatcher.start();
  }

  public void deactivate() {
    logger.debug("deactivate");
    if (tracker != null) {
      tracker.close();
    }
    try {
      unregisterHost(hostName);
    } catch (ServiceRegistryException e) {
      throw new IllegalStateException("Unable to unregister host " + hostName + " from the service registry", e);
    }
    if (emf != null) {
      emf.close();
    }

    // Stop the job dispatcher
    jobDispatcher.stopRunning();
    jobDispatcher.interrupt();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, java.lang.String,
   *      java.util.List)
   */
  @Override
  public Job createJob(String type, String operation, List<String> arguments) throws ServiceUnavailableException,
          ServiceRegistryException {
    return createJob(this.hostName, type, operation, arguments, null, false);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, java.lang.String,
   *      java.util.List, java.lang.String)
   */
  @Override
  public Job createJob(String type, String operation, List<String> arguments, String payload)
          throws ServiceUnavailableException, ServiceRegistryException {
    return createJob(this.hostName, type, operation, arguments, payload, false);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, java.lang.String,
   *      java.util.List, String, boolean)
   */
  @Override
  public Job createJob(String type, String operation, List<String> arguments, String payload, boolean start)
          throws ServiceUnavailableException, ServiceRegistryException {
    return createJob(this.hostName, type, operation, arguments, payload, start);
  }

  /**
   * Creates a job on a remote host.
   */
  public Job createJob(String host, String serviceType, String operation, List<String> arguments, String payload,
          boolean start) throws ServiceUnavailableException, ServiceRegistryException {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      ServiceRegistrationJpaImpl serviceRegistration = getServiceRegistration(em, serviceType, host);
      if (serviceRegistration == null) {
        throw new ServiceUnavailableException("No service registration exists for type '" + serviceType + "' on host '"
                + host + "'");
      }
      if (serviceRegistration.getHostRegistration().isMaintenanceMode()) {
        logger.warn("Creating a job from {}, which is currently in maintenance mode.", serviceRegistration.getHost());
      }
      JobJpaImpl job = new JobJpaImpl(serviceRegistration, operation, arguments, payload, start);

      serviceRegistration.creatorJobs.add(job);
      if (start) {
        serviceRegistration.processorJobs.add(job);
      }
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
  JobJpaImpl createJob(ServiceRegistrationJpaImpl serviceRegistration, String operation, List<String> arguments,
          String payload, boolean startImmediately) {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      JobJpaImpl job = new JobJpaImpl(serviceRegistration, operation, arguments, payload, startImmediately);
      serviceRegistration.creatorJobs.add(job);
      if (startImmediately) {
        serviceRegistration.processorJobs.add(job);
      }
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
      if (job == null) {
        throw new NotFoundException("Job " + id + " not found");
      }
      // JPA's caches can be out of date if external changes (e.g. another node in the cluster) have been made to
      // this row in the database
      em.refresh(job);
      job.getArguments();
      return job;
    } catch (Exception e) {
      if (e instanceof NotFoundException) {
        throw (NotFoundException) e;
      } else {
        throw new ServiceRegistryException(e);
      }
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
  public Job updateJob(Job job) throws ServiceRegistryException {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      JobJpaImpl fromDb;
      try {
        fromDb = (JobJpaImpl) getJob(job.getId()); // do not use the direct em.find(), since it depends on the em cache
      } catch (NoResultException e) {
        throw new NotFoundException("job " + job + " is not a persistent object.", e);
      }
      update(fromDb, (JaxbJob) job);
      em.merge(fromDb);
      tx.commit();
      ((JaxbJob) job).setVersion(getJob(job.getId()).getVersion());
      return job;
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
    fromDb.setPayload(job.getPayload());
    fromDb.setStatus(job.getStatus());
    fromDb.setVersion(job.getVersion());
    if (job.getDateCreated() == null) {
      job.setDateCreated(now);
      fromDb.setDateCreated(now);
    }
    if (Status.RUNNING.equals(status)) {
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
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#registerHost(java.lang.String, int)
   */
  @Override
  public void registerHost(String host, int maxJobs) throws ServiceRegistryException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      // Find the existing registrations for this host and if it exists, update it
      HostRegistration existingHostRegistration = em.find(HostRegistration.class, host);
      if (existingHostRegistration == null) {
        em.persist(new HostRegistration(host, maxJobs, true, false));
      } else {
        existingHostRegistration.setMaxJobs(maxJobs);
        existingHostRegistration.setOnline(true);
        em.merge(existingHostRegistration);
      }
      logger.info("Registering {} with a maximum load of {}", host, maxJobs);
      tx.commit();
    } catch (Exception e) {
      if (tx.isActive())
        tx.rollback();
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null && em.isOpen()) {
        em.close();
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#unregisterHost(java.lang.String)
   */
  @Override
  public void unregisterHost(String host) throws ServiceRegistryException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      HostRegistration existingHostRegistration = em.find(HostRegistration.class, host);
      if (existingHostRegistration == null) {
        throw new ServiceRegistryException("Host '" + host
                + "' is not currently registered, so it can not be unregistered");
      } else {
        existingHostRegistration.setOnline(false);
        for (ServiceRegistration serviceRegistration : getServiceRegistrationsByHost(host)) {
          ((ServiceRegistrationJpaImpl) serviceRegistration).setOnline(false);
          logger.info("Unregistering service '{}' on host {}", serviceRegistration.getServiceType(), host);
          em.merge(serviceRegistration);
        }
        em.merge(existingHostRegistration);
      }
      logger.info("Unregistering {}", host, maxJobs);
      tx.commit();
    } catch (Exception e) {
      if (tx.isActive())
        tx.rollback();
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null && em.isOpen()) {
        em.close();
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#registerService(java.lang.String, java.lang.String,
   *      java.lang.String)
   */
  @Override
  public ServiceRegistration registerService(String serviceType, String baseUrl, String path)
          throws ServiceRegistryException {
    return setOnlineStatus(serviceType, baseUrl, path, true, false);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#registerService(java.lang.String, java.lang.String,
   *      java.lang.String, boolean)
   */
  @Override
  public ServiceRegistration registerService(String serviceType, String baseUrl, String path, boolean jobProducer)
          throws ServiceRegistryException {
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
      HostRegistration hostRegistration = em.find(HostRegistration.class, baseUrl);
      if (hostRegistration == null) {
        throw new IllegalStateException(
                "A service registration can not be updated when it has no associated host registration");
      }
      ServiceRegistrationJpaImpl registration = getServiceRegistration(em, serviceType, baseUrl);
      if (registration == null) {
        if (isBlank(path)) {
          // we can not create a new registration without a path
          throw new IllegalArgumentException("path must not be blank when registering new services");
        }
        if (jobProducer == null) { // if we are not provided a value, consider it to be false
          registration = new ServiceRegistrationJpaImpl(hostRegistration, serviceType, path, false);
        } else {
          registration = new ServiceRegistrationJpaImpl(hostRegistration, serviceType, path, jobProducer);
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
    // TODO: create methods that accept an entity manager, so we can execute multiple queries using the same em and tx
    setOnlineStatus(serviceType, baseUrl, null, false, null);

    // Find all jobs running on this service, and set them back to QUEUED.
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      Query query = em.createNamedQuery("Job.host.status");
      query.setParameter("status", Status.RUNNING);
      query.setParameter("host", baseUrl);
      @SuppressWarnings("unchecked")
      List<JobJpaImpl> unregisteredJobs = query.getResultList();
      for (JobJpaImpl job : unregisteredJobs) {
        job.setStatus(Status.QUEUED);
        job.setProcessorServiceRegistration(null);
        em.merge(job);
      }
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
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#setMaintenanceStatus(java.lang.String, boolean)
   */
  @Override
  public void setMaintenanceStatus(String baseUrl, boolean maintenance) throws IllegalStateException {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      HostRegistration reg = em.find(HostRegistration.class, baseUrl);
      if (reg == null) {
        throw new IllegalArgumentException("Can not set maintenance mode on a host that has not been registered");
      }
      reg.setMaintenanceMode(maintenance);
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
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getJobs(java.lang.String,
   *      org.opencastproject.job.api.Job.Status)
   */
  @SuppressWarnings("unchecked")
  @Override
  public List<Job> getJobs(String type, Status status) throws ServiceRegistryException {
    Query query = null;
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      if (type == null && status == null) {
        query = em.createNamedQuery("Job.all");
      } else if (type == null) {
        query = em.createNamedQuery("Job.status");
        query.setParameter("status", status);
      } else if (status == null) {
        query = em.createNamedQuery("Job.type");
        query.setParameter("serviceType", type);
      } else {
        query = em.createNamedQuery("Job");
        query.setParameter("status", status);
        query.setParameter("serviceType", type);
      }
      return query.getResultList();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
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
  public long count(String type, Status status) throws ServiceRegistryException {
    Query query = null;
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      if (type == null && status == null) {
        query = em.createNamedQuery("Job.count.all");
      } else if (type == null) {
        query = em.createNamedQuery("Job.count.status");
        query.setParameter("status", status);
      } else if (status == null) {
        query = em.createNamedQuery("Job.count.type");
        query.setParameter("serviceType", type);
      } else {
        query = em.createNamedQuery("Job.count");
        query.setParameter("status", status);
        query.setParameter("serviceType", type);
      }
      Number countResult = (Number) query.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
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
  public long count(String type, Status status, String host) throws ServiceRegistryException {
    EntityManager em = emf.createEntityManager();
    try {
      Query query = em.createNamedQuery("Job.countByHost");
      query.setParameter("status", status);
      query.setParameter("serviceType", type);
      query.setParameter("host", host);
      Number countResult = (Number) query.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
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
  public List<ServiceStatistics> getServiceStatistics() throws ServiceRegistryException {
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
            default:
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
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
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
  public List<ServiceRegistration> getServiceRegistrationsByLoad(String serviceType) throws ServiceRegistryException {
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
      return em.createNamedQuery("ServiceRegistration.getByType").setParameter("serviceType", serviceType)
              .getResultList();
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
    protected static final String FILTER = "(&(objectClass=javax.servlet.Servlet)("
            + RestConstants.SERVICE_PATH_PROPERTY + "=*))";

    protected BundleContext bundleContext = null;

    RestServiceTracker(BundleContext bundleContext) throws InvalidSyntaxException {
      super(bundleContext, bundleContext.createFilter(FILTER), null);
      this.bundleContext = bundleContext;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.osgi.util.tracker.ServiceTracker#open(boolean)
     */
    @Override
    public void open(boolean trackAllServices) {
      super.open(trackAllServices);
      try {
        ServiceReference[] references = bundleContext.getAllServiceReferences(null, FILTER);
        if (references != null) {
          for (ServiceReference ref : references) {
            addingService(ref);
          }
        }
      } catch (InvalidSyntaxException e) {
        throw new IllegalStateException("The tracker filter '" + FILTER + "' has syntax errors", e);
      }
    }

    @Override
    public Object addingService(ServiceReference reference) {
      String serviceType = (String) reference.getProperty(RestConstants.SERVICE_TYPE_PROPERTY);
      String servicePath = (String) reference.getProperty(RestConstants.SERVICE_PATH_PROPERTY);
      boolean jobProducer = (Boolean) reference.getProperty(RestConstants.SERVICE_JOBPRODUCER_PROPERTY);
      try {
        registerService(serviceType, hostName, servicePath, jobProducer);
      } catch (ServiceRegistryException e) {
        logger.warn("Unable to register job producer of type " + serviceType + " on host " + hostName);
      }
      return super.addingService(reference);
    }

    @Override
    public void removedService(ServiceReference reference, Object service) {
      String serviceType = (String) reference.getProperty(RestConstants.SERVICE_TYPE_PROPERTY);
      try {
        unRegisterService(serviceType, hostName);
      } catch (ServiceRegistryException e) {
        logger.warn("Unable to unregister job producer of type " + serviceType + " on host " + hostName);
      }
      super.removedService(reference, service);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getLoad()
   */
  @Override
  public SystemLoad getLoad() throws ServiceRegistryException {
    throw new UnsupportedOperationException();
  }

  /**
   * Sets the trusted http client.
   * 
   * @param client
   *          the trusted http client
   */
  void setTrustedHttpClient(TrustedHttpClient client) {
    this.client = client;
  }

  /**
   * Dispatches the job to the least loaded service that will accept the job, or throws a
   * <code>ServiceUnavailableException</code> if there is no such service.
   * 
   * @param job
   *          the job to dispatch
   * @return the host that accepted the dispatched job, or <code>null</code> if no services took the job.
   * @throws ServiceRegistryException
   *           if the service registrations are unavailable
   */
  protected String dispatchJob(Job job) throws ServiceRegistryException {

    // Find service instances
    List<ServiceRegistration> registrations = getServiceRegistrationsByLoad(job.getJobType());
    if (registrations.size() == 0) {
      logger.info("No service is available to handle jobs of type '" + job.getJobType() + "'");
      return null;
    }

    // Try the service registrations, after the first one finished, we quit
    JobJpaImpl jpaJob = ((JobJpaImpl)job);
    jpaJob.setStatus(Status.RUNNING);
    for (ServiceRegistration registration : registrations) {
      jpaJob.setProcessorServiceRegistration((ServiceRegistrationJpaImpl) registration);
      try {
        updateJob(jpaJob);
      } catch (OptimisticLockException e) {
        logger.debug("Another service registry has already dispatched this job");
        return null;
      }

      String serviceUrl = UrlSupport
              .concat(new String[] { registration.getHost(), registration.getPath(), "dispatch" });
      HttpPost post = new HttpPost(serviceUrl);
      try {
        String jobXml = JobParser.toXml(jpaJob);
        List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        params.add(new BasicNameValuePair("job", jobXml));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
        post.setEntity(entity);
      } catch (IOException e) {
        throw new ServiceRegistryException("Can not serialize job " + jpaJob, e);
      }

      // Post the request
      HttpResponse response = null;
      int responseStatusCode;
      try {
        logger.debug("Trying to dispatch job {} of type '{}' to {}",
                new String[] { Long.toString(jpaJob.getId()), jpaJob.getJobType(), registration.getHost() });
        response = client.execute(post);
        responseStatusCode = response.getStatusLine().getStatusCode();
        if (responseStatusCode == HttpStatus.SC_SERVICE_UNAVAILABLE) {
          continue;
        } else if (responseStatusCode == HttpStatus.SC_NO_CONTENT) {
          return registration.getHost();
        }
      } catch (Exception e) {
        logger.warn("Unable to dispatch job {}", jpaJob.getId(), e);
      } finally {
        client.close(response);
      }
    }
    // We've tried dispatching to every online service that can handle this type of job, with no luck.
    jpaJob.setStatus(Status.QUEUED);
    jpaJob.setProcessorServiceRegistration(null);
    updateJob(jpaJob);
    return null;
  }

  /**
   * This dispatcher implementation will wake from time to time and check for new jobs. If new jobs are found, it will
   * dispatch them to the services as appropriate.
   */
  class JobDispatcher extends Thread {

    /** Running flag */
    private boolean keepRunning = true;

    /** Dispatcher timeout */
    private long timeout = DEFAULT_DISPATCHER_TIMEOUT;

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
      while (keepRunning) {
        try {
          // Go through the jobs and find those that have not yet been dispatched
          try {
            List<Job> jobsToDispatch = getJobs(null, Status.QUEUED);
            for (Job job : jobsToDispatch) {
              try {
                String hostAcceptingJob = dispatchJob(job);
                if (hostAcceptingJob == null) {
                  logger.debug("Job {} could not be dispatched and is put back into queue", job.getId());
                } else {
                  logger.debug("Job {} dispatched to {}", job.getId(), hostAcceptingJob);
                }
              } catch (ServiceRegistryException e) {
                Throwable cause = (e.getCause() != null) ? e.getCause() : e;
                logger.error("Error dispatching job " + job, cause);
              }
            }
          } catch (Exception e) {
            logger.warn("Error dispatching jobs", e);
          }
          Thread.sleep(timeout);
        } catch (InterruptedException e) {
          logger.debug("Job dispatcher thread activated");
        }
      }
    }

    /**
     * Tells the dispatcher thread to stop running.
     */
    public void stopRunning() {
      keepRunning = false;
      interrupt();
    }

    /**
     * Sets the dispatcher timeout in miliseconds.
     * 
     * @param timeout
     *          the timeout
     */
    public void setTimeout(long timeout) {
      this.timeout = timeout;
      interrupt();
    }

  }
}
