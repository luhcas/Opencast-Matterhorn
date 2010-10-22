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
package org.opencastproject.analysis.vsegmenter.endpoint;

import org.opencastproject.analysis.vsegmenter.VideoSegmenter;
import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.rest.RestPublisher;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.Param.Type;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;

import org.apache.commons.io.IOUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * The REST endpoint for the {@link VideoSegmenter} service
 */
@Path("")
public class VideoSegmenterRestEndpoint {

  private static final Logger logger = LoggerFactory.getLogger(VideoSegmenterRestEndpoint.class);

  protected String docs;
  
  protected VideoSegmenter videoSegmenter;

  /**
   * Callback from OSGi that is called when this service is activated.
   * 
   * @param cc
   *          OSGi component context
   */
  public void activate(ComponentContext cc) {
    String serviceUrl = (String) cc.getProperties().get(RestPublisher.SERVICE_PATH_PROPERTY);
    docs = generateDocs(serviceUrl);
  }

  public void deactivate() {
  }

  public void setVideoSegmenter(VideoSegmenter videoSegmenter) {
    this.videoSegmenter = videoSegmenter;
  }
  
  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("/")
  public Response segment(@FormParam("track") String trackAsXml) {
    try {
      DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document doc = docBuilder.parse(IOUtils.toInputStream(trackAsXml, "UTF-8"));
      MediaPackageElement element = MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
              .elementFromManifest(doc.getDocumentElement(), new DefaultMediaPackageSerializerImpl());
      Job job = videoSegmenter.analyze(element, false);
      return Response.ok(new JaxbJob(job)).build();
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      return Response.serverError().build();
    }
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("/{id}.xml")
  public Response getJob(@PathParam("id") String id) {
    Job job;
    try {
      job = videoSegmenter.getJob(id);
    } catch (NotFoundException e) {
      return Response.status(Status.NOT_FOUND).build();
    } catch (ServiceRegistryException e) {
      throw new WebApplicationException(e);
    }
    if (job == null) {
      return Response.status(Status.NOT_FOUND).build();
    } else {
      return Response.ok(new JaxbJob(job)).build();
    }
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocs() {
    return docs;
  }

  protected String generateDocs(String serviceUrl) {
    DocRestData data = new DocRestData("videoSegmenter", "Video Segmentation Service", serviceUrl,
            new String[] { "$Rev$" });
    // analyze
    RestEndpoint analyzeEndpoint = new RestEndpoint("segment", RestEndpoint.Method.POST, "/",
            "Submit a track for segmentation");
    analyzeEndpoint.addStatus(org.opencastproject.util.doc.Status
            .OK("The job ID to use when polling for the resulting mpeg7 catalog"));
    analyzeEndpoint.addRequiredParam(new Param("track", Type.TEXT, "",
            "The track to segment."));
    analyzeEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, analyzeEndpoint);

    // job
    RestEndpoint receiptEndpoint = new RestEndpoint("job", RestEndpoint.Method.GET, "/{id}.xml",
            "Retrieve a job for a segmentation task");
    receiptEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("Results in an xml document containing the "
            + "status of the analysis job, and the catalog produced by this analysis job if it the task is finished"));
    receiptEndpoint.addPathParam(new Param("id", Param.Type.STRING, null, "the job id"));
    receiptEndpoint.addFormat(new Format("xml", null, null));
    receiptEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, receiptEndpoint);

    return DocUtil.generate(data);
  }
}
