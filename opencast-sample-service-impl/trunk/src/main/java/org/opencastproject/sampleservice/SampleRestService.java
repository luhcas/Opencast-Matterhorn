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

import org.opencastproject.notification.api.NotificationMessage;
import org.opencastproject.notification.api.NotificationMessageImpl;
import org.opencastproject.repository.api.OpencastRepository;

import java.io.InputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/sample")
public class SampleRestService {
  
  private static final String DOCS;
  static {
    StringBuilder sb = new StringBuilder();
    sb.append("*This* is the first paragraph of the **documentation**.\n");
    sb.append("\tThis is an indented paragraph.\n");
    sb.append("See http://docutils.sourceforge.net/docs/user/rst/quickstart.html for help with " +
        "restructured text.");
    DOCS = sb.toString();
  }

  protected OpencastRepository repo;

  public SampleRestService(OpencastRepository repo) {
    this.repo = repo;
  }
  
  public SampleRestService() {
  }
  
  public void setRepository(OpencastRepository repo) {
    this.repo = repo;
  }

  /**
   * Gets the documentation for this restful service.
   * 
   * @return The documentation as restructured text
   */
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String getDocumentation() {
    return DOCS;
  }

  @GET
  @Path("/html")
  @Produces(MediaType.TEXT_HTML)
  public String getHtml() {
    return "<h1>sample</h1>";
  }
  
  @GET
  @Path("/json")
  @Produces(MediaType.APPLICATION_JSON)
  public NotificationMessage getJson() {
    return new NotificationMessageImpl("a message", "a reference", SampleRestService.class.getName());
  }

  @GET
  @Path("/xml")
  @Produces(MediaType.APPLICATION_XML)
  public NotificationMessage getStatusMessage() {
    return new NotificationMessageImpl("a message", "a reference", SampleRestService.class.getName());
  }
  
  @GET
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("/fetch/{path:.*}")
  public InputStream getFromRepository(@PathParam("path") String path) {
    return repo.getObject(InputStream.class, path);
  }
}
