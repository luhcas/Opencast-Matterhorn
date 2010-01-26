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
import org.opencastproject.ingest.impl.IngestServiceImpl;
import org.opencastproject.media.mediapackage.DublinCoreCatalog;
import org.opencastproject.media.mediapackage.EName;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilder;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.MediaPackageElements;
import org.opencastproject.media.mediapackage.dublincore.DublinCore;
import org.opencastproject.media.mediapackage.dublincore.DublinCoreCatalogImpl;
import org.opencastproject.media.mediapackage.identifier.Id;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
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
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Creates and augments Matterhorn MediaPackages using the api. Stores media into the Working File Repository.
 */
@Path("/")
public class IngestRestService {

  private static final Logger logger = LoggerFactory.getLogger(IngestRestService.class);
  private MediaPackageBuilderFactory factory = null;
  private MediaPackageBuilder builder = null;
  private IngestService service = null;

  public void setService(IngestService service) {
    this.service = service;
    factory = MediaPackageBuilderFactory.newInstance();
    builder = factory.newMediaPackageBuilder();
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("createMediaPackage")
  public Response createMediaPackage() {
    MediaPackage mp;
    try {
      mp = service.createMediaPackage();
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
      mp = service.addTrack(new URI(url), MediaPackageElementFlavor.parseFlavor(flavor), mp);
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
    return addMediaPackageElement(request, MediaPackageElement.Type.Attachment);
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("addCatalog")
  public Response addMediaPackageCatalog(@FormParam("url") String url, @FormParam("flavor") String flavor,
          @FormParam("mediaPackage") MediaPackage mp) {
    try {
      MediaPackage resultingMediaPackage = service.addCatalog(new URI(url), MediaPackageElementFlavor.parseFlavor(flavor), mp);
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
    return addMediaPackageElement(request, MediaPackageElement.Type.Attachment);
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
            mp = builder.loadFromManifest(item.openStream());
            Id mpID = mp.getIdentifier();
            listener.setMediaPackageID(mpID.compact());
          } else if (item.getFieldName().equals("track")) {
            String fullname = item.getName();
            String slashType = (fullname.lastIndexOf("\\") > 0) ? "\\" : "/";
            int startIndex = fullname.lastIndexOf(slashType);
            String filename = fullname.substring(startIndex + 1, fullname.length());
            listener.setFilename(filename);
            /* This will only work if the mediaPackage is received before the track. */
            service.addTrack(item.openStream(), item.getName(), null, mp);
          }
        }
        service.ingest(mp);
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
      mp = service.addAttachment(new URI(url), MediaPackageElementFlavor.parseFlavor(flavor), mp);
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
              mp = builder.loadFromManifest(item.openStream());
            }
          } else {
            fileName = item.getName();
            in = item.openStream();
          }
        }
        switch(type) {
        case Attachment:
          service.addAttachment(in, fileName, flavor, mp);
          break;
        case Catalog:
          service.addCatalog(in, fileName, flavor, mp);
          break;
        case Track:
          service.addTrack(in, fileName, flavor, mp);
          break;
        default:
          throw new IllegalStateException("Type must be one of track, catalog, or attachment");
        }
        service.ingest(mp);
        return Response.ok(getStringFromDocument(mp.toXml())).build();
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
      MediaPackage mp = service.createMediaPackage();
      DublinCoreCatalog dcc = DublinCoreCatalogImpl.newInstance();
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
            service.addTrack(item.openStream(), item.getName(), flavor, mp);
          }
        }
        String xml = getStringFromDocument(dcc.toXml());
        service.addCatalog(IOUtils.toInputStream(xml), "dublincore.xml", MediaPackageElements.DUBLINCORE_CATALOG, mp);
        service.ingest(mp);
        return Response.ok(mp).build();
      }
      return Response.serverError().status(Status.BAD_REQUEST).build();
    } catch (Exception e) {
      logger.warn(e.getMessage());
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @POST
  @Path("addZippedMediaPackage")
  @Consumes("application/zip")
  public Response addZippedMediaPackage(InputStream mp) {
    logger.debug("addZippedMediaPackage(InputStream) called.");
    try {
      service.addZippedMediaPackage(mp);
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
      service.ingest(mp);
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
    return docs;
  }
  protected final String docs;

  public IngestRestService() {
    service = new IngestServiceImpl();
    String docsFromClassloader = null;
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream("/html/index.html");
      docsFromClassloader = IOUtils.toString(in);
    } catch (IOException e) {
      logger.error("failed to read documentation", e);
      docsFromClassloader = "unable to load documentation for " + IngestRestService.class.getName();
    } finally {
      IOUtils.closeQuietly(in);
    }
    docs = docsFromClassloader;
  }

  // method to convert Document to String
  private String getStringFromDocument(Document doc) throws Exception {
    try {
      DOMSource domSource = new DOMSource(doc);
      StringWriter writer = new StringWriter();
      StreamResult result = new StreamResult(writer);
      TransformerFactory tf = TransformerFactory.newInstance();
      Transformer transformer = tf.newTransformer();
      transformer.transform(domSource, result);
      return writer.toString();
    } catch (Exception e) {
      logger.error("Failed transforming xml to string");
      throw e;
    }
  }
}
