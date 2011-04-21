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
package org.opencastproject.ingest.endpoint;

import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.rest.RestConstants;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.Param.Type;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowParser;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.spi.PersistenceProvider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
@RestService(name = "ingestservice", title = "Ingest Service", notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is not working and is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In other words, there is a bug! You should file an error report with your server logs from the time when the error occurred: <a href=\"https://issues.opencastproject.org\">Opencast Issue Tracker</a>" }, abstractText = "This service creates and augments Matterhorn media packages that include media tracks, metadata catalogs and attachments.")
public class IngestRestService {

  private static final Logger logger = LoggerFactory.getLogger(IngestRestService.class);

  /** The collection name used for temporarily storing uploaded zip files */
  private static final String COLLECTION_ID = "ingest-temp";

  /** The http request parameter used to provide the workflow instance id */
  private static final String WORKFLOW_INSTANCE_ID_PARAM = "workflowInstanceId";

  /** The http request parameter used to provide the workflow definition id */
  private static final String WORKFLOW_DEFINITION_ID_PARAM = "workflowDefinitionId";

  private MediaPackageBuilderFactory factory = null;
  private IngestService ingestService = null;
  private Workspace workspace = null;
  private DublinCoreCatalogService dublinCoreService;
  protected PersistenceProvider persistenceProvider;
  protected Map<String, Object> persistenceProperties;
  protected EntityManagerFactory emf = null;
  // For the progress bar -1 bug workaround, keeping UploadJobs in memory rather than saving them using JPA
  private HashMap<String, UploadJob> jobs;

  public IngestRestService() {
    factory = MediaPackageBuilderFactory.newInstance();
    jobs = new HashMap<String, UploadJob>();
  }

  public void setIngestService(IngestService ingestService) {
    this.ingestService = ingestService;
  }

  public void setDublinCoreService(DublinCoreCatalogService dcService) {
    this.dublinCoreService = dcService;
  }

  public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void setPersistenceProperties(Map persistenceProperties) {
    this.persistenceProperties = persistenceProperties;
  }

  /**
   * Callback for activation of this component.
   */
  public void activate(ComponentContext context) {
    try {
      emf = persistenceProvider
              .createEntityManagerFactory("org.opencastproject.ingest.endpoint", persistenceProperties);
    } catch (Exception e) {
      logger.error("Unable to initialize JPA EntityManager: " + e.getMessage());
    }
    if (context != null) {
      String serviceUrl = (String) context.getProperties().get(RestConstants.SERVICE_PATH_PROPERTY);
      docs = generateDocs(serviceUrl);
    }
  }
  
  /**
   * Callback for deactivation of this component.
   */
  public void deactivate() {
    if (emf != null && emf.isOpen()) {
      emf.close();
    }
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("createMediaPackage")
  @RestQuery(name = "createMediaPackage", description = "Create an empty media package", pathParameters = { }, restParameters = { }, reponses = {
          @RestResponse(description = "Returns media package", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "")
  public Response createMediaPackage() {
    MediaPackage mp;
    try {
      mp = ingestService.createMediaPackage();
      return Response.ok(mp).build();
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @POST
  @Path("discardMediaPackage")
  @RestQuery(name = "discardMediaPackage", description = "Discard a media package", pathParameters = { }, restParameters = { @RestParameter(defaultValue = "", description = "Given media package to be destroyed", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT) }, reponses = {
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "")
  public Response discardMediaPackage(@FormParam("mediaPackage") String mpx) {
    logger.debug("discardMediaPackage(MediaPackage): {}", mpx);
    try {
      MediaPackage mp = factory.newMediaPackageBuilder().loadFromXml(mpx);
      ingestService.discardMediaPackage(mp);
      return Response.ok().build();
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("addTrack")
  @RestQuery(name = "addTrackURL", description = "Add a media track to a given media package using an URL", pathParameters = { }, restParameters = {
          @RestParameter(defaultValue = "", description = "The location of the media", isRequired = true, name = "url", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "The kind of media", isRequired = true, name = "flavor", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "The media package as XML", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT) }, reponses = {
          @RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "")
  public Response addMediaPackageTrack(@FormParam("url") String url, @FormParam("flavor") String flavor,
          @FormParam("mediaPackage") String mpx) {
    try {
      MediaPackage mp = factory.newMediaPackageBuilder().loadFromXml(mpx);
      mp = ingestService.addTrack(new URI(url), MediaPackageElementFlavor.parseFlavor(flavor), mp);
      return Response.ok(mp).build();
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("addTrack")
  @RestQuery(name = "addTrackInputStream", description = "Add a media track to a given media package using an input stream", pathParameters = { }, restParameters = {
          @RestParameter(defaultValue = "", description = "The kind of media track", isRequired = true, name = "flavor", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "The media package as XML", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT) }, bodyParameter = @RestParameter(defaultValue = "", description = "The media track file", isRequired = true, name = "BODY", type = RestParameter.Type.FILE), reponses = {
          @RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "")
  public Response addMediaPackageTrack(@Context HttpServletRequest request) {
    return addMediaPackageElement(request, MediaPackageElement.Type.Track);
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("addCatalog")
  @RestQuery(name = "addCatalogURL", description = "Add a metadata catalog to a given media package using an URL", pathParameters = { }, restParameters = {
          @RestParameter(defaultValue = "", description = "The location of the catalog", isRequired = true, name = "url", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "The kind of catalog", isRequired = true, name = "flavor", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "The media package as XML", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT) }, reponses = {
          @RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "")
  public Response addMediaPackageCatalog(@FormParam("url") String url, @FormParam("flavor") String flavor,
          @FormParam("mediaPackage") String mpx) {
    try {
      MediaPackage mp = factory.newMediaPackageBuilder().loadFromXml(mpx);
      MediaPackage resultingMediaPackage = ingestService.addCatalog(new URI(url),
              MediaPackageElementFlavor.parseFlavor(flavor), mp);
      return Response.ok(resultingMediaPackage).build();
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("addCatalog")
  @RestQuery(name = "addCatalogInputStream", description = "Add a metadata catalog to a given media package using an input stream", pathParameters = { }, restParameters = {
          @RestParameter(defaultValue = "", description = "The kind of media catalog", isRequired = true, name = "flavor", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "The media package as XML", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT) }, bodyParameter = @RestParameter(defaultValue = "", description = "The metadata catalog file", isRequired = true, name = "BODY", type = RestParameter.Type.FILE), reponses = {
          @RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "")
  public Response addMediaPackageCatalog(@Context HttpServletRequest request) {
    return addMediaPackageElement(request, MediaPackageElement.Type.Catalog);
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("addAttachment")
  @RestQuery(name = "addAttachmentURL", description = "Add an attachment to a given media package using an URL", pathParameters = { }, restParameters = {
          @RestParameter(defaultValue = "", description = "The location of the attachment", isRequired = true, name = "url", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "The kind of attachment", isRequired = true, name = "flavor", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "The media package as XML", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT) }, reponses = {
          @RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "")
  public Response addMediaPackageAttachment(@FormParam("url") String url, @FormParam("flavor") String flavor,
          @FormParam("mediaPackage") String mpx) {
    try {
      MediaPackage mp = factory.newMediaPackageBuilder().loadFromXml(mpx);
      mp = ingestService.addAttachment(new URI(url), MediaPackageElementFlavor.parseFlavor(flavor), mp);
      return Response.ok(mp).build();
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("addAttachment")
  @RestQuery(name = "addAttachmentInputStream", description = "Add an attachment to a given media package using an input stream", pathParameters = { }, restParameters = {
          @RestParameter(defaultValue = "", description = "The kind of attachment", isRequired = true, name = "flavor", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "The media package as XML", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT) }, bodyParameter = @RestParameter(defaultValue = "", description = "The attachment file", isRequired = true, name = "BODY", type = RestParameter.Type.FILE), reponses = {
          @RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "")
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
        boolean isDone = false;
        for (FileItemIterator iter = new ServletFileUpload().getItemIterator(request); iter.hasNext();) {
          FileItemStream item = iter.next();
          String fieldName = item.getFieldName();
          if (item.isFormField()) {
            if ("flavor".equals(fieldName)) {
              String flavorString = Streams.asString(item.openStream());
              if (flavorString != null) {
                flavor = MediaPackageElementFlavor.parseFlavor(flavorString);
              }
            } else if ("mediaPackage".equals(fieldName)) {
              mp = factory.newMediaPackageBuilder().loadFromXml(item.openStream());
            }
          } else {
            // once the body gets read iter.hasNext must not be invoked
            // or the stream can not be read
            fileName = item.getName();
            in = item.openStream();
            isDone = true;
          }
          if (isDone) {
            break;
          }
        }
        switch (type) {
          case Attachment:
            mp = ingestService.addAttachment(in, fileName, flavor, mp);
            break;
          case Catalog:
            mp = ingestService.addCatalog(in, fileName, flavor, mp);
            break;
          case Track:
            mp = ingestService.addTrack(in, fileName, flavor, mp);
            break;
          default:
            throw new IllegalStateException("Type must be one of track, catalog, or attachment");
        }
        // ingestService.ingest(mp);
        return Response.ok(MediaPackageParser.getAsXml(mp)).build();
      }
      return Response.serverError().status(Status.BAD_REQUEST).build();
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("addMediaPackage")
  @RestQuery(name = "addMediaPackage", description = "Create media package from a media tracks and optional Dublin Core metadata fields", pathParameters = { }, restParameters = {
          @RestParameter(defaultValue = "", description = "The kind of media track", isRequired = true, name = "flavor", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "abstract", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "accessRights", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "available", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "contributor", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "coverage", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "created", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "creator", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "date", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "description", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "extent", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "format", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "identifier", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "isPartOf", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "isReferencedBy", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "isReplacedBy", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "language", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "license", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "publisher", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "relation", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "replaces", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "rights", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "rightsHolder", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "source", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "spatial", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "subject", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "temporal", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "title", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "type", type = RestParameter.Type.STRING) }, bodyParameter = @RestParameter(defaultValue = "", description = "The media track file", isRequired = true, name = "BODY", type = RestParameter.Type.FILE), reponses = {
          @RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "")
  public Response addMediaPackage(@Context HttpServletRequest request) {
    return addMediaPackage(request, null);
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("addMediaPackage/{wdID}")
  @RestQuery(name = "addMediaPackage", description = "Create media package from a media tracks and optional Dublin Core metadata fields", pathParameters = { @RestParameter(defaultValue = "", description = "Workflow definition id", isRequired = true, name = "wdID", type = RestParameter.Type.STRING) }, restParameters = {
          @RestParameter(defaultValue = "", description = "The kind of media track", isRequired = true, name = "flavor", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "abstract", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "accessRights", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "available", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "contributor", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "coverage", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "created", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "creator", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "date", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "description", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "extent", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "format", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "identifier", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "isPartOf", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "isReferencedBy", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "isReplacedBy", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "language", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "license", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "publisher", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "relation", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "replaces", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "rights", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "rightsHolder", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "source", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "spatial", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "subject", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "temporal", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "title", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "Metadata value", isRequired = false, name = "type", type = RestParameter.Type.STRING) }, bodyParameter = @RestParameter(defaultValue = "", description = "The media track file", isRequired = true, name = "BODY", type = RestParameter.Type.FILE), reponses = {
          @RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "")
  public Response addMediaPackage(@Context HttpServletRequest request, @PathParam("wdID") String wdID) {
    MediaPackageElementFlavor flavor = null;
    try {
      MediaPackage mp = ingestService.createMediaPackage();
      DublinCoreCatalog dcc = dublinCoreService.newInstance();
      if (ServletFileUpload.isMultipartContent(request)) {
        for (FileItemIterator iter = new ServletFileUpload().getItemIterator(request); iter.hasNext();) {
          FileItemStream item = iter.next();
          if (item.isFormField()) {
            String fieldName = item.getFieldName();
            if ("flavor".equals(fieldName)) {
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
        ingestService.addCatalog(in, "dublincore.xml", MediaPackageElements.EPISODE, mp);
        WorkflowInstance workflow;
        if (wdID == null) {
          workflow = ingestService.ingest(mp);
        } else {
          workflow = ingestService.ingest(mp, wdID);
        }
        return Response.ok(workflow).build();
      }
      return Response.serverError().status(Status.BAD_REQUEST).build();
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @POST
  @Path("addZippedMediaPackage")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "addZippedMediaPackage", description = "Create media package from a compressed file containing a manifest.xml document and all media tracks, metadata catalogs and attachments", pathParameters = { }, restParameters = {
          @RestParameter(defaultValue = "full", description = "The workflow definition ID to run on this mediapackage", isRequired = true, name = WORKFLOW_DEFINITION_ID_PARAM, type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "", description = "The workflow instance ID to associate with this zipped mediapackage", isRequired = false, name = WORKFLOW_INSTANCE_ID_PARAM, type = RestParameter.Type.STRING) }, bodyParameter = @RestParameter(defaultValue = "", description = "The compressed (application/zip) media package file", isRequired = true, name = "BODY", type = RestParameter.Type.FILE), reponses = {
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_BAD_REQUEST) }, returnDescription = "")
  public Response addZippedMediaPackage(@Context HttpServletRequest request) {
    logger.debug("addZippedMediaPackage(HttpRequest)");
    FileInputStream zipInputStream = null;
    String zipFileName = UUID.randomUUID().toString() + ".zip";
    try {
      String workflowDefinitionId = null;
      Long workflowInstanceIdAsLong = null;
      URI zipFileUri = null;
      Map<String, String> workflowConfig = new HashMap<String, String>();
      if (ServletFileUpload.isMultipartContent(request)) {
        for (FileItemIterator iter = new ServletFileUpload().getItemIterator(request); iter.hasNext();) {
          FileItemStream item = iter.next();
          if (item.isFormField()) {
            if (WORKFLOW_INSTANCE_ID_PARAM.equals(item.getFieldName())) {
              String workflowIdAsString = IOUtils.toString(item.openStream(), "UTF-8");
              try {
                workflowInstanceIdAsLong = Long.parseLong(workflowIdAsString);
              } catch (NumberFormatException e) {
                logger.warn("{} '{}' is not numeric", WORKFLOW_INSTANCE_ID_PARAM, workflowIdAsString);
              }
            } else if (WORKFLOW_DEFINITION_ID_PARAM.equals(item.getFieldName())) {
              workflowDefinitionId = IOUtils.toString(item.openStream(), "UTF-8");
            } else {
              logger.debug("Processing form field: " + item.getFieldName());
              workflowConfig.put(item.getFieldName(), IOUtils.toString(item.openStream(), "UTF-8"));
            }
          } else {
            logger.debug("Processing file item");
            InputStream in = item.openStream();
            try {
              zipFileUri = workspace.putInCollection(COLLECTION_ID, zipFileName, in);
            } finally {
              IOUtils.closeQuietly(in);
            }
          }
        }
      } else {
        InputStream in = request.getInputStream();
        try {
          zipFileUri = workspace.putInCollection(COLLECTION_ID, zipFileName, in);
        } finally {
          IOUtils.closeQuietly(in);
        }
      }
      File zipFileFromWorkspace = workspace.get(zipFileUri);
      zipInputStream = new FileInputStream(zipFileFromWorkspace);
      WorkflowInstance workflow = ingestService.addZippedMediaPackage(zipInputStream, workflowDefinitionId,
              workflowConfig, workflowInstanceIdAsLong);
      return Response.ok(WorkflowParser.toXml(workflow)).build();

    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    } finally {
      IOUtils.closeQuietly(zipInputStream);
      try {
        workspace.deleteFromCollection(COLLECTION_ID, zipFileName);
      } catch (NotFoundException nfe) {
        // That's fine, we failed somewhere on the way
        logger.debug("Error removing missing temporary ingest file " + COLLECTION_ID + "/" + zipFileName, nfe);
      } catch (IOException ioe) {
        logger.warn("Error removing temporary ingest file " + COLLECTION_ID + "/" + zipFileName, ioe);
      }
    }
  }

  @POST
  @Produces(MediaType.TEXT_HTML)
  @Path("ingest")
  @RestQuery(name = "ingest", description = "Ingest the completed media package into the system, retrieving all URL-referenced files", pathParameters = { }, restParameters = { @RestParameter(defaultValue = "", description = "The ID of the given media package", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT) }, reponses = {
          @RestResponse(description = "Returns the media package", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_BAD_REQUEST) }, returnDescription = "")
  public Response ingest(@FormParam("mediaPackage") String mpx) {
    logger.debug("ingest(MediaPackage): {}", mpx);
    try {
      MediaPackage mp = factory.newMediaPackageBuilder().loadFromXml(mpx);
      WorkflowInstance workflow = ingestService.ingest(mp);
      return Response.ok(WorkflowParser.toXml(workflow)).build();
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /*
   * @POST
   * 
   * @Produces(MediaType.TEXT_HTML)
   * 
   * @Path("ingest/{wdID}") public Response ingest(@FormParam("mediaPackage") String mpx, @PathParam("wdID") String
   * wdID) { logger.debug("ingest(MediaPackage, ID): {}, {}", mpx, wdID); try { MediaPackage mp =
   * builder.loadFromXml(mpx); WorkflowInstance workflow = ingestService.ingest(mp, wdID); return
   * Response.ok(WorkflowBuilder.toXml(workflow)).build(); } catch (Exception e) { logger.warn(e.getMessage(), e);
   * return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build(); } }
   */
  @POST
  @Produces(MediaType.TEXT_HTML)
  @Path("ingest/{wdID}")
  @SuppressWarnings("unchecked")
  @RestQuery(name = "ingest", description = "Ingest the completed media package into the system, retrieving all URL-referenced files, and starting a specified workflow", pathParameters = { @RestParameter(defaultValue = "", description = "Workflow definition id", isRequired = true, name = "wdID", type = RestParameter.Type.STRING) }, restParameters = { @RestParameter(defaultValue = "", description = "The ID of the given media package", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT) }, reponses = {
          @RestResponse(description = "Returns the media package", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_BAD_REQUEST) }, returnDescription = "")
  public Response ingest(@PathParam("wdID") String wdID, @Context HttpServletRequest request) {
    Map<String, String[]> params = request.getParameterMap();
    HashMap<String, String> wfConfig = new HashMap<String, String>();
    MediaPackage mp = null;
    try {
      for (Iterator<String> i = params.keySet().iterator(); i.hasNext();) {
        String key = i.next();
        if ("MEDIAPACKAGE".equalsIgnoreCase(key)) {
          mp = factory.newMediaPackageBuilder().loadFromXml(((String[]) params.get(key))[0]);
        } else {
          wfConfig.put(key, ((String[]) params.get(key))[0]); // TODO how do we handle multiple values eg. resulting
          // from checkboxes
        }
      }
      if (mp != null) {
        WorkflowInstance workflow = ingestService.ingest(mp, wdID, wfConfig);
        return Response.ok(WorkflowParser.toXml(workflow)).build();
      } else {
        return Response.status(Status.BAD_REQUEST).build();
      }
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  protected UploadJob createUploadJob() {
    /*
     * EntityManager em = emf.createEntityManager(); EntityTransaction tx = em.getTransaction(); try { UploadJob job =
     * new UploadJob(); tx.begin(); em.persist(job); tx.commit(); return job; } catch (RollbackException ex) {
     * logger.error(ex.getMessage(), ex); tx.rollback(); throw new RuntimeException(ex); } finally { em.close(); }
     */
    UploadJob job = new UploadJob();
    jobs.put(job.getId(), job);
    return job;
  }

  /**
   * Creates an upload job and returns an HTML form ready for uploading the file to the newly created upload job.
   * Returns 500 if something goes wrong unexpectedly
   * 
   * @return HTML form ready for uploading the file
   */
  @GET
  @Path("filechooser-local.html")
  @Produces(MediaType.TEXT_HTML)
  public Response createUploadJobHtml() {
    InputStream is = null;
    try {
      UploadJob job = createUploadJob();
      is = getClass().getResourceAsStream("/templates/uploadform.html");
      String html = IOUtils.toString(is, "UTF-8");
      // String uploadURL = serverURL + "/ingest/addElementMonitored/" + job.getId();
      String uploadURL = "addElementMonitored/" + job.getId();
      html = html.replaceAll("\\{uploadURL\\}", uploadURL);
      html = html.replaceAll("\\{jobId\\}", job.getId());
      logger.info("New upload job created: " + job.getId());
      jobs.put(job.getId(), job);
      return Response.ok(html).build();
    } catch (Exception ex) {
      logger.warn(ex.getMessage(), ex);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    } finally {
      IOUtils.closeQuietly(is);
    }
  }

  @GET
  @Path("filechooser-inbox.html")
  @Produces(MediaType.TEXT_HTML)
  public Response createInboxHtml() {
    InputStream is = null;
    try {
      is = getClass().getResourceAsStream("/templates/inboxform.html");
      String html = IOUtils.toString(is, "UTF-8");
      return Response.ok(html).build();
    } catch (Exception ex) {
      logger.warn(ex.getMessage(), ex);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    } finally {
      IOUtils.closeQuietly(is);
    }
  }

  /**
   * Add an elements to a MediaPackage and keeps track of the progress of the upload. Returns an HTML that triggers the
   * host sites UploadListener.uploadComplete javascript event Returns an HTML that triggers the host sites
   * UploadListener.uplaodFailed javascript event in case of error
   * 
   * @param jobId
   *          of the upload job
   * @param request
   *          containing the file, the flavor and the MediaPackage to which it should be added
   * @return HTML that calls the UploadListener.uploadComplete javascript handler
   */
  @POST
  @Path("addElementMonitored/{jobId}")
  @Produces(MediaType.TEXT_HTML)
  public Response addElementMonitored(@PathParam("jobId") String jobId, @Context HttpServletRequest request) {
    UploadJob job = null;
    MediaPackage mp = null;
    String fileName = null;
    MediaPackageElementFlavor flavor = null;
    String elementType = "track";
    EntityManager em = emf.createEntityManager();
    try {
      try { // try to get UploadJob, responde 404 if not successful
        // job = em.find(UploadJob.class, jobId);
        if (jobs.containsKey(jobId)) {
          job = jobs.get(jobId);
        } else {
          throw new NoResultException("Job not found");
        }
      } catch (NoResultException e) {
        logger.warn("Upload job not found for Id: " + jobId);
        return buildUploadFailedRepsonse(job);
      }
      if (ServletFileUpload.isMultipartContent(request)) {
        ServletFileUpload upload = new ServletFileUpload();
        UploadProgressListener listener = new UploadProgressListener(job, this.emf);
        upload.setProgressListener(listener);
        for (FileItemIterator iter = upload.getItemIterator(request); iter.hasNext();) {
          FileItemStream item = iter.next();
          String fieldName = item.getFieldName();
          if ("mediaPackage".equalsIgnoreCase(fieldName)) {
            mp = factory.newMediaPackageBuilder().loadFromXml(item.openStream());
          } else if ("flavor".equals(fieldName)) {
            String flavorString = Streams.asString(item.openStream());
            if (flavorString != null) {
              flavor = MediaPackageElementFlavor.parseFlavor(flavorString);
            }
          } else if ("elementType".equalsIgnoreCase(fieldName)) {
            String typeString = Streams.asString(item.openStream());
            if (typeString != null) {
              elementType = typeString;
            }
          } else if ("file".equalsIgnoreCase(fieldName)) {
            fileName = item.getName();
            job.setFilename(fileName);
            if ((mp != null) && (flavor != null) && (fileName != null)) {
              // decide which element type to add
              if ("TRACK".equalsIgnoreCase(elementType)) {
                mp = ingestService.addTrack(item.openStream(), fileName, flavor, mp);
              } else if ("CATALOG".equalsIgnoreCase(elementType)) {
                logger.info("Adding Catalog: " + fileName + " - " + flavor);
                mp = ingestService.addCatalog(item.openStream(), fileName, flavor, mp);
              }
              InputStream is = null;
              try {
                is = getClass().getResourceAsStream("/templates/complete.html");
                String html = IOUtils.toString(is, "UTF-8");
                html = html.replaceAll("\\{mediaPackage\\}", MediaPackageParser.getAsXml(mp));
                html = html.replaceAll("\\{jobId\\}", job.getId());
                return Response.ok(html).build();
              } finally {
                IOUtils.closeQuietly(is);
              }
            }
          }
        }
      } else {
        logger.warn("Job " + job.getId() + ": message is not multipart/form-data encoded");
      }
      return buildUploadFailedRepsonse(job);
    } catch (Exception ex) {
      logger.error(ex.getMessage());
      ex.printStackTrace();
      return buildUploadFailedRepsonse(job);
    } finally {
      em.close();
    }
  }

  /**
   * Builds a Response containing an HTML that calls the UploadListener.uploadFailed javascript handler.
   * 
   * @return HTML that calls the UploadListener.uploadFailed js function
   */
  private Response buildUploadFailedRepsonse(UploadJob job) {
    InputStream is = null;
    try {
      is = getClass().getResourceAsStream("/templates/error.html");
      String html = IOUtils.toString(is, "UTF-8");
      html = html.replaceAll("\\{jobId\\}", job.getId());
      return Response.ok(html).build();
    } catch (IOException ex) {
      logger.error("Unable to build upload failed Response");
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    } finally {
      IOUtils.closeQuietly(is);
    }
  }

  /**
   * Adds a dublinCore metadata catalog to the MediaPackage and returns the grown mediaPackage. JQuery Ajax functions
   * doesn't support multipart/form-data encoding.
   * 
   * @param mp
   *          MediaPackage
   * @param dc
   *          DublinCoreCatalog
   * @return grown MediaPackage XML
   */
  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("addDCCatalog")
  @RestQuery(name = "addDCCatalog", description = "Add a dublincore episode catalog to a given media package using an url", pathParameters = { }, restParameters = {
          @RestParameter(defaultValue = "", description = "The media package as XML", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT),
          @RestParameter(defaultValue = "", description = "DublinCore catalog as XML", isRequired = true, name = "dublinCore", type = RestParameter.Type.STRING), // TODO
                                                                                                                                                                  // should
                                                                                                                                                                  // this
                                                                                                                                                                  // be
                                                                                                                                                                  // TEXT??
          @RestParameter(defaultValue = "dublincore/episode", description = "DublinCore Flavor", isRequired = false, name = "flavor", type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "")
  public Response addDCCatalog(@FormParam("mediaPackage") String mp, @FormParam("dublinCore") String dc,
          @FormParam("flavor") String flavor) {
    MediaPackageElementFlavor dcFlavor = MediaPackageElements.EPISODE;
    if (flavor != null) {
      try {
        dcFlavor = MediaPackageElementFlavor.parseFlavor(flavor);
      } catch (IllegalArgumentException e) {
        logger.warn("Unable to set dublin core flavor to {}, using {} instead", flavor, MediaPackageElements.EPISODE);
      }
    }
    try {
      MediaPackage mediaPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().loadFromXml(mp); // @FormParam("mediaPackage")
      // MediaPackage
      // mp
      // yields
      // Exception
      mediaPackage = ingestService.addCatalog(IOUtils.toInputStream(dc, "UTF-8"), "dublincore.xml", dcFlavor,
              mediaPackage);
      return Response.ok(mediaPackage).build();
    } catch (Exception e) {
      logger.error(e.getMessage());
      e.printStackTrace();
      return Response.serverError().build();
    }
  }

  /**
   * Returns information about the progress of a file upload as a JSON string. Returns 404 if upload job id doesn't
   * exists Returns 500 if something goes wrong unexpectedly
   * 
   * TODO cache UploadJobs because endpoint is asked periodically so that not each request yields a DB query operation
   * 
   * @param jobId
   * @return progress JSON string
   */
  @SuppressWarnings("unchecked")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("getProgress/{jobId}")
  public Response getProgress(@PathParam("jobId") String jobId) throws NotFoundException {
    // try to get UploadJob, responde 404 if not successful
    UploadJob job = null;
    if (jobs.containsKey(jobId)) {
      job = jobs.get(jobId);
    } else {
      throw new NotFoundException("Job not found");
    }
    /*
     * String json = "{total:" + Long.toString(job.getBytesTotal()) + ", received:" +
     * Long.toString(job.getBytesReceived()) + "}"; return Response.ok(json).build();
     */
    JSONObject out = new JSONObject();
    out.put("filename", job.getFilename());
    out.put("total", Long.toString(job.getBytesTotal()));
    out.put("received", Long.toString(job.getBytesReceived()));
    return Response.ok(out.toJSONString()).header("Content-Type", MediaType.APPLICATION_JSON).build();
  }

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

  // CHECKSTYLE:OFF
  private String generateDocs(String serviceUrl) {
    DocRestData data = new DocRestData("ingestservice", "Ingest Service", serviceUrl, notes);

    // abstract
    data.setAbstract("This service creates and augments Matterhorn media packages that include media tracks, metadata catalogs and attachments.");

    // createMediaPackage
    RestEndpoint endpoint = new RestEndpoint("createMediaPackage", RestEndpoint.Method.GET, "/createMediaPackage",
            "Create an empty media package");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addStatus(org.opencastproject.util.doc.Status.ok("Returns media package"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.error(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, endpoint);

    // get workflow instance
    endpoint = new RestEndpoint("getWorkflowInstance", RestEndpoint.Method.GET, "/getWorkflowInstance/{id}.xml",
            "Get un updated workflow instance");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addPathParam(new Param("id", Param.Type.STRING, null, "The ID of the given workflow instance"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.ok("Returns workflow instance"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.error(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, endpoint);

    // addTrack (URL)
    endpoint = new RestEndpoint("addTrackURL", RestEndpoint.Method.POST, "/addTrack",
            "Add a media track to a given media package using an URL");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addRequiredParam(new Param("url", Param.Type.STRING, null, "The location of the media"));
    endpoint.addRequiredParam(new Param("flavor", Param.Type.STRING, null, "The kind of media"));
    endpoint.addRequiredParam(new Param("mediaPackage", Param.Type.TEXT, null, "The media package as XML"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.ok("Returns augmented media package"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.error(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // addTrack (InputStream)
    endpoint = new RestEndpoint("addTrackInputStream", RestEndpoint.Method.POST, "/addTrack",
            "Add a media track to a given media package using an input stream");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addBodyParam(true, null, "The media track file");
    endpoint.addRequiredParam(new Param("flavor", Param.Type.STRING, null, "The kind of media track"));
    endpoint.addRequiredParam(new Param("mediaPackage", Param.Type.TEXT, null, "The media package as XML"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.ok("Returns augmented media package"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.badRequest(null));
    endpoint.addStatus(org.opencastproject.util.doc.Status.error(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // addCatalog (URL)
    endpoint = new RestEndpoint("addCatalogURL", RestEndpoint.Method.POST, "/addCatalog",
            "Add a metadata catalog to a given media package using an URL");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addRequiredParam(new Param("url", Param.Type.STRING, null, "The location of the catalog"));
    endpoint.addRequiredParam(new Param("flavor", Param.Type.STRING, null, "The kind of catalog"));
    endpoint.addRequiredParam(new Param("mediaPackage", Param.Type.TEXT, null, "The media package as XML"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.ok("Returns augmented media package"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.error(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // addCatalog (InputStream)
    endpoint = new RestEndpoint("addCatalogInputStream", RestEndpoint.Method.POST, "/addCatalog",
            "Add a metadata catalog to a given media package using an input stream");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addBodyParam(true, null, "The metadata catalog file");
    endpoint.addRequiredParam(new Param("flavor", Param.Type.STRING, null, "The kind of media catalog"));
    endpoint.addRequiredParam(new Param("mediaPackage", Param.Type.TEXT, null, "The media package as XML"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.ok("Returns augmented media package"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.badRequest(null));
    endpoint.addStatus(org.opencastproject.util.doc.Status.error(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // addDCCatalog (String)
    endpoint = new RestEndpoint("addDCCatalog", RestEndpoint.Method.POST, "/addDCCatalog",
            "Add a dublincore episode catalog to a given media package using an url");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addRequiredParam(new Param("mediaPackage", Param.Type.TEXT, null, "The media package as XML"));
    endpoint.addRequiredParam(new Param("dublinCore", Param.Type.STRING, null, "DublinCore catalog as XML"));
    endpoint.addOptionalParam(new Param("flavor", Param.Type.STRING, "dublincore/episode", "DublinCore Flavor"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.ok("Returns augmented media package"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.error(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // addAttachment (InputStream)
    endpoint = new RestEndpoint("addAttachmentInputStream", RestEndpoint.Method.POST, "/addAttachment",
            "Add an attachment to a given media package using an input stream");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addBodyParam(true, null, "The attachment file");
    endpoint.addRequiredParam(new Param("flavor", Param.Type.STRING, null, "The kind of attachment"));
    endpoint.addRequiredParam(new Param("mediaPackage", Param.Type.TEXT, null, "The media package as XML"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.ok("Returns augmented media package"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.badRequest(null));
    endpoint.addStatus(org.opencastproject.util.doc.Status.error(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // addAttachment (URL)
    endpoint = new RestEndpoint("addAttachmentURL", RestEndpoint.Method.POST, "/addAttachment",
            "Add an attachment to a given media package using an URL");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addRequiredParam(new Param("url", Param.Type.STRING, null, "The location of the attachment"));
    endpoint.addRequiredParam(new Param("flavor", Param.Type.STRING, null, "The kind of attachment"));
    endpoint.addRequiredParam(new Param("mediaPackage", Param.Type.TEXT, null, "The media package as XML"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.ok("Returns augmented media package"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.error(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // ingest
    endpoint = new RestEndpoint("ingest", RestEndpoint.Method.POST, "/ingest",
            "Ingest the completed media package into the system, retrieving all URL-referenced files");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addRequiredParam(new Param("mediaPackage", Param.Type.TEXT, null, "The ID of the given media package"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.ok("Returns the media package"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.error(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // ingest with workflow
    endpoint = new RestEndpoint(
            "ingest",
            RestEndpoint.Method.POST,
            "/ingest/{wdID}",
            "Ingest the completed media package into the system, retrieving all URL-referenced files, and starting a specified workflow");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addPathParam(new Param("wdID", Param.Type.STRING, null, "Workflow definition id"));
    endpoint.addRequiredParam(new Param("mediaPackage", Param.Type.TEXT, null, "The ID of the given media package"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.ok("Returns the media package"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.error(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // discardMediaPackage
    endpoint = new RestEndpoint("discardMediaPackage", RestEndpoint.Method.POST, "/discardMediaPackage",
            "Discard a media package");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addRequiredParam(new Param("mediaPackage", Param.Type.TEXT, null, "Given media package to be destroyed"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.ok(null));
    endpoint.addStatus(org.opencastproject.util.doc.Status.error(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // addMediaPackage
    endpoint = new RestEndpoint("addMediaPackage", RestEndpoint.Method.POST, "/addMediaPackage",
            "Create media package from a media tracks and optional Dublin Core metadata fields");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addBodyParam(true, null, "The media track file");
    endpoint.addRequiredParam(new Param("flavor", Param.Type.STRING, null, "The kind of media track"));
    endpoint.addOptionalParam(new Param("abstract", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("accessRights", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("available", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("contributor", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("coverage", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("created", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("creator", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("date", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("description", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("extent", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("format", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("identifier", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("isPartOf", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("isReferencedBy", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("isReplacedBy", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("language", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("license", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("publisher", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("relation", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("replaces", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("rights", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("rightsHolder", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("source", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("spatial", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("subject", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("temporal", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("title", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("type", Param.Type.STRING, null, "Metadata value"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.ok("Returns augmented media package"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.badRequest(null));
    endpoint.addStatus(org.opencastproject.util.doc.Status.error(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // addMediaPackage with workflow id
    endpoint = new RestEndpoint("addMediaPackage", RestEndpoint.Method.POST, "/addMediaPackage/{wdID}",
            "Create media package from a media tracks and optional Dublin Core metadata fields");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addBodyParam(true, null, "The media track file");
    endpoint.addRequiredParam(new Param("flavor", Param.Type.STRING, null, "The kind of media track"));
    endpoint.addOptionalParam(new Param("abstract", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("accessRights", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("available", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("contributor", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("coverage", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("created", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("creator", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("date", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("description", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("extent", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("format", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("identifier", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("isPartOf", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("isReferencedBy", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("isReplacedBy", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("language", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("license", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("publisher", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("relation", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("replaces", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("rights", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("rightsHolder", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("source", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("spatial", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("subject", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("temporal", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("title", Param.Type.STRING, null, "Metadata value"));
    endpoint.addOptionalParam(new Param("type", Param.Type.STRING, null, "Metadata value"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.ok("Returns augmented media package"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.badRequest(null));
    endpoint.addStatus(org.opencastproject.util.doc.Status.error(null));
    endpoint.addPathParam(new Param("wdID", Param.Type.STRING, null, "Workflow definition id"));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // addZippedMediaPackage
    endpoint = new RestEndpoint(
            "addZippedMediaPackage",
            RestEndpoint.Method.POST,
            "/addZippedMediaPackage",
            "Create media package from a compressed file containing a manifest.xml document and all media tracks, metadata catalogs and attachments");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addOptionalParam(new Param(WORKFLOW_INSTANCE_ID_PARAM, Type.STRING, null,
            "The workflow instance ID to associate with this zipped mediapackage"));
    endpoint.addRequiredParam(new Param(WORKFLOW_DEFINITION_ID_PARAM, Type.STRING, "full",
            "The workflow definition ID to run on this mediapackage"));
    endpoint.addBodyParam(true, null, "The compressed (application/zip) media package file");
    endpoint.addStatus(org.opencastproject.util.doc.Status.ok(null));
    endpoint.addStatus(org.opencastproject.util.doc.Status.error(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // createUploadJobHtml
    /* having '.html' in the URL yields Exception in Rest Docs Utility */
    /*
     * endpoint = new RestEndpoint( "createUploadJobHtml", RestEndpoint.Method.GET, "/filechooser-local.html",
     * "Creates an upload job and returns an html form ready to submit the selected file to /addTrackMonitored with the newly created upload job Id."
     * ); endpoint.addFormat(new Format("HTML", "HTML form for submitting the file with the newly created upload job",
     * null)); endpoint.addStatus(org.opencastproject.util.doc.Status.ok(null));
     * endpoint.addStatus(org.opencastproject.util.doc.Status.error(null)); endpoint.setTestForm(RestTestForm.auto());
     * data.addEndpoint(RestEndpoint.Type.READ, endpoint);
     */

    // addTrackMonitored
    /*
     * doc deactivated, MH-6634 endpoint = new RestEndpoint( "addTrackMonitored", RestEndpoint.Method.POST,
     * "/addTrackMonitored/{jobId}",
     * "Adds a track to the specified MediaPackage while counting the bytes received during the upload of the file. POSTs to this method must be of type multipart/form-data."
     * ); endpoint.addFormat(new Format( "HTML",
     * "HTML that triggers a javascript function in the parent site (upload form lives in an iframe) to indicate the POST to this method was successfully finished."
     * , null)); endpoint.addPathParam(new Param("jobId", Param.Type.STRING, null, "Upload job id"));
     * endpoint.addRequiredParam(new Param("mediaPackage", Param.Type.STRING, null,
     * "Id of the MediaPackage to which the file should be added")); endpoint.addRequiredParam(new Param("flavor",
     * Param.Type.STRING, null,
     * "The flavor of the file in the MediaPackage (eg. presenter/source, presentation/source etc)"));
     * endpoint.addBodyParam(false, "presenter/source", "flavor : "); endpoint.addBodyParam(true, null,
     * "file : binary content of the uploaded file"); endpoint.addStatus(org.opencastproject.util.doc.Status.ok(null));
     * endpoint.addStatus(org.opencastproject.util.doc.Status.badRequest(null));
     * endpoint.addStatus(org.opencastproject.util.doc.Status.error(null)); endpoint.setTestForm(RestTestForm.auto());
     * data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);
     */

    // getUploadProgress
    /*
     * doc deactivated, MH-6634 endpoint = new RestEndpoint("getUploadProgress", RestEndpoint.Method.GET,
     * "/getUploadProgress/{jobId}",
     * "Returns a JSON object reporting the status of the upload with the provided upload job id.");
     * endpoint.addFormat(new Format("JSON",
     * "JSON object reporting the status of the upload with the provided upload job id.", null));
     * endpoint.addPathParam(new Param("jobId", Param.Type.STRING, null, "Upload job id."));
     * endpoint.addStatus(org.opencastproject.util.doc.Status.ok(null));
     * endpoint.addStatus(org.opencastproject.util.doc.Status.badRequest(null));
     * endpoint.addStatus(org.opencastproject.util.doc.Status.error(null)); endpoint.setTestForm(RestTestForm.auto());
     * data.addEndpoint(RestEndpoint.Type.READ, endpoint);
     */

    // TODO: v v v --- check the documentation and implementation of the existing methods --- v v v

    // // addTrackMonitored (InputStream)
    // endpoint = new RestEndpoint(
    // "addTrackMonitored",
    // RestEndpoint.Method.POST,
    // "/addTrackMonitored",
    // "Asynchronously add a media track to a given media package using an input stream. Upload progress can be polled with /getUploadProgress");
    // endpoint.addFormat(new Format("XML", null, null));
    // endpoint.addRequiredParam(new Param("mediaPackage", Param.Type.STRING, null, "The media package as XML"));
    // endpoint.addBodyParam(true, null, "The media track file");
    // endpoint.addStatus(org.opencastproject.util.doc.Status.OK("Returns augmented media package"));
    // endpoint.addStatus(org.opencastproject.util.doc.Status.BAD_REQUEST(null));
    // endpoint.addStatus(org.opencastproject.util.doc.Status.ERROR(null));
    // endpoint.setTestForm(RestTestForm.auto());
    // data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);
    //
    // // getUploadProgress
    // endpoint = new RestEndpoint("getUploadProgress", RestEndpoint.Method.GET, "/getUploadProgress/{mpId}/{filename}",
    // "Get the progress of a file upload");
    // endpoint.addFormat(new Format("JSON", null, null));
    // endpoint.addPathParam(new Param("mpId", Param.Type.STRING, null, "The media package ID"));
    // endpoint.addPathParam(new Param("filename", Param.Type.STRING, null, "The name of the file"));
    // endpoint.addStatus(org.opencastproject.util.doc.Status
    // .OK("Returns the total and currently received number of bytes"));
    // endpoint.setTestForm(RestTestForm.auto());
    // data.addEndpoint(RestEndpoint.Type.READ, endpoint);

    return DocUtil.generate(data);
  }
  // CHECKSTYLE:ON
}
