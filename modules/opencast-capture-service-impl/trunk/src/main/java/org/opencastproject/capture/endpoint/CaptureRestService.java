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

import org.opencastproject.capture.api.CaptureAgent;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * FIXME -- Add javadocs
 */
@Path("/")
public class CaptureRestService {
  private static final Logger logger = LoggerFactory.getLogger(CaptureRestService.class);
  private CaptureAgent service;
  public void setService(CaptureAgent service) {
    this.service = service;
  }

  public void unsetService(CaptureAgent service) {
    this.service = null;
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("startCapture")
  public Response startCapture() {
    String out;
    try {
      out = service.startCapture();
      return Response.ok("Start Capute OK. OUT: " + out).build();
    } catch (Exception e) {
      return Response.serverError().status(400).build();
    }
  }
 
  @POST
  @Produces(MediaType.TEXT_PLAIN)
  @Path("startCapture")
  public Response startCapture(@FormParam("configuration") HashMap<String, String> configuration) {
    String out;
    try {
      out = service.startCapture(configuration);
      return Response.ok("Start Capute OK. OUT: " + out).build();
    } catch (Exception e) {
      return Response.serverError().status(400).build();
    }
  }


  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    return docs;
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("test")
  public String getTest() {
    return "Huevo de pascua!!!!";
  }


  protected final String docs;
  
  public CaptureRestService() {
    String docsFromClassloader = null;
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream("/html/index.html");
      docsFromClassloader = IOUtils.toString(in);
    } catch (IOException e) {
      logger.error("failed to read documentation", e);
      docsFromClassloader = "unable to load documentation for " + CaptureRestService.class.getName();
    } finally {
      IOUtils.closeQuietly(in);
    }
    docs = docsFromClassloader;
  }
}
