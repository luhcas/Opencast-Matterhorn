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
package org.opencastproject.composer.impl.endpoint;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.impl.EncodingProfileImpl;
import org.opencastproject.media.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.media.mediapackage.jaxb.MediapackageType;
import org.opencastproject.media.mediapackage.jaxb.TrackType;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * A REST endpoint delegating functionality to the {@link ComposerService}
 */
@Path("/")
public class ComposerRestService {
  private static final Logger logger = LoggerFactory.getLogger(ComposerRestService.class);
  
  protected ComposerService composerService;
  public void setComposerService(ComposerService composerService) {
    this.composerService = composerService;
  }

  /**
   * Encodes a track in a media package.
   * 
   * @param mediaPackageType The JAXB version of MediaPackage
   * @param audioSourceTrackId The ID of the audio source track in the media package to be encoded
   * @param videoSourceTrackId The ID of the video source track in the media package to be encoded
   * @param profileId The profile to use in encoding this track
   * @return The JAXB version of {@link Track} {@link TrackType} 
   * @throws Exception
   */
  @POST
  @Path("encode")
  @Produces(MediaType.TEXT_XML)
  public Response encode(
          @FormParam("mediapackage") MediapackageType mediaPackageType,
          @FormParam("audioSourceTrackId") String audioSourceTrackId,
          @FormParam("videoSourceTrackId") String videoSourceTrackId,
          @FormParam("targetTrackId") String targetTrackId,
          @FormParam("profileId") String profileId) throws Exception {
    // Ensure that the POST parameters are present
    if(mediaPackageType == null || audioSourceTrackId == null || videoSourceTrackId == null || profileId == null) {
      return Response.status(Status.BAD_REQUEST).entity("mediapackage, audioSourceTrackId, videoSourceTrackId, and profileId must not be null").build();
    }
    logger.info("Encoding audio track {} and video track {} of mediapackage {} using profile {}",
            new String[] {audioSourceTrackId, videoSourceTrackId, mediaPackageType.getId(), profileId});

    // Build a media package from the POSTed XML
    MediaPackage mediaPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().
        loadFromManifest(IOUtils.toInputStream(mediaPackageType.toXml()));
    
    // Encode the specified tracks, TODO Make use of the Future to turn this into an asynchronous rest call
    Track track = composerService.encode(mediaPackage, videoSourceTrackId, audioSourceTrackId, targetTrackId, profileId).get();
    
    // Return the JAXB version of the track
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    doc.appendChild(track.toManifest(doc, new DefaultMediaPackageSerializerImpl()));
    return Response.ok(TrackType.fromXml(doc)).build();
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.composer.api.ComposerService#listProfiles()
   */
  @GET
  @Path("profiles")
  @Produces(MediaType.TEXT_XML)
  public EncodingProfileList listProfiles() {
    List<EncodingProfileImpl> list = new ArrayList<EncodingProfileImpl>();
    for(EncodingProfile p : composerService.listProfiles()) {
      list.add((EncodingProfileImpl) p);
    }
    return new EncodingProfileList(list);
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    return docs;
  }

  protected final String docs;
  
  public ComposerRestService() {
    // Pre-load the documentation
    String docsFromClassloader = null;
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream("/html/index.html");
      docsFromClassloader = IOUtils.toString(in);
    } catch (IOException e) {
      logger.error("failed to read documentation", e);
      docsFromClassloader = "unable to load documentation for " + ComposerRestService.class.getName();
    } finally {
      IOUtils.closeQuietly(in);
    }
    docs = docsFromClassloader;
  }
}
