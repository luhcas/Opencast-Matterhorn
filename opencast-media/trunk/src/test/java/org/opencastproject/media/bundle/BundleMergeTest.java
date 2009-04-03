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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.opencastproject.media.bundle.BundleSupport.MergeMode;
import org.opencastproject.util.FileSupport;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Test case to make sure bundle support works as expected.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public class BundleMergeTest {
	
	/** tmp directory */
	protected File tmpDir = null;

	/** The bundle builder */
	protected BundleBuilder bundleBuilder = null;

	/** The source bundle directory for merge tests */
	File sourceDir = null;
	
	/** The target bundle directory for merge tests */
	File targetDir = null;

	@Before
	public void setUp() throws Exception {

		// Get hold of the tmp directory
		tmpDir = FileSupport.getTempDirectory();
		
		// Create a bundle builder
		bundleBuilder = BundleBuilderFactory.newInstance().newBundleBuilder();

		// Create source and target bundles
		setUpSourceBundle();
		setUpTargetBundle();
	}

	/**
	 * Creates the source bundle.
	 * 
	 * @throws IOException
	 * @throws BundleException 
	 */
	private void setUpSourceBundle() throws IOException, BundleException {
		// Create the bundle directory
		File bundleDir = new File(tmpDir, Long.toString(System.currentTimeMillis()));
		
		// Create subdirectories
		File trackDir = new File(bundleDir, "tracks");
		trackDir.mkdirs();
		File metadataDir = new File(bundleDir, "metadata");
		metadataDir.mkdirs();
		File attachmentDir = new File(bundleDir, "attachments");
		attachmentDir.mkdirs();
		
		// Setup test files
		File manifestTestFile = new File(this.getClass().getResource("/source-manifest.xml").getPath());
		File videoTestFile = new File(this.getClass().getResource("/vonly.mov").getPath());
		File audioTestFile = new File(this.getClass().getResource("/aonly.mov").getPath());
		File dcTestFile = new File(this.getClass().getResource("/dublincore.xml").getPath());
		File dcSeriesTestFile = new File(this.getClass().getResource("/series-dublincore.xml").getPath());
		File mpeg7TestFile = new File(this.getClass().getResource("/mpeg-7.xml").getPath());
		File coverTestFile = new File(this.getClass().getResource("/cover.png").getPath());

		// Copy files into place
		FileSupport.copy(manifestTestFile, new File(bundleDir, Manifest.FILENAME));
		FileSupport.copy(videoTestFile, trackDir);
		FileSupport.copy(audioTestFile, trackDir);
		FileSupport.copy(dcTestFile, metadataDir);
		FileSupport.copy(dcSeriesTestFile, metadataDir);
		FileSupport.copy(dcSeriesTestFile, new File(metadataDir, "series-dublincore-new.xml"));
		FileSupport.copy(mpeg7TestFile, metadataDir);
		FileSupport.copy(coverTestFile, attachmentDir);
		FileSupport.copy(coverTestFile, new File(attachmentDir, "series-cover.png"));
		
		// Test the bundle
		Bundle bundle = bundleBuilder.loadFromDirectory(bundleDir);
		bundle.verify();
		
		// Set the bundle directory
		sourceDir = bundleDir;
	}
	
	/**
	 * Creates the target bundle.
	 * 
	 * @throws IOException
	 * @throws BundleException 
	 */
	private void setUpTargetBundle() throws IOException, BundleException {
		// Create the bundle directory
		File bundleDir = new File(tmpDir, Long.toString(System.currentTimeMillis()));
		
		// Create subdirectories
		File trackDir = new File(bundleDir, "tracks");
		trackDir.mkdirs();
		File metadataDir = new File(bundleDir, "metadata");
		metadataDir.mkdirs();
		File attachmentDir = new File(bundleDir, "attachments");
		attachmentDir.mkdirs();
		
		// Setup test files
		File manifestTestFile = new File(this.getClass().getResource("/target-manifest.xml").getPath());
		File videoTestFile = new File(this.getClass().getResource("/vonly.mov").getPath());
		File audioTestFile = new File(this.getClass().getResource("/aonly.mov").getPath());
		File dcTestFile = new File(this.getClass().getResource("/dublincore.xml").getPath());
		File dcSeriesTestFile = new File(this.getClass().getResource("/series-dublincore.xml").getPath());
		File mpeg7TestFile = new File(this.getClass().getResource("/mpeg-7.xml").getPath());
		File coverTestFile = new File(this.getClass().getResource("/cover.png").getPath());

		// Copy files into place
		FileSupport.copy(manifestTestFile, new File(bundleDir, Manifest.FILENAME));
		FileSupport.copy(videoTestFile, trackDir);
		FileSupport.copy(audioTestFile, trackDir);
		FileSupport.copy(audioTestFile, new File(trackDir, "aonly2.mov"));
		FileSupport.copy(dcTestFile, metadataDir);
		FileSupport.copy(dcSeriesTestFile, metadataDir);
		FileSupport.copy(mpeg7TestFile, metadataDir);
		FileSupport.copy(coverTestFile, attachmentDir);
		
		// Test the bundle
		bundleBuilder.loadFromDirectory(bundleDir);
		Bundle bundle = bundleBuilder.loadFromDirectory(bundleDir);
		bundle.verify();

		// Set the bundle directory
		targetDir = bundleDir;
	}
	
	/**
	 * Checks the bundle for duplicate identifier and files.
	 * 
	 * @param bundle the bundle to test
	 * @throws BundleException
	 */
	private void testBundleConsistency(Bundle bundle) throws BundleException {
		List<String> ids = new ArrayList<String>();
		List<File> files = new ArrayList<File>();
		for (BundleElement e : bundle.elements()) {
			if (ids.contains(e.getIdentifier()))
				throw new BundleException("Duplicate id " + e.getIdentifier() + "' found");
			ids.add(e.getIdentifier());
			if (files.contains(e.getFile()))
				throw new BundleException("Duplicate filename " + e.getFile() + "' found");
			files.add(e.getFile());
		}
	}
	
	@Test
	public void testMergeByMerging() {
		try {
			Bundle bundle = BundleSupport.merge(targetDir, sourceDir, true, MergeMode.Merge);
			assertTrue(bundle.getTracks().length == 5);
			assertTrue(bundle.getCatalogs().length == 6);
			assertTrue(bundle.getAttachments().length == 3);
			testBundleConsistency(bundle);
		} catch (BundleException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testMergeByReplacing() {
		try {
			Bundle bundle = bundleBuilder.loadFromDirectory(targetDir);
			
			// Should be replaced
			BundleElement track1 = bundle.getElementById("track-1");
			BundleElement catalog1 = bundle.getElementById("catalog-1");
			BundleElement catalog2 = bundle.getElementById("catalog-2");
			BundleElement cover = bundle.getElementById("cover");

			// Should remain untouched
			BundleElement track2 = bundle.getElementById("track-2");
			BundleElement track4 = bundle.getElementById("track-4");
			BundleElement catalog3 = bundle.getElementById("catalog-3");
			
			// Merge the bundle
			bundle = BundleSupport.merge(targetDir, sourceDir, true, MergeMode.Replace);
			
			// Test number of elements
			assertEquals(bundle.getTracks().length, 4);
			assertEquals(bundle.getCatalogs().length, 4);
			assertEquals(bundle.getAttachments().length, 2);
			
			// Test for replaced elements
			assertNotSame(track1, bundle.getElementById("track-1"));
			assertNotSame(catalog1, bundle.getElementById("catalog-1"));
			assertNotSame(catalog2, bundle.getElementById("catalog-2"));
			assertNotSame(cover, bundle.getElementById("cover"));

			// Test for untouched elements
			assertEquals(track2, bundle.getElementById("track-2"));
			assertEquals(track4, bundle.getElementById("track-4"));
			assertEquals(catalog3, bundle.getElementById("catalog-3"));

			testBundleConsistency(bundle);
		} catch (BundleException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testMergeBySkipping() {
		try {
			Bundle targetBundle = bundleBuilder.loadFromDirectory(targetDir);
			
			// Should remain untouched
			BundleElement track1 = targetBundle.getElementById("track-1");
			BundleElement catalog1 = targetBundle.getElementById("catalog-1");
			BundleElement catalog2 = targetBundle.getElementById("catalog-2");
			BundleElement cover = targetBundle.getElementById("cover");

			// Merge the bundle
			Bundle bundle = BundleSupport.merge(targetDir, sourceDir, true, MergeMode.Skip);
			
			// Test number of elements
			assertEquals(bundle.getTracks().length, 4);
			assertEquals(bundle.getCatalogs().length, 4);
			assertEquals(bundle.getAttachments().length, 2);
			
			// Test for untouched elements
			assertEquals(track1, bundle.getElementById("track-1"));
			assertEquals(catalog1, bundle.getElementById("catalog-1"));
			assertEquals(catalog2, bundle.getElementById("catalog-2"));
			assertEquals(cover, bundle.getElementById("cover"));

			testBundleConsistency(bundle);
		} catch (BundleException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testMergeByFailing() {
		try {
			BundleSupport.merge(targetDir, sourceDir, true, MergeMode.Fail);
			fail("Merging should have failed but didn't");
		} catch (BundleException e) {
			// This is excpected
		}
	}

}