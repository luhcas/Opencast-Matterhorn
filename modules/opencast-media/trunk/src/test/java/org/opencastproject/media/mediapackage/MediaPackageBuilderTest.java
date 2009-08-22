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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * Test case used to make sure the media package builder works as expected.
 */
public class MediaPackageBuilderTest extends AbstractMediaPackageTest {

  /**
   * Test method for
   * {@link org.opencastproject.media.mediapackage.MediaPackageBuilderImpl#createNew(org.opencastproject.media.mediapackage.handle.Handle)}
   * .
   */
  @Test
  public void testCreateNew() {
    MediaPackage mediaPackage = null;
    try {
      mediaPackage = mediaPackageBuilder.createNew(identifier);
      assertEquals(identifier, mediaPackage.getIdentifier());
    } catch (MediaPackageException e) {
      fail("Error creating new media package: " + e.getMessage());
    }
  }

  /**
   * Test method for {@link org.opencastproject.media.mediapackage.MediaPackageBuilderImpl#loadFromManifest(File)}.
   */
  @Test
  public void testLoadFromManifest() {
    try {
      MediaPackage mediaPackage = mediaPackageBuilder.loadFromManifest(new FileInputStream(manifestFile));
      assertEquals(mediaPackage.getCatalogs().length, 3);
      assertNotNull(mediaPackage.getCatalogs(MediaPackageElements.DUBLINCORE_CATALOG));
    } catch (MediaPackageException e) {
      fail("Media package excpetion while reading media package from manifest: " + e.getMessage());
    } catch (ConfigurationException e) {
      fail("Configuration exception while reading media package from manifest: " + e.getMessage());
    } catch (FileNotFoundException e) {
      fail("Configuration exception while reading media package from manifest: " + e.getMessage());
    }
  }

}