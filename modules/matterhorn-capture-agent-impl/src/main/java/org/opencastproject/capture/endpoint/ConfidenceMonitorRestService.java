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
package org.opencastproject.capture.endpoint;

import org.opencastproject.capture.api.ConfidenceMonitor;
import org.opencastproject.capture.api.AgentDevice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;


@Path("/")
public class ConfidenceMonitorRestService {
  
  private static final Logger logger = LoggerFactory.getLogger(ConfidenceMonitorRestService.class);
  
  private ConfidenceMonitor service;
  
  public void activate() {
    logger.info("Video Monitoring Service Activated");
  }
  
  public void setService(ConfidenceMonitor service) {
    this.service = service;
  }
  
  public void unsetService(ConfidenceMonitor service) {
    this.service = null;
  }
  
  @GET
  @Produces("image/jpeg")
  @Path("{name}")
  public byte[] grabFrame(@PathParam("name") String device) {
    return service.grabFrame(device);
  }
  
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("devices")
  public List<AgentDevice> getDevices() {
    LinkedList<AgentDevice> devices = new LinkedList<AgentDevice>();
    List<String> names = service.getFriendlyNames();
    for (String name : names) {
      devices.add(new AgentDevice(name));
    }
    return devices;
  }
  
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("audio/{name}")
  public String getRMSValues(@PathParam("name") String device) {
    List<Double> rmsValues = service.getRMSValues(device);
    String output = "";
    for (double value : rmsValues) {
      output += Double.toString(value) + ",";
    }
    return output.substring(0, output.length() - 2);
    
  }
  
}
