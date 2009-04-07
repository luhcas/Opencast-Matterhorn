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
import org.opencastproject.media.bundle.handle.HandleBuilder;
import org.opencastproject.media.bundle.handle.HandleBuilderFactory;
import org.opencastproject.util.FileSupport;

import org.junit.After;
import org.junit.Before;

import java.io.File;

/**
 * Base class for bundle tests.
 * 
 * @author Tobias Wunden
 * @version $Id
 */

public abstract class AbstractBundleTest {

  /** tmp directory */
  protected File tmpDir = null;

  /** The bundle used to test */
  protected Bundle bundle = null;

  /** The bundle builder */
  protected BundleBuilder bundleBuilder = null;

  /** The handle builder */
  protected HandleBuilder handleBuilder = null;

  /** The bundle identifier */
  protected Handle identifier = null;

  /** The test bundle's root directory */
  protected File bundleDir = null;

  /** The test bundle's metadata directory */
  protected File metadataDir = null;

  /** The test bundle's track directory */
  protected File trackDir = null;

  /** The test bundle's attachment directory */
  protected File attachmentDir = null;

  /** The bundle manifest */
  protected File manifestFile = null;

  /** The dublin core catalog */
  protected File dcFile = null;

  /** The series dublin core catalog */
  protected File dcSeriesFile = null;

  /** The mpeg-7 catalog */
  protected File mpeg7File = null;

  /** The cover */
  protected File coverFile = null;

  /** The vidoe track */
  protected File videoFile = null;

  /** The audio track */
  protected File audioFile = null;

  /** The zip file */
  protected File archiveFile = null;

  /** The unzipped bundle */
  protected Bundle unzippedBundle = null;

  /**
   * Creates everything that is needed to test a bundle.
   * 
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {

    // Create a bundle builder
    bundleBuilder = BundleBuilderFactory.newInstance().newBundleBuilder();

    // Create a handle builder
    handleBuilder = HandleBuilderFactory.newInstance().newHandleBuilder();

    identifier = handleBuilder.createNew();

    // Get hold of the tmp directory
    tmpDir = FileSupport.getTempDirectory();

    // Create the bundle directory
    bundleDir = new File(tmpDir, Long.toString(System.currentTimeMillis()));

    // Create subdirectories
    trackDir = new File(bundleDir, "tracks");
    trackDir.mkdirs();
    metadataDir = new File(bundleDir, "metadata");
    metadataDir.mkdirs();
    attachmentDir = new File(bundleDir, "attachments");
    attachmentDir.mkdirs();

    // Setup test files
    File manifestTestFile = new File(this.getClass().getResource(
        "/manifest.xml").getPath());
    File videoTestFile = new File(this.getClass().getResource("/vonly.mov")
        .getPath());
    File audioTestFile = new File(this.getClass().getResource("/aonly.mov")
        .getPath());
    File dcTestFile = new File(this.getClass().getResource("/dublincore.xml")
        .getPath());
    File dcSeriesTestFile = new File(this.getClass().getResource(
        "/series-dublincore.xml").getPath());
    File mpeg7TestFile = new File(this.getClass().getResource("/mpeg-7.xml")
        .getPath());
    File coverTestFile = new File(this.getClass().getResource("/cover.png")
        .getPath());

    // Copy files into place
    manifestFile = FileSupport.copy(manifestTestFile, new File(bundleDir,
        Manifest.FILENAME));
    videoFile = FileSupport.copy(videoTestFile, trackDir);
    audioFile = FileSupport.copy(audioTestFile, trackDir);
    dcFile = FileSupport.copy(dcTestFile, metadataDir);
    dcSeriesFile = FileSupport.copy(dcSeriesTestFile, metadataDir);
    mpeg7File = FileSupport.copy(mpeg7TestFile, metadataDir);
    coverFile = FileSupport.copy(coverTestFile, attachmentDir);

    // Create a bundle
    bundle = bundleBuilder.loadFromDirectory(bundleDir);
  }

  /**
   * Cleans up after every test method.
   * 
   * @throws Exception
   */
  @After
  public void tearDown() throws Exception {
    if (bundleDir.getParentFile().getName().equals(
        identifier.getNamingAuthority()))
      FileSupport.delete(bundleDir.getParentFile(), true);
    else
      FileSupport.delete(bundleDir, true);
  }

}