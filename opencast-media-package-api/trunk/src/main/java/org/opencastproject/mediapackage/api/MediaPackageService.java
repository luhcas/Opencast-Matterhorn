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
package org.opencastproject.mediapackage.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Gets a {@link MediaPackage} by its handle identifier
 */
@Path("/")
public interface MediaPackageService {
  @GET
  @Path("/{id}")
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  MediaPackage getMediaPackage(@PathParam("id") String handle);

  @GET
  @Path("/all")
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  MediaPackageList getMediaPackages();
  
  @GET
  @Path("/docs")
  @Produces({MediaType.TEXT_HTML})
  String getDocumentation();
}
