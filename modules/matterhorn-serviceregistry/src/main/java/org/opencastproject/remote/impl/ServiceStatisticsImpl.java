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

import org.opencastproject.remote.api.ServiceRegistration;
import org.opencastproject.remote.api.ServiceStatistics;

/**
 * Statistics for a service registration.
 */
public class ServiceStatisticsImpl implements ServiceStatistics {

  /** The service registration **/
  protected ServiceRegistrationImpl serviceRegistration;

  /** The mean run time for jobs **/
  protected long meanRunTime;
  
  /** The mean queue time for jobs **/
  protected long meanQueueTime;
  
  /** The number of currently running jobs **/
  protected int runningJobs;

  /** The number of currently queued jobs **/
  protected int queuedJobs;

  /**
   * Constructs a new service statistics instance without statistics.
   * 
   * @param serviceRegistration the service registration
   */
  public ServiceStatisticsImpl(ServiceRegistrationImpl serviceRegistration) {
    super();
    this.serviceRegistration = serviceRegistration;
  }

  /**
   * Constructs a new service statistics instance with statistics.
   * 
   * @param serviceRegistration the service registration
   * @param meanRunTime
   * @param meanQueueTime
   * @param runningJobs
   * @param queuedJobs
   */
  public ServiceStatisticsImpl(ServiceRegistrationImpl serviceRegistration, long meanRunTime, long meanQueueTime,
          int runningJobs, int queuedJobs) {
    this(serviceRegistration);
    this.meanRunTime = meanRunTime;
    this.meanQueueTime = meanQueueTime;
    this.runningJobs = runningJobs;
    this.queuedJobs = queuedJobs;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.remote.api.ServiceStatistics#getMeanQueueTime()
   */
  @Override
  public long getMeanQueueTime() {
    return meanQueueTime;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.remote.api.ServiceStatistics#getMeanRunTime()
   */
  @Override
  public long getMeanRunTime() {
    return meanRunTime;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.remote.api.ServiceStatistics#getQueuedJobs()
   */
  @Override
  public int getQueuedJobs() {
    return queuedJobs;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.remote.api.ServiceStatistics#getRunningJobs()
   */
  @Override
  public int getRunningJobs() {
    return runningJobs;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.remote.api.ServiceStatistics#getServiceRegistration()
   */
  @Override
  public ServiceRegistration getServiceRegistration() {
    return serviceRegistration;
  }
  
}
