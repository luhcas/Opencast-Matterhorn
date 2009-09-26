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
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.MediaPackageElements;
import org.opencastproject.media.mediapackage.dublincore.DublinCore;
import org.opencastproject.media.mediapackage.dublincore.DublinCoreCatalogImpl;
import org.opencastproject.media.mediapackage.jaxb.MediapackageType;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
      MediapackageType mpt = new MediapackageType();
      mpt = MediapackageType.fromXml(mp.toXml());
      return Response.ok(mpt).build();
    } catch (Exception e) {
      return Response.serverError().status(400).build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_HTML)
  @Path("discardMediaPackage")
  public Response discardMediaPackage(MediapackageType mpt) {
    try {
      MediaPackage mp = builder.loadFromManifest(IOUtils.toInputStream(mpt.toXml()));
      service.discardMediaPackage(mp);
      return Response.ok("Media package discarded.").build();
    } catch (Exception e) {
      return Response.serverError().status(400).build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("addTrack")
  public Response addMediaPackageTrack(@FormParam("url") String url, @FormParam("flavor") String flavor,
          @FormParam("mediaPackage") MediapackageType mpt) {
    try {
      MediaPackage mp = builder.loadFromManifest(IOUtils.toInputStream(mpt.toXml()));
      URL u = new URL(url);
      mp = service.addTrack(u, MediaPackageElementFlavor.parseFlavor(flavor), mp);
      mpt = MediapackageType.fromXml(mp.toXml());
      return Response.ok(mpt).build();
    } catch (Exception e) {
      return Response.serverError().status(400).build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("addTrack")
  public Response addMediaPackageTrack(@FormParam("file") InputStream file, @FormParam("flavor") String flavor,
          @FormParam("mediaPackage") MediapackageType mpt) {
    try {
      MediaPackage mp = builder.loadFromManifest(IOUtils.toInputStream(mpt.toXml()));
      mp = service.addTrack(file, MediaPackageElementFlavor.parseFlavor(flavor), mp);
      mpt = MediapackageType.fromXml(mp.toXml());
      return Response.ok(mpt).build();
    } catch (Exception e) {
      return Response.serverError().status(400).build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("addCatalog")
  public Response addMediaPackageCatalog(@FormParam("url") String url, @FormParam("flavor") String flavor,
          @FormParam("mediaPackage") MediapackageType mpt) {
    try {
      MediaPackage mp = builder.loadFromManifest(IOUtils.toInputStream(mpt.toXml()));
      URL u = new URL(url);
      mp = service.addCatalog(u, MediaPackageElementFlavor.parseFlavor(flavor), mp);
      mpt = MediapackageType.fromXml(mp.toXml());
      return Response.ok(mpt).build();
    } catch (Exception e) {
      return Response.serverError().status(400).build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("addCatalog")
  public Response addMediaPackageCatalog(@FormParam("file") InputStream file, @FormParam("flavor") String flavor,
          @FormParam("mediaPackage") MediapackageType mpt) {
    try {
      MediaPackage mp = builder.loadFromManifest(IOUtils.toInputStream(mpt.toXml()));
      mp = service.addCatalog(file, MediaPackageElementFlavor.parseFlavor(flavor), mp);
      mpt = MediapackageType.fromXml(mp.toXml());
      return Response.ok(mpt).build();
    } catch (Exception e) {
      return Response.serverError().status(400).build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("addAttachment")
  public Response addMediaPackageAttachment(@FormParam("url") String url, @FormParam("flavor") String flavor,
          @FormParam("mediaPackage") MediapackageType mpt) {
    try {
      MediaPackage mp = builder.loadFromManifest(IOUtils.toInputStream(mpt.toXml()));
      URL u = new URL(url);
      mp = service.addAttachment(u, MediaPackageElementFlavor.parseFlavor(flavor), mp);
      mpt = MediapackageType.fromXml(mp.toXml());
      return Response.ok(mpt).build();
    } catch (Exception e) {
      return Response.serverError().status(400).build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("addAttachment")
  public Response addMediaPackageAttachment(@FormParam("file") InputStream file, @FormParam("flavor") String flavor,
          @FormParam("mediaPackage") MediapackageType mpt) {
    try {
      MediaPackage mp = builder.loadFromManifest(IOUtils.toInputStream(mpt.toXml()));
      mp = service.addAttachment(file, MediaPackageElementFlavor.parseFlavor(flavor), mp);
      mpt = MediapackageType.fromXml(mp.toXml());
      return Response.ok(mpt).build();
    } catch (Exception e) {
      return Response.serverError().status(400).build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("addMediaPackage")
  public Response addMediaPackage(@Context HttpServletRequest request) {
    try {
      MediaPackage mp = service.createMediaPackage();
      DublinCoreCatalog dcc = DublinCoreCatalogImpl.newInstance();
      if (ServletFileUpload.isMultipartContent(request)) {
        for (FileItemIterator iter = new ServletFileUpload().getItemIterator(request); iter.hasNext();) {
          FileItemStream item = iter.next();
          if (item.isFormField()) {
            EName en = new EName(DublinCore.TERMS_NS_URI, item.getFieldName());
            dcc.add(en, Streams.asString(item.openStream()));
          } else {
            service.addTrack(item.openStream(), MediaPackageElements.INDEFINITE_TRACK, mp);
          }
        }
        service.addCatalog(IOUtils.toInputStream(getStringFromDocument(dcc.toXml())),
                MediaPackageElements.DUBLINCORE_CATALOG, mp);
        MediapackageType mpt = MediapackageType.fromXml(mp.toXml());
        return Response.ok(mpt).build();
      }
      return Response.serverError().status(400).build();
    } catch (Exception e) {
      return Response.serverError().status(400).build();
    }

  }

  @POST
  @Produces(MediaType.TEXT_HTML)
  @Path("ingest")
  public Response ingestMediaPackage(@FormParam("mediaPackage") MediapackageType mpt) {
    try {
      MediaPackage mp = builder.loadFromManifest(IOUtils.toInputStream(mpt.toXml()));
      service.ingest(mp);
      mpt = MediapackageType.fromXml(mp.toXml());
      return Response.ok(mpt).build();
    } catch (Exception e) {
      return Response.serverError().status(400).build();
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
