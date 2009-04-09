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
package org.opencastproject.sampleservice;

import org.opencastproject.rest.OpencastRestService;
import org.opencastproject.status.api.StatusMessage;
import org.opencastproject.status.impl.StatusMessageImpl;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/sample")
public class SampleRestService implements OpencastRestService {
  @GET
  @Path("/html")
  @Produces(MediaType.TEXT_HTML)
  public String getHtml() {
    return "<h1>sample</h1>";
  }
  
  @GET
  @Path("/json")
  @Produces(MediaType.APPLICATION_JSON)
  public StatusMessage getJson() {
    return new StatusMessageImpl("a message", "a reference", SampleRestService.class.getName());
  }

  @GET
  @Path("/xml")
  @Produces(MediaType.APPLICATION_XML)
  public StatusMessage getStatusMessage() {
    return new StatusMessageImpl("a message", "a reference", SampleRestService.class.getName());
  }
}
