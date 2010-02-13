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
package org.opencastproject.distribution.youtube.endpoint;

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

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * Rest endpoint for distributing media to the youtube distribution channel.
 */
@Path("/")
public class YoutubeDistributionRestService {
  private static final Logger logger = LoggerFactory.getLogger(YoutubeDistributionRestService.class);
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
    return  "<ns2:mediapackage xmlns:ns2=\"http://mediapackage.opencastproject.org\" id=\"10.0000/1\" start=\"2007-12-05T13:40:00\" duration=\"1004400000\">\n"+
            "  <media>\n"+
            "    <track id=\"track-1\" type=\"presentation/source\">\n"+
            "      <mimetype>video/quicktime</mimetype>\n"+
            "      <url>"+serverUrl+"/workflow/samples/camera.mpg</url>\n"+
            "      <checksum type=\"md5\">43b7d843b02c4a429b2f547a4f230d31</checksum>\n"+
            "      <duration>1004400000</duration>\n"+
            "      <video>\n"+
            "        <device type=\"UFG03\" version=\"30112007\" vendor=\"Unigraf\" />\n"+
            "        <encoder type=\"H.264\" version=\"7.4\" vendor=\"Apple Inc\" />\n"+
            "        <resolution>640x480</resolution>\n"+
            "        <scanType type=\"progressive\" />\n"+
            "        <bitrate>540520</bitrate>\n"+
            "        <frameRate>2</frameRate>\n"+
            "      </video>\n"+
            "    </track>\n"+
            "  </media>\n"+
            "  <metadata>\n"+
            "    <catalog id=\"catalog-1\" type=\"metadata/dublincore\">\n"+
            "      <mimetype>text/xml</mimetype>\n"+
            "      <url>"+serverUrl+"/workflow/samples/dc-1.xml</url>\n"+
            "      <checksum type=\"md5\">20e466615251074e127a1627fd0dae3e</checksum>\n"+
            "    </catalog>\n"+
            "  </metadata>\n"+
            "</ns2:mediapackage>";
  }

  private String generateDocs() {
    DocRestData data = new DocRestData("youtubedistributionservice", "Youtube Distribution Service", "/distribution/youtube", notes);

    // abstract
    data.setAbstract("This service distributes media packages to the Matterhorn feed and engage services.");
    
    // distribute
    RestEndpoint endpoint = new RestEndpoint("distribute", RestEndpoint.Method.POST,
        "/",
        "Distribute a media package");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addRequiredParam(new Param("mediaPackage", Param.Type.TEXT, generateMediaPackage(),
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
      String ccServerUrl = cc.getBundleContext().getProperty("serverUrl");
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
