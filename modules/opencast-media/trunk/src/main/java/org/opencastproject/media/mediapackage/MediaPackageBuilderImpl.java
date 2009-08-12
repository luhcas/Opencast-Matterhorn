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
import org.opencastproject.media.mediapackage.handle.HandleBuilder;
import org.opencastproject.media.mediapackage.handle.HandleBuilderFactory;
import org.opencastproject.media.mediapackage.handle.HandleException;
import org.opencastproject.util.FileSupport;
import org.opencastproject.util.IdBuilderFactory;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.UnknownFileTypeException;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

/**
 * This class provides factory methods for the creation of media packages from manifest files, directories or from
 * sratch.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: MediaPackageBuilderImpl.java 2908 2009-07-17 16:51:07Z ced $
 */
public class MediaPackageBuilderImpl implements MediaPackageBuilder {

  /** The directory used to build media packages */
  private static String TMP_DIR_NAME = "mediapackagebuilder";

  /** Temp directory for the creation of media packages */
  private static File TMP_DIR = null;

  /** The handle builder */
  private HandleBuilder handleBuilder = null;

  /**
   * Creates a new media package builder. The builder will try to setup a temporary directory from the java environment
   * and throws Exceptions if this operation fails.
   * 
   * @throws IllegalStateException
   *           if the temporary directory cannot be created or is not accessible
   */
  public MediaPackageBuilderImpl() {
    TMP_DIR = FileSupport.getTempDirectory(TMP_DIR_NAME);
    HandleBuilderFactory builderFactory = HandleBuilderFactory.newInstance();
    handleBuilder = builderFactory.newHandleBuilder();
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackagBuilder#createNew()
   */
  public MediaPackage createNew() throws MediaPackageException {
    String packageRootPath = PathSupport.concat(new String[] { TMP_DIR.getAbsolutePath(),
            IdBuilderFactory.newInstance().newIdBuilder().createNew() });
    return createNew(null, new File(packageRootPath));
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackagBuilder#createNew(org.opencastproject.media.mediapackage.handle.Handle)
   */
  public MediaPackage createNew(Handle identifier) throws MediaPackageException {
    String packageRootPath = PathSupport.concat(new String[] { TMP_DIR.getAbsolutePath(),
            identifier.getNamingAuthority(), identifier.getLocalName() });
    return createNew(identifier, new File(packageRootPath));
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackagBuilder#createNew(org.opencastproject.media.mediapackage.handle.Handle)
   */
  public MediaPackage createNew(Handle identifier, File packageRoot) throws MediaPackageException {
    try {
      if (packageRoot.exists())
        FileSupport.delete(packageRoot, true);
      packageRoot = createMediaPackageFilesystem(packageRoot, identifier);
      return new MediaPackageImpl(packageRoot, identifier);
    } catch (IOException e) {
      throw new MediaPackageException("Media package creation failed: " + e.getMessage());
    } catch (UnknownFileTypeException e) {
      throw new MediaPackageException("File type not supported: " + e.getMessage());
    } catch (NoSuchAlgorithmException e) {
      throw new MediaPackageException("Unsupported digest algorithm: " + e.getMessage());
    }
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackagBuilder#loadFromManifest(File)
   */
  public MediaPackage loadFromManifest(File manifestFile) throws MediaPackageException {
    return loadFromManifest(manifestFile, false);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackagBuilder#loadFromManifest(java.io.File, boolean)
   */
  public MediaPackage loadFromManifest(File manifestFile, boolean wrap) throws MediaPackageException {
    ManifestImpl manifest = null;

    // Look for manifest
    if (!manifestFile.exists() || !manifestFile.canRead())
      throw new MediaPackageException("Media package manifest missing");

    if (!manifestFile.getName().equals(MediaPackageElements.MANIFEST_FILENAME))
      throw new MediaPackageException("Manifest file must be named " + MediaPackageElements.MANIFEST_FILENAME);

    // Read the manifest
    try {
      manifest = ManifestImpl.fromFile(manifestFile, false, wrap, false);
    } catch (NoSuchAlgorithmException e) {
      throw new MediaPackageException("Unsupported digest algorithm: " + e.getMessage());
    } catch (IOException e) {
      throw new MediaPackageException("I/O error while accessing manifest: " + e.getMessage());
    } catch (UnknownFileTypeException e) {
      throw new MediaPackageException("Media package manifest is of unsuported mime type: " + e.getMessage());
    } catch (ParserConfigurationException e) {
      throw new MediaPackageException("Media package manifest cannot be parsed: " + e.getMessage());
    } catch (TransformerException e) {
      throw new MediaPackageException("Error while updating media package manifest: " + e.getMessage());
    } catch (SAXException e) {
      throw new MediaPackageException("Error while parsing media package manifest: " + e.getMessage());
    } catch (XPathExpressionException e) {
      throw new MediaPackageException("Media package manifest cannot be parsed: " + e.getMessage());
    } catch (ConfigurationException e) {
      throw new MediaPackageException("Configuration error while accessing manifest: " + e.getMessage());
    } catch (HandleException e) {
      throw new MediaPackageException("Handle system error while accessing manifest: " + e.getMessage());
    } catch (ParseException e) {
      throw new MediaPackageException("Error while parsing invalid media package start date: " + e.getMessage());
    }

    return new MediaPackageImpl(manifest);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackagBuilder#loadFromDirectory(java.io.File)
   */
  public MediaPackage loadFromDirectory(File dir) throws MediaPackageException {
    ManifestImpl manifest = null;

    // Look for manifest
    File manifestFile = new File(dir, MediaPackageElements.MANIFEST_FILENAME);
    if (!manifestFile.exists() || !manifestFile.canRead()) {
      throw new MediaPackageException("Media package manifest missing in " + dir);
    }

    // Read the manifest
    try {
      manifest = ManifestImpl.fromFile(manifestFile, false, false, false);
    } catch (NoSuchAlgorithmException e) {
      throw new MediaPackageException("Unsupported digest algorithm: " + e.getMessage());
    } catch (IOException e) {
      throw new MediaPackageException("IO Exception while accessing manifest: " + e.getMessage());
    } catch (UnknownFileTypeException e) {
      throw new MediaPackageException("Media package manifest is of unsuported mime type: " + e.getMessage());
    } catch (ParserConfigurationException e) {
      throw new MediaPackageException("Media package manifest cannot be parsed: " + e.getMessage());
    } catch (SAXException e) {
      throw new MediaPackageException("Error while parsing media package manifest: " + e.getMessage());
    } catch (TransformerException e) {
      throw new MediaPackageException("Error while updating media package manifest: " + e.getMessage());
    } catch (XPathExpressionException e) {
      throw new MediaPackageException("Error while parsing media package manifest: " + e.getMessage());
    } catch (ConfigurationException e) {
      throw new MediaPackageException("Error while parsing media package manifest: " + e.getMessage());
    } catch (HandleException e) {
      throw new MediaPackageException("Error while parsing media package manifest: " + e.getMessage());
    } catch (ParseException e) {
      throw new MediaPackageException("Error while parsing media package start date: " + e.getMessage());
    }

    return new MediaPackageImpl(manifest);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackagBuilder#rebuildFromDirectory(java.io.File)
   */
  public MediaPackage rebuildFromDirectory(File dir) throws MediaPackageException {
    return rebuildFromDirectory(dir, false);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackagBuilder#rebuildFromDirectory(java.io.File, boolean)
   */
  public MediaPackage rebuildFromDirectory(File dir, boolean ignoreChecksums) throws MediaPackageException {
    return rebuildFromDirectory(dir, ignoreChecksums, false);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackagBuilder#rebuildFromDirectory(java.io.File, boolean, boolean)
   */
  public MediaPackage rebuildFromDirectory(File dir, boolean ignoreChecksums, boolean verify)
          throws MediaPackageException {
    ManifestImpl manifest = null;

    // Look for manifest
    File manifestFile = new File(dir, MediaPackageElements.MANIFEST_FILENAME);
    if (!manifestFile.exists() || !manifestFile.canRead()) {
      throw new MediaPackageException("Media package manifest missing in " + dir);
    }

    // Read the manifest
    try {
      manifest = ManifestImpl.fromFile(manifestFile, true, ignoreChecksums, verify);
    } catch (NoSuchAlgorithmException e) {
      throw new MediaPackageException("Unsupported digest algorithm: " + e.getMessage());
    } catch (IOException e) {
      throw new MediaPackageException("IO Exception while accessing manifest: " + e.getMessage());
    } catch (UnknownFileTypeException e) {
      throw new MediaPackageException("Media package manifest is of unsuported mime type: " + e.getMessage());
    } catch (ParserConfigurationException e) {
      throw new MediaPackageException("Media package manifest cannot be parsed: " + e.getMessage());
    } catch (SAXException e) {
      throw new MediaPackageException("Error while parsing media package manifest: " + e.getMessage());
    } catch (TransformerException e) {
      throw new MediaPackageException("Error while updating media package manifest: " + e.getMessage());
    } catch (XPathExpressionException e) {
      throw new MediaPackageException("Error while parsing media package manifest: " + e.getMessage());
    } catch (ConfigurationException e) {
      throw new MediaPackageException("Error while parsing media package manifest: " + e.getMessage());
    } catch (HandleException e) {
      throw new MediaPackageException("Error while parsing media package manifest: " + e.getMessage());
    } catch (ParseException e) {
      throw new MediaPackageException("Error while parsing media package start date: " + e.getMessage());
    }

    return new MediaPackageImpl(manifest);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackagBuilder#createFromElements(File, boolean)
   */
  public MediaPackage createFromElements(File dir, boolean ignoreUnknown) throws MediaPackageException,
          UnsupportedElementException {
    Handle identifier = null;

    // Create handle
    try {
      identifier = handleBuilder.createNew();
    } catch (HandleException e) {
      throw new MediaPackageException("Unable to retreive new handle: " + e.getMessage());
    }

    // Create media package root
    File packageRoot = null;
    MediaPackageImpl mediaPackage = null;
    File manifest = null;
    try {
      packageRoot = createMediaPackageFilesystem(dir, identifier);
      mediaPackage = new MediaPackageImpl(packageRoot, identifier);
      manifest = mediaPackage.getManifestFile();

      // Create a media package element builder
      MediaPackageElementBuilderFactory elementBuilderFactory = MediaPackageElementBuilderFactory.newInstance();
      MediaPackageElementBuilder elementBuilder = elementBuilderFactory.newElementBuilder();

      // Look for elements and add them to the media package
      Stack<File> files = new Stack<File>();
      for (File f : packageRoot.listFiles())
        files.push(f);
      while (!files.empty()) {
        File f = files.pop();

        // Process directories
        if (f.isDirectory()) {
          for (File f2 : f.listFiles())
            files.push(f2);
          continue;
        }

        // Ignore manifest
        else if (f.getAbsolutePath().equals(manifest.getAbsolutePath()))
          continue;

        // This might be an element
        MediaPackageElement element = null;
        try {
          element = elementBuilder.elementFromFile(f);
          mediaPackage.add(element);
        } catch (MediaPackageException e) {
          if (!ignoreUnknown)
            throw new UnsupportedElementException(e.getMessage());
          else
            continue;
        } catch (UnsupportedElementException e) {
          if (!ignoreUnknown)
            throw e;
        }
      }
      // Save the (recreated) manifest
      mediaPackage.save();
      return mediaPackage;
    } catch (IOException e) {
      throw new MediaPackageException("I/O error while reading media package elements: " + e.getMessage());
    } catch (UnknownFileTypeException e) {
      throw new MediaPackageException("Unsupported filetype: " + e.getMessage());
    } catch (NoSuchAlgorithmException e) {
      throw new MediaPackageException("Unsupported digest algorithm: " + e.getMessage());
    }
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackagBuilder#loadFromPackage(org.opencastproject.media.mediapackage.MediaPackagePackager,
   *      java.io.InputStream)
   */
  public MediaPackage loadFromPackage(MediaPackagePackager packager, InputStream in) throws IOException,
          MediaPackageException {
    if (packager == null)
      throw new IllegalArgumentException("The packager must not be null");
    if (in == null)
      throw new IllegalArgumentException("The input stream must not be null");
    return packager.unpack(in);
  }

  /**
   * Creates a new media package by creating a directory within <code>TMP_DIR</code> and an emtpy manifest file.
   * 
   * @param packageRoot
   *          the base dir
   * @param identifier
   *          the media package identifier
   * @throws MediaPackageException
   *           if the media package cannot be created
   */
  private File createMediaPackageFilesystem(File packageRoot, Handle identifier) throws MediaPackageException {
    File manifestFile = new File(packageRoot, MediaPackageElements.MANIFEST_FILENAME);
    try {
      // Create media package root dir if not existing
      if (!packageRoot.exists())
        packageRoot.mkdirs();
      if (!packageRoot.exists())
        throw new MediaPackageException("Unable to create media package directory " + packageRoot.getAbsolutePath());
      // Always create a new manifest
      if (manifestFile.exists() && !manifestFile.delete()) {
        throw new MediaPackageException("Unable to delete existing manifest on media package filesystem recreation");
      }
      manifestFile.createNewFile();
      ManifestImpl.newInstance(packageRoot, identifier);
      return packageRoot;
    } catch (IOException e) {
      throw new MediaPackageException("Unable to create media package " + identifier + ": " + e.getMessage());
    } catch (UnknownFileTypeException e) {
      throw new MediaPackageException("Unable to create media package " + identifier + ": " + e.getMessage());
    } catch (ParserConfigurationException e) {
      throw new MediaPackageException("Unable to create media package " + identifier + ": " + e.getMessage());
    } catch (TransformerException e) {
      throw new MediaPackageException("Unable to create media package " + identifier + ": " + e.getMessage());
    } catch (NoSuchAlgorithmException e) {
      throw new MediaPackageException("Error creating media package " + identifier + ": " + e.getMessage());
    }
  }

}