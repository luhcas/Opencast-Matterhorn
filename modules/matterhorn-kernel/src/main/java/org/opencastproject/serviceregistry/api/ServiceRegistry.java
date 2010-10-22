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

import java.util.List;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.util.NotFoundException;

/**
 * Manages clustered services and the {@link Job}s they may create to enable asynchronous job handling.
 */
public interface ServiceRegistry {

  /**
   * Registers a host to handle a specific type of job
   * 
   * @param serviceType
   *          The job type
   * @param host
   *          The base URL where the service that can handle this service type can be found
   * @param path
   *          The path to the service endpoint
   * @throws ServiceRegistryException
   *           if communication with the service registry fails
   * @return the service registration
   */
  ServiceRegistration registerService(String serviceType, String host, String path) throws ServiceRegistryException;

  /**
   * Registers a host to handle a specific type of job
   * 
   * @param serviceType
   *          The service type
   * @param host
   *          The base URL where the service that can handle this job type can be found
   * @param path
   *          The path to the service endpoint
   * @param jobProducer
   *          Whether this service registration produces {@link Job}s to track long running operations
   * @throws ServiceRegistryException
   *           if communication with the service registry fails
   * @return the service registration
   */
  ServiceRegistration registerService(String serviceType, String host, String path, boolean jobProducer)
          throws ServiceRegistryException;

  /**
   * Unregisters a host from handling a specific type of job
   * 
   * @param serviceType
   *          The service type
   * @param host
   *          The base URL where the service that can handle this job type can be found
   * @throws ServiceRegistryException
   *           if communication with the service registry fails
   */
  void unRegisterService(String serviceType, String host) throws ServiceRegistryException;

  /**
   * Sets a registered host's maintenance status
   * 
   * @param serviceType
   *          The service type
   * @param host
   *          The base URL where the service that can handle this service type can be found
   * @param maintenance
   *          the new maintenance status for this service
   * @throws ServiceUnavailableException
   *           if this is called for a jobType and baseUrl that is not registered
   * @throws ServiceRegistryException
   *           if communication with the service registry fails
   */
  void setMaintenanceStatus(String serviceType, String host, boolean maintenance) throws ServiceUnavailableException,
          ServiceRegistryException;

  /**
   * Create and store a new job in {@link Status#QUEUED} state on this host. This is equivalent to calling
   * createJob(type, false).
   * 
   * @param type
   *          the type of service responsible for this job
   * @return the job
   * @throws ServiceRegistryException
   *           if there is a problem creating the job
   * @throws ServiceUnavailableException
   *           if no service registration exists for this job type on this host
   */
  Job createJob(String type) throws ServiceUnavailableException, ServiceRegistryException;

  /**
   * Create and store a new job on this host. If start is true, the job will be in the {@link Status#RUNNING} state.
   * Otherwise, it will be {@link Status#QUEUED}.
   * 
   * @param type
   *          the type of service responsible for this job
   * @param start
   *          whether the job should be created in the running state (true) or the queued state (false)
   * @return the job
   * @throws ServiceRegistryException
   *           if there is a problem creating the job
   * @throws ServiceUnavailableException
   *           if no service registration exists for this job type on this host
   */
  Job createJob(String type, boolean start) throws ServiceUnavailableException, ServiceRegistryException;

  /**
   * Update the job in the database
   * 
   * @param job
   * @throws ServiceRegistryException
   *           if there is a problem updating the job
   * @throws NotFoundException
   *           if the job does not exist
   * @throws ServiceUnavailableException
   *           if no service registration exists for this job
   */
  void updateJob(Job job) throws NotFoundException, ServiceRegistryException, ServiceUnavailableException;

  /**
   * Gets a receipt by its ID, or null if not found
   * 
   * @param id
   *          the job id
   * @return the job or null
   */
  Job getJob(String id) throws NotFoundException, ServiceRegistryException;

  /**
   * Finds the service registrations for this kind of job, ordered by load (lightest to heaviest).
   * 
   * @param serviceType
   *          The type of service that must be handled by the hosts
   * @return A list of hosts that handle this job type, in order of their running and queued job load
   * @throws ServiceRegistryException
   *           if there is a problem accessing the service registry
   */
  List<ServiceRegistration> getServiceRegistrationsByLoad(String serviceType) throws ServiceRegistryException;

  /**
   * Finds the service registrations for this kind of job, including offline services and those in maintenance mode.
   * 
   * @param serviceType
   *          The type of service that must be handled by the hosts
   * @return A list of hosts that handle this job type
   * @throws ServiceRegistryException
   *           if there is a problem accessing the service registry
   */
  List<ServiceRegistration> getServiceRegistrationsByType(String serviceType) throws ServiceRegistryException;

  /**
   * Finds the service registrations on the given host, including offline services and those in maintenance mode.
   * 
   * @param host
   *          The host
   * @return A list of service registrations on a single host
   * @throws ServiceRegistryException
   *           if there is a problem accessing the service registry
   */
  List<ServiceRegistration> getServiceRegistrationsByHost(String host) throws ServiceRegistryException;

  /**
   * Finds a single service registration by host and type, even if the service is offline or in maintenance mode.
   * 
   * @param serviceType
   *          The type of service
   * @param host
   *          the base URL of the host
   * @return The service registration, or null
   * @throws ServiceRegistryException
   *           if there is a problem accessing the service registry
   */
  ServiceRegistration getServiceRegistration(String serviceType, String host) throws ServiceRegistryException;

  /**
   * Finds all service registrations, including offline services and those in maintenance mode.
   * 
   * @return A list of service registrations
   * @throws ServiceRegistryException
   *           if there is a problem accessing the service registry
   */
  List<ServiceRegistration> getServiceRegistrations() throws ServiceRegistryException;

  /**
   * Gets performance and runtime statistics for each known service registration.
   * 
   * @return the service statistics
   * @throws ServiceRegistryException
   *           if there is a problem accessing the service registry
   */
  List<ServiceStatistics> getServiceStatistics() throws ServiceRegistryException;

  /**
   * Count the number of receipts of this type in this {@link Status} across all hosts
   * 
   * @param serviceType
   *          The jobs run by this type of service
   * @param status
   *          The status of the receipts
   * @return the number of jobs
   * @throws ServiceRegistryException
   *           if there is a problem accessing the service registry
   */
  long count(String serviceType, Status status) throws ServiceRegistryException;

  /**
   * Count the number of jobs in this {@link Status} on this host
   * 
   * @param serviceType
   *          The jobs run by this type of service
   * @param status
   *          The status of the jobs
   * @param host
   *          The server that created and will be handling the job
   * @return the number of jobs
   * @throws ServiceRegistryException
   *           if there is a problem accessing the service registry
   */
  long count(String serviceType, Status status, String host) throws ServiceRegistryException;

}
