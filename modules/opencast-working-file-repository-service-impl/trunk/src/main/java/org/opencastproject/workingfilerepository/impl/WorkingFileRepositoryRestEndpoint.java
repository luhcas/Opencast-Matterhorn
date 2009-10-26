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
package org.opencastproject.workingfilerepository.impl;

import org.opencastproject.workingfilerepository.api.WorkingFileRepository;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
public class WorkingFileRepositoryRestEndpoint {
  private static final Logger logger = LoggerFactory.getLogger(WorkingFileRepositoryRestEndpoint.class);
  WorkingFileRepository repo;
  public void setRepository(WorkingFileRepository repo) {
    this.repo = repo;
  }
  public void unsetRepository(WorkingFileRepository repo) {
    this.repo = null;
  }

  protected final String docs;
  
  public WorkingFileRepositoryRestEndpoint() {
    String docsFromClassloader = null;
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream("/html/index.html");
      docsFromClassloader = IOUtils.toString(in);
    } catch (IOException e) {
      logger.error("failed to read documentation", e);
      docsFromClassloader = "unable to load documentation for " + WorkingFileRepositoryRestEndpoint.class.getName();
    } finally {
      IOUtils.closeQuietly(in);
    }
    docs = docsFromClassloader;
  }
  
  @POST
  @Produces(MediaType.TEXT_HTML)
  @Path("{mediaPackageID}/{mediaPackageElementID}")
  public Response put(
      @PathParam("mediaPackageID") String mediaPackageID,
      @PathParam("mediaPackageElementID") String mediaPackageElementID,
      @Context HttpServletRequest request) throws Exception {
    checkService();
    if(ServletFileUpload.isMultipartContent(request)) {
      for(FileItemIterator iter = new ServletFileUpload().getItemIterator(request); iter.hasNext();) {
        FileItemStream item = iter.next();
        if(item.isFormField()) continue;
        URL url = repo.put(mediaPackageID, mediaPackageElementID, item.getName(), item.openStream());
        return Response.ok("File stored at " + url.toString()).build();
      }
    }
    return Response.serverError().status(400).build();
  }

  @DELETE
  @Produces(MediaType.TEXT_HTML)
  @Path("{mediaPackageID}/{mediaPackageElementID}")
  public Response deleteViaHttp(
      @PathParam("mediaPackageID") String mediaPackageID,
      @PathParam("mediaPackageElementID") String mediaPackageElementID) {
    checkService();
    repo.delete(mediaPackageID, mediaPackageElementID);
    return Response.ok().build();
  }
  
  @GET
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("{mediaPackageID}/{mediaPackageElementID}")
  public InputStream get(
      @PathParam("mediaPackageID") String mediaPackageID,
      @PathParam("mediaPackageElementID") String mediaPackageElementID) {
    checkService();
    return repo.get(mediaPackageID, mediaPackageElementID);
  }

  @GET
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("{mediaPackageID}/{mediaPackageElementID}/{fileName}")
  public InputStream get(
      @PathParam("mediaPackageID") String mediaPackageID,
      @PathParam("mediaPackageElementID") String mediaPackageElementID,
      @PathParam("fileName") String fileName) {
    checkService();
    return repo.get(mediaPackageID, mediaPackageElementID);
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    return docs;
  }
  
  protected void checkService() {
    if(repo == null) {
      // TODO What should we do in this case?
      throw new RuntimeException("Working File Repository is currently unavailable");
    }
  }
}
