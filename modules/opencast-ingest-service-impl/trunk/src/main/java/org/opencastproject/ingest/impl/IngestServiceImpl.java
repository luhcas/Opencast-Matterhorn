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
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilder;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.media.mediapackage.UnsupportedElementException;
import org.opencastproject.media.mediapackage.handle.Handle;
import org.opencastproject.media.mediapackage.handle.HandleBuilderFactory;
import org.opencastproject.media.mediapackage.handle.HandleException;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Dictionary;
import java.util.UUID;

/**
 * Creates and augments Matterhorn MediaPackages. Stores media into the Working File Repository.
 */
public class IngestServiceImpl implements IngestService, ManagedService {
  private static final Logger logger = LoggerFactory.getLogger(IngestServiceImpl.class);
  private MediaPackageBuilderFactory factory = null;
  private MediaPackageBuilder builder = null;
  private WorkingFileRepository repo;

  // private MediaInspectionService inspection;

  public IngestServiceImpl() {
    logger.info("Ingest Service started.");
    factory = MediaPackageBuilderFactory.newInstance();
    builder = factory.newMediaPackageBuilder();
  }

  @SuppressWarnings("unchecked")
  public void updated(Dictionary props) throws ConfigurationException {
    // Update any configuration properties here
  }

  /**
   * {@inheritDoc}
   * 
   * @throws MediaPackageException
   * @see org.opencastproject.ingest.api.IngestService#addMediaPackage(java.io.InputStream)
   */
  public MediaPackage addMediaPackage(InputStream MediaPackageManifest) throws MediaPackageException {
    // String id = UUID.randomUUID().toString();
    return builder.loadFromManifest(MediaPackageManifest);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.ingest.api.IngestService#createMediaPackage()
   */
  public MediaPackage createMediaPackage() throws MediaPackageException,
          org.opencastproject.util.ConfigurationException, HandleException {
    MediaPackage mediaPackage;
    Handle h = HandleBuilderFactory.newInstance().newHandleBuilder().createNew();
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
   * @see org.opencastproject.ingest.api.IngestService#addMediaPackageAttachment( URL,
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
   * @see org.opencastproject.ingest.api.IngestService#addMediaPackageAttachment( InputStream,
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
      // TODO deal with inspection
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
    // --- TODO broadcast event ---
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
   * @see org.opencastproject.ingest.api.IngestService#setWorkingFileRepository(org.opencastproject.workingfilerepository.api.WorkingFileRepository)
   */
  public void setWorkingFileRepository(WorkingFileRepository repo) {
    this.repo = repo;
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

  private MediaPackage addContentToPackage(MediaPackage mp, String elementId, URL url, MediaPackageElement.Type type,
          MediaPackageElementFlavor flavor) throws MediaPackageException, UnsupportedElementException {
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
          MediaPackageElement.Type type, MediaPackageElementFlavor flavor) throws MediaPackageException,
          UnsupportedElementException, MalformedURLException {
    repo.put(mp.getIdentifier().getLocalName(), elementId, file);
    URL url = repo.getURL(mp.getIdentifier().getLocalName(), elementId);
    return addContentToPackage(mp, elementId, url, type, flavor);
  }
}
