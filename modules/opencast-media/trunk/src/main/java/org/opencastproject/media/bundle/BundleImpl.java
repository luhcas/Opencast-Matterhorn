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

package org.opencastproject.media.bundle;

import org.opencastproject.media.bundle.handle.Handle;
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
 * Default implementation for a media bundle.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public final class BundleImpl implements Bundle {

  /** The bundle root directory */
  File rootDir = null;

  /** The media bundle meta data */
  ManifestImpl manifest = null;

  /** The bundle size */
  private long size = -1;

  /** List of observers */
  List<BundleObserver> observers = new ArrayList<BundleObserver>();

  /** The bundle element builder, may remain <code>null</code> */
  BundleElementBuilder bundleElementBuilder = null;

  /** the logging facility provided by log4j */
  private final static Logger log_ = LoggerFactory.getLogger(BundleImpl.class);

  /**
   * Creates a bundle object from the given root directory and bundle
   * identifier.
   * 
   * @param directory
   *          the bundle root directory
   * @param handle
   *          the bundle identifier
   * @throws IOException
   *           if the bundle contents cannot be accessed
   * @throws UnknownFileTypeException
   *           if the bundle contains unknown file types
   * @throws NoSuchAlgorithmException
   *           if the md5 checksum cannot be computed
   */
  BundleImpl(File directory, Handle handle) throws IOException,
      UnknownFileTypeException, NoSuchAlgorithmException {
    this.rootDir = directory;
    this.manifest = new ManifestImpl(this, handle);
  }

  /**
   * Creates a new media bundle derived from the given media bundle catalog
   * document.
   * 
   * @param manifest
   *          the manifest
   */
  BundleImpl(ManifestImpl manifest) {
    this.manifest = manifest;
    this.rootDir = manifest.getFile().getParentFile();

    // Set a reference to the bundle
    for (BundleElement element : manifest.getEntries()) {
      if (element instanceof AbstractBundleElement) {
        ((AbstractBundleElement) element).setBundle(this);
      }
    }
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#getIdentifier()
   */
  public Handle getIdentifier() {
    return manifest.getIdentifier();
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#getManifest()
   */
  public Manifest getManifest() {
    return manifest;
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#isLocked()
   */
  public boolean isLocked() {
    File lockfile = new File(rootDir, LOCKFILE);
    return lockfile.exists();
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#getDuration()
   */
  public long getDuration() {
    return manifest.getDuration();
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#getStartDate()
   */
  public long getStartDate() {
    return manifest.getStartDate();
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#elements()
   */
  public Iterable<BundleElement> elements() {
    return Arrays.asList(manifest.getEntries());
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#getElementByReference(org.opencastproject.media.bundle.BundleReference)
   */
  public BundleElement getElementByReference(BundleReference reference) {
    if (reference == null)
      throw new IllegalArgumentException("Argument reference is null");
    for (BundleElement element : manifest.getEntries()) {
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
   * @see org.opencastproject.media.bundle.Bundle#getElementById(java.lang.String)
   */
  public BundleElement getElementById(String id) {
    for (BundleElement element : manifest.getEntries()) {
      if (id.equals(element.getIdentifier()))
        return element;
    }
    return null;
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#contains(org.opencastproject.media.bundle.BundleElement)
   */
  public boolean contains(BundleElement element) {
    return manifest.contains(element);
  }

  /**
   * Returns <code>true</code> if the bundle contains an element with the
   * specified identifier.
   * 
   * @param identifier
   *          the identifier
   * @return <code>true</code> if the bundle contains an element with this
   *         identifier
   */
  boolean contains(String identifier) {
    for (BundleElement element : manifest.getEntries()) {
      if (element.getIdentifier().equals(identifier))
        return true;
    }
    return false;
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#add(org.opencastproject.media.bundle.Catalog)
   */
  public void add(Catalog catalog) throws BundleException,
      UnsupportedBundleElementException {
    add(catalog, true);
  }

  public void add(Catalog catalog, boolean move) throws BundleException,
      UnsupportedBundleElementException {
    try {
      integrateCatalog(catalog, move);
      manifest.add(catalog);
      fireElementAdded(catalog);
    } catch (IOException e) {
      throw new BundleException("Error integrating " + catalog
          + " into bundle: " + e.getMessage());
    }
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#add(org.opencastproject.media.bundle.Track)
   */
  public void add(Track track) throws BundleException,
      UnsupportedBundleElementException {
    add(track, true);
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#add(org.opencastproject.media.bundle.Track,
   *      boolean)
   */
  public void add(Track track, boolean move) throws BundleException,
      UnsupportedBundleElementException {
    try {
      integrateTrack(track, move);
      manifest.add(track);
      fireElementAdded(track);
    } catch (IOException e) {
      throw new BundleException("Error integrating " + track + " into bundle: "
          + e.getMessage());
    }
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#add(org.opencastproject.media.bundle.Attachment)
   */
  public void add(Attachment attachment) throws BundleException,
      UnsupportedBundleElementException {
    add(attachment, true);
  }

  public void add(Attachment attachment, boolean move) throws BundleException,
      UnsupportedBundleElementException {
    try {
      integrateAttachment(attachment, move);
      manifest.add(attachment);
      fireElementAdded(attachment);
    } catch (IOException e) {
      throw new BundleException("Error integrating " + attachment
          + " into bundle: " + e.getMessage());
    }
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#getCatalogs()
   */
  public Catalog[] getCatalogs() {
    return manifest.getCatalogs();
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#getCatalogs(BundleElementFlavor)
   */
  public Catalog[] getCatalogs(BundleElementFlavor type) {
    return manifest.getCatalogs(type);
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#getCatalogs(org.opencastproject.media.bundle.BundleReference)
   */
  public Catalog[] getCatalogs(BundleReference reference) {
    return manifest.getCatalogs(reference);
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#getCatalogs(org.opencastproject.media.bundle.BundleElementFlavor,
   *      org.opencastproject.media.bundle.BundleReference)
   */
  public Catalog[] getCatalogs(BundleElementFlavor flavor,
      BundleReference reference) {
    return manifest.getCatalogs(flavor, reference);
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#hasCatalogs()
   */
  public boolean hasCatalogs() {
    return manifest.hasCatalogs();
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#hasCatalogs(BundleElementFlavor)
   */
  public boolean hasCatalogs(BundleElementFlavor type, BundleReference reference) {
    return manifest.hasCatalogs(type, reference);
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#hasCatalogs(BundleElementFlavor)
   */
  public boolean hasCatalogs(BundleElementFlavor type) {
    return manifest.hasCatalogs(type);
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#getTracks()
   */
  public Track[] getTracks() {
    return manifest.getTracks();
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#getTracks(BundleElementFlavor)
   */
  public Track[] getTracks(BundleElementFlavor type) {
    return manifest.getTracks(type);
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#getTracks(org.opencastproject.media.bundle.BundleReference)
   */
  public Track[] getTracks(BundleReference reference) {
    return manifest.getTracks(reference);
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#getTracks(org.opencastproject.media.bundle.BundleElementFlavor,
   *      org.opencastproject.media.bundle.BundleReference)
   */
  public Track[] getTracks(BundleElementFlavor flavor, BundleReference reference) {
    return manifest.getTracks(flavor, reference);
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#hasTracks()
   */
  public boolean hasTracks() {
    return manifest.hasTracks();
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#hasTracks(BundleElementFlavor)
   */
  public boolean hasTracks(BundleElementFlavor type) {
    return manifest.hasTracks(type);
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#getUnclassifiedElements()
   */
  public BundleElement[] getUnclassifiedElements() {
    return manifest.getUnclassifiedElements(null);
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#getUnclassifiedElements(org.opencastproject.media.bundle.BundleElementFlavor)
   */
  public BundleElement[] getUnclassifiedElements(BundleElementFlavor type) {
    return manifest.getUnclassifiedElements(type);
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#hasUnclassifiedElements(org.opencastproject.media.bundle.BundleElementFlavor)
   */
  public boolean hasUnclassifiedElements(BundleElementFlavor type) {
    return manifest.hasUnclassifiedElements(type);
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#hasUnclassifiedElements()
   */
  public boolean hasUnclassifiedElements() {
    return manifest.hasUnclassifiedElements();
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#addObserver(BundleObserver)
   */
  public void addObserver(BundleObserver observer) {
    synchronized (observers) {
      observers.add(observer);
    }
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#getAttachments()
   */
  public Attachment[] getAttachments() {
    return manifest.getAttachments();
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#getAttachments(BundleElementFlavor)
   */
  public Attachment[] getAttachments(BundleElementFlavor flavor) {
    return manifest.getAttachments(flavor);
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#getAttachments(org.opencastproject.media.bundle.BundleReference)
   */
  public Attachment[] getAttachments(BundleReference reference) {
    return manifest.getAttachments(reference);
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#getAttachments(org.opencastproject.media.bundle.BundleElementFlavor,
   *      org.opencastproject.media.bundle.BundleReference)
   */
  public Attachment[] getAttachments(BundleElementFlavor flavor,
      BundleReference reference) {
    return manifest.getAttachments(flavor, reference);
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#hasAttachments()
   */
  public boolean hasAttachments() {
    return manifest.hasAttachments();
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#hasAttachments(BundleElementFlavor)
   */
  public boolean hasAttachments(BundleElementFlavor type) {
    return manifest.hasAttachments(type);
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#remove(org.opencastproject.media.bundle.BundleElement)
   */
  public void remove(BundleElement element) throws BundleException {
    removeElement(element);
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#remove(org.opencastproject.media.bundle.Attachment)
   */
  public void remove(Attachment attachment) throws BundleException {
    removeElement(attachment);
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#remove(org.opencastproject.media.bundle.Catalog)
   */
  public void remove(Catalog catalog) throws BundleException {
    removeElement(catalog);
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#remove(org.opencastproject.media.bundle.Track)
   */
  public void remove(Track track) throws BundleException {
    removeElement(track);
  }

  /**
   * Removes an element from the bundle
   * 
   * @param element
   * @throws BundleException
   */
  protected void removeElement(BundleElement element) throws BundleException {
    try {
      element.getFile().delete();
      if (element instanceof AbstractBundleElement) {
        ((AbstractBundleElement) element).setBundle(null);
      }
      manifest.remove(element);
    } catch (Throwable t) {
      throw new BundleException(t);
    }
    fireElementRemoved(element);
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#getCover()
   */
  public Cover getCover() {
    Attachment[] covers = getAttachments(Cover.FLAVOR);
    if (covers.length > 0) {
      if (covers[0] instanceof Cover)
        return (Cover) covers[0];
      else {
        log_.warn("Cover with inconsistant object type contained in bundle "
            + this);
        return null;
      }
    }
    return null;
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#setCover(org.opencastproject.media.bundle.Cover)
   */
  public void setCover(Cover cover) throws BundleException,
      UnsupportedBundleElementException {
    setCover(cover, true);
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#setCover(org.opencastproject.media.bundle.Cover,
   *      boolean)
   */
  public void setCover(Cover cover, boolean move) throws BundleException,
      UnsupportedBundleElementException {
    Cover oldCover = getCover();
    if (oldCover != null)
      remove(oldCover);
    add(cover, move);
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#removeCover()
   */
  public void removeCover() throws BundleException {
    Attachment[] covers = getAttachments(Cover.FLAVOR);
    if (covers.length > 0) {
      remove(covers[0]);
    }
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#removeObserver(BundleObserver)
   */
  public void removeObserver(BundleObserver observer) {
    synchronized (observers) {
      observers.remove(observer);
    }
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#getRoot()
   */
  public File getRoot() {
    return rootDir;
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#getCatalogRoot()
   */
  public File getCatalogRoot() {
    return new File(rootDir, CATALOG_DIR);
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#getTrackRoot()
   */
  public File getTrackRoot() {
    return new File(rootDir, TRACK_DIR);
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#getAttachmentRoot()
   */
  public File getAttachmentRoot() {
    return new File(rootDir, ATTACHMENT_DIR);
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#getDistributionRoot()
   */
  public File getDistributionRoot() {
    return new File(rootDir, TEMP_DIR);
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#wrap()
   */
  public void wrap() throws BundleException {
    manifest.wrap();
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#pack(org.opencastproject.media.bundle.BundlePackager,
   *      java.io.OutputStream)
   */
  public void pack(BundlePackager packager, OutputStream out)
      throws IOException, BundleException {
    if (packager == null)
      throw new IllegalArgumentException("The packager must not be null");
    if (out == null)
      throw new IllegalArgumentException("The output stream must not be null");
    packager.pack(this, out);
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#add(java.io.File)
   */
  public BundleElement add(File file) throws BundleException,
      UnsupportedBundleElementException {
    return add(file, true);
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#add(java.io.File, boolean)
   */
  public BundleElement add(File file, boolean move) throws BundleException,
      UnsupportedBundleElementException {
    if (bundleElementBuilder == null) {
      bundleElementBuilder = BundleElementBuilderFactory.newInstance()
          .newElementBuilder();
    }
    BundleElement element = bundleElementBuilder.elementFromFile(file);
    add(element, move);
    return element;
  }

  /**
   * Adds a new element to this bundle.
   * 
   * @param element
   *          the new element
   */
  public void add(BundleElement element) throws BundleException,
      UnsupportedBundleElementException {
    add(element, true);
  }

  public void add(BundleElement element, boolean move) throws BundleException,
      UnsupportedBundleElementException {
    try {
      if (element.getElementType().equals(BundleElement.Type.Track)
          && element instanceof Track) {
        integrateTrack((Track) element, move);
      } else if (element.getElementType().equals(BundleElement.Type.Catalog)
          && element instanceof Catalog) {
        integrateCatalog((Catalog) element, move);
      } else if (element.getElementType().equals(BundleElement.Type.Attachment)
          && element instanceof Attachment) {
        integrateAttachment((Attachment) element, move);
      } else {
        integrate(element);
      }
      manifest.add(element);
      fireElementAdded(element);
    } catch (IOException e) {
      throw new BundleException("Error integrating " + element
          + " into bundle: " + e.getMessage());
    }
  }

  /**
   * Notify observers of a removed bundle element.
   * 
   * @param element
   *          the removed element
   */
  protected void fireElementAdded(BundleElement element) {
    synchronized (observers) {
      for (BundleObserver o : observers) {
        try {
          o.bundleElementAdded(element);
        } catch (Throwable th) {
          log_.error("BundleOberserver " + o
              + " throw exception while processing callback", th);
        }
      }
    }
  }

  /**
   * Notify observers of a removed bundle element.
   * 
   * @param element
   *          the removed element
   */
  protected void fireElementRemoved(BundleElement element) {
    synchronized (observers) {
      for (BundleObserver o : observers) {
        try {
          o.bundleElementRemoved(element);
        } catch (Throwable th) {
          log_.error("BundleObserver " + o
              + " threw exception while processing callback", th);
        }
      }
    }
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#copyTo(java.io.File)
   */
  public void copyTo(File destination) throws IOException {
    destination = new File(destination, rootDir.getName());
    FileSupport.copy(rootDir, destination);
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#delete()
   */
  public boolean delete() {
    return FileSupport.delete(rootDir, FileSupport.DELETE_ROOT);
  }

  /**
   * @throws IOException
   * @see org.opencastproject.media.bundle.Bundle#moveTo(java.io.File)
   */
  public boolean moveTo(File destination) throws IOException {
    File oldRoot = rootDir;
    try {
      BundleSupport.lockBundle(oldRoot);
      rootDir = FileSupport.move(oldRoot, destination);

      // Tell the bundle elements about the new location
      manifest.bundleMoved(oldRoot, rootDir);
      for (BundleElement e : manifest.getEntries()) {
        e.bundleMoved(oldRoot, rootDir);
      }
    } catch (Throwable t) {
      rootDir = oldRoot;
      throw new IOException(t.getMessage());
    } finally {
      try {
        BundleSupport.unlockBundle(rootDir);
      } catch (BundleException e) {
        log_.error("Unable to unlock bundle after moving", e);
      }
    }
    return true;
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#renameTo(org.opencastproject.media.bundle.handle.Handle)
   */
  public boolean renameTo(Handle identifier) {
    return rootDir.renameTo(new File(rootDir.getParentFile(), identifier
        .toString()));
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#getSize()
   */
  public long getSize() {
    if (size >= 0)
      return size;
    size = 0;
    for (BundleElement e : manifest.getEntries()) {
      size += e.getFile().length();
    }
    return size;
  }

  /**
   * Integrates the element into the bundle. This mainly involves moving the
   * element into the bundle file structure.
   * 
   * @param element
   *          the element to integrate
   * @throws IOException
   *           if integration of the element failed
   */
  private void integrate(BundleElement element) throws IOException {
    // TODO: Add element as an attachment?
    throw new IllegalStateException(
        "This element is not supported by this bundle implementation");
  }

  /**
   * Integrates the catalog into the bundle. This mainly involves moving the
   * catalog into the bundle file structure.
   * 
   * @param catalog
   *          the catalog to integrate
   * @param move
   * @throws IOException
   *           if integration of the catalog failed
   */
  private void integrateCatalog(Catalog catalog, boolean move)
      throws IOException {
    // Check (uniqueness of) catalog identifier
    String id = catalog.getIdentifier();
    if (id == null || contains(id)) {
      id = createElementIdentifier("catalog", manifest.getCatalogs().length + 1);
      catalog.setIdentifier(id.toString());
    }
    integrateIntoBundle(catalog, getCatalogRoot());
  }

  /**
   * Integrates the track into the bundle. This mainly involves moving the track
   * into the bundle file structure.
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
    integrateIntoBundle(track, getTrackRoot());
  }

  /**
   * Integrates the attachment into the bundle. This mainly involves moving the
   * attachment into the bundle file structure.
   * 
   * @param attachment
   *          the attachment to integrate
   * @param move
   * @throws IOException
   *           if integration of the attachment failed
   */
  private void integrateAttachment(Attachment attachment, boolean move)
      throws IOException {
    // Check (uniqueness of) attachment identifier
    String id = attachment.getIdentifier();
    if (id == null || contains(id)) {
      id = createElementIdentifier("attachment",
          manifest.getAttachments().length + 1);
      attachment.setIdentifier(id.toString());
    }
    integrateIntoBundle(attachment, getAttachmentRoot());
  }

  /**
   * Integrates (= copies) the elements underlying resource into the bundle and
   * wires the element with the bundle
   * 
   * @param element
   *          the element to integrate
   * @param elementRoot
   *          the directory, where the element shall reside within the bundle
   * @throws IOException
   */
  private void integrateIntoBundle(BundleElement element, File elementRoot)
      throws IOException {
    // Only integrate if not already in bundle
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

    // Set a reference to the bundle
    if (element instanceof AbstractBundleElement) {
      ((AbstractBundleElement) element).setBundle(this);
    }
  }

  /**
   * Returns a bundle element identifier with the given prefix and the given
   * number or a higher one as the suffix. The identifier will be unique within
   * the bundle.
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
   * Creates a unique filename inside the root folder, based on the parameter
   * <code>filename</code>.
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
      name = new StringBuffer(baseName).append("-").append(count).append(".")
          .append(extension);
      f = new File(root, name.toString());
      count++;
    }
    return f;
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#verify()
   */
  public void verify() throws BundleException {
    for (BundleElement e : manifest.getEntries()) {
      e.verify();
    }
  }

  /**
   * @see org.opencastproject.media.bundle.Bundle#save()
   */
  public void save() throws BundleException {
    try {
      manifest.save();
    } catch (TransformerException e) {
      throw new BundleException(e);
    } catch (ParserConfigurationException e) {
      throw new BundleException(e);
    } catch (IOException e) {
      throw new BundleException(e);
    }
  }

  /**
   * Dumps the bundle contents to standard out.
   */
  public String dump() {
    StringBuffer dump = new StringBuffer("Bundle:");
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
      for (BundleElement e : getUnclassifiedElements()) {
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
    if (obj instanceof Bundle) {
      return getIdentifier().equals(((Bundle) obj).getIdentifier());
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
      return "Unknown bundle";
  }
}