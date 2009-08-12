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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Interface for a media package, which is a data container moving through the system, containing metadata, tracks and
 * attachments.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: MediaPackage.java 2908 2009-07-17 16:51:07Z ced $
 */
public interface MediaPackage {

  /**
   * This enumeration lists locations where media packages may reside.
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
        throw new IllegalArgumentException("Repository type '" + value + "' is unkown");
    }
  }

  /** Name of the lock file */
  String LOCKFILE = "__lock.lck";

  /** Relative path to media package catalogs */
  String CATALOG_DIR = "metadata";

  /** Relative path to media package tracks */
  String TRACK_DIR = "tracks";

  /** Relative path to media package attachments */
  String ATTACHMENT_DIR = "attachments";

  /** Relative path to media package temporary files */
  String TEMP_DIR = "temp";

  /**
   * Returns the media package identifier.
   * 
   * @return the identifier
   */
  Handle getIdentifier();

  /**
   * Returns the media package's root path.
   * 
   * @return the root path
   */
  File getRoot();

  /**
   * Returns the media package start time.
   * 
   * @return the start time
   */
  long getStartDate();

  /**
   * Returns the media package duration in milliseconds.
   * 
   * @return the duration
   */
  long getDuration();

  /**
   * Returns the media package's catalog root path.
   * 
   * @return the catalog root path
   */
  File getCatalogRoot();

  /**
   * Returns the media package's track root path.
   * 
   * @return the track root path
   */
  File getTrackRoot();

  /**
   * Returns the media package's attachment root path.
   * 
   * @return the attachment root path
   */
  File getAttachmentRoot();

  /**
   * Returns the media package's temporary items folder.
   * 
   * @return the temporary items folder
   */
  File getDistributionRoot();

  /**
   * Returns <code>true</code> if the media package is locked. A media package is considered locked, if it contains a
   * {@link #LOCKFILE}.
   * 
   * @return <code>true</code> if the media package is locked
   */
  boolean isLocked();

  /**
   * Returns <code>true</code> if the given element is part of the media package.
   * 
   * @param element
   *          the element
   * @return <code>true</code> if the element belongs to the media package
   */
  boolean contains(MediaPackageElement element);

  /**
   * Returns an iteration of the media package elements.
   * 
   * @return the media package elements
   */
  Iterable<MediaPackageElement> elements();

  /**
   * Returns the element that is identified by the given reference or <code>null</code> if no such element exists.
   * 
   * @param reference
   *          the reference
   * @return the element
   */
  MediaPackageElement getElementByReference(MediaPackageReference reference);

  /**
   * Returns the element that is identified by the given identifier or <code>null</code> if no such element exists.
   * 
   * @param id
   *          the element identifier
   * @return the element
   */
  MediaPackageElement getElementById(String id);

  /**
   * Returns the tracks that are part of this media package.
   * 
   * @return the tracks
   */
  Track[] getTracks();

  /**
   * Returns the tracks that are part of this media package and match the given flavor as defined in {@link Track}.
   * 
   * @param flavor
   *          the track's flavor
   * @return the tracks with the specified flavor
   */
  Track[] getTracks(MediaPackageElementFlavor flavor);

  /**
   * Returns the tracks that are part of this media package and are refering to the element identified by
   * <code>reference</code>.
   * 
   * @param reference
   *          the reference
   * @return the tracks with the specified reference
   */
  Track[] getTracks(MediaPackageReference reference);

  /**
   * Returns the tracks that are part of this media package and are refering to the element identified by
   * <code>reference</code>.
   * 
   * @param flavor
   *          the element flavor
   * @param reference
   *          the reference
   * @return the tracks with the specified reference
   */
  Track[] getTracks(MediaPackageElementFlavor flavor, MediaPackageReference reference);

  /**
   * Returns <code>true</code> if the media package contains media tracks of any kind.
   * 
   * @return <code>true</code> if the media package contains tracks
   */
  boolean hasTracks();

  /**
   * Returns <code>true</code> if the media package contains media tracks of the specified flavor.
   * 
   * @param flavor
   *          the track's flavor
   * @return <code>true</code> if the media package contains tracks
   */
  boolean hasTracks(MediaPackageElementFlavor flavor);

  /**
   * Returns the attachments that are part of this media package.
   * 
   * @return the attachments
   */
  Attachment[] getAttachments();

  /**
   * Returns the attachments that are part of this media package and match the specified flavor.
   * 
   * @param flavor
   *          the attachment flavor
   * @return the attachments
   */
  Attachment[] getAttachments(MediaPackageElementFlavor flavor);

  /**
   * Returns the attachments that are part of this media package and are refering to the element identified by
   * <code>reference</code>.
   * 
   * @param reference
   *          the reference
   * @return the attachments with the specified reference
   */
  Attachment[] getAttachments(MediaPackageReference reference);

  /**
   * Returns the attachments that are part of this media package and are refering to the element identified by
   * <code>reference</code>.
   * 
   * @param flavor
   *          the element flavor
   * @param reference
   *          the reference
   * @return the attachments with the specified reference
   */
  Attachment[] getAttachments(MediaPackageElementFlavor flavor, MediaPackageReference reference);

  /**
   * Returns <code>true</code> if the media package contains attachments of any kind.
   * 
   * @return <code>true</code> if the media package contains attachments
   */
  boolean hasAttachments();

  /**
   * Returns <code>true</code> if the media package contains attachments of the specified flavor.
   * 
   * @param flavor
   *          the attachment flavor
   * @return <code>true</code> if the media package contains attachments
   */
  boolean hasAttachments(MediaPackageElementFlavor flavor);

  /**
   * Returns the catalogs associated with this media package.
   * 
   * @return the catalogs
   */
  Catalog[] getCatalogs();

  /**
   * Returns the catalogs associated with this media package that matches the specified flavor.
   * 
   * @param flavor
   *          the catalog type
   * @return the media package catalogs
   */
  Catalog[] getCatalogs(MediaPackageElementFlavor flavor);

  /**
   * Returns the catalogs that are part of this media package and are refering to the element identified by
   * <code>reference</code>.
   * 
   * @param reference
   *          the reference
   * @return the catalogs with the specified reference
   */
  Catalog[] getCatalogs(MediaPackageReference reference);

  /**
   * Returns the catalogs that are part of this media package and are refering to the element identified by
   * <code>reference</code>.
   * 
   * @param flavor
   *          the element flavor
   * @param reference
   *          the reference
   * @return the catalogs with the specified reference
   */
  Catalog[] getCatalogs(MediaPackageElementFlavor flavor, MediaPackageReference reference);

  /**
   * Returns <code>true</code> if the media package contains catalogs of any kind.
   * 
   * @return <code>true</code> if the media package contains catalogs
   */
  boolean hasCatalogs();

  /**
   * Returns <code>true</code> if the media package contains catalogs of any kind.
   * 
   * @param flavor
   *          the catalog flavor
   * @return <code>true</code> if the media package contains catalogs
   */
  boolean hasCatalogs(MediaPackageElementFlavor flavor);

  /**
   * Returns <code>true</code> if the media package contains catalogs of any kind refering to the element identified by
   * <code>reference</code>..
   * 
   * @param flavor
   *          the catalog flavor
   * @param reference
   *          the reference
   * @return <code>true</code> if the media package contains catalogs
   */
  boolean hasCatalogs(MediaPackageElementFlavor flavor, MediaPackageReference reference);

  /**
   * Returns media package elements that are neither, attachments, catalogs nor tracks.
   * 
   * @return the other media package elements
   */
  MediaPackageElement[] getUnclassifiedElements();

  /**
   * Returns media package elements that are neither, attachments, catalogs nor tracks but have the given element
   * flavor.
   * 
   * @param flavor
   *          the element flavor
   * @return the other media package elements
   */
  MediaPackageElement[] getUnclassifiedElements(MediaPackageElementFlavor flavor);

  /**
   * Returns <code>true</code> if the media package contains unclassified elements.
   * 
   * @return <code>true</code> if the media package contains unclassified elements
   */
  boolean hasUnclassifiedElements();

  /**
   * Returns <code>true</code> if the media package contains unclassified elements matching the specified element type.
   * 
   * @param flavor
   *          element flavor of the unclassified element
   * @return <code>true</code> if the media package contains unclassified elements
   */
  boolean hasUnclassifiedElements(MediaPackageElementFlavor flavor);

  /**
   * Adds an arbitrary {@link File} to this media package, utilizing a {@link MediaPackageBuilder} to create a suitable
   * media package element out of the file. If the file cannot be recognized as being either a metadata catalog or
   * multimedia track, it is added as an attachment.
   * <p>
   * Note that the implementation is actually <em>moving</em> the underlying file in the filesystem. Use this method
   * <em>only</em> if you do not need the media package element in its originial place anymore.
   * <p>
   * Depending on the implementation, this method may provide significant performance benefits over copying the media
   * package element.
   * 
   * @param file
   *          the element file
   * @throws MediaPackageException
   *           if the element cannot be accessed
   * @throws UnsupportedElementException
   *           if the element is of an unsupported format
   * @see #add(File, boolean)
   */
  MediaPackageElement add(File file) throws MediaPackageException, UnsupportedElementException;

  /**
   * Adds an arbitrary {@link File} to this media package, utilizing a {@link MediaPackageBuilder} to create a suitable
   * media package element out of the file. If the file cannot be recognized as being either a metadata catalog or
   * multimedia track, it is added as an attachment.
   * <p>
   * Depending on the parameter <code>move</code>, the underlying file will be moved or copied to the media package. Use
   * this <code>move = true</code> <em>only</em> if you do not need the catalog in its originial place anymore.
   * 
   * @param file
   *          the element file
   * @throws MediaPackageException
   *           if the element cannot be accessed
   * @throws UnsupportedElementException
   *           if the element is of an unsupported format
   * @see #add(File, boolean)
   */
  MediaPackageElement add(File file, boolean move) throws MediaPackageException, UnsupportedElementException;

  /**
   * Adds an arbitrary {@link MediaPackageElement} to this media package, actually <em>moving</em> the underlying file
   * in the filesystem. Use this method <em>only</em> if you do not need the media package element in its originial
   * place anymore.
   * <p>
   * Depending on the implementation, this method may provide significant performance benefits over copying the media
   * package element.
   * 
   * @param element
   *          the element
   * @throws MediaPackageException
   *           if the element cannot be accessed
   * @throws UnsupportedElementException
   *           if the element is of an unsupported format
   * @see #add(MediaPackageElement, boolean)
   */
  void add(MediaPackageElement element) throws MediaPackageException, UnsupportedElementException;

  /**
   * Adds an arbitrary {@link MediaPackageElement} to this media package. Depending on the parameter <code>move</code>,
   * the underlying file will be moved or copied to the media package. Use this <code>move = true</code> <em>only</em>
   * if you do not need the catalog in its originial place anymore.
   * 
   * @param element
   *          the element
   * @param move
   *          true = move the underlying file, false = create a copy
   * @throws MediaPackageException
   *           if the element cannot be accessed
   * @throws UnsupportedElementException
   *           if the element is of an unsupported format
   */
  void add(MediaPackageElement element, boolean move) throws MediaPackageException, UnsupportedElementException;

  /**
   * Adds a track to this media package, actually <em>moving</em> the underlying file in the filesystem. Use this method
   * <em>only</em> if you do not need the track in its originial place anymore.
   * <p>
   * Depending on the implementation, this method may provide significant performance benefits over copying the track.
   * 
   * @param track
   *          the track
   * @throws MediaPackageException
   *           if the track cannot be accessed
   * @throws UnsupportedElementException
   *           if the track is of an unsupported format
   * @see #add(Track, boolean)
   */
  void add(Track track) throws MediaPackageException, UnsupportedElementException;

  /**
   * Adds a track to this media package. Depending on the parameter <code>move</code>, the underlying file will be moved
   * or copied to the media package. Use this <code>move = true</code> <em>only</em> if you do not need the track in its
   * originial place anymore.
   * 
   * @param track
   *          the track
   * @param move
   *          true = move the underlying file, false = create a copy
   * @throws MediaPackageException
   *           if the track cannot be accessed
   * @throws UnsupportedElementException
   *           if the track is of an unsupported format
   */
  void add(Track track, boolean move) throws MediaPackageException, UnsupportedElementException;

  /**
   * Removes the track from the media package.
   * 
   * @param track
   *          the track
   * @throws MediaPackageException
   *           if the track cannot be removed
   */
  void remove(Track track) throws MediaPackageException;

  /**
   * Adds catalog information to this media package, actually <em>moving</em> the underlying file in the filesystem. Use
   * this method <em>only</em> if you do not need the catalog in its originial place anymore.
   * <p>
   * Depending on the implementation, this method may provide significant performance benefits over copying the catalog.
   * 
   * @param catalog
   *          the catalog
   * @throws MediaPackageException
   *           if the catalog cannot be accessed
   * @throws UnsupportedElementException
   *           if the catalog is of an unsupported format
   */
  void add(Catalog catalog) throws MediaPackageException, UnsupportedElementException;

  /**
   * Adds catalog information to this media package. Depending on the parameter <code>move</code>, the underlying file
   * will be moved or copied to the media package. Use this <code>move = true</code> <em>only</em> if you do not need
   * the catalog in its originial place anymore.
   * 
   * @param catalog
   *          the catalog
   * @param move
   *          true = move the underlying file, false = create a copy
   * @throws MediaPackageException
   *           if the catalog cannot be accessed
   * @throws UnsupportedElementException
   *           if the catalog is of an unsupported format
   */
  void add(Catalog catalog, boolean move) throws MediaPackageException, UnsupportedElementException;

  /**
   * Removes the catalog from the media package.
   * 
   * @param catalog
   *          the catalog
   * @throws MediaPackageException
   *           if the catalog cannot be removed
   */
  void remove(Catalog catalog) throws MediaPackageException;

  /**
   * Adds an attachment to this media package, actually <em>moving</em> the underlying file in the filesystem. Use this
   * method <em>only</em> if you do not need the attachment in its originial place anymore.
   * <p>
   * Depending on the implementation, this method may provide significant performance benefits over copying the
   * attachment.
   * 
   * @param attachment
   *          the attachment
   * @throws MediaPackageException
   *           if the attachment cannot be accessed
   * @throws UnsupportedElementException
   *           if the attachment is of an unsupported format
   * @see #add(Attachment, boolean)
   */
  void add(Attachment attachment) throws MediaPackageException, UnsupportedElementException;

  /**
   * Adds an attachment to this media package. Depending on the parameter <code>move</code>, the underlying file will be
   * moved or copied to the media package. Use this <code>move = true</code> <em>only</em> if you do not need the
   * attachment in its originial place anymore.
   * 
   * @param attachment
   *          the attachment
   * @param move
   *          true = move the underlying file, false = create a copy
   * @throws MediaPackageException
   *           if the attachment cannot be accessed
   * @throws UnsupportedElementException
   *           if the attachment is of an unsupported format
   */
  void add(Attachment attachment, boolean move) throws MediaPackageException, UnsupportedElementException;

  /**
   * Removes an arbitrary media package element.
   * 
   * @param element
   *          the media package element
   * @throws MediaPackageException
   *           if the element cannot be removed
   */
  void remove(MediaPackageElement element) throws MediaPackageException;

  /**
   * Removes the attachment from the media package.
   * 
   * @param attachment
   *          the attachment
   * @throws MediaPackageException
   *           if the attachment cannot be removed
   */
  void remove(Attachment attachment) throws MediaPackageException;

  /**
   * Returns the media package's cover or <code>null</code> if no cover is defined.
   * 
   * @return the cover
   */
  Cover getCover();

  /**
   * Adds a cover to this media package, actually <em>moving</em> the underlying file in the filesystem. Use this method
   * <em>only</em> if you do not need the cover in its originial place anymore.
   * <p>
   * Depending on the implementation, this method may provide significant performance benefits over copying the cover.
   * 
   * @param cover
   *          the cover
   * @throws MediaPackageException
   *           if the cover cannot be accessed
   * @throws UnsupportedElementException
   *           if the cover is of an unsupported format
   * @see #setCover(Cover, boolean)
   */
  void setCover(Cover cover) throws MediaPackageException, UnsupportedElementException;

  /**
   * Adds a cover to this media package. Depending on the parameter <code>move</code>, the underlying file will be moved
   * or copied to the media package. Use this <code>move = true</code> <em>only</em> if you do not need the cover in its
   * originial place anymore.
   * 
   * @param cover
   *          the cover art
   * @param move
   *          true = move the underlying file, false = create a copy
   * @throws MediaPackageException
   *           if the cover cannot be accessed
   * @throws UnsupportedElementException
   *           if the cover is of an unsupported format
   */
  void setCover(Cover cover, boolean move) throws MediaPackageException, UnsupportedElementException;

  /**
   * Removes the cover from the media package.
   * 
   * @throws MediaPackageException
   *           if the cover cannot be removed
   */
  void removeCover() throws MediaPackageException;

  /**
   * Returns the media package's size in bytes.
   * 
   * @return the media package size
   */
  long getSize();

  /**
   * Adds <code>observer</code> to the list of observers of this media package.
   * 
   * @param observer
   *          the observer
   */
  void addObserver(MediaPackageObserver observer);

  /**
   * Removes <code>observer</code> from the list of observers of this media package.
   * 
   * @param observer
   *          the observer
   */
  void removeObserver(MediaPackageObserver observer);

  /**
   * Saves the media package using the given packager to the output stream.
   * 
   * @param packager
   *          the packager
   * @param out
   *          the output stream
   * @throws IOException
   *           if an error occurs when writing to the package file
   * @throws MediaPackageException
   *           if errors occur while packaging the media package
   */
  void pack(MediaPackagePackager packager, OutputStream out) throws IOException, MediaPackageException;

  /**
   * Reconsiders the elements from the manifest and calculates their checksums, then updates the manifest to reflect any
   * updated elements.
   * 
   * @throws MediaPackageException
   */
  void wrap() throws MediaPackageException;

  /**
   * Verifies the media package consistency by checking the media package elements for mimetypes and checksums.
   * 
   * @throws MediaPackageException
   *           if an error occurs while checking the media package
   */
  void verify() throws MediaPackageException;

  /**
   * Saves the media package manifest.
   * 
   * @throws MediaPackageException
   *           if saving the manifest failed
   */
  void save() throws MediaPackageException;

  /**
   * Deletes the media package with all its content.
   * 
   * @return <code>true</code> if the media package could be deleted
   */
  boolean delete();

  /**
   * Moves the media package to <code>destination</code>.
   * 
   * @param destination
   *          the target location
   * @return <code>true</code> if the media package could be moved to the destination
   * @throws IOException
   *           if moving the media package failed
   */
  boolean moveTo(File destination) throws IOException;

  /**
   * Renames the media package to the new identifier.
   * 
   * @param identifier
   *          the identifier
   * @return <code>true</code> if the media package could be renamed
   */
  boolean renameTo(Handle identifier);

  /**
   * Copies the media package to the given location.
   * 
   * @param destination
   *          the destination folder
   * @throws IOException
   *           if moving the media package failed
   */
  void copyTo(File destination) throws IOException;

}