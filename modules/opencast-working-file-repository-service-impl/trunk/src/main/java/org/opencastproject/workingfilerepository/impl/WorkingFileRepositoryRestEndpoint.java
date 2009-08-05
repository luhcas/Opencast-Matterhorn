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

import java.io.IOException;
import java.io.InputStream;

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
public class WorkingFileRepositoryRestEndpoint implements WorkingFileRepository {
  WorkingFileRepository repo;
  public void setRepository(WorkingFileRepository repo) {
    this.repo = repo;
  }

  @POST
  @Produces(MediaType.TEXT_HTML)
  @Path("{mediaPackageID}/{mediaPackageElementID}")
  public Response put(
      @PathParam("mediaPackageID") String mediaPackageID,
      @PathParam("mediaPackageElementID") String mediaPackageElementID,
      @Context HttpServletRequest request) throws Exception {
    if(ServletFileUpload.isMultipartContent(request)) {
      for(FileItemIterator iter = new ServletFileUpload().getItemIterator(request); iter.hasNext();) {
        FileItemStream item = iter.next();
        if(item.isFormField()) continue;
        put(mediaPackageID, mediaPackageElementID, item.openStream());
        return Response.ok("File stored for media package " + mediaPackageID + ", element " + mediaPackageElementID).build();
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
    delete(mediaPackageID, mediaPackageElementID);
    return Response.ok().build();
  }
  
  @GET
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("{mediaPackageID}/{mediaPackageElementID}")
  public InputStream get(
      @PathParam("mediaPackageID") String mediaPackageID,
      @PathParam("mediaPackageElementID") String mediaPackageElementID) {
    return repo.get(mediaPackageID, mediaPackageElementID);
  }
  
  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getTestForm() {
    try {
      return IOUtils.toString(getClass().getResourceAsStream("/html/index.html"));
    } catch (IOException e) {
      e.printStackTrace();
      return e.getMessage();
    }
  }

  public void put(String mediaPackageID, String mediaPackageElementID, InputStream in) {
    repo.put(mediaPackageID, mediaPackageElementID, in);
  }

  public void delete(String mediaPackageID, String mediaPackageElementID) {
    repo.delete(mediaPackageID, mediaPackageElementID);
  }

}
