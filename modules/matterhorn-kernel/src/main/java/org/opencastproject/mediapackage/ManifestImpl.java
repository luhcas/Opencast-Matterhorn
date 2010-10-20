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

import org.opencastproject.mediapackage.identifier.HandleBuilderFactory;
import org.opencastproject.mediapackage.identifier.HandleException;
import org.opencastproject.mediapackage.identifier.Id;
import org.opencastproject.mediapackage.identifier.IdBuilder;
import org.opencastproject.mediapackage.identifier.IdBuilderFactory;
import org.opencastproject.mediapackage.identifier.UUIDIdBuilderImpl;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.util.DateTimeSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * Implements a media package manifest that keeps information about a media package's contents.
 */
final class ManifestImpl {

  /** the logging facility provided by log4j */
  private final static Logger logger = LoggerFactory.getLogger(ManifestImpl.class.getName());

  /** id builder, for internal use only */
  private static final IdBuilder idBuilder = new UUIDIdBuilderImpl();

  /** The media package's identifier */
  private Id identifier = null;

  /** The start date and time */
  private long startTime = 0L;

  /** The media package duration */
  private long duration = -1L;

  /** The media package's other (uncategorized) files */
  private List<MediaPackageElement> elements = new ArrayList<MediaPackageElement>();

  /** Number of tracks */
  private int tracks = 0;

  /** Number of metadata catalogs */
  private int catalogs = 0;

  /** Number of attachments */
  private int attachments = 0;

  /** Numer of unclassified elements */
  private int others = 0;

  /**
   * Creates a new manifest implementation used to be filled up using the manifest SAX parser.
   */
  ManifestImpl() {
    this(idBuilder.createNew());
  }

  /**
   * Creates a new media package manifest within the given root folder.
   * 
   * @param identifier
   *          the media package identifier
   * @throws IOException
   *           if the specified file does not exist or cannot be created
   */
  ManifestImpl(Id identifier) {
    this.identifier = identifier;
  }

  /**
   * @see org.opencastproject.mediapackage.ManifestImpl#getIdentifier()
   */
  Id getIdentifier() {
    return identifier;
  }

  /**
   * Sets the media package identifier.
   * 
   * @param identifier
   *          the identifier
   */
  void setIdentifier(Id identifier) {
    this.identifier = identifier;
  }

  /**
   * @see org.opencastproject.mediapackage.ManifestImpl#getDuration()
   */
  long getDuration() {
    return duration;
  }

  /**
   * @see org.opencastproject.mediapackage.ManifestImpl#getStartDate()
   */
  long getStartDate() {
    return startTime;
  }
  
  void setStartDate(long startTime) {
    this.startTime = startTime;
  }

  /**
   * @see org.opencastproject.mediapackage.ManifestImpl#getEntries()
   */
  MediaPackageElement[] getEntries() {
    ArrayList<MediaPackageElement> entries = new ArrayList<MediaPackageElement>();
    for (MediaPackageElement e : elements) {
      entries.add(e);
    }
    return entries.toArray(new MediaPackageElement[entries.size()]);
  }

  /**
   * Registers a new media package element with this manifest.
   * 
   * @param element
   *          the new element
   * @throws MediaPackageException
   *           if adding the element fails
   */
  void add(MediaPackageElement element) {
    if (element == null)
      throw new IllegalArgumentException("Media package element must not be null");
    String id = null;
    if (elements.add(element)) {
      if (element instanceof Track) {
        tracks++;
        id = "track-" + tracks;
        long duration = ((Track) element).getDuration();
        // Todo Do not demand equal durations for now... This is an issue that has to be discussed further
        // if (this.duration > 0 && this.duration != duration)
        // throw new MediaPackageException("Track " + element + " cannot be added due to varying duration (" + duration
        // +
        // " instead of " + this.duration +")");
        // else
        if (this.duration < 0)
          this.duration = duration;
      } else if (element instanceof Attachment) {
        attachments++;
        id = "attachment-" + attachments;
      } else if (element instanceof Catalog) {
        catalogs++;
        id = "catalog-" + catalogs;
      } else {
        others++;
        id = "unknown-" + others;
      }
    }

    // Check if element has an id
    if (element.getIdentifier() == null) {
      if (element instanceof AbstractMediaPackageElement) {
        ((AbstractMediaPackageElement) element).setIdentifier(id);
      }
      else
        throw new UnsupportedElementException(element, "Found unkown element without id");
    }
  }

  /**
   * Removes the media package element from the manifest.
   * 
   * @param element
   *          the element to remove
   */
  void remove(MediaPackageElement element) {
    if (element == null)
      throw new IllegalArgumentException("Media package element must not be null");
    if (elements.remove(element)) {
      if (element instanceof Track) {
        tracks--;
        if (tracks == 0)
          duration = 0L;
      } else if (element instanceof Attachment)
        attachments--;
      else if (element instanceof Catalog)
        catalogs--;
      else
        others--;
    }
  }

  /**
   * Returns <code>true</code> if the manifest contains the media package element.
   * 
   * @param element
   *          the media package element
   * @return <code>true</code> if the element is listed in the manifest
   */
  boolean contains(MediaPackageElement element) {
    if (element == null)
      throw new IllegalArgumentException("Media package element must not be null");
    return (elements.contains(element));
  }

  /**
   * Extracts the list of tracks from the media package.
   * 
   * @return the tracks
   */
  private Collection<Track> loadTracks() {
    List<Track> tracks = new ArrayList<Track>();
    synchronized (elements) {
      for (MediaPackageElement e : elements) {
        if (e instanceof Track) {
          tracks.add((Track) e);
        }
      }
    }
    return tracks;
  }

  /**
   * Returns the track with id <code>trackId</code> or <code>null</code> if that track doesn't exist.
   * 
   * @param trackId
   *          the track identifier
   * @return the track
   */
  Track getTrack(String trackId) {
    synchronized (elements) {
      for (MediaPackageElement e : elements) {
        if (e.getIdentifier().equals(trackId) && e instanceof Track)
          return (Track) e;
      }
    }
    return null;
  }

  /**
   * Returns the media package's tracks.
   * 
   * @return the tracks
   */
  Track[] getTracks() {
    Collection<Track> tracks = loadTracks();
    return tracks.toArray(new Track[tracks.size()]);
  }

  /**
   * Returns the attachments that are tagged with the given tag or an empty string array.
   * 
   * @param tag
   *          the tag
   * @return the attachments
   */
  public Track[] getTracksByTag(String tag) {
    List<Track> result = new ArrayList<Track>();
    synchronized (elements) {
      for (MediaPackageElement e : elements) {
        if (e instanceof Track && e.containsTag(tag))
          result.add((Track) e);
      }
    }
    return result.toArray(new Track[result.size()]);
  }

  /**
   * Returns the media package's tracks matching the specified flavor.
   * 
   * @param flavor
   *          the track flavor
   * @return the tracks
   */
  Track[] getTracks(MediaPackageElementFlavor flavor) {
    if (flavor == null)
      throw new IllegalArgumentException("Unable to filter by null criterion");

    // Go through tracks and remove those that don't match
    Collection<Track> tracks = loadTracks();
    List<Track> candidates = new ArrayList<Track>(tracks);
    for (Track a : tracks) {
      if (!flavor.equals(a.getFlavor())) {
        candidates.remove(a);
      }
    }
    return candidates.toArray(new Track[candidates.size()]);
  }

  /**
   * Returns the media package's tracks that contain the specified reference.
   * 
   * @param reference
   *          the reference
   * @return the tracks
   */
  Track[] getTracks(MediaPackageReference reference, boolean includeDerived) {
    if (reference == null)
      throw new IllegalArgumentException("Unable to filter by null reference");

    // Go through tracks and remove those that don't match
    Collection<Track> tracks = loadTracks();
    List<Track> candidates = new ArrayList<Track>(tracks);
    for (Track t : tracks) {
      MediaPackageReference r = t.getReference();
      if (!reference.matches(r)) {
        boolean indirectHit = false;
        
        // Create a reference that will match regardless of properties
        MediaPackageReference elementRef = new MediaPackageReferenceImpl(reference.getType(), reference.getIdentifier());

        // Try to find a derived match if possible
        while (includeDerived && (r = getElement(r).getReference()) != null) {
          if (r.matches(elementRef)) {
            indirectHit = true;
            break;
          }
        }
        
        if (!indirectHit)
          candidates.remove(t);
      }
    }    
    
    return candidates.toArray(new Track[candidates.size()]);
  }

  /**
   * Returns the media package's tracks that match the specified flavor and reference.
   * 
   * @param flavor
   *          the track flavor
   * @param reference
   *          the reference
   * @return the tracks
   */
  Track[] getTracks(MediaPackageElementFlavor flavor, MediaPackageReference reference) {
    if (flavor == null)
      throw new IllegalArgumentException("Unable to filter by null criterion");
    if (reference == null)
      throw new IllegalArgumentException("Unable to filter by null reference");

    // Go through tracks and remove those that don't match
    Collection<Track> tracks = loadTracks();
    List<Track> candidates = new ArrayList<Track>(tracks);
    for (Track a : tracks) {
      if (!flavor.equals(a.getFlavor()) || !reference.matches(a.getReference())) {
        candidates.remove(a);
      }
    }
    return candidates.toArray(new Track[candidates.size()]);
  }

  /**
   * Returns <code>true</code> if the media package contains tracks of any kind.
   * 
   * @return <code>true</code> if the media package contains tracks
   */
  boolean hasTracks() {
    return tracks > 0;
  }

  /**
   * Extracts the list of attachments from the media package.
   * 
   * @return the attachments
   */
  private Collection<Attachment> loadAttachments() {
    List<Attachment> attachments = new ArrayList<Attachment>();
    synchronized (elements) {
      for (MediaPackageElement e : elements) {
        if (e instanceof Attachment) {
          attachments.add((Attachment) e);
        }
      }
    }
    return attachments;
  }

  /**
   * Returns the attachment with id <code>attachmentId</code> or <code>null</code> if that attachment doesn't exist.
   * 
   * @param attachmentId
   *          the attachment identifier
   * @return the attachment
   */
  Attachment getAttachment(String attachmentId) {
    synchronized (elements) {
      for (MediaPackageElement e : elements) {
        if (e.getIdentifier().equals(attachmentId) && e instanceof Attachment)
          return (Attachment) e;
      }
    }
    return null;
  }

  /**
   * Returns the attachments that are tagged with the given tag or an empty string array.
   * 
   * @param tag
   *          the tag
   * @return the attachments
   */
  public Attachment[] getAttachmentsByTag(String tag) {
    List<Attachment> result = new ArrayList<Attachment>();
    synchronized (elements) {
      for (MediaPackageElement e : elements) {
        if (e instanceof Attachment && e.containsTag(tag))
          result.add((Attachment) e);
      }
    }
    return result.toArray(new Attachment[result.size()]);
  }
  
  /**
   * Returns the media package's attachments.
   * 
   * @return the attachments
   */
  Attachment[] getAttachments() {
    Collection<Attachment> attachments = loadAttachments();
    return attachments.toArray(new Attachment[attachments.size()]);
  }

  /**
   * Returns the media package's attachments matching the specified flavor.
   * 
   * @param flavor
   *          the attachment flavor
   * @return the attachments
   */
  Attachment[] getAttachments(MediaPackageElementFlavor flavor) {
    if (flavor == null)
      throw new IllegalArgumentException("Unable to filter by null criterion");

    // Go through attachments and remove those that don't match
    Collection<Attachment> attachments = loadAttachments();
    List<Attachment> candidates = new ArrayList<Attachment>(attachments);
    for (Attachment a : attachments) {
      if (!flavor.equals(a.getFlavor())) {
        candidates.remove(a);
      }
    }
    return candidates.toArray(new Attachment[candidates.size()]);
  }

  /**
   * Returns the media package's attachments that contain the specified reference.
   * 
   * @param reference
   *          the reference
   * @return the attachments
   */
  Attachment[] getAttachments(MediaPackageReference reference, boolean includeDerived) {
    if (reference == null)
      throw new IllegalArgumentException("Unable to filter by null reference");

    // Go through attachments and remove those that don't match
    Collection<Attachment> attachments = loadAttachments();
    List<Attachment> candidates = new ArrayList<Attachment>(attachments);
    for (Attachment a : attachments) {
      MediaPackageReference r = a.getReference();
      if (!reference.matches(r)) {
        boolean indirectHit = false;
        
        // Create a reference that will match regardless of properties
        MediaPackageReference elementRef = new MediaPackageReferenceImpl(reference.getType(), reference.getIdentifier());

        // Try to find a derived match if possible
        while (includeDerived && getElement(r) != null && (r = getElement(r).getReference()) != null) {
          if (r.matches(elementRef)) {
            indirectHit = true;
            break;
          }
        }
        
        if (!indirectHit)
          candidates.remove(a);
      }
    }
    return candidates.toArray(new Attachment[candidates.size()]);
  }

  /**
   * Returns the media package element that matches the given reference.
   * 
   * @param reference the reference
   * @return the element
   */
  MediaPackageElement getElement(MediaPackageReference reference) {
    if (reference == null)
      return null;
    for (MediaPackageElement e : elements) {
      if (e.getIdentifier().equals(reference.getIdentifier()))
        return e;
    }
    return null;
  }

  /**
   * Returns the media package elements that contain the specified reference.
   * 
   * @param reference
   *          the reference
   * @return the tracks
   */
  MediaPackageElement[] getElements(MediaPackageReference reference, boolean includeDerived) {
    if (reference == null)
      throw new IllegalArgumentException("Unable to filter by null reference");

    // Go through tracks and remove those that don't match
    Collection<MediaPackageElement> elements = new ArrayList<MediaPackageElement>();
    elements.addAll(this.elements);
    List<MediaPackageElement> candidates = new ArrayList<MediaPackageElement>(tracks);
    for (MediaPackageElement e : elements) {
      MediaPackageReference r = e.getReference();
      if (!reference.matches(r)) {
        boolean indirectHit = false;
        
        // Create a reference that will match regardless of properties
        MediaPackageReference elementRef = new MediaPackageReferenceImpl(reference.getType(), reference.getIdentifier());

        // Try to find a derived match if possible
        while (includeDerived && (r = getElement(r).getReference()) != null) {
          if (r.matches(elementRef)) {
            indirectHit = true;
            break;
          }
        }
        
        if (!indirectHit)
          candidates.remove(e);
      }
    }    
    
    return candidates.toArray(new MediaPackageElement[candidates.size()]);
  }

  /**
   * Returns the media package's attachments that match the specified flavor and reference.
   * 
   * @param flavor
   *          the attachment flavor
   * @param reference
   *          the reference
   * @return the attachments
   */
  Attachment[] getAttachments(MediaPackageElementFlavor flavor, MediaPackageReference reference) {
    if (flavor == null)
      throw new IllegalArgumentException("Unable to filter by null criterion");
    if (reference == null)
      throw new IllegalArgumentException("Unable to filter by null reference");

    // Go through attachments and remove those that don't match
    Collection<Attachment> attachments = loadAttachments();
    List<Attachment> candidates = new ArrayList<Attachment>(attachments);
    for (Attachment a : attachments) {
      if (!flavor.equals(a.getFlavor()) || !reference.matches(a.getReference())) {
        candidates.remove(a);
      }
    }
    return candidates.toArray(new Attachment[candidates.size()]);
  }

  /**
   * Returns <code>true</code> if the media package contains attachments of any kind.
   * 
   * @return <code>true</code> if the media package contains attachments
   */
  boolean hasAttachments() {
    return attachments > 0;
  }

  /**
   * Extracts the list of catalogs from the media package.
   * 
   * @return the catalogs
   */
  private Collection<Catalog> loadCatalogs() {
    List<Catalog> catalogs = new ArrayList<Catalog>();
    synchronized (elements) {
      for (MediaPackageElement e : elements) {
        if (e instanceof Catalog) {
          catalogs.add((Catalog) e);
        }
      }
    }
    return catalogs;
  }

  /**
   * Returns the catalog with id <code>catalogId</code> or <code>null</code> if that catalog doesn't exist.
   * 
   * @param catalogId
   *          the catalog identifier
   * @return the catalog
   */
  Catalog getCatalog(String catalogId) {
    synchronized (elements) {
      for (MediaPackageElement e : elements) {
        if (e.getIdentifier().equals(catalogId) && e instanceof Catalog)
          return (Catalog) e;
      }
    }
    return null;
  }

  /**
   * Returns the media package's catalogs.
   * 
   * @return the catalogs
   */
  Catalog[] getCatalogs() {
    Collection<Catalog> catalogs = loadCatalogs();
    return catalogs.toArray(new Catalog[catalogs.size()]);
  }

  /**
   * Returns the attachments that are tagged with the given tag or an empty string array.
   * 
   * @param tag
   *          the tag
   * @return the attachments
   */
  public Catalog[] getCatalogsByTag(String tag) {
    List<Catalog> result = new ArrayList<Catalog>();
    synchronized (elements) {
      for (MediaPackageElement e : elements) {
        if (e instanceof Catalog && e.containsTag(tag))
          result.add((Catalog) e);
      }
    }
    return result.toArray(new Catalog[result.size()]);
  }

  /**
   * Returns the media package's catalogs matching the specified flavor.
   * 
   * @param flavor
   *          the catalog flavor
   * @return the catalogs
   */
  Catalog[] getCatalogs(MediaPackageElementFlavor flavor) {
    if (flavor == null)
      throw new IllegalArgumentException("Unable to filter by null criterion");

    // Go through catalogs and remove those that don't match
    Collection<Catalog> catalogs = loadCatalogs();
    List<Catalog> candidates = new ArrayList<Catalog>(catalogs);
    for (Catalog c : catalogs) {
      if (c.getFlavor() == null || !c.getFlavor().matches(flavor)) {
        candidates.remove(c);
      }
    }
    return candidates.toArray(new Catalog[candidates.size()]);
  }

  /**
   * Returns the media package's catalogs that contain the specified reference.
   * 
   * @param reference
   *          the reference
   * @return the catalogs
   */
  Catalog[] getCatalogs(MediaPackageReference reference, boolean includeDerived) {
    if (reference == null)
      throw new IllegalArgumentException("Unable to filter by null reference");

    // Go through catalogs and remove those that don't match
    Collection<Catalog> catalogs = loadCatalogs();
    List<Catalog> candidates = new ArrayList<Catalog>(catalogs);
    for (Catalog c : catalogs) {
      MediaPackageReference r = c.getReference();
      if (!reference.matches(r)) {
        boolean indirectHit = false;
        
        // Create a reference that will match regardless of properties
        MediaPackageReference elementRef = new MediaPackageReferenceImpl(reference.getType(), reference.getIdentifier());

        // Try to find a derived match if possible
        while (includeDerived && (r = getElement(r).getReference()) != null) {
          if (r.matches(elementRef)) {
            indirectHit = true;
            break;
          }
        }
        
        if (!indirectHit)
          candidates.remove(c);
      }
    }
    
    return candidates.toArray(new Catalog[candidates.size()]);
  }

  /**
   * Returns the media package's catalogs that match the specified flavor and reference.
   * 
   * @param flavor
   *          the attachment flavor
   * @param reference
   *          the reference
   * @return the catalogs
   */
  Catalog[] getCatalogs(MediaPackageElementFlavor flavor, MediaPackageReference reference) {
    if (flavor == null)
      throw new IllegalArgumentException("Unable to filter by null criterion");
    if (reference == null)
      throw new IllegalArgumentException("Unable to filter by null reference");

    // Go through catalogs and remove those that don't match
    Collection<Catalog> catalogs = loadCatalogs();
    List<Catalog> candidates = new ArrayList<Catalog>(catalogs);
    for (Catalog c : catalogs) {
      if (!flavor.equals(c.getFlavor()) || (c.getReference() != null && !c.getReference().matches(reference))) {
        candidates.remove(c);
      }
    }
    return candidates.toArray(new Catalog[candidates.size()]);
  }

  /**
   * Returns <code>true</code> if the media package contains catalogs of any kind.
   * 
   * @return <code>true</code> if the media package contains catalogs
   */
  boolean hasCatalogs() {
    return catalogs > 0;
  }

  /**
   * Returns <code>true</code> if the media package contains catalogs of any kind with a matching reference.
   * 
   * @return <code>true</code> if the media package contains matching catalogs
   */
  boolean hasCatalogs(MediaPackageElementFlavor flavor, MediaPackageReference reference) {
    if (flavor == null)
      throw new IllegalArgumentException("Unable to filter by null criterion");
    if (reference == null)
      throw new IllegalArgumentException("Unable to filter by null reference");
    else
      return getCatalogs(flavor, reference).length > 0;
  }

  /**
   * Returns the media package elements that are not attachments, metadata or tracks.
   * 
   * @return the elements
   */
  MediaPackageElement[] getUnclassifiedElements() {
    return getUnclassifiedElements(null);
  }

  /**
   * Returns the media package elements that are not attachments, metadata or tracks and match the specified mime type.
   * 
   * @param type
   *          the mime type
   * @return the elements
   */
  MediaPackageElement[] getUnclassifiedElements(MediaPackageElementFlavor type) {
    List<MediaPackageElement> unclassifieds = new ArrayList<MediaPackageElement>();
    synchronized (elements) {
      for (MediaPackageElement e : elements) {
        if (!(e instanceof Attachment) && !(e instanceof Catalog) && !(e instanceof Track)) {
          if (type == null || type.equals(e.getFlavor())) {
            unclassifieds.add(e);
          }
        }
      }
    }
    return unclassifieds.toArray(new MediaPackageElement[unclassifieds.size()]);
  }

  /**
   * Returns <code>true</code> if the media package contains unclassified elements.
   * 
   * @return <code>true</code> if the media package contains unclassified elements
   */
  boolean hasUnclassifiedElements() {
    return hasUnclassifiedElements(null);
  }

  /**
   * Returns <code>true</code> if the media package contains unclassified elements.
   * 
   * @return <code>true</code> if the media package contains unclassified elements
   */
  boolean hasUnclassifiedElements(MediaPackageElementFlavor type) {
    if (type == null)
      return others > 0;
    synchronized (elements) {
      for (MediaPackageElement e : elements) {
        if (!(e instanceof Attachment) && !(e instanceof Catalog) && !(e instanceof Track)) {
          if (type.equals(e.getFlavor())) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Returns an xml presentation of the manifest.
   * 
   * @throws TransformerException
   *           if serializing the document fails
   * @throws ParserConfigurationException
   *           if the creating a document builder fails
   * @throws IOException
   */
  Document toXml() throws TransformerException, ParserConfigurationException {
    return toXml(null);
  }

  /**
   * Returns an xml presentation of the manifest.
   * 
   * @param serializer
   *          the media package serializer
   * @throws TransformerException
   *           if serializing the document fails
   * @throws ParserConfigurationException
   *           if the creating a document builder fails
   * @throws IOException
   */
  Document toXml(MediaPackageSerializer serializer) throws TransformerException, ParserConfigurationException {
    DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    docBuilderFactory.setNamespaceAware(true);
    DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
    Document doc = docBuilder.newDocument();

    // Root element "mediapackage"
    Element mediaPackage = doc.createElement("mediapackage");
    doc.appendChild(mediaPackage);

    // Handle
    if (identifier != null)
      mediaPackage.setAttribute("id", identifier.toString());

    // Start time
    if (startTime > 0)
      mediaPackage.setAttribute("start", DateTimeSupport.toUTC(startTime));

    // Duration
    if (duration > 0)
      mediaPackage.setAttribute("duration", Long.toString(duration));

    // Separate the media package members
    List<Track> tracks = new ArrayList<Track>();
    List<Attachment> attachments = new ArrayList<Attachment>();
    List<Catalog> metadata = new ArrayList<Catalog>();
    List<MediaPackageElement> others = new ArrayList<MediaPackageElement>();

    // Sort media package elements
    for (MediaPackageElement e : elements) {
      if (e instanceof Track)
        tracks.add((Track) e);
      else if (e instanceof Attachment)
        attachments.add((Attachment) e);
      else if (e instanceof Catalog)
        metadata.add((Catalog) e);
      else
        others.add(e);
    }

    // Tracks
    if (tracks.size() > 0) {
      Element tracksNode = doc.createElement("media");
      Collections.sort(tracks);
      for (Track t : tracks) {
        tracksNode.appendChild(t.toManifest(doc, serializer));
      }
      mediaPackage.appendChild(tracksNode);
    }

    // Metadata
    if (metadata.size() > 0) {
      Element metadataNode = doc.createElement("metadata");
      Collections.sort(metadata);
      for (Catalog m : metadata) {
        metadataNode.appendChild(m.toManifest(doc, serializer));
      }
      mediaPackage.appendChild(metadataNode);
    }

    // Attachments
    if (attachments.size() > 0) {
      Element attachmentsNode = doc.createElement("attachments");
      Collections.sort(attachments);
      for (Attachment a : attachments) {
        attachmentsNode.appendChild(a.toManifest(doc, serializer));
      }
      mediaPackage.appendChild(attachmentsNode);
    }

    // Unclassified
    if (others.size() > 0) {
      Element othersNode = doc.createElement("unclassified");
      Collections.sort(others);
      for (MediaPackageElement e : others) {
        othersNode.appendChild(e.toManifest(doc, serializer));
      }
      mediaPackage.appendChild(othersNode);
    }

    return mediaPackage.getOwnerDocument();
  }

  /**
   * Creates a new manifest file at the specified location for a media package with the given identifier.
   * 
   * @param packageRoot
   *          the media package root directory
   * @param identifier
   *          the media package identifier
   * @return the new manifest
   */
  static ManifestImpl newInstance(Id identifier) {
    ManifestImpl m = new ManifestImpl(identifier);
    return m;
  }

  /**
   * Reads a manifest from the specified file and returns it encapsulated in a manifest object.
   * 
   * @param url
   *          the manifest location
   * @param serializer
   *          the media package serializer
   * @param ignoreMissingElements
   *          <code>true</code> to ignore and remove missing elements
   * @return the manifest object
   * @throws MediaPackageException
   *           if the media package is in an inconsistent state
   * @throws IOException
   *           if reading the manifest file fails
   * @throws ParserConfigurationException
   *           if the manifest parser cannot be created
   * @throws SAXException
   *           if reading the manifest fails
   * @throws XPathExpressionException
   *           if querying the manifest failed
   * @throws HandleException
   *           if reading the handle from the manifest fails
   * @throws ConfigurationException
   *           if a configuration error occurs
   * @throws ParseException
   *           if the manifest contains a malformed start date
   */
  static ManifestImpl fromStream(InputStream is, MediaPackageSerializer serializer, boolean ignoreMissingElements)
          throws MediaPackageException, IOException, ParserConfigurationException, SAXException,
          XPathExpressionException, ConfigurationException, HandleException, ParseException {

    DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
    Document doc = docBuilder.parse(is);

    // Prepare xpath and element builder
    ManifestImpl manifest = new ManifestImpl();
    XPath xPath = XPathFactory.newInstance().newXPath();
    MediaPackageElementBuilder elementBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
    if (elementBuilder == null)
      throw new IllegalStateException("Unable to create a media package element builder");

    // Handle or ID (we currently have a mix, so we need to support both)
    String id = xPath.evaluate("/mediapackage/@id", doc);

    if (id != null && !"".equals(id)) {
      try {
        manifest.identifier = IdBuilderFactory.newInstance().newIdBuilder().fromString(id);
      } catch (Exception e) {
        // The default ID builder didn't work? Try the handle builder.
        manifest.identifier = HandleBuilderFactory.newInstance().newHandleBuilder().fromString(id);
      }
    } else {
      manifest.identifier = IdBuilderFactory.newInstance().newIdBuilder().createNew();
      logger.info("Created handle {} for manifest", manifest.identifier);
    }

    // Start time
    String strStart = xPath.evaluate("/mediapackage/@start", doc);
    if (strStart != null && !"".equals(strStart)) {
      manifest.startTime = DateTimeSupport.fromUTC(strStart);
    }

    // Duration
    String strDuration = xPath.evaluate("/mediapackage/@duration", doc);
    if (strDuration != null && !"".equals(strDuration)) {
      manifest.duration = Long.parseLong(strDuration);
    }

    // Read tracks
    NodeList trackNodes = (NodeList) xPath.evaluate("/mediapackage/media/track", doc, XPathConstants.NODESET);
    for (int i = 0; i < trackNodes.getLength(); i++) {
      try {
        MediaPackageElement track = elementBuilder.elementFromManifest(trackNodes.item(i), serializer);
        if (track != null) {
          URI elementUrl = track.getURI();
          if (elementUrl != null) // TODO: Check existence
            manifest.add(track);
          else if (!ignoreMissingElements)
            throw new MediaPackageException("Track file " + elementUrl + " is missing");
        }
      } catch (IllegalStateException e) {
        logger.warn("Unable to create tracks from manifest: " + e.getMessage());
        throw e;
      } catch (MediaPackageException e) {
        if (!ignoreMissingElements) {
          logger.warn("Error while creating track from manifest:" + e.getMessage());
          throw e;
        }
      } catch (Throwable t) {
        if (!ignoreMissingElements) {
          logger.warn("Error reading track: " + t.getMessage());
          throw new IllegalStateException(t);
        }
      }
    }

    // Read catalogs
    NodeList catalogNodes = (NodeList) xPath.evaluate("/mediapackage/metadata/catalog", doc, XPathConstants.NODESET);
    logger.debug("{} catalog nodes found", catalogNodes.getLength());
    for (int i = 0; i < catalogNodes.getLength(); i++) {
      try {
        MediaPackageElement catalog = elementBuilder.elementFromManifest(catalogNodes.item(i), serializer);
        logger.debug("catalog node {} (#{} of {}) parsed to {}", new Object[] {catalogNodes.item(i), i+1, catalogNodes.getLength(), catalog});
        if (catalog != null) {
          logger.debug("catalog flavor={}", catalog.getFlavor());
          URI elementUrl = catalog.getURI();
          if (elementUrl != null) // TODO: Check existence
            manifest.add(catalog);
          else if (!ignoreMissingElements)
            throw new MediaPackageException("Catalog file " + elementUrl + " is missing");
        }
      } catch (IllegalStateException e) {
        if (!ignoreMissingElements) {
          logger.warn("Unable to load catalog from manifest: " + e.getMessage());
          throw e;
        }
      } catch (MediaPackageException e) {
        if (!ignoreMissingElements) {
          logger.warn("Error while loading catalog from manifest:" + e.getMessage());
          throw e;
        }
      } catch (Throwable t) {
        logger.warn("Error reading catalog: " + t.getMessage());
        t.printStackTrace();
        throw new IllegalStateException(t);
      }
    }

    // Read attachments
    NodeList attachmentNodes = (NodeList) xPath.evaluate("/mediapackage/attachments/attachment", doc,
            XPathConstants.NODESET);
    for (int i = 0; i < attachmentNodes.getLength(); i++) {
      try {
        MediaPackageElement attachment = elementBuilder.elementFromManifest(attachmentNodes.item(i), serializer);
        if (attachment != null) {
          URI elementUrl = attachment.getURI();
          if (elementUrl != null) // TODO: Check existence
            manifest.add(attachment);
          else if (!ignoreMissingElements)
            throw new MediaPackageException("Attachment file " + elementUrl + " is missing");
        }
      } catch (IllegalStateException e) {
        if (!ignoreMissingElements) {
          logger.warn("Unable to load attachment from manifest: " + e.getMessage());
          throw e;
        }
      } catch (MediaPackageException e) {
        if (!ignoreMissingElements) {
          logger.warn("Error while loading attachment from manifest:" + e.getMessage());
          throw e;
        }
      } catch (Throwable t) {
        logger.warn("Error reading attachment: " + t.getMessage());
        throw new IllegalStateException(t);
      }
    }

    return manifest;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "manifest";
  }

}
