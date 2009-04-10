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

import org.opencastproject.api.OpencastJcrServer;
import org.opencastproject.rest.OpencastRestService;
import org.opencastproject.status.api.StatusMessage;
import org.opencastproject.status.impl.StatusMessageImpl;

import java.io.InputStream;

import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/sample")
public class SampleRestService implements OpencastRestService {
  protected Repository repo;

  public SampleRestService(OpencastJcrServer jcrServer) {
    this.repo = jcrServer.getRepository();
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
  public StatusMessage getJson() {
    return new StatusMessageImpl("a message", "a reference", SampleRestService.class.getName());
  }

  @GET
  @Path("/xml")
  @Produces(MediaType.APPLICATION_XML)
  public StatusMessage getStatusMessage() {
    return new StatusMessageImpl("a message", "a reference", SampleRestService.class.getName());
  }
  
  @GET
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("/fetch/{path:.*}")
  public InputStream getFromRepository(@PathParam("path") String path) {
    Session session = getSession();
    if (session == null) {
      throw new RuntimeException("Couldn't log in to the repository");
    } else {
      Node node = null;
      try {
        node = session.getRootNode().getNode(path);
      } catch (PathNotFoundException e) {
        e.printStackTrace();
      } catch (RepositoryException e) {
        e.printStackTrace();
      }
      if (node == null) {
        throw new RuntimeException("Couldn't find node " + path);
      } else {
        try {
          return node.getProperty("jcr:data").getStream();
        } catch (ItemNotFoundException e) {
          throw new RuntimeException(e);
        } catch (RepositoryException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  protected Session getSession() {
    Session session = null;
    try {
      session = repo.login(new SimpleCredentials("foo", "bar".toCharArray()));
    } catch (LoginException e) {
      e.printStackTrace();
    } catch (RepositoryException e) {
      e.printStackTrace();
    }
    return session;
  }
}
