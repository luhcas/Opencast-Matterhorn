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
package org.opencastproject.remote.api;

import org.opencastproject.remote.api.Job.Status;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Manages clustered services and the {@link Job}s they may create to enable asynchronous job handling.
 */
public interface RemoteServiceManager {

  /**
   * Registers a host to handle a specific type of job
   * 
   * @param jobType
   *          The job type
   * @param baseUrl
   *          The base URL where the service that can handle this job type can be found
   * @return the service registration
   */
  ServiceRegistration registerService(String jobType, String baseUrl);

  /**
   * Unregisters a host from handling a specific type of job
   * 
   * @param jobType
   *          The job type
   * @param baseUrl
   *          The base URL where the service that can handle this job type can be found
   */
  void unRegisterService(String jobType, String baseUrl);

  /**
   * Sets a registered host's maintenance status
   * 
   * @param jobType
   *          The job type
   * @param baseUrl
   *          The base URL where the service that can handle this job type can be found
   * @param maintenance
   *          the new maintenance status for this service
   * @throws IllegalStateException
   *           if this is called for a jobType and baseUrl that is not registered
   */
  void setMaintenanceStatus(String jobType, String baseUrl, boolean maintenance) throws IllegalStateException;

  /**
   * Parses an xml string representing a Receipt
   * 
   * @param xml
   *          The xml string
   * @return The job
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
   * Finds the servers registered to handle this kind of job, ordered by their load (lightest to heaviest).
   * 
   * @param jobType
   *          The type of job that must be handled by the hosts
   * @return A list of hosts that handle this job type, in order of their running and queued job load
   */
  List<String> getActiveHosts(String jobType);

  /**
   * Finds all current service registrations.
   * 
   * @return the current service registrations
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
   * @param jobType
   *          The type of jobs to count
   * @param status
   *          The status of the receipts
   * @return the number of jobs
   */
  long count(String jobType, Status status);

  /**
   * Count the number of jobs in this {@link Status} on this host
   * 
   * @param type
   *          The type of jobs
   * @param status
   *          The status of the jobs
   * @param host
   *          The server that created and will be handling the job
   * @return the number of jobs
   */
  long count(String type, Status status, String host);

}
