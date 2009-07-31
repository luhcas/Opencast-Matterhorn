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

import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

@Path("/")
public class WorkingFileRepositoryRestEndpoint implements WorkingFileRepository {
  WorkingFileRepository repo;
  public void setRepository(WorkingFileRepository repo) {
    this.repo = repo;
  }

  @DELETE
  @Path("/{mediaPackageID}/{mediaPackageElementID}")
  public void delete(
      @PathParam("mediaPackageID") String mediaPackageID,
      @PathParam("mediaPackageElementID") String mediaPackageElementID) {
    repo.delete(mediaPackageID, mediaPackageElementID);
  }

  @GET
  @Path("/{mediaPackageID}/{mediaPackageElementID}")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public InputStream get(
      @PathParam("mediaPackageID") String mediaPackageID,
      @PathParam("mediaPackageElementID") String mediaPackageElementID) {
    return repo.get(mediaPackageID, mediaPackageElementID);
  }

  @PUT
  @POST
  @Path("/{mediaPackageID}/{mediaPackageElementID}")
  public void put(
      @PathParam("mediaPackageID") String mediaPackageID,
      @PathParam("mediaPackageElementID") String mediaPackageElementID,
      @Context HttpServletRequest request) throws Exception {
    if(ServletFileUpload.isMultipartContent(request)) {
      FileItemStream item = new ServletFileUpload().getItemIterator(request).next();
      put(mediaPackageID, mediaPackageElementID, item.openStream());
    } else {
      throw new IllegalArgumentException("this method expects a file upload");
    }
  }

  public void put(String mediaPackageID, String mediaPackageElementID, InputStream in) {
    repo.put(mediaPackageID, mediaPackageElementID, in);
  }
  
  @GET
  @Path("/docs")
  @Produces(MediaType.TEXT_HTML)
  public String getDocumentation() {
    return "Please Document Me!";
  }

}
