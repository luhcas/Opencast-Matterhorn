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
import java.io.InputStream;

/**
 * A media package builder provides factory methods for the creation of media packages from manifest files, packages,
 * directories or from sratch.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: MediaPackageBuilder.java 2700 2009-03-18 15:49:02Z wunden $
 */
public interface MediaPackageBuilder {

  /**
   * Creates a new media package in the temporary directory defined by the java runtime property
   * <code>java.io.tmpdir</code>.
   * 
   * @return the new media package
   * @throws MediaPackageException
   *           if creation of the new media package fails
   */
  MediaPackage createNew() throws MediaPackageException;

  /**
   * Creates a new media package in the temporary directory defined by the java runtime property
   * <code>java.io.tmpdir</code>.
   * <p>
   * The name of the media package root folder will be equal to the handle value.
   * </p>
   * 
   * @param handle
   *          the media package identifier
   * @return the new media package
   * @throws MediaPackageException
   *           if creation of the new media package fails
   */
  MediaPackage createNew(Handle identifier) throws MediaPackageException;

  /**
   * Creates a new media package in the temporary directory defined by the java runtime property
   * <code>java.io.tmpdir</code>.
   * 
   * @param handle
   *          the media package identifier
   * @param rootFolder
   *          the media package root folder
   * @return the new media package
   * @throws MediaPackageException
   *           if creation of the new media package fails
   */
  MediaPackage createNew(Handle identifier, File rootFolder) throws MediaPackageException;

  /**
   * Loads a media package from the manifest document.
   * 
   * @param manifest
   *          the media package manifest file
   * @return the media package
   * @throws MediaPackageException
   *           if loading of the media package fails
   */
  MediaPackage loadFromManifest(File manifest) throws MediaPackageException;

  /**
   * Loads a media package from the manifest document.
   * 
   * @param manifest
   *          the media package manifest file
   * @param wrap
   *          <code>true</code> to wrap the media package (ignore checksum errors)
   * @return the media package
   * @throws MediaPackageException
   *           if loading of the media package fails
   */
  MediaPackage loadFromManifest(File manifest, boolean wrap) throws MediaPackageException;

  /**
   * Creates a media package from the given root directory by trying to locate and evaluate the media package manifest.
   * 
   * @param dir
   *          the media package directory
   * @return the new media package
   * @throws IllegalStateException
   *           if the manifest was not found
   * @throws MediaPackageException
   *           if a media package element cannot be added
   */
  MediaPackage loadFromDirectory(File dir) throws MediaPackageException;

  /**
   * Creates a media package from the given root directory by trying to locate and evaluate the media package manifest.
   * In addition to {@link #loadFromDirectory(File)} this method will ignore missing media package elements, allowing
   * for handling of only parts of a media package.
   * <p>
   * Note however, that the media package manifest will be saved to reflect the current media package state.
   * </p>
   * 
   * @param dir
   *          the media package directory
   * @return the new media package
   * @throws IllegalStateException
   *           if the manifest was not found
   * @throws MediaPackageException
   *           if a media package element cannot be added
   */
  MediaPackage rebuildFromDirectory(File dir) throws MediaPackageException;

  /**
   * Creates a media package from the given root directory by trying to locate and evaluate the media package manifest.
   * In addition to {@link #loadFromDirectory(File)} this method will ignore missing media package elements, allowing
   * for handling of only parts of a media package. If <code>ignoreChecksums</code> is set to <code>true</code>, the
   * media package builder will also handle edited media package elements that differ in terms of the checksum.
   * <p>
   * Note however, that the media package manifest will be saved to reflect the current media package state.
   * </p>
   * 
   * @param dir
   *          the media package directory
   * @param ignoreChecksums
   *          <code>true</code> to ignore checksum errors
   * @return the new media package
   * @throws IllegalStateException
   *           if the manifest was not found
   * @throws MediaPackageException
   *           if a media package element cannot be added
   */
  MediaPackage rebuildFromDirectory(File dir, boolean ignoreChecksums) throws MediaPackageException;

  /**
   * Creates a media package from the given root directory by trying to locate and evaluate the media package manifest.
   * In addition to {@link #loadFromDirectory(File)} this method will ignore missing media package elements, allowing
   * for handling of only parts of a media package.
   * <p>
   * If <code>ignoreChecksums</code> is set to <code>true</code>, the media package builder will also handle edited
   * media package elements that differ in terms of the checksum.
   * </p>
   * <p>
   * If <code>verify</code> is set to <code>true</code>, the media package builder will make sure that the media package
   * has a valid identifier.
   * </p>
   * <p>
   * Note however, that the media package manifest will be saved to reflect the current media package state.
   * </p>
   * 
   * @param dir
   *          the media package directory
   * @param ignoreChecksums
   *          <code>true</code> to ignore checksum errors
   * @param verify
   *          <code>true</code> to verify the media package contents
   * @return the new media package
   * @throws IllegalStateException
   *           if the manifest was not found
   * @throws MediaPackageException
   *           if a media package element cannot be added
   */
  MediaPackage rebuildFromDirectory(File dir, boolean ignoreChecksums, boolean verify) throws MediaPackageException;

  /**
   * Loads a media package from the input stream, using the provided packager to decode the stream.
   * 
   * @param packager
   *          the packager
   * @param in
   *          the input stream
   * @return the media package
   * @throws MediaPackageException
   *           if loading of the media package fails
   */
  MediaPackage loadFromPackage(MediaPackagePackager packager, InputStream in) throws IOException, MediaPackageException;

  /**
   * Creates a media package from the elements found in the specified directory. The builder also tries to retreive an
   * identifier for the newly created media package.
   * 
   * @param dir
   *          the directory
   * @param ignoreUnknown
   *          <code>true</code> to ignore unknown or unsupported elements
   * @return the media package
   * @throws MediaPackageException
   *           if creation of the media package fails
   * @throws UnsupportedElementException
   *           if an unsupported file was found
   */
  MediaPackage createFromElements(File dir, boolean ignoreUnknown) throws MediaPackageException,
          UnsupportedElementException;

}