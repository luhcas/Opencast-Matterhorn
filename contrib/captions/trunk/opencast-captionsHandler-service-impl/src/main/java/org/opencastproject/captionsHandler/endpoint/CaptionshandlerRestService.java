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
package org.opencastproject.captionsHandler.endpoint;

import org.opencastproject.captionsHandler.api.CaptionshandlerService;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * This is the REST endpoint for the captions handler service
 */
@Path("/")
public class CaptionshandlerRestService {
  private static final Logger logger = LoggerFactory.getLogger(CaptionshandlerRestService.class);

  private CaptionshandlerService service;
  public void setService(CaptionshandlerService service) {
    this.service = service;
  }
  public void unsetService(CaptionshandlerService service) {
    this.service = null;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("search")
  public String getSearchResults(@QueryParam("count") Integer perPage, @QueryParam("startPage") Integer page, @QueryParam("sort") String sort) {
    if (page == null || page < 1) {
      page = 1;
    }
    if (perPage == null || perPage < 1) {
      perPage = 10;
    }
    if (sort == null || "".equals(page)) {
      sort = "title asc";
    }
    // TODO make this do something real, right now it just loads a sample file
    // http://repo2.maven.org/maven2/org/json/json/20090211/json-20090211.jar
    String json;
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream("/sample/search.json");
      json = IOUtils.toString(in);
    } catch (Exception e) {
      logger.error("failed to load sample file: " + e, e);
      json = "ERROR: failed to load sample file: " + e;
    } finally {
      IOUtils.closeQuietly(in);
    }
    // replace values as a test of receiving the values
    json = json.replace("\"page\": 0", "\"page\": "+page.toString());
    json = json.replace("\"perPage\": 10", "\"perPage\": "+perPage.toString());
    //json = json.replace("\"count\": 3", "\"count\": "+"3");
    return json;
  }

  @POST
  @PUT
  @Path("/add/{id}/{type}")
  public Response addCaption(@PathParam("id") String mediaId, @PathParam("type") String captionType, @Context HttpServletRequest request, InputStream stream) {
    InputStream is = null;
    try {
      is = request.getInputStream();
    } catch (Exception e) {
      logger.error("failed to load request body file: " + e, e);
    } finally {
      IOUtils.closeQuietly(is);
    }
    // TODO make this do something real, it is just echoing out what it received in the header
    String url = "http://sample.url/"+mediaId+"/"+captionType;
    return Response.ok()
      .header("_dataFound", is != null)
      .header("_streamFound", stream != null)
      .header("_mediaId", mediaId)
      .header("_captionType", captionType)
      .header("_captionURL", url)
      .build();
  }
  
  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    return docs;
  }

  protected final String docs;
  
  public CaptionshandlerRestService() {
    String docsFromClassloader = null;
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream("/html/index.html");
      docsFromClassloader = IOUtils.toString(in);
    } catch (IOException e) {
      logger.error("failed to read documentation", e);
      docsFromClassloader = "unable to load documentation for " + CaptionshandlerRestService.class.getName();
    } finally {
      IOUtils.closeQuietly(in);
    }
    docs = docsFromClassloader;
  }
}
