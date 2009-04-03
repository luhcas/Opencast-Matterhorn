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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.opencastproject.util.ConfigurationException;

import org.junit.Test;

import java.io.File;

/**
 * Test case for bundle references.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public class BundleReferenceTest extends AbstractBundleTest {

	/**
	 * Test method for {@link org.opencastproject.media.bundle.BundleReferenceImpl#matches(BundleReference)}.
	 */
	@Test
	public void testMatches() {
		BundleReference bundleReference = new BundleReferenceImpl(bundle);
		BundleReference genericBundleReference = new BundleReferenceImpl("bundle", "*");
		BundleReference trackReference = new BundleReferenceImpl(bundle.getElementById("track-2"));
		BundleReference genericTrackReference = new BundleReferenceImpl("track", "*");
		
		assertFalse(bundleReference.matches(trackReference));
		assertFalse(trackReference.matches(bundleReference));
		
		assertTrue(bundleReference.matches(bundleReference));
		assertFalse(bundleReference.matches(genericBundleReference));
		assertTrue(genericBundleReference.matches(bundleReference));

		assertTrue(trackReference.matches(trackReference));
		assertFalse(trackReference.matches(genericTrackReference));
		assertTrue(genericTrackReference.matches(trackReference));
	}

	/**
	 * Test method for {@link org.opencastproject.media.bundle.BundleImpl#save()}.
	 */
	@Test
	public void testBundleReference() {
		try {
			// Add first catalog without any reference
			File catalogXTestFile = new File(this.getClass().getResource("/dublincore.xml").getPath());
			BundleElement catalogX = bundle.add(catalogXTestFile, false);
			catalogX.setIdentifier("catalog-x");

			// Add second catalog with bundle reference
			File catalogYTestFile = new File(this.getClass().getResource("/dublincore.xml").getPath());
			BundleElement catalogY = bundle.add(catalogYTestFile, false);
			catalogY.referTo(new BundleReferenceImpl(bundle));
			catalogY.setIdentifier("catalog-y");

			// Add third catalog with track reference
			File catalogZTestFile = new File(this.getClass().getResource("/dublincore.xml").getPath());
			BundleElement catalogZ = bundle.add(catalogZTestFile, false);
			catalogZ.referTo(new BundleReferenceImpl("track", "track-1"));
			catalogZ.setIdentifier("catalog-z");

			// Save the bundle
			bundle.save();
		} catch (BundleException e) {
			fail("Adding of catalog failed: " + e.getMessage());
		} catch (UnsupportedBundleElementException e) {
			fail("Adding of catalog failed: " + e.getMessage());
		}
		
		// Re-read the bundle and test the references
		try {
			bundle = bundleBuilder.loadFromManifest(manifestFile);
			BundleElement catalogX = bundle.getElementById("catalog-x");
			assertNotNull(catalogX.getReference());
			BundleElement catalogY = bundle.getElementById("catalog-y");
			assertNotNull(catalogY.getReference());
			BundleElement catalogZ = bundle.getElementById("catalog-z");
			assertNotNull(catalogZ.getReference());
			assertTrue(catalogZ.getReference().matches(new BundleReferenceImpl("track", "track-1")));
		} catch (ConfigurationException e) {
			fail("Configuration error while loading bundle from manifest: " + e.getMessage());
		} catch (BundleException e) {
			fail("Bundle exception while loading bundle from manifest: " + e.getMessage());
		}
	}

}