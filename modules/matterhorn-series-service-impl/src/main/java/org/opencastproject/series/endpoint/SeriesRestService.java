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
package org.opencastproject.series.endpoint;

import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesQuery;
import org.opencastproject.series.api.SeriesResult;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.SolrUtils;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * REST endpoint for Series Service.
 * 
 */
@Path("/")
public class SeriesRestService {
  /** Logging utility */
  private static final Logger logger = LoggerFactory.getLogger(SeriesRestService.class);
  /** Series Service */
  private SeriesService seriesService;
  /** Dublin Core Catalog service */
  private DublinCoreCatalogService dcService;

  /** Default server URL */
  protected String serverUrl = "http://localhost:8080";
  /** Default number of items on page */
  private static final int DEFAULT_LIMIT = 20;
  /** Maximum number of items on page */
  private static final int MAX_LIMIT = 100;
  /** Suffix to mark descending ordering of results */
  public static final String DESCENDING_SUFFIX = "_DESC";

  /** REST documentation */
  protected String docs;
  private String[] notes = {
          "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
          "If the service is down or not working it will return a status 503, this means the the underlying service is not working and is either restarting or has failed",
          "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In other words, there is a bug! You should file an error report with your server logs from the time when the error occurred: <a href=\"https://issues.opencastproject.org\">Opencast Issue Tracker</a>" };

  /**
   * OSGi callback for setting series service.
   * 
   * @param seriesService
   */
  public void setService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  /**
   * OSGi callback for setting Dublin Core Catalog service.
   * 
   * @param dcService
   */
  public void setDublinCoreService(DublinCoreCatalogService dcService) {
    this.dcService = dcService;
  }

  /**
   * Activates REST service.
   * 
   * @param cc
   *          ComponentContext
   */
  public void activate(ComponentContext cc) {
    if (cc == null) {
      this.serverUrl = "http://localhost:8080";
    } else {
      String ccServerUrl = cc.getBundleContext().getProperty("org.opencastproject.server.url");
      logger.debug("Configured server url is {}", ccServerUrl);
      if (ccServerUrl == null)
        this.serverUrl = "http://localhost:8080";
      else {
        this.serverUrl = ccServerUrl;
      }
      String serviceUrl = (String) cc.getProperties().get("opencast.service.path");
      this.docs = generateDocs(serviceUrl);
    }
  }

  @GET
  @Produces({ "text/xml" })
  @Path("{seriesID}.xml")
  public Response getSeriesXml(@PathParam("seriesID") String seriesID) {
    logger.debug("Series Lookup: {}", seriesID);
    try {
      DublinCoreCatalog dc = this.seriesService.getSeries(seriesID);
      String dcXML = serializeDublinCore(dc);
      return Response.ok(dcXML).build();
    } catch (NotFoundException e) {
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (Exception e) {
    }
    throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
  }

  /**
   * Serializes Dublin Core and returns is as string.
   * 
   * @param dc
   *          {@link DublinCoreCatalog} to be serialized.
   * @return String representation of Dublin core
   * @throws IOException
   *           if serialization fails
   */
  private String serializeDublinCore(DublinCoreCatalog dc) throws IOException {
    InputStream in = this.dcService.serialize(dc);

    StringWriter writer = new StringWriter();
    IOUtils.copy(in, writer, "UTF-8");

    return writer.toString();
  }

  @POST
  @Path("/")
  public Response addOrUpdateSeries(@FormParam("series") String series) {
    if (series == null) {
      logger.warn("series that should be added is null");
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
    DublinCoreCatalog dc;
    try {
      dc = this.dcService.load(new ByteArrayInputStream(series.getBytes("UTF-8")));
    } catch (UnsupportedEncodingException e1) {
      logger.error("Could not deserialize dublin core catalog: {}", e1);
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    } catch (IOException e1) {
      logger.warn("Could not deserialize dublin core catalog: {}", e1);
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
    try {
      this.seriesService.updateSeries(dc);
      logger.debug("Added series {} ", dc.getFirst(DublinCore.PROPERTY_IDENTIFIER));
      return Response.status(Response.Status.OK).build();
    } catch (SeriesException e) {
    }
    throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
  }

  @DELETE
  @Path("/{seriesID}")
  public Response deleteSeries(@PathParam("seriesID") String seriesID) {
    try {
      this.seriesService.deleteSeries(seriesID);
      return Response.ok().build();
    } catch (NotFoundException e) {
      logger.warn(e.getMessage());
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (SeriesException se) {
    }
    throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
  }

  @GET
  @Produces({ "text/xml" })
  @Path("series.xml")
  // CHECKSTYLE:OFF
  public Response getWorkflowsAsXml(@QueryParam("q") String text, @QueryParam("seriesId") String seriesId,
          @QueryParam("seriesTitle") String seriesTitle, @QueryParam("creator") String creator,
          @QueryParam("contributor") String contributor, @QueryParam("publisher") String publisher,
          @QueryParam("rightsholder") String rightsHolder, @QueryParam("createdfrom") String createdFrom,
          @QueryParam("createdto") String createdTo, @QueryParam("language") String language,
          @QueryParam("license") String license, @QueryParam("subject") String subject,
          @QueryParam("abstract") String seriesAbstract, @QueryParam("description") String description,
          @QueryParam("sort") String sort, @QueryParam("startPage") int startPage, @QueryParam("count") int count) {
  // CHECKSTYLE:ON  
    if ((count < 1) || (count > MAX_LIMIT)) {
      count = DEFAULT_LIMIT;
    }
    if (startPage < 0) {
      startPage = 0;
    }

    SeriesQuery q = new SeriesQuery();
    q.setCount(count);
    q.setStartPage(startPage);
    q.setText(text.toLowerCase());
    q.setSeriesId(seriesId.toLowerCase());
    q.setSeriesTitle(seriesTitle.toLowerCase());
    q.setCreator(creator.toLowerCase());
    q.setContributor(contributor.toLowerCase());
    q.setLanguage(language.toLowerCase());
    q.setLicense(license.toLowerCase());
    q.setSubject(subject.toLowerCase());
    q.setPublisher(publisher.toLowerCase());
    q.setSeriesAbstract(seriesAbstract.toLowerCase());
    q.setDescription(description.toLowerCase());
    q.setRightsHolder(rightsHolder.toLowerCase());
    try {
      q.setCreatedFrom(SolrUtils.parseDate(createdFrom));
      q.setCreatedTo(SolrUtils.parseDate(createdTo));
    } catch (ParseException e1) {
      logger.warn("Could not parse date parameter: {}", e1);
    }

    if (StringUtils.isNotBlank(sort)) {
      SeriesQuery.Sort sortField = null;
      if (sort.endsWith("_DESC")) {
        String enumKey = sort.substring(0, sort.length() - "_DESC".length()).toUpperCase();
        try {
          sortField = SeriesQuery.Sort.valueOf(enumKey);
          q.withSort(sortField, false);
        } catch (IllegalArgumentException e) {
          logger.warn("No sort enum matches '{}'", enumKey);
        }
      } else {
        try {
          sortField = SeriesQuery.Sort.valueOf(sort);
          q.withSort(sortField);
        } catch (IllegalArgumentException e) {
          logger.warn("No sort enum matches '{}'", sort);
        }
      }
    }
    SeriesResult result;
    try {
      result = this.seriesService.getSeries(q);
    } catch (SeriesException e) {
      logger.error(e.getMessage());
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
    return Response.ok(result).build();
  }

  @GET
  @Produces({ "text/html" })
  @Path("docs")
  public String getDocumentation() {
    return this.docs;
  }

  /**
   * Generate REST Docs.
   * 
   * @param serviceUrl
   * @return
   */
  protected String generateDocs(String serviceUrl) {
    DocRestData data = new DocRestData("Series", "Series Service", serviceUrl, this.notes);

    data.setAbstract("This service creates, edits and retrieves and helps manage series.");

    // add or update
    RestEndpoint updateEndpoint = new RestEndpoint(
            "addOrUpdateSeries",
            RestEndpoint.Method.POST,
            "/",
            "Accepts an XML form parameter representing a new Series as Dublin Core document and stores it in the database. Returns HTTP Status 201 (Created) if successful. 400 (Bad Request) if the no series is supplied. 500 (Internal Server Error) if there was an error creating the series.");
    updateEndpoint.addStatus(org.opencastproject.util.doc.Status.created("Series was created successfully."));
    updateEndpoint.addStatus(org.opencastproject.util.doc.Status
            .badRequest("No series was supplied or invalid Dublin Core."));

    updateEndpoint.addStatus(org.opencastproject.util.doc.Status.error("The series was not created successfully."));
    updateEndpoint.addRequiredParam(new Param("series", Param.Type.TEXT, generateDublinCore(),
            "The XML representation of the series to be stored."));
    updateEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, updateEndpoint);

    // remove
    RestEndpoint removeEndpoint = new RestEndpoint("deleteSeries", RestEndpoint.Method.DELETE, "/{seriesId}",
            "Removes the specified series from the database. Returns true if the series could be removed.");
    removeEndpoint.addStatus(org.opencastproject.util.doc.Status.ok("Series has been successfully deleted."));
    removeEndpoint.addStatus(org.opencastproject.util.doc.Status
            .notFound("A Series matching the supplied seriesId was not found."));
    removeEndpoint.addPathParam(new Param("seriesId", Param.Type.STRING, "10.0000/5819", "Identifier of the series."));
    removeEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, removeEndpoint);

    // retrieve
    RestEndpoint getEndpoint = new RestEndpoint("getSeries", RestEndpoint.Method.GET, "/{seriesId}.xml",
            "Get a specific Series.");
    getEndpoint.addFormat(Format.xml("XML Representation of a series."));
    getEndpoint.addStatus(org.opencastproject.util.doc.Status
            .ok("Series found and response contains XML representation of the series."));
    getEndpoint.addStatus(org.opencastproject.util.doc.Status
            .noContent("A Series matching the supplied seriesId was not found."));
    getEndpoint.addPathParam(new Param("seriesId", Param.Type.STRING, "10.0000/5819", "Identifier of the Series."));
    getEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getEndpoint);

    // search
    RestEndpoint searchEndpoint = new RestEndpoint("search", RestEndpoint.Method.GET, "/series.xml",
            "List all series matching the query parameters");
    searchEndpoint.addFormat(new Format("xml", null, null));
    searchEndpoint.addStatus(org.opencastproject.util.doc.Status.ok("Valid request, results returned"));
    searchEndpoint.addOptionalParam(new Param("q", Param.Type.STRING, null,
            "Filter results by string in metadata catalog"));
    searchEndpoint.addOptionalParam(new Param("seriesId", Param.Type.STRING, null, "Filter results by series ID"));
    searchEndpoint
            .addOptionalParam(new Param("seriesTitle", Param.Type.STRING, null, "Filter results by series title"));
    searchEndpoint.addOptionalParam(new Param("title", Param.Type.STRING, null, "Filter results by title"));
    searchEndpoint.addOptionalParam(new Param("creator", Param.Type.STRING, null, "Filter results by creator"));
    searchEndpoint.addOptionalParam(new Param("contributor", Param.Type.STRING, null, "Filter results by contributor"));
    searchEndpoint.addOptionalParam(new Param("publisher", Param.Type.STRING, null, "Filter results by publisher"));
    searchEndpoint.addOptionalParam(new Param("rightsholder", Param.Type.STRING, null,
            "Filter results by rights holder"));
    searchEndpoint.addOptionalParam(new Param("language", Param.Type.STRING, null, "Filter results by language"));
    searchEndpoint.addOptionalParam(new Param("license", Param.Type.STRING, null, "Filter results by license"));
    searchEndpoint.addOptionalParam(new Param("subject", Param.Type.STRING, null, "Filter results by subject"));
    searchEndpoint.addOptionalParam(new Param("abstract", Param.Type.STRING, null, "Filter results by abstract"));
    searchEndpoint.addOptionalParam(new Param("description", Param.Type.STRING, null, "Filter results by description"));
    searchEndpoint.addOptionalParam(new Param("createdfrom", Param.Type.STRING, null,
            "Filter results by created from (yyyy-MM-dd'T'HH:mm:ss'Z')"));
    searchEndpoint.addOptionalParam(new Param("createdto", Param.Type.STRING, null,
            "Filter results by created to (yyyy-MM-dd'T'HH:mm:ss'Z')"));
    searchEndpoint.addOptionalParam(new Param("count", Param.Type.STRING, "20", "Results per page (max 100)"));
    searchEndpoint.addOptionalParam(new Param("startPage", Param.Type.STRING, "0", "Page offset"));
    searchEndpoint
            .addOptionalParam(new Param(
                    "sort",
                    Param.Type.STRING,
                    "CREATED",
                    "The sort order.  May include any of the following: TITLE, SUBJECT, CREATOR, PUBLISHER, CONTRIBUTOR, ABSTRACT, DESCRIPTION, CREATED, AVAILABLE_FROM, AVAILABLE_TO, LANGUAGE, RIGHTS_HOLDER, SPATIAL, TEMPORAL, IS_PART_OF, REPLACES, TYPE, ACCESS, LICENCE.  Add '_DESC' to reverse the sort order (e.g. TITLE_DESC)."));
    searchEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, searchEndpoint);

    return DocUtil.generate(data);
  }

  /**
   * Generates sample Dublin core.
   * 
   * @return sample Dublin core
   */
  private String generateDublinCore() {
    return "<?xml version=\"1.0\"?>\n<dublincore xmlns=\"http://www.opencastproject.org/xsd/1.0/dublincore/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance/\"\n  xsi:schemaLocation=\"http://www.opencastproject.org http://www.opencastproject.org/schema.xsd\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n  xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:oc=\"http://www.opencastproject.org/matterhorn\">\n\n  <dcterms:title xml:lang=\"en\">\n    Land and Vegetation: Key players on the Climate Scene\n    </dcterms:title>\n  <dcterms:subject>\n    climate, land, vegetation\n    </dcterms:subject>\n  <dcterms:description xml:lang=\"en\">\n    Introduction lecture from the Institute for\n    Atmospheric and Climate Science.\n    </dcterms:description>\n  <dcterms:publisher>\n    ETH Zurich, Switzerland\n    </dcterms:publisher>\n  <dcterms:identifier>\n    10.0000/5819\n    </dcterms:identifier>\n  <dcterms:modified xsi:type=\"dcterms:W3CDTF\">\n    2007-12-05\n    </dcterms:modified>\n  <dcterms:format xsi:type=\"dcterms:IMT\">\n    video/x-dv\n    </dcterms:format>\n  <oc:promoted>\n    true\n  </oc:promoted>\n</dublincore>";
  }
}
