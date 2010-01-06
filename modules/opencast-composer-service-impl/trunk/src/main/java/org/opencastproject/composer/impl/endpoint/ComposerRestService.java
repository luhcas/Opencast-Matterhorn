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
import org.opencastproject.composer.impl.endpoint.Receipt.STATUS;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.media.mediapackage.jaxb.MediapackageType;
import org.opencastproject.media.mediapackage.jaxb.TrackType;

import org.apache.commons.io.IOUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

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
 * A REST endpoint delegating functionality to the {@link ComposerService}
 */
@Path("/")
public class ComposerRestService {
  private static final Logger logger = LoggerFactory.getLogger(ComposerRestService.class);
  
  protected ComposerService composerService;
  public void setComposerService(ComposerService composerService) {
    this.composerService = composerService;
  }

  private ComposerServiceDao dao;
  public void setDao(ComposerServiceDao dao) {
    this.dao = dao;
  }

  protected Map<String, Future<Track>> futuresMap = null;
  protected Thread pollingThread = null;
  protected boolean poll;
  public void activate(ComponentContext cc) {
    futuresMap = new ConcurrentHashMap<String, Future<Track>>();
    poll = true;
    pollingThread = new Thread(new Runnable() {
      public void run() {
        while(poll) {
          logger.debug("polling for completed encoding tasks");
          for(Iterator<Entry<String, Future<Track>>> entryIter = futuresMap.entrySet().iterator(); entryIter.hasNext();) {
            Entry<String, Future<Track>> entry = entryIter.next();
            Future<Track> futureTrack = entry.getValue();
            String id = entry.getKey();
            logger.debug("found receipt {} while polling", id);
            if(futureTrack.isDone()) {
              logger.debug("encoding task with receipt {} is done", id);
              // update the database
              Receipt receipt = dao.getReceipt(id);
              if(receipt == null) throw new RuntimeException("Could not find the receipt for encoding job " + id);
              try {
                Track t = futureTrack.get();
                if(t == null) {
                  // this was a failed encoding job
                  receipt.setStatus(STATUS.FAILED.toString());
                } else {
                  receipt.setStatus(STATUS.FINISHED.toString());
                  receipt.setTrack(TrackType.fromTrack(t));
                }
                dao.updateReceipt(receipt);
                entryIter.remove();
              } catch (Exception e) {
                e.printStackTrace();
                logger.error(e.getMessage());
                receipt.setStatus(STATUS.FAILED.toString());
                dao.updateReceipt(receipt);
              }
            } else {
              logger.debug("encoding task for receipt {} is still running", id);
            }
          }
          try {
            Thread.sleep(10 * 1000); // check again in 10 seconds
          } catch (InterruptedException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
          }
        }
      }
    });
    pollingThread.start();
  }

  protected void deactivate() {
    poll = false;
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

    // Build a media package from the POSTed XML
    MediaPackage mediaPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().
        loadFromManifest(IOUtils.toInputStream(mediaPackageType.toXml()));
    
    // Asynchronously encode the specified tracks
    Receipt receipt = dao.createReceipt();
    receipt.setStatus(STATUS.RUNNING.toString());
    logger.debug("created receipt {}", receipt.getId());
    try {
      Future<Track> futureTrack = composerService.encode(mediaPackage, videoSourceTrackId, audioSourceTrackId, targetTrackId, profileId);
      futuresMap.put(receipt.getId(), futureTrack);
      return Response.ok().entity(receipt).build();
    } catch (RuntimeException e) {
      e.printStackTrace();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new Receipt(null, STATUS.FAILED.toString())).build();
    }
  }

  @GET
  @Path("receipt/{id}.xml")
  @Produces(MediaType.TEXT_XML)
  public Response getReceipt(@PathParam("id") String id) {
    Receipt r = dao.getReceipt(id);
    if(r== null) return Response.status(Status.NOT_FOUND).entity("no receipt found with id " + id).build();
    return Response.ok().entity(r).build();
  }
  
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
