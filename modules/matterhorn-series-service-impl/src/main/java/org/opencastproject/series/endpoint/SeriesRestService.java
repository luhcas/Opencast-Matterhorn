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
import org.opencastproject.series.api.Series;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.doc.DocRestData;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
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
   * @param service
   */
  public void setService(SeriesService service) {
    this.service = service;
  }
  
  /**
   * Method to unset the service this REST endpoint uses
   * @param service
   */
  public void unsetService(SeriesService service) {
    this.service = null;
  }
  
  /**
   * The method that will be called, if the service will be activated
   * @param cc The ComponentContext of this service
   */
  public void activate(ComponentContext cc) {
    // Get the configured server URL
    if(cc == null) {
      serverUrl = UrlSupport.DEFAULT_BASE_URL;
    } else {
      String ccServerUrl = cc.getBundleContext().getProperty("org.opencastproject.server.url");
      logger.info("configured server url is {}", ccServerUrl);
      if(ccServerUrl == null) {
        serverUrl = UrlSupport.DEFAULT_BASE_URL;
      } else {
        serverUrl = ccServerUrl;
      }
    }
  }  
  
  
  /**
   * Get a specific series.
   * @param seriesID The unique ID of the series.
   * @return series XML with the data of the series
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("series/{seriesID}")
  public Response getSeries(@PathParam("seriesID") String seriesID) {
    logger.debug("Series Lookup: {}", seriesID);
    try {
      Series s = service.getSeries(seriesID);
      if (s == null) return Response.status(Status.BAD_REQUEST).build();
      return Response.ok(new SeriesJaxbImpl(s)).build();
    } catch (Exception e) {
      logger.warn("Series Lookup failed: {}", seriesID);
      return Response.status(Status.SERVICE_UNAVAILABLE).build();
    }
  }
  
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("series/{seriesID}/dublincore")
  public Response getDublinCoreForSeries(@PathParam("seriesID") String seriesID) {
    logger.debug("Series Lookup: {}", seriesID);
    try {
      DublinCore dc = service.getDublinCore(seriesID);
      if (dc == null) return Response.status(Status.BAD_REQUEST).build();
      return Response.ok(dc).build();
    } catch (Exception e) {
      logger.warn("Series Lookup failed: {}", seriesID);
      return Response.status(Status.SERVICE_UNAVAILABLE).build();
    }
  }  

  /**
   * Stores a new series in the database. 
   * @param s The Series that should be stored.
   * @return true, if the Series could be stored
   */
  @PUT
  @Produces(MediaType.TEXT_XML)
  @Path("series")
  public Response addSeries (@FormParam("event") SeriesJaxbImpl s) {
    logger.debug("addseries: {}", s);
    
    if (s == null) {
      logger.error("series that should be added is null");
      return Response.status(Status.BAD_REQUEST).build();
    }
    Series series = s.getSeries();
    if (series == null) {
      logger.error("series that should be added is null");
      return Response.status(Status.BAD_REQUEST).build();
    }   
    boolean result = service.addSeries(series);
    logger.info("Adding event {} to scheduler",series.getSeriesId());
    return Response.ok(result).build();
  }  
  
  /**
   * 
   * Removes the specified event from the database. Returns true if the event was found and could be removed.
   * @param eventID The unique ID of the event.
   * @return true if the event was found and could be deleted.
   */
  @DELETE
  @Produces(MediaType.TEXT_PLAIN)
  @Path("series/{seriesID}")
  public Response deleteSeries (@PathParam("seriesID") String seriesID) {
    return Response.ok(service.removeSeries(seriesID)).build();
  }  
  
  
  /**
   * Updates an existing event in the database. The event-id has to be stored in the database already. Will return true, if the event was found and could be updated.
   * @param e The SchedulerEvent that should be updated 
   * @return true if the event was found and could be updated.
   */
  @POST
  @Produces(MediaType.TEXT_PLAIN)
  @Path("series")
  public Response updateSeries (@FormParam("series") SeriesJaxbImpl series) {
    
    if (series == null) {
      logger.error("series that should be updated is null");
      return Response.status(Status.BAD_REQUEST).build();
    }
    Series s = series.getSeries();
    if (s == null) {
      logger.error("series that should be updated is null");
      return Response.status(Status.BAD_REQUEST).build();
    }
    if(service.updateSeries(s)) {
      return Response.ok(true).build();
    } else {
      return Response.serverError().build();
    }
  }  
  
 
 /**
   * returns scheduled events, that pass the filter. filter: an xml definition of the filter. Tags that are not included will noct be filtered. Possible values for order by are title,creator,series,time-asc,time-desc,contributor,channel,location,device
   * @param filter exact id to search for pattern to search for pattern to search for A short description of the content of the lecture begin of the period of valid events end of the period of valid events pattern to search for ID of the series which will be filtered ID of the channel that will be filtered pattern to search for pattern to search for pattern to search for title|creator|series|time-asc|time-desc|contributor|channel|location|device">
   * @return List of SchedulerEvents as XML 
   */
  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("all/series")
  public Response getAllSeries () {
    logger.debug("getting all series.");
    List<Series> series = service.getAllSeries();
    if (series == null) return Response.status(Status.BAD_REQUEST).build();
    LinkedList<SeriesJaxbImpl> list = new LinkedList<SeriesJaxbImpl>();
    for (Series s : series) {
      list.add(new SeriesJaxbImpl(s));
    }
    return Response.ok((new GenericEntity<List<SeriesJaxbImpl>> (list){})).build();
  }         
  
  /**
   * returns the REST documentation
   * @return the REST documentation, if available
   */
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
  
  /**
   * Generates the REST documentation
   * @return The HTML with the documentation
   */
  protected String generateDocs() {
    DocRestData data = new DocRestData("Scheduler", "Scheduler Service", "/scheduler/rest", notes);

    // abstract
    data.setAbstract("This service creates, edits and retrieves and helps manage sereies that capture metadata."); 

    return DocUtil.generate(data);
  }
  
}
