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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilder;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.media.mediapackage.JarPackager;
import org.opencastproject.media.mediapackage.ZipPackager;
import org.opencastproject.util.FileSupport;

import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Test case for media package packaging.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: MediaPackagePackagerTest.java 1749 2008-12-19 12:55:39Z wunden $
 */
public class MediaPackagePackagerTest extends AbstractMediaPackageTest {

  /** The zip file */
  File archiveFile = null;

  /** The unzipped media package */
  MediaPackage unpackedMediaPackage = null;

  @After
  public void tearDown() throws Exception {
    super.tearDown();
    if (archiveFile != null)
      FileSupport.delete(archiveFile);
    if (unpackedMediaPackage != null)
      FileSupport.delete(unpackedMediaPackage.getRoot(), true);
  }

  /**
   * Test method that will package a media package to a <t>zip</t> file and then try to read it back in.
   */
  @Test
  public void testZip() {
    try {
      archiveFile = File.createTempFile("mediapackage-", ".zip");
      FileOutputStream fos = new FileOutputStream(archiveFile);
      mediaPackage.pack(new ZipPackager(), fos);
    } catch (FileNotFoundException e) {
      fail("File " + archiveFile + " could not be found");
    } catch (IOException e) {
      fail("Error while accessing " + archiveFile + ": " + e.getMessage());
    } catch (MediaPackageException e) {
      fail("Error packing the media package: " + e.getMessage());
    }

    assertTrue(archiveFile.exists() && archiveFile.isFile());
    FileSupport.delete(mediaPackage.getRoot(), true);

    // And finally, build the media package
    MediaPackageBuilderFactory builderFactory = MediaPackageBuilderFactory.newInstance();
    MediaPackageBuilder builder = builderFactory.newMediaPackageBuilder();
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(archiveFile);
      unpackedMediaPackage = builder.loadFromPackage(new ZipPackager(), fis);
    } catch (FileNotFoundException e) {
      fail("Error accessing the zipped media package: " + e.getMessage());
    } catch (IOException e) {
      fail("I/O error accessing the zipped media package: " + e.getMessage());
    } catch (MediaPackageException e) {
      fail("Error unzipping the media package package: " + e.getMessage());
    }

    assertTrue(unpackedMediaPackage.getTracks().length == 2);
    assertTrue(unpackedMediaPackage.getCatalogs().length == 3);
    assertTrue(unpackedMediaPackage.getAttachments().length == 1);
  }

  /**
   * Test method that will package a media package to a <t>jar</t> file and then try to read it back in.
   */
  @Test
  public void testJar() {
    try {
      archiveFile = File.createTempFile("mediapackage-", ".jar");
      FileOutputStream fos = new FileOutputStream(archiveFile);
      mediaPackage.pack(new JarPackager(), fos);
    } catch (FileNotFoundException e) {
      fail("File " + archiveFile + " could not be found");
    } catch (IOException e) {
      fail("Error while accessing " + archiveFile + ": " + e.getMessage());
    } catch (MediaPackageException e) {
      fail("Error packing the media package: " + e.getMessage());
    }

    assertTrue(archiveFile.exists() && archiveFile.isFile());
    FileSupport.delete(mediaPackage.getRoot(), true);

    // And finally, build the media package
    MediaPackageBuilderFactory builderFactory = MediaPackageBuilderFactory.newInstance();
    MediaPackageBuilder builder = builderFactory.newMediaPackageBuilder();
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(archiveFile);
      unpackedMediaPackage = builder.loadFromPackage(new JarPackager(), fis);
    } catch (FileNotFoundException e) {
      fail("Error accessing the packed media package: " + e.getMessage());
    } catch (IOException e) {
      fail("I/O error accessing the packed media package: " + e.getMessage());
    } catch (MediaPackageException e) {
      fail("Error unzipping the media package: " + e.getMessage());
    }

    assertTrue(unpackedMediaPackage.getTracks().length == 2);
    assertTrue(unpackedMediaPackage.getCatalogs().length == 3);
    assertTrue(unpackedMediaPackage.getAttachments().length == 1);
  }

}