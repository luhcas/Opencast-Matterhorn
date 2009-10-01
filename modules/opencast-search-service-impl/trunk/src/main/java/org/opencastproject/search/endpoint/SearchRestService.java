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
package org.opencastproject.search.endpoint;

import org.opencastproject.media.mediapackage.MediaPackageBuilder;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.jaxb.MediapackageType;
import org.opencastproject.search.api.SearchException;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.search.impl.SearchResultImpl;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * The REST endpoint 
 */
@Path("/")
public class SearchRestService {
  private static final Logger logger = LoggerFactory.getLogger(SearchRestService.class);
  
  protected SearchService searchService;
  public void setSearchService(SearchService searchService) {
    this.searchService = searchService;
  }

  protected String docs;
  
  public SearchRestService() {
    // Pre-load the documentation
    String docsFromClassloader = null;
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream("/html/index.html");
      docsFromClassloader = IOUtils.toString(in);
    } catch (IOException e) {
      logger.error("failed to read documentation", e);
      docsFromClassloader = "unable to load documentation for " + SearchRestService.class.getName();
    } finally {
      IOUtils.closeQuietly(in);
    }
    docs = docsFromClassloader;
  }

  @GET
  @Path("docs")
  public String getDocs() {
    return docs;
  }
  
  @POST
  @Path("add")
  public void add(@FormParam("mediapackage") MediapackageType mediaPackage) throws SearchException {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    try {
      searchService.add(builder.loadFromManifest(IOUtils.toInputStream(mediaPackage.toXml())));
    } catch (Exception e) {
      throw new SearchException(e);
    }
  }

  @POST
  @Path("remove")
  public void delete(String mediaPackageId) throws SearchException {
    searchService.delete(mediaPackageId);
  }

  @GET
  @Path("series")
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_JSON})
  public SearchResultImpl getEpisodeAndSeriesById(
          @QueryParam("id") String id,
          @QueryParam("q") String text,
          @QueryParam("episodes") boolean includeEpisodes,
          @QueryParam("limit") int limit,
          @QueryParam("offset") int offset) {
    // If id is specified, do a search based on id
    if(id != null) {
      return includeEpisodes ? (SearchResultImpl) searchService.getEpisodeAndSeriesById(id) :
        (SearchResultImpl) searchService.getSeriesById(id);
    }
    // If text is specified, do a free text search.  Otherwise, just return the most recent series
    if(text == null) {
      return (SearchResultImpl) searchService.getSeriesByDate(limit, offset);
    } else {
      return (SearchResultImpl) searchService.getSeriesByText(text, offset, limit);
    }
  }

  @GET
  @Path("episode")
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_JSON})
  public SearchResultImpl getEpisode(
          @QueryParam("id") String id,
          @QueryParam("seriesId") String seriesId,
          @QueryParam("q") String text,
          @QueryParam("limit") int limit,
          @QueryParam("offset") int offset) {
    // Try searching by episode ID first
    if(id != null) {
      logger.debug("Searching for episodes by episode ID");
      return (SearchResultImpl) searchService.getEpisodeById(id);
    }
    // Then try searching by series ID
    if(seriesId != null) {
      logger.debug("Searching for episodes by series ID");
      return (SearchResultImpl) searchService.getEpisodesBySeries(seriesId);
    }
    
    // If text is specified, do a free text search.  Otherwise, just return the most recent episodes
    if(text == null) {
      logger.debug("Searching for all episodes, ordered by date");
      return (SearchResultImpl) searchService.getEpisodesByDate(limit, offset);
    } else {
      logger.debug("Searching for episodes via free text search");
      return (SearchResultImpl) searchService.getEpisodesByText(text, offset, limit);
    }
  }

  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_JSON})
  public SearchResultImpl getEpisodesAndSeries(
          @QueryParam("q") String text,
          @QueryParam("limit") int limit,
          @QueryParam("offset") int offset) {
    return (SearchResultImpl) searchService.getEpisodesAndSeriesByText(text, offset, limit);
  }
}
