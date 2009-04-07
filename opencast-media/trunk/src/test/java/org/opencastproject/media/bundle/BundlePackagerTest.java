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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.opencastproject.util.FileSupport;

import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Test case for bundle packaging.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public class BundlePackagerTest extends AbstractBundleTest {

  /** The zip file */
  File archiveFile = null;

  /** The unzipped bundle */
  Bundle unpackedBundle = null;

  @Override
  @After
  public void tearDown() throws Exception {
    super.tearDown();
    if (archiveFile != null)
      FileSupport.delete(archiveFile);
    if (unzippedBundle != null)
      FileSupport.delete(unzippedBundle.getRoot(), true);
  }

  /**
   * Test method that will package a bundle to a <t>zip</t> file and then try to
   * read it back in.
   */
  @Test
  public void testZip() {
    try {
      archiveFile = File.createTempFile("bundle-", ".zip");
      FileOutputStream fos = new FileOutputStream(archiveFile);
      bundle.pack(new ZipPackager(), fos);
    } catch (FileNotFoundException e) {
      fail("File " + archiveFile + " could not be found");
    } catch (IOException e) {
      fail("Error while accessing " + archiveFile + ": " + e.getMessage());
    } catch (BundleException e) {
      fail("Error packing the bundle: " + e.getMessage());
    }

    assertTrue(archiveFile.exists() && archiveFile.isFile());
    FileSupport.delete(bundle.getRoot(), true);

    // And finally, build the bundle
    BundleBuilderFactory builderFactory = BundleBuilderFactory.newInstance();
    BundleBuilder builder = builderFactory.newBundleBuilder();
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(archiveFile);
      unpackedBundle = builder.loadFromPackage(new ZipPackager(), fis);
    } catch (FileNotFoundException e) {
      fail("Error accessing the zipped bundle: " + e.getMessage());
    } catch (IOException e) {
      fail("I/O error accessing the zipped bundle: " + e.getMessage());
    } catch (BundleException e) {
      fail("Error unzipping the bundle package: " + e.getMessage());
    }

    assertTrue(unpackedBundle.getTracks().length == 2);
    assertTrue(unpackedBundle.getCatalogs().length == 3);
    assertTrue(unpackedBundle.getAttachments().length == 1);
  }

  /**
   * Test method that will package a bundle to a <t>jar</t> file and then try to
   * read it back in.
   */
  @Test
  public void testJar() {
    try {
      archiveFile = File.createTempFile("bundle-", ".jar");
      FileOutputStream fos = new FileOutputStream(archiveFile);
      bundle.pack(new JarPackager(), fos);
    } catch (FileNotFoundException e) {
      fail("File " + archiveFile + " could not be found");
    } catch (IOException e) {
      fail("Error while accessing " + archiveFile + ": " + e.getMessage());
    } catch (BundleException e) {
      fail("Error packing the bundle: " + e.getMessage());
    }

    assertTrue(archiveFile.exists() && archiveFile.isFile());
    FileSupport.delete(bundle.getRoot(), true);

    // And finally, build the bundle
    BundleBuilderFactory builderFactory = BundleBuilderFactory.newInstance();
    BundleBuilder builder = builderFactory.newBundleBuilder();
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(archiveFile);
      unpackedBundle = builder.loadFromPackage(new JarPackager(), fis);
    } catch (FileNotFoundException e) {
      fail("Error accessing the packed bundle: " + e.getMessage());
    } catch (IOException e) {
      fail("I/O error accessing the packed bundle: " + e.getMessage());
    } catch (BundleException e) {
      fail("Error unzipping the bundle package: " + e.getMessage());
    }

    assertTrue(unpackedBundle.getTracks().length == 2);
    assertTrue(unpackedBundle.getCatalogs().length == 3);
    assertTrue(unpackedBundle.getAttachments().length == 1);
  }

}