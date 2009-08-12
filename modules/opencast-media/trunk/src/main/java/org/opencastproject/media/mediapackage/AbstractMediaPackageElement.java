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

import org.opencastproject.util.Checksum;
import org.opencastproject.util.ChecksumType;
import org.opencastproject.util.FileSupport;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.UnknownFileTypeException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;

/**
 * This class provides base functionality for media package elements.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: AbstractMediaPackageElement.java 2905 2009-07-15 16:16:05Z ced $
 */
public abstract class AbstractMediaPackageElement implements MediaPackageElement, Serializable {

  /** Serial version uid */
  private static final long serialVersionUID = 1L;

  /** The element identifier */
  protected String id = null;

  /** The element's type whithin the manifest: Track, Catalog etc. */
  protected Type elementType = null;

  /** The element's description */
  protected String description = null;

  /** The element's mime type, e. g. 'audio/mp3' */
  protected MimeType mimeType = null;

  /** The element's type, e. g. 'track/slide' */
  protected MediaPackageElementFlavor flavor = null;

  /** Path to the parent directory */
  protected String path = null;

  /** Complete file name, including suffix */
  protected String fileName = null;

  /** File size in bytes */
  protected long size = -1L;

  /** The element's checksum */
  protected Checksum checksum = null;

  /** The parent media package */
  protected MediaPackage mediaPackage = null;

  /** The optional reference to other elements or series */
  protected MediaPackageReference reference = null;

  /**
   * Creates a new media package element, consisting of the given file.
   * 
   * @param elementType
   *          the type, e. g. Track, Catalog etc.
   * @param flavor
   *          the flavor
   * @param file
   *          the element file
   * @throws IOException
   *           if the file cannot be accessed
   * @throws UnknownFileTypeException
   *           if the file type is unknown
   * @throws NoSuchAlgorithmException
   *           if the md5 checksum cannot be computed
   */
  protected AbstractMediaPackageElement(Type elementType, MediaPackageElementFlavor flavor, File file)
          throws IOException, UnknownFileTypeException, NoSuchAlgorithmException {
    this(null, elementType, flavor, MimeTypes.fromFile(file), file, Checksum.create(ChecksumType.DEFAULT_TYPE, file));
  }

  /**
   * Creates a new media package element, consisting of the given file.
   * 
   * @param elementType
   *          the type, e. g. Track, Catalog etc.
   * @param flavor
   *          the flavor
   * @param file
   *          the element file
   * @param checksum
   *          the file's checksum
   * @throws IOException
   *           if the file cannot be accessed
   * @throws UnknownFileTypeException
   *           if the file type is unknown
   */
  protected AbstractMediaPackageElement(Type elementType, MediaPackageElementFlavor flavor, File file, Checksum checksum)
          throws IOException, UnknownFileTypeException {
    this(null, elementType, flavor, MimeTypes.fromFile(file), file, checksum);
  }

  /**
   * Creates a new media package element, consisting of the given file.
   * 
   * @param elementType
   *          the type, e. g. Track, Catalog etc.
   * @param flavor
   *          the flavor
   * @param mimeType
   *          the file's mime type
   * @param file
   *          the element file
   * @throws IOException
   *           if the file cannot be accessed
   * @throws NoSuchAlgorithmException
   *           if the md5 checksum cannot be computed
   */
  protected AbstractMediaPackageElement(Type elementType, MediaPackageElementFlavor flavor, MimeType mimeType, File file)
          throws IOException, NoSuchAlgorithmException {
    this(null, elementType, flavor, mimeType, file, Checksum.create(ChecksumType.DEFAULT_TYPE, file));
  }

  /**
   * Creates a new media package element, consisting of the given file.
   * 
   * @param elementType
   *          the type, e. g. Track, Catalog etc.
   * @param flavor
   *          the flavor
   * @param mimeType
   *          the file's mime type
   * @param file
   *          the element file
   * @param checksum
   *          the file's checksum
   * @throws IOException
   *           if the file cannot be accessed
   */
  protected AbstractMediaPackageElement(Type elementType, MediaPackageElementFlavor flavor, MimeType mimeType,
          File file, Checksum checksum) throws IOException {
    this(null, elementType, flavor, mimeType, file, checksum);
  }

  /**
   * Creates a new media package element, consisting of the given file and mime type.
   * 
   * @param id
   *          the element identifier
   * @param elementType
   *          the type, e. g. Track, Catalog etc.
   * @param flavor
   *          the flavor
   * @param mimeType
   *          the file's mime type
   * @param file
   *          the element file
   * @param checksum
   *          the file's checksum
   * @throws IOException
   *           if the file cannot be accessed
   */
  protected AbstractMediaPackageElement(String id, Type elementType, MediaPackageElementFlavor flavor,
          MimeType mimeType, File file, Checksum checksum) throws IOException {
    if (elementType == null)
      throw new IllegalArgumentException("Argument 'elementType' is null");
    if (file == null)
      throw new IllegalArgumentException("Argument 'file' is null");
    if (flavor == null)
      throw new IllegalArgumentException("Argument 'flavor' is null");
    if (mimeType == null)
      throw new IllegalArgumentException("Argument 'mimeType' is null");
    if (checksum == null)
      throw new IllegalArgumentException("Argument 'checksum' is null");
    this.id = id;
    this.elementType = elementType;
    this.flavor = flavor;
    this.mimeType = mimeType;
    this.fileName = file.getName();
    this.path = file.getParentFile().getAbsolutePath();
    this.checksum = checksum;
  }

  /**
   * Sets the element id.
   * 
   * @param id
   *          the new id
   */
  public void setIdentifier(String id) {
    this.id = id;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElement#getIdentifier()
   */
  public String getIdentifier() {
    return id;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElement#getMediaPackage()
   */
  public MediaPackage getMediaPackage() {
    return mediaPackage;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElement#getElementType()
   */
  public Type getElementType() {
    return elementType;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElement#getElementDescription()
   */
  public String getElementDescription() {
    return (description != null) ? description : getFilename();
  }

  /**
   * Sets the element name.
   * 
   * @param name
   *          the name
   */
  public void setElementDescription(String name) {
    this.description = name;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElement#getReference()
   */
  public MediaPackageReference getReference() {
    return reference;
  }

  /**
   * Sets the media package element's reference.
   * 
   * @param reference
   *          the reference
   */
  public void setReference(MediaPackageReference reference) {
    this.reference = reference;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElement#getFilename()
   */
  public String getFilename() {
    return fileName;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElement#getPath()
   */
  public String getPath() {
    return path;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElement#getFile()
   */
  public File getFile() {
    return new File(this.path, fileName);
  }

  /**
   * Sets the file that is used to store the media package element. Only call this method if you know what you are
   * doing.
   * 
   * @param file
   *          the file
   */
  public void setFile(File file) {
    this.fileName = file.getName();
    this.path = file.getParentFile().getAbsolutePath();
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElement#getChecksum()
   */
  public Checksum getChecksum() {
    return checksum;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElement#getMimeType()
   */
  public MimeType getMimeType() {
    return mimeType;
  }

  /**
   * Sets the element's flavor.
   * 
   * @param flavor
   *          the flavor
   */
  public void setFlavor(MediaPackageElementFlavor flavor) {
    this.flavor = flavor;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElement#getFlavor()
   */
  public MediaPackageElementFlavor getFlavor() {
    return flavor;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElement#getSize()
   */
  public long getSize() {
    if (size < 0) {
      size = getFile().length();
    }
    return size;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElement#mediaPackageMoved(File, java.io.File)
   */
  public void mediaPackageMoved(File oldRoot, File newRoot) {
    String relativePath = this.path.substring(oldRoot.getAbsolutePath().length());
    this.path = PathSupport.concat(newRoot.getAbsolutePath(), relativePath);
  }

  /**
   * Deletes the media package element from its current location.
   * 
   * @throws IOException
   *           if the element cannot be deleted
   */
  void delete() throws IOException {
    getFile().delete();
  }

  /**
   * Sets the parent media package.
   * <p>
   * <b>Note</b> This method is only used by the media package and should not be called from elsewhere.
   * 
   * @param mediaPackage
   *          the parent media package
   */
  void setMediaPackage(MediaPackage mediaPackage) {
    this.mediaPackage = mediaPackage;

    // Adjust the reference
    if (reference == null) {
      if (mediaPackage != null)
        reference = new MediaPackageReferenceImpl();
      else
        reference = null;
    }
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElement#referTo(org.opencastproject.media.mediapackage.MediaPackage)
   */
  public void referTo(MediaPackage mediaPackage) {
    referTo(new MediaPackageReferenceImpl(mediaPackage));
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElement#referTo(org.opencastproject.media.mediapackage.MediaPackageElement)
   */
  public void referTo(MediaPackageElement element) {
    referTo(new MediaPackageReferenceImpl(element));
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElement#referTo(org.opencastproject.media.mediapackage.MediaPackageReference)
   */
  public void referTo(MediaPackageReference reference) {
    // TODO: Check reference consistency
    this.reference = reference;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElement#clearReference()
   */
  public void clearReference() {
    this.reference = null;
  }

  public void integrate(File dest) throws IOException {
    if (dest == null)
      throw new IllegalArgumentException("Integration destination must not be null");
    if (dest.exists())
      throw new IllegalArgumentException("Integration destination already exists");

    File currentLocation = getFile();
    fileName = dest.getName();
    String parent = dest.getParent();
    path = parent != null ? parent : "";
    FileSupport.copy(currentLocation, dest);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElement#wrap()
   */
  public void wrap() throws MediaPackageException {
    checksum = null;
    Checksum c = calculateChecksum();
    if (checksum == null || !checksum.equals(c)) {
      checksum = c;
    }
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElement#verify()
   */
  public void verify() throws MediaPackageException {
    Checksum c = calculateChecksum();
    if (checksum != null && !checksum.equals(c)) {
      throw new MediaPackageException("Checksum mismatch for " + this);
    }
    checksum = c;
  }

  /**
   * Calculates and returns the underlying file's checksum.
   * 
   * @return the checksum
   * @throws MediaPackageException
   *           if the file could not be accessed or the checsum could not be created
   */
  private Checksum calculateChecksum() throws MediaPackageException {
    Checksum c = null;
    File file = getFile();
    if (!file.exists())
      throw new MediaPackageException(this + " " + file.getName() + " not found");
    if (!file.canRead())
      throw new MediaPackageException(this + " " + file.getName() + " not readable");
    if (!file.canRead())
      throw new MediaPackageException(this + " " + file.getName() + " not a file");
    if (checksum != null) {
      try {
        c = Checksum.create(checksum.getType(), file);
      } catch (NoSuchAlgorithmException e) {
        throw new MediaPackageException("Unable to compute checksum for " + this + ": Checksum algorithm for "
                + checksum.getType() + " not found: " + e);
      } catch (Throwable t) {
        throw new MediaPackageException("Unable to compute checksum for " + this + ": " + t.getMessage());
      }
    }
    return c;
  }

  /**
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(MediaPackageElement o) {
    return getFile().getAbsolutePath().compareTo(o.getFile().getAbsolutePath());
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof MediaPackageElement) {
      MediaPackageElement e = (MediaPackageElement) obj;
      if (mediaPackage != null && !mediaPackage.equals(e.getMediaPackage()))
        return false;
      return getFile().equals(e.getFile());
    }
    return false;
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return getFilename().hashCode();
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElement#toManifest(Document)
   */
  public Node toManifest(Document document) {
    Element node = document.createElement(elementType.toString());
    node.setAttribute("id", id);
    node.setAttribute("type", flavor.toString());
    if (reference != null && (mediaPackage == null || !reference.matches(new MediaPackageReferenceImpl(mediaPackage))))
      node.setAttribute("ref", reference.toString());

    // Description
    if (description != null) {
      Element descriptionNode = document.createElement("Description");
      descriptionNode.appendChild(document.createTextNode(description));
      node.appendChild(descriptionNode);
    }

    // MimeType
    Element mimeNode = document.createElement("MimeType");
    mimeNode.appendChild(document.createTextNode(mimeType.toString()));
    node.appendChild(mimeNode);

    // Checksum
    Element checksumNode = document.createElement("Checksum");
    checksumNode.setAttribute("type", checksum.getType().getName());
    checksumNode.appendChild(document.createTextNode(checksum.getValue()));
    node.appendChild(checksumNode);

    return node;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    String s = (description != null) ? description : fileName;
    s += " (" + flavor + ", " + mimeType + ")";
    return s.toLowerCase();
  }

}
