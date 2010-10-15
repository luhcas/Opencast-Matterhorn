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
import org.opencastproject.serviceregistry.impl.ServiceStatisticsList;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;
import org.opencastproject.util.doc.Param.Type;

import org.osgi.service.component.ComponentContext;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Displays hosts and the service IDs they provide.
 */
@Path("")
public class ServiceRegistryEndpoint {

  /** The remote service maanger */
  protected ServiceRegistry serviceRegistry = null;

  /** The runtime documentation for this endpoint */
  protected String docs = null;
  
  /**  Sets the service registry instance for delegation */
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
    return DocUtil.generate(data);
  }
  
}
