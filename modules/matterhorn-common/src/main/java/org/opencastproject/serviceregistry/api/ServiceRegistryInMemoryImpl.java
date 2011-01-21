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
package org.opencastproject.serviceregistry.api;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.job.api.JobImpl;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.util.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple and in-memory implementation of a the service registry intended for testing scenarios.
 */
public class ServiceRegistryInMemoryImpl implements ServiceRegistry {

  /** Logging facility */
  static final Logger logger = LoggerFactory.getLogger(ServiceRegistryInMemoryImpl.class);

  /** Default dispatcher timeout (1 second) */
  static final long DEFAULT_DISPATCHER_TIMEOUT = 1000;

  /** Hostname for localhost */
  private static final String LOCALHOST = "localhost";

  /** The hosts */
  protected Map<String, Long> hosts = new HashMap<String, Long>();

  /** The service registrations */
  protected Map<String, List<ServiceRegistrationInMemoryImpl>> services = new HashMap<String, List<ServiceRegistrationInMemoryImpl>>();

  /** The jobs */
  protected List<Job> jobs = new ArrayList<Job>();

  /** The job dispatching thread */
  protected JobDispatcher jobDispatcher = null;

  /** The job identifier */
  protected static AtomicLong idCounter = new AtomicLong();

  public ServiceRegistryInMemoryImpl(JobProducer service) throws ServiceRegistryException {
    if (service != null)
      registerService(service);
    jobDispatcher = new JobDispatcher();
    jobDispatcher.setTimeout(DEFAULT_DISPATCHER_TIMEOUT);
    jobDispatcher.start();
  }

  /**
   * Creates a new service registry in memory.
   */
  public ServiceRegistryInMemoryImpl() {
    jobDispatcher = new JobDispatcher();
    jobDispatcher.setTimeout(DEFAULT_DISPATCHER_TIMEOUT);
    jobDispatcher.start();
  }

  /**
   * This method shuts down the service registry.
   */
  public void dispose() {
    jobDispatcher.stopRunning();
    jobDispatcher.interrupt();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#registerHost(java.lang.String, int)
   */
  @Override
  public void registerHost(String host, int maxConcurrentJobs) throws ServiceRegistryException {
    hosts.put(host, new Long(maxConcurrentJobs));
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#unregisterHost(java.lang.String)
   */
  @Override
  public void unregisterHost(String host) throws ServiceRegistryException {
    hosts.remove(host);
    services.remove(host);
  }

  /**
   * Method to register locally running services.
   * 
   * @param localService
   *          the service instance
   * @param serviceType
   *          the service type
   * @return the service registration
   * @throws ServiceRegistryException
   */
  public ServiceRegistration registerService(JobProducer localService) throws ServiceRegistryException {

    List<ServiceRegistrationInMemoryImpl> servicesOnHost = services.get(LOCALHOST);
    if (servicesOnHost == null) {
      servicesOnHost = new ArrayList<ServiceRegistrationInMemoryImpl>();
      services.put(LOCALHOST, servicesOnHost);
    }

    ServiceRegistrationInMemoryImpl registration = new ServiceRegistrationInMemoryImpl(localService);
    servicesOnHost.add(registration);
    return registration;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#registerService(java.lang.String, java.lang.String,
   *      java.lang.String)
   */
  @Override
  public ServiceRegistration registerService(String serviceType, String host, String path)
          throws ServiceRegistryException {
    return registerService(serviceType, host, path, false);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#registerService(java.lang.String, java.lang.String,
   *      java.lang.String, boolean)
   */
  @Override
  public ServiceRegistration registerService(String serviceType, String host, String path, boolean jobProducer)
          throws ServiceRegistryException {

    List<ServiceRegistrationInMemoryImpl> servicesOnHost = services.get(host);
    if (servicesOnHost == null) {
      servicesOnHost = new ArrayList<ServiceRegistrationInMemoryImpl>();
      services.put(host, servicesOnHost);
    }

    ServiceRegistrationInMemoryImpl registration = new ServiceRegistrationInMemoryImpl(serviceType, host, path,
            jobProducer);
    servicesOnHost.add(registration);
    return registration;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#unRegisterService(java.lang.String, java.lang.String)
   */
  @Override
  public void unRegisterService(String serviceType, String host) throws ServiceRegistryException {
    List<ServiceRegistrationInMemoryImpl> servicesOnHost = services.get(host);
    if (servicesOnHost != null) {
      Iterator<ServiceRegistrationInMemoryImpl> ri = servicesOnHost.iterator();
      while (ri.hasNext()) {
        ServiceRegistration registration = ri.next();
        if (serviceType.equals(registration.getServiceType()))
          ri.remove();
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#setMaintenanceStatus(java.lang.String, boolean)
   */
  @Override
  public void setMaintenanceStatus(String host, boolean maintenance) throws ServiceRegistryException {
    List<ServiceRegistrationInMemoryImpl> servicesOnHost = services.get(host);
    if (servicesOnHost != null) {
      for (ServiceRegistrationInMemoryImpl r : servicesOnHost) {
        r.setMaintenance(maintenance);
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, java.lang.String)
   */
  @Override
  public Job createJob(String type, String operation) throws ServiceRegistryException {
    return createJob(type, operation, null, null, true);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, java.lang.String,
   *      java.util.List)
   */
  @Override
  public Job createJob(String type, String operation, List<String> arguments) throws ServiceRegistryException {
    return createJob(type, operation, arguments, null, true);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, java.lang.String,
   *      java.util.List, java.lang.String)
   */
  @Override
  public Job createJob(String type, String operation, List<String> arguments, String payload)
          throws ServiceRegistryException {
    return createJob(type, operation, arguments, payload, true);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, java.lang.String,
   *      java.util.List, String, boolean)
   */
  @Override
  public Job createJob(String type, String operation, List<String> arguments, String payload, boolean enqueuImmediately)
          throws ServiceRegistryException {
    if (getServiceRegistrationsByType(type).size() == 0)
      logger.warn("Service " + type + " not available");

    JobImpl job = null;
    synchronized (this) {
      job = new JobImpl(idCounter.addAndGet(1));
      job.setJobType(type);
      job.setOperation(operation);
      job.setArguments(arguments);
      job.setPayload(payload);
      if (enqueuImmediately)
        job.setStatus(Status.QUEUED);
      else
        job.setStatus(Status.INSTANTIATED);
    }

    synchronized (jobs) {
      jobs.add(job);
    }

    jobDispatcher.interrupt();

    return job;
  }

  /**
   * Dispatches the job to the least loaded service or throws a <code>ServiceUnavailableException</code> if there is no
   * such service.
   * 
   * @param job
   *          the job to dispatch
   * @throws ServiceUnavailableException
   *           if no service is available to dispatch the job
   * @throws ServiceRegistryException
   *           if the service registrations are unavailable or dispatching of the job fails
   */
  protected void dispatchJob(Job job) throws ServiceUnavailableException, ServiceRegistryException {
    List<ServiceRegistration> registrations = getServiceRegistrationsByLoad(job.getJobType());
    if (registrations.size() == 0)
      throw new ServiceUnavailableException("No service is available to handle jobs of type '" + job.getJobType() + "'");
    for (ServiceRegistration registration : registrations) {
      if (registration.isJobProducer()) {
        ServiceRegistrationInMemoryImpl inMemoryRegistration = (ServiceRegistrationInMemoryImpl) registration;
        JobProducer service = inMemoryRegistration.getService();
        service.acceptJob(job, job.getOperation(), job.getArguments());
        break;
      } else {
        logger.warn("This implementation of the service registry doesn't support dispatching to remote services");
        // TODO: Add remote dispatching
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#updateJob(org.opencastproject.job.api.Job)
   */
  @Override
  public Job updateJob(Job job) throws NotFoundException, ServiceRegistryException {
    // Nothing to do, everything is in memory only anyway
    return job;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getJob(long)
   */
  @Override
  public Job getJob(long id) throws NotFoundException, ServiceRegistryException {
    synchronized (jobs) {
      for (Job job : jobs) {
        if (id == job.getId())
          return job;
      }
    }
    throw new NotFoundException(Long.toString(id));
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getJobs(java.lang.String,
   *      org.opencastproject.job.api.Job.Status)
   */
  @Override
  public List<Job> getJobs(String serviceType, Status status) throws ServiceRegistryException {
    List<Job> result = new ArrayList<Job>();
    synchronized (jobs) {
      for (Job job : jobs) {
        if (serviceType.equals(job.getJobType()) && status.equals(job.getStatus()))
          result.add(job);
      }
    }
    return result;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistrationsByLoad(java.lang.String)
   */
  @Override
  public List<ServiceRegistration> getServiceRegistrationsByLoad(String serviceType) throws ServiceRegistryException {
    return getServiceRegistrationsByType(serviceType);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistrationsByType(java.lang.String)
   */
  @Override
  public List<ServiceRegistration> getServiceRegistrationsByType(String serviceType) throws ServiceRegistryException {
    List<ServiceRegistration> result = new ArrayList<ServiceRegistration>();
    for (List<ServiceRegistrationInMemoryImpl> servicesPerHost : services.values()) {
      for (ServiceRegistrationInMemoryImpl r : servicesPerHost) {
        if (serviceType.equals(r.getServiceType()))
          result.add(r);
      }
    }
    return result;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistrationsByHost(java.lang.String)
   */
  @Override
  public List<ServiceRegistration> getServiceRegistrationsByHost(String host) throws ServiceRegistryException {
    List<ServiceRegistration> result = new ArrayList<ServiceRegistration>();
    List<ServiceRegistrationInMemoryImpl> servicesPerHost = services.get(host);
    if (servicesPerHost != null) {
      result.addAll(servicesPerHost);
    }
    return result;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistration(java.lang.String,
   *      java.lang.String)
   */
  @Override
  public ServiceRegistration getServiceRegistration(String serviceType, String host) throws ServiceRegistryException {
    List<ServiceRegistrationInMemoryImpl> servicesPerHost = services.get(host);
    if (servicesPerHost != null) {
      for (ServiceRegistrationInMemoryImpl r : servicesPerHost) {
        if (serviceType.equals(r.getServiceType()))
          return r;
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistrations()
   */
  @Override
  public List<ServiceRegistration> getServiceRegistrations() throws ServiceRegistryException {
    List<ServiceRegistration> result = new ArrayList<ServiceRegistration>();
    for (List<ServiceRegistrationInMemoryImpl> servicesPerHost : services.values()) {
      result.addAll(servicesPerHost);
    }
    return result;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceStatistics()
   */
  @Override
  public List<ServiceStatistics> getServiceStatistics() throws ServiceRegistryException {
    throw new IllegalStateException("Operation not yet implemented");
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#count(java.lang.String,
   *      org.opencastproject.job.api.Job.Status)
   */
  @Override
  public long count(String serviceType, Status status) throws ServiceRegistryException {
    int count = 0;
    synchronized (jobs) {
      for (Job job : jobs) {
        if (serviceType.equals(job.getJobType()) && status.equals(job.getStatus()))
          count++;
      }
    }
    return count;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#count(java.lang.String,
   *      org.opencastproject.job.api.Job.Status, java.lang.String)
   */
  @Override
  public long count(String serviceType, Status status, String host) throws ServiceRegistryException {
    int count = 0;
    synchronized (jobs) {
      for (Job job : jobs) {
        if (serviceType.equals(job.getJobType()) && status.equals(job.getStatus()))
          count++;
      }
    }
    return count;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getLoad()
   */
  @Override
  public SystemLoad getLoad() throws ServiceRegistryException {
    throw new IllegalStateException("Not yet implemented");
  }

  /**
   * This dispatcher implementation will wake from time to time and check for new jobs. If new jobs are found, it will
   * dispatch them to the services as appropriate.
   */
  class JobDispatcher extends Thread {

    /** Running flag */
    private boolean keepRunning = true;

    /** Dispatcher timeout */
    private long timeout = ServiceRegistryInMemoryImpl.DEFAULT_DISPATCHER_TIMEOUT;

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
          synchronized (jobs) {
            for (Job job : jobs) {
              if (Status.QUEUED.equals(job.getStatus())) {
                job.setStatus(Status.RUNNING);
                JobWorker worker = new JobWorker(job);
                worker.start();
              }
            }
          }

          Thread.sleep(timeout);
        } catch (InterruptedException e) {
          ServiceRegistryInMemoryImpl.logger.debug("Job dispatcher thread activated");
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

  /**
   * Thread that will try to execute a single job.
   */
  class JobWorker extends Thread {

    /** The job to work on */
    private Job job = null;

    /**
     * Creates a new worker that will try to get the job <code>job</code> done.
     * 
     * @param job
     *          the job to execute
     */
    public JobWorker(Job job) {
      this.job = job;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
      try {
        dispatchJob(job);
      } catch (ServiceUnavailableException e) {
        job.setStatus(Status.FAILED);
        Throwable cause = (e.getCause() != null) ? e.getCause() : e;
        logger.error("Unable to find a service for job " + job, cause);
      } catch (ServiceRegistryException e) {
        job.setStatus(Status.FAILED);
        Throwable cause = (e.getCause() != null) ? e.getCause() : e;
        logger.error("Error dispatching job " + job, cause);
      }
    }

  }

}
