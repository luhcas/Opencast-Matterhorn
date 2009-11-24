/**
 *  Copyright 2009 The Regents of the University of California
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

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.opencastproject.capture.admin.api.CaptureAgentStatusService;
import org.opencastproject.capture.api.StatusService;
import org.opencastproject.capture.impl.CaptureAgentImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The REST endpoint for the status service on the capture device
 */
@Path("/")
public class StatusRestService {

  private static final Logger logger = LoggerFactory.getLogger(StatusRestService.class);

  private StatusService service;

  public void setService(StatusService service) {
    this.service = service;
  }

  public void unsetService(CaptureAgentStatusService service) {
    this.service = null;
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("GetState")
  public String getState() {
    return this.service.getAgentState();
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    return docs;
  }

  protected final String docs;

  public StatusRestService() {
    service = new CaptureAgentImpl();
    String docsFromClassloader = null;
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream("/html/index.html");
      docsFromClassloader = IOUtils.toString(in);
    } catch (IOException e) {
      logger.error("failed to read documentation", e);
      docsFromClassloader = "unable to load documentation for " + StatusRestService.class.getName();
    } finally {
      IOUtils.closeQuietly(in);
    }
    docs = docsFromClassloader;
  }
}
