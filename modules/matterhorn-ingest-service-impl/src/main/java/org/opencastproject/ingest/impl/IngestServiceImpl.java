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
package org.opencastproject.ingest.impl;

import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.media.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilder;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.media.mediapackage.UnsupportedElementException;
import org.opencastproject.media.mediapackage.identifier.HandleException;
import org.opencastproject.util.ZipUtil;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workspace.api.NotFoundException;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Date;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;

/**
 * Creates and augments Matterhorn MediaPackages. Stores media into the Working File Repository.
 */
public class IngestServiceImpl implements IngestService {
  // TODO CONFIGURATION (tempPath, BUFFER)

  private static final Logger logger = LoggerFactory.getLogger(IngestServiceImpl.class);
  private MediaPackageBuilder builder = null;
  private WorkflowService workflowService;
  private Workspace workspace;
  private String tempFolder;
  private String fs;

  public IngestServiceImpl() {
    logger.info("Ingest Service started.");
    builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    fs = File.separator;
    tempFolder = System.getProperty("java.io.tmpdir");
    if (!tempFolder.endsWith(fs))
      tempFolder += fs;
    tempFolder += "opencast" + fs + "ingest-temp" + fs;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.ingest.api.IngestService#addZippedMediaPackage(java.io.InputStream)
   */
  public WorkflowInstance addZippedMediaPackage(InputStream zipStream) throws Exception {
    return addZippedMediaPackage(zipStream, null);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.ingest.api.IngestService#addZippedMediaPackage(java.io.InputStream, java.lang.String)
   */
  public WorkflowInstance addZippedMediaPackage(InputStream zipStream, String wd) throws Exception {
    // locally unpack the mediaPackage
    String tempPath = tempFolder + UUID.randomUUID().toString();
    // save inputStream to file
    File tempDir = createDirectory(tempPath);
    File f = new File(tempPath + fs + UUID.randomUUID().toString() + ".zip");
    OutputStream out = new FileOutputStream(f);
    int len;
    byte[] buffer = new byte[1024];
    while ((len = zipStream.read(buffer)) > 0)
      out.write(buffer, 0, len);
    out.close();
    zipStream.close();
    // unpack
    ZipUtil.unzip(f, tempDir);
    f.delete();
    // check media package and write data to file repo
    File manifest = getManifest(new File(tempPath));
    if (manifest == null) {
      // try to find the manifest in a subdirectory, since the zip may
      // have been constructed this way
      File[] subDirs = tempDir.listFiles(new FileFilter() {
        public boolean accept(File pathname) {
          return pathname.isDirectory();
        }
      });
      for (File subdir : subDirs) {
        manifest = getManifest(subdir);
        if (manifest != null)
          break;
      }
      if (manifest == null)
        throw new RuntimeException("no manifest found in this zip");
    }

    MediaPackage mp = null;
    try {
      builder.setSerializer(new DefaultMediaPackageSerializerImpl(manifest.getParentFile()));
      InputStream manifestStream = manifest.toURI().toURL().openStream();
      mp = builder.loadFromXml(manifestStream);
      try {
        manifestStream.close(); // FIXME move to finally
      } catch (IOException e) {
        logger.error(e.getMessage());
      }
      for (MediaPackageElement element : mp.elements()) {
        String elId = element.getIdentifier();
        if (elId == null) {
          elId = UUID.randomUUID().toString();
          element.setIdentifier(elId);
        }
        String filename = element.getURI().toURL().getFile();
        filename = filename.substring(filename.lastIndexOf("/"));
        InputStream elementStream = element.getURI().toURL().openStream();
        URI newUrl = addContentToRepo(mp, elId, filename, elementStream);
        try {
          elementStream.close();
        } catch (IOException e) {
          logger.error(e.getMessage());
        }
        element.setURI(newUrl);
      }

    } catch (Exception e) {
      logger.error("Ingest service: Failed to ingest media package: {}", e.getMessage());
      throw (e);
    }
    removeDirectory(tempPath);
    if(wd == null) {
      return ingest(mp);
    } else {
      return ingest(mp, wd);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.ingest.api.IngestService#createMediaPackage()
   */
  public MediaPackage createMediaPackage() throws MediaPackageException,
          org.opencastproject.util.ConfigurationException, HandleException {
    MediaPackage mediaPackage;
    try {
      mediaPackage = builder.createNew();
    } catch (MediaPackageException e) {
      logger.error("INGEST:Failed to create media package " + e.getLocalizedMessage());
      throw e;
    }
    mediaPackage.setDate(new Date());
    return mediaPackage;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.ingest.api.IngestService#addMediaPackageTrack(URI, MediaPackageElementFlavor,
   *      MediaPackage)
   */
  public MediaPackage addTrack(URI uri, MediaPackageElementFlavor flavor, MediaPackage mediaPackage)
          throws MediaPackageException, UnsupportedElementException, IOException {
    String elementId = UUID.randomUUID().toString();
    URI newUrl = addContentToRepo(mediaPackage, elementId, uri);
    return addContentToMediaPackage(mediaPackage, elementId, newUrl, MediaPackageElement.Type.Track, flavor);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.ingest.api.IngestService#addMediaPackageTrack(InputStream, MediaPackageElementFlavor,
   *      MediaPackage)
   */
  public MediaPackage addTrack(InputStream in, String fileName, MediaPackageElementFlavor flavor,
          MediaPackage mediaPackage) throws MediaPackageException, UnsupportedElementException, IOException {
    String elementId = UUID.randomUUID().toString();
    URI newUrl = addContentToRepo(mediaPackage, elementId, fileName, in);
    return addContentToMediaPackage(mediaPackage, elementId, newUrl, MediaPackageElement.Type.Track, flavor);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.ingest.api.IngestService#addMediaPackageCatalog(URI, MediaPackageElementFlavor,
   *      MediaPackage)
   */
  public MediaPackage addCatalog(URI uri, MediaPackageElementFlavor flavor, MediaPackage mediaPackage)
          throws MediaPackageException, UnsupportedElementException, IOException {
    String elementId = UUID.randomUUID().toString();
    URI newUrl = addContentToRepo(mediaPackage, elementId, uri);
    return addContentToMediaPackage(mediaPackage, elementId, newUrl, MediaPackageElement.Type.Catalog, flavor);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.ingest.api.IngestService#addMediaPackageCatalog(InputStream, MediaPackageElementFlavor,
   *      MediaPackage)
   */
  public MediaPackage addCatalog(InputStream in, String fileName, MediaPackageElementFlavor flavor,
          MediaPackage mediaPackage) throws MediaPackageException, UnsupportedElementException, IOException {
    String elementId = UUID.randomUUID().toString();
    URI newUrl = addContentToRepo(mediaPackage, elementId, fileName, in);
    return addContentToMediaPackage(mediaPackage, elementId, newUrl, MediaPackageElement.Type.Catalog, flavor);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.ingest.api.IngestService#addMediaPackageAttachment(URI, MediaPackageElementFlavor,
   *      MediaPackage)
   */
  public MediaPackage addAttachment(URI uri, MediaPackageElementFlavor flavor, MediaPackage mediaPackage)
          throws MediaPackageException, UnsupportedElementException, IOException {
    String elementId = UUID.randomUUID().toString();
    URI newUrl = addContentToRepo(mediaPackage, elementId, uri);
    return addContentToMediaPackage(mediaPackage, elementId, newUrl, MediaPackageElement.Type.Attachment, flavor);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.ingest.api.IngestService#addMediaPackageAttachment(InputStream, MediaPackageElementFlavor,
   *      MediaPackage)
   */
  public MediaPackage addAttachment(InputStream in, String fileName, MediaPackageElementFlavor flavor,
          MediaPackage mediaPackage) throws MediaPackageException, UnsupportedElementException, IOException {
    String elementId = UUID.randomUUID().toString();
    URI newUrl = addContentToRepo(mediaPackage, elementId, fileName, in);
    return addContentToMediaPackage(mediaPackage, elementId, newUrl, MediaPackageElement.Type.Attachment, flavor);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.ingest.api.IngestService#ingest(java.lang.String)
   */
  public WorkflowInstance ingest(MediaPackage mp) throws Exception {
    return workflowService.start(mp);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.ingest.api.IngestService#ingest(java.lang.String, java.lang.String)
   */
  public WorkflowInstance ingest(MediaPackage mp, String wd) throws Exception {
    return ingest(mp, wd, null);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.ingest.api.IngestService#ingest(java.lang.String, java.lang.String, java.util.Map)
   */
  @Override
  public WorkflowInstance ingest(MediaPackage mp, String wd, Map<String,String> properties) throws Exception {
    WorkflowInstance workflowInst;
    WorkflowDefinition workflowDef = workflowService.getWorkflowDefinitionById(wd);
    if(workflowDef == null) throw new IllegalStateException(wd + " is not a registered workflow definition");
    if (properties == null) {
      workflowInst = workflowService.start(workflowDef, mp);
    } else {
      workflowInst = workflowService.start(workflowDef, mp, properties);
    }
    return workflowInst;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.ingest.api.IngestService#discardMediaPackage(java.lang.String)
   */
  public void discardMediaPackage(MediaPackage mp) {
    String mediaPackageId = mp.getIdentifier().compact();
    for (MediaPackageElement element : mp.getElements()) {
      try {
        workspace.delete(mediaPackageId, element.getIdentifier());
      } catch (NotFoundException e) {
        logger.warn("Unable to find (and hence, delete), this mediapackage element", e);
      }
    }
  }
  
  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.ingest.api.IngestService#getWorkflowInstance(java.lang.String)
   */
  @Override
  public WorkflowInstance getWorkflowInstance(String id) {
    return workflowService.getWorkflowById(id);
  }

  private URI addContentToRepo(MediaPackage mp, String elementId, URI uri) throws IOException,
          UnsupportedElementException {
    InputStream uriStream = uri.toURL().openStream();
    URI returnedUri = workspace.put(mp.getIdentifier().compact(), elementId, FilenameUtils.getName(uri.toURL()
            .toString()), uriStream);
    try {
      uriStream.close();
    } catch (IOException e) {
      logger.error(e.getMessage());
    }
    return returnedUri;
    // return addContentToMediaPackage(mp, elementId, newUrl, type, flavor);
  }

  private URI addContentToRepo(MediaPackage mp, String elementId, String filename, InputStream file)
          throws UnsupportedElementException {
    return workspace.put(mp.getIdentifier().compact(), elementId, filename, file);
  }

  private MediaPackage addContentToMediaPackage(MediaPackage mp, String elementId, URI uri,
          MediaPackageElement.Type type, MediaPackageElementFlavor flavor) throws UnsupportedElementException {
    MediaPackageElement mpe = mp.add(uri, type, flavor);
    mpe.setIdentifier(elementId);
    return mp;
  }

  /**
   * Returns the manifest from a media package directory or <code>null</code> if no manifest was found. Manifests are
   * expected to be named <code>index.xml</code> or <code>manifest.xml</code>.
   * 
   * @param mediapackageDir
   *          the potential mediapackage directory
   * @return the manifest file
   */
  private File getManifest(File mediapackageDir) {
    Stack<File> stack = new Stack<File>();
    stack.push(mediapackageDir);
    for (File f : mediapackageDir.listFiles()) {
      if (f.isDirectory())
        stack.push(f);
    }
    while (!stack.empty()) {
      File dir = stack.pop();
      File[] files = dir.listFiles(new FileFilter() {
        @Override
        public boolean accept(File pathname) {
          return pathname.getName().endsWith(".xml");
        }
      });
      for (File f : files) {
        if ("index.xml".equals(f.getName()) || "manifest.xml".equals(f.getName()))
          return f;
      }
    }
    return null;
  }

  private File createDirectory(String dir) {
    File f = new File(dir);
    if (!f.exists()) {
      try {
        FileUtils.forceMkdir(f);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return f;
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

  // ---------------------------------------------
  // --------- config ---------
  // ---------------------------------------------
  public void setTempFolder(String tempFolder) {
    this.tempFolder = tempFolder;
  }

  // ---------------------------------------------
  // --------- bind and unbind bundles ---------
  // ---------------------------------------------
  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

}
