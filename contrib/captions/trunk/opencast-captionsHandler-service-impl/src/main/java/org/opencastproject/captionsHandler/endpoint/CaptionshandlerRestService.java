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

import org.opencastproject.captionsHandler.api.CaptionsMediaItem;
import org.opencastproject.captionsHandler.api.CaptionshandlerService;
import org.opencastproject.captionsHandler.api.CaptionshandlerService.CaptionsResults;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
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
      item.put("mediaURL", "http://localhost:8080/rest/samplevideo.mov");
      item.put("captionable", true);
      item.put("DFXPURL", null);
      item.put("describable", false);
      item.put("DAURL", null);
      results.add(item);
      total = 1;
    } else {
      // real stuff
      // http://repo2.maven.org/maven2/org/json/json/20090211/json-20090211.jar OR http://repo1.maven.org/maven2/com/googlecode/json-simple/json-simple/1.1/json-simple-1.1.jar
      int start = (page - 1) * perPage;
      CaptionsResults cr = service.getCaptionableMedia(start, perPage, sort);
      for (CaptionsMediaItem cmi : cr.results) {
        LinkedHashMap<String, Object> item = cmiToMap(cmi);
        results.add(item);
      }
      perPage = cr.max;
      total = cr.total;
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

  @GET
  @Path("/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public String getCaptionsItem(@PathParam("id") String workflowId) {
    checkNotNull(service);
    CaptionsMediaItem cmi = service.getCaptionsMediaItem(workflowId);
    if (cmi == null) {
      throw new WebApplicationException(Response.Status.NOT_FOUND);
    }
    LinkedHashMap<String, Object> data = cmiToMap(cmi);
    String json = JSONValue.toJSONString(data);
    return json;
  }

  private LinkedHashMap<String, Object> cmiToMap(CaptionsMediaItem cmi) {
    LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
    item.put("id", cmi.getWorkflowId());
    item.put("timestamp", new Date().getTime()); // TODO MP created date?
    item.put("title", cmi.getTitle());
    item.put("mediaURL", cmi.getMediaURL() != null ? cmi.getMediaURL().toExternalForm() : null);
    item.put("captionable", true);
    URL ttURL = cmi.getCaptionsURL(CaptionshandlerService.CAPTIONS_TYPE_TIMETEXT);
    URL daURL = cmi.getCaptionsURL(CaptionshandlerService.CAPTIONS_TYPE_DESCAUDIO);
    item.put("DFXPURL", ttURL != null ? ttURL.toString() : null);
    item.put("describable", false);
    item.put("DAURL", daURL != null ? daURL.toString() : null);
    return item;
  }

  @POST
  @PUT
  @Path("/{id}/{type}")
  public Response addCaption(@PathParam("id") String workflowId, @PathParam("type") String captionType, @Context HttpServletRequest request, InputStream stream) {
    checkNotNull(service);
    InputStream is = null;
    try {
      is = request.getInputStream();
    } catch (Exception e) {
      logger.error("failed to load request body file: " + e, e);
      return Response.serverError()
        .header("_dataFound", is != null)
        .header("_streamFound", stream != null)
        .header("_mediaId", workflowId)
        .header("_captionType", captionType)
        .build();
    } finally {
      IOUtils.closeQuietly(is);
    }
    // TODO make this do something real, it is just echoing out what it received in the header
    String url = "http://sample.url/"+workflowId+"/"+captionType;
    return Response.status(Response.Status.NO_CONTENT)
      .header("_dataFound", is != null)
      .header("_streamFound", stream != null)
      .header("_mediaId", workflowId)
      .header("_captionType", captionType)
      .header("_captionURL", url)
      .build();
  }

  @GET
  @Path("/samplevideo.mov")
  @Produces("video/quicktime")
  public StreamingOutput getSampleVideo() {
    return new StreamingOutput() {
      public void write(OutputStream out) throws IOException, WebApplicationException {
        InputStream in = null;
        try {
          in = getClass().getResourceAsStream("/sample/sample.mov");
          if (in == null) {
            throw new NullPointerException("No sample video could be found");
          }
          IOUtils.copy(in, out);
        } catch (Exception e) {
          logger.error("failed to load sample file: " + e, e);
          throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
          IOUtils.closeQuietly(in);
        }
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
