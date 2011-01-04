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

package org.opencastproject.mediapackage;

import org.opencastproject.mediapackage.MediaPackageElement.Type;
import org.opencastproject.mediapackage.identifier.Id;
import org.opencastproject.util.DateTimeSupport;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

/**
 * Default implementation for a media media package.
 * 
 * TODO: Finish code documentation
 */
@XmlType(name = "mediapackage", namespace = "http://mediapackage.opencastproject.org", propOrder = { "title", "series",
        "seriesTitle", "creators", "contributors", "subjects", "license", "language", "tracks", "catalogs",
        "attachments" })
@XmlRootElement(name = "mediapackage", namespace = "http://mediapackage.opencastproject.org")
@XmlAccessorType(XmlAccessType.NONE)
public final class MediaPackageImpl implements MediaPackage {

  /** the logging facility provided by log4j */
  private static final Logger logger = LoggerFactory.getLogger(MediaPackageImpl.class.getName());

  /**
   * The prefix indicating that a tag should be excluded from a search for elements using
   * {@link #getElementsByTags(Collection)}
   */
  public static final String NEGATE_TAG_PREFIX = "-";

  /** Context for serializing and deserializing */
  static final JAXBContext context;

  /** The media media package meta data */
  private ManifestImpl manifest = null;

  /** List of observers */
  protected List<MediaPackageObserver> observers = new ArrayList<MediaPackageObserver>();

  /** The media package element builder, may remain <code>null</code> */
  private MediaPackageElementBuilder mediaPackageElementBuilder = null;

  @XmlElement(name = "title")
  protected String title = null;

  @XmlElement(name = "seriestitle")
  protected String seriesTitle = null;

  @XmlElement(name = "language")
  protected String language = null;

  @XmlElement(name = "series")
  protected String series = null;

  @XmlElement(name = "license")
  protected String license = null;

  @XmlElementWrapper(name = "creators")
  @XmlElement(name = "creator")
  protected Set<String> creators = null;

  @XmlElementWrapper(name = "contributors")
  @XmlElement(name = "contributor")
  protected Set<String> contributors = null;

  @XmlElementWrapper(name = "subjects")
  @XmlElement(name = "subject")
  protected Set<String> subjects = null;

  static {
    try {
      context = JAXBContext.newInstance("org.opencastproject.mediapackage", MediaPackageImpl.class.getClassLoader());
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
   * Creates a new media media package derived from the given media media package catalog document.
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
   * @see org.opencastproject.mediapackage.MediaPackage#getIdentifier()
   */
  @XmlAttribute(name = "id")
  public Id getIdentifier() {
    return manifest.getIdentifier();
  }

  public void setIdentifier(Id identifier) {
    manifest.setIdentifier(identifier);
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackage#getDuration()
   */
  @XmlAttribute(name = "duration")
  public long getDuration() {
    return manifest.getDuration();
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackage#getDate()
   */
  public Date getDate() {
    return new Date(manifest.getStartDate());
  }

  @XmlAttribute(name = "start")
  public String getStartDateAsString() {
    long d = manifest.getStartDate();
    String startDate = "";
    if (d > 0) {
      startDate = DateTimeSupport.toUTC(d);
    }
    return startDate;
  }

  public void setStartDateAsString(String startTime) {
    if (startTime != null && !"0".equals(startTime) && !startTime.isEmpty()) {
      try {
        manifest.setStartDate(DateTimeSupport.fromUTC(startTime));
      } catch (Exception e) {
        logger.info("Unable to parse start time {}", startTime);
      }
    } else {
      manifest.setStartDate(0);
    }
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackage#elements()
   */
  public Iterable<MediaPackageElement> elements() {
    return Arrays.asList(manifest.getEntries());
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackage#getElements()
   */
  public MediaPackageElement[] getElements() {
    return manifest.getEntries();
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackage#getElementByReference(org.opencastproject.mediapackage.MediaPackageReference)
   */
  public MediaPackageElement getElementByReference(MediaPackageReference reference) {
    MediaPackageElement[] e = getElementsByReference(reference, false);
    return (e != null && e.length > 0) ? e[0] : null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackage#getElementsByReference(org.opencastproject.mediapackage.MediaPackageReference,
   *      boolean)
   */
  public MediaPackageElement[] getElementsByReference(MediaPackageReference reference, boolean includeDerived) {
    return manifest.getElements(reference, includeDerived);
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackage#getElementById(java.lang.String)
   */
  public MediaPackageElement getElementById(String id) {
    for (MediaPackageElement element : manifest.getEntries()) {
      if (id.equals(element.getIdentifier()))
        return element;
    }
    return null;
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackage#getElementById(java.lang.String)
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
   * @see org.opencastproject.mediapackage.MediaPackage#getElementsByTags(java.util.Collection)
   */
  @Override
  public MediaPackageElement[] getElementsByTags(Collection<String> tags) {
    if (tags == null || tags.isEmpty())
      return getElements();
    Set<String> keep = new HashSet<String>();
    Set<String> lose = new HashSet<String>();
    for (String tag : tags) {
      if (StringUtils.isBlank(tag))
        continue;
      if (tag.startsWith(NEGATE_TAG_PREFIX)) {
        lose.add(tag.substring(NEGATE_TAG_PREFIX.length()));
      } else {
        keep.add(tag);
      }
    }
    List<MediaPackageElement> result = new ArrayList<MediaPackageElement>();
    for (MediaPackageElement element : manifest.getEntries()) {
      boolean add = false;
      for (String elementTag : element.getTags()) {
        if (lose.contains(elementTag)) {
          add = false;
          break;
        } else if (keep.contains(elementTag)) {
          add = true;
        }
      }
      if (add) {
        result.add(element);
      }
    }
    return result.toArray(new MediaPackageElement[result.size()]);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackage#getAttachmentsByTags(java.util.Collection)
   */
  @Override
  public Attachment[] getAttachmentsByTags(Collection<String> tags) {
    MediaPackageElement[] matchingElements = getElementsByTags(tags);
    List<Attachment> attachments = new ArrayList<Attachment>();
    for (MediaPackageElement element : matchingElements) {
      if (Attachment.TYPE.equals(element.getElementType())) {
        attachments.add((Attachment) element);
      }
    }
    return attachments.toArray(new Attachment[0]);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackage#getCatalogsByTags(java.util.Collection)
   */
  @Override
  public Catalog[] getCatalogsByTags(Collection<String> tags) {
    MediaPackageElement[] matchingElements = getElementsByTags(tags);
    List<Catalog> catalogs = new ArrayList<Catalog>();
    for (MediaPackageElement element : matchingElements) {
      if (Catalog.TYPE.equals(element.getElementType())) {
        catalogs.add((Catalog) element);
      }
    }
    return catalogs.toArray(new Catalog[0]);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackage#getTracksByTags(java.util.Collection)
   */
  @Override
  public Track[] getTracksByTags(Collection<String> tags) {
    MediaPackageElement[] matchingElements = getElementsByTags(tags);
    List<Track> tracks = new ArrayList<Track>();
    for (MediaPackageElement element : matchingElements) {
      if (Track.TYPE.equals(element.getElementType())) {
        tracks.add((Track) element);
      }
    }
    return tracks.toArray(new Track[0]);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackage#getElementsByFlavor(org.opencastproject.mediapackage.MediaPackageElementFlavor)
   */
  public MediaPackageElement[] getElementsByFlavor(MediaPackageElementFlavor flavor) {
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
   * @see org.opencastproject.mediapackage.MediaPackage#contains(org.opencastproject.mediapackage.MediaPackageElement)
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
   * @see org.opencastproject.mediapackage.MediaPackage#add(org.opencastproject.mediapackage.Catalog)
   */
  public void add(Catalog catalog) {
    integrateCatalog(catalog);
    manifest.add(catalog);
    fireElementAdded(catalog);
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackage#add(org.opencastproject.mediapackage.Track)
   */
  public void add(Track track) {
    integrateTrack(track);
    manifest.add(track);
    fireElementAdded(track);
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackage#add(org.opencastproject.mediapackage.Attachment)
   */
  public void add(Attachment attachment) {
    integrateAttachment(attachment);
    manifest.add(attachment);
    fireElementAdded(attachment);
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackage#getCatalog(java.lang.String)
   */
  public Catalog getCatalog(String catalogId) {
    return manifest.getCatalog(catalogId);
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackage#getCatalogs()
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
        add(newCatalog);
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackage#getCatalogsByTag(java.lang.String)
   */
  public Catalog[] getCatalogsByTag(String tag) {
    return manifest.getCatalogsByTag(tag);
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackage#getCatalogs(MediaPackageElementFlavor)
   */
  public Catalog[] getCatalogs(MediaPackageElementFlavor type) {
    return manifest.getCatalogs(type);
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackage#getCatalogs(org.opencastproject.mediapackage.MediaPackageReference)
   */
  public Catalog[] getCatalogs(MediaPackageReference reference) {
    return getCatalogs(reference, false);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackage#getCatalogs(org.opencastproject.mediapackage.MediaPackageReference,
   *      boolean)
   */
  @Override
  public Catalog[] getCatalogs(MediaPackageReference reference, boolean includeDerived) {
    return manifest.getCatalogs(reference, includeDerived);
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackage#getCatalogs(org.opencastproject.mediapackage.MediaPackageElementFlavor,
   *      org.opencastproject.mediapackage.MediaPackageReference)
   */
  public Catalog[] getCatalogs(MediaPackageElementFlavor flavor, MediaPackageReference reference) {
    return manifest.getCatalogs(flavor, reference);
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackage#hasCatalogs()
   */
  public boolean hasCatalogs() {
    return manifest.hasCatalogs();
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackage#getTrack(java.lang.String)
   */
  public Track getTrack(String trackId) {
    return manifest.getTrack(trackId);
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackage#getTracks()
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
        add(newTrack);
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackage#getTracksByTag(java.lang.String)
   */
  public Track[] getTracksByTag(String tag) {
    return manifest.getTracksByTag(tag);
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackage#getTracks(MediaPackageElementFlavor)
   */
  public Track[] getTracks(MediaPackageElementFlavor type) {
    return manifest.getTracks(type);
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackage#getTracks(org.opencastproject.mediapackage.MediaPackageReference)
   */
  public Track[] getTracks(MediaPackageReference reference) {
    return getTracks(reference, false);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackage#getTracks(org.opencastproject.mediapackage.MediaPackageReference,
   *      boolean)
   */
  public Track[] getTracks(MediaPackageReference reference, boolean includeDerived) {
    return manifest.getTracks(reference, includeDerived);
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackage#getTracks(org.opencastproject.mediapackage.MediaPackageElementFlavor,
   *      org.opencastproject.mediapackage.MediaPackageReference)
   */
  public Track[] getTracks(MediaPackageElementFlavor flavor, MediaPackageReference reference) {
    return manifest.getTracks(flavor, reference);
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackage#hasTracks()
   */
  public boolean hasTracks() {
    return manifest.hasTracks();
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackage#getUnclassifiedElements()
   */
  public MediaPackageElement[] getUnclassifiedElements() {
    return manifest.getUnclassifiedElements(null);
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackage#getUnclassifiedElements(org.opencastproject.mediapackage.MediaPackageElementFlavor)
   */
  public MediaPackageElement[] getUnclassifiedElements(MediaPackageElementFlavor type) {
    return manifest.getUnclassifiedElements(type);
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackage#hasUnclassifiedElements(org.opencastproject.mediapackage.MediaPackageElementFlavor)
   */
  public boolean hasUnclassifiedElements(MediaPackageElementFlavor type) {
    return manifest.hasUnclassifiedElements(type);
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackage#hasUnclassifiedElements()
   */
  public boolean hasUnclassifiedElements() {
    return manifest.hasUnclassifiedElements();
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackage#addObserver(MediaPackageObserver)
   */
  public void addObserver(MediaPackageObserver observer) {
    synchronized (observers) {
      observers.add(observer);
    }
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackage#getAttachment(java.lang.String)
   */
  public Attachment getAttachment(String attachmentId) {
    return manifest.getAttachment(attachmentId);
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackage#getAttachments()
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
        add(newAttachment);
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackage#getAttachmentsByTag(java.lang.String)
   */
  public Attachment[] getAttachmentsByTag(String tag) {
    return manifest.getAttachmentsByTag(tag);
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackage#getAttachments(MediaPackageElementFlavor)
   */
  public Attachment[] getAttachments(MediaPackageElementFlavor flavor) {
    return manifest.getAttachments(flavor);
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackage#getAttachments(org.opencastproject.mediapackage.MediaPackageReference)
   */
  public Attachment[] getAttachments(MediaPackageReference reference) {
    return getAttachments(reference, false);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackage#getAttachments(org.opencastproject.mediapackage.MediaPackageReference,
   *      boolean)
   */
  @Override
  public Attachment[] getAttachments(MediaPackageReference reference, boolean includeDerived) {
    return manifest.getAttachments(reference, includeDerived);
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackage#getAttachments(org.opencastproject.mediapackage.MediaPackageElementFlavor,
   *      org.opencastproject.mediapackage.MediaPackageReference)
   */
  public Attachment[] getAttachments(MediaPackageElementFlavor flavor, MediaPackageReference reference) {
    return manifest.getAttachments(flavor, reference);
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackage#hasAttachments()
   */
  public boolean hasAttachments() {
    return manifest.hasAttachments();
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackage#remove(org.opencastproject.mediapackage.MediaPackageElement)
   */
  public void remove(MediaPackageElement element) {
    removeElement(element);
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackage#remove(org.opencastproject.mediapackage.Attachment)
   */
  public void remove(Attachment attachment) {
    removeElement(attachment);
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackage#remove(org.opencastproject.mediapackage.Catalog)
   */
  public void remove(Catalog catalog) {
    removeElement(catalog);
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackage#remove(org.opencastproject.mediapackage.Track)
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
   * @see org.opencastproject.mediapackage.MediaPackage#removeObserver(MediaPackageObserver)
   */
  public void removeObserver(MediaPackageObserver observer) {
    synchronized (observers) {
      observers.remove(observer);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackage#add(java.net.URI)
   */
  public MediaPackageElement add(URI url) {
    if (url == null)
      throw new IllegalArgumentException("Argument 'url' may not be null");

    if (mediaPackageElementBuilder == null) {
      mediaPackageElementBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
    }
    MediaPackageElement element = mediaPackageElementBuilder.elementFromURI(url);
    add(element);
    return element;
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackage#add(URI,
   *      org.opencastproject.mediapackage.MediaPackageElement.Type,
   *      org.opencastproject.mediapackage.MediaPackageElementFlavor)
   */
  public MediaPackageElement add(URI uri, Type type, MediaPackageElementFlavor flavor) {
    if (uri == null)
      throw new IllegalArgumentException("Argument 'url' may not be null");
    if (type == null)
      throw new IllegalArgumentException("Argument 'type' may not be null");

    if (mediaPackageElementBuilder == null) {
      mediaPackageElementBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
    }
    MediaPackageElement element = mediaPackageElementBuilder.elementFromURI(uri, type, flavor);
    add(element);
    return element;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackage#add(org.opencastproject.mediapackage.MediaPackageElement)
   */
  public void add(MediaPackageElement element) {
    if (element.getElementType().equals(MediaPackageElement.Type.Track) && element instanceof Track) {
      integrateTrack((Track) element);
    } else if (element.getElementType().equals(MediaPackageElement.Type.Catalog) && element instanceof Catalog) {
      integrateCatalog((Catalog) element);
    } else if (element.getElementType().equals(MediaPackageElement.Type.Attachment) && element instanceof Attachment) {
      integrateAttachment((Attachment) element);
    } else {
      integrate(element);
    }
    manifest.add(element);
    fireElementAdded(element);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackage#addDerived(org.opencastproject.mediapackage.MediaPackageElement,
   *      org.opencastproject.mediapackage.MediaPackageElement)
   */
  public void addDerived(MediaPackageElement derivedElement, MediaPackageElement sourceElement) {
    addDerived(derivedElement, sourceElement, null);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackage#addDerived(org.opencastproject.mediapackage.MediaPackageElement,
   *      org.opencastproject.mediapackage.MediaPackageElement, java.util.Map)
   */
  @Override
  public void addDerived(MediaPackageElement derivedElement, MediaPackageElement sourceElement,
          Map<String, String> properties) {
    if (derivedElement == null)
      throw new IllegalArgumentException("The derived element is null");
    if (sourceElement == null)
      throw new IllegalArgumentException("The source element is null");
    if (!manifest.contains(sourceElement))
      throw new IllegalStateException("The sourceElement needs to be part of the media package");

    derivedElement.referTo(sourceElement);
    add(derivedElement);

    if (properties != null) {
      MediaPackageReference ref = derivedElement.getReference();
      for (Map.Entry<String, String> entry : properties.entrySet()) {
        ref.setProperty(entry.getKey(), entry.getValue());
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackage#getDerived(org.opencastproject.mediapackage.MediaPackageElement,
   *      org.opencastproject.mediapackage.MediaPackageElementFlavor)
   */
  public MediaPackageElement[] getDerived(MediaPackageElement sourceElement, MediaPackageElementFlavor derivateFlavor) {
    if (sourceElement == null)
      throw new IllegalArgumentException("Source element cannot be null");
    if (derivateFlavor == null)
      throw new IllegalArgumentException("Derivate flavor cannot be null");

    MediaPackageReference reference = new MediaPackageReferenceImpl(sourceElement);
    List<MediaPackageElement> elements = new ArrayList<MediaPackageElement>();
    for (MediaPackageElement element : manifest.getEntries()) {
      if (derivateFlavor.equals(element.getFlavor()) && reference.equals(element.getReference()))
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
          logger.error("MediaPackageOberserver " + o + " throw exception while processing callback", th);
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
          logger.error("MediaPackageObserver " + o + " threw exception while processing callback", th);
        }
      }
    }
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackage#renameTo(org.opencastproject.mediapackage.identifier.Id)
   */
  public void renameTo(Id identifier) {
    manifest.setIdentifier(identifier);
  }

  /**
   * Integrates the element into the media package. This mainly involves moving the element into the media package file
   * structure.
   * 
   * @param element
   *          the element to integrate
   */
  private void integrate(MediaPackageElement element) {
    if (element instanceof AbstractMediaPackageElement)
      ((AbstractMediaPackageElement) element).setMediaPackage(this);
  }

  /**
   * Integrates the catalog into the media package. This mainly involves moving the catalog into the media package file
   * structure.
   * 
   * @param catalog
   *          the catalog to integrate
   */
  private void integrateCatalog(Catalog catalog) {
    // Check (uniqueness of) catalog identifier
    String id = catalog.getIdentifier();
    if (id == null || contains(id)) {
      id = createElementIdentifier("catalog", manifest.getCatalogs().length + 1);
      catalog.setIdentifier(id.toString());
    }
    integrate(catalog);
  }

  /**
   * Integrates the track into the media package. This mainly involves moving the track into the media package file
   * structure.
   * 
   * @param track
   *          the track to integrate
   */
  private void integrateTrack(Track track) {
    // Check (uniqueness of) track identifier
    String id = track.getIdentifier();
    if (id == null || contains(id)) {
      id = createElementIdentifier("track", manifest.getTracks().length + 1);
      track.setIdentifier(id.toString());
    }
    integrate(track);
  }

  /**
   * Integrates the attachment into the media package. This mainly involves moving the attachment into the media package
   * file structure.
   * 
   * @param attachment
   *          the attachment to integrate
   */
  private void integrateAttachment(Attachment attachment) {
    // Check (uniqueness of) attachment identifier
    String id = attachment.getIdentifier();
    if (id == null || contains(id)) {
      id = createElementIdentifier("attachment", manifest.getAttachments().length + 1);
      attachment.setIdentifier(id.toString());
    }
    integrate(attachment);
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
   * @see org.opencastproject.mediapackage.MediaPackage#verify()
   */
  public void verify() throws MediaPackageException {
    for (MediaPackageElement e : manifest.getEntries()) {
      e.verify();
    }
  }

  /**
   * Unmarshals XML representation of a MediaPackage via JAXB.
   * 
   * @param xml
   *          the serialized xml string
   * @return the deserialized media package
   * @throws MediaPackageException
   */
  public static MediaPackageImpl valueOf(String xml) throws MediaPackageException {
    try {
      return MediaPackageImpl.valueOf(IOUtils.toInputStream(xml, "UTF-8"));
    } catch (IOException e) {
      throw new MediaPackageException(e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackage#toXml()
   */
  public String toXml() {
    try {
      Marshaller marshaller = context.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);
      StringWriter writer = new StringWriter();
      marshaller.marshal(this, writer);
      return writer.toString();
    } catch (JAXBException e) {
      throw new IllegalStateException(e.getLinkedException() != null ? e.getLinkedException() : e);
    }
  }

  /**
   * Serializes the media package to a dom document.
   * 
   * @param serializer
   *          the media package serializer
   * @throws ParserConfigurationException
   * @throws TransformerException
   * @see org.opencastproject.mediapackage.MediaPackage#toXml()
   */
  public Document toXml(MediaPackageSerializer serializer) throws MediaPackageException {
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
      String xml = this.toXml();
      return MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().loadFromXml(xml);
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

  /**
   * A JAXB adapter that allows the {@link MediaPackage} interface to be un/marshalled
   */
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
   * @see org.opencastproject.mediapackage.MediaPackage#toXml(OutputStream, boolean)
   */
  @Override
  public void toXml(OutputStream out, boolean format) throws MediaPackageException {
    try {
      Marshaller marshaller = context.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, format);
      marshaller.marshal(this, out);
    } catch (JAXBException e) {
      throw new MediaPackageException(e.getLinkedException() != null ? e.getLinkedException() : e);
    }
  }

  /**
   * Reads the media package from the input stream.
   * 
   * @param xml
   *          the input stream
   * @return the deserialized media package
   */
  public static MediaPackageImpl valueOf(InputStream xml) throws MediaPackageException {
    try {
      Unmarshaller unmarshaller = context.createUnmarshaller();
      Source source = new StreamSource(xml);
      return unmarshaller.unmarshal(source, MediaPackageImpl.class).getValue();
    } catch (JAXBException e) {
      throw new MediaPackageException(e.getLinkedException() != null ? e.getLinkedException() : e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackage#getContributors()
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
   * @see org.opencastproject.mediapackage.MediaPackage#getCreators()
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
   * @see org.opencastproject.mediapackage.MediaPackage#getLanguage()
   */
  @Override
  public String getLanguage() {
    return language;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackage#getLicense()
   */
  @Override
  public String getLicense() {
    return license;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackage#getSeries()
   */
  @Override
  public String getSeries() {
    return series;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackage#getSubjects()
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
   * @see org.opencastproject.mediapackage.MediaPackage#getTitle()
   */
  @Override
  public String getTitle() {
    return title;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackage#getSeriesTitle()
   */
  public String getSeriesTitle() {
    return seriesTitle;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackage#setSeriesTitle(java.lang.String)
   */
  public void setSeriesTitle(String seriesTitle) {
    this.seriesTitle = seriesTitle;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackage#addContributor(java.lang.String)
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
   * @see org.opencastproject.mediapackage.MediaPackage#addCreator(java.lang.String)
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
   * @see org.opencastproject.mediapackage.MediaPackage#addSubject(java.lang.String)
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
   * @see org.opencastproject.mediapackage.MediaPackage#removeContributor(java.lang.String)
   */
  @Override
  public void removeContributor(String contributor) {
    if (contributors != null)
      contributors.remove(contributor);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackage#removeCreator(java.lang.String)
   */
  @Override
  public void removeCreator(String creator) {
    if (creators != null)
      creators.remove(creator);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackage#removeSubject(java.lang.String)
   */
  @Override
  public void removeSubject(String subject) {
    if (subjects != null)
      subjects.remove(subject);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackage#setDate(java.util.Date)
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
   * @see org.opencastproject.mediapackage.MediaPackage#setLanguage(java.lang.String)
   */
  @Override
  public void setLanguage(String language) {
    this.language = language;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackage#setLicense(java.lang.String)
   */
  @Override
  public void setLicense(String license) {
    this.license = license;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackage#setSeries(java.lang.String)
   */
  @Override
  public void setSeries(String identifier) {
    this.series = identifier;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackage#setTitle(java.lang.String)
   */
  @Override
  public void setTitle(String title) {
    this.title = title;
  }

}
