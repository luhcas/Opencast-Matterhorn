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

import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.MediaPackageImpl;
import org.opencastproject.search.api.SearchException;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.search.impl.SearchQueryImpl;
import org.opencastproject.search.impl.SearchResultImpl;
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
    // episode
    RestEndpoint episodeEndpoint = new RestEndpoint("episode", RestEndpoint.Method.GET, "/episode", "Search for episodes matching the query parameters");
    episodeEndpoint.addFormat(new Format("XML", null, null));
    episodeEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("The search results, expressed as xml"));
    episodeEndpoint.addOptionalParam(new Param("id", Type.STRING, null, "The ID of the single episode to be returned, if it exists"));
    episodeEndpoint.addOptionalParam(new Param("q", Type.STRING, null, "Any episode that matches this free-text query"));
    episodeEndpoint.addOptionalParam(new Param("limit", Type.STRING, "0", "The maximum number of items to return per page"));
    episodeEndpoint.addOptionalParam(new Param("offset", Type.STRING, "0", "The page number"));
    episodeEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, episodeEndpoint);

    // series
    RestEndpoint seriesEndpoint = new RestEndpoint("series", RestEndpoint.Method.GET, "/series", "Search for series matching the query parameters");
    seriesEndpoint.addFormat(new Format("XML", null, null));
    seriesEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("The search results, expressed as xml"));
    seriesEndpoint.addOptionalParam(new Param("id", Type.STRING, null, "The series ID. This takes the additional boolean \"episodes\" parameter. If true, the result set will include this series episodes."));
    seriesEndpoint.addOptionalParam(new Param("q", Type.STRING, null, "Any series that matches this free-text query. This takes the additional boolean \"episodes\" parameter. If true, the result set will include this series episodes."));
    seriesEndpoint.addOptionalParam(new Param("episodes", Type.BOOLEAN, null, "Whether to include this series episodes.  This can be used in combination with id or q"));
    seriesEndpoint.addOptionalParam(new Param("limit", Type.STRING, "0", "The maximum number of items to return per page"));
    seriesEndpoint.addOptionalParam(new Param("offset", Type.STRING, "0", "The page number"));
    seriesEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, seriesEndpoint);

    // add
    RestEndpoint addEndpoint = new RestEndpoint("add", RestEndpoint.Method.POST, "/add", "Adds a mediapackage to the search index");
    addEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("Results in an xml document containing the search results"));
    addEndpoint.addRequiredParam(new Param("mediapackage", Type.TEXT, generateMediaPackage(), "The media package to add to the search index"));
    addEndpoint.addFormat(new Format("XML", null, null));
    addEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, addEndpoint);    

    return DocUtil.generate(data);
  }

  protected String generateMediaPackage() {
    return "<ns2:mediapackage xmlns:ns2=\"http://mediapackage.opencastproject.org\" start=\"2007-12-05T13:40:00\" duration=\"1004400000\">\n" +
    "  <metadata>\n" +
    "    <catalog id=\"catalog-1\" type=\"metadata/dublincore\">\n" +
    "      <mimetype>text/xml</mimetype>\n" +
    "      <url>http://source.opencastproject.org/svn/modules/opencast-media/trunk/src/test/resources/dublincore.xml</url>\n" +
    "      <checksum type=\"md5\">2b8a52878c536e64e20e309b5d7c1070</checksum>\n" +
    "    </catalog>\n" +
    "    <catalog id=\"catalog-3\" type=\"metadata/mpeg-7\" ref=\"track:track-1\">\n" +
    "      <mimetype>text/xml</mimetype>\n" +
    "      <url>http://source.opencastproject.org/svn/modules/opencast-media/trunk/src/test/resources/mpeg7.xml</url>\n" +
    "      <checksum type=\"md5\">2b8a52878c536e64e20e309b5d7c1070</checksum>\n" +
    "    </catalog>\n" +
    "  </metadata>\n" +
    "</ns2:mediapackage>";
  }
  
  @POST
  @Path("add")
  public void add(@FormParam("mediapackage") MediaPackageImpl mediaPackage) throws SearchException {
    searchService.add(mediaPackage);
  }

  @POST
  @Path("remove")
  public void delete(@QueryParam("id") String mediaPackageId) throws SearchException {
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
    if(!StringUtils.isEmpty(id)) {
      return includeEpisodes ? (SearchResultImpl) searchService.getEpisodeAndSeriesById(id) :
        (SearchResultImpl) searchService.getSeriesById(id);
    }
    // If text is specified, do a free text search.  Otherwise, just return the most recent series
    if(StringUtils.isEmpty(text)) {
      return (SearchResultImpl) searchService.getSeriesByDate(limit, offset);
    } else {
      return (SearchResultImpl) searchService.getSeriesByText(text, limit, offset);
    }
  }

  @GET
  @Path("episode")
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_JSON})
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
        flavorSet.add(MediaPackageElementFlavor.parseFlavor(f));
      }
    }
    
    SearchQueryImpl search = new SearchQueryImpl();
    search.withId(id).withElementFlavors(flavorSet.toArray(new MediaPackageElementFlavor[flavorSet.size()])).
    withText(text).withElementTags(tags).withLimit(limit).withOffset(offset);
    return (SearchResultImpl)searchService.getByQuery(search);
  }

  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_JSON})
  public SearchResultImpl getEpisodesAndSeries(
          @QueryParam("q") String text,
          @QueryParam("limit") int limit,
          @QueryParam("offset") int offset) {
    return (SearchResultImpl) searchService.getEpisodesAndSeriesByText(text, limit, offset);
  }
}
