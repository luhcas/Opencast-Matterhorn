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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilder;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.media.mediapackage.MediaPackageSupport;
import org.opencastproject.media.mediapackage.MediaPackageElements;
import org.opencastproject.media.mediapackage.MediaPackageSupport.MergeMode;
import org.opencastproject.util.FileSupport;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Test case to make sure media package support works as expected.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: MediaPackageMergeTest.java 2905 2009-07-15 16:16:05Z ced $
 */
public class MediaPackageMergeTest {

  /** tmp directory */
  protected File tmpDir = null;

  /** The media package builder */
  protected MediaPackageBuilder mediaPackageBuilder = null;

  /** The source media package directory for merge tests */
  File sourceDir = null;

  /** The target media package directory for merge tests */
  File targetDir = null;

  @Before
  public void setUp() throws Exception {

    // Get hold of the tmp directory
    tmpDir = FileSupport.getTempDirectory();

    // Create a media package builder
    mediaPackageBuilder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();

    // Create source and target media package
    setUpSourceMediaPackage();
    setUpTargeMediaPackage();
  }

  /**
   * Creates the source media package.
   * 
   * @throws IOException
   * @throws MediaPackageException
   */
  private void setUpSourceMediaPackage() throws IOException, MediaPackageException {
    // Create the media package directory
    File packageDir = new File(tmpDir, Long.toString(System.currentTimeMillis()));

    // Create subdirectories
    File trackDir = new File(packageDir, "tracks");
    trackDir.mkdirs();
    File metadataDir = new File(packageDir, "metadata");
    metadataDir.mkdirs();
    File attachmentDir = new File(packageDir, "attachments");
    attachmentDir.mkdirs();

    // Setup test files
    File manifestTestFile = new File(MediaPackageBuilderTest.class.getResource("/source-manifest.xml").getPath());
    File videoTestFile = new File(MediaPackageBuilderTest.class.getResource("/vonly.mov").getPath());
    File audioTestFile = new File(MediaPackageBuilderTest.class.getResource("/aonly.mov").getPath());
    File dcTestFile = new File(MediaPackageBuilderTest.class.getResource("/dublincore.xml").getPath());
    File dcSeriesTestFile = new File(MediaPackageBuilderTest.class.getResource("/series-dublincore.xml").getPath());
    File mpeg7TestFile = new File(MediaPackageBuilderTest.class.getResource("/mpeg-7.xml").getPath());
    File coverTestFile = new File(MediaPackageBuilderTest.class.getResource("/cover.png").getPath());

    // Copy files into place
    FileSupport.copy(manifestTestFile, new File(packageDir, MediaPackageElements.MANIFEST_FILENAME));
    FileSupport.copy(videoTestFile, trackDir);
    FileSupport.copy(audioTestFile, trackDir);
    FileSupport.copy(dcTestFile, metadataDir);
    FileSupport.copy(dcSeriesTestFile, metadataDir);
    FileSupport.copy(dcSeriesTestFile, new File(metadataDir, "series-dublincore-new.xml"));
    FileSupport.copy(mpeg7TestFile, metadataDir);
    FileSupport.copy(coverTestFile, attachmentDir);
    FileSupport.copy(coverTestFile, new File(attachmentDir, "series-cover.png"));

    // Test the media package
    MediaPackage mediaPackage = mediaPackageBuilder.loadFromDirectory(packageDir);
    mediaPackage.verify();

    // Set the media package directory
    sourceDir = packageDir;
  }

  /**
   * Creates the target media package.
   * 
   * @throws IOException
   * @throws MediaPackageException
   */
  private void setUpTargeMediaPackage() throws IOException, MediaPackageException {
    // Create the media package directory
    File packageDir = new File(tmpDir, Long.toString(System.currentTimeMillis()));

    // Create subdirectories
    File trackDir = new File(packageDir, "tracks");
    trackDir.mkdirs();
    File metadataDir = new File(packageDir, "metadata");
    metadataDir.mkdirs();
    File attachmentDir = new File(packageDir, "attachments");
    attachmentDir.mkdirs();

    // Setup test files
    File manifestTestFile = new File(MediaPackageBuilderTest.class.getResource("/target-manifest.xml").getPath());
    File videoTestFile = new File(MediaPackageBuilderTest.class.getResource("/vonly.mov").getPath());
    File audioTestFile = new File(MediaPackageBuilderTest.class.getResource("/aonly.mov").getPath());
    File dcTestFile = new File(MediaPackageBuilderTest.class.getResource("/dublincore.xml").getPath());
    File dcSeriesTestFile = new File(MediaPackageBuilderTest.class.getResource("/series-dublincore.xml").getPath());
    File mpeg7TestFile = new File(MediaPackageBuilderTest.class.getResource("/mpeg-7.xml").getPath());
    File coverTestFile = new File(MediaPackageBuilderTest.class.getResource("/cover.png").getPath());

    // Copy files into place
    FileSupport.copy(manifestTestFile, new File(packageDir, MediaPackageElements.MANIFEST_FILENAME));
    FileSupport.copy(videoTestFile, trackDir);
    FileSupport.copy(audioTestFile, trackDir);
    FileSupport.copy(audioTestFile, new File(trackDir, "aonly2.mov"));
    FileSupport.copy(dcTestFile, metadataDir);
    FileSupport.copy(dcSeriesTestFile, metadataDir);
    FileSupport.copy(mpeg7TestFile, metadataDir);
    FileSupport.copy(coverTestFile, attachmentDir);

    // Test the media package
    mediaPackageBuilder.loadFromDirectory(packageDir);
    MediaPackage mediaPackage = mediaPackageBuilder.loadFromDirectory(packageDir);
    mediaPackage.verify();

    // Set the media package directory
    targetDir = packageDir;
  }

  /**
   * Checks the media package for duplicate identifier and files.
   * 
   * @param mediaPackage
   *          the media package to test
   * @throws MediaPackageException
   */
  private void testMediaPackageConsistency(MediaPackage mediaPackage) throws MediaPackageException {
    List<String> ids = new ArrayList<String>();
    List<File> files = new ArrayList<File>();
    for (MediaPackageElement e : mediaPackage.elements()) {
      if (ids.contains(e.getIdentifier()))
        throw new MediaPackageException("Duplicate id " + e.getIdentifier() + "' found");
      ids.add(e.getIdentifier());
      if (files.contains(e.getFile()))
        throw new MediaPackageException("Duplicate filename " + e.getFile() + "' found");
      files.add(e.getFile());
    }
  }

  @Test
  public void testMergeByMerging() {
    try {
      MediaPackage mediaPackage = MediaPackageSupport.merge(targetDir, sourceDir, true, MergeMode.Merge);
      assertTrue(mediaPackage.getTracks().length == 5);
      assertTrue(mediaPackage.getCatalogs().length == 6);
      assertTrue(mediaPackage.getAttachments().length == 3);
      testMediaPackageConsistency(mediaPackage);
    } catch (MediaPackageException e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testMergeByReplacing() {
    try {
      MediaPackage mediaPackage = mediaPackageBuilder.loadFromDirectory(targetDir);

      // Should be replaced
      MediaPackageElement track1 = mediaPackage.getElementById("track-1");
      MediaPackageElement catalog1 = mediaPackage.getElementById("catalog-1");
      MediaPackageElement catalog2 = mediaPackage.getElementById("catalog-2");
      MediaPackageElement cover = mediaPackage.getElementById("cover");

      // Should remain untouched
      MediaPackageElement track2 = mediaPackage.getElementById("track-2");
      MediaPackageElement track4 = mediaPackage.getElementById("track-4");
      MediaPackageElement catalog3 = mediaPackage.getElementById("catalog-3");

      // Merge the media package
      mediaPackage = MediaPackageSupport.merge(targetDir, sourceDir, true, MergeMode.Replace);

      // Test number of elements
      assertEquals(mediaPackage.getTracks().length, 4);
      assertEquals(mediaPackage.getCatalogs().length, 4);
      assertEquals(mediaPackage.getAttachments().length, 2);

      // Test for replaced elements
      assertNotSame(track1, mediaPackage.getElementById("track-1"));
      assertNotSame(catalog1, mediaPackage.getElementById("catalog-1"));
      assertNotSame(catalog2, mediaPackage.getElementById("catalog-2"));
      assertNotSame(cover, mediaPackage.getElementById("cover"));

      // Test for untouched elements
      assertEquals(track2, mediaPackage.getElementById("track-2"));
      assertEquals(track4, mediaPackage.getElementById("track-4"));
      assertEquals(catalog3, mediaPackage.getElementById("catalog-3"));

      testMediaPackageConsistency(mediaPackage);
    } catch (MediaPackageException e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testMergeBySkipping() {
    try {
      MediaPackage targetMediaPackage = mediaPackageBuilder.loadFromDirectory(targetDir);

      // Should remain untouched
      MediaPackageElement track1 = targetMediaPackage.getElementById("track-1");
      MediaPackageElement catalog1 = targetMediaPackage.getElementById("catalog-1");
      MediaPackageElement catalog2 = targetMediaPackage.getElementById("catalog-2");
      MediaPackageElement cover = targetMediaPackage.getElementById("cover");

      // Merge the media package
      MediaPackage mediaPackage = MediaPackageSupport.merge(targetDir, sourceDir, true, MergeMode.Skip);

      // Test number of elements
      assertEquals(mediaPackage.getTracks().length, 4);
      assertEquals(mediaPackage.getCatalogs().length, 4);
      assertEquals(mediaPackage.getAttachments().length, 2);

      // Test for untouched elements
      assertEquals(track1, mediaPackage.getElementById("track-1"));
      assertEquals(catalog1, mediaPackage.getElementById("catalog-1"));
      assertEquals(catalog2, mediaPackage.getElementById("catalog-2"));
      assertEquals(cover, mediaPackage.getElementById("cover"));

      testMediaPackageConsistency(mediaPackage);
    } catch (MediaPackageException e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testMergeByFailing() {
    try {
      MediaPackageSupport.merge(targetDir, sourceDir, true, MergeMode.Fail);
      fail("Merging should have failed but didn't");
    } catch (MediaPackageException e) {
      // This is excpected
    }
  }

}