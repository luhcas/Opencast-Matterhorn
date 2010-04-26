/**
 *  Copyright 2009, 2010 The Regents of the University of California
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
package org.opencastproject.distribution.local.endpoint;

import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageImpl;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;

import org.apache.commons.io.IOUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Rest endpoint for distributing media to the local distribution channel.
 */
@Path("/")
public class DistributionRestService {
  private static final Logger logger = LoggerFactory.getLogger(DistributionRestService.class);
  protected String serverUrl = UrlSupport.DEFAULT_BASE_URL;
  private DistributionService service;
  public void setService(DistributionService service) {
    this.service = service;
  }

  public void unsetService(DistributionService service) {
    this.service = null;
  }

  @POST
  @Path("")
  @Produces(MediaType.TEXT_XML)
  public Response distribute(@FormParam("mediapackage") MediaPackageImpl mediaPackage, @FormParam("elementId") List<String> elementIds) throws Exception {
    MediaPackage result = null;
    String[] elements = elementIds == null ? new String[0] : elementIds.toArray(new String[elementIds.size()]);
    try {
      result = service.distribute(mediaPackage, elements);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
    return Response.ok(result).build();
  }
  
  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    return docs;
  }

  protected String docs;
  private String[] notes = {
    "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
    "If the service is down or not working it will return a status 503, this means the the underlying service is not working and is either restarting or has failed",
    "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In other words, there is a bug! You should file an error report with your server logs from the time when the error occurred: <a href=\"https://issues.opencastproject.org\">Opencast Issue Tracker</a>", };
  
  private String generateMediaPackage() {
    try {
      String template = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("manifest.xml"));
      return template.replaceAll("@SAMPLES_URL@", serverUrl + "/workflow/samples");
    } catch(IOException e) {
      logger.warn("Unable to load the sample mediapackage");
      return null;
    }
  }

  private String generateDocs() {
    DocRestData data = new DocRestData("localdistributionservice", "Local Distribution Service", "/distribution/local/rest", notes);

    // abstract
    data.setAbstract("This service distributes media packages to the Matterhorn feed and engage services.");
    
    // distribute
    RestEndpoint endpoint = new RestEndpoint("distribute", RestEndpoint.Method.POST,
        "/",
        "Distribute a media package");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addRequiredParam(new Param("mediapackage", Param.Type.TEXT, generateMediaPackage(),
        "The media package as XML"));
    endpoint.addRequiredParam(new Param("elementId", Param.Type.STRING, "track-1",
        "A media package element ID"));
    endpoint.addNote("Accepts additional elementId fields");
    endpoint.addStatus(org.opencastproject.util.doc.Status.OK(null));
    endpoint.addStatus(org.opencastproject.util.doc.Status.ERROR(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    return DocUtil.generate(data);
  }

  public void activate(ComponentContext cc) {
    // Get the configured server URL
    if(cc == null) {
      serverUrl = UrlSupport.DEFAULT_BASE_URL;
    } else {
      String ccServerUrl = cc.getBundleContext().getProperty("org.opencastproject.server.url");
      logger.info("configured server url is {}", ccServerUrl);
      if(ccServerUrl == null) {
        serverUrl = UrlSupport.DEFAULT_BASE_URL;
      } else {
        serverUrl = ccServerUrl;
      }
    }
    docs = generateDocs();
  }
}
