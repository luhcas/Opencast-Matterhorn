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
package org.opencastproject.serviceregistry.impl.endpoint;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.net.URI;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobParser;
import org.opencastproject.rest.RestPublisher;
import org.opencastproject.serviceregistry.api.JaxbServiceRegistration;
import org.opencastproject.serviceregistry.api.JaxbServiceRegistrationList;
import org.opencastproject.serviceregistry.api.JaxbServiceStatisticsList;
import org.opencastproject.serviceregistry.api.ServiceRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.serviceregistry.impl.ServiceRegistryJpaImpl;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.Param.Type;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;
import org.osgi.service.component.ComponentContext;

/**
 * Displays hosts and the service IDs they provide.
 */
@Path("")
public class ServiceRegistryEndpoint {

  /** The remote service maanger */
  protected ServiceRegistry serviceRegistry = null;

  /** This server's URL */
  protected String serverUrl = UrlSupport.DEFAULT_BASE_URL;

  /** The service path for this endpoint */
  protected String servicePath = "/";

  /** The runtime documentation for this endpoint */
  protected String docs = null;

  /** Sets the service registry instance for delegation */
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * Callback from OSGi that is called when this service is activated.
   * 
   * @param cc
   *          OSGi component context
   */
  public void activate(ComponentContext cc) {
    serverUrl = (String) cc.getBundleContext().getProperty("org.opencastproject.server.url");
    servicePath = (String) cc.getProperties().get(RestPublisher.SERVICE_PATH_PROPERTY);
    docs = generateDocs(servicePath);
  }

  @GET
  @Path("/docs")
  @Produces(MediaType.TEXT_HTML)
  public String getDocs() {
    return docs;
  }

  @GET
  @Path("/statistics.json")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getStatisticsAsJson() {
    try {
      return Response.ok(new JaxbServiceStatisticsList(serviceRegistry.getServiceStatistics())).build();
    } catch (ServiceRegistryException e) {
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Path("/statistics.xml")
  @Produces(MediaType.TEXT_XML)
  public Response getStatisticsAsXml() throws ServiceRegistryException {
    return getStatisticsAsJson();
  }

  @POST
  @Path("/register")
  @Produces(MediaType.TEXT_XML)
  public JaxbServiceRegistration register(@FormParam("serviceType") String serviceType, @FormParam("host") String host,
          @FormParam("path") String path, @FormParam("jobProducer") boolean jobProducer) {
    try {
      return new JaxbServiceRegistration(serviceRegistry.registerService(serviceType, host, path, jobProducer));
    } catch (ServiceRegistryException e) {
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Path("/unregister")
  public Response unregister(@FormParam("serviceType") String serviceType, @FormParam("host") String host) {
    try {
      serviceRegistry.unRegisterService(serviceType, host);
      return Response.status(Status.NO_CONTENT).build();
    } catch (ServiceRegistryException e) {
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Path("/maintenance")
  public Response maintenance(@FormParam("serviceType") String serviceType, @FormParam("host") String host,
          @FormParam("maintenance") boolean maintenance) {
    try {
      serviceRegistry.setMaintenanceStatus(serviceType, host, maintenance);
      return Response.status(Status.NO_CONTENT).build();
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("/available.xml")
  @Produces(MediaType.TEXT_XML)
  public Response getAvailableServicesAsXml(@QueryParam("serviceType") String serviceType) {
    if (isBlank(serviceType))
      throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Service type must be specified")
              .build());
    JaxbServiceRegistrationList registrations = new JaxbServiceRegistrationList();
    try {
      for (ServiceRegistration reg : serviceRegistry.getServiceRegistrationsByLoad(serviceType)) {
        registrations.add(new JaxbServiceRegistration(reg));
      }
      return Response.ok(registrations).build();
    } catch (ServiceRegistryException e) {
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("/available.json")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAvailableServicesAsJson(@QueryParam("serviceType") String serviceType) {
    return getAvailableServicesAsXml(serviceType);
  }
  
  @GET
  @Path("/services.xml")
  @Produces(MediaType.TEXT_XML)
  public Response getRegistrationsAsXml(@QueryParam("serviceType") String serviceType, @QueryParam("host") String host) {
    JaxbServiceRegistrationList registrations = new JaxbServiceRegistrationList();
    try {
      if (isNotBlank(serviceType) && isNotBlank(host)) {
        // This is a request for one specific service. Return it, or 404 if not found
        ServiceRegistration reg = serviceRegistry.getServiceRegistration(serviceType, host);
        if (reg == null) {
          return Response.status(Status.NOT_FOUND).build();
        } else {
          return Response.ok(new JaxbServiceRegistration(reg)).build();
        }
      } else if (isBlank(serviceType) && isBlank(host)) {
        // This is a request for all service registrations
        for (ServiceRegistration reg : serviceRegistry.getServiceRegistrations()) {
          registrations.add(new JaxbServiceRegistration(reg));
        }
      } else if (isNotBlank(serviceType)) {
        // This is a request for all service registrations of a particular type
        for (ServiceRegistration reg : serviceRegistry.getServiceRegistrationsByLoad(serviceType)) {
          registrations.add(new JaxbServiceRegistration(reg));
        }
      } else if (isNotBlank(host)) {
        // This is a request for all service registrations of a particular host
        for (ServiceRegistration reg : serviceRegistry.getServiceRegistrationsByHost(host)) {
          registrations.add(new JaxbServiceRegistration(reg));
        }
      }
      return Response.ok(registrations).build();
    } catch (ServiceRegistryException e) {
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("/services.json")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getRegistrationsAsJson(@QueryParam("serviceType") String serviceType, @QueryParam("host") String host) {
    return getRegistrationsAsXml(serviceType, host);
  }

  @POST
  @Path("/job")
  @Produces(MediaType.TEXT_XML)
  public Response createJob(@FormParam("jobType") String jobType, @FormParam("host") String host) {
    try {
      Job job = ((ServiceRegistryJpaImpl) serviceRegistry).createJob(jobType, host);
      URI uri = new URI(UrlSupport.concat(new String[] { serverUrl, servicePath, "job", job.getId() + ".xml" }));
      return Response.ok(new JaxbJob(job)).location(uri).build();
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
  }

  @PUT
  @Path("/job/{id}.xml")
  @Produces(MediaType.TEXT_XML)
  public Response updateJob(@PathParam("id") String id, @FormParam("jobType") String jobXml) {
    try {
      Job job = JobParser.parseJob(jobXml);
      serviceRegistry.updateJob(job);
      return Response.status(Status.NO_CONTENT).build();
    } catch (NotFoundException e) {
      return Response.status(Status.NOT_FOUND).build();
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("/job/{id}.xml")
  @Produces(MediaType.TEXT_XML)
  public Response getJobAsXml(@PathParam("id") String id) {
    try {
      Job job = serviceRegistry.getJob(id);
      return Response.ok(new JaxbJob(job)).build();
    } catch (NotFoundException e) {
      return Response.status(Status.NOT_FOUND).build();
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("/job/{id}.json")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getJobAsJson(@PathParam("id") String id) {
    try {
      Job job = serviceRegistry.getJob(id);
      if (job == null) {
        return Response.status(Status.NOT_FOUND).build();
      } else {
        return Response.ok(new JaxbJob(job)).build();
      }
    } catch (NotFoundException e) {
      return Response.status(Status.NOT_FOUND).build();
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("/count")
  @Produces(MediaType.TEXT_PLAIN)
  public long count(@QueryParam("serviceType") String serviceType, @QueryParam("status") Job.Status status,
          @QueryParam("host") String host) {
    if (isBlank(serviceType) || status == null) {
      throw new WebApplicationException(Response.serverError().entity("service type and status must not be null.")
              .build());
    }
    try {
      if (isBlank(host)) {
        return serviceRegistry.count(serviceType, status);
      } else {
        return serviceRegistry.count(serviceType, status, host);
      }
    } catch (ServiceRegistryException e) {
      throw new WebApplicationException(e);
    }
  }

  protected String generateDocs(String serviceUrl) {
    DocRestData data = new DocRestData("serviceregistry", "Service Registry", serviceUrl, null);
    data.setAbstract("This service lists the members of this cluster.");

    // statistics
    RestEndpoint statistics = new RestEndpoint("stats", RestEndpoint.Method.GET, "/statistics.{format}",
            "List the service registrations in the cluster, along with some simple statistics.");
    statistics.addPathParam(new Param("format", Type.STRING, "xml", "the output format"));
    statistics.addFormat(new Format("xml", null, null));
    statistics.addFormat(new Format("json", null, null));
    statistics.addStatus(org.opencastproject.util.doc.Status.OK("Returns the service statistics."));
    statistics.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, statistics);

    // services
    RestEndpoint getServicesEndpoint = new RestEndpoint("services", RestEndpoint.Method.GET, "/services.{format}",
            "Get a list of services matching the query criteria");
    getServicesEndpoint.addOptionalParam(new Param("serviceType", Type.STRING, "org.opencastproject.[type]",
            "The service identifier"));
    getServicesEndpoint.addOptionalParam(new Param("host", Type.STRING, serverUrl, "The host providing the service"));
    getServicesEndpoint.addPathParam(new Param("format", Type.STRING, "json", "The format, xml or json"));
    getServicesEndpoint.addFormat(new Format("xml", null, null));
    getServicesEndpoint.addFormat(new Format("json", null, null));
    getServicesEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("Returns the services list."));
    getServicesEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getServicesEndpoint);

    // available services ordered by load
    RestEndpoint getServicesByLoadEndpoint = new RestEndpoint("available", RestEndpoint.Method.GET,
            "/available.{format}", "Get a list of available services ordered by load");
    getServicesByLoadEndpoint.addOptionalParam(new Param("serviceType", Type.STRING, "org.opencastproject.[type]",
            "The service identifier"));
    getServicesByLoadEndpoint.addPathParam(new Param("format", Type.STRING, "json", "The format, xml or json"));
    getServicesByLoadEndpoint.addFormat(new Format("xml", null, null));
    getServicesByLoadEndpoint.addFormat(new Format("json", null, null));
    getServicesByLoadEndpoint.addStatus(org.opencastproject.util.doc.Status
            .OK("Returns the services list, ordered from least to most loaded."));
    getServicesByLoadEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getServicesByLoadEndpoint);

    // register
    RestEndpoint registerEndpoint = new RestEndpoint("register", RestEndpoint.Method.POST, "/register",
            "Add a new service registration to the cluster.");
    registerEndpoint.addRequiredParam(new Param("serviceType", Type.STRING, "org.opencastproject.[type]",
            "The service identifier"));
    registerEndpoint
            .addRequiredParam(new Param("host", Type.STRING, serverUrl, "The host's base URL for this service"));
    registerEndpoint.addRequiredParam(new Param("path", Type.STRING, "/myservice/rest",
            "the path on the server responsible for handling this type of service"));
    registerEndpoint.addRequiredParam(new Param("jobProducer", Type.BOOLEAN, "false",
            "whether this service produces jobs, which track long running operations"));
    registerEndpoint.addFormat(new Format("xml", null, null));
    registerEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("Returns the service registration."));
    registerEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, registerEndpoint);

    // unregister
    RestEndpoint unRegisterEndpoint = new RestEndpoint("unregister", RestEndpoint.Method.POST, "/unregister",
            "Remove a service registration from the cluster.");
    unRegisterEndpoint.addRequiredParam(new Param("serviceType", Type.STRING, "org.opencastproject.[type]",
            "The service identifier"));
    unRegisterEndpoint.addRequiredParam(new Param("host", Type.STRING, serverUrl,
            "The host's base URL for this service"));
    unRegisterEndpoint.addFormat(new Format("xml", null, null));
    unRegisterEndpoint.addStatus(org.opencastproject.util.doc.Status
            .NO_CONTENT("The service registration was removed."));
    unRegisterEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, unRegisterEndpoint);

    // count jobs
    RestEndpoint countEndpoint = new RestEndpoint("count", RestEndpoint.Method.GET, "/count",
            "Count the number of jobs.");
    countEndpoint.addOptionalParam(new Param("serviceType", Type.STRING, "org.opencastproject.[type]",
            "The service identifier"));
    countEndpoint.addOptionalParam(new Param("status", Type.STRING, "FINISHED",
            "The job status: QUEUED, RUNNING, FINISHED, or FAILED"));
    countEndpoint.addOptionalParam(new Param("host", Type.STRING, serverUrl, "The host's base URL for this service"));
    countEndpoint.addFormat(new Format("plain", null, null));
    countEndpoint.addStatus(org.opencastproject.util.doc.Status
            .OK("The number of jobs matching the request criteria has been returned in the http body."));
    countEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, countEndpoint);

    // create a job
    RestEndpoint createJobEndpoint = new RestEndpoint("create", RestEndpoint.Method.POST, "/job",
            "Creates a new job in the queued state");
    createJobEndpoint.addRequiredParam(new Param("jobType", Type.STRING, "org.opencastproject.[type]",
            "The service that is creating and handling this job"));
    createJobEndpoint.addFormat(new Format("xml", null, null));
    createJobEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("Returns the new job."));
    createJobEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, createJobEndpoint);

    // getJob
    RestEndpoint getJobEndpoint = new RestEndpoint("job", RestEndpoint.Method.GET, "/job/{id}.{format}",
            "Get a job by its identifier");
    getJobEndpoint.addPathParam(new Param("id", Type.STRING, null, "The job identifier"));
    getJobEndpoint.addPathParam(new Param("format", Type.STRING, "json", "The format, xml or json"));
    getJobEndpoint.addFormat(new Format("xml", null, null));
    getJobEndpoint.addFormat(new Format("json", null, null));
    getJobEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("Returns the job."));
    getJobEndpoint.addStatus(org.opencastproject.util.doc.Status.NOT_FOUND("No job with this identifier exists."));
    getJobEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getJobEndpoint);

    return DocUtil.generate(data);
  }

}
