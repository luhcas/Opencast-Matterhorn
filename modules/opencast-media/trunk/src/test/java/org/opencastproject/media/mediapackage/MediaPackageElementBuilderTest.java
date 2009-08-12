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
import static org.junit.Assert.fail;

import org.opencastproject.media.mediapackage.dublincore.DublinCoreTest;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Node;

import java.io.File;
import java.io.IOException;

/**
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: MediaPackageElementBuilderTest.java 2905 2009-07-15 16:16:05Z ced $
 */
public class MediaPackageElementBuilderTest {

  /** The media package builder */
  MediaPackageElementBuilder mediaPackageElementBuilder = null;

  /** The test catalog */
  private File catalogFile = null;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    mediaPackageElementBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
  }

  /**
   * Test method for
   * {@link org.opencastproject.media.mediapackage.MediaPackageElementBuilderImpl#elementFromFile(File file)}.
   */
  @Test
  public void testElementFromFile() {
    catalogFile = new File(DublinCoreTest.class.getResource("/dublincore.xml").getPath());
    MediaPackageElement element = null;
    try {
      element = mediaPackageElementBuilder.elementFromFile(catalogFile);
      assertEquals(element.getElementType(), Catalog.TYPE);
      assertEquals(element.getFlavor(), MediaPackageElements.DUBLINCORE_CATALOG);
    } catch (MediaPackageException e) {
      fail(e.getMessage());
    }
  }

  /**
   * Test method for
   * {@link org.opencastproject.media.mediapackage.MediaPackageElementBuilderImpl#elementFromFile(File file, org.opencastproject.media.mediapackage.MediaPackageElement.Type type, MediaPackageElementFlavor flavor)}
   * .
   */
  @Test
  public void testElementFromFileWithHints() {
    catalogFile = new File(DublinCoreTest.class.getResource("/dublincore.xml").getPath());

    // Test correct hints
    try {
      mediaPackageElementBuilder.elementFromFile(catalogFile, MediaPackageElement.Type.Catalog,
              MediaPackageElements.DUBLINCORE_CATALOG);
      mediaPackageElementBuilder.elementFromFile(catalogFile, MediaPackageElement.Type.Catalog, null);
      mediaPackageElementBuilder.elementFromFile(catalogFile, null, MediaPackageElements.DUBLINCORE_CATALOG);
      mediaPackageElementBuilder.elementFromFile(catalogFile, null, null);
    } catch (MediaPackageException e) {
      fail(e.getMessage());
    }

    // Test incorrect hints
    try {
      mediaPackageElementBuilder.elementFromFile(catalogFile, MediaPackageElement.Type.Track,
              MediaPackageElements.DUBLINCORE_CATALOG);
      fail("Specified type was wrong but didn't matter");
    } catch (MediaPackageException e) {
      // Expected
    }

    // Test incorrect flavor
    try {
      mediaPackageElementBuilder.elementFromFile(catalogFile, MediaPackageElement.Type.Catalog,
              MediaPackageElements.PRESENTER_TRACK);
      fail("Specified flavor was wrong but didn't matter");
    } catch (MediaPackageException e) {
      // Expected
    }

  }

  /**
   * Test method for
   * {@link org.opencastproject.media.mediapackage.MediaPackageElementBuilderImpl#elementFromManifest(Node elementNode, File packageRoot, boolean verify)}
   * .
   */
  @Test
  public void testElementFromManifest() {
    System.out.println("Not yet implemented");
  }

  /**
   * Test method for
   * {@link org.opencastproject.media.mediapackage.MediaPackageElementBuilderImpl#newElement(org.opencastproject.media.mediapackage.MediaPackageElement.Type type, MediaPackageElementFlavor flavor)}
   * .
   */
  @Test
  public void testNewElement() {
    try {
      mediaPackageElementBuilder.newElement(MediaPackageElement.Type.Catalog, MediaPackageElements.DUBLINCORE_CATALOG);
    } catch (IOException e) {
      fail(e.getMessage());
    }
  }

}