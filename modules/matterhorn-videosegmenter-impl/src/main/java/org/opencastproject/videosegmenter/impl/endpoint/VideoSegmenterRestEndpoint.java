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
package org.opencastproject.videosegmenter.impl.endpoint;

import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.kernel.rest.AbstractJobProducerEndpoint;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.rest.RestConstants;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.Param.Type;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;
import org.opencastproject.videosegmenter.api.VideoSegmenterService;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.ComponentContext;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * The REST endpoint for the {@link VideoSegmenterService} service
 */
@Path("")
public class VideoSegmenterRestEndpoint extends AbstractJobProducerEndpoint {

  /** The rest docs */
  protected String docs;

  /** The video segmenter */
  protected VideoSegmenterService service;

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
   * Sets the segmenter
   * 
   * @param videoSegmenter
   *          the segmenter
   */
  protected void setVideoSegmenter(VideoSegmenterService videoSegmenter) {
    this.service = videoSegmenter;
  }

  /**
   * Segments a track.
   * 
   * @param trackAsXml the track xml to segment
   * @return the job in the body of a JAX-RS response
   * @throws Exception
   */
  @POST
  @Path("")
  @Produces(MediaType.TEXT_XML)
  public Response segment(@FormParam("track") String trackAsXml) throws Exception {
    // Ensure that the POST parameters are present
    if (StringUtils.isBlank(trackAsXml)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("track must not be null").build();
    }

    // Deserialize the track
    MediaPackageElement sourceTrack = MediaPackageElementParser.getFromXml(trackAsXml);
    if (!Track.TYPE.equals(sourceTrack.getElementType())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("mediapackage element must be of type track").build();
    }

    // Asynchronously segment the specified track
    Job job = service.segment((Track) sourceTrack);
    if (job == null)
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Segmentation failed").build();
    return Response.ok().entity(new JaxbJob(job)).build();
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
            .ok("The job ID to use when polling for the resulting mpeg7 catalog"));
    analyzeEndpoint.addRequiredParam(new Param("track", Type.TEXT, "", "The track to segment."));
    analyzeEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, analyzeEndpoint);

    return DocUtil.generate(data);
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
