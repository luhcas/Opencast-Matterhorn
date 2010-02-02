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

import org.opencastproject.media.mediapackage.MediaPackageElement.Type;
import org.opencastproject.media.mediapackage.identifier.Id;
import org.opencastproject.util.DateTimeSupport;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * Default implementation for a media media package.
 * 
 * TODO: Finish code documentation
 */
@XmlType(name = "mediapackage", namespace = "http://mediapackage.opencastproject.org", propOrder = {
        "title", "series", "creators", "contributors", "subjects", "license",
        "language", "tracks", "catalogs", "attachments" })
@XmlRootElement(name = "mediapackage", namespace = "http://mediapackage.opencastproject.org")
@XmlAccessorType(XmlAccessType.NONE)
public final class MediaPackageImpl implements MediaPackage {

  /** the logging facility provided by log4j */
  private final static Logger log_ = LoggerFactory
          .getLogger(MediaPackageImpl.class.getName());

  /** Context for serializing and deserializing */
  static JAXBContext context;

  /** The media media package meta data */
  ManifestImpl manifest = null;

  /** List of observers */
  List<MediaPackageObserver> observers = new ArrayList<MediaPackageObserver>();

  /** The media package element builder, may remain <code>null</code> */
  MediaPackageElementBuilder mediaPackageElementBuilder = null;

  @XmlElement(name = "title")
  String title = null;

  @XmlElement(name = "language")
  String language = null;

  @XmlElement(name = "series")
  Id series = null;

  @XmlElement(name = "license")
  String license = null;

  @XmlElementWrapper(name = "creators")
  @XmlElement(name = "creator")
  Set<String> creators = null;

  @XmlElementWrapper(name = "contributors")
  @XmlElement(name = "contributor")
  Set<String> contributors = null;

  @XmlElementWrapper(name = "subjects")
  @XmlElement(name = "subject")
  Set<String> subjects = null;

  static {
    try {
      context = JAXBContext.newInstance(
              "org.opencastproject.media.mediapackage", MediaPackageImpl.class
                      .getClassLoader());
    } catch (JAXBException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Creates a media package object.
   */
  MediaPackageImpl() {
    this.manifest = new ManifestImpl();
  }

  /**
   * Creates a media package object with the media package identifier.
   * 
   * @param handle
   *          the media package identifier
   */
  MediaPackageImpl(Id handle) {
    this.manifest = new ManifestImpl(handle);
  }

  /**
   * Creates a new media media package derived from the given media media
   * package catalog document.
   * 
   * @param manifest
   *          the manifest
   */
  MediaPackageImpl(ManifestImpl manifest) {
    this.manifest = manifest;

    // Set a reference to the media package
    for (MediaPackageElement element : manifest.getEntries()) {
      if (element instanceof AbstractMediaPackageElement) {
        ((AbstractMediaPackageElement) element).setMediaPackage(this);
      }
    }
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#getIdentifier()
   */
  @XmlAttribute(name = "id")
  public Id getIdentifier() {
    return manifest.getIdentifier();
  }

  public void setIdentifier(Id identifier) {
    manifest.setIdentifier(identifier);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#getDuration()
   */
  @XmlAttribute(name = "duration")
  public long getDuration() {
    return manifest.getDuration();
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#getDate()
   */
  public Date getDate() {
    return new Date(manifest.getStartDate());
  }

  @XmlAttribute(name = "start")
  protected String startDate;

  protected String getStartDateAsString(String startTime) {
    long d = manifest.getStartDate();
    if (d > 0) {
      startDate = DateTimeSupport.toUTC(d);
    }
    return startDate;
  }

  public void setStartDateAsString(String startTime) {
    if (startTime != null && !"0".equals(startTime)) {
      try {
        manifest.setStartDate(DateTimeSupport.fromUTC(startTime));
      } catch (Exception e) {
        log_.info("Unable to parse start time {}", startTime);
      }
    }
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#elements()
   */
  public Iterable<MediaPackageElement> elements() {
    return Arrays.asList(manifest.getEntries());
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.media.mediapackage.MediaPackage#getElements()
   */
  public MediaPackageElement[] getElements() {
    return manifest.getEntries();
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#getElementByReference(org.opencastproject.media.mediapackage.MediaPackageReference)
   */
  public MediaPackageElement getElementByReference(
          MediaPackageReference reference) {
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
   * @see org.opencastproject.media.mediapackage.MediaPackage#getElementById(java.lang.String)
   */
  public MediaPackageElement[] getElementsByTag(String tag) {
    List<MediaPackageElement> result = new ArrayList<MediaPackageElement>();
    for (MediaPackageElement element : manifest.getEntries()) {
      if (element.containsTag(tag)) {
        result.add(element);
      }
    }
    return result.toArray(new MediaPackageElement[result.size()]);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.media.mediapackage.MediaPackage#getElementsByFlavor(org.opencastproject.media.mediapackage.MediaPackageElementFlavor)
   */
  public MediaPackageElement[] getElementsByFlavor(
          MediaPackageElementFlavor flavor) {
    if (flavor == null)
      throw new IllegalArgumentException("Flavor cannot be null");

    List<MediaPackageElement> elements = new ArrayList<MediaPackageElement>();
    for (MediaPackageElement element : manifest.getEntries()) {
      if (flavor.equals(element.getFlavor()))
        elements.add(element);
    }
    return elements.toArray(new MediaPackageElement[elements.size()]);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#contains(org.opencastproject.media.mediapackage.MediaPackageElement)
   */
  public boolean contains(MediaPackageElement element) {
    return manifest.contains(element);
  }

  /**
   * Returns <code>true</code> if the media package contains an element with the
   * specified identifier.
   * 
   * @param identifier
   *          the identifier
   * @return <code>true</code> if the media package contains an element with
   *         this identifier
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
  public void add(Catalog catalog) throws UnsupportedElementException {
    try {
      integrateCatalog(catalog);
      manifest.add(catalog);
      fireElementAdded(catalog);
    } catch (IOException e) {
      throw new UnsupportedElementException("Error integrating " + catalog
              + " into media package: " + e.getMessage());
    }
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#add(org.opencastproject.media.mediapackage.Track)
   */
  public void add(Track track) throws UnsupportedElementException {
    try {
      integrateTrack(track);
      manifest.add(track);
      fireElementAdded(track);
    } catch (IOException e) {
      throw new UnsupportedElementException("Error integrating " + track
              + " into media package: " + e.getMessage());
    }
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#add(org.opencastproject.media.mediapackage.Attachment)
   */
  public void add(Attachment attachment) throws UnsupportedElementException {
    try {
      integrateAttachment(attachment);
      manifest.add(attachment);
      fireElementAdded(attachment);
    } catch (IOException e) {
      throw new UnsupportedElementException("Error integrating " + attachment
              + " into media package: " + e.getMessage());
    }
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#getCatalog(java.lang.String)
   */
  public Catalog getCatalog(String catalogId) {
    return manifest.getCatalog(catalogId);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#getCatalogs()
   */
  @XmlElementWrapper(name = "metadata")
  @XmlElement(name = "catalog")
  public Catalog[] getCatalogs() {
    return manifest.getCatalogs();
  }

  protected void setCatalogs(Catalog[] catalogs) {
    List<Catalog> newCatalogs = Arrays.asList(catalogs);
    List<Catalog> oldCatalogs = Arrays.asList(manifest.getCatalogs());
    // remove any catalogs not in this array
    for (Catalog existing : oldCatalogs) {
      if (!newCatalogs.contains(existing)) {
        manifest.remove(existing);
      }
    }
    for (Catalog newCatalog : newCatalogs) {
      if (!oldCatalogs.contains(newCatalog)) {
        manifest.add(newCatalog);
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.media.mediapackage.MediaPackage#getCatalogsByTag(java.lang.String)
   */
  public Catalog[] getCatalogsByTag(String tag) {
    return manifest.getCatalogsByTag(tag);
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
  public Catalog[] getCatalogs(MediaPackageElementFlavor flavor,
          MediaPackageReference reference) {
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
  public boolean hasCatalogs(MediaPackageElementFlavor type,
          MediaPackageReference reference) {
    return manifest.hasCatalogs(type, reference);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#hasCatalogs(MediaPackageElementFlavor)
   */
  public boolean hasCatalogs(MediaPackageElementFlavor type) {
    return manifest.hasCatalogs(type);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#getTrack(java.lang.String)
   */
  public Track getTrack(String trackId) {
    return manifest.getTrack(trackId);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#getTracks()
   */
  @XmlElementWrapper(name = "media")
  @XmlElement(name = "track")
  public Track[] getTracks() {
    return manifest.getTracks();
  }

  protected void setTracks(Track[] tracks) {
    List<Track> newTracks = Arrays.asList(tracks);
    List<Track> oldTracks = Arrays.asList(manifest.getTracks());
    // remove any catalogs not in this array
    for (Track existing : oldTracks) {
      if (!newTracks.contains(existing)) {
        manifest.remove(existing);
      }
    }
    for (Track newTrack : newTracks) {
      if (!oldTracks.contains(newTrack)) {
        manifest.add(newTrack);
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.media.mediapackage.MediaPackage#getTracksByTag(java.lang.String)
   */
  public Track[] getTracksByTag(String tag) {
    return manifest.getTracksByTag(tag);
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
  public Track[] getTracks(MediaPackageElementFlavor flavor,
          MediaPackageReference reference) {
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
  public MediaPackageElement[] getUnclassifiedElements(
          MediaPackageElementFlavor type) {
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
   * @see org.opencastproject.media.mediapackage.MediaPackage#getAttachment(java.lang.String)
   */
  public Attachment getAttachment(String attachmentId) {
    return manifest.getAttachment(attachmentId);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#getAttachments()
   */
  @XmlElementWrapper(name = "attachments")
  @XmlElement(name = "attachment")
  public Attachment[] getAttachments() {
    return manifest.getAttachments();
  }

  public void setAttachments(Attachment[] catalogs) {
    List<Attachment> newAttachments = Arrays.asList(catalogs);
    List<Attachment> oldAttachments = Arrays.asList(manifest.getAttachments());
    // remove any catalogs not in this array
    for (Attachment existing : oldAttachments) {
      if (!newAttachments.contains(existing)) {
        manifest.remove(existing);
      }
    }
    for (Attachment newAttachment : newAttachments) {
      if (!oldAttachments.contains(newAttachment)) {
        manifest.add(newAttachment);
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.media.mediapackage.MediaPackage#getAttachmentsByTag(java.lang.String)
   */
  public Attachment[] getAttachmentsByTag(String tag) {
    return manifest.getAttachmentsByTag(tag);
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
  public Attachment[] getAttachments(MediaPackageElementFlavor flavor,
          MediaPackageReference reference) {
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
  public void remove(MediaPackageElement element) {
    removeElement(element);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#remove(org.opencastproject.media.mediapackage.Attachment)
   */
  public void remove(Attachment attachment) {
    removeElement(attachment);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#remove(org.opencastproject.media.mediapackage.Catalog)
   */
  public void remove(Catalog catalog) {
    removeElement(catalog);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#remove(org.opencastproject.media.mediapackage.Track)
   */
  public void remove(Track track) {
    removeElement(track);
  }

  /**
   * Removes an element from the media package
   * 
   * @param element
   */
  protected void removeElement(MediaPackageElement element) {
    if (element instanceof AbstractMediaPackageElement) {
      ((AbstractMediaPackageElement) element).setMediaPackage(null);
    }
    manifest.remove(element);
    fireElementRemoved(element);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#getCover()
   */
  public Attachment getCover() {
    Attachment[] covers = getAttachments(MediaPackageElements.COVER_FLAVOR);
    if (covers.length > 0) {
      return covers[0];
    }
    return null;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#setCover(org.opencastproject.media.mediapackage.Attachment)
   */
  public void setCover(Attachment cover) throws MediaPackageException,
          UnsupportedElementException {
    Attachment oldCover = getCover();
    if (oldCover != null)
      remove(oldCover);
    add(cover);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#removeCover()
   */
  public void removeCover() {
    Attachment[] covers = getAttachments(MediaPackageElements.COVER_FLAVOR);
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
   * @see org.opencastproject.media.mediapackage.MediaPackage#pack(org.opencastproject.media.mediapackage.MediaPackagePackager,
   *      java.io.OutputStream)
   */
  public void pack(MediaPackagePackager packager, OutputStream out)
          throws IOException, MediaPackageException {
    if (packager == null)
      throw new IllegalArgumentException("The packager must not be null");
    if (out == null)
      throw new IllegalArgumentException("The output stream must not be null");
    packager.pack(this, out);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#add(uri)
   */
  public MediaPackageElement add(URI url) throws UnsupportedElementException {
    if (url == null)
      throw new IllegalArgumentException("Argument 'url' may not be null");

    if (mediaPackageElementBuilder == null) {
      mediaPackageElementBuilder = MediaPackageElementBuilderFactory
              .newInstance().newElementBuilder();
    }
    MediaPackageElement element = mediaPackageElementBuilder
            .elementFromURI(url);
    add(element);
    return element;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#add(URI,
   *      org.opencastproject.media.mediapackage.MediaPackageElement.Type,
   *      org.opencastproject.media.mediapackage.MediaPackageElementFlavor)
   */
  public MediaPackageElement add(URI uri, Type type,
          MediaPackageElementFlavor flavor) throws UnsupportedElementException {
    if (uri == null)
      throw new IllegalArgumentException("Argument 'url' may not be null");
    if (type == null)
      throw new IllegalArgumentException("Argument 'type' may not be null");

    if (mediaPackageElementBuilder == null) {
      mediaPackageElementBuilder = MediaPackageElementBuilderFactory
              .newInstance().newElementBuilder();
    }
    MediaPackageElement element = mediaPackageElementBuilder.elementFromURI(
            uri, type, flavor);
    add(element);
    return element;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.media.mediapackage.MediaPackage#add(org.opencastproject.media.mediapackage.MediaPackageElement)
   */
  public void add(MediaPackageElement element)
          throws UnsupportedElementException {
    try {
      if (element.getElementType().equals(MediaPackageElement.Type.Track)
              && element instanceof Track) {
        integrateTrack((Track) element);
      } else if (element.getElementType().equals(
              MediaPackageElement.Type.Catalog)
              && element instanceof Catalog) {
        integrateCatalog((Catalog) element);
      } else if (element.getElementType().equals(
              MediaPackageElement.Type.Attachment)
              && element instanceof Attachment) {
        integrateAttachment((Attachment) element);
      } else {
        integrate(element);
      }
      manifest.add(element);
      fireElementAdded(element);
    } catch (IOException e) {
      throw new UnsupportedElementException("Error integrating " + element
              + " into media package: " + e.getMessage());
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @throws UnsupportedElementException
   * @throws MediaPackageException
   * @see org.opencastproject.media.mediapackage.MediaPackage#addDerived(org.opencastproject.media.mediapackage.MediaPackageElement,
   *      org.opencastproject.media.mediapackage.MediaPackageElement)
   */
  public void addDerived(MediaPackageElement derivedElement,
          MediaPackageElement sourceElement) throws UnsupportedElementException {
    if (derivedElement == null)
      throw new IllegalArgumentException("The derived element is null");
    if (sourceElement == null)
      throw new IllegalArgumentException("The source element is null");
    if (!manifest.contains(sourceElement))
      throw new IllegalStateException(
              "The sourceElement needs to be part of the media package");

    derivedElement.referTo(sourceElement);
    add(derivedElement);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.media.mediapackage.MediaPackage#getDerived(org.opencastproject.media.mediapackage.MediaPackageElement,
   *      org.opencastproject.media.mediapackage.MediaPackageElementFlavor)
   */
  public MediaPackageElement[] getDerived(MediaPackageElement sourceElement,
          MediaPackageElementFlavor derivateFlavor) {
    if (sourceElement == null)
      throw new IllegalArgumentException("Source element cannot be null");
    if (derivateFlavor == null)
      throw new IllegalArgumentException("Derivate flavor cannot be null");

    MediaPackageReference reference = new MediaPackageReferenceImpl(
            sourceElement);
    List<MediaPackageElement> elements = new ArrayList<MediaPackageElement>();
    for (MediaPackageElement element : manifest.getEntries()) {
      if (derivateFlavor.equals(element.getFlavor())
              && reference.equals(element.getReference()))
        elements.add(element);
    }
    return elements.toArray(new MediaPackageElement[elements.size()]);
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
          log_.error("MediaPackageOberserver " + o
                  + " throw exception while processing callback", th);
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
          log_.error("MediaPackageObserver " + o
                  + " threw exception while processing callback", th);
        }
      }
    }
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackage#renameTo(org.opencastproject.media.mediapackage.identifier.Id)
   */
  public void renameTo(Id identifier) {
    manifest.setIdentifier(identifier);
  }

  /**
   * Integrates the element into the media package. This mainly involves moving
   * the element into the media package file structure.
   * 
   * @param element
   *          the element to integrate
   * @throws IOException
   *           if integration of the element failed
   */
  private void integrate(MediaPackageElement element) throws IOException {
    if (element instanceof AbstractMediaPackageElement)
      ((AbstractMediaPackageElement) element).setMediaPackage(this);
  }

  /**
   * Integrates the catalog into the media package. This mainly involves moving
   * the catalog into the media package file structure.
   * 
   * @param catalog
   *          the catalog to integrate
   * @throws IOException
   *           if integration of the catalog failed
   */
  private void integrateCatalog(Catalog catalog) throws IOException {
    // Check (uniqueness of) catalog identifier
    String id = catalog.getIdentifier();
    if (id == null || contains(id)) {
      id = createElementIdentifier("catalog", manifest.getCatalogs().length + 1);
      catalog.setIdentifier(id.toString());
    }
    integrate(catalog);
  }

  /**
   * Integrates the track into the media package. This mainly involves moving
   * the track into the media package file structure.
   * 
   * @param track
   *          the track to integrate
   * @throws IOException
   *           if integration of the track failed
   */
  private void integrateTrack(Track track) throws IOException {
    // Check (uniqueness of) track identifier
    String id = track.getIdentifier();
    if (id == null || contains(id)) {
      id = createElementIdentifier("track", manifest.getTracks().length + 1);
      track.setIdentifier(id.toString());
    }
    integrate(track);
  }

  /**
   * Integrates the attachment into the media package. This mainly involves
   * moving the attachment into the media package file structure.
   * 
   * @param attachment
   *          the attachment to integrate
   * @throws IOException
   *           if integration of the attachment failed
   */
  private void integrateAttachment(Attachment attachment) throws IOException {
    // Check (uniqueness of) attachment identifier
    String id = attachment.getIdentifier();
    if (id == null || contains(id)) {
      id = createElementIdentifier("attachment",
              manifest.getAttachments().length + 1);
      attachment.setIdentifier(id.toString());
    }
    integrate(attachment);
  }

  /**
   * Returns a media package element identifier with the given prefix and the
   * given number or a higher one as the suffix. The identifier will be unique
   * within the media package.
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
   * @see org.opencastproject.media.mediapackage.MediaPackage#verify()
   */
  public void verify() throws MediaPackageException {
    for (MediaPackageElement e : manifest.getEntries()) {
      e.verify();
    }
  }

  /**
   * Public constructor for JAXB deserialization. Do not use this constructor
   * directly, instead use a {@link MediaPackageBuilderFactory}.
   * 
   * @param xml
   *          the serialized xml string
   * @return the deserialized media package
   * @throws JAXBException
   *           if deserialization fails
   */
  public static MediaPackageImpl valueOf(String xml) throws JAXBException {
    return (MediaPackageImpl) context.createUnmarshaller().unmarshal(
            IOUtils.toInputStream(xml));
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.media.mediapackage.MediaPackage#toXml()
   */
  public String toXml() throws MediaPackageException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    toXml(os, false);
    return os.toString();
  }

  /**
   * Serializes the media package to a dom document.
   * 
   * @param serializer
   *          the media package serializer
   * @throws ParserConfigurationException
   * @throws TransformerException
   * @see org.opencastproject.media.mediapackage.MediaPackage#toXml()
   */
  public Document toXml(MediaPackageSerializer serializer)
          throws MediaPackageException {
    try {
      return manifest.toXml(serializer);
    } catch (TransformerException e) {
      throw new MediaPackageException(e);
    } catch (ParserConfigurationException e) {
      throw new MediaPackageException(e);
    }
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
   * {@inheritDoc}
   * 
   * @see java.lang.Object#clone()
   */
  public Object clone() {
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      this.toXml(out, false);
      return MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
              .loadFromXml(new ByteArrayInputStream(out.toByteArray()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
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

  // TODO: Comment me!
  static class Adapter extends XmlAdapter<MediaPackageImpl, MediaPackage> {
    public MediaPackageImpl marshal(MediaPackage mp) throws Exception {
      return (MediaPackageImpl) mp;
    }

    public MediaPackage unmarshal(MediaPackageImpl mp) throws Exception {
      return mp;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.media.mediapackage.MediaPackage#toXml(OutputStream,
   *      boolean)
   */
  @Override
  public void toXml(OutputStream out, boolean format)
          throws MediaPackageException {
    try {
      Marshaller marshaller = context.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, format);
      marshaller.marshal(this, out);
    } catch (JAXBException e) {
      throw new MediaPackageException(e);
    }
  }

  /**
   * Reads the media package from the input stream.
   * 
   * @param xml
   *          the input stream
   * @return the deserialized media package
   */
  public static MediaPackageImpl valueOf(InputStream xml)
          throws MediaPackageException {
    try {
      Unmarshaller unmarshaller = context.createUnmarshaller();
      return (MediaPackageImpl) unmarshaller.unmarshal(xml);
    } catch (JAXBException e) {
      throw new MediaPackageException(e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.media.mediapackage.MediaPackage#getContributors()
   */
  @Override
  public String[] getContributors() {
    if (contributors == null)
      return new String[] {};
    return contributors.toArray(new String[contributors.size()]);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.media.mediapackage.MediaPackage#getCreators()
   */
  @Override
  public String[] getCreators() {
    if (creators == null)
      return new String[] {};
    return creators.toArray(new String[creators.size()]);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.media.mediapackage.MediaPackage#getLanguage()
   */
  @Override
  public String getLanguage() {
    return language;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.media.mediapackage.MediaPackage#getLicense()
   */
  @Override
  public String getLicense() {
    return license;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.media.mediapackage.MediaPackage#getSeries()
   */
  @Override
  public Id getSeries() {
    return series;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.media.mediapackage.MediaPackage#getSubjects()
   */
  @Override
  public String[] getSubjects() {
    if (subjects == null)
      return new String[] {};
    return subjects.toArray(new String[subjects.size()]);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.media.mediapackage.MediaPackage#getTitle()
   */
  @Override
  public String getTitle() {
    return title;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.media.mediapackage.MediaPackage#addContributor(java.lang.String)
   */
  @Override
  public void addContributor(String contributor) {
    if (contributors == null)
      contributors = new TreeSet<String>();
    contributors.add(contributor);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.media.mediapackage.MediaPackage#addCreator(java.lang.String)
   */
  @Override
  public void addCreator(String creator) {
    if (creators == null)
      creators = new TreeSet<String>();
    creators.add(creator);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.media.mediapackage.MediaPackage#addSubject(java.lang.String)
   */
  @Override
  public void addSubject(String subject) {
    if (subjects == null)
      subjects = new TreeSet<String>();
    subjects.add(subject);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.media.mediapackage.MediaPackage#removeContributor(java.lang.String)
   */
  @Override
  public void removeContributor(String contributor) {
    if (contributors != null)
      contributors.remove(contributor);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.media.mediapackage.MediaPackage#removeCreator(java.lang.String)
   */
  @Override
  public void removeCreator(String creator) {
    if (creators != null)
      creators.remove(creator);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.media.mediapackage.MediaPackage#removeSubject(java.lang.String)
   */
  @Override
  public void removeSubject(String subject) {
    if (subjects != null)
      subjects.remove(subject);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.media.mediapackage.MediaPackage#setDate(java.util.Date)
   */
  @Override
  public void setDate(Date date) {
    if (date != null)
      this.manifest.setStartDate(date.getTime());
    else
      this.manifest.setStartDate(0);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.media.mediapackage.MediaPackage#setLanguage(java.lang.String)
   */
  @Override
  public void setLanguage(String language) {
    this.language = language;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.media.mediapackage.MediaPackage#setLicense(java.lang.String)
   */
  @Override
  public void setLicense(String license) {
    this.license = license;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.media.mediapackage.MediaPackage#setSeries(org.opencastproject.media.mediapackage.identifier.Id)
   */
  @Override
  public void setSeries(Id identifier) {
    this.series = identifier;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.media.mediapackage.MediaPackage#setTitle(java.lang.String)
   */
  @Override
  public void setTitle(String title) {
    this.title = title;
  }

}
