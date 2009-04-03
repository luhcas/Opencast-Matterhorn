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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.opencastproject.media.bundle.handle.HandleException;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.util.FileSupport;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * Test case used to make sure the bundle builder works as expected.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public class BundleBuilderTest extends AbstractBundleTest {

	/**
	 * Test method for {@link org.opencastproject.media.bundle.BundleBuilderImpl#createNew(org.opencastproject.media.bundle.handle.Handle)}.
	 */
	@Test
	public void testCreateNew() {
		Bundle bundle = null;
		try {
			bundle = bundleBuilder.createNew(identifier);
			assertEquals(identifier, bundle.getIdentifier());
		} catch (BundleException e) {
			fail("Error creating new bundle: " + e.getMessage());
		} finally {
			FileSupport.delete(bundle.getRoot().getParentFile(), true);
		}
	}

	/**
	 * Test method for {@link org.opencastproject.media.bundle.BundleBuilderImpl#loadFromManifest(File)}.
	 */
	@Test
	public void testLoadFromManifest() {
		try {
			Bundle bundle = bundleBuilder.loadFromManifest(manifestFile);
			assertEquals(bundle.getCatalogs().length, 3);
			assertNotNull(bundle.getCatalogs(DublinCoreCatalog.FLAVOR));
		} catch (BundleException e) {
			fail("Bundle excpetion while reading bundle from manifest: " + e.getMessage());
		} catch (ConfigurationException e) {
			fail("Configuration exception while reading bundle from manifest: " + e.getMessage());
		}
	}

	/**
	 * Test method for {@link org.opencastproject.media.bundle.BundleBuilderImpl#loadFromDirectory(java.io.File)}.
	 */
	@Test
	public void testLoadFromDirectory() {
		try {
			Bundle bundle = bundleBuilder.loadFromDirectory(bundleDir);
			assertEquals(bundle.getCatalogs().length, 3);
			assertNotNull(bundle.getCatalogs(DublinCoreCatalog.FLAVOR));
		} catch (BundleException e) {
			fail("Bundle excpetion while reading bundle from directory: " + e.getMessage());
		} catch (ConfigurationException e) {
			fail("Configuration exception while reading bundle from directory: " + e.getMessage());
		}
	}

	/**
	 * Test method for {@link org.opencastproject.media.bundle.BundleBuilderImpl#createFromElements(java.io.File,boolean)}.
	 * @throws IOException 
	 */
	@Test
	public void testCreateFromElements() throws IOException {
		try {
			Bundle bundle = bundleBuilder.createFromElements(bundleDir, false);
			assertEquals(bundle.getCatalogs().length, 3);
			assertNotNull(bundle.getCatalogs(DublinCoreCatalog.FLAVOR));
			assertEquals(bundle.getCatalogs(DublinCoreCatalog.FLAVOR).length, 2);
		} catch (BundleException e) {
			fail("Bundle exception while reading bundle from elements: " + e.getMessage());
		} catch (UnsupportedBundleElementException e) {
			fail("Unsupported bundle element found while reading bundle from elements: " + e.getMessage());
		}

		// Test unknown elements
		File showstopper = new File(bundleDir, "showstopper.zzz");
		showstopper.createNewFile();
		try {
			try {
				bundleBuilder.createFromElements(bundleDir, false);
				fail("Bundle builder ignored an unknown file where it shouldn't");
			} catch (BundleException e) {
				fail("Bundle exception while reading bundle from elements: " + e.getMessage());
			} catch (UnsupportedBundleElementException e) {
				// This was expected
			}
			try {
				bundleBuilder.createFromElements(bundleDir, true);
			} catch (BundleException e) {
				fail("Bundle exception while reading bundle from elements: " + e.getMessage());
			} catch (UnsupportedBundleElementException e) {
				fail("A UnsupportedBundleElementException exception was raised although ignoreUnknown was true");
			}
		} finally {
			showstopper.delete();
		}

		// Try again without manifest
		manifestFile.delete();
		try {
			Bundle bundle = bundleBuilder.createFromElements(bundleDir, false);
			assertEquals(bundle.getCatalogs().length, 3);
			assertNotNull(bundle.getCatalogs(DublinCoreCatalog.FLAVOR));
		} catch (BundleException e) {
			fail("Bundle exception while reading bundle from elements: " + e.getMessage());
		} catch (UnsupportedBundleElementException e) {
			fail("Unsupported bundle element found while reading bundle from elements: " + e.getMessage());
		}

	}

	@Test
	public void testMediaAnalyzer() {
		String audioVideoPath = this.getClass().getResource("/av.mov").getPath();
		String videoOnlyPath = this.getClass().getResource("/vonly.mov").getPath();
		String audioOnlyPath = this.getClass().getResource("/aonly.mov").getPath();
		Bundle bundle = null;
		BundleElement audiovisualElement = null;
		BundleElement videoElement = null;
		BundleElement audioElement = null;
		try {
			try {
				bundle = bundleBuilder.createNew(handleBuilder.createNew());
				audiovisualElement = bundle.add(new File(audioVideoPath), false);
				videoElement = bundle.add(new File(videoOnlyPath), false);
				audioElement = bundle.add(new File(audioOnlyPath), false);
			} catch (BundleException e) {
				fail("Bundle exception while reading bundle from elements: " + e.getMessage());
			} catch (UnsupportedBundleElementException e) {
				fail("Unsupported bundle element found while reading bundle from elements: " + e.getMessage());
			} catch (HandleException e) {
				fail("Error creating handle: " + e.getMessage());
			}
			
			// Test element types
			if (!(audiovisualElement instanceof AudioVisualTrack))
				fail("Audiovisual track has not been recognized by the bundle builder");
			AudioVisualTrack audiovisualTrack = (AudioVisualTrack)audiovisualElement;
			
			if (!(videoElement instanceof VideoTrack))
				fail("Video track has not been recognized by the bundle builder");
			VideoTrack videoTrack = (VideoTrack)videoElement;			
			
			if (!(audioElement instanceof AudioTrack))
				fail("Audio track has not been recognized by the bundle builder");
			AudioTrack audioTrack = (AudioTrack)audioElement;
			
			// Test track durations
            // Todo Bundle is not requested to take tracks with equal length for now so this test is disabled.
            // Todo See ManifestImpl.add(BundleElement)
			//assertEquals(audiovisualTrack.getDuration(), bundle.getDuration());
			//assertEquals(videoTrack.getDuration(), bundle.getDuration());
			//assertEquals(audioTrack.getDuration(), bundle.getDuration());
			
			// Test adding of track with different duration
			
			// Test Audio/Video settings
			assertTrue(audiovisualTrack.hasAudio() && audiovisualTrack.hasVideo());
			assertTrue(!videoTrack.hasAudio() && videoTrack.hasVideo());
			assertTrue(audioTrack.hasAudio() && !audioTrack.hasVideo());
			
		} finally {
			if (bundle != null)
				FileSupport.delete(bundle.getRoot());
		}
	}
	
	/**
	 * Test method for {@link org.opencastproject.media.bundle.BundleBuilderImpl#loadFromPackage(org.opencastproject.media.bundle.BundlePackager, java.io.InputStream)}.
	 */
	@Test
	public void testLoadFromPackage() {
		System.out.println("Not yet implemented");
	}

}