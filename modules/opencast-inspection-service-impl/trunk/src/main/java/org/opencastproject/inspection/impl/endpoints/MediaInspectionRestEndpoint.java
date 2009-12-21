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
package org.opencastproject.inspection.impl.endpoints;

import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.media.mediapackage.Stream;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.media.mediapackage.jaxb.AudioType;
import org.opencastproject.media.mediapackage.jaxb.ChecksumType;
import org.opencastproject.media.mediapackage.jaxb.ObjectFactory;
import org.opencastproject.media.mediapackage.jaxb.ScanTypeType;
import org.opencastproject.media.mediapackage.jaxb.TrackType;
import org.opencastproject.media.mediapackage.jaxb.VideoType;
import org.opencastproject.media.mediapackage.track.AudioStreamImpl;
import org.opencastproject.media.mediapackage.track.VideoStreamImpl;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;
import org.opencastproject.util.doc.Status;

import java.net.URI;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * A service endpoint to expose the {@link MediaInspectionService} via REST.
 */
@Path("/")
public class MediaInspectionRestEndpoint {
  protected MediaInspectionService service;

  public void setService(MediaInspectionService service) {
    this.service = service;
  }

  public MediaInspectionRestEndpoint() {
    docs = generateDocs();
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  public Response getTrack(@QueryParam("url") URI url) {
    try {
      Track t = service.inspect(url);
      ObjectFactory of = new ObjectFactory();
      TrackType track = of.createTrackType();
      ChecksumType checksum = of.createChecksumType();
      checksum.setType(t.getChecksum().getType().getName());
      checksum.setValue(t.getChecksum().getValue());
      track.setDuration(t.getDuration());
      track.setChecksum(checksum);

      for (Stream stream : t.getStreams()) {
        if (stream instanceof AudioStreamImpl) {
          AudioStreamImpl a = (AudioStreamImpl) stream;
          AudioType audio = of.createAudioType();
          audio.setBitrate(a.getBitRate());
          audio.setChannels(a.getChannels());
          audio.setSamplingrate(a.getSamplingRate());
          track.setAudio(audio);
        } else if (stream instanceof VideoStreamImpl) {
          VideoStreamImpl v = (VideoStreamImpl) stream;
          VideoType video = of.createVideoType();
          video.setBitrate(v.getBitRate());
          video.setFrameRate(v.getFrameRate());
          ScanTypeType scanType = of.createScanTypeType();
          scanType.setType(v.getScanType().name());
          video.setScanType(scanType);
          track.setVideo(video);
        } else {
          throw new IllegalStateException("stream is of an unknown type: " + stream);
        }
      }
      return Response.ok(track).build();
    } catch (Exception e) {
      return Response.serverError().status(400).build();
    }
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    return docs;
  }

  protected final String docs;
  private String[] notes = {
          "All paths above are relative to the REST endpoint base (something like http://your.server/inspection/rest)",
          "If the service is down or not working it will return a status 503, this means the the underlying service is not working and is either restarting or has failed",
          "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In other words, there is a bug! You should file an error report with your server logs from the time when the error occurred: <a href=\"https://issues.opencastproject.org\">Opencast Issue Tracker</a>",
          "Here is a sample video for testing: <a href=\"./?url=http://source.opencastproject.org/svn/modules/opencast-media/trunk/src/test/resources/aonly.mov\">analyze sample video</a>" };

  private String generateDocs() {

    DocRestData data = new DocRestData("inspection", "Media inspection", "/inspection/rest", notes);

    // getTrack
    RestEndpoint endpoint = new RestEndpoint("getTrack", RestEndpoint.Method.GET, "/",
            "Analyzing media file provided by url");
    endpoint.addOptionalParam(new Param("url", Param.Type.STRING, null, "URL to the media that will be analyzed"));
    endpoint.addFormat(Format.xml());
    endpoint.addStatus(Status.OK("Result is returned"));
    endpoint.addStatus(new Status(400, "Problem retrieving media file or invalid media file"));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, endpoint);

    return DocUtil.generate(data);
  }
}
