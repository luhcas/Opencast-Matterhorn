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
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;
import org.opencastproject.util.doc.Param.Type;

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
  
  protected String docs;
  
  protected String generateDocs() {
    DocRestData data = new DocRestData("ConfidenceMonitor", "Confidence Monitor", "/confidence/rest", null);
    
    // grabFrame Endpoint
    RestEndpoint grabFrameEndpoint = new RestEndpoint("grabFrame", RestEndpoint.Method.GET, "/{name}", "Loads a JPEG image from the device specified");
    grabFrameEndpoint.addFormat(new Format("jpeg", "The image of the device", null));
    grabFrameEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, results returned"));
    grabFrameEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("Couldn't grab a frame from specified device"));
    Param device = new Param("name", Type.STRING, null, "The device to grab a frame from");
    grabFrameEndpoint.addPathParam(device);
    grabFrameEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, grabFrameEndpoint);
    
    // list devices endpoint
    RestEndpoint getDevices = new RestEndpoint("getDevices", RestEndpoint.Method.GET, "/devices", "Lists devices accessible on capture agent");
    getDevices.addFormat(new Format("XML", "Devices that support confidence monitoring", null));
    getDevices.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, results returned"));
    getDevices.addStatus(org.opencastproject.util.doc.Status.ERROR("Couldn't list devices"));
    getDevices.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getDevices);
    
    // audio rms endpoint
    RestEndpoint getRMSValues = new RestEndpoint("getRMSValues", RestEndpoint.Method.GET, "/audio/{name}/{timestamp}", "List X seconds of RMS values from audio device");
    getRMSValues.addFormat(new Format("String", "Current timestamp of capture agent followed by list of RMS values", null));
    getRMSValues.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, results returned"));
    getRMSValues.addStatus(org.opencastproject.util.doc.Status.ERROR("Couldn't grab RMS values"));
    Param audioDevice = new Param("name", Type.STRING, null, "The device to get RMS values from");
    Param timestamp = new Param("timestamp", Type.STRING, null, "The timestamp to start getting RMS values from");
    getRMSValues.addPathParam(audioDevice);
    getRMSValues.addPathParam(timestamp);
    getRMSValues.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getRMSValues);
    
    return DocUtil.generate(data);
  }
  
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
      String nameType[] = name.split(",");
      devices.add(new AgentDevice(nameType[0], nameType[1]));
    }
    return devices;
  }
  
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("audio/{name}/{timestamp}")
  public String getRMSValues(@PathParam("name") String device, @PathParam("timestamp") double timestamp) {
    List<Double> rmsValues = service.getRMSValues(device, timestamp);
    String output = Long.toString(System.currentTimeMillis()) + "\n";
    for (double value : rmsValues) {
      value = Math.round(value * 100.00) / 100.00;
      output += Double.toString(value) + "\n";
    }
    return output.substring(0, output.length() - 2);
  }
  
  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    if (docs == null) { docs = generateDocs(); }
    return docs;
  }
  
}
