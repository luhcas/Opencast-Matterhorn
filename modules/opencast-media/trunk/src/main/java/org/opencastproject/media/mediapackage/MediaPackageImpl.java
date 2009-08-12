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

package org.opencastproject.media.mediapackage;

import org.opencastproject.media.mediapackage.handle.Handle;
import org.opencastproject.util.FileSupport;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.UnknownFileTypeException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * Default implementation for a media media package.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: MediaPackageImpl.java 2908 2009-07-17 16:51:07Z ced $
 */
public final class MediaPackageImpl implements MediaPackage {

  /** The media package root directory */
  File rootDir = null;

  /** The media media package meta data */
  ManifestImpl manifest = null;

  /** The media package size */
  private long size = -1;

  /** List of observers */
  List<MediaPackageObserver> observers = new ArrayList<MediaPackageObserver>();

  /** The media package element builder, may remain <code>null</code> */
  MediaPackageElementBuilder mediaPackageElementBuilder = null;

  /** the logging facility provided by log4j */
  private final static Logger log_ = LoggerFactory.getLogger(MediaPackageImpl.class.getName());

  /**
   * Creates a media package object from the given root directory and media package identifier.
   * 
   * @param directory
   *          the media package root directory
   * @param handle
   *          the media package identifier
   * @throws IOException
   *           if the media package contents cannot be accessed
   * @throws UnknownFileTypeException
   *           if the media package contains unknown file types
   * @throws NoSuchAlgorithmException
   *           if the md5 checksum cannot be computed
   */
  MediaPackageImpl(File directory, Handle handle) throws IOException, UnknownFileTypeException,
          NoSuchAlgorithmException {
    this.rootDir = directory;
    this.manifest = new ManifestImpl(this, handle);
  }

  /**
   * Creates a new media media package derived from the given media media package catalog document.
   * 
   * @param manifest
   *          the manifest
   */
  MediaPackageImpl(ManifestImpl manifest) {
    this.manifest = manifest;
    this.rootDir = manifest.getFile().getParentFile();

    // Set a reference to the media package
    for (MediaPackageElement element : manifest.getEntries()) {
      if (element instanceof AbstractMediaPackageElement) {
        ((AbstractMediaPackageElement) element).setMediaPackage(this);
      }
    }
  }

  public File getManifestFile() {
    return manifest.getFile();
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#getIdentifier()
   */
  public Handle getIdentifier() {
    return manifest.getIdentifier();
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#isLocked()
   */
  public boolean isLocked() {
    File lockfile = new File(rootDir, LOCKFILE);
    return lockfile.exists();
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#getDuration()
   */
  public long getDuration() {
    return manifest.getDuration();
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#getStartDate()
   */
  public long getStartDate() {
    return manifest.getStartDate();
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#elements()
   */
  public Iterable<MediaPackageElement> elements() {
    return Arrays.asList(manifest.getEntries());
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#getElementByReference(org.opencastproject.media.mediapackage.MediaPackageReference)
   */
  public MediaPackageElement getElementByReference(MediaPackageReference reference) {
    if (reference == null)
      throw new IllegalArgumentException("Argument reference is null");
    for (MediaPackageElement element : manifest.getEntries()) {
      String elementType = element.getElementType().toString().toLowerCase();
      String elementId = element.getIdentifier().toLowerCase();
      String refType = reference.getType().toLowerCase();
      String refId = reference.getIdentifier().toLowerCase();
      if (elementType.equals(refType) && elementId.equals(refId))
        return element;
    }
    return null;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#getElementById(java.lang.String)
   */
  public MediaPackageElement getElementById(String id) {
    for (MediaPackageElement element : manifest.getEntries()) {
      if (id.equals(element.getIdentifier()))
        return element;
    }
    return null;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#contains(org.opencastproject.media.mediapackage.MediaPackageElement)
   */
  public boolean contains(MediaPackageElement element) {
    return manifest.contains(element);
  }

  /**
   * Returns <code>true</code> if the media package contains an element with the specified identifier.
   * 
   * @param identifier
   *          the identifier
   * @return <code>true</code> if the media package contains an element with this identifier
   */
  boolean contains(String identifier) {
    for (MediaPackageElement element : manifest.getEntries()) {
      if (element.getIdentifier().equals(identifier))
        return true;
    }
    return false;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#add(org.opencastproject.media.mediapackage.Catalog)
   */
  public void add(Catalog catalog) throws MediaPackageException, UnsupportedElementException {
    add(catalog, true);
  }

  public void add(Catalog catalog, boolean move) throws MediaPackageException, UnsupportedElementException {
    try {
      integrateCatalog(catalog, move);
      manifest.add(catalog);
      fireElementAdded(catalog);
    } catch (IOException e) {
      throw new MediaPackageException("Error integrating " + catalog + " into media package: " + e.getMessage());
    }
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#add(org.opencastproject.media.mediapackage.Track)
   */
  public void add(Track track) throws MediaPackageException, UnsupportedElementException {
    add(track, true);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#add(org.opencastproject.media.mediapackage.Track, boolean)
   */
  public void add(Track track, boolean move) throws MediaPackageException, UnsupportedElementException {
    try {
      integrateTrack(track, move);
      manifest.add(track);
      fireElementAdded(track);
    } catch (IOException e) {
      throw new MediaPackageException("Error integrating " + track + " into media package: " + e.getMessage());
    }
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#add(org.opencastproject.media.mediapackage.Attachment)
   */
  public void add(Attachment attachment) throws MediaPackageException, UnsupportedElementException {
    add(attachment, true);
  }

  public void add(Attachment attachment, boolean move) throws MediaPackageException, UnsupportedElementException {
    try {
      integrateAttachment(attachment, move);
      manifest.add(attachment);
      fireElementAdded(attachment);
    } catch (IOException e) {
      throw new MediaPackageException("Error integrating " + attachment + " into media package: " + e.getMessage());
    }
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#getCatalogs()
   */
  public Catalog[] getCatalogs() {
    return manifest.getCatalogs();
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#getCatalogs(MediaPackageElementFlavor)
   */
  public Catalog[] getCatalogs(MediaPackageElementFlavor type) {
    return manifest.getCatalogs(type);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#getCatalogs(org.opencastproject.media.mediapackage.MediaPackageReference)
   */
  public Catalog[] getCatalogs(MediaPackageReference reference) {
    return manifest.getCatalogs(reference);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#getCatalogs(org.opencastproject.media.mediapackage.MediaPackageElementFlavor,
   *      org.opencastproject.media.mediapackage.MediaPackageReference)
   */
  public Catalog[] getCatalogs(MediaPackageElementFlavor flavor, MediaPackageReference reference) {
    return manifest.getCatalogs(flavor, reference);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#hasCatalogs()
   */
  public boolean hasCatalogs() {
    return manifest.hasCatalogs();
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#hasCatalogs(MediaPackageElementFlavor)
   */
  public boolean hasCatalogs(MediaPackageElementFlavor type, MediaPackageReference reference) {
    return manifest.hasCatalogs(type, reference);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#hasCatalogs(MediaPackageElementFlavor)
   */
  public boolean hasCatalogs(MediaPackageElementFlavor type) {
    return manifest.hasCatalogs(type);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#getTracks()
   */
  public Track[] getTracks() {
    return manifest.getTracks();
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#getTracks(MediaPackageElementFlavor)
   */
  public Track[] getTracks(MediaPackageElementFlavor type) {
    return manifest.getTracks(type);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#getTracks(org.opencastproject.media.mediapackage.MediaPackageReference)
   */
  public Track[] getTracks(MediaPackageReference reference) {
    return manifest.getTracks(reference);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#getTracks(org.opencastproject.media.mediapackage.MediaPackageElementFlavor,
   *      org.opencastproject.media.mediapackage.MediaPackageReference)
   */
  public Track[] getTracks(MediaPackageElementFlavor flavor, MediaPackageReference reference) {
    return manifest.getTracks(flavor, reference);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#hasTracks()
   */
  public boolean hasTracks() {
    return manifest.hasTracks();
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#hasTracks(MediaPackageElementFlavor)
   */
  public boolean hasTracks(MediaPackageElementFlavor type) {
    return manifest.hasTracks(type);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#getUnclassifiedElements()
   */
  public MediaPackageElement[] getUnclassifiedElements() {
    return manifest.getUnclassifiedElements(null);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#getUnclassifiedElements(org.opencastproject.media.mediapackage.MediaPackageElementFlavor)
   */
  public MediaPackageElement[] getUnclassifiedElements(MediaPackageElementFlavor type) {
    return manifest.getUnclassifiedElements(type);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#hasUnclassifiedElements(org.opencastproject.media.mediapackage.MediaPackageElementFlavor)
   */
  public boolean hasUnclassifiedElements(MediaPackageElementFlavor type) {
    return manifest.hasUnclassifiedElements(type);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#hasUnclassifiedElements()
   */
  public boolean hasUnclassifiedElements() {
    return manifest.hasUnclassifiedElements();
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#addObserver(MediaPackageObserver)
   */
  public void addObserver(MediaPackageObserver observer) {
    synchronized (observers) {
      observers.add(observer);
    }
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#getAttachments()
   */
  public Attachment[] getAttachments() {
    return manifest.getAttachments();
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#getAttachments(MediaPackageElementFlavor)
   */
  public Attachment[] getAttachments(MediaPackageElementFlavor flavor) {
    return manifest.getAttachments(flavor);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#getAttachments(org.opencastproject.media.mediapackage.MediaPackageReference)
   */
  public Attachment[] getAttachments(MediaPackageReference reference) {
    return manifest.getAttachments(reference);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#getAttachments(org.opencastproject.media.mediapackage.MediaPackageElementFlavor,
   *      org.opencastproject.media.mediapackage.MediaPackageReference)
   */
  public Attachment[] getAttachments(MediaPackageElementFlavor flavor, MediaPackageReference reference) {
    return manifest.getAttachments(flavor, reference);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#hasAttachments()
   */
  public boolean hasAttachments() {
    return manifest.hasAttachments();
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#hasAttachments(MediaPackageElementFlavor)
   */
  public boolean hasAttachments(MediaPackageElementFlavor type) {
    return manifest.hasAttachments(type);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#remove(org.opencastproject.media.mediapackage.MediaPackageElement)
   */
  public void remove(MediaPackageElement element) throws MediaPackageException {
    removeElement(element);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#remove(org.opencastproject.media.mediapackage.Attachment)
   */
  public void remove(Attachment attachment) throws MediaPackageException {
    removeElement(attachment);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#remove(org.opencastproject.media.mediapackage.Catalog)
   */
  public void remove(Catalog catalog) throws MediaPackageException {
    removeElement(catalog);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#remove(org.opencastproject.media.mediapackage.Track)
   */
  public void remove(Track track) throws MediaPackageException {
    removeElement(track);
  }

  /**
   * Removes an element from the media package
   * 
   * @param element
   * @throws MediaPackageException
   */
  protected void removeElement(MediaPackageElement element) throws MediaPackageException {
    try {
      element.getFile().delete();
      if (element instanceof AbstractMediaPackageElement) {
        ((AbstractMediaPackageElement) element).setMediaPackage(null);
      }
      manifest.remove(element);
    } catch (Throwable t) {
      throw new MediaPackageException(t);
    }
    fireElementRemoved(element);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#getCover()
   */
  public Cover getCover() {
    Attachment[] covers = getAttachments(Cover.FLAVOR);
    if (covers.length > 0) {
      if (covers[0] instanceof Cover)
        return (Cover) covers[0];
      else {
        log_.warn("Cover with inconsistant object type contained in media package " + this);
        return null;
      }
    }
    return null;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#setCover(org.opencastproject.media.mediapackage.Cover)
   */
  public void setCover(Cover cover) throws MediaPackageException, UnsupportedElementException {
    setCover(cover, true);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#setCover(org.opencastproject.media.mediapackage.Cover,
   *      boolean)
   */
  public void setCover(Cover cover, boolean move) throws MediaPackageException, UnsupportedElementException {
    Cover oldCover = getCover();
    if (oldCover != null)
      remove(oldCover);
    add(cover, move);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#removeCover()
   */
  public void removeCover() throws MediaPackageException {
    Attachment[] covers = getAttachments(Cover.FLAVOR);
    if (covers.length > 0) {
      remove(covers[0]);
    }
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#removeObserver(MediaPackageObserver)
   */
  public void removeObserver(MediaPackageObserver observer) {
    synchronized (observers) {
      observers.remove(observer);
    }
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#getRoot()
   */
  public File getRoot() {
    return rootDir;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#getCatalogRoot()
   */
  public File getCatalogRoot() {
    return new File(rootDir, CATALOG_DIR);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#getTrackRoot()
   */
  public File getTrackRoot() {
    return new File(rootDir, TRACK_DIR);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#getAttachmentRoot()
   */
  public File getAttachmentRoot() {
    return new File(rootDir, ATTACHMENT_DIR);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#getDistributionRoot()
   */
  public File getDistributionRoot() {
    return new File(rootDir, TEMP_DIR);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#wrap()
   */
  public void wrap() throws MediaPackageException {
    manifest.wrap();
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#pack(org.opencastproject.media.mediapackage.MediaPackagePackager,
   *      java.io.OutputStream)
   */
  public void pack(MediaPackagePackager packager, OutputStream out) throws IOException, MediaPackageException {
    if (packager == null)
      throw new IllegalArgumentException("The packager must not be null");
    if (out == null)
      throw new IllegalArgumentException("The output stream must not be null");
    packager.pack(this, out);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#add(java.io.File)
   */
  public MediaPackageElement add(File file) throws MediaPackageException, UnsupportedElementException {
    return add(file, true);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#add(java.io.File, boolean)
   */
  public MediaPackageElement add(File file, boolean move) throws MediaPackageException, UnsupportedElementException {
    if (mediaPackageElementBuilder == null) {
      mediaPackageElementBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
    }
    MediaPackageElement element = mediaPackageElementBuilder.elementFromFile(file);
    add(element, move);
    return element;
  }

  /**
   * Adds a new element to this media package.
   * 
   * @param element
   *          the new element
   */
  public void add(MediaPackageElement element) throws MediaPackageException, UnsupportedElementException {
    add(element, true);
  }

  public void add(MediaPackageElement element, boolean move) throws MediaPackageException, UnsupportedElementException {
    try {
      if (element.getElementType().equals(MediaPackageElement.Type.Track) && element instanceof Track) {
        integrateTrack((Track) element, move);
      } else if (element.getElementType().equals(MediaPackageElement.Type.Catalog) && element instanceof Catalog) {
        integrateCatalog((Catalog) element, move);
      } else if (element.getElementType().equals(MediaPackageElement.Type.Attachment) && element instanceof Attachment) {
        integrateAttachment((Attachment) element, move);
      } else {
        integrate(element);
      }
      manifest.add(element);
      fireElementAdded(element);
    } catch (IOException e) {
      throw new MediaPackageException("Error integrating " + element + " into media package: " + e.getMessage());
    }
  }

  /**
   * Notify observers of a removed media package element.
   * 
   * @param element
   *          the removed element
   */
  protected void fireElementAdded(MediaPackageElement element) {
    synchronized (observers) {
      for (MediaPackageObserver o : observers) {
        try {
          o.elementAdded(element);
        } catch (Throwable th) {
          log_.error("MediaPackageOberserver " + o + " throw exception while processing callback", th);
        }
      }
    }
  }

  /**
   * Notify observers of a removed media package element.
   * 
   * @param element
   *          the removed element
   */
  protected void fireElementRemoved(MediaPackageElement element) {
    synchronized (observers) {
      for (MediaPackageObserver o : observers) {
        try {
          o.elementRemoved(element);
        } catch (Throwable th) {
          log_.error("MediaPackageObserver " + o + " threw exception while processing callback", th);
        }
      }
    }
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#copyTo(java.io.File)
   */
  public void copyTo(File destination) throws IOException {
    destination = new File(destination, rootDir.getName());
    FileSupport.copy(rootDir, destination);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#delete()
   */
  public boolean delete() {
    return FileSupport.delete(rootDir, FileSupport.DELETE_ROOT);
  }

  /**
   * @throws IOException
   * @see org.opencastproject.media.mediapackage.MediaPackage#moveTo(java.io.File)
   */
  public boolean moveTo(File destination) throws IOException {
    File oldRoot = rootDir;
    try {
      MediaPackageSupport.lockMediaPackage(oldRoot);
      rootDir = FileSupport.move(oldRoot, destination);

      // Tell the media package elements about the new location
      manifest.mediaPackageMoved(oldRoot, rootDir);
      for (MediaPackageElement e : manifest.getEntries()) {
        e.mediaPackageMoved(oldRoot, rootDir);
      }
    } catch (Throwable t) {
      rootDir = oldRoot;
      throw new IOException(t.getMessage());
    } finally {
      try {
        MediaPackageSupport.unlockMediaPackage(rootDir);
      } catch (MediaPackageException e) {
        log_.error("Unable to unlock media package after moving", e);
      }
    }
    return true;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#renameTo(org.opencastproject.media.mediapackage.handle.Handle)
   */
  public boolean renameTo(Handle identifier) {
    return rootDir.renameTo(new File(rootDir.getParentFile(), identifier.toString()));
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#getSize()
   */
  public long getSize() {
    if (size >= 0)
      return size;
    size = 0;
    for (MediaPackageElement e : manifest.getEntries()) {
      size += e.getFile().length();
    }
    return size;
  }

  /**
   * Integrates the element into the media package. This mainly involves moving the element into the media package file
   * structure.
   * 
   * @param element
   *          the element to integrate
   * @throws IOException
   *           if integration of the element failed
   */
  private void integrate(MediaPackageElement element) throws IOException {
    // TODO: Add element as an attachment?
    throw new IllegalStateException("This element is not supported by this media package implementation");
  }

  /**
   * Integrates the catalog into the media package. This mainly involves moving the catalog into the media package file
   * structure.
   * 
   * @param catalog
   *          the catalog to integrate
   * @param move
   * @throws IOException
   *           if integration of the catalog failed
   */
  private void integrateCatalog(Catalog catalog, boolean move) throws IOException {
    // Check (uniqueness of) catalog identifier
    String id = catalog.getIdentifier();
    if (id == null || contains(id)) {
      id = createElementIdentifier("catalog", manifest.getCatalogs().length + 1);
      catalog.setIdentifier(id.toString());
    }
    integrate(catalog, getCatalogRoot());
  }

  /**
   * Integrates the track into the media package. This mainly involves moving the track into the media package file
   * structure.
   * 
   * @param track
   *          the track to integrate
   * @param move
   * @throws IOException
   *           if integration of the track failed
   */
  private void integrateTrack(Track track, boolean move) throws IOException {
    // Check (uniqueness of) track identifier
    String id = track.getIdentifier();
    if (id == null || contains(id)) {
      id = createElementIdentifier("track", manifest.getTracks().length + 1);
      track.setIdentifier(id.toString());
    }
    integrate(track, getTrackRoot());
  }

  /**
   * Integrates the attachment into the media package. This mainly involves moving the attachment into the media package
   * file structure.
   * 
   * @param attachment
   *          the attachment to integrate
   * @param move
   * @throws IOException
   *           if integration of the attachment failed
   */
  private void integrateAttachment(Attachment attachment, boolean move) throws IOException {
    // Check (uniqueness of) attachment identifier
    String id = attachment.getIdentifier();
    if (id == null || contains(id)) {
      id = createElementIdentifier("attachment", manifest.getAttachments().length + 1);
      attachment.setIdentifier(id.toString());
    }
    integrate(attachment, getAttachmentRoot());
  }

  /**
   * Integrates (= copies) the elements underlying resource into the media package and wires the element with the media
   * package
   * 
   * @param element
   *          the element to integrate
   * @param elementRoot
   *          the directory, where the element shall reside within the media package
   * @throws IOException
   */
  private void integrate(MediaPackageElement element, File elementRoot) throws IOException {
    // Only integrate if not already in media package
    if (!FileSupport.equals(elementRoot, element.getFile().getParentFile())) {
      // Ensure uniqueness of filename
      File dest = new File(elementRoot, element.getFilename());
      if (dest.exists()) {
        dest = createElementFilename(elementRoot, element.getFilename());
      }
      // Put track into place
      File src = element.getFile();
      if (!src.canRead())
        throw new IllegalStateException("Cannot read track file " + src);
      elementRoot.mkdirs();
      element.integrate(dest);
    }

    // Set a reference to the media package
    if (element instanceof AbstractMediaPackageElement) {
      ((AbstractMediaPackageElement) element).setMediaPackage(this);
    }
  }

  /**
   * Returns a media package element identifier with the given prefix and the given number or a higher one as the
   * suffix. The identifier will be unique within the media package.
   * 
   * @param prefix
   *          the identifier prefix
   * @param count
   *          the number
   * @return the element identifier
   */
  private String createElementIdentifier(String prefix, int count) {
    prefix = prefix + "-";
    String id = prefix + count;
    while (getElementById(id) != null) {
      id = prefix + (++count);
    }
    return id;
  }

  /**
   * Creates a unique filename inside the root folder, based on the parameter <code>filename</code>.
   * 
   * @param root
   *          the root folder
   * @param filename
   *          the original filename
   * @return the new and unique filename
   */
  private File createElementFilename(File root, String filename) {
    String baseName = PathSupport.removeFileExtension(filename);
    String extension = PathSupport.getFileExtension(filename);
    int count = 1;
    StringBuffer name = null;
    File f = new File(root, filename);
    while (f.exists()) {
      name = new StringBuffer(baseName).append("-").append(count).append(".").append(extension);
      f = new File(root, name.toString());
      count++;
    }
    return f;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#verify()
   */
  public void verify() throws MediaPackageException {
    for (MediaPackageElement e : manifest.getEntries()) {
      e.verify();
    }
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#save()
   */
  public void save() throws MediaPackageException {
    try {
      manifest.save();
    } catch (TransformerException e) {
      throw new MediaPackageException(e);
    } catch (ParserConfigurationException e) {
      throw new MediaPackageException(e);
    } catch (IOException e) {
      throw new MediaPackageException(e);
    }
  }

  /**
   * Dumps the media package contents to standard out.
   */
  public String dump() {
    StringBuffer dump = new StringBuffer("Media package:");
    dump.append("\n");
    if (hasCatalogs()) {
      dump.append("  Metadata:");
      dump.append("\n");
      for (Catalog m : getCatalogs()) {
        dump.append("    " + m);
        dump.append("\n");
      }
    }
    if (hasTracks()) {
      dump.append("  Tracks:");
      dump.append("\n");
      for (Track t : getTracks()) {
        dump.append("    " + t);
        dump.append("\n");
      }
    }
    if (hasAttachments()) {
      dump.append("  Attachments:");
      dump.append("\n");
      for (Attachment a : getAttachments()) {
        dump.append("    " + a);
        dump.append("\n");
      }
    }
    if (hasUnclassifiedElements()) {
      dump.append("  Others:");
      dump.append("\n");
      for (MediaPackageElement e : getUnclassifiedElements()) {
        dump.append("    " + e);
        dump.append("\n");
      }
    }
    return dump.toString();
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return getIdentifier().hashCode();
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof MediaPackage) {
      return getIdentifier().equals(((MediaPackage) obj).getIdentifier());
    }
    return false;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    if (manifest.getIdentifier() != null)
      return manifest.getIdentifier().toString();
    else
      return "Unknown media package";
  }
}