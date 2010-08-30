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
package org.opencastproject.search.endpoint;

import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageImpl;
import org.opencastproject.search.api.SearchException;
import org.opencastproject.search.api.SearchResultImpl;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.search.impl.SearchQueryImpl;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;
import org.opencastproject.util.doc.Param.Type;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    if (docs == null) { docs = generateDocs(); }
    return docs;
  }

  protected String docs;
  private String[] notes = {
    "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
    "If the service is down or not working it will return a status 503, this means the the underlying service is not working and is either restarting or has failed",
    "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In other words, there is a bug! You should file an error report with your server logs from the time when the error occurred: <a href=\"https://issues.opencastproject.org\">Opencast Issue Tracker</a>", };
  
  protected String generateDocs() {
    DocRestData data = new DocRestData("Search", "Search Service", "/search/rest", notes);

    // abstract
    data.setAbstract("This service indexes and queries available (distributed) episodes.");
    // episode
    RestEndpoint episodeEndpoint = new RestEndpoint("episode", RestEndpoint.Method.GET, "/episode{format}", "Search for episodes matching the query parameters");
    episodeEndpoint.addFormat(new Format("XML", null, null));
    episodeEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("The search results"));
    episodeEndpoint.addPathParam(new Param("format", Type.STRING, ".xml", "The output format (.xml or .json).  Defaults to xml."));
    episodeEndpoint.addOptionalParam(new Param("id", Type.STRING, null, "The ID of the single episode to be returned, if it exists"));
    episodeEndpoint.addOptionalParam(new Param("q", Type.STRING, null, "Any episode that matches this free-text query"));
    episodeEndpoint.addOptionalParam(new Param("limit", Type.STRING, "0", "The maximum number of items to return per page"));
    episodeEndpoint.addOptionalParam(new Param("offset", Type.STRING, "0", "The page number"));
    episodeEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, episodeEndpoint);

    // series
    RestEndpoint seriesEndpoint = new RestEndpoint("series", RestEndpoint.Method.GET, "/series{format}", "Search for series matching the query parameters");
    seriesEndpoint.addFormat(new Format("XML", null, null));
    seriesEndpoint.addFormat(new Format("JSON", null, null));
    seriesEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("The search results"));
    seriesEndpoint.addPathParam(new Param("format", Type.STRING, ".xml", "The output format (.xml or .json).  Defaults to xml."));
    seriesEndpoint.addOptionalParam(new Param("id", Type.STRING, null, "The series ID. This takes the additional boolean \"episodes\" parameter. If true, the result set will include this series episodes."));
    seriesEndpoint.addOptionalParam(new Param("q", Type.STRING, null, "Any series that matches this free-text query. This takes the additional boolean \"episodes\" parameter. If true, the result set will include this series episodes."));
    seriesEndpoint.addOptionalParam(new Param("episodes", Type.BOOLEAN, "false", "Whether to include this series episodes.  This can be used in combination with id or q"));
    seriesEndpoint.addOptionalParam(new Param("series", Type.BOOLEAN, "false", "Whether to include this series information itself.  This can be used in combination with id or q"));
    seriesEndpoint.addOptionalParam(new Param("limit", Type.STRING, "0", "The maximum number of items to return per page"));
    seriesEndpoint.addOptionalParam(new Param("offset", Type.STRING, "0", "The page number"));
    seriesEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, seriesEndpoint);

    // episode and series
    RestEndpoint episodeAndSeriesEndpoint = new RestEndpoint("episodeAndSeries", RestEndpoint.Method.GET, "/", "Search for episodes and series matching the query parameters");
    episodeAndSeriesEndpoint.addFormat(new Format("XML", null, null));
    episodeAndSeriesEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("The search results, expressed as xml"));
    episodeAndSeriesEndpoint.addOptionalParam(new Param("q", Type.STRING, null, "Any episode or series that matches this free-text query."));
    episodeAndSeriesEndpoint.addOptionalParam(new Param("limit", Type.STRING, "0", "The maximum number of items to return per page"));
    episodeAndSeriesEndpoint.addOptionalParam(new Param("offset", Type.STRING, "0", "The page number"));
    episodeAndSeriesEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, episodeAndSeriesEndpoint);
    
    // remove
    RestEndpoint removeEndpoint = new RestEndpoint("remove", RestEndpoint.Method.DELETE, "/{id}", "Removes a mediapackage from the search index");
    removeEndpoint.addStatus(org.opencastproject.util.doc.Status.NO_CONTENT("The mediapackage was removed, no content to return"));
    removeEndpoint.addPathParam(new Param("id", Type.STRING, "", "The media package ID to remove from the search index"));
    removeEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, removeEndpoint);    
    
    // add
    RestEndpoint addEndpoint = new RestEndpoint("add", RestEndpoint.Method.POST, "/add", "Adds a mediapackage to the search index");
    addEndpoint.addStatus(org.opencastproject.util.doc.Status.NO_CONTENT("The mediapackage was added, no content to return"));
    addEndpoint.addRequiredParam(new Param("mediapackage", Type.TEXT, generateMediaPackage(), "The media package to add to the search index"));
    addEndpoint.addFormat(new Format("XML", null, null));
    addEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, addEndpoint);    

    // clear
    RestEndpoint clearEndpoint = new RestEndpoint("clear", RestEndpoint.Method.POST, "/clear", "Clears the entire search index");
    clearEndpoint.addStatus(org.opencastproject.util.doc.Status.NO_CONTENT("The search index was cleared, no content to return"));
    clearEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, clearEndpoint);    

    logger.debug("generated documentation for {}", data);
    
    return DocUtil.generate(data);
  }

  protected String generateMediaPackage() {
    return "<ns2:mediapackage xmlns:ns2=\"http://mediapackage.opencastproject.org\" start=\"2007-12-05T13:40:00\" duration=\"1004400000\">\n" +
    "  <metadata>\n" +
    "    <catalog id=\"catalog-1\" type=\"dublincore/episode\">\n" +
    "      <mimetype>text/xml</mimetype>\n" +
    "      <url>https://opencast.jira.com/svn/MH/trunk/modules/matterhorn-kernel/src/test/resources/dublincore.xml</url>\n" +
    "      <checksum type=\"md5\">2b8a52878c536e64e20e309b5d7c1070</checksum>\n" +
    "    </catalog>\n" +
    "    <catalog id=\"catalog-3\" type=\"metadata/mpeg-7\" ref=\"track:track-1\">\n" +
    "      <mimetype>text/xml</mimetype>\n" +
    "      <url>https://opencast.jira.com/svn/MH/trunk/modules/matterhorn-kernel/src/test/resources/mpeg7.xml</url>\n" +
    "      <checksum type=\"md5\">2b8a52878c536e64e20e309b5d7c1070</checksum>\n" +
    "    </catalog>\n" +
    "  </metadata>\n" +
    "</ns2:mediapackage>";
  }
  
  @POST
  @Path("add")
  public Response add(@FormParam("mediapackage") MediaPackageImpl mediaPackage) throws SearchException {
    try {
      searchService.add(mediaPackage);
      return Response.noContent().build();
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      return Response.serverError().build();
    }
  }

  @DELETE
  @Path("{id}")
  public Response remove(@PathParam("id") String mediaPackageId) throws SearchException {
    try {
      searchService.delete(mediaPackageId);
      return Response.noContent().build();
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      return Response.serverError().build();
    }
  }

  @POST
  @Path("clear")
  public Response clear() throws SearchException {
    try {
      searchService.clear();
      return Response.noContent().build();
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      return Response.serverError().build();
    }
  }
  
  @GET
  @Path("series.json")
  @Produces(MediaType.APPLICATION_JSON)
  public SearchResultImpl getEpisodeAndSeriesByIdAsJson(
          @QueryParam("id") String id,
          @QueryParam("q") String text,
          @QueryParam("episodes") boolean includeEpisodes,
          @QueryParam("series") boolean includeSeries,
          @QueryParam("limit") int limit,
          @QueryParam("offset") int offset) {
    return getEpisodeAndSeriesById(id, text, includeEpisodes, includeSeries, limit, offset);
  }

  @GET
  @Path("series.xml")
  @Produces(MediaType.APPLICATION_XML)
  public SearchResultImpl getEpisodeAndSeriesById(
          @QueryParam("id") String id,
          @QueryParam("q") String text,
          @QueryParam("episodes") boolean includeEpisodes,
          @QueryParam("series") boolean includeSeries,
          @QueryParam("limit") int limit,
          @QueryParam("offset") int offset) {
    SearchQueryImpl query = new SearchQueryImpl();
    
    // If id is specified, do a search based on id
    if (!StringUtils.isBlank(id)) {
      query.withId(id);
    }
    
    // Include series data in the results?
    query.includeSeries(includeSeries);

    // Include episodes in the result?
    query.includeEpisodes(includeEpisodes);

    // Include free-text search?
    if (!StringUtils.isBlank(text)) {
      query.withText(text);
    }
    
    query.withPublicationDateSort(true);
    query.withLimit(limit);
    query.withOffset(offset);
    return (SearchResultImpl) searchService.getByQuery(query);
  }

  @GET
  @Path("episode.json")
  @Produces(MediaType.APPLICATION_JSON)
  public SearchResultImpl getEpisodeAsJson(
          @QueryParam("id") String id,
          @QueryParam("q") String text,
          @QueryParam("tag") String[] tags,
          @QueryParam("flavor") String[] flavors,
          @QueryParam("limit") int limit,
          @QueryParam("offset") int offset) {
    return getEpisode(id, text, tags, flavors, limit, offset);
  }
  
  @GET
  @Path("episode.xml")
  @Produces(MediaType.APPLICATION_XML)
  public SearchResultImpl getEpisode(
          @QueryParam("id") String id,
          @QueryParam("q") String text,
          @QueryParam("tag") String[] tags,
          @QueryParam("flavor") String[] flavors,
          @QueryParam("limit") int limit,
          @QueryParam("offset") int offset) {

    // Prepare the flavors
    List<MediaPackageElementFlavor> flavorSet = new ArrayList<MediaPackageElementFlavor>();
    if (flavors != null) {
      for (String f : flavors) {
        try {
          flavorSet.add(MediaPackageElementFlavor.parseFlavor(f));
        } catch(IllegalArgumentException e) {
          logger.debug("invalid flavor '{}' specified in query", f);
        }
      }
    }
    
    SearchQueryImpl search = new SearchQueryImpl();
    search.withId(id).withElementFlavors(flavorSet.toArray(new MediaPackageElementFlavor[flavorSet.size()])).
    withElementTags(tags).withLimit(limit).withOffset(offset);
    if (!StringUtils.isBlank(text))
      search.withText(text);
    else
      search.withPublicationDateSort(true);
    return (SearchResultImpl)searchService.getByQuery(search);
  }

  @GET
  @Produces(MediaType.APPLICATION_XML)
  public SearchResultImpl getEpisodesAndSeries(
          @QueryParam("q") String text,
          @QueryParam("limit") int limit,
          @QueryParam("offset") int offset) {
    SearchQueryImpl query = new SearchQueryImpl();
    query.includeEpisodes(true);
    query.includeSeries(true);
    query.withLimit(limit);
    query.withOffset(offset);
    if (!StringUtils.isBlank(text))
      query.withText(text);
    else
      query.withPublicationDateSort(true);
    return (SearchResultImpl) searchService.getByQuery(query);
  }
  
  @GET
  @Path("lucene.xml")
  @Produces(MediaType.APPLICATION_XML)
  public SearchResultImpl getByLuceneQuery(
          @QueryParam("q") String q,
          @QueryParam("limit") int limit,
          @QueryParam("offset") int offset) {
    SearchQueryImpl query = new SearchQueryImpl();
    if (!StringUtils.isBlank(q))
      query.withQuery(q);
    else
      query.withPublicationDateSort(true);
    query.withLimit(limit);
    query.withOffset(offset);
    return (SearchResultImpl) searchService.getByQuery(query);
  }

  @GET
  @Path("lucene.json")
  @Produces(MediaType.APPLICATION_JSON)
  public SearchResultImpl getByLuceneQueryAsJson(
          @QueryParam("q") String q,
          @QueryParam("limit") int limit,
          @QueryParam("offset") int offset) {
    return getByLuceneQuery(q, limit, offset);
  }

}
