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
package org.opencastproject.adminui.endpoint;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

import java.util.HashMap;
import java.util.Iterator;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.simple.JSONObject;
import org.opencastproject.adminui.api.AdminuiService;
import org.opencastproject.adminui.api.RecordingDataViewListImpl;

/**
 * REST endpoint for the Admin UI proxy service
 */
@Path("/")
public class AdminuiRestService {
  private static final Logger logger = LoggerFactory.getLogger(AdminuiRestService.class);
  private AdminuiService service;
  public void setService(AdminuiService service) {
    this.service = service;
  }

  public void unsetService(AdminuiService service) {
    this.service = null;
  }

  /**
   * Returns a list of recordings in a certain state.
   * @param state state according to which the recordings should filtered
   * @return recordings list of recordings in specified state
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("recordings/{state}")
  public RecordingDataViewListImpl getRecordings(@PathParam("state") String state) {
    return (RecordingDataViewListImpl)service.getRecordings(state);     // cast to annotated Impl so JAXB can produce XML
  }

  /**
   * Retruns simple statistics about "recordings" in the system
   * @return simple statistics about "recordings" in the system
   */
  @GET
  @Path("countRecordings")
  public Response countRecordings() {
    HashMap<String,Integer> stats = service.getRecordingsStatistic();
    Iterator<String> i = stats.keySet().iterator();
    JSONObject out = new JSONObject();
    while (i.hasNext()) {
      String key = i.next();
      out.put(key, stats.get(key));
    }
    return Response.ok(out.toJSONString()).header("Content-Type", MediaType.APPLICATION_JSON).build();
  }

  /**
   * Retruns documentation for this endpoint
   * @return documentation for this endpoint
   */
  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    return docs;
  }

  protected final String docs;
  
  public AdminuiRestService() {
    String docsFromClassloader = null;
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream("/html/index.html");
      docsFromClassloader = IOUtils.toString(in);
    } catch (IOException e) {
      logger.error("failed to read documentation", e);
      docsFromClassloader = "unable to load documentation for " + AdminuiRestService.class.getName();
    } finally {
      IOUtils.closeQuietly(in);
    }
    docs = docsFromClassloader;
  }
}
