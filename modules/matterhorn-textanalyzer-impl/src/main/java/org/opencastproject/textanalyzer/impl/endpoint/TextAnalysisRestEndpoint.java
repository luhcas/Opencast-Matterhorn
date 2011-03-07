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
package org.opencastproject.textanalyzer.impl.endpoint;

import static org.opencastproject.util.doc.Status.badRequest;

import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.kernel.rest.AbstractJobProducerEndpoint;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.rest.RestConstants;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.textanalyzer.api.TextAnalyzerService;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.Param.Type;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * The REST endpoint for {@link MediaAnalysisService}s
 */
@Path("")
public class TextAnalysisRestEndpoint extends AbstractJobProducerEndpoint {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(TextAnalysisRestEndpoint.class);
  
  /** The rest docs */
  protected String docs;

  /** The text analyzer */
  protected TextAnalyzerService service;

  /** The service registry */
  protected ServiceRegistry serviceRegistry = null;

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

  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("")
  public Response analyze(@FormParam("image") String image) {
    if (service == null)
      throw new WebApplicationException(Status.SERVICE_UNAVAILABLE);
    try {
      MediaPackageElement element = MediaPackageElementParser.getFromXml(image);
      if (!(element instanceof Attachment))
        throw new WebApplicationException(Status.BAD_REQUEST);
      Job job = service.extract((Attachment)element);
      return Response.ok(new JaxbJob(job)).build();
    } catch (Exception e) {
      logger.info(e.getMessage(), e);
      return Response.serverError().build();
    }
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocs() {
    return docs;
  }

  protected String generateDocs(String serviceUrl) {
    DocRestData data = new DocRestData("textanalysis", "Text Analysis Service", serviceUrl,
            new String[] { "$Rev$" });
    // analyze
    RestEndpoint analyzeEndpoint = new RestEndpoint("analyze", RestEndpoint.Method.POST, "/",
            "Submit a track for analysis");
    analyzeEndpoint.addStatus(org.opencastproject.util.doc.Status
            .ok("The receipt to use when polling for the resulting mpeg7 catalog"));
    analyzeEndpoint.addRequiredParam(new Param("image", Type.TEXT, "", "The image to analyze for text."));
    analyzeEndpoint.addStatus(badRequest("If the argument cannot be parsed into a media package element"));
    analyzeEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, analyzeEndpoint);

    return DocUtil.generate(data);
  }

  /**
   * Callback from the OSGi declarative services to set the service registry.
   * 
   * @param serviceRegistry
   *          the service registry
   */
  protected void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * Sets the text analyzer
   * 
   * @param textAnalyzer
   *          the text analyzer
   */
  protected void setTextAnalyzer(TextAnalyzerService textAnalyzer) {
    this.service = textAnalyzer;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.kernel.rest.AbstractJobProducerEndpoint#getService()
   */
  @Override
  public JobProducer getService() {
    if (service instanceof JobProducer)
      return (JobProducer) service;
    else
      return null;
  }

}
