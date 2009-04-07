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

import org.opencastproject.util.Checksum;
import org.opencastproject.util.MimeType;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.File;
import java.io.IOException;

/**
 * All classes that will be part of a bundle must implement this interface.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public interface BundleElement extends Comparable<BundleElement> {

  /** The manifest type */
  enum Type {
    Manifest, Timeline, Track, Catalog, Attachment, Other
  };

  /**
   * Returns the element identifier.
   * 
   * @return the element identifier
   */
  String getIdentifier();

  /**
   * Sets the element identifier.
   * 
   * @param id
   *          the new element identifier
   */
  void setIdentifier(String id);

  /**
   * Returns the element's manifest type.
   * 
   * @return the manifest type
   */
  Type getElementType();

  /**
   * Returns a human readable name for this bundle element. If no name was
   * provided, the filename is returned instead.
   * 
   * @return the element name
   */
  String getElementDescription();

  /**
   * Returns the bundle if the element has been added, <code>null</code>
   * otherwise.
   * 
   * @return the bundle
   */
  Bundle getBundle();

  /**
   * Returns a reference to another entitiy, both inside or outside the bundle.
   * 
   * @return the reference
   */
  BundleReference getReference();

  /**
   * Returns the element's filename without the path.
   * <p>
   * For example, if the track is located at <code>
   * /home/replay/archive/mybundle/tracks/slides.mjpeg</code>
   * , this method will return <code>slides.mpeg</code>.
   * 
   * @return the filename
   */
  String getFilename();

  /**
   * Returns the element's path without the filename.
   * <p>
   * For example, if the track is located at <code>
   * /home/replay/archive/mybundle/tracks/slides.mjpeg</code>
   * , this method will return <code>/home/replay/archive/mybundle/tracks</code>.
   * 
   * @return the path
   */
  String getPath();

  /**
   * Returns a reference to the element's file object.
   * 
   * @return the file reference
   */
  File getFile();

  /**
   * Returns the file's checksum.
   * 
   * @return the checksum
   */
  Checksum getChecksum();

  /**
   * Returns the element's mimetype as found in the ISO Mime Type Registrations.
   * <p>
   * For example, in case of motion jpeg slides, this method will return the
   * mime type for <code>video/mj2</code>.
   * 
   * @return the mime type
   */
  MimeType getMimeType();

  /**
   * Returns the element's type as defined for the specific bundle element.
   * <p>
   * For example, in case of a video track, the type could be
   * <code>video/x-presentation</code>.
   * 
   * @return the element flavor
   */
  BundleElementFlavor getFlavor();

  /**
   * Returns the number of bytes that are occupied by this bundle element.
   * 
   * @return the size
   */
  long getSize();

  /**
   * Wraps the element by calculating it's checksums and other properties,
   * updating it should the underlying file have changed.
   * 
   * @throws BundleException
   *           if wrapping the element failed
   */
  void wrap() throws BundleException;

  /**
   * Verifies the integrity of the bundle element.
   * 
   * @throws BundleException
   *           if the bundle element is in an incosistant state
   */
  void verify() throws BundleException;

  /**
   * This method returns the element's xml representation which is used to store
   * it's characteristic data in the bundle manifest.
   * 
   * @param document
   *          the parent
   * @return the element's xml representation
   */
  Node toManifest(Document document);

  /**
   * Tells the element that the bundle was moved to the specified location.
   * 
   * @param oldRoot
   *          the former bundle root directory
   * @param newRoot
   *          the new bundle root directory
   */
  void bundleMoved(File oldRoot, File newRoot);

  /**
   * Integrates the element by copying the underlying resource file to the given
   * destination.
   * 
   * @param dest
   *          the element's new destination. Note that a <em>file</em>, not a
   *          directory must be provided here
   * @throws IOException
   */
  void integrate(File dest) throws IOException;

  /**
   * Adds a reference to the bundle element <code>element</code>.
   * <p>
   * Note that an element can only have one reference. Therefore, any existing
   * reference will be replaced. Also note that if this element is part of a
   * bundle, a consistency check will be made making sure the refered element is
   * also part of the same bundle. If not, a {@link BundleException} will be
   * thrown.
   * 
   * @param element
   *          the element to refere to
   */
  void referTo(BundleElement element);

  /**
   * Adds an arbitrary reference.
   * <p>
   * Note that an element can only have one reference. Therefore, any existing
   * reference will be replaced. Also note that if this element is part of a
   * bundle, a consistency check will be made making sure the refered element is
   * also part of the same bundle. If not, a {@link BundleException} will be
   * thrown.
   * 
   * @param reference
   *          the reference
   */
  void referTo(BundleReference reference);

  /**
   * Removes any reference.
   */
  void clearReference();

}
