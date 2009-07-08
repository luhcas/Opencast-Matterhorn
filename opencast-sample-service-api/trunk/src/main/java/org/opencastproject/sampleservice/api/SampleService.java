/**
 *  Copyright 2009 Opencast Project (http://www.opencastproject.org)
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
package org.opencastproject.sampleservice.api;

import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;

import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * A sample service to use as a template when building your own Matterhorn services.
 */
@Path("/")
public interface SampleService {
  
  @GET
  @Path("/{path:.*}")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  InputStream getFileFromRepository(@PathParam("path") String path);

  @POST
  @Path("/{path:.*}")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  void setFileInRepository(@PathParam("path") String path, MultipartBody body);

  @GET
  @Path("/docs")
  @Produces(MediaType.TEXT_HTML)
  public String getDocumentation();

}
