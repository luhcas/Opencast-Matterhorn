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

import org.opencastproject.rest.RestPublisher;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.serviceregistry.impl.ServiceRegistrationJpaImpl;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.Param.Type;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;

import org.osgi.service.component.ComponentContext;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Displays hosts and the service IDs they provide.
 */
@Path("")
public class ServiceRegistryEndpoint {

  /** The remote service maanger */
  protected ServiceRegistry serviceRegistry = null;

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
    String serviceUrl = (String) cc.getProperties().get(RestPublisher.SERVICE_PATH_PROPERTY);
    docs = generateDocs(serviceUrl);
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
  public Response getServicesAsJson() {
    try {
      return Response.ok(new ServiceStatisticsList(serviceRegistry.getServiceStatistics())).build();
    } catch (ServiceRegistryException e) {
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Path("/statistics.xml")
  @Produces(MediaType.TEXT_XML)
  public Response getServicesAsXml() throws ServiceRegistryException {
    return getServicesAsJson();
  }

  @POST
  @Path("/register")
  @Produces(MediaType.APPLICATION_JSON)
  public ServiceRegistrationJpaImpl register(@FormParam("serviceType") String serviceType, @FormParam("host") String host,
          @FormParam("path") String path, @FormParam("jobProducer") boolean jobProducer) {
    try {
      return (ServiceRegistrationJpaImpl) serviceRegistry.registerService(serviceType, host, path, jobProducer);
    } catch (ServiceRegistryException e) {
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Path("/unregister")
  public Response unregister(@FormParam("serviceType") String serviceType, @FormParam("host") String host,
          @FormParam("path") String path) {
    try {
      serviceRegistry.unRegisterService(serviceType, host, path);
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
    } catch (ServiceRegistryException e) {
      throw new WebApplicationException(e);
    }
  }

  protected String generateDocs(String serviceUrl) {
    DocRestData data = new DocRestData("serviceregistry", "Service Registry", serviceUrl, null);
    data.setAbstract("This service lists the members of this cluster.");
    RestEndpoint endpoint = new RestEndpoint("stats", RestEndpoint.Method.GET, "/statistics.{format}",
            "List the service registrations in the cluster, along with some simple statistics.");
    endpoint.addPathParam(new Param("format", Type.STRING, "xml", "the output format"));
    endpoint.addFormat(new Format("xml", null, null));
    endpoint.addFormat(new Format("json", null, null));
    endpoint.addStatus(org.opencastproject.util.doc.Status.OK("Returns the service statistics."));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, endpoint);

    RestEndpoint registerEndpoint = new RestEndpoint("register", RestEndpoint.Method.POST, "/register",
            "Add a new service registration to the cluster.");
    registerEndpoint.addRequiredParam(new Param("serviceType", Type.STRING, "org.opencastproject.[type]", "The service identifier"));
    registerEndpoint.addRequiredParam(new Param("host", Type.STRING, "http://", "The host's base URL for this service"));
    registerEndpoint.addRequiredParam(new Param("path", Type.STRING, "/myservice/rest", "the path on the server responsible for handling this type of service"));
    registerEndpoint.addRequiredParam(new Param("jobProducer", Type.BOOLEAN, "false", "whether this service produces jobs, which track long running operations"));
    registerEndpoint.addFormat(new Format("json", null, null));
    registerEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("Returns the service registration."));
    registerEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, registerEndpoint);

    return DocUtil.generate(data);
  }

}
