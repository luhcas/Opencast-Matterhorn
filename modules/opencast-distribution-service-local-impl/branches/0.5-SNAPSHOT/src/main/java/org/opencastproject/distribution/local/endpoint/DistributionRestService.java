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
package org.opencastproject.distribution.local.endpoint;

import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.jaxb.MediapackageType;
import org.opencastproject.util.UrlSupport;

import org.apache.commons.io.IOUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
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
  protected String docs = null;
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
  public Response distribute(@FormParam("mediapackage") MediapackageType mediaPackage, @FormParam("elementId") List<String> elementIds) throws Exception {
    MediaPackage result = null;
    String[] elements = elementIds == null ? new String[0] : elementIds.toArray(new String[elementIds.size()]);
    try {
      result = service.distribute(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().loadFromManifest(
              IOUtils.toInputStream(mediaPackage.toXml())), elements);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
    MediapackageType jaxbResult = MediapackageType.fromXml(result.toXml());
    return Response.ok(jaxbResult).build();
  }
  
  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    return docs;
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

    // Pre-load the documentation
    String docsFromClassloader = null;
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream("/html/index.html");
      docsFromClassloader = IOUtils.toString(in);
      docsFromClassloader = docsFromClassloader.replaceAll("@SERVER_URL@", serverUrl + "/workflow/rest");
      docsFromClassloader = docsFromClassloader.replaceAll("@SAMPLES_URL@", serverUrl + "/workflow/samples");
    } catch (IOException e) {
      logger.error("failed to read documentation", e);
      docsFromClassloader = "unable to load documentation for " + DistributionRestService.class.getName();
    } finally {
      IOUtils.closeQuietly(in);
    }
    docs = docsFromClassloader;
  }
}
