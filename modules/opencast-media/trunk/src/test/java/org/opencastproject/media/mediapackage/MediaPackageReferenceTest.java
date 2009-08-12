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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.File;

/**
 * Test case for media package references.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: MediaPackageReferenceTest.java 1769 2009-01-05 11:19:35Z wunden $
 */
public class MediaPackageReferenceTest extends AbstractMediaPackageTest {

  /**
   * Test method for
   * {@link org.opencastproject.media.mediapackage.MediaPackageReferenceImpl#matches(MediaPackageReference)}.
   */
  @Test
  public void testMatches() {
    MediaPackageReference mediaPackageReference = new MediaPackageReferenceImpl(mediaPackage);
    MediaPackageReference genericMediaPackageReference = new MediaPackageReferenceImpl(
            MediaPackageReference.TYPE_MEDIAPACKAGE, "*");
    MediaPackageReference trackReference = new MediaPackageReferenceImpl(mediaPackage.getElementById("track-2"));
    MediaPackageReference genericTrackReference = new MediaPackageReferenceImpl("track", "*");

    assertFalse(mediaPackageReference.matches(trackReference));
    assertFalse(trackReference.matches(mediaPackageReference));

    assertTrue(mediaPackageReference.matches(mediaPackageReference));
    assertTrue(mediaPackageReference.matches(genericMediaPackageReference));
    assertTrue(genericMediaPackageReference.matches(mediaPackageReference));

    assertTrue(trackReference.matches(trackReference));
    assertTrue(trackReference.matches(genericTrackReference));
    assertTrue(genericTrackReference.matches(trackReference));
  }

  /**
   * Test method for {@link org.opencastproject.media.mediapackage.MediaPackageImpl#save()}.
   */
  @Test
  public void testMediaPackageReference() {
    try {
      // Add first catalog without any reference
      File catalogXTestFile = new File(MediaPackageTest.class.getResource("/dublincore.xml").getPath());
      MediaPackageElement catalogX = mediaPackage.add(catalogXTestFile, false);
      catalogX.setIdentifier("catalog-x");

      // Add second catalog with media package reference
      File catalogYTestFile = new File(MediaPackageTest.class.getResource("/dublincore.xml").getPath());
      MediaPackageElement catalogY = mediaPackage.add(catalogYTestFile, false);
      catalogY.referTo(new MediaPackageReferenceImpl(mediaPackage));
      catalogY.setIdentifier("catalog-y");

      // Add third catalog with track reference
      File catalogZTestFile = new File(MediaPackageTest.class.getResource("/dublincore.xml").getPath());
      MediaPackageElement catalogZ = mediaPackage.add(catalogZTestFile, false);
      catalogZ.referTo(new MediaPackageReferenceImpl("track", "track-1"));
      catalogZ.setIdentifier("catalog-z");

      // Save the media package
      mediaPackage.save();
    } catch (MediaPackageException e) {
      fail("Adding of catalog failed: " + e.getMessage());
    } catch (UnsupportedElementException e) {
      fail("Adding of catalog failed: " + e.getMessage());
    }

    // Re-read the media package and test the references
    try {
      mediaPackage = mediaPackageBuilder.loadFromManifest(manifestFile);
      MediaPackageElement catalogX = mediaPackage.getElementById("catalog-x");
      assertNotNull(catalogX.getReference());
      MediaPackageElement catalogY = mediaPackage.getElementById("catalog-y");
      assertNotNull(catalogY.getReference());
      MediaPackageElement catalogZ = mediaPackage.getElementById("catalog-z");
      assertNotNull(catalogZ.getReference());
      assertTrue(catalogZ.getReference().matches(new MediaPackageReferenceImpl("track", "track-1")));
    } catch (ConfigurationException e) {
      fail("Configuration error while loading media package from manifest: " + e.getMessage());
    } catch (MediaPackageException e) {
      fail("Media package exception while loading media package from manifest: " + e.getMessage());
    }
  }

}