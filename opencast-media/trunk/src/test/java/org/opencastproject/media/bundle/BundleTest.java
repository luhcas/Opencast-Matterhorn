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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.opencastproject.media.bundle.attachment.CoverImpl;
import org.opencastproject.media.bundle.dublincore.DublinCoreCatalogImpl;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.util.FileSupport;
import org.opencastproject.util.UnknownFileTypeException;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Test case for the bundle management.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public class BundleTest extends AbstractBundleTest {

	/**
	 * Test method for {@link org.opencastproject.media.bundle.BundleImpl#add(org.opencastproject.media.bundle.Catalog)}.
	 */
	@Test
	public void testAddAndRemoveCatalog() {
		DublinCoreCatalog catalog = null;
		try {
			int numCatalogs = bundle.getCatalogs().length;
			
			// Create and add catalog
			catalog = DublinCoreCatalogImpl.newInstance();
			bundle.add(catalog);
			
			// Check object consistency
			assertTrue(bundle.getCatalogs().length == numCatalogs + 1);
			Catalog[] catalogsInBundle = bundle.getCatalogs(DublinCoreCatalog.FLAVOR);
			assertTrue(Arrays.asList(catalogsInBundle).contains(catalog));
			
			// Check path
			File expectedCatalogFile = new File(bundle.getCatalogRoot(), catalog.getFilename());
			assertEquals(expectedCatalogFile, catalog.getFile());

			// Remove catalog
			bundle.remove(catalog);
			catalogsInBundle = bundle.getCatalogs();
			assertTrue(catalogsInBundle.length == numCatalogs);
			assertFalse(Arrays.asList(catalogsInBundle).contains(catalog));
			assertFalse(expectedCatalogFile.exists());

		} catch (NoSuchAlgorithmException e) {
			fail("Error verifying the catalog checksum: " + e.getMessage());
		} catch (IOException e) {
			fail("Error creating the catalog: " + e.getMessage());
		} catch (UnknownFileTypeException e) {
			fail("The catalog's mime type is not supported: " + e.getMessage());
		} catch (BundleException e) {
			fail("Error adding catalog to bundle: " + e.getMessage());
		} catch (UnsupportedBundleElementException e) {
			fail("Error adding catalog to bundle: " + e.getMessage());
		}
	}

	/**
	 * Test method for {@link org.opencastproject.media.bundle.BundleImpl#add(org.opencastproject.media.bundle.Track)}.
	 */
	@Test
	public void testAddAndRemoveTrack() {
		System.out.println("Not yet implemented");
	}

	/**
	 * Test method for {@link org.opencastproject.media.bundle.BundleImpl#add(org.opencastproject.media.bundle.Attachment)}.
	 */
	@Test
	public void testAddAndRemoveAttachment() {
		Attachment attachment = null;
		try {
			int numAttachments = bundle.getAttachments().length;
			attachment = CoverImpl.fromFile(coverFile);
			File expectedCoverFile = new File(bundle.getAttachmentRoot(), coverFile.getName());

			// Remove attachment
			bundle.remove(attachment);
			Attachment[] attachmentsInBundle = bundle.getAttachments();
			assertTrue(attachmentsInBundle.length == numAttachments - 1);
			assertFalse(Arrays.asList(attachmentsInBundle).contains(attachment));
			assertFalse(expectedCoverFile.exists());

			// Add attachment
			File coverTestFile = new File(this.getClass().getResource("/cover.png").getPath());
			attachment = CoverImpl.fromFile(coverTestFile);
			bundle.add(attachment, false);
			
			// Check bundle consistency
			assertTrue(bundle.getAttachments().length == numAttachments);
			attachmentsInBundle = bundle.getAttachments();
			assertTrue(Arrays.asList(attachmentsInBundle).contains(attachment));
			
			// Check path
			assertEquals(expectedCoverFile, attachment.getFile());

		} catch (NoSuchAlgorithmException e) {
			fail("Error verifying the attachment checksum: " + e.getMessage());
		} catch (IOException e) {
			fail("Error creating the attachment: " + e.getMessage());
		} catch (UnknownFileTypeException e) {
			fail("The attachment's mime type is not supported: " + e.getMessage());
		} catch (BundleException e) {
			fail("Error adding attachment to bundle: " + e.getMessage());
		} catch (UnsupportedBundleElementException e) {
			fail("Error adding attachment to bundle: " + e.getMessage());
		}
	}

	@Test
	public void testSetAndRemoveCover() {
		Cover cover = null;
		try {
			int numAttachments = bundle.getAttachments().length;
			File expectedCoverFile = new File(bundle.getAttachmentRoot(), coverFile.getName());
			
			// Make sure the cover is around
			assertNotNull(bundle.getCover());
			
			// Remove cover
			bundle.removeCover();
			Attachment[] attachmentsInBundle = bundle.getAttachments(Cover.FLAVOR);
			assertTrue(attachmentsInBundle.length == numAttachments - 1);
			assertFalse(Arrays.asList(attachmentsInBundle).contains(cover));
			assertFalse(expectedCoverFile.exists());

			// Create and set cover
			File coverTestFile = new File(this.getClass().getResource("/cover.png").getPath());
			cover = CoverImpl.fromFile(coverTestFile);
			bundle.setCover(cover, false);
			
			// Check bundle consistency
			assertTrue(bundle.getAttachments().length == numAttachments);
			attachmentsInBundle = bundle.getAttachments(Cover.FLAVOR);
			assertTrue(Arrays.asList(attachmentsInBundle).contains(cover));
			assertTrue(cover.equals(bundle.getCover()));

		} catch (NoSuchAlgorithmException e) {
			fail("Error verifying the cover checksum: " + e.getMessage());
		} catch (IOException e) {
			fail("Error creating the cover: " + e.getMessage());
		} catch (UnknownFileTypeException e) {
			fail("The cover's mime type is not supported: " + e.getMessage());
		} catch (BundleException e) {
			fail("Error adding cover to bundle: " + e.getMessage());
		} catch (UnsupportedBundleElementException e) {
			fail("Error adding cover to bundle: " + e.getMessage());
		}
	}
	
	/**
	 * Test method for {@link org.opencastproject.media.bundle.BundleImpl#moveTo(File)}.
	 */
	@Test
	public void testMoveTo() {
		File tmpInbox = new File(tmpDir, Long.toString(System.currentTimeMillis()));
		File newBundleRoot = new File(tmpInbox, bundle.getRoot().getName());
		String catalogName = bundle.getCatalogs(DublinCoreCatalog.FLAVOR)[0].getFilename();
		try {
			tmpInbox.mkdirs();
			bundle.moveTo(tmpInbox);
			assertEquals(bundle.getRoot().getAbsolutePath(), newBundleRoot.getAbsolutePath());
			
			// Update test instance directories
			bundleDir = bundle.getRoot();
			metadataDir = new File(bundleDir, "metadata");
			
			// Check filesystem
			assertTrue((new File(newBundleRoot, Manifest.FILENAME)).exists());
			
			// Check logical setup
			Catalog[] catalogsInBundle = bundle.getCatalogs();
			File newCatalogRoot = new File(newBundleRoot, "metadata");
			assertTrue(catalogsInBundle.length == 3);
			assertTrue(catalogsInBundle[0].getFile().getAbsolutePath().equals((new File(newCatalogRoot, catalogName)).getAbsolutePath()));
		} catch (IOException e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Test method for {@link org.opencastproject.media.bundle.BundleImpl#save()}.
	 */
	@Test
	public void testAddCatalogAndSave() {
		Catalog[] catalogsInBundle = bundle.getCatalogs(DublinCoreCatalog.FLAVOR);
		int numCatalogs = catalogsInBundle.length;
		
		// Add the catalog twice in two different ways
		try {
			File catalogTestFile = new File(this.getClass().getResource("/dublincore.xml").getPath());
			bundle.add(catalogTestFile, false);
		} catch (BundleException e) {
			fail("Adding of catalog failed: " + e.getMessage());
		} catch (UnsupportedBundleElementException e) {
			fail("Adding of catalog failed: " + e.getMessage());
		}
		
		// Save the bundle
		try {
			bundle.save();
		} catch (BundleException e) {
			fail("Saving the bundle failed");
		}

		// Re-read the bundle
		try {
			bundle = bundleBuilder.loadFromManifest(manifestFile);
		} catch (ConfigurationException e) {
			fail("Configuration error while loading bundle from manifest: " + e.getMessage());
		} catch (BundleException e) {
			fail("Bundle exception while loading bundle from manifest: " + e.getMessage());
		}
		
		// Test number of catalogs
		catalogsInBundle = bundle.getCatalogs(DublinCoreCatalog.FLAVOR);
		assertTrue(catalogsInBundle.length == (numCatalogs + 1));
	}

	/**
	 * Test method for {@link org.opencastproject.media.bundle.BundleImpl#save()}.
	 */
	@Test
	public void testRemoveCatalogAndSave() {
		Catalog[] catalogsInBundle = bundle.getCatalogs(DublinCoreCatalog.FLAVOR);
		int numCatalogs = catalogsInBundle.length;
		assertTrue(numCatalogs > 0);
		
		// Remove the catalog
		try {
			bundle.remove(catalogsInBundle[0]);
		} catch (BundleException e) {
			fail("Removal of catalog failed");
		}
		
		// Save the bundle
		try {
			bundle.save();
		} catch (BundleException e) {
			fail("Saving the bundle failed");
		}

		// Re-read the bundle
		try {
			bundle = bundleBuilder.loadFromManifest(manifestFile);
		} catch (ConfigurationException e) {
			fail("Configuration error while loading bundle from manifest: " + e.getMessage());
		} catch (BundleException e) {
			fail("Bundle exception while loading bundle from manifest: " + e.getMessage());
		}
		
		// Test number of catalogs
		catalogsInBundle = bundle.getCatalogs(DublinCoreCatalog.FLAVOR);
		assertTrue(catalogsInBundle.length == (numCatalogs -1 ));
	}
	
	@Test
	public void testReference() {
		BundleReference reference = null;
		Track track = null;
		BundleElement mpeg7Catalog = null;
		
		// Check for null reference
		Track[] tracksInBundle = bundle.getTracks(PresentationTrack.FLAVOR);
		assertTrue(tracksInBundle.length > 0);
		track = tracksInBundle[0];
		assertNull(track.getReference());

		// Check dublin core reference
		mpeg7Catalog = bundle.getElementById("catalog-3");
		assertNotNull(mpeg7Catalog);
		reference = mpeg7Catalog.getReference();
		assertNotNull(reference);
		assertEquals(reference.getType(), "track");
		assertEquals(reference.getIdentifier(), "track-1");
		
		// Add a new reference and save the bundle
		track.referTo(mpeg7Catalog);
		try {
			bundle.save();
		} catch (BundleException e) {
			fail("Error saving the bundle");
		}
		
		// Re-read the bundle
		try {
			bundle = bundleBuilder.loadFromManifest(manifestFile);
		} catch (ConfigurationException e) {
			fail("Configuration error while loading bundle from manifest: " + e.getMessage());
		} catch (BundleException e) {
			fail("Bundle exception while loading bundle from manifest: " + e.getMessage());
		}
		
		// Check the new reference
		tracksInBundle = bundle.getTracks(PresentationTrack.FLAVOR);
		assertTrue(tracksInBundle.length > 0);
		track = tracksInBundle[0];
		reference = track.getReference();
		assertNotNull(reference);
		assertEquals(reference.getType(), "catalog");
		assertEquals(reference.getIdentifier(), "catalog-3");
		
		// TODO: Finish this test case
		//BundleElement[] elements = null;

		// Get all catalogs referencing the bundle
		// elements = bundle.getCatalogs(new BundleReferenceImpl(bundle));
		

		// Get all dublin core catalogs referencing the bundle
		// elements = bundle.getCatalogs(DublinCoreCatalog.FLAVOR, reference);
		

		// Get all catalogs referencing a bundle element

		// Get all catalogs referencing track-2
		
		// Get all catalogs referencing a series
		
	}

	/**
	 * Test method for {@link org.opencastproject.media.bundle.Bundle#getElementById(java.lang.String)}
	 */
	@Test
	public void testGetElementById() {
		BundleElement element = bundle.getElementById("track-2");
		assertNotNull(element);
		assertTrue(element instanceof Track);
		
		// Test non existing element
		element = bundle.getElementById("track-99");
		assertNull(element);
	}

	/**
	 * Test method for {@link org.opencastproject.media.bundle.Bundle#getElementByReference(org.opencastproject.media.bundle.BundleReference)}
	 */
	@Test
	public void testGetElementByReference() {
		Track[] tracksInBundle = bundle.getTracks(PresentationTrack.FLAVOR);
		assertTrue(tracksInBundle.length > 0);
		
		// Try to get an element by explicit reference
		Track track = tracksInBundle[0];
		BundleReference reference = new BundleReferenceImpl("track", "track-1");
		BundleElement element = bundle.getElementByReference(reference);
		assertEquals(element, track);		
	}
	
	/**
	 * Test method for {@link org.opencastproject.media.bundle.Bundle#wrap()}
	 */
	@Test
	public void testWrap() {
		// Get the presentation track
		Track[] tracksInBundle = bundle.getTracks(PresentationTrack.FLAVOR);
		assertTrue(tracksInBundle.length > 0);
		Track track = tracksInBundle[0];

		// Overwrite presentation with audio file
		try {
			FileSupport.copy(audioFile, track.getFile());
		} catch (IOException e) {
			fail("Error overwriting presentation track with audio file: " + e.getMessage());
		}
		
		// Verify bundle, should fail because of the checksum
		try {
			bundle.verify();
			fail("Bundle verification passed although bundle is inconsistent!");
		} catch (BundleException e) {
			// This is expected
		}
		
		// Wrap the bundle
		try {
			bundle.wrap();
		} catch (BundleException e) {
			fail("Wrapping the bundle failed: " + e.getMessage());
		}
		
		// Verify bundle again, should pass
		try {
			bundle.verify();
		} catch (BundleException e) {
			fail("Bundle verification failed despite wrap()");
		}
	}
	
}