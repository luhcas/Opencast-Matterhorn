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

import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.serviceregistry.api.ServiceUnavailableException;
import org.opencastproject.util.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Base implementation for job producer REST endpoints.
 */
public abstract class JobProducerRestEndpointSupport {

  /** To enable threading when dispatching jobs to the service */
  protected ExecutorService executor = Executors.newCachedThreadPool();

  /**
   * Sets the service registry for this job producer endpoint.
   * 
   * @param serviceRegistry
   *          the service registry to use when accepting jobs to run
   */
  protected abstract void setServiceRegistry(ServiceRegistry serviceRegistry);

  /** Gets the service registry for this job handler */
  protected abstract ServiceRegistry getServiceRegistry();
  
  /**
   * @see org.opencastproject.job.api.JobProducer#getJob(long)
   */
  @GET
  @Path("/job/{id}.xml")
  @Produces(MediaType.TEXT_XML)
  public JaxbJob getJob(@PathParam("id") long jobId) throws ServiceRegistryException {
    final JobProducer service = getService();
    if (service == null)
      throw new WebApplicationException(Status.PRECONDITION_FAILED);
    try {
      return new JaxbJob(service.getJob(jobId));
    } catch (NotFoundException e) {
      throw new WebApplicationException(Status.NOT_FOUND);
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
  }

  /**
   * @see org.opencastproject.job.api.JobProducer#startJob(org.opencastproject.job.api.Job, java.lang.String,
   *      java.util.List)
   */
  @POST
  @Path("/dispatch")
  public Response dispatchJob(@FormParam("job") String jobXml) throws ServiceRegistryException {
    final JobProducer service = getService();
    if (service == null)
      throw new WebApplicationException(Status.PRECONDITION_FAILED);

    final Job job;
    try {
      job = JobParser.parseJob(jobXml);
    } catch (IOException e) {
      throw new WebApplicationException(Status.BAD_REQUEST);
    }

    // Finally, have the service execute the job
    executor.submit(new JobRunner(job, service, getServiceRegistry()));

    return Response.noContent().build();
  }

  /**
   * A utility class to run jobs
   */
  static class JobRunner implements Runnable {
    /** The logger */
    private static final Logger logger = LoggerFactory.getLogger(JobRunner.class);

    /** The job */
    private Job job = null;

    /** The job producer */
    private JobProducer service = null;

    /** The service registry */
    private ServiceRegistry serviceRegistry = null;

    /**
     * Constructs a new job runner
     * 
     * @param job
     *          the job to run
     * @param service
     *          the service to execute the job
     */
    JobRunner(Job job, JobProducer service, ServiceRegistry serviceRegistry) {
      this.job = job;
      this.service = service;
      this.serviceRegistry = serviceRegistry;
    }

    @Override
    public void run() {
      try {
        logger.debug("Attempting to update job {} to status RUNNING", job);
        job.setStatus(Job.Status.RUNNING);
        serviceRegistry.updateJob(job);
        logger.debug("Updated job {} to status RUNNING", job);
        service.startJob(job, job.getOperation(), job.getArguments());
      } catch (ServiceRegistryException e) {
        logger.warn("Unable to start job {}", job, e);
      } catch (NotFoundException e) {
        logger.warn("Unable to start job {} because the job could not be found", job, e);
      } catch (ServiceUnavailableException e) {
        logger.warn("Unable to start job {} because the service registry is not available", job, e);
      } catch(Exception e) {
        logger.warn("Unable to start job {}", job);
        e.printStackTrace();
      }
    }

  }

  /**
   * Returns the job producer that is backing this REST endpoint.
   * 
   * @return the job producer
   */
  public abstract JobProducer getService();

}
