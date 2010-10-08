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
package org.opencastproject.distribution.rest;

import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageImpl;
import org.opencastproject.rest.RestPublisher;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;
import org.opencastproject.util.doc.Param.Type;

import org.apache.commons.io.IOUtils;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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
  protected ComponentContext componentContext;

  /**
   * Gets the distribution service that is registered with the specified "distribution.channel" property
   * 
   * @param distributionChannel
   *          the id of this distribution channel
   * @return the distribution service for this channel
   * @throws IllegalArgumentException
   *           if there is not service registered for this channel
   */
  protected DistributionService getService(String distributionChannel) {
    ServiceReference[] refs = null;
    try {
      refs = componentContext.getBundleContext().getAllServiceReferences(DistributionService.class.getName(),
              "(distribution.channel=" + distributionChannel + ")");
    } catch (InvalidSyntaxException e) {
      throw new IllegalArgumentException("Unable to find a distribution service with id = " + distributionChannel);
    }
    switch (refs.length) {
    case 0:
      throw new IllegalArgumentException("Unable to find a distribution service with id = " + distributionChannel);
    case 1:
      return (DistributionService) componentContext.getBundleContext().getService(refs[0]);
    default:
      throw new IllegalStateException("more than one distribution service is registered with a channel id="
              + distributionChannel);
    }
  }

  /** The base URL for this rest endpoint */
  protected String alias;

  @POST
  @Path("/{distChannel}")
  @Produces(MediaType.TEXT_XML)
  public Response distribute(@PathParam("distChannel") String distChannel,
          @FormParam("mediapackage") MediaPackageImpl mediaPackage, @FormParam("elementId") List<String> elementIds)
          throws Exception {
    MediaPackage result = null;
    String[] elements = elementIds == null ? new String[0] : elementIds.toArray(new String[elementIds.size()]);

    if (elements == null || elements.length == 0) {
      return Response.status(Status.BAD_REQUEST).build();
    }

    try {
      DistributionService service = getService(distChannel);
      result = service.distribute(mediaPackage, elements);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
    return Response.ok(result).build();
  }

  @POST
  @Path("/retract/{distChannel}")
  @Produces(MediaType.TEXT_XML)
  public Response retract(@PathParam("distChannel") String distChannel,
          @FormParam("mediapackageId") String mediaPackageId) throws Exception {
    try {
      getService(distChannel).retract(mediaPackageId);
    } catch (Exception e) {
      logger.warn("Unable to retract mediapackage '{}' from channel '{}': {}", new Object[] { mediaPackageId,
              distChannel, e });
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
    return Response.noContent().build();
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
          "If the service is down or not working it will return a status 503, this means the the underlying service "
                  + "is not working and is either restarting or has failed. A status code 500 means a general failure has "
                  + "occurred which is not recoverable and was not anticipated. In other words, there is a bug!" };

  private String generateMediaPackage() {
    try {
      String template = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("manifest.xml"), "UTF-8");
      return template.replaceAll("@SAMPLES_URL@", serverUrl + "/workflow/samples");
    } catch (IOException e) {
      logger.warn("Unable to load the sample mediapackage");
      return null;
    }
  }

  private String generateDocs() {
    DocRestData data = new DocRestData("localdistributionservice", "Local Distribution Service", alias, notes);

    // abstract
    data.setAbstract("This service distributes media packages to the Matterhorn feed and engage services.");

    // distribute
    RestEndpoint endpoint = new RestEndpoint("distribute", RestEndpoint.Method.POST, "/{distChannel}",
            "Distribute a media package to a distribution channel");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addPathParam(new Param("distChannel", Type.STRING, "download",
            "The distribution channel identifier (e.g. download, streaming, youtube, etc.)"));
    endpoint.addRequiredParam(new Param("mediapackage", Param.Type.TEXT, generateMediaPackage(),
            "The media package as XML"));
    endpoint.addRequiredParam(new Param("elementId", Param.Type.STRING, "track-1", "A media package element ID"));
    endpoint.addNote("Accepts additional elementId fields");
    endpoint.addStatus(org.opencastproject.util.doc.Status.OK(null));
    endpoint.addStatus(org.opencastproject.util.doc.Status.ERROR(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // retract
    RestEndpoint retractEndpoint = new RestEndpoint("retract", RestEndpoint.Method.POST, "/retract/{distChannel}",
            "Retract a media package from a distribution channel");
    retractEndpoint.addPathParam(new Param("distChannel", Type.STRING, "download",
            "The distribution channel identifier (e.g. download, streaming, youtube, etc.)"));
    retractEndpoint.addRequiredParam(new Param("mediapackageId", Param.Type.STRING, null, "The media package ID"));
    retractEndpoint.addStatus(org.opencastproject.util.doc.Status.OK(null));
    retractEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR(null));
    retractEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, retractEndpoint);

    return DocUtil.generate(data);
  }

  public void activate(ComponentContext cc) {
    this.componentContext = cc;
    // Get the configured server URL
    if (cc == null) {
      serverUrl = UrlSupport.DEFAULT_BASE_URL;
    } else {
      String ccServerUrl = cc.getBundleContext().getProperty("org.opencastproject.server.url");
      logger.info("configured server url is {}", ccServerUrl);
      alias = (String) cc.getProperties().get(RestPublisher.SERVICE_PATH_PROPERTY);
      if (ccServerUrl == null) {
        serverUrl = UrlSupport.DEFAULT_BASE_URL;
      } else {
        serverUrl = ccServerUrl;
      }
    }
    docs = generateDocs();
  }
}
