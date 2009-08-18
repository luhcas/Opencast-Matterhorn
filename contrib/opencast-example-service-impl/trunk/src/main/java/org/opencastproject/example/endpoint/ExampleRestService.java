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
package org.opencastproject.example.endpoint;

import org.opencastproject.example.api.ExampleEntity;
import org.opencastproject.example.api.ExampleService;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * FIXME -- Add javadocs
 */
@Path("/")
public class ExampleRestService {
  private static final Logger logger = LoggerFactory.getLogger(ExampleRestService.class);
  private ExampleService service;
  public void setService(ExampleService service) {
    this.service = service;
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  public ExampleEntityJaxbImpl getAnEntity(@QueryParam("id") String id) {
    ExampleEntity entity = service.getExampleEntity(id);
    return new ExampleEntityJaxbImpl(entity);
  }

  @POST
  @Consumes(MediaType.TEXT_XML)
  public Response storeAnEntity(@FormParam("entity") ExampleEntityJaxbImpl jaxbEntity) {
    service.saveExampleEntity(jaxbEntity.getEntity());
    return Response.ok(jaxbEntity).build();
  }
  
  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    return docs;
  }

  protected final String docs;
  
  public ExampleRestService() {
    String docsFromClassloader = null;
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream("/html/index.html");
      docsFromClassloader = IOUtils.toString(in);
    } catch (IOException e) {
      logger.error("failed to read documentation", e);
      docsFromClassloader = "unable to load documentation for " + ExampleRestService.class.getName();
    } finally {
      IOUtils.closeQuietly(in);
    }
    docs = docsFromClassloader;
  }
}
