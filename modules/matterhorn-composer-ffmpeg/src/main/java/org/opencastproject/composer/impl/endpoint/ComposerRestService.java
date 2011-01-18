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
package org.opencastproject.composer.impl.endpoint;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EmbedderException;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.api.EncodingProfileImpl;
import org.opencastproject.composer.api.EncodingProfileList;
import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.kernel.rest.AbstractJobProducerEndpoint;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageSerializer;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.rest.RestConstants;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.UrlSupport;
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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * A REST endpoint delegating functionality to the {@link ComposerService}
 */
@Path("/")
public class ComposerRestService extends AbstractJobProducerEndpoint {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(ComposerRestService.class);
  
  /** The rest documentation */
  protected String docs;
  
  /** The base server URL */
  protected String serverUrl;
  
  /** The composer service */
  protected ComposerService composerService = null;
  
  /** The service registry */
  protected ServiceRegistry serviceRegistry = null;

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
   * {@inheritDoc}
   *
   * @see org.opencastproject.kernel.rest.AbstractJobProducerEndpoint#getServiceRegistry()
   */
  @Override
  protected ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }


  /**
   * Sets the composer service.
   * 
   * @param composerService
   *          the composer service
   */
  public void setComposerService(ComposerService composerService) {
    this.composerService = composerService;
  }

  /**
   * Callback from OSGi that is called when this service is activated.
   * 
   * @param cc
   *          OSGi component context
   */
  public void activate(ComponentContext cc) {
    if (cc == null || cc.getBundleContext().getProperty("org.opencastproject.server.url") == null) {
      serverUrl = UrlSupport.DEFAULT_BASE_URL;
    } else {
      serverUrl = cc.getBundleContext().getProperty("org.opencastproject.server.url");
      String serviceUrl = (String) cc.getProperties().get(RestConstants.SERVICE_PATH_PROPERTY);
      docs = generateDocs(serviceUrl);
    }
  }

  /**
   * Encodes a track.
   * 
   * @param sourceTrack
   *          The source track
   * @param profileId
   *          The profile to use in encoding this track
   * @return A response containing the job for this encoding job in the response body.
   * @throws Exception
   */
  @POST
  @Path("encode")
  @Produces(MediaType.TEXT_XML)
  public Response encode(@FormParam("sourceTrack") String sourceTrackAsXml, @FormParam("profileId") String profileId)
          throws Exception {
    // Ensure that the POST parameters are present
    if (sourceTrackAsXml == null || profileId == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack and profileId must not be null").build();
    }

    // Deserialize the track
    MediaPackageElement sourceTrack = MediaPackageElementParser.getFromXml(sourceTrackAsXml);
    if (!Track.TYPE.equals(sourceTrack.getElementType())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack element must be of type track").build();
    }

    // Asynchronously encode the specified tracks
    Job job = composerService.encode((Track) sourceTrack, profileId);
    if (job == null)
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Encoding failed").build();
    return Response.ok().entity(new JaxbJob(job)).build();
  }

  /**
   * Trims a track to a new length.
   * 
   * @param sourceTrack
   *          The source track
   * @param profileId
   *          the encoding profile to use for trimming
   * @param start
   *          the new trimming start time
   * @param duration
   *          the new video duration
   * @return A response containing the job for this encoding job in the response body.
   * @throws Exception
   */
  @POST
  @Path("trim")
  @Produces(MediaType.TEXT_XML)
  public Response trim(@FormParam("sourceTrack") String sourceTrackAsXml, @FormParam("profileId") String profileId,
          @FormParam("start") long start, @FormParam("duration") long duration) throws Exception {
    // Ensure that the POST parameters are present
    if (sourceTrackAsXml == null || profileId == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack and profileId must not be null").build();
    }

    // Deserialize the track
    MediaPackageElement sourceElement = MediaPackageElementParser.getFromXml(sourceTrackAsXml);
    if (!Track.TYPE.equals(sourceElement.getElementType())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack element must be of type track").build();
    }

    // Make sure the trim times make sense
    Track sourceTrack = (Track) sourceElement;
    if (start < 0) {
      return Response.status(Response.Status.BAD_REQUEST).entity("start time must be greater than 00:00:00").build();
    } else if (duration <= 0) {
      return Response.status(Response.Status.BAD_REQUEST).entity("duration must be greater than 00:00:00").build();
    } else if (start + duration > sourceTrack.getDuration()) {
      return Response.status(Response.Status.BAD_REQUEST).entity("requested duration exceeds track").build();
    }

    // Asynchronously encode the specified tracks
    Job job = composerService.trim(sourceTrack, profileId, start, duration);
    if (job == null)
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Trimming failed").build();
    return Response.ok().entity(new JaxbJob(job)).build();
  }

  /**
   * Encodes a track.
   * 
   * @param audioSourceTrack
   *          The audio source track
   * @param videoSourceTrack
   *          The video source track
   * @param profileId
   *          The profile to use in encoding this track
   * @return A response containing the job for this encoding job in the response body.
   * @throws Exception
   */
  @POST
  @Path("mux")
  @Produces(MediaType.TEXT_XML)
  public Response mux(@FormParam("audioSourceTrack") String audioSourceTrackXml,
          @FormParam("videoSourceTrack") String videoSourceTrackXml, @FormParam("profileId") String profileId)
          throws Exception {
    // Ensure that the POST parameters are present
    if (audioSourceTrackXml == null || videoSourceTrackXml == null || profileId == null) {
      return Response.status(Response.Status.BAD_REQUEST)
              .entity("audioSourceTrack, videoSourceTrack, and profileId must not be null").build();
    }

    // Deserialize the audio track
    MediaPackageElement audioSourceTrack = MediaPackageElementParser.getFromXml(audioSourceTrackXml);
    if (!Track.TYPE.equals(audioSourceTrack.getElementType())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("audioSourceTrack element must be of type track")
              .build();
    }

    // Deserialize the video track
    MediaPackageElement videoSourceTrack = MediaPackageElementParser.getFromXml(videoSourceTrackXml);
    if (!Track.TYPE.equals(videoSourceTrack.getElementType())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("videoSourceTrack element must be of type track")
              .build();
    }

    // Asynchronously encode the specified tracks
    Job job = composerService.mux((Track) videoSourceTrack, (Track) audioSourceTrack, profileId);
    return Response.ok().entity(new JaxbJob(job)).build();
  }

  /**
   * Encodes a track in a media package.
   * 
   * @param sourceTrack
   *          The source track
   * @param profileId
   *          The profile to use in encoding this track
   * @return A {@link Response} with the resulting track in the response body
   * @throws Exception
   */
  @POST
  @Path("image")
  @Produces(MediaType.TEXT_XML)
  public Response image(@FormParam("sourceTrack") String sourceTrackXml, @FormParam("profileId") String profileId,
          @FormParam("time") long time) throws Exception {
    // Ensure that the POST parameters are present
    if (sourceTrackXml == null || profileId == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack and profileId must not be null").build();
    }

    // Deserialize the source track
    MediaPackageElement sourceTrack = MediaPackageElementParser.getFromXml(sourceTrackXml);
    if (!Track.TYPE.equals(sourceTrack.getElementType())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack element must be of type track").build();
    }

    try {
      Job job = composerService.image((Track) sourceTrack, profileId, time);
      return Response.ok().entity(new JaxbJob(job)).build();
    } catch (EncoderException e) {
      logger.warn("Unable to extract image: " + e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Embeds captions in media file.
   * 
   * @param sourceTrackXml
   *          media file to which captions will be embedded
   * @param captionsXml
   *          captions that will be embedded
   * @param language
   *          language of captions
   * @return A response containing the job for this encoding job in the response body.
   * @throws Exception
   */
  @POST
  @Path("captions")
  @Produces(MediaType.TEXT_XML)
  public Response captions(@FormParam("mediaTrack") String sourceTrackXml, @FormParam("captions") String captionsAsXml,
          @FormParam("language") String language) throws Exception {
    if (sourceTrackXml == null || captionsAsXml == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Source track and captions must not be null").build();
    }

    MediaPackageElement mediaTrack = MediaPackageElementParser.getFromXml(sourceTrackXml);
    if (!Track.TYPE.equals(mediaTrack.getElementType())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Source track element must be of type track").build();
    }

    MediaPackageElement[] mpElements = toMediaPackageElementArray(captionsAsXml);
    if (mpElements.length == 0) {
      return Response.status(Response.Status.BAD_REQUEST).entity("At least one caption must be present").build();
    }
    // cast to catalogs
    Catalog[] captions = new Catalog[mpElements.length];
    for (int i = 0; i < mpElements.length; i++) {
      if (!Catalog.TYPE.equals(mpElements[i].getElementType())) {
        return Response.status(Response.Status.BAD_REQUEST).entity("All captions must be of type catalog").build();
      }
      captions[i] = (Catalog) mpElements[i];
    }

    try {
      Job job = composerService.captions((Track) mediaTrack, captions);
      return Response.ok().entity(new JaxbJob(job)).build();
    } catch (EmbedderException e) {
      logger.warn("Unable to embed captions: " + e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GET
  @Path("profiles.xml")
  @Produces(MediaType.TEXT_XML)
  public EncodingProfileList listProfiles() {
    List<EncodingProfileImpl> list = new ArrayList<EncodingProfileImpl>();
    for (EncodingProfile p : composerService.listProfiles()) {
      list.add((EncodingProfileImpl) p);
    }
    return new EncodingProfileList(list);
  }

  @GET
  @Path("profile/{id}.xml")
  @Produces(MediaType.TEXT_XML)
  public Response getProfile(@PathParam("id") String profileId) {
    EncodingProfileImpl profile = (EncodingProfileImpl) composerService.getProfile(profileId);
    if (profile == null)
      return Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).build();
    return Response.ok(profile).build();
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    return docs;
  }

  protected String generateDocs(String serviceUrl) {
    DocRestData data = new DocRestData("Composer", "Composer Service", serviceUrl, new String[] { "$Rev$" });
    // profiles
    RestEndpoint profilesEndpoint = new RestEndpoint("profiles", RestEndpoint.Method.GET, "/profiles.xml",
            "Retrieve the encoding profiles");
    profilesEndpoint.addStatus(org.opencastproject.util.doc.Status
            .ok("Results in an xml document describing the available encoding profiles"));
    profilesEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, profilesEndpoint);

    RestEndpoint profileEndpoint = new RestEndpoint("profiles", RestEndpoint.Method.GET, "/profile/{id}.xml",
            "Retrieve an encoding profile");
    profileEndpoint.addStatus(org.opencastproject.util.doc.Status
            .ok("Results in an xml document describing the requested encoding profile"));
    profileEndpoint.addPathParam(new Param("id", Param.Type.STRING, "mov-low.http", "the profile ID"));
    profileEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, profileEndpoint);

    // job
    RestEndpoint jobEndpoint = new RestEndpoint("job", RestEndpoint.Method.GET, "/job/{id}.xml",
            "Retrieve a job for an encoding task");
    jobEndpoint.addStatus(org.opencastproject.util.doc.Status
            .ok("Results in an xml document containing the status of the encoding job, and the track produced by this "
                    + "encoding job if it the task is finished"));
    jobEndpoint.addPathParam(new Param("id", Param.Type.STRING, null, "the job id"));
    jobEndpoint.addFormat(new Format("xml", null, null));
    jobEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, jobEndpoint);

    // count
    RestEndpoint countEndpoint = new RestEndpoint("count", RestEndpoint.Method.GET, "/count",
            "Count the number of jobs");
    countEndpoint.addStatus(org.opencastproject.util.doc.Status
            .ok("Result body contains the number of jobs matching the query parameters"));
    countEndpoint.addOptionalParam(new Param("status", Param.Type.STRING, "FINISHED",
            "the job status (QUEUED, RUNNING, FINISHED, FAILED)"));
    countEndpoint.addOptionalParam(new Param("host", Param.Type.STRING, serverUrl,
            "the host responsible for this encoding job"));
    countEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, countEndpoint);

    // encode
    RestEndpoint encodeEndpoint = new RestEndpoint("encode", RestEndpoint.Method.POST, "/encode",
            "Starts an encoding process, based on the specified encoding profile ID and the track");
    encodeEndpoint.addStatus(org.opencastproject.util.doc.Status
            .ok("Results in an xml document containing the job for the encoding task"));
    encodeEndpoint.addRequiredParam(new Param("sourceTrack", Type.STRING, generateVideoTrack(),
            "The track containing the stream"));
    encodeEndpoint.addRequiredParam(new Param("profileId", Type.STRING, "flash.http", "The encoding profile to use"));
    encodeEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, encodeEndpoint);

    // @FormParam("sourceTrack") String sourceTrackAsXml, @FormParam("profileId") String profileId,
    // @FormParam("start") long start, @FormParam("duration") long duration
    // trim
    RestEndpoint trimEndpoint = new RestEndpoint("trim", RestEndpoint.Method.POST, "/trim",
            "Starts a trimming process, based on the specified track, start time and duration in ms");
    trimEndpoint.addStatus(org.opencastproject.util.doc.Status
            .ok("Results in an xml document containing the job for the trimming task"));
    trimEndpoint.addStatus(org.opencastproject.util.doc.Status
            .badRequest("if the start time is negative or exceeds the track duration"));
    trimEndpoint.addStatus(org.opencastproject.util.doc.Status
            .badRequest("if the duration is negative or, including the new start time, exceeds the track duration"));
    trimEndpoint.addRequiredParam(new Param("sourceTrack", Type.TEXT, generateVideoTrack(),
            "The track containing the stream"));
    trimEndpoint.addRequiredParam(new Param("profileId", Type.STRING, "trim.work",
            "The encoding profile to use for trimming"));
    trimEndpoint.addRequiredParam(new Param("start", Type.STRING, "0", "The start time in milisecond"));
    trimEndpoint.addRequiredParam(new Param("duration", Type.STRING, "10000", "The duration in milisecond"));
    trimEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, trimEndpoint);

    // mux
    RestEndpoint muxEndpoint = new RestEndpoint("mux", RestEndpoint.Method.POST, "/mux",
            "Starts an encoding process, which will mux the two tracks using the given encoding profile");
    muxEndpoint.addStatus(org.opencastproject.util.doc.Status
            .ok("Results in an xml document containing the job for the encoding task"));
    muxEndpoint.addRequiredParam(new Param("sourceAudioTrack", Type.STRING, generateAudioTrack(),
            "The track containing the audio stream"));
    muxEndpoint.addRequiredParam(new Param("sourceVideoTrack", Type.STRING, generateVideoTrack(),
            "The track containing the video stream"));
    muxEndpoint.addRequiredParam(new Param("profileId", Type.STRING, "flash.http", "The encoding profile to use"));
    muxEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, muxEndpoint);

    // image
    RestEndpoint imageEndpoint = new RestEndpoint("image", RestEndpoint.Method.POST, "/image",
            "Starts an image extraction process, based on the specified encoding profile ID and the source track");
    imageEndpoint.addStatus(org.opencastproject.util.doc.Status
            .ok("Results in an xml document containing the image attachment"));
    imageEndpoint.addRequiredParam(new Param("time", Type.STRING, "1",
            "The number of seconds into the video to extract the image"));
    imageEndpoint.addRequiredParam(new Param("sourceTrack", Type.STRING, generateVideoTrack(),
            "The track containing the video stream"));
    imageEndpoint.addRequiredParam(new Param("profileId", Type.STRING, "player-preview.http",
            "The encoding profile to use"));
    imageEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, imageEndpoint);

    // captions
    RestEndpoint captionsEndpoint = new RestEndpoint("captions", RestEndpoint.Method.POST, "/captions",
            "Starts caption embedding process, based on the specified source track and captions");
    captionsEndpoint.addStatus(org.opencastproject.util.doc.Status
            .ok("Result in an xml document containing resulting media file."));
    captionsEndpoint.addRequiredParam(new Param("mediaTrack", Type.STRING, generateMediaTrack(),
            "QuickTime file containg video stream"));
    captionsEndpoint.addRequiredParam(new Param("captions", Type.STRING, generateCaptionsCatalogs(),
            "Catalog(s) containing captions in SRT format"));
    captionsEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, captionsEndpoint);

    return DocUtil.generate(data);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.kernel.rest.AbstractJobProducerEndpoint#getService()
   */
  @Override
  public JobProducer getService() {
    if (composerService instanceof JobProducer)
      return (JobProducer) composerService;
    else
      return null;
  }

  /**
   * Converts string representation of the one or more catalogs to object array
   * 
   * @param elementsAsXml
   *          the serialized elements array representation
   * @return
   * @throws ParserConfigurationException
   * @throws SAXException
   * @throws IOException
   */
  protected MediaPackageElement[] toMediaPackageElementArray(String elementsAsXml) throws ParserConfigurationException,
          SAXException, IOException {

    List<MediaPackageElement> mpElements = new LinkedList<MediaPackageElement>();

    DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document doc = docBuilder.parse(IOUtils.toInputStream(elementsAsXml, "UTF-8"));
    // TODO -> explicit check for root node name?
    NodeList nodeList = doc.getDocumentElement().getChildNodes();

    MediaPackageSerializer serializer = new DefaultMediaPackageSerializerImpl();
    MediaPackageElementBuilder builder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();

    for (int i = 0; i < nodeList.getLength(); i++) {
      if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
        mpElements.add(builder.elementFromManifest(nodeList.item(i), serializer));
      }
    }

    return mpElements.toArray(new MediaPackageElement[mpElements.size()]);
  }

  protected String generateVideoTrack() {
    return "<track id=\"track-1\" type=\"presentation/source\">\n" + "  <mimetype>video/quicktime</mimetype>\n"
            + "  <url>" + serverUrl + "/workflow/samples/camera.mpg</url>\n"
            + "  <checksum type=\"md5\">43b7d843b02c4a429b2f547a4f230d31</checksum>\n"
            + "  <duration>14546</duration>\n" + "  <video>\n"
            + "    <device type=\"UFG03\" version=\"30112007\" vendor=\"Unigraf\" />\n"
            + "    <encoder type=\"H.264\" version=\"7.4\" vendor=\"Apple Inc\" />\n"
            + "    <resolution>640x480</resolution>\n" + "    <scanType type=\"progressive\" />\n"
            + "    <bitrate>540520</bitrate>\n" + "    <frameRate>2</frameRate>\n" + "  </video>\n" + "</track>";
  }

  protected String generateAudioTrack() {
    return "<track id=\"track-2\" type=\"presentation/source\">\n" + "  <mimetype>audio/mp3</mimetype>\n"
            + "  <url>serverUrl/workflow/samples/audio.mp3</url>\n"
            + "  <checksum type=\"md5\">950f9fa49caa8f1c5bbc36892f6fd062</checksum>\n"
            + "  <duration>10472</duration>\n" + "  <audio>\n" + "    <channels>2</channels>\n"
            + "    <bitdepth>0</bitdepth>\n" + "    <bitrate>128004.0</bitrate>\n"
            + "    <samplingrate>44100</samplingrate>\n" + "  </audio>\n" + "</track>";
  }

  protected String generateMediaTrack() {
    return "<track id=\"track-3\">\n" + "  <mimetype>video/quicktime</mimetype>\n"
            + "  <url>serverUrl/workflow/samples/slidechanges.mov</url>\n"
            + "  <checksum type=\"md5\">4cbcc9223c0425a54c3f253823487d5f</checksum>\n"
            + "  <duration>27626</duration>\n" + "  <video>\n" + "    <resolution>1024x768</resolution>"
            + "  </video>\n" + "</track>";
  }

  protected String generateCaptionsCatalogs() {
    return "<captions>\n" + "  <catalog id=\"catalog-1\">\n" + "    <mimetype>application/x-subrip</mimetype>\n"
            + "    <url>serverUrl/workflow/samples/captions_test_eng.srt</url>\n"
            + "    <checksum type=\"md5\">55d70b062896aa685e2efc4226b32980</checksum>\n" + "    <tags>\n"
            + "      <tag>lang:en</tag>\n" + "    </tags>\n" + "  </catalog>\n" + "  <catalog id=\"catalog-2\">\n"
            + "    <mimetype>application/x-subrip</mimetype>\n"
            + "    <url>serverUrl/workflow/samples/captions_test_fra.srt</url>\n"
            + "    <checksum type=\"md5\">8f6cd99bbb6d591107f3b5c47ee51f2c</checksum>\n" + "    <tags>\n"
            + "      <tag>lang:fr</tag>\n" + "    </tags>\n" + "  </catalog>\n" + "</captions>\n";
  }

}
