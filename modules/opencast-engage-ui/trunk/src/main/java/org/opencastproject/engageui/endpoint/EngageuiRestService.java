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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.opencastproject.engageui.api.EpisodeView;
import org.opencastproject.engageui.api.EpisodeViewImpl;
import org.opencastproject.engageui.api.EpisodeViewListImpl;
import org.opencastproject.engageui.api.EpisodeViewListResultImpl;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchResultItem;
import org.opencastproject.search.api.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST endpoint for the Engage UI proxy service
 */
@Path("/")
public class EngageuiRestService {
  private static final Logger logger = LoggerFactory.getLogger(EngageuiRestService.class);

  private SearchService searchService;

  public static final int TITLE_MAX_LENGTH = 60;
  public static final int ABSTRACT_MAX_LENGTH = 175;

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
  @Path("search")
  public EpisodeViewListResultImpl getEpisodesByDate(@QueryParam("q") String text, @QueryParam("page") int page) {
    // Variables
    int pagemax, fromIndex, toIndex;
    long episodesMax;
    String mediaPackageId, dcTitle, dcCreator, dcContributor, dcAbstract, cover, dcCreated, dcRightsHolder;
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
      return episodeViewListResult;
    }

    if (StringUtils.isEmpty(text)) {
      // Get episodes by date from the search service
      result = searchService.getEpisodesByDate((page - 1) * 10, 10);
    }
    else{
      result = searchService.getEpisodesByText(text, (page - 1) * 10, 10);
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
    episodeViewListResult.setFromIndex(fromIndex + 1);

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

      episodeViewItem.setDcCreated(dcCreated);
      if (cover == null || cover.equals(""))
        episodeViewItem.setCover("img/thumbnail.png");
      else
        episodeViewItem.setCover(cover);
      episodeViewItem.setURLEncodedMediaPackageId(mediaPackageId);

      // Adding the episodeViewItem to the episodeViewList
      episodeViewList.add(episodeViewItem);
    }

    return episodeViewListResult;
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

  protected final String docs;

  public EngageuiRestService() {
    docs = "FIXME -- add documentation";
  }
}
