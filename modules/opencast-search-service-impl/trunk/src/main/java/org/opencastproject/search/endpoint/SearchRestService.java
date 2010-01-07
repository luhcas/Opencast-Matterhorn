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
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;
import org.opencastproject.util.doc.Param.Type;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    this.docs = generateDocs();
  }
  
  protected String generateDocs() {
    DocRestData data = new DocRestData("Search", "Search Service", "/search/rest", new String[] {"$Rev$"});
    // episode
    RestEndpoint episodeEndpoint = new RestEndpoint("episode", RestEndpoint.Method.GET, "/episode", "Search for episodes matching the query parameters");
    episodeEndpoint.addFormat(new Format("xml", null, null));
    episodeEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("The search results, expressed as xml"));
    episodeEndpoint.addOptionalParam(new Param("id", Type.STRING, null, "The ID of the single episode to be returned, if it exists"));
    episodeEndpoint.addOptionalParam(new Param("seriesId", Type.STRING, null, "All episodes in the series with this ID"));
    episodeEndpoint.addOptionalParam(new Param("q", Type.STRING, null, "Any episode that matches this free-text query"));
    episodeEndpoint.addOptionalParam(new Param("limit", Type.STRING, "0", "The maximum number of items to return per page"));
    episodeEndpoint.addOptionalParam(new Param("offset", Type.STRING, "0", "The page number"));
    episodeEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, episodeEndpoint);

    // series
    RestEndpoint seriesEndpoint = new RestEndpoint("series", RestEndpoint.Method.GET, "/series", "Search for series matching the query parameters");
    seriesEndpoint.addFormat(new Format("xml", null, null));
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
    addEndpoint.addFormat(new Format("xml", null, null));
    addEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, addEndpoint);    

    return DocUtil.generate(data);
  }

  protected String generateMediaPackage() {
    return "<mediapackage start=\"2007-12-05T13:40:00\" duration=\"1004400000\">\n" +
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
    "</mediapackage>";
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
    if(!StringUtils.isEmpty(id)) {
      logger.debug("Searching for episodes by episode ID");
      return (SearchResultImpl) searchService.getEpisodeById(id);
    }
    // Then try searching by series ID
    if(!StringUtils.isEmpty(seriesId)) {
      logger.debug("Searching for episodes by series ID");
      return (SearchResultImpl) searchService.getEpisodesBySeries(seriesId);
    }
    
    // If text is specified, do a free text search.  Otherwise, just return the most recent episodes
    if(StringUtils.isEmpty(text)) {
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
