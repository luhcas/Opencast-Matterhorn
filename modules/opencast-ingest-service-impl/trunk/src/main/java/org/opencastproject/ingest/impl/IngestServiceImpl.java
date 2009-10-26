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
package org.opencastproject.ingest.impl;

import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.media.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilder;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.media.mediapackage.UnsupportedElementException;
import org.opencastproject.media.mediapackage.MediaPackageElement.Type;
import org.opencastproject.media.mediapackage.handle.Handle;
import org.opencastproject.media.mediapackage.handle.HandleBuilder;
import org.opencastproject.media.mediapackage.handle.HandleBuilderFactory;
import org.opencastproject.media.mediapackage.handle.HandleException;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;

import org.apache.commons.io.FileUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Creates and augments Matterhorn MediaPackages. Stores media into the Working File Repository.
 */
public class IngestServiceImpl implements IngestService, ManagedService {
  private static final Logger logger = LoggerFactory.getLogger(IngestServiceImpl.class);
  private MediaPackageBuilder builder = null;
  private HandleBuilder handleBuilder = null;
  private WorkingFileRepository repo;
  //private MediaInspectionService inspection;
  private String tempFolder;
  private String fs;

  public IngestServiceImpl() {
    logger.info("Ingest Service started.");
    builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    handleBuilder = HandleBuilderFactory.newInstance().newHandleBuilder();
    fs = File.separator;
    tempFolder = System.getProperty("java.io.tmpdir") + "opencast" + fs + "ingest-temp" + fs;
  }

  @SuppressWarnings("unchecked")
  public void updated(Dictionary props) throws ConfigurationException {
    // Update any configuration properties here
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.ingest.api.IngestService#addMediaPackage(java.io.InputStream)
   */
  public MediaPackage addMediaPackage(InputStream mediaPackage) throws Exception {
    // locally unzip the mediaPackage
    String tempPath = tempFolder + UUID.randomUUID().toString();
    ZipInputStream zipStream = new ZipInputStream(mediaPackage);
    ZipEntry entry = null;
    try {
      ArrayList<String> allFiles = new ArrayList<String>();
      createDirectory(tempPath);
      while ((entry = zipStream.getNextEntry()) != null) {
        String fileName = entry.getName();
        logger.info("Unzipping " + fileName);
        allFiles.add(fileName);
        FileOutputStream fout = new FileOutputStream(tempPath + File.separator + fileName);
        byte[] buffer = new byte[1024];
        int trueCount;
        while((trueCount = zipStream.read(buffer))!=-1) {
          fout.write(buffer, 0, trueCount);
        }
        zipStream.closeEntry();
        fout.close();
      }
      zipStream.close();
    } catch (FileNotFoundException e) {
      logger.error("Error while decompressing media package! Files could not be written. "+e.getMessage());
      throw (e);
    } catch (IOException e) {
      logger.error("Error while decompressing media package! "+e.getMessage());
      throw (e);
    }
    // check media package and write data to file repo
    File manifest = new File(tempPath + File.separator + "manifest.xml");
    MediaPackage mp;
    try {
      builder.setSerializer(new DefaultMediaPackageSerializerImpl(new File(tempPath)));
      mp = builder.loadFromManifest(manifest.toURL().openStream());
      mp.renameTo(handleBuilder.createNew(manifest.toURL()));
      builder.createNew();
      for (MediaPackageElement element : mp.elements()) {
        element.setIdentifier(UUID.randomUUID().toString());
        if (element.getElementType() == Type.Track) {
          // TODO: inspection and extension of missing metadata
          //AbstractMediaPackageElement el = (AbstractMediaPackageElement)element;
          //Track t = inspection.inspect(element.getURL());
        }
        String filename = element.getURL().getFile();
        filename = filename.substring(filename.lastIndexOf("/"));
        repo.put(mp.getIdentifier().getLocalName(), 
                element.getIdentifier(), 
                filename, 
                element.getURL().openStream());
      }
      removeDirectory(tempFolder);
    } catch (Exception e) {
      logger.error("Ingest service: Failed to ingest media package!");
      throw (e);
    }
    // broadcast event
    if (eventAdmin != null) {
      Dictionary<String, MediaPackage> properties = new Hashtable<String, MediaPackage>();
      Event event = new Event("org/opencastproject/ingest/INGEST_DONE", properties);
      eventAdmin.postEvent(event);
    } else {
      logger.error("Ingest service: Broadcasting event failed - Event admin not available");
      // throw new NullPointerException();
    }

    return mp;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.ingest.api.IngestService#createMediaPackage()
   */
  public MediaPackage createMediaPackage() throws MediaPackageException,
          org.opencastproject.util.ConfigurationException, HandleException {
    MediaPackage mediaPackage;
    Handle h = handleBuilder.createNew();
    try {
      mediaPackage = builder.createNew(h);
    } catch (MediaPackageException e) {
      logger.error("INGEST:Failed to create media package " + e.getLocalizedMessage());
      throw e;
    }
    return mediaPackage;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.ingest.api.IngestService#addMediaPackageTrack( URL,
   *      org.opencastproject.media.mediapackage.MediaPackageElementFlavor flavor, MediaPackage)
   */
  public MediaPackage addTrack(URL url, MediaPackageElementFlavor flavor, MediaPackage mediaPackage)
          throws MediaPackageException, UnsupportedElementException {
    String elementId = UUID.randomUUID().toString();
    addContentToPackage(mediaPackage, elementId, url, MediaPackageElement.Type.Track, flavor);
    return mediaPackage;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.ingest.api.IngestService#addMediaPackageTrack( InputStream,
   *      org.opencastproject.media.mediapackage.MediaPackageElementFlavor flavor, MediaPackage)
   */
  public MediaPackage addTrack(InputStream file, MediaPackageElementFlavor flavor, MediaPackage mediaPackage)
          throws MediaPackageException, UnsupportedElementException, MalformedURLException {
    String elementId = UUID.randomUUID().toString();
    addContentToPackage(mediaPackage, elementId, file, MediaPackageElement.Type.Track, flavor);
    return mediaPackage;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.ingest.api.IngestService#addMediaPackageCatalog( URL,
   *      org.opencastproject.media.mediapackage.MediaPackageElementFlavor flavor, MediaPackage)
   */
  public MediaPackage addCatalog(URL url, MediaPackageElementFlavor flavor, MediaPackage mediaPackage)
          throws MediaPackageException, UnsupportedElementException {
    String elementId = UUID.randomUUID().toString();
    addContentToPackage(mediaPackage, elementId, url, MediaPackageElement.Type.Catalog, flavor);
    return mediaPackage;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.ingest.api.IngestService#addMediaPackageCatalog( InputStream,
   *      org.opencastproject.media.mediapackage.MediaPackageElementFlavor flavor, MediaPackage)
   */
  public MediaPackage addCatalog(InputStream file, MediaPackageElementFlavor flavor, MediaPackage mediaPackage)
          throws MediaPackageException, UnsupportedElementException, MalformedURLException {
    String elementId = UUID.randomUUID().toString();
    addContentToPackage(mediaPackage, elementId, file, MediaPackageElement.Type.Catalog, flavor);
    return mediaPackage;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.ingest.api.IngestService#addMediaPackageAttachment ( URL,
   *      org.opencastproject.media.mediapackage.MediaPackageElementFlavor flavor, MediaPackage)
   */
  public MediaPackage addAttachment(URL url, MediaPackageElementFlavor flavor, MediaPackage mediaPackage)
          throws MediaPackageException, UnsupportedElementException {
    String elementId = UUID.randomUUID().toString();
    addContentToPackage(mediaPackage, elementId, url, MediaPackageElement.Type.Attachment, flavor);
    return mediaPackage;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.ingest.api.IngestService#addMediaPackageAttachment ( InputStream,
   *      org.opencastproject.media.mediapackage.MediaPackageElementFlavor flavor, MediaPackage)
   */
  public MediaPackage addAttachment(InputStream file, MediaPackageElementFlavor flavor, MediaPackage mediaPackage)
          throws MediaPackageException, UnsupportedElementException, MalformedURLException {
    String elementId = UUID.randomUUID().toString();
    addContentToPackage(mediaPackage, elementId, file, MediaPackageElement.Type.Attachment, flavor);
    return mediaPackage;
  }

  /**
   * {@inheritDoc}
   * 
   * @throws Exception
   * @see org.opencastproject.ingest.api.IngestService#ingest(java.lang.String,
   *      org.opencastproject.notification.api.NotificationService)
   */
  public void ingest(MediaPackage mediaPackage) {
    final MediaPackage mp = mediaPackage;
    Thread t = new Thread(new Runnable() {
      public void run() {
        try {
          doIngest(mp);
        } catch (IOException e) {
          logger.error("IngestService: Ingest failed!");
        }
      }
    });
    t.start();
  }

  // actual work of the method ingest()
  private void doIngest(MediaPackage mp) throws IOException {
    String mediaPackageId = mp.getIdentifier().getLocalName();
    if (mp == null)
      throw new NullPointerException("Ingest Service: no media Package");
    for (MediaPackageElement element : mp.getTracks()) {
      URL url = element.getURL();
      String elementId = element.getIdentifier();
      // element = inspection.inspect(url);
      repo.put(mediaPackageId, elementId, getInputStreamFromURL(url));
      URL newURL = repo.getURL(mediaPackageId, elementId);
      element.setURL(newURL);
    }
    for (MediaPackageElement element : mp.getAttachments()) {
      URL url = element.getURL();
      String elementId = element.getIdentifier();
      repo.put(mediaPackageId, elementId, getInputStreamFromURL(url));
      URL newURL = repo.getURL(mediaPackageId, elementId);
      element.setURL(newURL);
    }
    for (MediaPackageElement element : mp.getCatalogs()) {
      URL url = element.getURL();
      String elementId = element.getIdentifier();
      repo.put(mediaPackageId, elementId, getInputStreamFromURL(url));
      URL newURL = repo.getURL(mediaPackageId, elementId);
      element.setURL(newURL);
    }

    if (eventAdmin != null) {
      Dictionary<String, MediaPackage> properties = new Hashtable<String, MediaPackage>();
      properties.put("mediaPackageId", mp);
      Event event = new Event("org/opencastproject/ingest/INGEST_DONE", properties);
      eventAdmin.postEvent(event);
    } else {
      logger.error("Ingest service: Broadcasting event failed - Event admin not available");
      // Test will fail
      // throw new NullPointerException();
    }

  }

  private EventAdmin eventAdmin;

  protected void setEventAdmin(EventAdmin eventAdmin) {
    this.eventAdmin = eventAdmin;
  }

  protected void unsetEventAdmin(EventAdmin eventAdmin) {
    this.eventAdmin = null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.ingest.api.IngestService#discardMediaPackage(java.lang.String)
   */
  public void discardMediaPackage(MediaPackage mp) {
    String mediaPackageId = mp.getIdentifier().getLocalName();
    for (MediaPackageElement element : mp.getAttachments()) {
      repo.delete(mediaPackageId, element.getIdentifier());
    }
    for (MediaPackageElement element : mp.getCatalogs()) {
      repo.delete(mediaPackageId, element.getIdentifier());
    }
    for (MediaPackageElement element : mp.getTracks()) {
      repo.delete(mediaPackageId, element.getIdentifier());
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.ingest.api.IngestService#setWorkingFileRepository(
   * org.opencastproject.workingfilerepository.api.WorkingFileRepository)
   */
  public void setWorkingFileRepository(WorkingFileRepository repo) {
    this.repo = repo;
  }

  public void setMediaInspection(MediaInspectionService inspection) {
    //this.inspection = inspection;
  }

  private InputStream getInputStreamFromURL(URL url) throws IOException {
    // Create the streams
    URLConnection urlConn;
    try {
      urlConn = url.openConnection();
      return urlConn.getInputStream();
    } catch (IOException e) {
      logger.error("IngestService: unable to read url into stream");
      throw e;
    }
  }

  private MediaPackage addContentToPackage(
          MediaPackage mp, String elementId, URL url, MediaPackageElement.Type type,
          MediaPackageElementFlavor flavor) 
  throws MediaPackageException, UnsupportedElementException {
    try {
      MediaPackageElement mpe = mp.add(url, type, flavor);
      mpe.setIdentifier(elementId);
    } catch (MediaPackageException mpe) {
      logger.error("IngestService: Failed to access media package for ingest");
      throw mpe;
    } catch (UnsupportedElementException uee) {
      logger.error("IngestService: Unsupported element for ingest");
      throw uee;
    }
    return mp;
  }

  private MediaPackage addContentToPackage(MediaPackage mp, String elementId, InputStream file,
          MediaPackageElement.Type type, MediaPackageElementFlavor flavor) 
  throws MediaPackageException, UnsupportedElementException, MalformedURLException {
    repo.put(mp.getIdentifier().getLocalName(), elementId, file);
    URL url = repo.getURL(mp.getIdentifier().getLocalName(), elementId);
    return addContentToPackage(mp, elementId, url, type, flavor);
  }

  private void createDirectory(String dir) {
    File f = new File(dir);
    if (!f.exists()) {
      try {
        FileUtils.forceMkdir(f);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
  private void removeDirectory(String dir) {
    File f = new File(dir);
    if (f.exists()) {
      try {
        FileUtils.deleteDirectory(f);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
