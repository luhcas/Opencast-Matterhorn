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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

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
   * @return the service registration
   */
  ServiceRegistration registerService(String serviceType, String host, String path);

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
   * @return the service registration
   */
  ServiceRegistration registerService(String serviceType, String host, String path, boolean jobProducer);

  /**
   * Unregisters a host from handling a specific type of job
   * 
   * @param serviceType
   *          The service type
   * @param host
   *          The base URL where the service that can handle this job type can be found
   * @param path
   *          The path to the service endpoint
   */
  void unRegisterService(String serviceType, String host, String path);

  /**
   * Sets a registered host's maintenance status
   * 
   * @param serviceType
   *          The service type
   * @param host
   *          The base URL where the service that can handle this service type can be found
   * @param maintenance
   *          the new maintenance status for this service
   * @throws IllegalStateException
   *           if this is called for a jobType and baseUrl that is not registered
   */
  void setMaintenanceStatus(String serviceType, String host, boolean maintenance) throws IllegalStateException;

  /**
   * Parses an xml string representing a Receipt
   * 
   * @param xml
   *          The xml string
   * @return The job TODO: Move this out of the service if possible
   */
  Job parseJob(String xml) throws IOException;

  /**
   * Parses an xml stream representing a Receipt
   * 
   * @param in
   *          The xml input stream
   * @return The receipt
   */
  Job parseJob(InputStream in) throws IOException;

  /**
   * Create and store a new job in {@link Status#QUEUED}
   * 
   * @return the job
   */
  Job createJob(String type);

  /**
   * Update the job in the database
   * 
   * @param job
   */
  void updateJob(Job job);

  /**
   * Gets a receipt by its ID, or null if not found
   * 
   * @param id
   *          the job id
   * @return the job or null
   */
  Job getJob(String id);

  /**
   * Finds the service registrations for this kind of job, ordered by load (lightest to heaviest).
   * 
   * @param serviceType
   *          The type of service that must be handled by the hosts
   * @return A list of hosts that handle this job type, in order of their running and queued job load
   */
  List<ServiceRegistration> getServiceRegistrations(String serviceType);

  /**
   * Finds a single service registration by host and type.
   * 
   * @param serviceType
   *          The type of service
   * @param host
   *          the base URL of the host
   * @return The service registration, or null
   */
  ServiceRegistration getServiceRegistration(String serviceType, String host);

  /**
   * Finds all service registrations.
   * 
   * @return A list of service registrations
   */
  List<ServiceRegistration> getServiceRegistrations();

  /**
   * Gets performance and runtime statistics for each known service registration.
   * 
   * @return the service statistics
   */
  List<ServiceStatistics> getServiceStatistics();

  /**
   * Count the number of receipts of this type in this {@link Status} across all hosts
   * 
   * @param serviceType
   *          The jobs run by this type of service
   * @param status
   *          The status of the receipts
   * @return the number of jobs
   */
  long count(String serviceType, Status status);

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
   */
  long count(String serviceType, Status status, String host);

}
