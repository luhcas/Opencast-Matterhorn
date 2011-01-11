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
import org.opencastproject.util.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Simple and in-memory implementation of a the service registry.
 * 
 */
public class ServiceRegistryInMemoryImpl implements ServiceRegistry {

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ServiceRegistryInMemoryImpl.class);
  
  /** The hosts */
  protected Map<String, Long> hosts = new HashMap<String, Long>();

  /** The service registrations */
  protected Map<String, List<ServiceRegistrationInMemoryImpl>> services = new HashMap<String, List<ServiceRegistrationInMemoryImpl>>();
  
  /** The jobs */
  protected List<Job> jobs = new ArrayList<Job>();

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

    ServiceRegistrationInMemoryImpl registration = new ServiceRegistrationInMemoryImpl(serviceType, host, path, jobProducer);
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
  public void setMaintenanceStatus(String host, boolean maintenance) throws ServiceUnavailableException,
          ServiceRegistryException {
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
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, java.lang.String,
   *      java.util.List)
   */
  @Override
  public Job createJob(String type, String operation, List<String> arguments) throws ServiceUnavailableException,
          ServiceRegistryException {
    return createJob(type, operation, arguments, false);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, java.lang.String,
   *      java.util.List, boolean)
   */
  @Override
  public Job createJob(String type, String operation, List<String> arguments, boolean start)
          throws ServiceUnavailableException, ServiceRegistryException {
    if (getServiceRegistrationsByType(type).size() == 0)
     logger.warn("Service " + type + " not available");
    
    JobImpl job = new JobImpl(System.currentTimeMillis());
    job.setJobType(type);
    job.setOperationType(operation);
    job.setArguments(arguments);
    if (start)
      job.setStatus(Status.RUNNING);
    
    jobs.add(job);

    return job;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#updateJob(org.opencastproject.job.api.Job)
   */
  @Override
  public Job updateJob(Job job) throws NotFoundException, ServiceRegistryException, ServiceUnavailableException {
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
    for (Job job : jobs) {
      if (id == job.getId())
        return job;
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
    for (Job job : jobs) {
      if (serviceType.equals(job.getJobType()) && status.equals(job.getStatus()))
        result.add(job);
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
    for (Job job : jobs) {
      if (serviceType.equals(job.getJobType()) && status.equals(job.getStatus()))
        count ++;
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
    for (Job job : jobs) {
      if (serviceType.equals(job.getJobType()) && status.equals(job.getStatus()))
        count ++;
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

}
