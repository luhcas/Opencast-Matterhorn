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
import org.opencastproject.media.mediapackage.handle.HandleBuilderFactory;
import org.opencastproject.media.mediapackage.handle.HandleException;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.UnknownFileTypeException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * Implements a media package manifest that keeps information about a media package's contents.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: ManifestImpl.java 2908 2009-07-17 16:51:07Z ced $
 */
public final class ManifestImpl {

  /** The media package's identifier */
  private Handle identifier = null;

  /** The manifest file */
  private File file = null;

  /** The start date and time */
  private long startTime = 0L;

  /** The media package duration */
  private long duration = 0L;

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

  /** the logging facility provided by log4j */
  private final static Logger log_ = LoggerFactory.getLogger(ManifestImpl.class.getName());

  /**
   * Creates a new manifest implementation used to be filled up using the manifest SAX parser.
   * 
   * @param packageRoot
   *          the media package root
   * @throws IOException
   *           if the specified file does not exist or cannot be created
   * @throws UnknownFileTypeException
   *           if the manifest is of an unknown file type
   * @throws NoSuchAlgorithmException
   *           if the md5 checksum cannot be computed
   */
  private ManifestImpl(File packageRoot) throws IOException, UnknownFileTypeException, NoSuchAlgorithmException {
    this(packageRoot, null);
  }

  /**
   * Creates a new media package manifest for the given media package.
   * 
   * @param mediaPackage
   *          the associated media package
   * @param identifier
   *          the media package identifier
   * @throws IOException
   *           if the specified file does not exist or cannot be created
   * @throws UnknownFileTypeException
   *           if the manifest is of an unknown file type
   * @throws NoSuchAlgorithmException
   *           if the md5 checksum cannot be computed
   */
  ManifestImpl(MediaPackage mediaPackage, Handle identifier) throws IOException, UnknownFileTypeException,
          NoSuchAlgorithmException {
    this(mediaPackage.getRoot(), identifier);
  }

  /**
   * Creates a new media package manifest within the given root folder.
   * 
   * @param packageRoot
   *          the media package root folder
   * @param identifier
   *          the media package identifier
   * @throws IOException
   *           if the specified file does not exist or cannot be created
   * @throws UnknownFileTypeException
   *           if the manifest is of an unknown file type
   * @throws NoSuchAlgorithmException
   *           if the md5 checksum cannot be computed
   */
  ManifestImpl(File packageRoot, Handle identifier) throws IOException, UnknownFileTypeException,
          NoSuchAlgorithmException {
    this.file = new File(packageRoot, MediaPackageElements.MANIFEST_FILENAME);
    if (!this.file.exists())
      file.createNewFile();
    this.identifier = identifier;
  }

  /**
   * @see org.opencastproject.media.mediapackage.ManifestImpl#getIdentifier()
   */
  public Handle getIdentifier() {
    return identifier;
  }

  /**
   * @see org.opencastproject.media.mediapackage.ManifestImpl#getFile()
   */
  public File getFile() {
    return file;
  }

  /**
   * Tells the manifest that the media package has been moved to a new location.
   * 
   * @param oldRoot
   *          the former media package root directory
   * @param newRoot
   *          the new media package root directory
   */
  void mediaPackageMoved(File oldRoot, File newRoot) {
    file = new File(newRoot, file.getName());
  }

  /**
   * @see org.opencastproject.media.mediapackage.ManifestImpl#getDuration()
   */
  public long getDuration() {
    return duration;
  }

  /**
   * @see org.opencastproject.media.mediapackage.ManifestImpl#getStartDate()
   */
  public long getStartDate() {
    return startTime;
  }

  /**
   * @see org.opencastproject.media.mediapackage.ManifestImpl#getEntries()
   */
  public MediaPackageElement[] getEntries() {
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
  void add(MediaPackageElement element) throws MediaPackageException {
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
        if (this.duration == 0)
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

      // Adjust media package reference
      MediaPackageReference reference = element.getReference();
      if (reference != null) {
        if (reference.getType().equals(MediaPackageReference.TYPE_MEDIAPACKAGE)
                && reference.getIdentifier().equals(MediaPackageReference.SELF)) {
          element
                  .referTo(new MediaPackageReferenceImpl(MediaPackageReference.TYPE_MEDIAPACKAGE, identifier.toString()));
        }
      }
    }

    // Check if element has an id
    if (element.getIdentifier() == null) {
      if (element instanceof AbstractMediaPackageElement)
        ((AbstractMediaPackageElement) element).setIdentifier(id);
      else
        throw new IllegalStateException("Found unkown element without id");
    }
  }

  /**
   * Removes the media package element from the manifest.
   * 
   * @param element
   *          the element to remove
   * @throws MediaPackageException
   *           if removing the element fails
   */
  void remove(MediaPackageElement element) throws MediaPackageException {
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

    try {
      save();
    } catch (ParserConfigurationException e) {
      throw new MediaPackageException("Parser configuration exception while updating media package manifest: "
              + e.getMessage());
    } catch (TransformerException e) {
      throw new MediaPackageException("Transformer exception while updating media package manifest: " + e.getMessage());
    } catch (IOException e) {
      throw new MediaPackageException("I/O exception while updating media package manifest: " + e.getMessage());
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
   * Returns the media package's tracks.
   * 
   * @return the tracks
   */
  Track[] getTracks() {
    Collection<Track> tracks = loadTracks();
    return tracks.toArray(new Track[tracks.size()]);
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
  public Track[] getTracks(MediaPackageReference reference) {
    if (reference == null)
      throw new IllegalArgumentException("Unable to filter by null reference");

    // Go through tracks and remove those that don't match
    Collection<Track> tracks = loadTracks();
    List<Track> candidates = new ArrayList<Track>(tracks);
    for (Track a : tracks) {
      if (!reference.matches(a.getReference())) {
        candidates.remove(a);
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
  public Track[] getTracks(MediaPackageElementFlavor flavor, MediaPackageReference reference) {
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
   * Returns <code>true</code> if the media package contains tracks of any kind.
   * 
   * @return <code>true</code> if the media package contains matching tracks
   */
  boolean hasTracks(MediaPackageElementFlavor flavor) {
    if (flavor == null)
      throw new IllegalArgumentException("Unable to filter by null criterion");
    return getTracks(flavor).length > 0;
  }

  /**
   * Returns <code>true</code> if the media package contains tracks with a matching reference.
   * 
   * @return <code>true</code> if the media package contains matching tracks
   */
  boolean hasTracks(MediaPackageReference reference) {
    if (reference == null)
      throw new IllegalArgumentException("Unable to filter by null reference");
    else
      return getTracks(reference).length > 0;
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
  public Attachment[] getAttachments(MediaPackageReference reference) {
    if (reference == null)
      throw new IllegalArgumentException("Unable to filter by null reference");

    // Go through attachments and remove those that don't match
    Collection<Attachment> attachments = loadAttachments();
    List<Attachment> candidates = new ArrayList<Attachment>(attachments);
    for (Attachment a : attachments) {
      if (!reference.matches(a.getReference())) {
        candidates.remove(a);
      }
    }
    return candidates.toArray(new Attachment[candidates.size()]);
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
  public Attachment[] getAttachments(MediaPackageElementFlavor flavor, MediaPackageReference reference) {
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
   * Returns <code>true</code> if the media package contains attachments of any kind.
   * 
   * @return <code>true</code> if the media package contains matching attachments
   */
  boolean hasAttachments(MediaPackageElementFlavor flavor) {
    if (flavor == null)
      throw new IllegalArgumentException("Unable to filter by null criterion");
    return getAttachments(flavor).length > 0;
  }

  /**
   * Returns <code>true</code> if the media package contains attachments with a matching reference.
   * 
   * @return <code>true</code> if the media package contains matching attachments
   */
  boolean hasAttachments(MediaPackageReference reference) {
    if (reference == null)
      throw new IllegalArgumentException("Unable to filter by null reference");
    else
      return getAttachments(reference).length > 0;
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
   * Returns the media package's catalogs.
   * 
   * @return the catalogs
   */
  Catalog[] getCatalogs() {
    Collection<Catalog> catalogs = loadCatalogs();
    return catalogs.toArray(new Catalog[catalogs.size()]);
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
      if (!flavor.equals(c.getFlavor())) {
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
  public Catalog[] getCatalogs(MediaPackageReference reference) {
    if (reference == null)
      throw new IllegalArgumentException("Unable to filter by null reference");

    // Go through catalogs and remove those that don't match
    Collection<Catalog> catalogs = loadCatalogs();
    List<Catalog> candidates = new ArrayList<Catalog>(catalogs);
    for (Catalog c : catalogs) {
      if (!reference.matches(c.getReference())) {
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
  public Catalog[] getCatalogs(MediaPackageElementFlavor flavor, MediaPackageReference reference) {
    if (flavor == null)
      throw new IllegalArgumentException("Unable to filter by null criterion");
    if (reference == null)
      throw new IllegalArgumentException("Unable to filter by null reference");

    // Go through catalogs and remove those that don't match
    Collection<Catalog> catalogs = loadCatalogs();
    List<Catalog> candidates = new ArrayList<Catalog>(catalogs);
    for (Catalog c : catalogs) {
      if (!flavor.equals(c.getFlavor()) || !reference.matches(c.getReference())) {
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
   * Returns <code>true</code> if the media package contains catalogs of any kind.
   * 
   * @return <code>true</code> if the media package contains matching catalogs
   */
  boolean hasCatalogs(MediaPackageElementFlavor flavor) {
    if (flavor == null)
      throw new IllegalArgumentException("Unable to filter by null criterion");
    return getCatalogs(flavor).length > 0;
  }

  /**
   * Returns <code>true</code> if the media package contains catalogs with a matching reference.
   * 
   * @return <code>true</code> if the media package contains matching catalogs
   */
  boolean hasCatalogs(MediaPackageReference reference) {
    if (reference == null)
      throw new IllegalArgumentException("Unable to filter by null reference");
    else
      return getCatalogs(reference).length > 0;
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
  public MediaPackageElement[] getUnclassifiedElements() {
    return getUnclassifiedElements(null);
  }

  /**
   * Returns the media package elements that are not attachments, metadata or tracks and match the specified mime type.
   * 
   * @param type
   *          the mime type
   * @return the elements
   */
  public MediaPackageElement[] getUnclassifiedElements(MediaPackageElementFlavor type) {
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
   * Reconsiders the elements from the manifest and calculates their checksums, then updates the manifest to reflect any
   * updated elements.
   * 
   * @throws MediaPackageException
   *           if the checksum cannot be recalculated
   */
  void wrap() throws MediaPackageException {
    for (MediaPackageElement element : elements) {
      element.wrap();
    }
  }

  /**
   * Saves the manifest to disk.
   * 
   * @throws TransformerException
   *           if serializing the document fails
   * @throws ParserConfigurationException
   *           if the creating a document builder fails
   * @throws IOException
   */
  void save() throws TransformerException, ParserConfigurationException, IOException {
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
        tracksNode.appendChild(t.toManifest(doc));
      }
      mediaPackage.appendChild(tracksNode);
    }

    // Metadata
    if (metadata.size() > 0) {
      Element metadataNode = doc.createElement("metadata");
      Collections.sort(metadata);
      for (Catalog m : metadata) {
        metadataNode.appendChild(m.toManifest(doc));
      }
      mediaPackage.appendChild(metadataNode);
    }

    // Attachments
    if (attachments.size() > 0) {
      Element attachmentsNode = doc.createElement("attachments");
      Collections.sort(attachments);
      for (Attachment a : attachments) {
        attachmentsNode.appendChild(a.toManifest(doc));
      }
      mediaPackage.appendChild(attachmentsNode);
    }

    // Unclassified
    if (others.size() > 0) {
      Element othersNode = doc.createElement("unclassified");
      Collections.sort(others);
      for (MediaPackageElement e : others) {
        othersNode.appendChild(e.toManifest(doc));
      }
      mediaPackage.appendChild(othersNode);
    }

    // Create the file
    if (!file.exists())
      if (!file.createNewFile())
        throw new IOException("Unable to create manifest " + file);

    // Save document to disk
    FileOutputStream fos = new FileOutputStream(getFile());
    StreamResult streamResult = new StreamResult(fos);
    TransformerFactory tf = TransformerFactory.newInstance();
    Transformer serializer = tf.newTransformer();
    serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    serializer.setOutputProperty(OutputKeys.METHOD, "xml");
    serializer.setOutputProperty(OutputKeys.INDENT, "yes");
    serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
    serializer.transform(new DOMSource(doc), streamResult);
    fos.flush();
    fos.close();
  }

  /**
   * Creates a new manifest file at the specified location for a media package with the given identifier.
   * 
   * @param packageRoot
   *          the media package root directory
   * @param identifier
   *          the media package identifier
   * @return the new manifest
   * @throws UnknownFileTypeException
   *           if the manifest file type is unknown (very unlikely)
   * @throws IOException
   *           if creating the manifest file fails
   * @throws TransformerException
   *           if saving the xml file fails
   * @throws ParserConfigurationException
   *           if creating the xml parser fails
   * @throws NoSuchAlgorithmException
   *           if the md5 checksum cannot be computed
   */
  static ManifestImpl newInstance(File packageRoot, Handle identifier) throws IOException, UnknownFileTypeException,
          ParserConfigurationException, TransformerException, NoSuchAlgorithmException {
    ManifestImpl m = new ManifestImpl(packageRoot, identifier);
    m.save();
    return m;
  }

  /**
   * Reads a manifest from the specified file and returns it encapsulated in a manifest object. The integrity of
   * all elements is verified before they are added to the media package.
   * 
   * @param file
   *          the manifest file
   * @return the manifest object
   * @throws MediaPackageException
   *           if the media package is in an inconsistent state
   * @throws IOException
   *           if reading the manifest file fails
   * @throws UnknownFileTypeException
   *           if the manifest file is of an unknown file type
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
  public static ManifestImpl fromFile(File file) throws MediaPackageException, IOException, UnknownFileTypeException,
          ParserConfigurationException, SAXException, TransformerException, XPathExpressionException,
          ConfigurationException, HandleException, ParseException {
    try {
      return fromFile(file, false, false, false);
    } catch (NoSuchAlgorithmException e) {
      // This will not happen, since verify is false
      throw new IllegalStateException("Unpredicted application state reached");
    }
  }

  /**
   * Reads a manifest from the specified file and returns it encapsulated in a manifest object.
   * 
   * @param file
   *          the manifest file
   * @param ignoreMissingElements
   *          <code>true</code> to ignore and remove missing elements
   * @param wrap
   *          <code>true</code> to ignore and recreate wrong checksums
   * @param verify
   *          <code>true</code> to verify the media package element's integrity
   * @return the manifest object
   * @throws MediaPackageException
   *           if the media package is in an inconsistent state
   * @throws IOException
   *           if reading the manifest file fails
   * @throws UnknownFileTypeException
   *           if the manifest file is of an unknown file type
   * @throws ParserConfigurationException
   *           if the manifest parser cannot be created
   * @throws SAXException
   *           if reading the manifest fails
   * @throws NoSuchAlgorithmException
   *           if the md5 checksum cannot be computed
   * @throws XPathExpressionException
   *           if querying the manifest failed
   * @throws HandleException
   *           if reading the handle from the manifest fails
   * @throws ConfigurationException
   *           if a configuration error occurs
   * @throws ParseException
   *           if the manifest contains a malformed start date
   */
  public static ManifestImpl fromFile(File file, boolean ignoreMissingElements, boolean wrap, boolean verify)
          throws MediaPackageException, IOException, UnknownFileTypeException, NoSuchAlgorithmException,
          ParserConfigurationException, SAXException, TransformerException, XPathExpressionException,
          ConfigurationException, HandleException, ParseException {
    DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
    Document doc = docBuilder.parse(new FileInputStream(file));

    // True if the manifest has been updated while beeing parsed. If the
    // checksums are being recreated, we do a save just to make sure
    boolean updated = wrap;

    // Prepare xpath and element builder
    ManifestImpl manifest = new ManifestImpl(file.getParentFile());
    XPath xPath = XPathFactory.newInstance().newXPath();
    MediaPackageElementBuilder elementBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
    if (elementBuilder == null)
      throw new IllegalStateException("Unable to create a media package element builder");

    // Handle
    String id = xPath.evaluate("/mediapackage/@id", doc);
    if (id != null && !"".equals(id)) {
      manifest.identifier = HandleBuilderFactory.newInstance().newHandleBuilder().fromValue(id);
    } else {
      manifest.identifier = HandleBuilderFactory.newInstance().newHandleBuilder().createNew();
      updated = true;
      log_.info("Created handle " + manifest.identifier + " for manifest " + file);
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

    // Set the media package root
    File packageRoot = file.getParentFile();

    // Read tracks
    NodeList trackNodes = (NodeList) xPath.evaluate("/mediapackage/media/track", doc, XPathConstants.NODESET);
    for (int i = 0; i < trackNodes.getLength(); i++) {
      try {
        MediaPackageElement track = elementBuilder
                .elementFromManifest(trackNodes.item(i), packageRoot, !wrap && verify);
        if (track != null) {
          File elementFile = track.getFile();
          if (track != null && (elementFile == null || elementFile.exists()))
            manifest.add(track);
          else if (!ignoreMissingElements)
            throw new MediaPackageException("Track file " + elementFile + " is missing");
          if (wrap)
            track.wrap();
        }
      } catch (IllegalStateException e) {
        log_.warn("Unable to create tracks from manifest: " + e.getMessage());
        throw e;
      } catch (MediaPackageException e) {
        if (!ignoreMissingElements) {
          log_.warn("Error while creating track from manifest:" + e.getMessage());
          throw e;
        }
        updated = true;
      } catch (Throwable t) {
        if (!ignoreMissingElements) {
          log_.warn("Error reading track: " + t.getMessage());
          throw new IllegalStateException(t);
        }
        updated = true;
      }
    }

    // Read catalogs
    NodeList catalogNodes = (NodeList) xPath.evaluate("/mediapackage/metadata/catalog", doc, XPathConstants.NODESET);
    for (int i = 0; i < catalogNodes.getLength(); i++) {
      try {
        MediaPackageElement catalog = elementBuilder.elementFromManifest(catalogNodes.item(i), packageRoot, !wrap
                && verify);
        if (catalog != null) {
          File elementFile = catalog.getFile();
          if (elementFile == null || elementFile.exists())
            manifest.add(catalog);
          else if (!ignoreMissingElements)
            throw new MediaPackageException("Catalog file " + elementFile + " is missing");
          if (wrap)
            catalog.wrap();
        }
      } catch (IllegalStateException e) {
        if (!ignoreMissingElements) {
          log_.warn("Unable to load catalog from manifest: " + e.getMessage());
          throw e;
        }
        updated = true;
      } catch (MediaPackageException e) {
        if (!ignoreMissingElements) {
          log_.warn("Error while loading catalog from manifest:" + e.getMessage());
          throw e;
        }
        updated = true;
      } catch (Throwable t) {
        log_.warn("Error reading catalog: " + t.getMessage());
        throw new IllegalStateException(t);
      }
    }

    // Read attachments
    NodeList attachmentNodes = (NodeList) xPath.evaluate("/mediapackage/attachments/attachment", doc,
            XPathConstants.NODESET);
    for (int i = 0; i < attachmentNodes.getLength(); i++) {
      try {
        MediaPackageElement attachment = elementBuilder.elementFromManifest(attachmentNodes.item(i), packageRoot, !wrap
                && verify);
        if (attachment != null) {
          File elementFile = attachment.getFile();
          if (elementFile == null || elementFile.exists())
            manifest.add(attachment);
          else if (!ignoreMissingElements)
            throw new MediaPackageException("Attachment file " + elementFile + " is missing");
          if (wrap)
            attachment.wrap();
        }
      } catch (IllegalStateException e) {
        if (!ignoreMissingElements) {
          log_.warn("Unable to load attachment from manifest: " + e.getMessage());
          throw e;
        }
        updated = true;
      } catch (MediaPackageException e) {
        if (!ignoreMissingElements) {
          log_.warn("Error while loading attachment from manifest:" + e.getMessage());
          throw e;
        }
        updated = true;
      } catch (Throwable t) {
        log_.warn("Error reading attachment: " + t.getMessage());
        throw new IllegalStateException(t);
      }
    }

    // Has the manifest been updated?
    if (updated) {
      log_.debug("Updating manifest " + manifest);
      manifest.save();
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