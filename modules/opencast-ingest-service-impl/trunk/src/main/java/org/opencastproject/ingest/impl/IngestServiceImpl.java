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
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;
import org.opencastproject.workingfilerepository.impl.WorkingFileRepositoryImpl;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.UUID;

/**
 * Creates and augments Matterhorn MediaPackages.
 */
public class IngestServiceImpl implements IngestService, ManagedService {
  private static final Logger logger = LoggerFactory.getLogger(IngestServiceImpl.class);

  // FIXME: This implementation assumes either a single instance of this service, or stick sessions behind a load
  // balancer
  private HashMap<String, MediaPackage> localMediaPackages = new HashMap<String, MediaPackage>();

  private MediaPackageBuilderFactory factory = null;
  private MediaPackageBuilder builder = null;

  // private MediaInspectionService inspection = new MediaInspectionServiceImpl();

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
   * @see org.opencastproject.ingest.api.IngestService#addMediaPackage(org.w3c.dom.Document)
   */
  public String addMediaPackage(InputStream MediaPackageManifest) throws MediaPackageException {
    String id = UUID.randomUUID().toString();
    MediaPackage mp = builder.loadFromManifest(MediaPackageManifest);
    localMediaPackages.put(id, mp);
    return id;
  }

  /**
   * {@inheritDoc}
   * 
   * @throws MediaPackageException
   * @see org.opencastproject.ingest.api.IngestService#createMediaPackage()
   */
  public String createMediaPackage() throws MediaPackageException {
    String id = UUID.randomUUID().toString();
    MediaPackage mediaPackage;
    try {
      mediaPackage = builder.createNew();
    } catch (MediaPackageException e) {
      logger.error("INGEST:Failed to create media package " + e.getLocalizedMessage());
      throw e;
    }
    localMediaPackages.put(id, mediaPackage);
    return id;
  }

  /**
   * {@inheritDoc}
   * 
   * @throws UnsupportedElementException
   * @throws MediaPackageException
   * @see org.opencastproject.ingest.api.IngestService#addMediaPackageTrack( java.net.URI,
   *      org.opencastproject.media.mediapackage.MediaPackageElementFlavor flavor, java.lang.String)
   */
  public String addTrack(URL url, MediaPackageElementFlavor flavor, String mediaPackageId)
          throws MediaPackageException, UnsupportedElementException {
    String elementId = UUID.randomUUID().toString();
    addContentToPackage(mediaPackageId, elementId, url, MediaPackageElement.Type.Track, flavor);
    return elementId;
  }

  /**
   * {@inheritDoc}
   * 
   * @throws UnsupportedElementException
   * @throws MediaPackageException
   * @see org.opencastproject.ingest.api.IngestService#addMediaPackageCatalog( java.net.URI,
   *      org.opencastproject.media.mediapackage.MediaPackageElementFlavor flavor, java.lang.String)
   */
  public String addCatalog(URL url, MediaPackageElementFlavor flavor, String mediaPackageId)
          throws MediaPackageException, UnsupportedElementException {
    String elementId = UUID.randomUUID().toString();
    addContentToPackage(mediaPackageId, elementId, url, MediaPackageElement.Type.Catalog, flavor);
    return elementId;
  }

  /**
   * {@inheritDoc}
   * 
   * @throws UnsupportedElementException
   * @throws MediaPackageException
   * @see org.opencastproject.ingest.api.IngestService#addMediaPackageAttachment( java.net.URI,
   *      org.opencastproject.media.mediapackage.MediaPackageElementFlavor flavor, java.lang.String)
   */
  public String addAttachment(URL url, MediaPackageElementFlavor flavor, String mediaPackageId)
          throws MediaPackageException, UnsupportedElementException {
    String elementId = UUID.randomUUID().toString();
    addContentToPackage(mediaPackageId, elementId, url, MediaPackageElement.Type.Attachment, flavor);
    return elementId;
  }

  /**
   * {@inheritDoc}
   * 
   * @throws Exception
   * @see org.opencastproject.ingest.api.IngestService#ingest(java.lang.String,
   *      org.opencastproject.notification.api.NotificationService)
   */
  public void ingest(String mediaPackageId) {
    final String mpId = mediaPackageId;
    Thread t = new Thread(new Runnable() {
      public void run() {
        doIngest(mpId);
      }
    });
    t.start();
  }

  // actual work of the method ingest()
  private void doIngest(String mediaPackageId) {
    MediaPackage mp = localMediaPackages.get(mediaPackageId);
    if (mp == null)
      throw new NullPointerException("Ingest Service: no media Package");
    // FIXME -- this must be injected by the container. Never "new" a service.
    WorkingFileRepository repo = new WorkingFileRepositoryImpl();
    for (MediaPackageElement element : mp.getTracks()) {
      URL url = element.getURL();
      String elementId = element.getIdentifier();
      // TODO deal with inspection
      // element = inspection.inspect(url);
      repo.put(mediaPackageId, elementId, getInputStreamFromURL(url));
      // --------- TODO deal with the repository url ---------------
      // URL newURL = repo.getUrl(mediaPackageId, elementId);
      // element.setURL(newURL);
    }
    for (MediaPackageElement element : mp.getAttachments()) {
      URL url = element.getURL();
      String elementId = element.getIdentifier();
      repo.put(mediaPackageId, elementId, getInputStreamFromURL(url));
      // --------- TODO deal with the repository url ---------------
      // URL newURL = repo.getUrl(mediaPackageId, elementId);
      // element.setURL(newURL);
    }
    for (MediaPackageElement element : mp.getCatalogs()) {
      URL url = element.getURL();
      String elementId = element.getIdentifier();
      repo.put(mediaPackageId, elementId, getInputStreamFromURL(url));
      // --------- TODO deal with the repository url ---------------
      // URL newURL = repo.getUrl(mediaPackageId, elementId);
      // element.setURL(newURL);
    }
    // --- TODO broadcast event ---
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.ingest.api.IngestService#discardMediaPackage(java.lang.String)
   */
  public void discardMediaPackage(String mediaPackageId) {
    localMediaPackages.remove(mediaPackageId);
  }

  private InputStream getInputStreamFromURL(URL url) {
    // Create the streams
    URLConnection urlConn;
    try {
      urlConn = url.openConnection();
      return urlConn.getInputStream();
    } catch (IOException e) {
      logger.error("IngestService: unable to read url into stream");
      // FIXME: calling methods will happily put this null into the file repo, and the client will never know that there
      // was a problem.
      return null;
    }
  }

  private void addContentToPackage(String mediaPackageId, String elementId, URL url, MediaPackageElement.Type type,
          MediaPackageElementFlavor flavor) throws MediaPackageException, UnsupportedElementException {
    MediaPackage mp = localMediaPackages.get(mediaPackageId);
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
  }
}
