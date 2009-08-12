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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.opencastproject.media.mediapackage.attachment.CoverImpl;
import org.opencastproject.media.mediapackage.dublincore.DublinCoreCatalogImpl;
import org.opencastproject.util.FileSupport;
import org.opencastproject.util.UnknownFileTypeException;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Test case for the media package management.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: MediaPackageTest.java 2905 2009-07-15 16:16:05Z ced $
 */
public class MediaPackageTest extends AbstractMediaPackageTest {

  /**
   * Test method for
   * {@link org.opencastproject.media.mediapackage.MediaPackageImpl#add(org.opencastproject.media.mediapackage.Catalog)}
   * .
   */
  @Test
  public void testAddAndRemoveCatalog() {
    DublinCoreCatalog catalog = null;
    try {
      int numCatalogs = mediaPackage.getCatalogs().length;

      // Create and add catalog
      catalog = DublinCoreCatalogImpl.newInstance();
      mediaPackage.add(catalog);

      // Check object consistency
      assertTrue(mediaPackage.getCatalogs().length == numCatalogs + 1);
      Catalog[] catalogsInMediaPackage = mediaPackage.getCatalogs(MediaPackageElements.DUBLINCORE_CATALOG);
      assertTrue(Arrays.asList(catalogsInMediaPackage).contains(catalog));

      // Check path
      File expectedCatalogFile = new File(mediaPackage.getCatalogRoot(), catalog.getFilename());
      assertEquals(expectedCatalogFile, catalog.getFile());

      // Remove catalog
      mediaPackage.remove(catalog);
      catalogsInMediaPackage = mediaPackage.getCatalogs();
      assertTrue(catalogsInMediaPackage.length == numCatalogs);
      assertFalse(Arrays.asList(catalogsInMediaPackage).contains(catalog));
      assertFalse(expectedCatalogFile.exists());

    } catch (NoSuchAlgorithmException e) {
      fail("Error verifying the catalog checksum: " + e.getMessage());
    } catch (IOException e) {
      fail("Error creating the catalog: " + e.getMessage());
    } catch (UnknownFileTypeException e) {
      fail("The catalogs mime type is not supported: " + e.getMessage());
    } catch (MediaPackageException e) {
      fail("Error adding catalog to media package: " + e.getMessage());
    } catch (UnsupportedElementException e) {
      fail("Error adding catalog to media package: " + e.getMessage());
    }
  }

  /**
   * Test method for
   * {@link org.opencastproject.media.mediapackage.MediaPackageImpl#add(org.opencastproject.media.mediapackage.Track)}.
   */
  @Test
  public void testAddAndRemoveTrack() {
    System.out.println("Not yet implemented");
  }

  /**
   * Test method for
   * {@link org.opencastproject.media.mediapackage.MediaPackageImpl#add(org.opencastproject.media.mediapackage.Attachment)}
   * .
   */
  @Test
  public void testAddAndRemoveAttachment() {
    Attachment attachment = null;
    try {
      int numAttachments = mediaPackage.getAttachments().length;
      attachment = CoverImpl.fromFile(coverFile);
      File expectedCoverFile = new File(mediaPackage.getAttachmentRoot(), coverFile.getName());

      // Remove attachment
      mediaPackage.remove(attachment);
      Attachment[] attachmentsInMediaPackage = mediaPackage.getAttachments();
      assertTrue(attachmentsInMediaPackage.length == numAttachments - 1);
      assertFalse(Arrays.asList(attachmentsInMediaPackage).contains(attachment));
      assertFalse(expectedCoverFile.exists());

      // Add attachment
      File coverTestFile = new File(MediaPackageTest.class.getResource("/cover.png").getPath());
      attachment = CoverImpl.fromFile(coverTestFile);
      mediaPackage.add(attachment, false);

      // Check media package consistency
      assertTrue(mediaPackage.getAttachments().length == numAttachments);
      attachmentsInMediaPackage = mediaPackage.getAttachments();
      assertTrue(Arrays.asList(attachmentsInMediaPackage).contains(attachment));

      // Check path
      assertEquals(expectedCoverFile, attachment.getFile());

    } catch (NoSuchAlgorithmException e) {
      fail("Error verifying the attachment checksum: " + e.getMessage());
    } catch (IOException e) {
      fail("Error creating the attachment: " + e.getMessage());
    } catch (UnknownFileTypeException e) {
      fail("The attachments mime type is not supported: " + e.getMessage());
    } catch (MediaPackageException e) {
      fail("Error adding attachment to media package: " + e.getMessage());
    } catch (UnsupportedElementException e) {
      fail("Error adding attachment to media package: " + e.getMessage());
    }
  }

  @Test
  public void testSetAndRemoveCover() {
    Cover cover = null;
    try {
      int numAttachments = mediaPackage.getAttachments().length;
      File expectedCoverFile = new File(mediaPackage.getAttachmentRoot(), coverFile.getName());

      // Make sure the cover is around
      assertNotNull(mediaPackage.getCover());

      // Remove cover
      mediaPackage.removeCover();
      Attachment[] attachmentsInMediaPackage = mediaPackage.getAttachments(Cover.FLAVOR);
      assertTrue(attachmentsInMediaPackage.length == numAttachments - 1);
      assertFalse(Arrays.asList(attachmentsInMediaPackage).contains(cover));
      assertFalse(expectedCoverFile.exists());

      // Create and set cover
      File coverTestFile = new File(MediaPackageBuilderTest.class.getResource("/cover.png").getPath());
      cover = CoverImpl.fromFile(coverTestFile);
      mediaPackage.setCover(cover, false);

      // Check media package consistency
      assertTrue(mediaPackage.getAttachments().length == numAttachments);
      attachmentsInMediaPackage = mediaPackage.getAttachments(Cover.FLAVOR);
      assertTrue(Arrays.asList(attachmentsInMediaPackage).contains(cover));
      assertTrue(cover.equals(mediaPackage.getCover()));

    } catch (NoSuchAlgorithmException e) {
      fail("Error verifying the cover checksum: " + e.getMessage());
    } catch (IOException e) {
      fail("Error creating the cover: " + e.getMessage());
    } catch (UnknownFileTypeException e) {
      fail("The covers mime type is not supported: " + e.getMessage());
    } catch (MediaPackageException e) {
      fail("Error adding cover to media package: " + e.getMessage());
    } catch (UnsupportedElementException e) {
      fail("Error adding cover to media package: " + e.getMessage());
    }
  }

  /**
   * Test method for {@link org.opencastproject.media.mediapackage.MediaPackageImpl#moveTo(File)}.
   */
  @Test
  public void testMoveTo() {
    File tmpInbox = new File(tmpDir, Long.toString(System.currentTimeMillis()));
    File newMediaPackageRoot = new File(tmpInbox, mediaPackage.getRoot().getName());
    String catalogName = mediaPackage.getCatalogs(MediaPackageElements.DUBLINCORE_CATALOG)[0].getFilename();
    try {
      tmpInbox.mkdirs();
      mediaPackage.moveTo(tmpInbox);
      assertEquals(mediaPackage.getRoot().getAbsolutePath(), newMediaPackageRoot.getAbsolutePath());

      // Update test instance directories
      packageDir = mediaPackage.getRoot();
      metadataDir = new File(packageDir, "metadata");

      // Check filesystem
      assertTrue((new File(newMediaPackageRoot, MediaPackageElements.MANIFEST_FILENAME)).exists());

      // Check logical setup
      Catalog[] catalogsInMediaPackage = mediaPackage.getCatalogs();
      File newCatalogRoot = new File(newMediaPackageRoot, "metadata");
      assertTrue(catalogsInMediaPackage.length == 3);
      assertTrue(catalogsInMediaPackage[0].getFile().getAbsolutePath().equals(
              (new File(newCatalogRoot, catalogName)).getAbsolutePath()));
    } catch (IOException e) {
      fail(e.getMessage());
    }
  }

  /**
   * Test method for {@link org.opencastproject.media.mediapackage.MediaPackageImpl#save()}.
   */
  @Test
  public void testAddCatalogAndSave() {
    Catalog[] catalogsInMediaPackage = mediaPackage.getCatalogs(MediaPackageElements.DUBLINCORE_CATALOG);
    int numCatalogs = catalogsInMediaPackage.length;

    // Add the catalog twice in two different ways
    try {
      File catalogTestFile = new File(MediaPackageTest.class.getResource("/dublincore.xml").getPath());
      mediaPackage.add(catalogTestFile, false);
    } catch (MediaPackageException e) {
      fail("Adding of catalog failed: " + e.getMessage());
    } catch (UnsupportedElementException e) {
      fail("Adding of catalog failed: " + e.getMessage());
    }

    // Save the media package
    try {
      mediaPackage.save();
    } catch (MediaPackageException e) {
      fail("Saving the media package failed");
    }

    // Re-read the media package
    try {
      mediaPackage = mediaPackageBuilder.loadFromManifest(manifestFile);
    } catch (ConfigurationException e) {
      fail("Configuration error while loading media package from manifest: " + e.getMessage());
    } catch (MediaPackageException e) {
      fail("Media package exception while loading media package from manifest: " + e.getMessage());
    }

    // Test number of catalogs
    catalogsInMediaPackage = mediaPackage.getCatalogs(MediaPackageElements.DUBLINCORE_CATALOG);
    assertTrue(catalogsInMediaPackage.length == (numCatalogs + 1));
  }

  /**
   * Test method for {@link org.opencastproject.media.mediapackage.MediaPackageImpl#save()}.
   */
  @Test
  public void testRemoveCatalogAndSave() {
    Catalog[] catalogsInMediaPackage = mediaPackage.getCatalogs(MediaPackageElements.DUBLINCORE_CATALOG);
    int numCatalogs = catalogsInMediaPackage.length;
    assertTrue(numCatalogs > 0);

    // Remove the catalog
    try {
      mediaPackage.remove(catalogsInMediaPackage[0]);
    } catch (MediaPackageException e) {
      fail("Removal of catalog failed");
    }

    // Save the media package
    try {
      mediaPackage.save();
    } catch (MediaPackageException e) {
      fail("Saving the media package failed");
    }

    // Re-read the media package
    try {
      mediaPackage = mediaPackageBuilder.loadFromManifest(manifestFile);
    } catch (ConfigurationException e) {
      fail("Configuration error while loading media package from manifest: " + e.getMessage());
    } catch (MediaPackageException e) {
      fail("Media package exception while loading media package from manifest: " + e.getMessage());
    }

    // Test number of catalogs
    catalogsInMediaPackage = mediaPackage.getCatalogs(MediaPackageElements.DUBLINCORE_CATALOG);
    assertTrue(catalogsInMediaPackage.length == (numCatalogs - 1));
  }

  @Test
  public void testReference() {
    MediaPackageReference reference = null;
    Track track = null;
    MediaPackageElement mpeg7Catalog = null;

    // Check for null reference
    Track[] tracksInMediaPackage = mediaPackage.getTracks(MediaPackageElements.PRESENTATION_TRACK);
    assertTrue(tracksInMediaPackage.length > 0);
    track = tracksInMediaPackage[0];
    assertEquals(MediaPackageReference.TYPE_MEDIAPACKAGE, track.getReference().getType());
    assertEquals(MediaPackageReference.SELF, track.getReference().getIdentifier());

    // Check dublin core reference
    mpeg7Catalog = mediaPackage.getElementById("catalog-3");
    assertNotNull(mpeg7Catalog);
    reference = mpeg7Catalog.getReference();
    assertNotNull(reference);
    assertEquals(reference.getType(), "track");
    assertEquals(reference.getIdentifier(), "track-1");

    // Add a new reference and save the media package
    track.referTo(mpeg7Catalog);
    try {
      mediaPackage.save();
    } catch (MediaPackageException e) {
      fail("Error saving the media package");
    }

    // Re-read the media package
    try {
      mediaPackage = mediaPackageBuilder.loadFromManifest(manifestFile);
    } catch (ConfigurationException e) {
      fail("Configuration error while loading media package from manifest: " + e.getMessage());
    } catch (MediaPackageException e) {
      fail("Media package exception while loading media package from manifest: " + e.getMessage());
    }

    // Check the new reference
    tracksInMediaPackage = mediaPackage.getTracks(MediaPackageElements.PRESENTATION_TRACK);
    assertTrue(tracksInMediaPackage.length > 0);
    track = tracksInMediaPackage[0];
    reference = track.getReference();
    assertNotNull(reference);
    assertEquals(reference.getType(), "catalog");
    assertEquals(reference.getIdentifier(), "catalog-3");

    // TODO: Finish this test case
    // MediaPackageElement[] elements = null;

    // Get all catalogs referencing the media package
    // elements = mediaPackage.getCatalogs(new MediaPackageReferenceImpl(mediaPackage));

    // Get all dublin core catalogs referencing the media package
    // elements = mediaPackage.getCatalogs(DublinCoreCatalog.DUBLINCORE_CATALOG, reference);

    // Get all catalogs referencing a media package element

    // Get all catalogs referencing track-2

    // Get all catalogs referencing a series

  }

  /**
   * Test method for {@link org.opencastproject.media.mediapackage.MediaPackage#getElementById(java.lang.String)}
   */
  @Test
  public void testGetElementById() {
    MediaPackageElement element = mediaPackage.getElementById("track-2");
    assertNotNull(element);
    assertTrue(element instanceof Track);

    // Test non existing element
    element = mediaPackage.getElementById("track-99");
    assertNull(element);
  }

  /**
   * Test method for
   * {@link org.opencastproject.media.mediapackage.MediaPackage#getElementByReference(org.opencastproject.media.mediapackage.MediaPackageReference)}
   */
  @Test
  public void testGetElementByReference() {
    Track[] tracksInMediaPackage = mediaPackage.getTracks(MediaPackageElements.PRESENTATION_TRACK);
    assertTrue(tracksInMediaPackage.length > 0);

    // Try to get an element by explicit reference
    Track track = tracksInMediaPackage[0];
    MediaPackageReference reference = new MediaPackageReferenceImpl("track", "track-1");
    MediaPackageElement element = mediaPackage.getElementByReference(reference);
    assertEquals(element, track);
  }

  /**
   * Test method for {@link org.opencastproject.media.mediapackage.MediaPackage#wrap()}
   */
  @Test
  public void testWrap() {
    // Get the presentation track
    Track[] tracksInMediaPackage = mediaPackage.getTracks(MediaPackageElements.PRESENTATION_TRACK);
    assertTrue(tracksInMediaPackage.length > 0);
    Track track = tracksInMediaPackage[0];

    // Overwrite presentation with audio file
    try {
      FileSupport.copy(audioFile, track.getFile());
    } catch (IOException e) {
      fail("Error overwriting presentation track with audio file: " + e.getMessage());
    }

    // Verify media package, should fail because of the checksum
    try {
      mediaPackage.verify();
      fail("Media package verification passed although media package is inconsistent!");
    } catch (MediaPackageException e) {
      // This is expected
    }

    // Wrap the media package
    try {
      mediaPackage.wrap();
    } catch (MediaPackageException e) {
      fail("Wrapping the media package failed: " + e.getMessage());
    }

    // Verify media package again, should pass
    try {
      mediaPackage.verify();
    } catch (MediaPackageException e) {
      fail("Media package verification failed despite wrap()");
    }
  }

}