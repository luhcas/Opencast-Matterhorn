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
package org.opencastproject.remote.impl.endpoint;

import org.opencastproject.remote.api.RemoteServiceManager;
import org.opencastproject.remote.api.ServiceStatistics;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Displays hosts and the service IDs they provide.
 */
@Path("")
public class RemoteServiceManagerEndpoint {

  /** The remote service maanger */
  protected RemoteServiceManager service = null;

  /** The runtime documentation for this endpoint */
  protected String docs = null;
  
  /**  Sets the remote service manager instance for delegation */
  public void setRemoteServiceManager(RemoteServiceManager service) {
    this.service = service;
  }

  /**
   * Default, no-arg constructor generates the default rest documentation
   */
  public RemoteServiceManagerEndpoint() {
    DocRestData data = new DocRestData("remote", "Remote Services", "/remote/rest", null);
    data.setAbstract("This service lists the members of this cluster.");

    RestEndpoint endpoint = new RestEndpoint("list", RestEndpoint.Method.GET, "/services.json",
            "List the service registrations in the cluster, along with some simple statistics.");
    endpoint.addFormat(new Format("json", null, null));
    endpoint.addStatus(org.opencastproject.util.doc.Status.OK("Returns the remote services."));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, endpoint);

    docs = DocUtil.generate(data);
  }

  @GET
  @Path("/docs")
  @Produces(MediaType.TEXT_HTML)
  public String getDocs() {
    return docs;
  }

  @GET
  @Path("/services.json")
  @Produces(MediaType.APPLICATION_JSON)
  @SuppressWarnings("unchecked")
  public String getRemoteServiceRegistrations() {
    JSONArray jsonArray = new JSONArray();
    List<ServiceStatistics> statistics = service.getServiceStatistics();
    for(ServiceStatistics stats : statistics) {
      JSONObject reg = new JSONObject();
      reg.put("host", stats.getServiceRegistration().getHost());
      reg.put("type", stats.getServiceRegistration().getServiceType());
      reg.put("online", stats.getServiceRegistration().isOnline());
      reg.put("maintenance", stats.getServiceRegistration().isInMaintenanceMode());
      reg.put("running", stats.getRunningJobs());
      reg.put("queued", stats.getQueuedJobs());
      reg.put("meanQueueTime", stats.getMeanQueueTime());
      reg.put("meanRunTime", stats.getMeanRunTime());
      jsonArray.add(reg);
    }
    return jsonArray.toJSONString();
  }
  

  
}
