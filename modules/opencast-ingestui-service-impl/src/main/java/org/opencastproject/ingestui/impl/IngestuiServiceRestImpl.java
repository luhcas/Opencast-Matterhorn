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
package org.opencastproject.ingestui.impl;

import org.opencastproject.ingestui.api.IngestuiService;
import org.opencastproject.ingestui.api.Metadata;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST endpoiont implementation for the Ingest UI service.
 * 
 * @author bwulff@uos.de
 */
@Path("/")
public class IngestuiServiceRestImpl implements IngestuiService {
  private static final Logger logger = LoggerFactory.getLogger(IngestuiServiceImpl.class);
  private IngestuiService service;

  public void setService(IngestuiService service) {
    this.service = service;
  }

  /**
   * Saves the metadata for a given filename. Data must be JSON encoded.
   * 
   * @param filename
   * @param data
   * @return
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.TEXT_HTML)
  @Path("metadata/{filename}")
  public Response acceptMetadataPOST(@PathParam("filename")
  String filename, Metadata data) {
    acceptMetadata(filename, data);
    return Response.ok("Metadata stored for " + filename).build();
  }

  /**
   * Returns the metadata for a given filename as XML
   * 
   * @param filename
   */
  @GET
  @Produces(MediaType.APPLICATION_XML)
  @Path("metadata/{filename}")
  public Metadata getMetadata(@PathParam("filename")
  String filename) {
    return service.getMetadata(filename);
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    return docs;
  }

  protected final String docs;

  public IngestuiServiceRestImpl() {
    String docsFromClassloader = null;
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream("/html/index.html");
      docsFromClassloader = IOUtils.toString(in);
    } catch (IOException e) {
      logger.error("failed to read documentation", e);
      docsFromClassloader = "unable to load documentation for " + IngestuiServiceRestImpl.class.getName();
    } finally {
      IOUtils.closeQuietly(in);
    }
    docs = docsFromClassloader;
  }

  public void acceptMetadata(String filename, Metadata data) {
    service.acceptMetadata(filename, data);
  }

}
