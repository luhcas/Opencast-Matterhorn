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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * A bundle builder provides factory methods for the creation of bundles from
 * manifest files, packages, directories or from sratch.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public interface BundleBuilder {

  /**
   * Creates a new bundle in the temporary directory defined by the java runtime
   * property <code>java.io.tmpdir</code>.
   * 
   * @return the new bundle
   * @throws BundleException
   *           if creation of the new bundle fails
   */
  Bundle createNew() throws BundleException;

  /**
   * Creates a new bundle in the temporary directory defined by the java runtime
   * property <code>java.io.tmpdir</code>.
   * <p>
   * The name of the bundle root folder will be equal to the handle value.
   * </p>
   * 
   * @param handle
   *          the bundle identifier
   * @return the new bundle
   * @throws BundleException
   *           if creation of the new bundle fails
   */
  Bundle createNew(Handle identifier) throws BundleException;

  /**
   * Creates a new bundle in the temporary directory defined by the java runtime
   * property <code>java.io.tmpdir</code>.
   * 
   * @param handle
   *          the bundle identifier
   * @param rootFolder
   *          the bundle root folder
   * @return the new bundle
   * @throws BundleException
   *           if creation of the new bundle fails
   */
  Bundle createNew(Handle identifier, File rootFolder) throws BundleException;

  /**
   * Loads a bundle from the manifest document.
   * 
   * @param manifest
   *          the bundle manifest file
   * @return the bundle
   * @throws BundleException
   *           if loading of the bundle fails
   */
  Bundle loadFromManifest(File manifest) throws BundleException;

  /**
   * Loads a bundle from the manifest document.
   * 
   * @param manifest
   *          the bundle manifest file
   * @param wrap
   *          <code>true</code> to wrap the bundle (ignore checksum errors)
   * @return the bundle
   * @throws BundleException
   *           if loading of the bundle fails
   */
  Bundle loadFromManifest(File manifest, boolean wrap) throws BundleException;

  /**
   * Creates a bundle from the given path in the repository by trying to locate
   * and evaluate the bundle manifest.
   * 
   * @param path
   *          the path into the repository
   * @return the new bundle
   * @throws IllegalStateException
   *           if the manifest was not found
   * @throws BundleException
   *           if a bundle element cannot be added
   */
  Bundle loadFromRepository(String path) throws BundleException;

  /**
   * Creates a bundle from the given root directory by trying to locate and
   * evaluate the bundle manifest.
   * 
   * @param dir
   *          the bundle directory
   * @return the new bundle
   * @throws IllegalStateException
   *           if the manifest was not found
   * @throws BundleException
   *           if a bundle element cannot be added
   */
  Bundle loadFromDirectory(File dir) throws BundleException;

  /**
   * Creates a bundle from the given root directory by trying to locate and
   * evaluate the bundle manifest. In addition to
   * {@link #loadFromDirectory(File)} this method will ignore missing bundle
   * elements, allowing for handling of only parts of a bundle.
   * <p>
   * Note however, that the bundle manifest will be saved to reflect the current
   * bundle state.
   * </p>
   * 
   * @param dir
   *          the bundle directory
   * @return the new bundle
   * @throws IllegalStateException
   *           if the manifest was not found
   * @throws BundleException
   *           if a bundle element cannot be added
   */
  Bundle rebuildFromDirectory(File dir) throws BundleException;

  /**
   * Creates a bundle from the given root directory by trying to locate and
   * evaluate the bundle manifest. In addition to
   * {@link #loadFromDirectory(File)} this method will ignore missing bundle
   * elements, allowing for handling of only parts of a bundle. If
   * <code>ignoreChecksums</code> is set to <code>true</code>, the bundle
   * builder will also handle edited bundle elements that differ in terms of the
   * checksum.
   * <p>
   * Note however, that the bundle manifest will be saved to reflect the current
   * bundle state.
   * </p>
   * 
   * @param dir
   *          the bundle directory
   * @param ignoreChecksums
   *          <code>true</code> to ignore checksum errors
   * @return the new bundle
   * @throws IllegalStateException
   *           if the manifest was not found
   * @throws BundleException
   *           if a bundle element cannot be added
   */
  Bundle rebuildFromDirectory(File dir, boolean ignoreChecksums)
      throws BundleException;

  /**
   * Creates a bundle from the given root directory by trying to locate and
   * evaluate the bundle manifest. In addition to
   * {@link #loadFromDirectory(File)} this method will ignore missing bundle
   * elements, allowing for handling of only parts of a bundle.
   * <p>
   * If <code>ignoreChecksums</code> is set to <code>true</code>, the bundle
   * builder will also handle edited bundle elements that differ in terms of the
   * checksum.
   * </p>
   * <p>
   * If <code>verify</code> is set to <code>true</code>, the bundle builder will
   * make sure that the bundle has a valid identifier.
   * </p>
   * <p>
   * Note however, that the bundle manifest will be saved to reflect the current
   * bundle state.
   * </p>
   * 
   * @param dir
   *          the bundle directory
   * @param ignoreChecksums
   *          <code>true</code> to ignore checksum errors
   * @param verify
   *          <code>true</code> to verify the bundle contents
   * @return the new bundle
   * @throws IllegalStateException
   *           if the manifest was not found
   * @throws BundleException
   *           if a bundle element cannot be added
   */
  Bundle rebuildFromDirectory(File dir, boolean ignoreChecksums, boolean verify)
      throws BundleException;

  /**
   * Loads a bundle from the input stream, using the provided packager to decode
   * the stream.
   * 
   * @param packager
   *          the packager
   * @param in
   *          the input stream
   * @return the bundle
   * @throws BundleException
   *           if loading of the bundle fails
   */
  Bundle loadFromPackage(BundlePackager packager, InputStream in)
      throws IOException, BundleException;

  /**
   * Creates a bundle from the elements found in the specified directory. The
   * builder also tries to retreive an identifier for the newly created bundle.
   * 
   * @param dir
   *          the directory
   * @param ignoreUnknown
   *          <code>true</code> to ignore unknown or unsupported elements
   * @return the bundle
   * @throws BundleException
   *           if creation of the bundle fails
   * @throws UnsupportedBundleElementException
   *           if an unsupported file was found
   */
  Bundle createFromElements(File dir, boolean ignoreUnknown)
      throws BundleException, UnsupportedBundleElementException;

}