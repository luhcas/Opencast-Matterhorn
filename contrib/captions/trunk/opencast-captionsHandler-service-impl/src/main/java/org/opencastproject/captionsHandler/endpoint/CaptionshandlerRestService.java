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
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.Response.Status;

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

  /**
   * Checks if the service or services are available,
   * if not it handles it by returning a 503 with a message
   * @param services an array of services to check
   */
  protected void checkNotNull(Object... services) {
    if (services != null) {
      for (Object object : services) {
        if (object == null) {
          throw new javax.ws.rs.WebApplicationException(Status.SERVICE_UNAVAILABLE);
        }
      }
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("search")
  public String getSearchResults(@QueryParam("count") Integer perPage, @QueryParam("startPage") Integer page, 
          @QueryParam("sort") String sort, @QueryParam("testing") boolean testing) {
    checkNotNull(service);
    if (page == null || page < 1) {
      page = 1;
    }
    if (perPage == null || perPage < 1) {
      perPage = 10;
    }
    if (sort == null || "".equals(page)) {
      sort = "title asc";
    }
    int total = 0;
    String json;
    ArrayList<LinkedHashMap<String, Object>> results = new ArrayList<LinkedHashMap<String,Object>>();
    if (testing) {
      // testing generates the sample feed which uses the sample video item
      LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
      item.put("id", "ABC123456");
      item.put("timestamp", new Date().getTime());
      item.put("title", "Sample item title");
      item.put("mediaURL", "http://localhost:8080/rest/samplevideo");
      item.put("captionable", true);
      item.put("DFXPURL", null);
      item.put("describable", false);
      item.put("DAURL", null);
      results.add(item);
      total = 1;
    } else {
      // real stuff
      // http://repo2.maven.org/maven2/org/json/json/20090211/json-20090211.jar OR http://repo1.maven.org/maven2/com/googlecode/json-simple/json-simple/1.1/json-simple-1.1.jar
    }
    LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
    data.put("page", page);
    data.put("perPage", perPage);
    data.put("count", results.size());
    data.put("total", total);
    data.put("results", results);
    json = JSONValue.toJSONString(data);
    return json;
  }

  @POST
  @PUT
  @Path("/add/{id}/{type}")
  public Response addCaption(@PathParam("id") String mediaId, @PathParam("type") String captionType, @Context HttpServletRequest request, InputStream stream) {
    checkNotNull(service);
    InputStream is = null;
    try {
      is = request.getInputStream();
    } catch (Exception e) {
      logger.error("failed to load request body file: " + e, e);
      return Response.serverError()
        .header("_dataFound", is != null)
        .header("_streamFound", stream != null)
        .header("_mediaId", mediaId)
        .header("_captionType", captionType)
        .build();
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
  @Path("/samplevideo")
  public StreamingOutput getSampleVideo() {
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream("/sample/sample.mov");
      if (in == null) {
        throw new NullPointerException("No sample video could be found");
      }
    } catch (Exception e) {
      logger.error("failed to load sample file: " + e, e);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    } finally {
      IOUtils.closeQuietly(in);
    }
    final InputStream inStream = in;
    return new StreamingOutput() {
      public void write(OutputStream out) throws IOException, WebApplicationException {
        IOUtils.copy(inStream, out);
      }
    };
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("/")
  public String getDefault() {
    return getDocumentation();
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
