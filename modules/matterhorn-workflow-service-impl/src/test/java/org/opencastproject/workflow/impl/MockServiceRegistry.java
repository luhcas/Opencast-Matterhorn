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
package org.opencastproject.workflow.impl;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.serviceregistry.api.ServiceRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.serviceregistry.api.ServiceStatistics;
import org.opencastproject.serviceregistry.api.ServiceUnavailableException;
import org.opencastproject.serviceregistry.api.SystemLoad;
import org.opencastproject.util.NotFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * An in-memory mock service registry.
 */
public class MockServiceRegistry implements ServiceRegistry {
  Map<Long, Job> jobMap = new HashMap<Long, Job>();

  @Override
  public long count(String serviceType, Job.Status status) throws ServiceRegistryException {
    return jobMap.size();
  }

  @Override
  public long count(String serviceType, Job.Status status, String host) throws ServiceRegistryException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Job getJob(long id) throws NotFoundException, ServiceRegistryException {
    Job j = jobMap.get(id);
    if (j == null)
      throw new NotFoundException();
    return j;
  }

  @Override
  public Job createJob(String type, String operation, List<String> arguments) throws ServiceUnavailableException, ServiceRegistryException {
    Job j = new MockJob();
    jobMap.put(j.getId(), j);
    return j;
  }

  @Override
  public Job createJob(String type, String operation, List<String> arguments, boolean start) throws ServiceUnavailableException, ServiceRegistryException {
    Job j = new MockJob();
    jobMap.put(j.getId(), j);
    j.setStatus(Job.Status.RUNNING);
    return j;
  }

  @Override
  public ServiceRegistration getServiceRegistration(String serviceType, String host) throws ServiceRegistryException {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<ServiceRegistration> getServiceRegistrations() throws ServiceRegistryException {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<ServiceRegistration> getServiceRegistrationsByHost(String host) throws ServiceRegistryException {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<ServiceRegistration> getServiceRegistrationsByLoad(String serviceType) throws ServiceRegistryException {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<ServiceRegistration> getServiceRegistrationsByType(String serviceType) throws ServiceRegistryException {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<ServiceStatistics> getServiceStatistics() throws ServiceRegistryException {
    throw new UnsupportedOperationException();
  }

  @Override
  public ServiceRegistration registerService(String serviceType, String host, String path)
          throws ServiceRegistryException {
    throw new UnsupportedOperationException();
  }

  @Override
  public ServiceRegistration registerService(String serviceType, String host, String path, boolean jobProducer)
          throws ServiceRegistryException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setMaintenanceStatus(String host, boolean maintenance) throws ServiceUnavailableException,
          ServiceRegistryException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void unRegisterService(String serviceType, String host) throws ServiceRegistryException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Job updateJob(Job job) throws NotFoundException, ServiceRegistryException, ServiceUnavailableException {
    jobMap.put(job.getId(), job);
    return job;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getJobs(java.lang.String,
   *      org.opencastproject.job.api.Job.Status)
   */
  @Override
  public List<Job> getJobs(String serviceType, Status status) throws ServiceRegistryException {
    List<Job> list = new ArrayList<Job>();
    for (Entry<Long, Job> entry : jobMap.entrySet()) {
      list.add(entry.getValue());
    }
    return list;
  }

  @Override
  public void registerHost(String host, int maxConcurrentJobs) throws ServiceRegistryException {
  }

  @Override
  public void unregisterHost(String host) throws ServiceRegistryException {
  }

  @Override
  public SystemLoad getLoad() throws ServiceRegistryException {
    throw new UnsupportedOperationException();
  }
}
