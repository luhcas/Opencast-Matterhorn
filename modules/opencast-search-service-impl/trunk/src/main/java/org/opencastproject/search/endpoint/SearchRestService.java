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

import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.search.api.SearchException;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.search.impl.SearchResultImpl;

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
  protected SearchService searchService;
  public void setSearchService(SearchService searchService) {
    this.searchService = searchService;
  }

  @POST
  @Path("add")
  public void add(@FormParam("mediapackage") MediaPackage mediaPackage) throws SearchException {
    searchService.add(mediaPackage);
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
      return (SearchResultImpl) searchService.getEpisodeById(id);
    }
    // Then try searching by series ID
    if(seriesId != null) {
      return (SearchResultImpl) searchService.getEpisodesBySeries(seriesId);
    }
    
    // If text is specified, do a free text search.  Otherwise, just return the most recent episodes
    if(text == null) {
      return (SearchResultImpl) searchService.getEpisodesByDate(limit, offset);
    } else {
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
