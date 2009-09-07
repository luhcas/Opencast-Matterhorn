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
package org.opencastproject.ingest.endpoint;

import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.ingest.impl.IngestServiceImpl;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.MediaPackageException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * FIXME -- Add javadocs
 */
@Path("/")
public class IngestRestService {
  private static final Logger logger = LoggerFactory.getLogger(IngestRestService.class);
  private IngestService service = null;

  public void setService(IngestService service) {
    this.service = service;
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("createMediaPackage")
  public Response createMediaPackage() {
    String id;
    try {
      id = service.createMediaPackage();
      return Response.ok("File temporarily stored for ingest service. Package " + id).build();
    } catch (MediaPackageException e) {
      return Response.serverError().status(400).build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_HTML)
  @Path("discardMediaPackage")
  public Response discardMediaPackage(String mediaPackageId) {
    service.discardMediaPackage(mediaPackageId);
    return Response.ok("Media package " + mediaPackageId + "discarded!.").build();
  }

  @POST
  @Produces(MediaType.TEXT_HTML)
  @Path("addTrack")
  public Response addMediaPackageTrack(@FormParam("url") String url, @FormParam("flavor") String flavor,
          @FormParam("mediaPackageId") String mediaPackageId) {
    URL u;
    try {
      u = new URL(url);
      String id = service.addTrack(u, MediaPackageElementFlavor.parseFlavor(flavor), mediaPackageId);
      return Response.ok("Track added to the MediaPackage: " + mediaPackageId + ", element " + id).build();
    } catch (Exception e) {
      return Response.serverError().status(400).build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_HTML)
  @Path("addCatalog")
  public Response addMediaPackageCatalog(@FormParam("url") String url, @FormParam("flavor") String flavor,
          @FormParam("mediaPackageId") String mediaPackageId) {
    URL u;
    try {
      u = new URL(url);
      String id = service.addCatalog(u, MediaPackageElementFlavor.parseFlavor(flavor), mediaPackageId);
      return Response.ok("Catalog added to the MediaPackage: " + mediaPackageId + ", element " + id).build();
    } catch (Exception e) {
      return Response.serverError().status(400).build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_HTML)
  @Path("addAttachment")
  public Response addMediaPackageAttachment(@FormParam("url") String url, @FormParam("flavor") String flavor,
          @FormParam("mediaPackageId") String mediaPackageId) {
    URL u;
    try {
      u = new URL(url);
      String id = service.addAttachment(u, MediaPackageElementFlavor.parseFlavor(flavor), mediaPackageId);
      return Response.ok("Attachment added to the MediaPackage: " + mediaPackageId + ", element " + id).build();
    } catch (Exception e) {
      return Response.serverError().status(400).build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_HTML)
  @Path("ingest")
  public Response ingestMediaPackage2(@FormParam("mediaPackageId") String mediaPackageId) {
    service.ingest(mediaPackageId);
    return Response.ok("Media package " + mediaPackageId + " being ingested!").build();
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    return docs;
  }

  protected final String docs;

  public IngestRestService() {
    service = new IngestServiceImpl();
    String docsFromClassloader = null;
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream("/html/index.html");
      docsFromClassloader = IOUtils.toString(in);
    } catch (IOException e) {
      logger.error("failed to read documentation", e);
      docsFromClassloader = "unable to load documentation for " + IngestRestService.class.getName();
    } finally {
      IOUtils.closeQuietly(in);
    }
    docs = docsFromClassloader;
  }

}
