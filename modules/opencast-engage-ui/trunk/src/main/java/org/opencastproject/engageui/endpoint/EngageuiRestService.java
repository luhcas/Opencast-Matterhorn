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
package org.opencastproject.engageui.endpoint;

import org.opencastproject.engageui.api.EpisodeView;
import org.opencastproject.engageui.api.EpisodeViewImpl;
import org.opencastproject.engageui.api.EpisodeViewListImpl;
import org.opencastproject.engageui.api.EpisodeViewListResultImpl;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageElements;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchResultItem;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;
import org.opencastproject.util.doc.Param.Type;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * REST endpoint for the Engage UI proxy service
 */
@Path("/")
public class EngageuiRestService {
  private static final Logger logger = LoggerFactory.getLogger(EngageuiRestService.class);

  private SearchService searchService;

  public static final int TITLE_MAX_LENGTH = 60;
  public static final int ABSTRACT_MAX_LENGTH = 175;
  public static final String DEFAULT_VIDEO_URL = "http://vs1.rz.uni-osnabrueck.de/public/virtmm/opencast/matterhorn.mp4";

  public void setSearchService(SearchService service) {
    logger.info("binding SearchService");
    searchService = service;
  }

  public void unsetSearchService(SearchService service) {
    logger.info("unbinding SearchService");
    searchService = null;
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("episode")
  public EpisodeViewImpl getEpisodeById(@QueryParam("episodeId") String episodeId) {
    // Variables
    String mediaPackageId, dcTitle, dcCreator, dcContributor, dcAbstract, cover, dcCreated, dcRightsHolder, videoUrl;
    EpisodeViewImpl episodeViewItem = null;
    SearchResult result;
    DateFormat format;
    SearchResultItem[] searchResultItems;

    result = searchService.getEpisodeById(episodeId);

    // Get a DateFormat
    format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);

    searchResultItems = result.getItems();

    for (int i = 0; i < searchResultItems.length; i++) {
      // Get the the sarchResultItem on position i in the array
      SearchResultItem searchResultItem = searchResultItems[i];

      // Getting fields from the searchResultItem
      mediaPackageId = searchResultItem.getId();
      cover = searchResultItem.getCover();

      dcTitle = searchResultItem.getDcTitle();

      dcCreator = searchResultItem.getDcCreator();

      dcContributor = searchResultItem.getDcContributor();
      dcAbstract = searchResultItem.getDcAbstract();
      dcRightsHolder = searchResultItem.getDcRightsHolder();

      // Get and format the recording date
      if (searchResultItem.getDcCreated() == null)
        dcCreated = "";
      else
        dcCreated = format.format(searchResultItem.getDcCreated());

      videoUrl = getVideoUrl(searchResultItem.getMediaPackage());

      try {
        // URLEncode the media package id
        mediaPackageId = URLEncoder.encode(mediaPackageId, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }

      // Setting fields to the episodeViewItem
      episodeViewItem = new EpisodeViewImpl();
      episodeViewItem.setDcTitle(dcTitle);
      episodeViewItem.setDcCreator(dcCreator);
      episodeViewItem.setDcContributor(dcContributor);
      episodeViewItem.setDcAbstract(dcAbstract);
      episodeViewItem.setDcRightsHolder(dcRightsHolder);
      episodeViewItem.setVideoUrl(videoUrl);

      episodeViewItem.setDcCreated(dcCreated);
      if (cover == null || cover.equals(""))
        episodeViewItem.setCover("img/thumbnail.png");
      else
        episodeViewItem.setCover(cover);
      episodeViewItem.setURLEncodedMediaPackageId(mediaPackageId);
    }

    return episodeViewItem;
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("search")
  public Response getEpisodesByDate(@QueryParam("q") String text, @QueryParam("page") int page) {
    // Variables
    int pagemax, fromIndex, toIndex;
    long episodesMax;
    String mediaPackageId, dcTitle, dcCreator, dcContributor, dcAbstract, cover, dcCreated, dcRightsHolder, videoUrl;
    EpisodeViewListResultImpl episodeViewListResult;
    EpisodeViewListImpl episodeViewList;
    EpisodeView episodeViewItem;
    SearchResult result;

    // Creating a new EpisodeViewListResultImpl
    episodeViewListResult = new EpisodeViewListResultImpl();

    // Creating a new EpisodeViewListImpl
    episodeViewList = new EpisodeViewListImpl();

    // Adding episodeViewList to episodeViewListResult
    episodeViewListResult.setEpisodeViewList(episodeViewList);

    // Checking if the searchService is available
    if (searchService == null) {
      logger.warn("search service not present, returning empty list");
      // in case of error
      Response.status(Status.BAD_REQUEST).build();
    }

    if (StringUtils.isEmpty(text)) {
      // Get episodes by date from the search service
      result = searchService.getEpisodesByDate(10, (page - 1) * 10);
    } else {
      result = searchService.getEpisodesByText(text, 10, (page - 1) * 10);
    }

    SearchResultItem[] searchResultItems = result.getItems();

    // Set episodesMax
    episodesMax = result.getTotalSize();

    // Set the episodes max
    episodeViewListResult.setEpisodesMax(episodesMax);

    // Calculate the page maximum
    pagemax = (int) (episodesMax / 10);

    if (episodesMax % 10 != 0)
      pagemax = pagemax + 1;
    else if (pagemax == 0)
      pagemax = 1;

    // Set the page maximum
    episodeViewListResult.setPageMax(pagemax);

    // Calculate fromIndex
    fromIndex = (page * 10) - 10;

    // Set the fromIndex
    if (searchResultItems.length == 0) {
      episodeViewListResult.setFromIndex(0);
    } else {
      episodeViewListResult.setFromIndex(fromIndex + 1);
    }

    // Calculate toIndex
    toIndex = Math.min(fromIndex + 10, (page - 1) * 10 + searchResultItems.length);

    // Set the toIndex
    episodeViewListResult.setToIndex(toIndex);

    // Get a DateFormat
    DateFormat format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);

    for (int i = 0; i < searchResultItems.length; i++) {
      // Get the the sarchResultItem on position i in the array
      SearchResultItem searchResultItem = searchResultItems[i];

      // Getting fields from the searchResultItem
      mediaPackageId = searchResultItem.getId();
      cover = searchResultItem.getCover();

      if (searchResultItem.getDcTitle().length() > TITLE_MAX_LENGTH)
        dcTitle = searchResultItem.getDcTitle().substring(0, TITLE_MAX_LENGTH) + "...";
      else
        dcTitle = searchResultItem.getDcTitle();

      dcCreator = searchResultItem.getDcCreator();

      dcContributor = searchResultItem.getDcContributor();
      if (searchResultItem.getDcAbstract().length() > ABSTRACT_MAX_LENGTH)
        dcAbstract = searchResultItem.getDcAbstract().substring(0, ABSTRACT_MAX_LENGTH) + "...";
      else
        dcAbstract = searchResultItem.getDcAbstract();
      dcRightsHolder = searchResultItem.getDcRightsHolder();

      // Get and format the recording date
      if (searchResultItem.getDcCreated() == null)
        dcCreated = "";
      else
        dcCreated = format.format(searchResultItem.getDcCreated());

      videoUrl = getVideoUrl(searchResultItem.getMediaPackage());

      try {
        // URLEncode the media package id
        mediaPackageId = URLEncoder.encode(mediaPackageId, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }

      // Setting fields to the episodeViewItem
      episodeViewItem = new EpisodeViewImpl();
      episodeViewItem.setDcTitle(dcTitle);
      episodeViewItem.setDcCreator(dcCreator);
      episodeViewItem.setDcContributor(dcContributor);
      episodeViewItem.setDcAbstract(dcAbstract);
      episodeViewItem.setDcRightsHolder(dcRightsHolder);
      episodeViewItem.setVideoUrl(videoUrl);

      episodeViewItem.setDcCreated(dcCreated);
      if (cover == null || cover.equals(""))
        episodeViewItem.setCover("img/thumbnail.png");
      else
        episodeViewItem.setCover(cover);
      episodeViewItem.setURLEncodedMediaPackageId(mediaPackageId);

      // Adding the episodeViewItem to the episodeViewList
      episodeViewList.add(episodeViewItem);
    }

    return Response.ok(episodeViewListResult).build();
  }

  /**
   * Iterates through the tracks of an mediaPackage and returns the video url of the last track
   * @param mediaPackage
   * @return String the video url
   */
  private String getVideoUrl(MediaPackage mediaPackage) {
//    FlavorPrioritySelector<Track> selector = new FlavorPrioritySelector<Track>();
//    selector.includeTag("engage");
//    selector.addFlavor(MediaPackageElements.PRESENTATION_TRACK);
//    selector.addFlavor(MediaPackageElements.PRESENTER_TRACK);
//    Collection<Track> c = selector.select(mediaPackage);
//    if(c.isEmpty()) return DEFAULT_VIDEO_URL;
//    return c.iterator().next().getURI().toString();
    for(Track track : mediaPackage.getTracks()) {
      for(String tag : track.getTags()) {
        if("engage".equals(tag) && (
                MediaPackageElements.PRESENTATION_TRACK.equals(track.getFlavor()) || 
                MediaPackageElements.PRESENTER_TRACK.equals(track.getFlavor()))) return track.getURI().toString();
      }
    }
    return DEFAULT_VIDEO_URL;
  }

  /**
   * @return documentation for this endpoint
   */
  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    return docs;
  }

  protected String docs;

  public EngageuiRestService() {
  }

  /**
   * The method that will be called, if the service is activated
   * 
   * @param cc
   *          The ComponentContext of this service
   */
  public void activate(ComponentContext cc) {
    docs = generateDocs();
  }

  /**
   * Generates the REST documentation
   * 
   * @return The HTML with the documentation
   */
  protected String generateDocs() {
    DocRestData data = new DocRestData("Engage", "Engage UI", "/engageui/rest", new String[] { "$Rev$" });
    // Scheduler addEvent
    RestEndpoint getEpisodesByDate = new RestEndpoint("getEpisodesByDate", RestEndpoint.Method.POST,
            "/getEpisodesByDate", "Gets the episode with the given episode id.");
    getEpisodesByDate.addFormat(new Format("xml", null, null));
    getEpisodesByDate.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, result returned"));
    getEpisodesByDate.addRequiredParam(new Param("episodeId", Type.TEXT, "123456",
            "The episode that should be requested"));
    getEpisodesByDate.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getEpisodesByDate);

    return DocUtil.generate(data);
  }
}
