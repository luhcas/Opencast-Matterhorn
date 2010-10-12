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

import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.rest.RestPublisher;
import org.opencastproject.series.api.Series;
import org.opencastproject.series.api.SeriesMetadata;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.series.impl.SeriesImpl;
import org.opencastproject.series.impl.SeriesListImpl;
import org.opencastproject.series.impl.SeriesMetadataImpl;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.Param.Type;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * REST Endpoint for Scheduler Service
 */
@Path("/")
public class SeriesRestService {
  private static final Logger logger = LoggerFactory.getLogger(SeriesRestService.class);
  private SeriesService service;

  protected String serverUrl = UrlSupport.DEFAULT_BASE_URL;

  /**
   * Method to set the service this REST endpoint uses
   * 
   * @param service
   */
  public void setService(SeriesService service) {
    this.service = service;
  }

  /**
   * Method to unset the service this REST endpoint uses
   * 
   * @param service
   */
  public void unsetService(SeriesService service) {
    this.service = null;
  }

  /**
   * The method that will be called, if the service will be activated
   * 
   * @param cc
   *          The ComponentContext of this service
   */
  public void activate(ComponentContext cc) {
    // Get the configured server URL
    if (cc == null) {
      serverUrl = UrlSupport.DEFAULT_BASE_URL;
    } else {
      String ccServerUrl = cc.getBundleContext().getProperty("org.opencastproject.server.url");
      logger.info("configured server url is {}", ccServerUrl);
      if (ccServerUrl == null) {
        serverUrl = UrlSupport.DEFAULT_BASE_URL;
      } else {
        serverUrl = ccServerUrl;
      }
      String serviceUrl = (String) cc.getProperties().get(RestPublisher.SERVICE_PATH_PROPERTY);
      docs = generateDocs(serviceUrl);
    }
  }

  /**
   * Get a specific series.
   * 
   * @param seriesID
   *          The unique ID of the series.
   * @return series XML with the data of the series
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("{seriesID}.xml")
  public SeriesImpl getSeriesXml(@PathParam("seriesID") String seriesID) {
    logger.debug("Series Lookup: {}", seriesID);
    try {
      return (SeriesImpl) service.getSeries(seriesID);
    } catch (NotFoundException e) {
      logger.debug("Could not find series: {}", e);
      return null;
      // TODO: What do I do here?
    }
    /*
     * try { Series s = service.getSeries(seriesID); if (s == null) { return
     * Response.status(Status.BAD_REQUEST).build(); } return Response.ok(s).build(); } catch (Exception e) {
     * logger.warn("Series Lookup failed: {}", seriesID); return Response.status(Status.SERVICE_UNAVAILABLE).build(); }
     */
  }

  /**
   * Get a specific series.
   * 
   * @param seriesID
   *          The unique ID of the series.
   * @return series JSON with the data of the series
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{seriesID}.json")
  public SeriesImpl getSeriesJson(@PathParam("seriesID") String seriesID) {
    logger.debug("Series Lookup: {}", seriesID);
    try {
      return (SeriesImpl) service.getSeries(seriesID);
    } catch (NotFoundException e) {
      logger.debug("Could not find series: {}", e);
      return null;
      // TODO: What do I do here?
    }
    /*
     * try { Series s = service.getSeries(seriesID); if (s == null) { return
     * Response.status(Status.BAD_REQUEST).build(); } return Response.ok(s).build(); } catch (Exception e) {
     * logger.warn("Series Lookup failed: {}", seriesID); return Response.status(Status.SERVICE_UNAVAILABLE).build(); }
     */
  }

  @SuppressWarnings("unchecked")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("search")
  public Response searchSeries(@QueryParam("term") String pattern) {
    logger.debug("Searching all Series that match the pattern {}", pattern);
    try {
      List<Series> list = service.searchSeries(pattern);
      if (list == null)
        return Response.status(Status.BAD_REQUEST).build();
      JSONArray a = new JSONArray();
      for (Series s : list) {
        JSONObject j = new JSONObject();
        j.put("id", s.getSeriesId());
        j.put("label", s.getDescription());
        j.put("value", s.getDescription());
        a.add(j);
      }
      return Response.ok(a.toJSONString()).build();
    } catch (Exception e) {
      logger.warn("search for series failed. {}", e);
      return Response.status(Status.SERVICE_UNAVAILABLE).build();
    }
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("{seriesID}/dublincore")
  public Response getDublinCoreForSeries(@PathParam("seriesID") String seriesID) {
    logger.debug("Series Lookup: {}", seriesID);
    try {
      DublinCoreCatalog dc = service.getDublinCore(seriesID);
      if (dc == null)
        return Response.status(Status.BAD_REQUEST).build();
      return Response.ok(dc.toXmlString()).build();
    } catch (Exception e) {
      logger.warn("Series Lookup failed: {}", seriesID);
      return Response.status(Status.SERVICE_UNAVAILABLE).build();
    }
  }

  @SuppressWarnings("unchecked")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("new/id")
  public Response newSeriesId() {
    logger.debug("create new Series Id");
    JSONObject j = new JSONObject();
    j.put("id", UUID.randomUUID().toString());
    return Response.ok(j.toString()).build();
  }

  /**
   * Stores a new series in the database.
   * 
   * @param s
   *          The Series that should be stored.
   * @return json object, success: true and uuid if succeeded
   */
  @PUT
  @Path("{seriesID}")
  // TODO: Figure out why we can't just accept a SeriesImpl instead of using a stupid form param on a PUT
  public Response addSeries(@PathParam("seriesID") String seriesId, @FormParam("series") SeriesImpl series) {
    if (series == null) {
      logger.error("series that should be added is null");
      return Response.status(Status.BAD_REQUEST).build();
    }
    series.setSeriesId(seriesId);
    logger.debug("Created Series: {}", series.getSeriesId());
    try {
      service.addSeries(series);
      logger.debug("Adding series {} ", series.getSeriesId());
      return Response.status(Status.CREATED).type("").build(); // get rid of content type
    } catch (IllegalArgumentException e) {
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * 
   * Removes the specified series from the database. Returns true if the series was found and could be removed.
   * 
   * @param seriesID
   *          The unique ID of the series.
   * @return true if the series was found and was deleted.
   */
  @DELETE
  @Path("{seriesID}")
  public Response deleteSeries(@PathParam("seriesID") String seriesID) {
    try {
      service.removeSeries(seriesID);
      return Response.noContent().type("").build(); // get rid of content type
    } catch (NotFoundException e) {
      return Response.status(Status.NOT_FOUND).build();
    }
  }

  /**
   * Updates an existing series in the database. The series has to be stored in the database already. Will return true,
   * if the event was found and could be updated.
   * 
   * @param series
   *          The series that should be updated
   * @return true if the event was found and could be updated.
   */
  @POST
  @Path("{seriesId}")
  public Response updateSeries(@PathParam("seriesId") String seriesId, @FormParam("series") SeriesImpl series) {
    if (series == null) {
      logger.error("series that should be updated is null");
      return Response.status(Status.BAD_REQUEST).build();
    }
    series.setSeriesId(seriesId);
    logger.debug("Updated Series: {}", series.getSeriesId());
    try {
      service.updateSeries(series);
      return Response.noContent().type("").build(); // get rid of content-type
    } catch (NotFoundException e) {
      return Response.status(Status.NOT_FOUND).build();
    }
  }

  /**
   * returns all series
   * 
   * @return List of Series as XML
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("all.xml")
  public SeriesListImpl getAllSeriesXml() {
    return getAllSeries();
  }

  /**
   * returns all series
   * 
   * @return List of Series as JSON
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("all.json")
  public SeriesListImpl getAllSeriesJson() {
    return getAllSeries();
  }

  private SeriesListImpl getAllSeries() {
    SeriesListImpl seriesList = new SeriesListImpl();
    logger.debug("getting all series.");
    List<Series> series = service.getAllSeries();
    seriesList.setSeriesList(series);
    return seriesList;
  }

  /**
   * returns the REST documentation
   * 
   * @return the REST documentation, if available
   */
  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    return docs;
  }

  protected String docs;
  private String[] notes = {
          "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
          "If the service is down or not working it will return a status 503, this means the the underlying service is not working and is either restarting or has failed",
          "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In other words, there is a bug! You should file an error report with your server logs from the time when the error occurred: <a href=\"https://issues.opencastproject.org\">Opencast Issue Tracker</a>", };

  /**
   * Generates the REST documentation
   * 
   * @param serviceUrl
   *          the service mountpoint
   * @return The HTML with the documentation
   */
  protected String generateDocs(String serviceUrl) {
    DocRestData data = new DocRestData("Series", "Series Service", serviceUrl, notes);

    // abstract
    data.setAbstract("This service creates, edits and retrieves and helps manage sereies that capture metadata.");

    // add Series
    RestEndpoint addEndpoint = new RestEndpoint(
            "addSeries",
            RestEndpoint.Method.PUT,
            "/{seriesId}",
            "Accepts an XML or JSON form parameter representing a new Series and stores it in the database. Returns HTTP Status 201 (Created) if successful. 400 (Bad Request) if the no seriesId is supplied. 500 (Internal Server Error) if there was an error creating the series.");
    addEndpoint.addStatus(org.opencastproject.util.doc.Status.CREATED("Series was created successfully."));
    addEndpoint.addStatus(org.opencastproject.util.doc.Status.BAD_REQUEST("No seriesId was supplied."));
    addEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("The series was not created successfully."));
    addEndpoint.addPathParam(new Param("seriesId", Type.STRING, "bfa99465-b81d-4391-9c6a-a5149d3b195a",
            "A UUID to use for the new Series."));
    addEndpoint.addRequiredParam(new Param("series", Type.TEXT, generateSeries(),
            "The XML or JSON representation of the series to be stored."));
    addEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, addEndpoint);

    // update Series
    RestEndpoint updateEndpoint = new RestEndpoint(
            "updateSeries",
            RestEndpoint.Method.POST,
            "/{seriesId}",
            "Accepts an XML or JSON form parameter representing the series to be updated. The seriesId has to be stored in the database already.");
    updateEndpoint.addStatus(org.opencastproject.util.doc.Status.NO_CONTENT("Series has be successfully updated."));
    updateEndpoint.addStatus(org.opencastproject.util.doc.Status.BAD_REQUEST("No seriesId was supplied."));
    updateEndpoint.addStatus(org.opencastproject.util.doc.Status
            .NOT_FOUND("A Series matching the supplied seriesId was not found."));
    updateEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("The Series was not successfully updated."));
    updateEndpoint.addPathParam(new Param("seriesId", Type.STRING, "bfa99465-b81d-4391-9c6a-a5149d3b195a",
            "The UUID of the series to be updated."));
    updateEndpoint.addRequiredParam(new Param("series", Type.TEXT, generateSeries(),
            "The XML or JSON representation of the series to be updated."));
    updateEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, updateEndpoint);

    // remove Series
    RestEndpoint removeEndpoint = new RestEndpoint("deleteSeries", RestEndpoint.Method.DELETE, "/{seriesId}",
            "Removes the specified series from the database. Returns true if the series could be removed.");
    removeEndpoint.addStatus(org.opencastproject.util.doc.Status.NO_CONTENT("Series has been successfully deleted."));
    removeEndpoint.addStatus(org.opencastproject.util.doc.Status
            .NOT_FOUND("A Series matching the supplied seriesId was not found."));
    removeEndpoint.addPathParam(new Param("seriesId", Type.STRING, "bfa99465-b81d-4391-9c6a-a5149d3b195a",
            "The UUID of the series."));
    removeEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, removeEndpoint);

    // get Series
    RestEndpoint getEndpoint = new RestEndpoint("getSeries", RestEndpoint.Method.GET, "/{seriesId}",
            "Get a specific Series.");
    getEndpoint.addFormat(Format.xml("XML Representation of a series."));
    getEndpoint.addFormat(Format.json("JSON Representation of a series."));
    getEndpoint.setAutoPathFormat(true);
    getEndpoint.addStatus(org.opencastproject.util.doc.Status
            .OK("Series found and response contains XML or JSON representation of the series."));
    getEndpoint.addStatus(org.opencastproject.util.doc.Status
            .NO_CONTENT("A Series matching the supplied seriesId was not found."));
    getEndpoint.addPathParam(new Param("seriesId", Type.STRING, "bfa99465-b81d-4391-9c6a-a5149d3b195a",
            "The UUID of the Series."));
    getEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getEndpoint);

    // get all series
    RestEndpoint getAllEndpoint = new RestEndpoint("getAllSeries", RestEndpoint.Method.GET, "/all",
            "returns all series");
    getAllEndpoint.addFormat(Format.xml("XML Representation of a series."));
    getAllEndpoint.addFormat(Format.json("JSON Representation of a series."));
    getAllEndpoint.setAutoPathFormat(true);
    getAllEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("List of Series as XML or JSON returned"));
    getAllEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getAllEndpoint);

    // get new seriesID
    RestEndpoint newIdEndpoint = new RestEndpoint("newSeriesId", RestEndpoint.Method.GET, "/new/id",
            "returns a new UUID for a new series in a JSON Wrapper");
    newIdEndpoint.addFormat(Format.json("JSON containg the new ID"));
    newIdEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("UUID for a new seriesID"));
    newIdEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, newIdEndpoint);

    // get Dublin Core for Series
    RestEndpoint dcEndpoint = new RestEndpoint("getDublinCoreForSeries", RestEndpoint.Method.GET,
            "/{seriesID}/dublincore", "Get the DublinCore metdata for a specific Series.");
    dcEndpoint.addFormat(Format.xml("Dublin Core representation"));
    dcEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("Dublincore XML representation of Series."));
    dcEndpoint.addPathParam(new Param("seriesID", Type.STRING, "bfa99465-b81d-4391-9c6a-a5149d3b195a",
            "The UUID of the Series."));
    dcEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, dcEndpoint);

    // search Series
    RestEndpoint searchEndpoint = new RestEndpoint("getSeries", RestEndpoint.Method.GET, "/search/{pattern}",
            "Get all Series that match this pattern in their Metadata.");
    searchEndpoint.addFormat(Format.json("A JSON list of Series matching the search pattern."));
    searchEndpoint.addStatus(org.opencastproject.util.doc.Status
            .OK("JSON Object with UUID of the series and a String describing the series"));
    searchEndpoint.addPathParam(new Param("pattern", Type.STRING, "lecturer", "a part of a metadat value"));
    searchEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, searchEndpoint);

    return DocUtil.generate(data);
  }

  protected String generateSeries() {
    try {
      SeriesBuilder builder = SeriesBuilder.getInstance();

      Series series = new SeriesImpl();

      LinkedList<SeriesMetadata> metadata = new LinkedList<SeriesMetadata>();

      metadata.add(new SeriesMetadataImpl(series, "title", "demo title"));
      metadata.add(new SeriesMetadataImpl(series, "license", "demo"));
      metadata.add(new SeriesMetadataImpl(series, "valid", "" + System.currentTimeMillis()));
      metadata.add(new SeriesMetadataImpl(series, "publisher", "demo"));
      metadata.add(new SeriesMetadataImpl(series, "creator", "demo"));
      metadata.add(new SeriesMetadataImpl(series, "subject", "demo"));
      metadata.add(new SeriesMetadataImpl(series, "temporal", "demo"));
      metadata.add(new SeriesMetadataImpl(series, "audience", "demo"));
      metadata.add(new SeriesMetadataImpl(series, "spatial", "demo"));
      metadata.add(new SeriesMetadataImpl(series, "rightsHolder", "demo"));
      metadata.add(new SeriesMetadataImpl(series, "extent", "3600000"));
      metadata.add(new SeriesMetadataImpl(series, "created", "" + System.currentTimeMillis()));
      metadata.add(new SeriesMetadataImpl(series, "language", "demo"));
      metadata.add(new SeriesMetadataImpl(series, "identifier", "demo"));
      metadata.add(new SeriesMetadataImpl(series, "isReplacedBy", "demo"));
      metadata.add(new SeriesMetadataImpl(series, "type", "demo"));
      metadata.add(new SeriesMetadataImpl(series, "available", "" + System.currentTimeMillis()));
      metadata.add(new SeriesMetadataImpl(series, "modified", "" + System.currentTimeMillis()));
      metadata.add(new SeriesMetadataImpl(series, "replaces", "demo"));
      metadata.add(new SeriesMetadataImpl(series, "contributor", "demo"));
      metadata.add(new SeriesMetadataImpl(series, "description", "demo"));
      metadata.add(new SeriesMetadataImpl(series, "issued", "" + System.currentTimeMillis()));

      series.setMetadata(metadata);

      String result = builder.marshallSeries(series);
      logger.info("Series: " + result);
      return result;
    } catch (Exception e1) {
      logger.warn("Could not marshall example series: {}", e1.getMessage());
      return null;
    }
  }

}
