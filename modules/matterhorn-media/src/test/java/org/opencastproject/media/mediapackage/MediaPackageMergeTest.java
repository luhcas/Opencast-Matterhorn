/**
 *  Copyright 2009, 2010 The Regents of the University of California
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

import org.opencastproject.media.mediapackage.MediaPackageSupport.MergeMode;
import org.opencastproject.util.FileSupport;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Test case to make sure media package support works as expected.
 */
public class MediaPackageMergeTest {

  /** tmp directory */
  protected File tmpDir = null;

  /** The media package builder */
  protected MediaPackageBuilder mediaPackageBuilder = null;

  /** The source media package directory for merge tests */
  MediaPackage sourcePackage = null;

  /** The target media package directory for merge tests */
  MediaPackage targetPackage = null;

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
    File sourceManifestFile = new File(MediaPackageBuilderTest.class.getResource("/source-manifest.xml").getPath());
    File videoTestFile = new File(MediaPackageBuilderTest.class.getResource("/vonly.mov").getPath());
    File audioTestFile = new File(MediaPackageBuilderTest.class.getResource("/aonly.mov").getPath());
    File dcTestFile = new File(MediaPackageBuilderTest.class.getResource("/dublincore.xml").getPath());
    File dcSeriesTestFile = new File(MediaPackageBuilderTest.class.getResource("/series-dublincore.xml").getPath());
    File mpeg7TestFile = new File(MediaPackageBuilderTest.class.getResource("/mpeg-7.xml").getPath());
    File coverTestFile = new File(MediaPackageBuilderTest.class.getResource("/cover.png").getPath());

    // Copy files into place
    File manifestFile = new File(packageDir, MediaPackageElements.MANIFEST_FILENAME);
    FileSupport.copy(sourceManifestFile, manifestFile);
    FileSupport.copy(videoTestFile, trackDir);
    FileSupport.copy(audioTestFile, trackDir);
    FileSupport.copy(dcTestFile, metadataDir);
    FileSupport.copy(dcSeriesTestFile, metadataDir);
    FileSupport.copy(dcSeriesTestFile, new File(metadataDir, "series-dublincore-new.xml"));
    FileSupport.copy(mpeg7TestFile, metadataDir);
    FileSupport.copy(coverTestFile, attachmentDir);
    FileSupport.copy(coverTestFile, new File(attachmentDir, "series-cover.png"));

    // Test the media package
    mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(manifestFile.getParentFile()));
    sourcePackage = mediaPackageBuilder.loadFromXml(new FileInputStream(manifestFile));
    sourcePackage.verify();
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
    File sourceManifestFile = new File(MediaPackageBuilderTest.class.getResource("/target-manifest.xml").getPath());
    File videoTestFile = new File(MediaPackageBuilderTest.class.getResource("/vonly.mov").getPath());
    File audioTestFile = new File(MediaPackageBuilderTest.class.getResource("/aonly.mov").getPath());
    File dcTestFile = new File(MediaPackageBuilderTest.class.getResource("/dublincore.xml").getPath());
    File dcSeriesTestFile = new File(MediaPackageBuilderTest.class.getResource("/series-dublincore.xml").getPath());
    File mpeg7TestFile = new File(MediaPackageBuilderTest.class.getResource("/mpeg-7.xml").getPath());
    File coverTestFile = new File(MediaPackageBuilderTest.class.getResource("/cover.png").getPath());

    // Copy files into place
    File manifestFile = new File(packageDir, MediaPackageElements.MANIFEST_FILENAME);
    FileSupport.copy(sourceManifestFile, manifestFile);
    FileSupport.copy(videoTestFile, trackDir);
    FileSupport.copy(audioTestFile, trackDir);
    FileSupport.copy(audioTestFile, new File(trackDir, "aonly2.mov"));
    FileSupport.copy(dcTestFile, metadataDir);
    FileSupport.copy(dcSeriesTestFile, metadataDir);
    FileSupport.copy(mpeg7TestFile, metadataDir);
    FileSupport.copy(coverTestFile, attachmentDir);

    // Test the media package
    mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(manifestFile.getParentFile()));
    targetPackage = mediaPackageBuilder.loadFromXml(new FileInputStream(manifestFile));
    targetPackage.verify();
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
    List<URI> files = new ArrayList<URI>();
    for (MediaPackageElement e : mediaPackage.elements()) {
      if (ids.contains(e.getIdentifier()))
        throw new MediaPackageException("Duplicate id " + e.getIdentifier() + "' found");
      ids.add(e.getIdentifier());
      if (files.contains(e.getURI()))
        throw new MediaPackageException("Duplicate filename " + e.getURI() + "' found");
      files.add(e.getURI());
    }
  }

  @Test
  public void testMergeByMerging() {
    try {
      MediaPackage mediaPackage = MediaPackageSupport.merge(targetPackage, sourcePackage, MergeMode.Merge);
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
      // Should be replaced
      MediaPackageElement track1 = targetPackage.getElementById("track-1");
      MediaPackageElement catalog1 = targetPackage.getElementById("catalog-1");
      MediaPackageElement catalog2 = targetPackage.getElementById("catalog-2");
      MediaPackageElement cover = targetPackage.getElementById("cover");

      // Should remain untouched
      MediaPackageElement track2 = targetPackage.getElementById("track-2");
      MediaPackageElement track4 = targetPackage.getElementById("track-4");
      MediaPackageElement catalog3 = targetPackage.getElementById("catalog-3");

      // Merge the media package
      MediaPackage mergedPackage = MediaPackageSupport.merge(targetPackage, sourcePackage, MergeMode.Replace);

      // Test number of elements
      assertEquals(mergedPackage.getTracks().length, 4);
      assertEquals(mergedPackage.getCatalogs().length, 4);
      assertEquals(mergedPackage.getAttachments().length, 2);

      // Test for replaced elements
      assertNotSame(track1, mergedPackage.getElementById("track-1"));
      assertNotSame(catalog1, mergedPackage.getElementById("catalog-1"));
      assertNotSame(catalog2, mergedPackage.getElementById("catalog-2"));
      assertNotSame(cover, mergedPackage.getElementById("cover"));

      // Test for untouched elements
      assertEquals(track2, mergedPackage.getElementById("track-2"));
      assertEquals(track4, mergedPackage.getElementById("track-4"));
      assertEquals(catalog3, mergedPackage.getElementById("catalog-3"));

      testMediaPackageConsistency(mergedPackage);
    } catch (MediaPackageException e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testMergeBySkipping() {
    try {
      // Should remain untouched
      MediaPackageElement track1 = targetPackage.getElementById("track-1");
      MediaPackageElement catalog1 = targetPackage.getElementById("catalog-1");
      MediaPackageElement catalog2 = targetPackage.getElementById("catalog-2");
      MediaPackageElement cover = targetPackage.getElementById("cover");

      // Merge the media package
      MediaPackage mergedPackage = MediaPackageSupport.merge(targetPackage, sourcePackage, MergeMode.Skip);

      // Test number of elements
      assertEquals(mergedPackage.getTracks().length, 4);
      assertEquals(mergedPackage.getCatalogs().length, 4);
      assertEquals(mergedPackage.getAttachments().length, 2);

      // Test for untouched elements
      assertEquals(track1, mergedPackage.getElementById("track-1"));
      assertEquals(catalog1, mergedPackage.getElementById("catalog-1"));
      assertEquals(catalog2, mergedPackage.getElementById("catalog-2"));
      assertEquals(cover, mergedPackage.getElementById("cover"));

      testMediaPackageConsistency(mergedPackage);
    } catch (MediaPackageException e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testMergeByFailing() {
    try {
      MediaPackageSupport.merge(targetPackage, sourcePackage, MergeMode.Fail);
      fail("Merging should have failed but didn't");
    } catch (MediaPackageException e) {
      // This is excpected
    }
  }

}
