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
package org.opencastproject.ingest.endpoint;

import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.media.mediapackage.EName;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilder;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.MediaPackageElements;
import org.opencastproject.media.mediapackage.identifier.Id;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;


/**
 * Creates and augments Matterhorn MediaPackages using the api. Stores media into the Working File Repository.
 */
@Path("/")
public class IngestRestService {

  private static final Logger logger = LoggerFactory.getLogger(IngestRestService.class);
  private MediaPackageBuilderFactory factory = null;
  private MediaPackageBuilder builder = null;
  private IngestService ingestService = null;
  private DublinCoreCatalogService dublinCoreService;
  
  public IngestRestService() {
  }

  public void setIngestService(IngestService ingestService) {
    this.ingestService = ingestService;
    factory = MediaPackageBuilderFactory.newInstance();
    builder = factory.newMediaPackageBuilder();
  }

  public void setDublinCoreService(DublinCoreCatalogService dcService) {
    this.dublinCoreService = dcService;
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("createMediaPackage")
  public Response createMediaPackage() {
    MediaPackage mp;
    try {
      mp = ingestService.createMediaPackage();
      return Response.ok(mp).build();
    } catch (Exception e) {
      logger.warn(e.getMessage());
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  // @POST
  // @Produces(MediaType.TEXT_HTML)
  // @Path("discardMediaPackage")
  // public Response discardMediaPackage(MediapackageType mpt) {
  // try {
  // MediaPackage mp = builder.loadFromManifest(IOUtils.toInputStream(mpt.toXml()));
  // service.discardMediaPackage(mp);
  // return Response.ok("Media package discarded.").build();
  // } catch (Exception e) {
  // return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
  // }
  // }
  @POST
  @Produces(MediaType.TEXT_XML)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("addTrack")
  public Response addMediaPackageTrack(@FormParam("url") String url, @FormParam("flavor") String flavor,
          @FormParam("mediaPackage") MediaPackage mp) {
    try {
      mp = ingestService.addTrack(new URI(url), MediaPackageElementFlavor.parseFlavor(flavor), mp);
      return Response.ok(mp).build();
    } catch (Exception e) {
      logger.warn(e.getMessage());
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("addTrack")
  public Response addMediaPackageTrack(@Context HttpServletRequest request) {
    return addMediaPackageElement(request, MediaPackageElement.Type.Track);
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("addCatalog")
  public Response addMediaPackageCatalog(@FormParam("url") String url, @FormParam("flavor") String flavor,
          @FormParam("mediaPackage") MediaPackage mp) {
    try {
      MediaPackage resultingMediaPackage = ingestService.addCatalog(new URI(url), MediaPackageElementFlavor.parseFlavor(flavor), mp);
      return Response.ok(resultingMediaPackage).build();
    } catch (Exception e) {
      logger.warn(e.getMessage());
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("addCatalog")
  public Response addMediaPackageCatalog(@Context HttpServletRequest request) {
    return addMediaPackageElement(request, MediaPackageElement.Type.Catalog);
  }

  /* -------------------- start of Benjamins weekend-fun -------------------- */

  /* modifyed version of addCatalog */
//  @POST
//  @Produces(MediaType.TEXT_XML)
//  @Consumes(MediaType.MULTIPART_FORM_DATA)
//  @Path("addCatalogFromFile")
//  public Response addMediaPackageCatalogFromFile(@FormParam("file") String file, @FormParam("flavor") String flavor,
//          @FormParam("mediaPackage") MediapackageType mpt) {
//    //logger.info("adding Catalog");
//    try {
//      MediaPackage mp = builder.loadFromManifest(IOUtils.toInputStream(mpt.toXml()));
//      mp = service.addCatalog(IOUtils.toInputStream(file), MediaPackageElementFlavor.parseFlavor(flavor), mp);
//      mpt = MediapackageType.fromXml(mp.toXml());
//      return Response.ok(mpt).build();
//    } catch (Exception e) {
//      logger.warn(e.getMessage());
//      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
//    }
//  }

  /* modifyed version of addTrack */
  @POST
  @Produces(MediaType.TEXT_XML)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("addTrackMonitored")
  public Response addTrackMonitored(@Context HttpServletRequest request) {
    //logger.info("adding Track monitored");
    try {
      if (ServletFileUpload.isMultipartContent(request)) {
        ServletFileUpload upload = new ServletFileUpload();
        FileUploadListener listener = new FileUploadListener();
        MediaPackage mp = null;
        listener.setConnection(getSQLConnection(myDataSource));
        upload.setProgressListener(listener);
        /* NOTE: mediaPackage MUST be send before track (see FileItemStream doc) */
        for (FileItemIterator iter = upload.getItemIterator(request); iter.hasNext();) {
          FileItemStream item = iter.next();
          if (item.getFieldName().equals("mediaPackage")) {
            mp = builder.loadFromXml(item.openStream());
            Id mpID = mp.getIdentifier();
            listener.setMediaPackageID(mpID.compact());
          } else if (item.getFieldName().equals("track")) {
            String fullname = item.getName();
            String slashType = (fullname.lastIndexOf("\\") > 0) ? "\\" : "/";
            int startIndex = fullname.lastIndexOf(slashType);
            String filename = fullname.substring(startIndex + 1, fullname.length());
            listener.setFilename(filename);
            /* This will only work if the mediaPackage is received before the track. */
            ingestService.addTrack(item.openStream(), item.getName(), null, mp);
          }
        }
        ingestService.ingest(mp);
        return Response.ok(mp).build();
      }
      return Response.serverError().status(Status.BAD_REQUEST).build();
    } catch (Exception e) {
      logger.warn(e.getMessage());
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @SuppressWarnings("unchecked")
  @GET
  @Path("getUploadProgress/{mpId}/{filename}")
  public Response getUploadProgress(@PathParam("mpId") String mediaPackageID, @PathParam("filename") String filename) {
    JSONObject obj = new JSONObject();
    obj.put("total", 0);
    obj.put("received", 0);
    try {
      Connection con = getSQLConnection(myDataSource);
      PreparedStatement s = con.prepareStatement("SELECT total, received FROM UPLOADPROGRESS WHERE mediapackageId = ? AND filename = ?");
      s.setString(1, mediaPackageID);
      s.setString(2, filename);
      ResultSet rs = s.executeQuery();
      if (rs.next()) {
        obj.put("total", rs.getString("UPLOADPROGRESS.total"));
        obj.put("received", rs.getString("UPLOADPROGRESS.received"));
      }
      con.close();
    } catch (SQLException e) {
      logger.warn(e.getMessage());
    }
    return Response.ok(obj.toJSONString()).header("Content-Type", MediaType.APPLICATION_JSON).build();
  }
  private DataSource myDataSource;    // this should be moved to head of file

  /**
   * method to connect with opencast-db
   * @param ds Datasource object
   */
  public void setDataSource(DataSource ds) {
    logger.info("registering DataSource");
    myDataSource = ds;

    // check for existence of UPLOADPROGRESS table, create it if not present
    try {
      boolean exists = false;
      Connection con = getSQLConnection(myDataSource);
      DatabaseMetaData meta = con.getMetaData();
      ResultSet rs = meta.getTables(null, null, "UPLOADPROGRESS", null);
      while (rs.next()) {                                                  // this could be done in a more elegant way
        if (rs.getString("TABLE_NAME").equals("UPLOADPROGRESS")) {
          exists = true;
          break;
        }
      }
      if (!exists) {
        logger.info("table UPLOADPROGRESS not existing, creating it");
        PreparedStatement s = con.prepareStatement("CREATE TABLE UPLOADPROGRESS (mediapackageId varchar(255), filename varchar(2048), total bigint, received bigint)");
        s.executeUpdate();
        con.commit();
      }
      con.close();
    } catch (SQLException e) {
      logger.warn(e.getMessage());
    }
  }

  private Connection getSQLConnection(DataSource ds) throws SQLException {
    Connection con = ds.getConnection();
    if (con == null) {
      throw new SQLException("Unable to get connection from DataSource");
    } else {
      return con;
    }
  }

  /* --------------------- end of Benjamins weekend-fun --------------------- */
  @POST
  @Produces(MediaType.TEXT_XML)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("addAttachment")
  public Response addMediaPackageAttachment(@FormParam("url") String url, @FormParam("flavor") String flavor,
          @FormParam("mediaPackage") MediaPackage mp) {
    try {
      mp = ingestService.addAttachment(new URI(url), MediaPackageElementFlavor.parseFlavor(flavor), mp);
      return Response.ok(mp).build();
    } catch (Exception e) {
      logger.warn(e.getMessage());
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("addAttachment")
  public Response addMediaPackageAttachment(@Context HttpServletRequest request) {
    return addMediaPackageElement(request, MediaPackageElement.Type.Attachment);
  }

  
  protected Response addMediaPackageElement(HttpServletRequest request, MediaPackageElement.Type type) {
    MediaPackageElementFlavor flavor = null;
    try {
      InputStream in = null;
      String fileName = null;
      MediaPackage mp = null;
      if (ServletFileUpload.isMultipartContent(request)) {
        for (FileItemIterator iter = new ServletFileUpload().getItemIterator(request); iter.hasNext();) {
          FileItemStream item = iter.next();
          String fieldName = item.getFieldName();
          if (item.isFormField()) {
            if("flavor".equals(fieldName)) {
              String flavorString = Streams.asString(item.openStream());
              if(flavorString != null) flavor = MediaPackageElementFlavor.parseFlavor(flavorString);
            } else if ("mediaPackage".equals(fieldName)) {
              mp = builder.loadFromXml(item.openStream());
            }
          } else {
            fileName = item.getName();
            in = item.openStream();
          }
        }
        switch(type) {
        case Attachment:
          ingestService.addAttachment(in, fileName, flavor, mp);
          break;
        case Catalog:
          ingestService.addCatalog(in, fileName, flavor, mp);
          break;
        case Track:
          ingestService.addTrack(in, fileName, flavor, mp);
          break;
        default:
          throw new IllegalStateException("Type must be one of track, catalog, or attachment");
        }
        ingestService.ingest(mp);
        return Response.ok(mp.toXml()).build();
      }
      return Response.serverError().status(Status.BAD_REQUEST).build();
    } catch (Exception e) {
      logger.warn(e.getMessage());
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("addMediaPackage")
  public Response addMediaPackage(@Context HttpServletRequest request) {
    MediaPackageElementFlavor flavor = null;
    try {
      MediaPackage mp = ingestService.createMediaPackage();
      DublinCoreCatalog dcc = dublinCoreService.newInstance();
      if (ServletFileUpload.isMultipartContent(request)) {
        for (FileItemIterator iter = new ServletFileUpload().getItemIterator(request); iter.hasNext();) {
          FileItemStream item = iter.next();
          if (item.isFormField()) {
            String fieldName = item.getFieldName();
            if(fieldName.equals("flavor")) {
              flavor = MediaPackageElementFlavor.parseFlavor(Streams.asString(item.openStream()));
            } else {
              // TODO not all form fields should be treated as dublin core fields
              EName en = new EName(DublinCore.TERMS_NS_URI, fieldName);              
              dcc.add(en, Streams.asString(item.openStream()));
            }
          } else {
            ingestService.addTrack(item.openStream(), item.getName(), flavor, mp);
          }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        dcc.toXml(out, true);
        InputStream in = new ByteArrayInputStream(out.toByteArray());
        ingestService.addCatalog(in, "dublincore.xml", MediaPackageElements.DUBLINCORE_CATALOG, mp);
        ingestService.ingest(mp);
        return Response.ok(mp).build();
      }
      return Response.serverError().status(Status.BAD_REQUEST).build();
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @POST
  @Path("addZippedMediaPackage")
  @Consumes("application/zip")
  public Response addZippedMediaPackage(InputStream mp) {
    logger.debug("addZippedMediaPackage(InputStream) called.");
    try {
      ingestService.addZippedMediaPackage(mp);
    } catch (Exception e) {
      logger.warn(e.getMessage());
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
    return Response.ok().build();
  }

  @POST
  @Produces(MediaType.TEXT_HTML)
  @Path("ingest")
  public Response ingest(@FormParam("mediaPackage") MediaPackage mp) {
    logger.debug("ingest(MediaPackage): {}", mp);
    try {
      ingestService.ingest(mp);
      return Response.ok(mp).build();
    } catch (Exception e) {
      logger.warn(e.getMessage());
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
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

  // CHECKSTYLE:OFF
  private String generateDocs() {
    DocRestData data = new DocRestData("ingestservice", "Ingest Service", "/ingest", notes);

    // abstract
    data.setAbstract("This service creates and augments Matterhorn media packages that include media tracks, metadata catalogs and attachments.");

    // createMediaPackage
    RestEndpoint endpoint = new RestEndpoint("createMediaPackage", RestEndpoint.Method.GET,
        "/createMediaPackage",
        "Create an empty media package");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addStatus(org.opencastproject.util.doc.Status.OK("Returns media package"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.ERROR(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, endpoint);

    // addTrack (URL)
    endpoint = new RestEndpoint("addTrackURL", RestEndpoint.Method.POST,
        "/addTrack",
        "Add a media track to a given media package using an URL");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addRequiredParam(new Param("url", Param.Type.STRING, null,
        "The location of the media"));
    endpoint.addRequiredParam(new Param("flavor", Param.Type.STRING, null,
        "The kind of media"));
    endpoint.addRequiredParam(new Param("mediaPackage", Param.Type.STRING, null,
        "The media package as XML"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.OK("Returns augmented media package"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.ERROR(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // addTrack (InputStream)
    endpoint = new RestEndpoint("addTrackInputStream", RestEndpoint.Method.POST,
        "/addTrack",
        "Add a media track to a given media package using an input stream");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addBodyParam(true, null, "The media track file");
    endpoint.addRequiredParam(new Param("flavor", Param.Type.STRING, null,
        "The kind of media track"));
    endpoint.addRequiredParam(new Param("mediaPackage", Param.Type.STRING, null,
        "The media package as XML"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.OK("Returns augmented media package"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.BAD_REQUEST(null));
    endpoint.addStatus(org.opencastproject.util.doc.Status.ERROR(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // addCatalog (URL)
    endpoint = new RestEndpoint("addCatalogURL", RestEndpoint.Method.POST,
        "/addCatalog",
        "Add a metadata catalog to a given media package using an URL");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addRequiredParam(new Param("url", Param.Type.STRING, null,
        "The location of the catalog"));
    endpoint.addRequiredParam(new Param("flavor", Param.Type.STRING, null,
        "The kind of catalog"));
    endpoint.addRequiredParam(new Param("mediaPackage", Param.Type.STRING, null,
        "The media package as XML"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.OK("Returns augmented media package"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.ERROR(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // addCatalog (InputStream)
    endpoint = new RestEndpoint("addCatalogInputStream", RestEndpoint.Method.POST,
        "/addCatalog",
        "Add a metadata catalog to a given media package using an input stream");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addBodyParam(true, null, "The metadata catalog file");
    endpoint.addRequiredParam(new Param("flavor", Param.Type.STRING, null,
        "The kind of media catalog"));
    endpoint.addRequiredParam(new Param("mediaPackage", Param.Type.STRING, null,
        "The media package as XML"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.OK("Returns augmented media package"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.BAD_REQUEST(null));
    endpoint.addStatus(org.opencastproject.util.doc.Status.ERROR(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // addTrackMonitored (InputStream)
    endpoint = new RestEndpoint("addTrackMonitored", RestEndpoint.Method.POST,
        "/addTrackMonitored",
        "Asynchronously add a media track to a given media package using an input stream. Upload progress can be polled with /getUploadProgress");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addRequiredParam(new Param("mediaPackage", Param.Type.STRING, null,
        "The media package as XML"));
    endpoint.addBodyParam(true, null, "The media track file");
    endpoint.addStatus(org.opencastproject.util.doc.Status.OK("Returns augmented media package"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.BAD_REQUEST(null));
    endpoint.addStatus(org.opencastproject.util.doc.Status.ERROR(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // getUploadProgress
    endpoint = new RestEndpoint("getUploadProgress", RestEndpoint.Method.GET,
        "/getUploadProgress/{mpId}/{filename}",
        "Get the progress of a file upload");
    endpoint.addFormat(new Format("JSON", null, null));
    endpoint.addPathParam(new Param("mpId", Param.Type.STRING, null,
        "The media package ID"));
    endpoint.addPathParam(new Param("filename", Param.Type.STRING, null,
        "The name of the file"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.OK("Returns the total and currently received number of bytes"));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, endpoint);

    // addAttachment (URL)
    endpoint = new RestEndpoint("addAttachmentURL", RestEndpoint.Method.POST,
        "/addAttachment",
        "Add an attachment to a given media package using an URL");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addRequiredParam(new Param("url", Param.Type.STRING, null,
        "The location of the attachment"));
    endpoint.addRequiredParam(new Param("flavor", Param.Type.STRING, null,
        "The kind of attachment"));
    endpoint.addRequiredParam(new Param("mediaPackage", Param.Type.STRING, null,
        "The media package as XML"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.OK("Returns augmented media package"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.ERROR(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // addAttachment (InputStream)
    endpoint = new RestEndpoint("addAttachmentInputStream", RestEndpoint.Method.POST,
        "/addAttachment",
        "Add an attachment to a given media package using an input stream");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addBodyParam(true, null, "The attachment file");
    endpoint.addRequiredParam(new Param("flavor", Param.Type.STRING, null,
        "The kind of attachment"));
    endpoint.addRequiredParam(new Param("mediaPackage", Param.Type.STRING, null,
        "The media package as XML"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.OK("Returns augmented media package"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.BAD_REQUEST(null));
    endpoint.addStatus(org.opencastproject.util.doc.Status.ERROR(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);
    
    // addMediaPackage
	endpoint = new RestEndpoint("addMediaPackage", RestEndpoint.Method.POST,
	    "/addMediaPackage",
	    "Create media package from a media tracks and optional Dublin Core metadata fields");
	endpoint.addFormat(new Format("XML", null, null));
	endpoint.addBodyParam(true, null, "The media track file");
	endpoint.addRequiredParam(new Param("flavor", Param.Type.STRING, null,
	    "The kind of media track"));
	endpoint.addOptionalParam(new Param("abstract", Param.Type.STRING, null, 
	    "Metadata value"));
	endpoint.addOptionalParam(new Param("accessRights", Param.Type.STRING, null,
	    "Metadata value"));
	endpoint.addOptionalParam(new Param("available", Param.Type.STRING, null,
	    "Metadata value"));
	endpoint.addOptionalParam(new Param("contributor", Param.Type.STRING, null,
	    "Metadata value"));
	endpoint.addOptionalParam(new Param("coverage", Param.Type.STRING, null,
	    "Metadata value"));
	endpoint.addOptionalParam(new Param("created", Param.Type.STRING, null,
	    "Metadata value"));
	endpoint.addOptionalParam(new Param("creator", Param.Type.STRING, null,
	    "Metadata value"));
	endpoint.addOptionalParam(new Param("date", Param.Type.STRING, null,
	    "Metadata value"));
	endpoint.addOptionalParam(new Param("description", Param.Type.STRING, null,
	    "Metadata value"));
	endpoint.addOptionalParam(new Param("extent", Param.Type.STRING, null,
	    "Metadata value"));
	endpoint.addOptionalParam(new Param("format", Param.Type.STRING, null,
	    "Metadata value"));
	endpoint.addOptionalParam(new Param("identifier", Param.Type.STRING, null,
	    "Metadata value"));
	endpoint.addOptionalParam(new Param("isPartOf", Param.Type.STRING, null,
	    "Metadata value"));
	endpoint.addOptionalParam(new Param("isReferencedBy", Param.Type.STRING, null,
	    "Metadata value"));
	endpoint.addOptionalParam(new Param("isReplacedBy", Param.Type.STRING, null,
	    "Metadata value"));
	endpoint.addOptionalParam(new Param("language", Param.Type.STRING, null,
	    "Metadata value"));
	endpoint.addOptionalParam(new Param("license", Param.Type.STRING, null,
	    "Metadata value"));
	endpoint.addOptionalParam(new Param("publisher", Param.Type.STRING, null,
	    "Metadata value"));
	endpoint.addOptionalParam(new Param("relation", Param.Type.STRING, null,
	    "Metadata value"));
	endpoint.addOptionalParam(new Param("replaces", Param.Type.STRING, null,
	    "Metadata value"));
	endpoint.addOptionalParam(new Param("rights", Param.Type.STRING, null,
	    "Metadata value"));
	endpoint.addOptionalParam(new Param("rightsHolder", Param.Type.STRING, null,
	    "Metadata value"));
	endpoint.addOptionalParam(new Param("source", Param.Type.STRING, null,
	    "Metadata value"));
	endpoint.addOptionalParam(new Param("spatial", Param.Type.STRING, null,
	    "Metadata value"));
	endpoint.addOptionalParam(new Param("subject", Param.Type.STRING, null,
	    "Metadata value"));
	endpoint.addOptionalParam(new Param("temporal", Param.Type.STRING, null,
	    "Metadata value"));
	endpoint.addOptionalParam(new Param("title", Param.Type.STRING, null,
	    "Metadata value"));
	endpoint.addOptionalParam(new Param("type", Param.Type.STRING, null,
	    "Metadata value"));
	endpoint.addStatus(org.opencastproject.util.doc.Status.OK("Returns augmented media package"));
	endpoint.addStatus(org.opencastproject.util.doc.Status.BAD_REQUEST(null));
	endpoint.addStatus(org.opencastproject.util.doc.Status.ERROR(null));
	endpoint.setTestForm(RestTestForm.auto());
	data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);
    
    // addZippedMediaPackage
    endpoint = new RestEndpoint("addZippedMediaPackage", RestEndpoint.Method.POST,
        "/addZippedMediaPackage",
        "Create media package from a compressed file containing a manifest.xml document and all media tracks, metadata catalogs and attachments");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addBodyParam(true, null, "The compressed (application/zip) media package file");
    endpoint.addStatus(org.opencastproject.util.doc.Status.OK(null));
    endpoint.addStatus(org.opencastproject.util.doc.Status.ERROR(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);
    
    // ingest
    endpoint = new RestEndpoint("ingest", RestEndpoint.Method.POST,
        "/ingest",
        "Ingest the completed media package into the system, retrieving all URL-referenced files");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addRequiredParam(new Param("mediaPackage", Param.Type.STRING, null,
        "The ID of the given media package"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.OK("Returns the media package"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.ERROR(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    return DocUtil.generate(data);
  }
  // CHECKSTYLE:ON

}
