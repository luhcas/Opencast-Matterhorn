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
import static org.junit.Assert.fail;

import org.opencastproject.media.bundle.BundleElement;
import org.opencastproject.media.bundle.BundleElementBuilder;
import org.opencastproject.media.bundle.BundleElementBuilderFactory;
import org.opencastproject.media.bundle.BundleElementFlavor;
import org.opencastproject.media.bundle.BundleException;
import org.opencastproject.media.bundle.Catalog;
import org.opencastproject.media.bundle.DublinCoreCatalog;
import org.opencastproject.media.bundle.PresenterTrack;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Node;

import java.io.File;
import java.io.IOException;

/**
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public class BundleElementBuilderTest {

  /** The bundle builder */
  BundleElementBuilder bundleElementBuilder = null;

  /** The catalog name */
  public static String catalogName = "/dublincore.xml";

  /** The test catalog */
  private File catalogFile = null;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    bundleElementBuilder = BundleElementBuilderFactory.newInstance()
        .newElementBuilder();
  }

  /**
   * Test method for
   * {@link org.opencastproject.media.bundle.BundleElementBuilderImpl#elementFromFile(File file)}
   * .
   */
  @Test
  public void testElementFromFile() {
    catalogFile = new File(this.getClass().getResource(catalogName).getPath());
    BundleElement element = null;
    try {
      element = bundleElementBuilder.elementFromFile(catalogFile);
      assertEquals(element.getElementType(), Catalog.TYPE);
      assertEquals(element.getFlavor(), DublinCoreCatalog.FLAVOR);
    } catch (BundleException e) {
      fail(e.getMessage());
    }
  }

  /**
   * Test method for
   * {@link org.opencastproject.media.bundle.BundleElementBuilderImpl#elementFromFile(File file, BundleElement.Type type, BundleElementFlavor flavor)}
   * .
   */
  @Test
  public void testElementFromFileWithHints() {
    catalogFile = new File(this.getClass().getResource(catalogName).getPath());

    // Test correct hints
    try {
      bundleElementBuilder.elementFromFile(catalogFile,
          BundleElement.Type.Catalog, DublinCoreCatalog.FLAVOR);
      bundleElementBuilder.elementFromFile(catalogFile,
          BundleElement.Type.Catalog, null);
      bundleElementBuilder.elementFromFile(catalogFile, null,
          DublinCoreCatalog.FLAVOR);
      bundleElementBuilder.elementFromFile(catalogFile, null, null);
    } catch (BundleException e) {
      fail(e.getMessage());
    }

    // Test incorrect hints
    try {
      bundleElementBuilder.elementFromFile(catalogFile,
          BundleElement.Type.Track, DublinCoreCatalog.FLAVOR);
      fail("Specified type was wrong but didn't matter");
    } catch (BundleException e) {
      // Expected
    }

    // Test incorrect flavor
    try {
      bundleElementBuilder.elementFromFile(catalogFile,
          BundleElement.Type.Catalog, PresenterTrack.FLAVOR);
      fail("Specified flavor was wrong but didn't matter");
    } catch (BundleException e) {
      // Expected
    }

  }

  /**
   * Test method for
   * {@link org.opencastproject.media.bundle.BundleElementBuilderImpl#elementFromManifest(Node elementNode, File bundleRoot, boolean verify)}
   * .
   */
  @Test
  public void testElementFromManifest() {
    System.out.println("Not yet implemented");
  }

  /**
   * Test method for
   * {@link org.opencastproject.media.bundle.BundleElementBuilderImpl#newElement(BundleElement.Type type, BundleElementFlavor flavor)}
   * .
   */
  @Test
  public void testNewElement() {
    try {
      bundleElementBuilder.newElement(BundleElement.Type.Catalog,
          DublinCoreCatalog.FLAVOR);
    } catch (IOException e) {
      fail(e.getMessage());
    }
  }

}