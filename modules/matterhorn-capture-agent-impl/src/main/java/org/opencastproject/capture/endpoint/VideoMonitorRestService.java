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

import org.opencastproject.capture.api.VideoMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;


@Path("/")
public class VideoMonitorRestService {
  
  private static final Logger logger = LoggerFactory.getLogger(VideoMonitorRestService.class);
  
  private VideoMonitor service;
  
  public void activate() {
    logger.info("Video Monitoring Service Activated");
  }
  
  public void setService(VideoMonitor service) {
    this.service = service;
  }
  
  public void unsetService(VideoMonitor service) {
    this.service = null;
  }
  
  @POST
  @Produces("image/jpeg")
  @Path("getConfidence")
  public byte[] getConfidence(@FormParam("device") String device) {
    return service.getConfidenceSource(device);
  }
  
}
