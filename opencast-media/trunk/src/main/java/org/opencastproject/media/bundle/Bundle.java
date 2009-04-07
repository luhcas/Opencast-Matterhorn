/**
 *  Copyright 2009 Opencast Project (http://www.opencastproject.org)
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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Interface for a bundle, which is a data container moving through the system,
 * containing metadata, tracks and attachments.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public interface Bundle {

  /**
   * This enumeration lists locations where bundles may reside.
   */
  enum Repository {
    Archive, Inbox, Quarantine;

    public static Repository parseString(String value) {
      if (Archive.toString().equalsIgnoreCase(value))
        return Archive;
      else if (Inbox.toString().equalsIgnoreCase(value))
        return Inbox;
      else if (Quarantine.toString().equalsIgnoreCase(value))
        return Quarantine;
      else
        throw new IllegalArgumentException("Repository type '" + value
            + "' is unkown");
    }
  }

  /** Name of the lock file */
  String LOCKFILE = "__lock.lck";

  /** Relative path to bundle catalogs */
  String CATALOG_DIR = "metadata";

  /** Relative path to bundle tracks */
  String TRACK_DIR = "tracks";

  /** Relative path to bundle attachments */
  String ATTACHMENT_DIR = "attachments";

  /** Relative path to bundle temporary files */
  String TEMP_DIR = "temp";

  /**
   * Returns the bundle identifier.
   * 
   * @return the identifier
   */
  Handle getIdentifier();

  /**
   * Returns the bundle manifest.
   * 
   * @return the manifest
   */
  Manifest getManifest();

  /**
   * Returns the bundle's root path.
   * 
   * @return the root path
   */
  File getRoot();

  /**
   * Returns the bundle start time.
   * 
   * @return the start time
   */
  long getStartDate();

  /**
   * Returns the bundle duration in milliseconds.
   * 
   * @return the duration
   */
  long getDuration();

  /**
   * Returns the bundle's catalog root path.
   * 
   * @return the catalog root path
   */
  File getCatalogRoot();

  /**
   * Returns the bundle's track root path.
   * 
   * @return the track root path
   */
  File getTrackRoot();

  /**
   * Returns the bundle's attachment root path.
   * 
   * @return the attachment root path
   */
  File getAttachmentRoot();

  /**
   * Returns the bundle's temporary items folder.
   * 
   * @return the temporary items folder
   */
  File getDistributionRoot();

  /**
   * Returns <code>true</code> if the bundle is locked. A bundle is considered
   * locked, if it contains a {@link #LOCKFILE}.
   * 
   * @return <code>true</code> if the bundle is locked
   */
  boolean isLocked();

  /**
   * Returns <code>true</code> if the given element is part of the bundle.
   * 
   * @param element
   *          the element
   * @return <code>true</code> if the element belongs to the bundle
   */
  boolean contains(BundleElement element);

  /**
   * Returns an iteration of the bundle elements.
   * 
   * @return the bundle elements
   */
  Iterable<BundleElement> elements();

  /**
   * Returns the element that is identified by the given reference or
   * <code>null</code> if no such element exists.
   * 
   * @param reference
   *          the reference
   * @return the element
   */
  BundleElement getElementByReference(BundleReference reference);

  /**
   * Returns the element that is identified by the given identifier or
   * <code>null</code> if no such element exists.
   * 
   * @param id
   *          the element identifier
   * @return the element
   */
  BundleElement getElementById(String id);

  /**
   * Returns the tracks that are part of this bundle.
   * 
   * @return the tracks
   */
  Track[] getTracks();

  /**
   * Returns the tracks that are part of this bundle and match the given flavor
   * as defined in {@link Track}.
   * 
   * @param flavor
   *          the track's flavor
   * @return the tracks with the specified flavor
   */
  Track[] getTracks(BundleElementFlavor flavor);

  /**
   * Returns the tracks that are part of this bundle and are refering to the
   * element identified by <code>reference</code>.
   * 
   * @param reference
   *          the reference
   * @return the tracks with the specified reference
   */
  Track[] getTracks(BundleReference reference);

  /**
   * Returns the tracks that are part of this bundle and are refering to the
   * element identified by <code>reference</code>.
   * 
   * @param flavor
   *          the element flavor
   * @param reference
   *          the reference
   * @return the tracks with the specified reference
   */
  Track[] getTracks(BundleElementFlavor flavor, BundleReference reference);

  /**
   * Returns <code>true</code> if the bundle contains media tracks of any kind.
   * 
   * @return <code>true</code> if the bundle contains tracks
   */
  boolean hasTracks();

  /**
   * Returns <code>true</code> if the bundle contains media tracks of the
   * specified flavor.
   * 
   * @param flavor
   *          the track's flavor
   * @return <code>true</code> if the bundle contains tracks
   */
  boolean hasTracks(BundleElementFlavor flavor);

  /**
   * Returns the attachments that are part of this bundle.
   * 
   * @return the attachments
   */
  Attachment[] getAttachments();

  /**
   * Returns the attachments that are part of this bundle and match the
   * specified flavor.
   * 
   * @param flavor
   *          the attachment flavor
   * @return the attachments
   */
  Attachment[] getAttachments(BundleElementFlavor flavor);

  /**
   * Returns the attachments that are part of this bundle and are refering to
   * the element identified by <code>reference</code>.
   * 
   * @param reference
   *          the reference
   * @return the attachments with the specified reference
   */
  Attachment[] getAttachments(BundleReference reference);

  /**
   * Returns the attachments that are part of this bundle and are refering to
   * the element identified by <code>reference</code>.
   * 
   * @param flavor
   *          the element flavor
   * @param reference
   *          the reference
   * @return the attachments with the specified reference
   */
  Attachment[] getAttachments(BundleElementFlavor flavor,
      BundleReference reference);

  /**
   * Returns <code>true</code> if the bundle contains attachments of any kind.
   * 
   * @return <code>true</code> if the bundle contains attachments
   */
  boolean hasAttachments();

  /**
   * Returns <code>true</code> if the bundle contains attachments of the
   * specified flavor.
   * 
   * @param flavor
   *          the attachment flavor
   * @return <code>true</code> if the bundle contains attachments
   */
  boolean hasAttachments(BundleElementFlavor flavor);

  /**
   * Returns the catalogs associated with this bundle.
   * 
   * @return the catalogs
   */
  Catalog[] getCatalogs();

  /**
   * Returns the catalogs associated with this bundle that matches the specified
   * flavor.
   * 
   * @param flavor
   *          the catalog type
   * @return the bundle catalogs
   */
  Catalog[] getCatalogs(BundleElementFlavor flavor);

  /**
   * Returns the catalogs that are part of this bundle and are refering to the
   * element identified by <code>reference</code>.
   * 
   * @param reference
   *          the reference
   * @return the catalogs with the specified reference
   */
  Catalog[] getCatalogs(BundleReference reference);

  /**
   * Returns the catalogs that are part of this bundle and are refering to the
   * element identified by <code>reference</code>.
   * 
   * @param flavor
   *          the element flavor
   * @param reference
   *          the reference
   * @return the catalogs with the specified reference
   */
  Catalog[] getCatalogs(BundleElementFlavor flavor, BundleReference reference);

  /**
   * Returns <code>true</code> if the bundle contains catalogs of any kind.
   * 
   * @return <code>true</code> if the bundle contains catalogs
   */
  boolean hasCatalogs();

  /**
   * Returns <code>true</code> if the bundle contains catalogs of any kind.
   * 
   * @param flavor
   *          the catalog flavor
   * @return <code>true</code> if the bundle contains catalogs
   */
  boolean hasCatalogs(BundleElementFlavor flavor);

  /**
   * Returns <code>true</code> if the bundle contains catalogs of any kind
   * refering to the element identified by <code>reference</code>..
   * 
   * @param flavor
   *          the catalog flavor
   * @param reference
   *          the reference
   * @return <code>true</code> if the bundle contains catalogs
   */
  boolean hasCatalogs(BundleElementFlavor flavor, BundleReference reference);

  /**
   * Returns bundle elements that are neither, attachments, catalogs nor tracks.
   * 
   * @return the other bundle elements
   */
  BundleElement[] getUnclassifiedElements();

  /**
   * Returns bundle elements that are neither, attachments, catalogs nor tracks
   * but have the given element flavor.
   * 
   * @param flavor
   *          the element flavor
   * @return the other bundle elements
   */
  BundleElement[] getUnclassifiedElements(BundleElementFlavor flavor);

  /**
   * Returns <code>true</code> if the bundle contains unclassified elements.
   * 
   * @return <code>true</code> if the bundle contains unclassified elements
   */
  boolean hasUnclassifiedElements();

  /**
   * Returns <code>true</code> if the bundle contains unclassified elements
   * matching the specified element type.
   * 
   * @param flavor
   *          element flavor of the unclassified element
   * @return <code>true</code> if the bundle contains unclassified elements
   */
  boolean hasUnclassifiedElements(BundleElementFlavor flavor);

  /**
   * Adds an arbitrary {@link File} to this bundle, utilizing a
   * {@link BundleBuilder} to create a suitable bundle element out of the file.
   * If the file cannot be recognized as being either a metadata catalog or
   * multimedia track, it is added as an attachment.
   * <p>
   * Note that the implementation is actually <em>moving</em> the underlying
   * file in the filesystem. Use this method <em>only</em> if you do not need
   * the bundle element in its originial place anymore.
   * <p>
   * Depending on the implementation, this method may provide significant
   * performance benefits over copying the bundle element.
   * 
   * @param file
   *          the element file
   * @throws BundleException
   *           if the element cannot be accessed
   * @throws UnsupportedBundleElementException
   *           if the element is of an unsupported format
   * @see #add(File, boolean)
   */
  BundleElement add(File file) throws BundleException,
      UnsupportedBundleElementException;

  /**
   * Adds an arbitrary {@link File} to this bundle, utilizing a
   * {@link BundleBuilder} to create a suitable bundle element out of the file.
   * If the file cannot be recognized as being either a metadata catalog or
   * multimedia track, it is added as an attachment.
   * <p>
   * Depending on the parameter <code>move</code>, the underlying file will be
   * moved or copied to the bundle. Use this <code>move = true</code>
   * <em>only</em> if you do not need the catalog in its originial place
   * anymore.
   * 
   * @param file
   *          the element file
   * @throws BundleException
   *           if the element cannot be accessed
   * @throws UnsupportedBundleElementException
   *           if the element is of an unsupported format
   * @see #add(File, boolean)
   */
  BundleElement add(File file, boolean move) throws BundleException,
      UnsupportedBundleElementException;

  /**
   * Adds an arbitrary {@link BundleElement} to this bundle, actually
   * <em>moving</em> the underlying file in the filesystem. Use this method
   * <em>only</em> if you do not need the bundle element in its originial place
   * anymore.
   * <p>
   * Depending on the implementation, this method may provide significant
   * performance benefits over copying the bundle element.
   * 
   * @param element
   *          the element
   * @throws BundleException
   *           if the element cannot be accessed
   * @throws UnsupportedBundleElementException
   *           if the element is of an unsupported format
   * @see #add(BundleElement, boolean)
   */
  void add(BundleElement element) throws BundleException,
      UnsupportedBundleElementException;

  /**
   * Adds an arbitrary {@link BundleElement} to this bundle. Depending on the
   * parameter <code>move</code>, the underlying file will be moved or copied to
   * the bundle. Use this <code>move = true</code> <em>only</em> if you do not
   * need the catalog in its originial place anymore.
   * 
   * @param element
   *          the element
   * @param move
   *          true = move the underlying file, false = create a copy
   * @throws BundleException
   *           if the element cannot be accessed
   * @throws UnsupportedBundleElementException
   *           if the element is of an unsupported format
   */
  void add(BundleElement element, boolean move) throws BundleException,
      UnsupportedBundleElementException;

  /**
   * Adds a track to this bundle, actually <em>moving</em> the underlying file
   * in the filesystem. Use this method <em>only</em> if you do not need the
   * track in its originial place anymore.
   * <p>
   * Depending on the implementation, this method may provide significant
   * performance benefits over copying the track.
   * 
   * @param track
   *          the track
   * @throws BundleException
   *           if the track cannot be accessed
   * @throws UnsupportedBundleElementException
   *           if the track is of an unsupported format
   * @see #add(Track, boolean)
   */
  void add(Track track) throws BundleException,
      UnsupportedBundleElementException;

  /**
   * Adds a track to this bundle. Depending on the parameter <code>move</code>,
   * the underlying file will be moved or copied to the bundle. Use this
   * <code>move = true</code> <em>only</em> if you do not need the track in its
   * originial place anymore.
   * 
   * @param track
   *          the track
   * @param move
   *          true = move the underlying file, false = create a copy
   * @throws BundleException
   *           if the track cannot be accessed
   * @throws UnsupportedBundleElementException
   *           if the track is of an unsupported format
   */
  void add(Track track, boolean move) throws BundleException,
      UnsupportedBundleElementException;

  /**
   * Removes the track from the bundle.
   * 
   * @param track
   *          the track
   * @throws BundleException
   *           if the track cannot be removed
   */
  void remove(Track track) throws BundleException;

  /**
   * Adds catalog information to this bundle, actually <em>moving</em> the
   * underlying file in the filesystem. Use this method <em>only</em> if you do
   * not need the catalog in its originial place anymore.
   * <p>
   * Depending on the implementation, this method may provide significant
   * performance benefits over copying the catalog.
   * 
   * @param catalog
   *          the catalog
   * @throws BundleException
   *           if the catalog cannot be accessed
   * @throws UnsupportedBundleElementException
   *           if the catalog is of an unsupported format
   */
  void add(Catalog catalog) throws BundleException,
      UnsupportedBundleElementException;

  /**
   * Adds catalog information to this bundle. Depending on the parameter
   * <code>move</code>, the underlying file will be moved or copied to the
   * bundle. Use this <code>move = true</code> <em>only</em> if you do not need
   * the catalog in its originial place anymore.
   * 
   * @param catalog
   *          the catalog
   * @param move
   *          true = move the underlying file, false = create a copy
   * @throws BundleException
   *           if the catalog cannot be accessed
   * @throws UnsupportedBundleElementException
   *           if the catalog is of an unsupported format
   */
  void add(Catalog catalog, boolean move) throws BundleException,
      UnsupportedBundleElementException;

  /**
   * Removes the catalog from the bundle.
   * 
   * @param catalog
   *          the catalog
   * @throws BundleException
   *           if the catalog cannot be removed
   */
  void remove(Catalog catalog) throws BundleException;

  /**
   * Adds an attachment to this bundle, actually <em>moving</em> the underlying
   * file in the filesystem. Use this method <em>only</em> if you do not need
   * the attachment in its originial place anymore.
   * <p>
   * Depending on the implementation, this method may provide significant
   * performance benefits over copying the attachment.
   * 
   * @param attachment
   *          the attachment
   * @throws BundleException
   *           if the attachment cannot be accessed
   * @throws UnsupportedBundleElementException
   *           if the attachment is of an unsupported format
   * @see #add(Attachment, boolean)
   */
  void add(Attachment attachment) throws BundleException,
      UnsupportedBundleElementException;

  /**
   * Adds an attachment to this bundle. Depending on the parameter
   * <code>move</code>, the underlying file will be moved or copied to the
   * bundle. Use this <code>move = true</code> <em>only</em> if you do not need
   * the attachment in its originial place anymore.
   * 
   * @param attachment
   *          the attachment
   * @param move
   *          true = move the underlying file, false = create a copy
   * @throws BundleException
   *           if the attachment cannot be accessed
   * @throws UnsupportedBundleElementException
   *           if the attachment is of an unsupported format
   */
  void add(Attachment attachment, boolean move) throws BundleException,
      UnsupportedBundleElementException;

  /**
   * Removes an arbitrary bundle element.
   * 
   * @param element
   *          the bundle element
   * @throws BundleException
   *           if the element cannot be removed
   */
  void remove(BundleElement element) throws BundleException;

  /**
   * Removes the attachment from the bundle.
   * 
   * @param attachment
   *          the attachment
   * @throws BundleException
   *           if the attachment cannot be removed
   */
  void remove(Attachment attachment) throws BundleException;

  /**
   * Returns the bundle's cover or <code>null</code> if no cover is defined.
   * 
   * @return the cover
   */
  Cover getCover();

  /**
   * Adds a cover to this bundle, actually <em>moving</em> the underlying file
   * in the filesystem. Use this method <em>only</em> if you do not need the
   * cover in its originial place anymore.
   * <p>
   * Depending on the implementation, this method may provide significant
   * performance benefits over copying the cover.
   * 
   * @param cover
   *          the cover
   * @throws BundleException
   *           if the cover cannot be accessed
   * @throws UnsupportedBundleElementException
   *           if the cover is of an unsupported format
   * @see #setCover(Cover, boolean)
   */
  void setCover(Cover cover) throws BundleException,
      UnsupportedBundleElementException;

  /**
   * Adds a cover to this bundle. Depending on the parameter <code>move</code>,
   * the underlying file will be moved or copied to the bundle. Use this
   * <code>move = true</code> <em>only</em> if you do not need the cover in its
   * originial place anymore.
   * 
   * @param cover
   *          the cover art
   * @param move
   *          true = move the underlying file, false = create a copy
   * @throws BundleException
   *           if the cover cannot be accessed
   * @throws UnsupportedBundleElementException
   *           if the cover is of an unsupported format
   */
  void setCover(Cover cover, boolean move) throws BundleException,
      UnsupportedBundleElementException;

  /**
   * Removes the cover from the bundle.
   * 
   * @throws BundleException
   *           if the cover cannot be removed
   */
  void removeCover() throws BundleException;

  /**
   * Returns the bundle's size in bytes.
   * 
   * @return the bundle size
   */
  long getSize();

  /**
   * Adds <code>observer</code> to the list of observers of this bundle.
   * 
   * @param observer
   *          the observer
   */
  void addObserver(BundleObserver observer);

  /**
   * Removes <code>observer</code> from the list of observers of this bundle.
   * 
   * @param observer
   *          the observer
   */
  void removeObserver(BundleObserver observer);

  /**
   * Saves the bundle using the given packager to the output stream.
   * 
   * @param packager
   *          the packager
   * @param out
   *          the output stream
   * @throws IOException
   *           if an error occurs when writing to the package file
   * @throws BundleException
   *           if errors occur while packaging the bundle
   */
  void pack(BundlePackager packager, OutputStream out) throws IOException,
      BundleException;

  /**
   * Reconsiders the elements from the manifest and calculates their checksums,
   * then updates the manifest to reflect any updated elements.
   * 
   * @throws BundleException
   */
  void wrap() throws BundleException;

  /**
   * Verifies the bundle consistency by checking the bundle elements for
   * mimetypes and checksums.
   * 
   * @throws BundleException
   *           if an error occurs while checking the bundle
   */
  void verify() throws BundleException;

  /**
   * Saves the bundle manifest.
   * 
   * @throws BundleException
   *           if saving the manifest failed
   */
  void save() throws BundleException;

  /**
   * Deletes the bundle with all its content.
   * 
   * @return <code>true</code> if the bundle could be deleted
   */
  boolean delete();

  /**
   * Moves the bundle to <code>destination</code>.
   * 
   * @param destination
   *          the target location
   * @return <code>true</code> if the bundle could be moved to the destination
   * @throws IOException
   *           if moving the bundle failed
   */
  boolean moveTo(File destination) throws IOException;

  /**
   * Renames the bundle to the new identifier.
   * 
   * @param identifier
   *          the identifier
   * @return <code>true</code> if the bundle could be renamed
   */
  boolean renameTo(Handle identifier);

  /**
   * Copies the bundle to the given location.
   * 
   * @param destination
   *          the destination folder
   * @throws IOException
   *           if moving the bundle failed
   */
  void copyTo(File destination) throws IOException;

}