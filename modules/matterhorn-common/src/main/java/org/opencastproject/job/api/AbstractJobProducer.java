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
package org.opencastproject.job.api;

import org.opencastproject.job.api.Job.Status;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.NotFoundException;

import java.util.List;

/**
 * This class serves as a convenience for services that implement the {@link JobProducer} api to deal with handling long
 * running, asynchronous operations.
 * <p>
 * Subclasses need to make sure to call {@link #setServiceRegistry}
 */
public abstract class AbstractJobProducer implements JobProducer {

  /** The types of job that this producer can handle */
  protected String jobType = null;

  /**
   * Creates a new abstract job producer for jobs of the given type.
   * 
   * @param jobType
   *          the job type
   */
  public AbstractJobProducer(String jobType) {
    this.jobType = jobType;
  }
  
  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JobProducer#getJobType()
   */
  @Override
  public String getJobType() {
    return jobType;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JobProducer#countJobs(org.opencastproject.job.api.Job.Status)
   */
  @Override
  public long countJobs(Status status) throws ServiceRegistryException {
    if (status == null)
      throw new IllegalArgumentException("Status must not be null");
    return getServiceRegistry().count(getJobType(), status);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JobProducer#acceptJob(org.opencastproject.job.api.Job, java.lang.String,
   *      java.util.List)
   */
  @Override
  public void acceptJob(Job job, String operation, List<String> arguments) throws ServiceRegistryException {
    try {
      String result = process(job, operation, arguments);
      job.setPayload(result);
      job.setStatus(Status.FINISHED);
    } catch (Exception e) {
      job.setStatus(Status.FAILED);
      if (e instanceof ServiceRegistryException)
        throw (ServiceRegistryException) e;
      throw new ServiceRegistryException("Error handling operation '" + operation + "': " + e.getMessage(), e);
    } finally {
      try {
        getServiceRegistry().updateJob(job);
      } catch (NotFoundException e) {
        throw new ServiceRegistryException(e);
      }
    }
  }

  /**
   * Returns a reference to the service registry.
   * 
   * @return the service registry
   */
  protected abstract ServiceRegistry getServiceRegistry();

  /**
   * Asks the overriding class to process the arguments using the given operation. The result will be added to the
   * associated job as the payload.
   * 
   * @param job
   *          TODO
   * @param operation
   *          the operation name
   * @param arguments
   *          the list of arguments for the operation
   * 
   * @return the operation result
   * @throws Exception
   */
  abstract protected String process(Job job, String operation, List<String> arguments) throws Exception;

}
