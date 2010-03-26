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
import org.opencastproject.media.mediapackage.EName;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilder;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.MediaPackageElements;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;
import org.opencastproject.workflow.api.WorkflowService;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.IOUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
//import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
//import javax.persistence.Query;
//import javax.persistence.RollbackException;
import javax.persistence.spi.PersistenceProvider;
import javax.servlet.http.HttpServletRequest;
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
  protected PersistenceProvider persistenceProvider;
  protected Map<String, Object> persistenceProperties;
  protected EntityManagerFactory emf = null;
  private String serverURL = null;

  // For the progress bar -1 bug workaround, keeping UploadJobs in memory rather than saving them using JPA
  private HashMap<String,UploadJob> jobs;

  public IngestRestService() {
    factory = MediaPackageBuilderFactory.newInstance();
    builder = factory.newMediaPackageBuilder();
    jobs = new HashMap<String,UploadJob>();
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

  @SuppressWarnings("unchecked")
  public void setPersistenceProperties(Map persistenceProperties) {
    this.persistenceProperties = persistenceProperties;
  }

  public void activate(ComponentContext context) {
    if (context == null || context.getBundleContext().getProperty("serverURL") == null) {
      serverURL = UrlSupport.DEFAULT_BASE_URL;
    } else {
      serverURL = context.getBundleContext().getProperty("serverURL");
    }
    try {
      emf = persistenceProvider
              .createEntityManagerFactory("org.opencastproject.ingest.endpoint", persistenceProperties);
    } catch (Exception e) {
      logger.error("Unable to initialize JPA EntityManager: " + e.getMessage());
    }
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
  @Path("addTrack")
  public Response addMediaPackageTrack(@FormParam("url") String url, @FormParam("flavor") String flavor,
          @FormParam("mediaPackage") String mpx) {
    try {
      MediaPackage mp = builder.loadFromXml(mpx);
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
  @Path("addCatalog")
  public Response addMediaPackageCatalog(@FormParam("url") String url, @FormParam("flavor") String flavor,
          @FormParam("mediaPackage") String mpx) {
    try {
      MediaPackage mp = builder.loadFromXml(mpx);
      MediaPackage resultingMediaPackage = ingestService.addCatalog(new URI(url), MediaPackageElementFlavor
              .parseFlavor(flavor), mp);
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

  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("addAttachment")
  public Response addMediaPackageAttachment(@FormParam("url") String url, @FormParam("flavor") String flavor,
          @FormParam("mediaPackage") String mpx) {
    try {
      MediaPackage mp = builder.loadFromXml(mpx);
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
            if ("flavor".equals(fieldName)) {
              String flavorString = Streams.asString(item.openStream());
              if (flavorString != null) {
                flavor = MediaPackageElementFlavor.parseFlavor(flavorString);
              }
            } else if ("mediaPackage".equals(fieldName)) {
              mp = builder.loadFromXml(item.openStream());
            }
          } else {
            fileName = item.getName();
            in = item.openStream();
          }
        }
        switch (type) {
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
      logger.warn(e.getMessage(), e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("addMediaPackage")
  public Response addMediaPackage(@Context HttpServletRequest request) {
    return _addMediaPackage(request, WorkflowService.DEFAULT_WORKFLOW_ID);
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("addMediaPackage/{wdID}")
  public Response addMediaPackage(@Context HttpServletRequest request, @PathParam("wdID") String wdID) {
    return _addMediaPackage(request, wdID);
  }

  private Response _addMediaPackage(HttpServletRequest request, String wdID) {
    MediaPackageElementFlavor flavor = null;
    try {
      MediaPackage mp = ingestService.createMediaPackage();
      DublinCoreCatalog dcc = dublinCoreService.newInstance();
      if (ServletFileUpload.isMultipartContent(request)) {
        for (FileItemIterator iter = new ServletFileUpload().getItemIterator(request); iter.hasNext();) {
          FileItemStream item = iter.next();
          if (item.isFormField()) {
            String fieldName = item.getFieldName();
            if (fieldName.equals("flavor")) {
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
        ingestService.ingest(mp, wdID);
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
  // @Consumes("application/zip")
  public Response addZippedMediaPackage(InputStream mp) {
    return _addZippedMediaPackage(mp, WorkflowService.DEFAULT_WORKFLOW_ID);
  }

  @POST
  @Path("addZippedMediaPackage/{wdID}")
  public Response addZippedMediaPackage(InputStream mp, @PathParam("wdID") String wdID) {
    return _addZippedMediaPackage(mp, wdID);
  }

  private Response _addZippedMediaPackage(InputStream mp, String wdID) {
    logger.debug("addZippedMediaPackage(InputStream) called.");
    try {
      ingestService.addZippedMediaPackage(mp, wdID);
    } catch (Exception e) {
      logger.warn(e.getMessage());
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
    return Response.ok().build();
  }

  @POST
  @Produces(MediaType.TEXT_HTML)
  @Path("ingest")
  public Response ingest(@FormParam("mediaPackage") String mpx) {
    return _ingest(mpx, WorkflowService.DEFAULT_WORKFLOW_ID);
  }

  @POST
  @Produces(MediaType.TEXT_HTML)
  @Path("ingest/{wdID}")
  public Response ingest(@FormParam("mediaPackage") String mpx, @PathParam("wdID") String wdID) {
    return _ingest(mpx, wdID);
  }

  private Response _ingest(String mpx, String wdID) {
    logger.debug("ingest(MediaPackage): {}", mpx);
    try {
      MediaPackage mp = builder.loadFromXml(mpx);
      ingestService.ingest(mp, wdID);
      return Response.ok(mpx).build();
    } catch (Exception e) {
      logger.warn(e.getMessage());
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  protected UploadJob createUploadJob() throws RuntimeException {
    /*EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      UploadJob job = new UploadJob();
      tx.begin();
      em.persist(job);
      tx.commit();
      return job;
    } catch (RollbackException ex) {
      logger.error(ex.getMessage(), ex);
      tx.rollback();
      throw new RuntimeException(ex);
    } finally {
      em.close();
    }*/
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
  @Path("uploadform.html")
  @Produces(MediaType.TEXT_HTML)
  public Response createUploadJobHtml() {
    try {
      UploadJob job = createUploadJob();
      String html = IOUtils.toString(getClass().getResourceAsStream("/templates/uploadform.html"));
      String uploadURL = serverURL + "/ingest/rest/addElementMonitored/" + job.getId();
      html = html.replaceAll("\\{uploadURL\\}", uploadURL);
      html = html.replaceAll("\\{jobId\\}", job.getId());
      logger.info("New upload job created: " + job.getId());
      jobs.put(job.getId(), job);
      return Response.ok(html).build();
    } catch (Exception ex) {
      logger.warn(ex.getMessage(), ex);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
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
    EntityManager em = emf.createEntityManager();
    try {
      try { // try to get UploadJob, responde 404 if not successful
        //job = em.find(UploadJob.class, jobId);
        if (jobs.containsKey(jobId)) {
          job = jobs.get(jobId);
        } else {
          throw new NoResultException("Job not found");
        }
      } catch (NoResultException e) {
        logger.warn("UploadJob not found for Id: " + jobId);
        return buildUploadFailedRepsonse();
      }
      if (ServletFileUpload.isMultipartContent(request)) {
        ServletFileUpload upload = new ServletFileUpload();
        UploadProgressListener listener = new UploadProgressListener(job, this.emf);
        upload.setProgressListener(listener);
        for (FileItemIterator iter = upload.getItemIterator(request); iter.hasNext();) {
          FileItemStream item = iter.next();
          String fieldName = item.getFieldName();
          if (fieldName.equals("mediaPackage")) {
            mp = builder.loadFromXml(item.openStream());
          } else if ("flavor".equals(fieldName)) {
            String flavorString = Streams.asString(item.openStream());
            if (flavorString != null) {
              flavor = MediaPackageElementFlavor.parseFlavor(flavorString);
            }
          } else if (fieldName.equals("file")) {
            fileName = item.getName();
            logger.debug("receiving track");
            job.setFilename(fileName);
            if ((mp != null) && (flavor != null) && (fileName != null)) {

              // TODO add ability to accept the other kinds of elements (Attachment etc..)

              mp = ingestService.addTrack(item.openStream(), fileName, flavor, mp);
              String html = IOUtils.toString(getClass().getResourceAsStream("/templates/complete.html"));
              html = html.replaceAll("\\{mediaPackage\\}", mp.toXml());
              return Response.ok(html).build();
            }
          }
        }
      } else {
        logger.warn("Job " + job.getId() + ": message is not multipart/form-data encoded");
      }
      return buildUploadFailedRepsonse();
    } catch (Exception ex) {
      logger.error(ex.getMessage());
      ex.printStackTrace();
      return buildUploadFailedRepsonse();
    } finally {
      em.close();
    }
  }

  /**
   * Builds a Response containing an HTML that calls the UploadListener.uploadFailed javascript handler.
   * 
   * @return HTML that calls the UploadListener.uploadFailed js function
   */
  private Response buildUploadFailedRepsonse() {
    try {
      String html = IOUtils.toString(getClass().getResourceAsStream("/templates/error.html"));
      return Response.ok(html).build();
    } catch (IOException ex) {
      logger.error("Unable to build upload failed Response");
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
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
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Path("addDCCatalog")
  public Response addDCCatalog(@FormParam("mediaPackage") String mp, @FormParam("dublinCore") String dc) {
    try {
      MediaPackage mediaPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().loadFromXml(mp); // @FormParam("mediaPackage")
      // MediaPackage
      // mp
      // yields
      // Exception
      mediaPackage = ingestService.addCatalog(IOUtils.toInputStream(dc), "dublinCore.xml", MediaPackageElementFlavor
              .parseFlavor("metadata/dublincore"), mediaPackage);
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
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("getProgress/{JobId}")
  public Response getProgress(@PathParam("JobId") String jobId) {
    EntityManager em = emf.createEntityManager();
    try {
      UploadJob job = null;
      try { // try to get UploadJob, responde 404 if not successful
        //Query q = em.createNamedQuery("UploadJob.getByID");
        //q.setParameter("id", jobId);
        //job = (UploadJob) q.getSingleResult();
        if (jobs.containsKey(jobId)) {
          job = jobs.get(jobId);
        } else {
          throw new NoResultException("Job not found");
        }
      } catch (NoResultException e) {
        logger.warn("UploadJob not found for Id: " + jobId);
        return Response.status(Status.NOT_FOUND).build();
      }
      String json = "{total:" + Long.toString(job.getBytesTotal()) + ", received:"
              + Long.toString(job.getBytesReceived()) + "}";
      return Response.ok(json).build();
    } catch (Exception ex) {
      logger.error(ex.getMessage());
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    } finally {
      em.close();
    }
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    if (docs == null) {
      docs = generateDocs();
    }
    return docs;
  }

  protected String docs;
  private String[] notes = {
          "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
          "If the service is down or not working it will return a status 503, this means the the underlying service is not working and is either restarting or has failed",
          "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In other words, there is a bug! You should file an error report with your server logs from the time when the error occurred: <a href=\"https://issues.opencastproject.org\">Opencast Issue Tracker</a>", };

  // CHECKSTYLE:OFF
  private String generateDocs() {
    DocRestData data = new DocRestData("ingestservice", "Ingest Service", "/ingest/rest", notes);

    // abstract
    data
            .setAbstract("This service creates and augments Matterhorn media packages that include media tracks, metadata catalogs and attachments.");

    // createMediaPackage
    RestEndpoint endpoint = new RestEndpoint("createMediaPackage", RestEndpoint.Method.GET, "/createMediaPackage",
            "Create an empty media package");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addStatus(org.opencastproject.util.doc.Status.OK("Returns media package"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.ERROR(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, endpoint);

    // addTrack (URL)
    endpoint = new RestEndpoint("addTrackURL", RestEndpoint.Method.POST, "/addTrack",
            "Add a media track to a given media package using an URL");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addRequiredParam(new Param("url", Param.Type.STRING, null, "The location of the media"));
    endpoint.addRequiredParam(new Param("flavor", Param.Type.STRING, null, "The kind of media"));
    endpoint.addRequiredParam(new Param("mediaPackage", Param.Type.STRING, null, "The media package as XML"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.OK("Returns augmented media package"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.ERROR(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // addTrack (InputStream)
    endpoint = new RestEndpoint("addTrackInputStream", RestEndpoint.Method.POST, "/addTrack",
            "Add a media track to a given media package using an input stream");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addBodyParam(true, null, "The media track file");
    endpoint.addRequiredParam(new Param("flavor", Param.Type.STRING, null, "The kind of media track"));
    endpoint.addRequiredParam(new Param("mediaPackage", Param.Type.STRING, null, "The media package as XML"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.OK("Returns augmented media package"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.BAD_REQUEST(null));
    endpoint.addStatus(org.opencastproject.util.doc.Status.ERROR(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // addCatalog (URL)
    endpoint = new RestEndpoint("addCatalogURL", RestEndpoint.Method.POST, "/addCatalog",
            "Add a metadata catalog to a given media package using an URL");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addRequiredParam(new Param("url", Param.Type.STRING, null, "The location of the catalog"));
    endpoint.addRequiredParam(new Param("flavor", Param.Type.STRING, null, "The kind of catalog"));
    endpoint.addRequiredParam(new Param("mediaPackage", Param.Type.STRING, null, "The media package as XML"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.OK("Returns augmented media package"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.ERROR(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // addCatalog (InputStream)
    endpoint = new RestEndpoint("addCatalogInputStream", RestEndpoint.Method.POST, "/addCatalog",
            "Add a metadata catalog to a given media package using an input stream");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addBodyParam(true, null, "The metadata catalog file");
    endpoint.addRequiredParam(new Param("flavor", Param.Type.STRING, null, "The kind of media catalog"));
    endpoint.addRequiredParam(new Param("mediaPackage", Param.Type.STRING, null, "The media package as XML"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.OK("Returns augmented media package"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.BAD_REQUEST(null));
    endpoint.addStatus(org.opencastproject.util.doc.Status.ERROR(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // addTrackMonitored (InputStream)
    endpoint = new RestEndpoint(
            "addTrackMonitored",
            RestEndpoint.Method.POST,
            "/addTrackMonitored",
            "Asynchronously add a media track to a given media package using an input stream. Upload progress can be polled with /getUploadProgress");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addRequiredParam(new Param("mediaPackage", Param.Type.STRING, null, "The media package as XML"));
    endpoint.addBodyParam(true, null, "The media track file");
    endpoint.addStatus(org.opencastproject.util.doc.Status.OK("Returns augmented media package"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.BAD_REQUEST(null));
    endpoint.addStatus(org.opencastproject.util.doc.Status.ERROR(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // getUploadProgress
    endpoint = new RestEndpoint("getUploadProgress", RestEndpoint.Method.GET, "/getUploadProgress/{mpId}/{filename}",
            "Get the progress of a file upload");
    endpoint.addFormat(new Format("JSON", null, null));
    endpoint.addPathParam(new Param("mpId", Param.Type.STRING, null, "The media package ID"));
    endpoint.addPathParam(new Param("filename", Param.Type.STRING, null, "The name of the file"));
    endpoint.addStatus(org.opencastproject.util.doc.Status
            .OK("Returns the total and currently received number of bytes"));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, endpoint);

    // addAttachment (URL)
    endpoint = new RestEndpoint("addAttachmentURL", RestEndpoint.Method.POST, "/addAttachment",
            "Add an attachment to a given media package using an URL");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addRequiredParam(new Param("url", Param.Type.STRING, null, "The location of the attachment"));
    endpoint.addRequiredParam(new Param("flavor", Param.Type.STRING, null, "The kind of attachment"));
    endpoint.addRequiredParam(new Param("mediaPackage", Param.Type.STRING, null, "The media package as XML"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.OK("Returns augmented media package"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.ERROR(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // addAttachment (InputStream)
    endpoint = new RestEndpoint("addAttachmentInputStream", RestEndpoint.Method.POST, "/addAttachment",
            "Add an attachment to a given media package using an input stream");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addBodyParam(true, null, "The attachment file");
    endpoint.addRequiredParam(new Param("flavor", Param.Type.STRING, null, "The kind of attachment"));
    endpoint.addRequiredParam(new Param("mediaPackage", Param.Type.STRING, null, "The media package as XML"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.OK("Returns augmented media package"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.BAD_REQUEST(null));
    endpoint.addStatus(org.opencastproject.util.doc.Status.ERROR(null));
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
    endpoint.addStatus(org.opencastproject.util.doc.Status.OK("Returns augmented media package"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.BAD_REQUEST(null));
    endpoint.addStatus(org.opencastproject.util.doc.Status.ERROR(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // addZippedMediaPackage
    endpoint = new RestEndpoint(
            "addZippedMediaPackage",
            RestEndpoint.Method.POST,
            "/addZippedMediaPackage",
            "Create media package from a compressed file containing a manifest.xml document and all media tracks, metadata catalogs and attachments");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addBodyParam(true, null, "The compressed (application/zip) media package file");
    endpoint.addStatus(org.opencastproject.util.doc.Status.OK(null));
    endpoint.addStatus(org.opencastproject.util.doc.Status.ERROR(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    // ingest
    endpoint = new RestEndpoint("ingest", RestEndpoint.Method.POST, "/ingest",
            "Ingest the completed media package into the system, retrieving all URL-referenced files");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addRequiredParam(new Param("mediaPackage", Param.Type.STRING, null, "The ID of the given media package"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.OK("Returns the media package"));
    endpoint.addStatus(org.opencastproject.util.doc.Status.ERROR(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint);

    return DocUtil.generate(data);
  }
  // CHECKSTYLE:ON
}
