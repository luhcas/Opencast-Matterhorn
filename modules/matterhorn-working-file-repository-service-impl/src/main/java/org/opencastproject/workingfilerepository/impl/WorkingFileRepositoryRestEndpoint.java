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
package org.opencastproject.workingfilerepository.impl;

import org.opencastproject.rest.RestPublisher;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;
import org.opencastproject.util.doc.Status;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
public class WorkingFileRepositoryRestEndpoint extends WorkingFileRepositoryImpl {
  
  private static final Logger logger = LoggerFactory.getLogger(WorkingFileRepositoryRestEndpoint.class);

  private final MimetypesFileTypeMap mimeMap = new MimetypesFileTypeMap(getClass().getClassLoader()
          .getResourceAsStream("mimetypes"));

  /**
   * Callback from OSGi that is called when this service is activated.
   * 
   * @param cc
   *          OSGi component context
   */
  public void activate(ComponentContext cc) {
    String serviceUrl = (String) cc.getProperties().get(RestPublisher.SERVICE_PATH_PROPERTY);
    docs = generateDocs(serviceUrl);
    super.activate(cc);
  }

  protected String docs;
  private String[] notes = {
          "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
          "If the service is down or not working it will return a status 503, this means the the underlying service is not working and is either restarting or has failed",
          "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In other words, there is a bug! You should file an error report with your server logs from the time when the error occurred: <a href=\"https://issues.opencastproject.org\">Opencast Issue Tracker</a>", };

  private String generateDocs(String serviceUrl) {
    DocRestData data = new DocRestData("workingfilerepository", "Working file repository", serviceUrl, notes);
    data.setAbstract("This service provides local file access and storage for processes such as encoding.");
    // put
    RestEndpoint endpoint = new RestEndpoint("put", RestEndpoint.Method.POST,
            WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX + "{mediaPackageID}/{mediaPackageElementID}",
            "Store a file in working repository under ./mediaPackageID/mediaPackageElementID");
    endpoint.addPathParam(new Param("mediaPackageID", Param.Type.STRING, null,
            "ID of the media package under which file will be stored"));
    endpoint.addPathParam(new Param("mediaPackageElementID", Param.Type.STRING, null,
            "ID of the element under which file will be stored"));
    endpoint.addBodyParam(true, null, "File that we want to store");
    endpoint.addFormat(new Format("HTML", null, null));
    endpoint.addStatus(Status.OK("Message of successful storage with url to the stored file"));
    endpoint.addStatus(new Status(400, "No file to store, invalid file location"));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // put in collection
    endpoint = new RestEndpoint("putInCollection", RestEndpoint.Method.POST,
            WorkingFileRepository.COLLECTION_PATH_PREFIX + "{collectionId}", "Stores a file in a collection");
    endpoint.addPathParam(new Param("collectionId", Param.Type.STRING, null, "ID of the collection"));
    endpoint.addBodyParam(true, null, "File that we want to store");
    endpoint.addFormat(new Format("HTML", null, null));
    endpoint.addStatus(Status.OK("Message of successful storage with url to the stored file"));
    endpoint.addStatus(new Status(400, "No file to store, invalid file location"));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // delete from collection
    endpoint = new RestEndpoint("deleteFromCollection", RestEndpoint.Method.DELETE,
            WorkingFileRepository.COLLECTION_PATH_PREFIX + "{collectionId}/{fileName}",
            "Deletes a file from a collection");
    endpoint.addPathParam(new Param("collectionId", Param.Type.STRING, null, "ID of the collection"));
    endpoint.addPathParam(new Param("fileName", Param.Type.STRING, null, "The filename"));
    endpoint.addStatus(Status.NO_CONTENT("The file was deleted"));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // list contents of a collection
    endpoint = new RestEndpoint("listCollection", RestEndpoint.Method.GET, "/list/{collectionId}.json",
            "Lists files in a collection");
    endpoint.addPathParam(new Param("collectionId", Param.Type.STRING, null, "ID of the collection"));
    endpoint.addFormat(new Format("json", null, null));
    endpoint.addStatus(Status.OK("The collection was found, returning the contents of the collection"));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, endpoint);

    // copy
    endpoint = new RestEndpoint("copy", RestEndpoint.Method.POST,
            "/copy/{fromCollection}/{fromFileName}/{toMediaPackage}/{toMediaPackageElement}/{toFileName}",
            "Copies a file from a collection to a mediapackage");
    endpoint.addPathParam(new Param("fromCollection", Param.Type.STRING, null, "ID of the source collection"));
    endpoint.addPathParam(new Param("fromFileName", Param.Type.STRING, null, "The source file name"));
    endpoint.addPathParam(new Param("toMediaPackage", Param.Type.STRING, null, "The destination mediapackage"));
    endpoint.addPathParam(new Param("toMediaPackageElement", Param.Type.STRING, null, "The destination element ID"));
    endpoint.addPathParam(new Param("toFileName", Param.Type.STRING, null, "The destination element file name"));
    endpoint.addStatus(Status.OK("The file was copied to the URL in the response body"));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // move
    endpoint = new RestEndpoint("move", RestEndpoint.Method.POST,
            "/move/{fromCollection}/{fromFileName}/{toMediaPackage}/{toMediaPackageElement}/{toFileName}",
            "Moves a file from a collection to a mediapackage");
    endpoint.addPathParam(new Param("fromCollection", Param.Type.STRING, null, "ID of the source collection"));
    endpoint.addPathParam(new Param("fromFileName", Param.Type.STRING, null, "The source file name"));
    endpoint.addPathParam(new Param("toMediaPackage", Param.Type.STRING, null, "The destination mediapackage"));
    endpoint.addPathParam(new Param("toMediaPackageElement", Param.Type.STRING, null, "The destination element ID"));
    endpoint.addPathParam(new Param("toFileName", Param.Type.STRING, null, "The destination element file name"));
    endpoint.addStatus(Status.OK("The file was moved to the URL in the response body"));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // delete
    endpoint = new RestEndpoint("deleteViaHttp", RestEndpoint.Method.DELETE,
            WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX + "{mediaPackageID}/{mediaPackageElementID}",
            "Delete media package element identified by mediaPackageID and MediaPackageElementID");
    endpoint.addPathParam(new Param("mediaPackageID", Param.Type.STRING, null,
            "ID of the media package where element is"));
    endpoint.addPathParam(new Param("mediaPackageElementID", Param.Type.STRING, null,
            "ID of the element that will be deleted"));
    endpoint.addStatus(Status.OK("If given file exists it is deleted"));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // get
    endpoint = new RestEndpoint("get", RestEndpoint.Method.GET, WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX
            + "{mediaPackageID}/{mediaPackageElementID}",
            "Retrieve the file stored in working repository under ./mediaPackageID/MediaPackageElementID");
    endpoint.addPathParam(new Param("mediaPackageID", Param.Type.STRING, null,
            "ID of the media package with desired element"));
    endpoint.addPathParam(new Param("mediaPackageElementID", Param.Type.STRING, null, "ID of desired element"));
    // endpoint.addFormat(new Format(".*", "Data that is stored in this location", null));
    endpoint.addStatus(Status.OK("Results in a header with retrieved file"));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, endpoint);

    // get with filename
    endpoint = new RestEndpoint("get_with_filename", RestEndpoint.Method.GET,
            WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX + "{mediaPackageID}/{mediaPackageElementID}/{fileName}",
            "Retrieve the file stored in working repository under ./mediaPackageID/MediaPackageElementID");
    endpoint.addPathParam(new Param("mediaPackageID", Param.Type.STRING, null,
            "ID of the media package with desired element"));
    endpoint.addPathParam(new Param("mediaPackageElementID", Param.Type.STRING, null, "ID of desired element"));
    endpoint
            .addPathParam(new Param("fileName", Param.Type.STRING, null, "Name under which the file will be retrieved"));
    // endpoint.addFormat(new Format(".*", "Data that is stored in this location", null));
    endpoint.addStatus(Status.OK("Results in a header with retrieved file"));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, endpoint);

    // URI
    endpoint = new RestEndpoint("collectionUriWithFilename", RestEndpoint.Method.GET,
            "/collectionuri/{collectionID}/{{fileName}", "Retrieve the URI for this collectionID and filename");
    endpoint.addPathParam(new Param("collectionID", Param.Type.STRING, null, "ID of the collection"));
    endpoint.addPathParam(new Param("fileName", Param.Type.STRING, null, "The filename"));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, endpoint);

    // URI
    endpoint = new RestEndpoint("uriWithoutFilename", RestEndpoint.Method.GET,
            "/uri/{mediaPackageID}/{mediaPackageElementID}",
            "Retrieve the URI for this mediaPackageID and MediaPackageElementID");
    endpoint.addPathParam(new Param("mediaPackageID", Param.Type.STRING, null,
            "ID of the media package with desired element"));
    endpoint.addPathParam(new Param("mediaPackageElementID", Param.Type.STRING, null, "ID of desired element"));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, endpoint);

    // URI
    endpoint = new RestEndpoint("uriWithFilename", RestEndpoint.Method.GET,
            "/uri/{mediaPackageID}/{mediaPackageElementID}/{fileName}",
            "Retrieve the URI for this mediaPackageID, MediaPackageElementID, and filename");
    endpoint.addPathParam(new Param("mediaPackageID", Param.Type.STRING, null,
            "ID of the media package with desired element"));
    endpoint.addPathParam(new Param("mediaPackageElementID", Param.Type.STRING, null, "ID of desired element"));
    endpoint.addPathParam(new Param("fileName", Param.Type.STRING, null, "The filename"));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, endpoint);

    // storage
    endpoint = new RestEndpoint("totalStorage", RestEndpoint.Method.GET, "/storage",
            "Retrieve a storage report for this repository");
    endpoint.addFormat(new Format("json", null, null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, endpoint);
    return DocUtil.generate(data);
  }

  @POST
  @Produces(MediaType.TEXT_HTML)
  @Path(WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX + "{mediaPackageID}/{mediaPackageElementID}")
  public Response restPut(@PathParam("mediaPackageID") String mediaPackageID,
          @PathParam("mediaPackageElementID") String mediaPackageElementID, @Context HttpServletRequest request)
          throws Exception {
    if (ServletFileUpload.isMultipartContent(request)) {
      for (FileItemIterator iter = new ServletFileUpload().getItemIterator(request); iter.hasNext();) {
        FileItemStream item = iter.next();
        if (item.isFormField())
          continue;
        URI url = this.put(mediaPackageID, mediaPackageElementID, item.getName(), item.openStream());
        return Response.ok(url.toString()).build();
      }
    }
    return Response.serverError().status(400).build();
  }

  @POST
  @Produces(MediaType.TEXT_HTML)
  @Path(WorkingFileRepository.COLLECTION_PATH_PREFIX + "{collectionId}")
  public Response restPutInCollection(@PathParam("collectionId") String collectionId, @Context HttpServletRequest request)
          throws Exception {
    if (ServletFileUpload.isMultipartContent(request)) {
      for (FileItemIterator iter = new ServletFileUpload().getItemIterator(request); iter.hasNext();) {
        FileItemStream item = iter.next();
        if (item.isFormField())
          continue;
        URI url = this.putInCollection(collectionId, item.getName(), item.openStream());
        return Response.ok(url.toString()).build();
      }
    }
    return Response.serverError().status(400).build();
  }

  @DELETE
  @Path(WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX + "{mediaPackageID}/{mediaPackageElementID}")
  public Response restDelete(@PathParam("mediaPackageID") String mediaPackageID,
          @PathParam("mediaPackageElementID") String mediaPackageElementID) {
    try {
      this.delete(mediaPackageID, mediaPackageElementID);
      return Response.noContent().build();
    } catch (Exception e) {
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @DELETE
  @Path(WorkingFileRepository.COLLECTION_PATH_PREFIX + "{collectionId}/{fileName}")
  public Response restDeleteFromCollection(@PathParam("collectionId") String collectionId,
          @PathParam("fileName") String fileName) {
    try {
      this.deleteFromCollection(collectionId, fileName);
      return Response.noContent().build();
    } catch (Exception e) {
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @GET
  @Path(WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX + "{mediaPackageID}/{mediaPackageElementID}")
  public Response restGet(@PathParam("mediaPackageID") String mediaPackageID,
          @PathParam("mediaPackageElementID") String mediaPackageElementID,
          @HeaderParam("If-None-Match") String ifNoneMatch) {
    String contentType = null;
    InputStream in = null;
    try {
      String md5 = this.hashMediaPackageElement(mediaPackageID, mediaPackageElementID);
      if (md5.equals(ifNoneMatch)) {
        IOUtils.closeQuietly(in);
        return Response.notModified().build();
      }
      in = this.get(mediaPackageID, mediaPackageElementID);
      contentType = extractContentType(in);
      return Response.ok(this.get(mediaPackageID, mediaPackageElementID)).header("Content-Type", contentType).build();
    } catch (IllegalStateException e) {
      IOUtils.closeQuietly(in);
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (IOException e) {
      IOUtils.closeQuietly(in);
      return Response.status(500).build();
    } catch (NotFoundException e) {
      IOUtils.closeQuietly(in);
      return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  /**
   * Determines the content type of an input stream. This method reads part of the stream, so it is typically best to
   * close the stream immediately after calling this method.
   * 
   * @param in
   *          the input stream
   * @return the content type
   */
  protected String extractContentType(InputStream in) {
    try {
      // Find the content type, based on the stream content
      BodyContentHandler contenthandler = new BodyContentHandler();
      Metadata metadata = new Metadata();
      Parser parser = new AutoDetectParser();
      ParseContext context = new ParseContext();
      parser.parse(in, contenthandler, metadata, context);
      return metadata.get(Metadata.CONTENT_TYPE);
    } catch (Exception e) {
      logger.warn("Unable to extract mimetype from input stream, ", e);
      return MediaType.APPLICATION_OCTET_STREAM;
    }
  }

  @GET
  @Path(WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX + "{mediaPackageID}/{mediaPackageElementID}/{fileName}")
  public Response restGet(@PathParam("mediaPackageID") String mediaPackageID,
          @PathParam("mediaPackageElementID") String mediaPackageElementID, @PathParam("fileName") String fileName,
          @HeaderParam("If-None-Match") String ifNoneMatch) {
    InputStream in = null;
    try {
      in = get(mediaPackageID, mediaPackageElementID);
      String md5 = this.hashMediaPackageElement(mediaPackageID, mediaPackageElementID);
      if (md5.equals(ifNoneMatch)) {
        IOUtils.closeQuietly(in);
        return Response.notModified().build();
      }
      String contentType = mimeMap.getContentType(fileName);
      int contentLength = 0;
      contentLength = in.available();
      return Response.ok().header("Content-disposition", "attachment; filename=" + fileName).header("Content-Type",
              contentType).header("Content-length", contentLength).entity(in).build();
    } catch (IllegalStateException e) {
      IOUtils.closeQuietly(in);
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (NotFoundException e) {
      IOUtils.closeQuietly(in);
      return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
    } catch (IOException e) {
      IOUtils.closeQuietly(in);
      logger.info("unable to get the content length for {}/{}/{}", new Object[] { mediaPackageElementID,
              mediaPackageElementID, fileName });
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GET
  @Path(WorkingFileRepository.COLLECTION_PATH_PREFIX + "{collectionId}/{fileName}")
  public Response restGetFromCollection(@PathParam("collectionId") String collectionId,
          @PathParam("fileName") String fileName) {
    InputStream in = null;
    try {
      in = super.getFromCollection(collectionId, fileName);
    } catch (NotFoundException e) {
      IOUtils.closeQuietly(in);
      return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
    }
    String contentType = mimeMap.getContentType(fileName);
    int contentLength = 0;
    try {
      contentLength = in.available();
    } catch (IOException e) {
      logger.info("unable to get the content length for collection/{}/{}", new Object[] { collectionId, fileName });
    }
    return Response.ok().header("Content-disposition", "attachment; filename=" + fileName).header("Content-Type",
            contentType).header("Content-length", contentLength).entity(in).build();
  }

  @GET
  @Path("/collectionuri/{collectionID}/{fileName}")
  public Response restGetCollectionUri(@PathParam("collectionID") String collectionId,
          @PathParam("fileName") String fileName) {
    URI uri = this.getCollectionURI(collectionId, fileName);
    return Response.ok(uri.toString()).build();
  }

  @GET
  @Path("/uri/{mediaPackageID}/{mediaPackageElementID}")
  public Response restGetUri(@PathParam("mediaPackageID") String mediaPackageID,
          @PathParam("mediaPackageElementID") String mediaPackageElementID) {
    URI uri = this.getURI(mediaPackageID, mediaPackageElementID);
    return Response.ok(uri.toString()).build();
  }

  @GET
  @Path("/uri/{mediaPackageID}/{mediaPackageElementID}/{fileName}")
  public Response restGetUri(@PathParam("mediaPackageID") String mediaPackageID,
          @PathParam("mediaPackageElementID") String mediaPackageElementID, @PathParam("fileName") String fileName) {
    URI uri = this.getURI(mediaPackageID, mediaPackageElementID, fileName);
    return Response.ok(uri.toString()).build();
  }

  @SuppressWarnings("unchecked")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/list/{collectionId}.json")
  public Response restGetCollectionContents(@PathParam("collectionId") String collectionId) {
    try {
      URI[] uris = super.getCollectionContents(collectionId);
      JSONArray jsonArray = new JSONArray();
      for (URI uri : uris) {
        jsonArray.add(uri.toString());
      }
      return Response.ok(jsonArray.toJSONString()).build();
    } catch (Exception e) {
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @POST
  @Path("/copy/{fromCollection}/{fromFileName}/{toMediaPackage}/{toMediaPackageElement}/{toFileName}")
  public Response restCopyTo(@PathParam("fromCollection") String fromCollection,
          @PathParam("fromFileName") String fromFileName, @PathParam("toMediaPackage") String toMediaPackage,
          @PathParam("toMediaPackageElement") String toMediaPackageElement, @PathParam("toFileName") String toFileName) {
    try {
      URI uri = super.copyTo(fromCollection, fromFileName, toMediaPackage, toMediaPackageElement, toFileName);
      return Response.ok().entity(uri.toString()).build();
    } catch (Exception e) {
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @POST
  @Path("/move/{fromCollection}/{fromFileName}/{toMediaPackage}/{toMediaPackageElement}/{toFileName}")
  public Response restMoveTo(@PathParam("fromCollection") String fromCollection,
          @PathParam("fromFileName") String fromFileName, @PathParam("toMediaPackage") String toMediaPackage,
          @PathParam("toMediaPackageElement") String toMediaPackageElement, @PathParam("toFileName") String toFileName) {
    try {
      URI uri = super.moveTo(fromCollection, fromFileName, toMediaPackage, toMediaPackageElement, toFileName);
      return Response.ok().entity(uri.toString()).build();
    } catch (Exception e) {
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @SuppressWarnings("unchecked")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("storage")
  public Response restGetTotalStorage() {
    long total = this.getTotalSpace();
    long usable = this.getUsableSpace();
    String summary = this.getDiskSpace();
    JSONObject json = new JSONObject();
    json.put("size", total);
    json.put("usable", usable);
    json.put("summary", summary);
    return Response.ok(json.toJSONString()).build();
  }
  
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("/baseUri")
  public Response restGetBaseUri() {
    return Response.ok(super.getBaseUri().toString()).build();
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    return docs;
  }
}
