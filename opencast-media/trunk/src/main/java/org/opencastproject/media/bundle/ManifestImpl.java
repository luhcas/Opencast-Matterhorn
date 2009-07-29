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
import org.opencastproject.media.bundle.handle.HandleBuilderFactory;
import org.opencastproject.media.bundle.handle.HandleException;
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
 * Implements a bundle manifest that keeps information about a bundle's
 * contents.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public final class ManifestImpl implements Manifest {

  /** The bundle's identifier */
  private Handle identifier = null;

  /** The manifest file */
  private File file = null;

  /** The start date and time */
  private long startTime = 0L;

  /** The bundle duration */
  private long duration = 0L;

  /** The bundle's other (uncategorized) files */
  private List<BundleElement> elements = new ArrayList<BundleElement>();

  /** Number of tracks */
  private int tracks = 0;

  /** Number of metadata catalogs */
  private int catalogs = 0;

  /** Number of attachments */
  private int attachments = 0;

  /** Numer of unclassified elements */
  private int others = 0;

  /** the logging facility provided by log4j */
  private final static Logger log_ = LoggerFactory
      .getLogger(ManifestImpl.class);

  /**
   * Creates a new manifest implementation used to be filled up using the
   * manifest SAX parser.
   * 
   * @param bundleRoot
   *          the bundle root
   * @throws IOException
   *           if the specified file does not exist or cannot be created
   * @throws UnknownFileTypeException
   *           if the manifest is of an unknown file type
   * @throws NoSuchAlgorithmException
   *           if the md5 checksum cannot be computed
   */
  private ManifestImpl(File bundleRoot) throws IOException,
      UnknownFileTypeException, NoSuchAlgorithmException {
    this(bundleRoot, null);
  }

  /**
   * Creates a new bundle manifest for the given bundle.
   * 
   * @param bundle
   *          the associated bundle
   * @param identifier
   *          the bundle identifier
   * @throws IOException
   *           if the specified file does not exist or cannot be created
   * @throws UnknownFileTypeException
   *           if the manifest is of an unknown file type
   * @throws NoSuchAlgorithmException
   *           if the md5 checksum cannot be computed
   */
  ManifestImpl(Bundle bundle, Handle identifier) throws IOException,
      UnknownFileTypeException, NoSuchAlgorithmException {
    this(bundle.getRoot(), identifier);
  }

  /**
   * Creates a new bundle manifest within the given root folder.
   * 
   * @param bundleRoot
   *          the bundle root folder
   * @param identifier
   *          the bundle identifier
   * @throws IOException
   *           if the specified file does not exist or cannot be created
   * @throws UnknownFileTypeException
   *           if the manifest is of an unknown file type
   * @throws NoSuchAlgorithmException
   *           if the md5 checksum cannot be computed
   */
  ManifestImpl(File bundleRoot, Handle identifier) throws IOException,
      UnknownFileTypeException, NoSuchAlgorithmException {
    this.file = new File(bundleRoot, FILENAME);
    if (!this.file.exists())
      file.createNewFile();
    this.identifier = identifier;
  }

  /**
   * @see org.opencastproject.media.bundle.Manifest#getIdentifier()
   */
  public Handle getIdentifier() {
    return identifier;
  }

  /**
   * @see org.opencastproject.media.bundle.Manifest#getFile()
   */
  public File getFile() {
    return file;
  }

  /**
   * Tells the manifest that the bundle has been moved to a new location.
   * 
   * @param oldRoot
   *          the former bundle root directory
   * @param newRoot
   *          the new bundle root directory
   */
  void bundleMoved(File oldRoot, File newRoot) {
    file = new File(newRoot, file.getName());
  }

  /**
   * @see org.opencastproject.media.bundle.Manifest#getDuration()
   */
  public long getDuration() {
    return duration;
  }

  /**
   * @see org.opencastproject.media.bundle.Manifest#getStartDate()
   */
  public long getStartDate() {
    return startTime;
  }

  /**
   * @see org.opencastproject.media.bundle.Manifest#getEntries()
   */
  public BundleElement[] getEntries() {
    ArrayList<BundleElement> entries = new ArrayList<BundleElement>();
    for (BundleElement e : elements) {
      entries.add(e);
    }
    return entries.toArray(new BundleElement[entries.size()]);
  }

  /**
   * Registers a new bundle element with this manifest.
   * 
   * @param element
   *          the new element
   * @throws BundleException
   *           if adding the element fails
   */
  void add(BundleElement element) throws BundleException {
    if (element == null)
      throw new IllegalArgumentException("Bundle element must not be null");
    String id = null;
    if (elements.add(element)) {
      if (element instanceof Track) {
        tracks++;
        id = "track-" + tracks;
        long duration = ((Track) element).getDuration();
        // Todo Do not demand equal durations for now... This is an issue that
        // has to be discussed further
        // if (this.duration > 0 && this.duration != duration)
        // throw new BundleException("Track " + element +
        // " cannot be added due to varying duration (" + duration +
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
    }

    // Check if element has an id
    if (element.getIdentifier() == null) {
      if (element instanceof AbstractBundleElement)
        ((AbstractBundleElement) element).setIdentifier(id);
      else
        throw new IllegalStateException("Found unkown element without id");
    }
  }

  /**
   * Removes the bundle element from the manifest.
   * 
   * @param element
   *          the element to remove
   * @throws BundleException
   *           if removing the element fails
   */
  void remove(BundleElement element) throws BundleException {
    if (element == null)
      throw new IllegalArgumentException("Bundle element must not be null");
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
      throw new BundleException(
          "Parser configuration exception while updating bundle manifest: "
              + e.getMessage());
    } catch (TransformerException e) {
      throw new BundleException(
          "Transformer exception while updating bundle manifest: "
              + e.getMessage());
    } catch (IOException e) {
      throw new BundleException(
          "I/O exception while updating bundle manifest: " + e.getMessage());
    }
  }

  /**
   * Returns <code>true</code> if the manifest contains the bundle element.
   * 
   * @param element
   *          the bundle element
   * @return <code>true</code> if the element is listed in the manifest
   */
  boolean contains(BundleElement element) {
    if (element == null)
      throw new IllegalArgumentException("Bundle element must not be null");
    return (elements.contains(element));
  }

  /**
   * Extracts the list of tracks from the bundle.
   * 
   * @return the tracks
   */
  private Collection<Track> loadTracks() {
    List<Track> tracks = new ArrayList<Track>();
    synchronized (elements) {
      for (BundleElement e : elements) {
        if (e instanceof Track) {
          tracks.add((Track) e);
        }
      }
    }
    return tracks;
  }

  /**
   * Returns the bundle's tracks.
   * 
   * @return the tracks
   */
  Track[] getTracks() {
    Collection<Track> tracks = loadTracks();
    return tracks.toArray(new Track[tracks.size()]);
  }

  /**
   * Returns the bundle's tracks matching the specified flavor.
   * 
   * @param flavor
   *          the track flavor
   * @return the tracks
   */
  Track[] getTracks(BundleElementFlavor flavor) {
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
   * Returns the bundle's tracks that contain the specified reference.
   * 
   * @param reference
   *          the reference
   * @return the tracks
   */
  public Track[] getTracks(BundleReference reference) {
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
   * Returns the bundle's tracks that match the specified flavor and reference.
   * 
   * @param flavor
   *          the track flavor
   * @param reference
   *          the reference
   * @return the tracks
   */
  public Track[] getTracks(BundleElementFlavor flavor, BundleReference reference) {
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
   * Returns <code>true</code> if the bundle contains tracks of any kind.
   * 
   * @return <code>true</code> if the bundle contains tracks
   */
  boolean hasTracks() {
    return tracks > 0;
  }

  /**
   * Returns <code>true</code> if the bundle contains tracks of any kind.
   * 
   * @return <code>true</code> if the bundle contains matching tracks
   */
  boolean hasTracks(BundleElementFlavor flavor) {
    if (flavor == null)
      throw new IllegalArgumentException("Unable to filter by null criterion");
    return getTracks(flavor).length > 0;
  }

  /**
   * Returns <code>true</code> if the bundle contains tracks with a matching
   * reference.
   * 
   * @return <code>true</code> if the bundle contains matching tracks
   */
  boolean hasTracks(BundleReference reference) {
    if (reference == null)
      throw new IllegalArgumentException("Unable to filter by null reference");
    else
      return getTracks(reference).length > 0;
  }

  /**
   * Extracts the list of attachments from the bundle.
   * 
   * @return the attachments
   */
  private Collection<Attachment> loadAttachments() {
    List<Attachment> attachments = new ArrayList<Attachment>();
    synchronized (elements) {
      for (BundleElement e : elements) {
        if (e instanceof Attachment) {
          attachments.add((Attachment) e);
        }
      }
    }
    return attachments;
  }

  /**
   * Returns the bundle's attachments.
   * 
   * @return the attachments
   */
  Attachment[] getAttachments() {
    Collection<Attachment> attachments = loadAttachments();
    return attachments.toArray(new Attachment[attachments.size()]);
  }

  /**
   * Returns the bundle's attachments matching the specified flavor.
   * 
   * @param flavor
   *          the attachment flavor
   * @return the attachments
   */
  Attachment[] getAttachments(BundleElementFlavor flavor) {
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
   * Returns the bundle's attachments that contain the specified reference.
   * 
   * @param reference
   *          the reference
   * @return the attachments
   */
  public Attachment[] getAttachments(BundleReference reference) {
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
   * Returns the bundle's attachments that match the specified flavor and
   * reference.
   * 
   * @param flavor
   *          the attachment flavor
   * @param reference
   *          the reference
   * @return the attachments
   */
  public Attachment[] getAttachments(BundleElementFlavor flavor,
      BundleReference reference) {
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
   * Returns <code>true</code> if the bundle contains attachments of any kind.
   * 
   * @return <code>true</code> if the bundle contains attachments
   */
  boolean hasAttachments() {
    return attachments > 0;
  }

  /**
   * Returns <code>true</code> if the bundle contains attachments of any kind.
   * 
   * @return <code>true</code> if the bundle contains matching attachments
   */
  boolean hasAttachments(BundleElementFlavor flavor) {
    if (flavor == null)
      throw new IllegalArgumentException("Unable to filter by null criterion");
    return getAttachments(flavor).length > 0;
  }

  /**
   * Returns <code>true</code> if the bundle contains attachments with a
   * matching reference.
   * 
   * @return <code>true</code> if the bundle contains matching attachments
   */
  boolean hasAttachments(BundleReference reference) {
    if (reference == null)
      throw new IllegalArgumentException("Unable to filter by null reference");
    else
      return getAttachments(reference).length > 0;
  }

  /**
   * Extracts the list of catalogs from the bundle.
   * 
   * @return the catalogs
   */
  private Collection<Catalog> loadCatalogs() {
    List<Catalog> catalogs = new ArrayList<Catalog>();
    synchronized (elements) {
      for (BundleElement e : elements) {
        if (e instanceof Catalog) {
          catalogs.add((Catalog) e);
        }
      }
    }
    return catalogs;
  }

  /**
   * Returns the bundle's catalogs.
   * 
   * @return the catalogs
   */
  Catalog[] getCatalogs() {
    Collection<Catalog> catalogs = loadCatalogs();
    return catalogs.toArray(new Catalog[catalogs.size()]);
  }

  /**
   * Returns the bundle's catalogs matching the specified flavor.
   * 
   * @param flavor
   *          the catalog flavor
   * @return the catalogs
   */
  Catalog[] getCatalogs(BundleElementFlavor flavor) {
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
   * Returns the bundle's catalogs that contain the specified reference.
   * 
   * @param reference
   *          the reference
   * @return the catalogs
   */
  public Catalog[] getCatalogs(BundleReference reference) {
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
   * Returns the bundle's catalogs that match the specified flavor and
   * reference.
   * 
   * @param flavor
   *          the attachment flavor
   * @param reference
   *          the reference
   * @return the catalogs
   */
  public Catalog[] getCatalogs(BundleElementFlavor flavor,
      BundleReference reference) {
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
   * Returns <code>true</code> if the bundle contains catalogs of any kind.
   * 
   * @return <code>true</code> if the bundle contains catalogs
   */
  boolean hasCatalogs() {
    return catalogs > 0;
  }

  /**
   * Returns <code>true</code> if the bundle contains catalogs of any kind.
   * 
   * @return <code>true</code> if the bundle contains matching catalogs
   */
  boolean hasCatalogs(BundleElementFlavor flavor) {
    if (flavor == null)
      throw new IllegalArgumentException("Unable to filter by null criterion");
    return getCatalogs(flavor).length > 0;
  }

  /**
   * Returns <code>true</code> if the bundle contains catalogs with a matching
   * reference.
   * 
   * @return <code>true</code> if the bundle contains matching catalogs
   */
  boolean hasCatalogs(BundleReference reference) {
    if (reference == null)
      throw new IllegalArgumentException("Unable to filter by null reference");
    else
      return getCatalogs(reference).length > 0;
  }

  /**
   * Returns <code>true</code> if the bundle contains catalogs of any kind with
   * a matching reference.
   * 
   * @return <code>true</code> if the bundle contains matching catalogs
   */
  boolean hasCatalogs(BundleElementFlavor flavor, BundleReference reference) {
    if (flavor == null)
      throw new IllegalArgumentException("Unable to filter by null criterion");
    if (reference == null)
      throw new IllegalArgumentException("Unable to filter by null reference");
    else
      return getCatalogs(flavor, reference).length > 0;
  }

  /**
   * Returns the bundle elements that are not attachments, metadata or tracks.
   * 
   * @return the elements
   */
  public BundleElement[] getUnclassifiedElements() {
    return getUnclassifiedElements(null);
  }

  /**
   * Returns the bundle elements that are not attachments, metadata or tracks
   * and match the specified mime type.
   * 
   * @param type
   *          the mime type
   * @return the elements
   */
  public BundleElement[] getUnclassifiedElements(BundleElementFlavor type) {
    List<BundleElement> unclassifieds = new ArrayList<BundleElement>();
    synchronized (elements) {
      for (BundleElement e : elements) {
        if (!(e instanceof Attachment) && !(e instanceof Catalog)
            && !(e instanceof Track)) {
          if (type == null || type.equals(e.getFlavor())) {
            unclassifieds.add(e);
          }
        }
      }
    }
    return unclassifieds.toArray(new BundleElement[unclassifieds.size()]);
  }

  /**
   * Returns <code>true</code> if the bundle contains unclassified elements.
   * 
   * @return <code>true</code> if the bundle contains unclassified elements
   */
  boolean hasUnclassifiedElements() {
    return hasUnclassifiedElements(null);
  }

  /**
   * Returns <code>true</code> if the bundle contains unclassified elements.
   * 
   * @return <code>true</code> if the bundle contains unclassified elements
   */
  boolean hasUnclassifiedElements(BundleElementFlavor type) {
    if (type == null)
      return others > 0;
    synchronized (elements) {
      for (BundleElement e : elements) {
        if (!(e instanceof Attachment) && !(e instanceof Catalog)
            && !(e instanceof Track)) {
          if (type.equals(e.getFlavor())) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Reconsiders the elements from the manifest and calculates their checksums,
   * then updates the manifest to reflect any updated elements.
   * 
   * @throws BundleException
   *           if the checksum cannot be recalculated
   */
  void wrap() throws BundleException {
    for (BundleElement element : elements) {
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
  void save() throws TransformerException, ParserConfigurationException,
      IOException {
    DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
        .newInstance();
    docBuilderFactory.setNamespaceAware(true);
    DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
    Document doc = docBuilder.newDocument();

    // Root element "bundle"
    Element bundle = doc.createElement("Bundle");
    doc.appendChild(bundle);

    // Handle
    if (identifier != null)
      bundle.setAttribute("id", identifier.toString());

    // Start time
    if (startTime > 0)
      bundle.setAttribute("start", DateTimeSupport.toUTC(startTime));

    // Duration
    if (duration > 0)
      bundle.setAttribute("duration", Long.toString(duration));

    // Separate the bundle members
    List<Track> tracks = new ArrayList<Track>();
    List<Attachment> attachments = new ArrayList<Attachment>();
    List<Catalog> metadata = new ArrayList<Catalog>();
    List<BundleElement> others = new ArrayList<BundleElement>();

    // Sort bundle elements
    for (BundleElement e : elements) {
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
      Element tracksNode = doc.createElement("Media");
      Collections.sort(tracks);
      for (Track t : tracks) {
        tracksNode.appendChild(t.toManifest(doc));
      }
      bundle.appendChild(tracksNode);
    }

    // Attachments
    if (attachments.size() > 0) {
      Element attachmentsNode = doc.createElement("Attachments");
      Collections.sort(attachments);
      for (Attachment a : attachments) {
        attachmentsNode.appendChild(a.toManifest(doc));
      }
      bundle.appendChild(attachmentsNode);
    }

    // Metadata
    if (metadata.size() > 0) {
      Element metadataNode = doc.createElement("Metadata");
      Collections.sort(metadata);
      for (Catalog m : metadata) {
        metadataNode.appendChild(m.toManifest(doc));
      }
      bundle.appendChild(metadataNode);
    }

    // Unclassified
    if (others.size() > 0) {
      Element othersNode = doc.createElement("Unclassified");
      Collections.sort(others);
      for (BundleElement e : others) {
        othersNode.appendChild(e.toManifest(doc));
      }
      bundle.appendChild(othersNode);
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
    serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount",
        "4");
    serializer.transform(new DOMSource(doc), streamResult);
    fos.flush();
    fos.close();
  }

  /**
   * Creates a new manifest file at the specified location for a bundle with the
   * given identifier.
   * 
   * @param bundleRoot
   *          the bundle root directory
   * @param identifier
   *          the bundle identifier
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
  static ManifestImpl newInstance(File bundleRoot, Handle identifier)
      throws IOException, UnknownFileTypeException,
      ParserConfigurationException, TransformerException,
      NoSuchAlgorithmException {
    ManifestImpl m = new ManifestImpl(bundleRoot, identifier);
    m.save();
    return m;
  }

  /**
   * Reads a manifest from the specified file and returns it encapsulated in a
   * {@link Manifest} object. The integrity of all elements is verified before
   * they are added to the bundle.
   * 
   * @param file
   *          the manifest file
   * @return the manifest object
   * @throws BundleException
   *           if the bundle is in an inconsistent state
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
  public static ManifestImpl fromFile(File file) throws BundleException,
      IOException, UnknownFileTypeException, ParserConfigurationException,
      SAXException, TransformerException, XPathExpressionException,
      ConfigurationException, HandleException, ParseException {
    try {
      return fromFile(file, false, false, false);
    } catch (NoSuchAlgorithmException e) {
      // This will not happen, since verify is false
      throw new IllegalStateException("Unpredicted application state reached");
    }
  }

  /**
   * Reads a manifest from the specified file and returns it encapsulated in a
   * {@link Manifest} object.
   * 
   * @param file
   *          the manifest file
   * @param ignoreMissingElements
   *          <code>true</code> to ignore and remove missing elements
   * @param wrap
   *          <code>true</code> to ignore and recreate wrong checksums
   * @param verify
   *          <code>true</code> to verify the bundle element's integrity
   * @return the manifest object
   * @throws BundleException
   *           if the bundle is in an inconsistent state
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
  public static ManifestImpl fromFile(File file, boolean ignoreMissingElements,
      boolean wrap, boolean verify) throws BundleException, IOException,
      UnknownFileTypeException, NoSuchAlgorithmException,
      ParserConfigurationException, SAXException, TransformerException,
      XPathExpressionException, ConfigurationException, HandleException,
      ParseException {
    DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
        .newInstance();
    DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
    Document doc = docBuilder.parse(new FileInputStream(file));

    // True if the manifest has been updated while beeing parsed. If the
    // checksums are being recreated, we do a save just to make sure
    boolean updated = wrap;

    // Prepare xpath and element builder
    ManifestImpl manifest = new ManifestImpl(file.getParentFile());
    XPath xPath = XPathFactory.newInstance().newXPath();
    BundleElementBuilder elementBuilder = BundleElementBuilderFactory
        .newInstance().newElementBuilder();
    if (elementBuilder == null)
      throw new IllegalStateException(
          "Unable to create a bundle element builder");

    // Handle
    String id = xPath.evaluate("/Bundle/@id", doc);
    if (id != null && !"".equals(id)) {
      manifest.identifier = HandleBuilderFactory.newInstance()
          .newHandleBuilder().fromValue(id);
    } else {
      manifest.identifier = HandleBuilderFactory.newInstance()
          .newHandleBuilder().createNew();
      updated = true;
      log_.info("Created handle " + manifest.identifier + " for manifest "
          + file);
    }

    // Start time
    String strStart = xPath.evaluate("/Bundle/@start", doc);
    if (strStart != null && !"".equals(strStart)) {
      manifest.startTime = DateTimeSupport.fromUTC(strStart);
    }

    // Duration
    String strDuration = xPath.evaluate("/Bundle/@duration", doc);
    if (strDuration != null && !"".equals(strDuration)) {
      manifest.duration = Long.parseLong(strDuration);
    }

    // Set the bundle root
    File bundleRoot = file.getParentFile();

    // Read tracks
    NodeList trackNodes = (NodeList) xPath.evaluate("/Bundle/Media/Track", doc,
        XPathConstants.NODESET);
    for (int i = 0; i < trackNodes.getLength(); i++) {
      try {
        BundleElement track = elementBuilder.elementFromManifest(trackNodes
            .item(i), bundleRoot, !wrap && verify);
        if (track != null) {
          File elementFile = track.getFile();
          if (track != null && (elementFile == null || elementFile.exists()))
            manifest.add(track);
          else if (!ignoreMissingElements)
            throw new BundleException("Track file " + elementFile
                + " is missing");
          if (wrap)
            track.wrap();
        }
      } catch (IllegalStateException e) {
        log_.warn("Unable to create tracks from manifest: " + e.getMessage());
        throw e;
      } catch (BundleException e) {
        if (!ignoreMissingElements) {
          log_.warn("Error while creating track from manifest:"
              + e.getMessage());
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
    NodeList catalogNodes = (NodeList) xPath.evaluate(
        "/Bundle/Metadata/Catalog", doc, XPathConstants.NODESET);
    for (int i = 0; i < catalogNodes.getLength(); i++) {
      try {
        BundleElement catalog = elementBuilder.elementFromManifest(catalogNodes
            .item(i), bundleRoot, !wrap && verify);
        if (catalog != null) {
          File elementFile = catalog.getFile();
          if (elementFile == null || elementFile.exists())
            manifest.add(catalog);
          else if (!ignoreMissingElements)
            throw new BundleException("Catalog file " + elementFile
                + " is missing");
          if (wrap)
            catalog.wrap();
        }
      } catch (IllegalStateException e) {
        if (!ignoreMissingElements) {
          log_.warn("Unable to load catalog from manifest: " + e.getMessage());
          throw e;
        }
        updated = true;
      } catch (BundleException e) {
        if (!ignoreMissingElements) {
          log_.warn("Error while loading catalog from manifest:"
              + e.getMessage());
          throw e;
        }
        updated = true;
      } catch (Throwable t) {
        log_.warn("Error reading catalog: " + t.getMessage());
        throw new IllegalStateException(t);
      }
    }

    // Read attachments
    NodeList attachmentNodes = (NodeList) xPath.evaluate(
        "/Bundle/Attachments/Attachment", doc, XPathConstants.NODESET);
    for (int i = 0; i < attachmentNodes.getLength(); i++) {
      try {
        BundleElement attachment = elementBuilder.elementFromManifest(
            attachmentNodes.item(i), bundleRoot, !wrap && verify);
        if (attachment != null) {
          File elementFile = attachment.getFile();
          if (elementFile == null || elementFile.exists())
            manifest.add(attachment);
          else if (!ignoreMissingElements)
            throw new BundleException("Attachment file " + elementFile
                + " is missing");
          if (wrap)
            attachment.wrap();
        }
      } catch (IllegalStateException e) {
        if (!ignoreMissingElements) {
          log_.warn("Unable to load attachment from manifest: "
              + e.getMessage());
          throw e;
        }
        updated = true;
      } catch (BundleException e) {
        if (!ignoreMissingElements) {
          log_.warn("Error while loading attachment from manifest:"
              + e.getMessage());
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