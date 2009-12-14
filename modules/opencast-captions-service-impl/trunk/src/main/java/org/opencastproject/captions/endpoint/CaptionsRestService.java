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
package org.opencastproject.captions.endpoint;

import org.opencastproject.captions.api.CaptionsMediaItem;
import org.opencastproject.captions.api.CaptionsService;
import org.opencastproject.captions.api.CaptionsService.CaptionsResults;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;
import org.opencastproject.util.doc.Status;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONValue;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
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

/**
 * This is the REST endpoint for the captions handler service
 */
@Path("/")
public class CaptionsRestService {
  private static final Logger logger = LoggerFactory.getLogger(CaptionsRestService.class);

  private CaptionsService service;
  public void setService(CaptionsService service) {
    this.service = service;
  }
  public void unsetService(CaptionsService service) {
    this.service = null;
  }

  protected void activate(ComponentContext context) {
    logger.info("ACTIVATE: service={}", service);
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
          throw new javax.ws.rs.WebApplicationException(javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE);
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
    if (sort == null || "".equals(sort)) {
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
    item.put("mediaURL", cmi.getMediaURI() != null ? cmi.getMediaURI().toString() : null);
    item.put("captionable", true);
    URI ttURL = cmi.getCaptionsURI(CaptionsService.CAPTIONS_TYPE_TIMETEXT);
    URI daURL = cmi.getCaptionsURI(CaptionsService.CAPTIONS_TYPE_DESCAUDIO);
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
      if (is == null) {
        logger.error("failed to load request body file (body is null)");
        throw new IllegalArgumentException("request body cannot be null (no file was sent)");
      }
      CaptionsMediaItem cmi = service.updateCaptions(workflowId, captionType, is);
      return Response.status(Response.Status.NO_CONTENT)
        .header("_mpId", cmi.getMediaPackageId())
        .header("_mediaURL", cmi.getMediaURI())
        .header("_mediaId", workflowId)
        .header("_captionType", captionType)
        .header("_captionURL", cmi.getCaptionsURI(captionType))
        .build();
    } catch (Exception e) {
      logger.error("Failure while adding caption ("+captionType+") to workflow ("+workflowId+"): " + e, e);
      return Response.serverError()
        .header("_dataFound", is != null)
        .header("_streamFound", stream != null)
        .header("_mediaId", workflowId)
        .header("_captionType", captionType)
        .header("_error", e.toString())
        .build();
    } finally {
      IOUtils.closeQuietly(is);
    }
  }

  @GET
  @Path("/samplevideo.mov")
  @Produces("video/quicktime")
  public StreamingOutput getSampleVideo() {
    return makeStream("/sample/sample.mov");
  }

  @GET
  @Path("/samplesearch.json")
  @Produces("application/json")
  public StreamingOutput getSampleJSON() {
    return makeStream("/sample/search.json");
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
  private static String[] notes = {
    "All paths above are relative to the REST endpoint base (something like http://your.server/captions/rest)",
    "If the service is down or not working it will return a status 503, this means the the underlying service is not working and is either restarting or has failed",
    "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In other words, there is a bug! You should file an error report with your server logs from the time when the error occurred: <a href=\"https://issues.opencastproject.org\">Opencast Issue Tracker</a>",
    "Here is a sample video for testing: <a href=\"/captions/rest/samplevideo.mov\">sample video</a>"
  };
  
  public CaptionsRestService() {
    /* old method
    String docsFromClassloader = null;
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream("/html/index.html");
      docsFromClassloader = IOUtils.toString(in);
    } catch (IOException e) {
      logger.error("failed to read documentation", e);
      docsFromClassloader = "unable to load documentation for " + CaptionsRestService.class.getName();
    } finally {
      IOUtils.closeQuietly(in);
    }
    docs = docsFromClassloader;
    */
    // generate using the rest doc util
    DocRestData data = new DocRestData("captions", "Captions Handler", "/captions/rest", notes);
    // search
    RestEndpoint endpoint = new RestEndpoint("search", RestEndpoint.Method.GET, "/search", 
            "Searches for and retrieves a list of media which needs to be captioned");
    endpoint.addOptionalParam(new Param("count", Param.Type.STRING,
            "0", "number of results to return per page (0 indicates all or maximum allowed)", null));
    endpoint.addOptionalParam(new Param("startPage", Param.Type.STRING,
            "0", "the page of results to display (0 is first page)", null));
    endpoint.addOptionalParam(new Param("sort", Param.Type.ENUM,
            "title asc", "the sort order to return items in, valid fields are date, title", 
            new String[] {"title asc","title desc","date asc","date desc"}));
    endpoint.addFormat( Format.json("<a href=\"/captions/rest/samplesearch.json\">sample</a>") );
    endpoint.addStatus( Status.OK("results returned") );
    endpoint.addStatus( Status.BAD_REQUEST("invalid arguments in request, error message") );
    endpoint.addNote("The search is only a search for captionable items and not for all items in general");
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, endpoint);
    // get
    endpoint = new RestEndpoint("get_caption", RestEndpoint.Method.GET, "/{id}",
            "Retrieve a single captionable item");
    endpoint.addPathParam(new Param("id", Param.Type.STRING,
            null, "the media ID from the search result", null));
    endpoint.addFormat(Format.json());
    endpoint.addStatus(Status.OK("results returned"));
    endpoint.addStatus(Status.NOT_FOUND("item does not exist, error message"));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, endpoint);
    // add
    endpoint = new RestEndpoint("add", RestEndpoint.Method.PUT, "/{id}/{type}",
            "Updates a media item with captions data");
    endpoint.addPathParam(new Param("id", Param.Type.STRING,
            null, "the media ID from the search result", null));
    endpoint.addPathParam(new Param("type", Param.Type.STRING,
            null, "the media type from the search result", null));
    endpoint.addBodyParam(false, null, "should be captions data (a timetext file)");
    endpoint.addStatus( new Status(204, "data was added, result info in headers") );
    endpoint.addStatus(Status.BAD_REQUEST("data was not added, error message"));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);
    docs = DocUtil.generate(data);
  }

  /**
   * Generate a JAXRS stream for a given path
   * 
   * @param path the path to the resource
   * @return the stream object
   * @throws RuntimeException if there is a failure
   */
  private static StreamingOutput makeStream(final String path) {
    return new StreamingOutput() {
      public void write(OutputStream out) throws IOException, WebApplicationException {
        InputStream in = null;
        try {
          in = getClass().getResourceAsStream(path);
          if (in == null) {
            throw new NullPointerException("No file could be found at path: " + path);
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

}
