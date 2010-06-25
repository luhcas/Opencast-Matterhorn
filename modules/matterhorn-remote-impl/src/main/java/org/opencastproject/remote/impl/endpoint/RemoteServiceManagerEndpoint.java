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
import org.opencastproject.remote.api.ServiceRegistration;
import org.opencastproject.remote.api.Receipt.Status;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

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

  /**  Sets the remote service manager instance for delegation */
  public void setRemoteServiceManager(RemoteServiceManager service) {
    this.service = service;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @SuppressWarnings("unchecked")
  public String getRemoteServiceRegistrations() {
    JSONArray jsonArray = new JSONArray();
    for(ServiceRegistration serviceRegistration : service.getServiceRegistrations()) {
      JSONObject reg = new JSONObject();
      reg.put("host", serviceRegistration.getHost());
      reg.put("type", serviceRegistration.getReceiptType());
      reg.put("maintenance", serviceRegistration.isInMaintenanceMode());
      reg.put("running", service.count(serviceRegistration.getReceiptType(), Status.RUNNING));
      reg.put("queued", service.count(serviceRegistration.getReceiptType(), Status.QUEUED));
      jsonArray.add(reg);
    }
    return jsonArray.toJSONString();
  }
}
