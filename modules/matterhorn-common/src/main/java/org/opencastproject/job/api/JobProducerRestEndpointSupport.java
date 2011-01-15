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

import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.NotFoundException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

    Job job;
    try {
      job = JobParser.parseJob(jobXml);
      
      // Due to our optimistic locking strategy, we need to re-read the job from the database, in order to obtain
      // the correct object version
      final Job dbJob = getService().getJob(job.getId());

      // Finally, have the service execute the job
      final List<Exception> errors = new ArrayList<Exception>();
      executor.execute(new Runnable() {
        @Override
        public void run() {
          try {
            service.startJob(dbJob, dbJob.getOperation(), dbJob.getArguments());
          } catch (Exception e) {
            errors.add(e);
          }
        }
      });
    
      // Have there been any errors?
      if (errors.size() > 0)
        throw new WebApplicationException(errors.get(0));

      return Response.ok().build();
    } catch (IOException e) {
      throw new WebApplicationException(e);
    } catch (NotFoundException e) {
      throw new WebApplicationException(e);
    }
  }

  /**
   * Returns the job producer that is backing this REST endpoint.
   * 
   * @return the job producer
   */
  public abstract JobProducer getService();

}
