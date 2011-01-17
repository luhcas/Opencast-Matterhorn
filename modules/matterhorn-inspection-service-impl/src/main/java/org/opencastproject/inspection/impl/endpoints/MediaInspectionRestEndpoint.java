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
package org.opencastproject.inspection.impl.endpoints;

import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.job.api.JobProducerRestEndpointSupport;
import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.rest.RestConstants;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;
import org.opencastproject.util.doc.Status;

import org.apache.commons.io.IOUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.net.URI;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * A service endpoint to expose the {@link MediaInspectionService} via REST.
 */
@Path("/")
public class MediaInspectionRestEndpoint extends JobProducerRestEndpointSupport {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(MediaInspectionRestEndpoint.class);

  /** The inspection service */
  protected MediaInspectionService service;

  /** The service registry */
  protected ServiceRegistry serviceRegistry = null;

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JobProducerRestEndpointSupport#setServiceRegistry(org.opencastproject.serviceregistry.api.ServiceRegistry)
   */
  @Override
  protected void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JobProducerRestEndpointSupport#getServiceRegistry()
   */
  @Override
  protected ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  /**
   * Sets the inspection service
   * 
   * @param service
   *          the inspection service
   */
  public void setService(MediaInspectionService service) {
    this.service = service;
  }

  /**
   * Removes the inspection service
   * 
   * @param service
   */
  public void unsetService(MediaInspectionService service) {
    this.service = null;
  }

  /**
   * Callback from OSGi that is called when this service is activated.
   * 
   * @param cc
   *          OSGi component context
   */
  public void activate(ComponentContext cc) {
    String serviceUrl = (String) cc.getProperties().get(RestConstants.SERVICE_PATH_PROPERTY);
    docs = generateDocs(serviceUrl);
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("inspect")
  public Response inspectTrack(@QueryParam("uri") URI uri) {
    checkNotNull(service);
    try {
      Job job = service.inspect(uri);
      return Response.ok(new JaxbJob(job)).build();
    } catch (Exception e) {
      logger.info(e.getMessage());
      return Response.serverError().build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("enrich")
  public Response enrichTrack(@FormParam("mediaPackageElement") String mediaPackageElement,
          @FormParam("override") boolean override) {
    checkNotNull(service);
    try {
      DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document doc = docBuilder.parse(IOUtils.toInputStream(mediaPackageElement, "UTF-8"));
      MediaPackageElement mpe = MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
              .elementFromManifest(doc.getDocumentElement(), new DefaultMediaPackageSerializerImpl());
      Job job = service.enrich(mpe, override);
      return Response.ok(new JaxbJob(job)).build();
    } catch (Exception e) {
      logger.info(e.getMessage(), e);
      return Response.serverError().build();
    }
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    return docs;
  }

  protected String docs;
  private String[] notes = {
          "All paths above are relative to the REST endpoint base (something like http://your.server/inspection/rest)",
          "If the service is down or not working it will return a status 503, this means the underlying service is not working and is either restarting or has failed",
          "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In other words, there is a bug! You should file an error report with your server logs from the time when the error occurred: <a href=\"https://issues.opencastproject.org\">Opencast Issue Tracker</a>",
          "Here is a sample video for testing: <a href=\"./?url=http://source.opencastproject.org/svn/modules/opencast-media/trunk/src/test/resources/aonly.mov\">analyze sample video</a>" };

  private String generateDocs(String serviceUrl) {
    DocRestData data = new DocRestData("inspection", "Media inspection", serviceUrl, notes);
    // abstract
    data.setAbstract("This service extracts technical metadata from media files.");
    // inspect
    RestEndpoint inspectEndpoint = new RestEndpoint("inspect", RestEndpoint.Method.GET, "/inspect",
            "Analyze a given media file, returning a receipt to check on the status and outcome of the job");
    inspectEndpoint.addOptionalParam(new Param("uri", Param.Type.STRING, null, "Location of the media file"));
    inspectEndpoint.addFormat(Format.xml());
    inspectEndpoint.addStatus(Status.ok("XML encoded receipt is returned"));
    inspectEndpoint.addStatus(new Status(500, "Problem retrieving media file or invalid media file or URL"));
    inspectEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, inspectEndpoint);

    // enrich
    RestEndpoint enrichEndpoint = new RestEndpoint(
            "enrich",
            RestEndpoint.Method.POST,
            "/enrich",
            "Analyze and add missing metadata of a given media file, returning a receipt to check on the status and outcome of the job");
    enrichEndpoint.addRequiredParam(new Param("mediaPackageElement", Param.Type.TEXT, null,
            "MediaPackage Element, that should be enriched with metadata"));
    enrichEndpoint.addRequiredParam(new Param("override", Param.Type.BOOLEAN, null,
            "Should the existing metadata values remain"));
    enrichEndpoint.addFormat(Format.xml());
    enrichEndpoint.addStatus(Status.ok("XML encoded receipt is returned"));
    enrichEndpoint.addStatus(new Status(500, "Problem retrieving media file or invalid media file or URL"));
    enrichEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, enrichEndpoint);

    // getReceipt
    RestEndpoint receiptEndpoint = new RestEndpoint("getReceipt", RestEndpoint.Method.GET, "/receipt/{id}.xml",
            "Check on the status of an inspection receipt");
    receiptEndpoint.addPathParam(new Param("id", Param.Type.STRING, null, "ID of the receipt"));
    receiptEndpoint.addFormat(Format.xml());
    receiptEndpoint.addStatus(Status.ok("XML encoded receipt is returned"));
    receiptEndpoint.addStatus(new Status(javax.ws.rs.core.Response.Status.NOT_FOUND.getStatusCode(),
            "No receipt with this identifier"));
    receiptEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, receiptEndpoint);

    return DocUtil.generate(data);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JobProducerRestEndpointSupport#getService()
   */
  @Override
  public JobProducer getService() {
    if (service instanceof JobProducer)
      return (JobProducer) service;
    else
      return null;
  }

  /**
   * Checks if the service or services are available, if not it handles it by returning a 503 with a message
   * 
   * @param services
   *          an array of services to check
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

}
