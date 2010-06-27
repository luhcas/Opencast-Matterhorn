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
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.api.EncodingProfileImpl;
import org.opencastproject.composer.api.EncodingProfileList;
import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageSerializer;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.remote.api.Receipt;
import org.opencastproject.remote.api.Receipt.Status;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;
import org.opencastproject.util.doc.Param.Type;

import org.apache.commons.io.IOUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * A REST endpoint delegating functionality to the {@link ComposerService}
 */
@Path("/")
public class ComposerRestService {

  private static final Logger logger = LoggerFactory.getLogger(ComposerRestService.class);
  protected String docs;
  protected String serverUrl;
  protected ComposerService composerService;

  public void setComposerService(ComposerService composerService) {
    this.composerService = composerService;
  }

  public void activate(ComponentContext cc) {
    if (cc == null || cc.getBundleContext().getProperty("org.opencastproject.server.url") == null) {
      serverUrl = UrlSupport.DEFAULT_BASE_URL;
    } else {
      serverUrl = cc.getBundleContext().getProperty("org.opencastproject.server.url");
    }
    docs = generateDocs();
  }

  protected void deactivate() {
  }

  /**
   * Encodes a track.
   * 
   * @param sourceTrack
   *          The source track
   * @param profileId
   *          The profile to use in encoding this track
   * @return A response containing the receipt for this encoding job in the response body.
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
    MediaPackageElement sourceTrack = toMediaPackageElement(sourceTrackAsXml);
    if (!Track.TYPE.equals(sourceTrack.getElementType())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack element must be of type track").build();
    }

    // Asynchronously encode the specified tracks
    Receipt receipt = composerService.encode((Track) sourceTrack, profileId);
    if (receipt == null)
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Encoding failed").build();
    return Response.ok().entity(receipt.toXml()).build();
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
   * @return A response containing the receipt for this encoding job in the response body.
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
      return Response.status(Response.Status.BAD_REQUEST).entity(
              "audioSourceTrack, videoSourceTrack, and profileId must not be null").build();
    }

    // Deserialize the audio track
    MediaPackageElement audioSourceTrack = toMediaPackageElement(audioSourceTrackXml);
    if (!Track.TYPE.equals(audioSourceTrack.getElementType())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("audioSourceTrack element must be of type track")
              .build();
    }

    // Deserialize the video track
    MediaPackageElement videoSourceTrack = toMediaPackageElement(videoSourceTrackXml);
    if (!Track.TYPE.equals(videoSourceTrack.getElementType())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("videoSourceTrack element must be of type track")
              .build();
    }

    // Asynchronously encode the specified tracks
    Receipt receipt = composerService.mux((Track) videoSourceTrack, (Track) audioSourceTrack, profileId);
    return Response.ok().entity(receipt.toXml()).build();
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
  public Response image(@FormParam("sourceTrack") String sourceTrackXml,
          @FormParam("profileId") String profileId, @FormParam("time") long time) throws Exception {
    // Ensure that the POST parameters are present
    if (sourceTrackXml == null || profileId == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack and profileId must not be null").build();
    }

    // Deserialize the source track
    MediaPackageElement sourceTrack = toMediaPackageElement(sourceTrackXml);
    if (!Track.TYPE.equals(sourceTrack.getElementType())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack element must be of type track").build();
    }

    try {
      Receipt receipt = composerService.image((Track) sourceTrack, profileId, time);
      return Response.ok().entity(receipt.toXml()).build();
    } catch (EncoderException e) {
      logger.warn("Unable to extract image: " + e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GET
  @Path("receipt/{id}.xml")
  @Produces(MediaType.TEXT_XML)
  public Response getReceipt(@PathParam("id") String id) {
    Receipt r = composerService.getReceipt(id);
    if (r == null)
      return Response.status(Response.Status.NOT_FOUND).build();
    return Response.ok().entity(r.toXml()).build();
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
  @Produces(MediaType.TEXT_PLAIN)
  @Path("count")
  public Response count(@QueryParam("status") String status, @QueryParam("host") String host) {
    if (status == null)
      return Response.status(javax.ws.rs.core.Response.Status.BAD_REQUEST).build();
    if (host == null)
      return Response.ok(composerService.countJobs(Status.valueOf(status.toUpperCase()))).build();
    return Response.ok(composerService.countJobs(Status.valueOf(status.toUpperCase()), host)).build();
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    return docs;
  }

  protected String generateDocs() {
    DocRestData data = new DocRestData("Composer", "Composer Service", "/composer/rest",
            new String[] { "$Rev$" });
    // profiles
    RestEndpoint profilesEndpoint = new RestEndpoint("profiles", RestEndpoint.Method.GET, "/profiles.xml",
            "Retrieve the encoding profiles");
    profilesEndpoint.addStatus(org.opencastproject.util.doc.Status
            .OK("Results in an xml document describing the available encoding profiles"));
    profilesEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, profilesEndpoint);

    RestEndpoint profileEndpoint = new RestEndpoint("profiles", RestEndpoint.Method.GET, "/profile/{id}.xml",
            "Retrieve an encoding profile");
    profileEndpoint.addStatus(org.opencastproject.util.doc.Status
            .OK("Results in an xml document describing the requested encoding profile"));
    profileEndpoint.addPathParam(new Param("id", Param.Type.STRING, "mov-low.http", "the profile ID"));
    profileEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, profileEndpoint);

    // receipt
    RestEndpoint receiptEndpoint = new RestEndpoint("receipt", RestEndpoint.Method.GET, "/receipt/{id}.xml",
            "Retrieve a receipt for an encoding task");
    receiptEndpoint.addStatus(org.opencastproject.util.doc.Status
            .OK("Results in an xml document containing the status of the encoding job, and the track produced by this "
                    + "encoding job if it the task is finished"));
    receiptEndpoint.addPathParam(new Param("id", Param.Type.STRING, null, "the receipt id"));
    receiptEndpoint.addFormat(new Format("xml", null, null));
    receiptEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, receiptEndpoint);

    // count
    RestEndpoint countEndpoint = new RestEndpoint("count", RestEndpoint.Method.GET, "/count",
            "Count the number of receipts");
    countEndpoint.addStatus(org.opencastproject.util.doc.Status
            .OK("Result body contains the number of receipts matching the query parameters"));
    countEndpoint.addOptionalParam(new Param("status", Param.Type.STRING, "FINISHED",
            "the receipt status (QUEUED, RUNNING, FINISHED, FAILED)"));
    countEndpoint.addOptionalParam(new Param("host", Param.Type.STRING, "localhost",
            "the host responsible for this encoding job"));
    countEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, countEndpoint);

    // encode
    RestEndpoint encodeEndpoint = new RestEndpoint("encode", RestEndpoint.Method.POST, "/encode",
            "Starts an encoding process, based on the specified encoding profile ID and the track");
    encodeEndpoint.addStatus(org.opencastproject.util.doc.Status
            .OK("Results in an xml document containing the receipt for the encoding task"));
    encodeEndpoint.addRequiredParam(new Param("sourceTrack", Type.STRING, generateVideoTrack(),
            "The track containing the stream"));
    encodeEndpoint.addRequiredParam(new Param("profileId", Type.STRING, "flash.http", "The encoding profile to use"));
    encodeEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, encodeEndpoint);

    // mux
    RestEndpoint muxEndpoint = new RestEndpoint("mux", RestEndpoint.Method.POST, "/mux",
            "Starts an encoding process, which will mux the two tracks using the given encoding profile");
    muxEndpoint.addStatus(org.opencastproject.util.doc.Status
            .OK("Results in an xml document containing the receipt for the encoding task"));
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
            .OK("Results in an xml document containing the image attachment"));
    imageEndpoint.addRequiredParam(new Param("time", Type.STRING, "1",
            "The number of seconds into the video to extract the image"));
    imageEndpoint.addRequiredParam(new Param("sourceTrack", Type.STRING, generateVideoTrack(),
            "The track containing the video stream"));
    imageEndpoint.addRequiredParam(new Param("profileId", Type.STRING, "player-preview.http",
            "The encoding profile to use"));
    imageEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, imageEndpoint);

    return DocUtil.generate(data);
  }

  /**
   * Converts the string representation of the track to an object.
   * 
   * @param trackAsXml
   *          the serialized track representation
   * @return the track object
   * @throws SAXException
   * @throws IOException
   * @throws ParserConfigurationException
   */
  protected MediaPackageElement toMediaPackageElement(String trackAsXml) throws SAXException, IOException,
          ParserConfigurationException {
    DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document doc = docBuilder.parse(IOUtils.toInputStream(trackAsXml, "UTF-8"));
    MediaPackageSerializer serializer = new DefaultMediaPackageSerializerImpl();
    MediaPackageElementBuilder builder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
    MediaPackageElement sourceTrack = builder.elementFromManifest(doc.getDocumentElement(), serializer);
    return sourceTrack;
  }

  protected String generateVideoTrack() {
    return "<track id=\"track-1\" type=\"presentation/source\">\n" + "  <mimetype>video/quicktime</mimetype>\n"
            + "  <url>serverUrl/workflow/samples/camera.mpg</url>\n"
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

}
