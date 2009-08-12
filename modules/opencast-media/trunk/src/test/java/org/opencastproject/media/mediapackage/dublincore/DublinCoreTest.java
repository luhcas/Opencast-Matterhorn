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

package org.opencastproject.media.mediapackage.dublincore;

import static org.opencastproject.media.mediapackage.dublincore.DublinCore.ENC_SCHEME_URI;
import static org.opencastproject.media.mediapackage.dublincore.DublinCore.LANGUAGE_ANY;
import static org.opencastproject.media.mediapackage.dublincore.DublinCore.LANGUAGE_UNDEFINED;
import static org.opencastproject.media.mediapackage.dublincore.DublinCore.PROPERTY_CONTRIBUTOR;
import static org.opencastproject.media.mediapackage.dublincore.DublinCore.PROPERTY_IDENTIFIER;
import static org.opencastproject.media.mediapackage.dublincore.DublinCore.PROPERTY_LICENSE;
import static org.opencastproject.media.mediapackage.dublincore.DublinCore.PROPERTY_PUBLISHER;
import static org.opencastproject.media.mediapackage.dublincore.DublinCore.PROPERTY_TITLE;
import static org.opencastproject.media.mediapackage.dublincore.DublinCoreCatalogImpl.PROPERTY_PROMOTED;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.opencastproject.media.mediapackage.DublinCoreCatalog;
import org.opencastproject.media.mediapackage.EName;
import org.opencastproject.media.mediapackage.NamespaceBindingException;
import org.opencastproject.media.mediapackage.dublincore.DublinCore;
import org.opencastproject.media.mediapackage.dublincore.DublinCoreCatalogImpl;
import org.opencastproject.media.mediapackage.dublincore.DublinCoreValue;
import org.opencastproject.util.UnknownFileTypeException;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * Test class for the dublin core implementation.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @author Christoph E. Driessen <ced@neopoly.de>
 * @version $Id: DublinCoreTest.java 238 2009-07-29 09:53:32Z jholtzman $
 */
public class DublinCoreTest {

  /**
   * The catalog name
   */
  public static String catalogName = "/dublincore.xml";

  /**
   * The test catalog
   */
  private File catalogFile = null;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    catalogFile = new File(this.getClass().getResource(catalogName).getPath());
    if (!catalogFile.exists() || !catalogFile.canRead())
      throw new Exception("Unable to access test catalog");
  }

  /**
   * Test method for
   * {@link org.opencastproject.media.mediapackage.dublincore.DublinCoreCatalogImpl#fromFile(java.io.File)} .
   */
  @Test
  public void testFromFile() {
    DublinCoreCatalog dc = parse(catalogFile);

    // Check if the fields are available
    assertEquals("ETH Zurich, Switzerland", dc.getFirst(PROPERTY_PUBLISHER, LANGUAGE_UNDEFINED));
    assertEquals("Land and Vegetation: Key players on the Climate Scene", dc.getFirst(PROPERTY_TITLE, "en"));
    assertNotNull(dc.getFirst(PROPERTY_TITLE));
    assertNull(dc.getFirst(PROPERTY_TITLE, "fr"));
    // Test custom metadata element
    assertEquals("true", dc.getFirst(PROPERTY_PROMOTED));
  }

  /**
   * Test method for {@link org.opencastproject.media.mediapackage.dublincore.DublinCoreCatalogImpl#save()} .
   */
  @Test
  public void testNewInstance() {
    // Read the sample catalog
    DublinCoreCatalog dcSample = parse(catalogFile);

    // Create a new catalog and fill it with a few fields
    DublinCoreCatalog dcNew = null;
    try {
      dcNew = DublinCoreCatalogImpl.newInstance();

      // Add the required fields
      dcNew.add(PROPERTY_IDENTIFIER, dcSample.getFirst(PROPERTY_IDENTIFIER));
      dcNew.add(PROPERTY_TITLE, dcSample.getFirst(PROPERTY_TITLE, "en"), "en");

      // Add an additional field
      dcNew.add(PROPERTY_PUBLISHER, dcSample.getFirst(PROPERTY_PUBLISHER));

      // Add a null-value field
      try {
        dcNew.add(PROPERTY_CONTRIBUTOR, (String) null);
        fail();
      } catch (IllegalArgumentException e) {
      }

      // Add a field with an encoding scheme
      dcNew.add(PROPERTY_LICENSE, new DublinCoreValue("http://www.opencastproject.org/license",
              DublinCore.LANGUAGE_UNDEFINED, ENC_SCHEME_URI));
      // Don't forget to bind the namespace...
      dcNew.bindPrefix("octest", "http://www.opencastproject.org");
      dcNew.add(PROPERTY_PROMOTED, new DublinCoreValue("true", DublinCore.LANGUAGE_UNDEFINED, new EName(
              "http://www.opencastproject.org", "Boolean")));
      try {
        dcNew.add(PROPERTY_PROMOTED, new DublinCoreValue("true", DublinCore.LANGUAGE_UNDEFINED, new EName(
                "http://www.opencastproject.org/enc-scheme", "Boolean")));
        fail();
      } catch (NamespaceBindingException e) {
        // Ok. This exception is expected to occur
      }

      // Store the document
      dcNew.save();
    } catch (NoSuchAlgorithmException e) {
      fail("Error verifying the catalog checksum: " + e.getMessage());
    } catch (IOException e) {
      fail("Error creating the catalog: " + e.getMessage());
    } catch (UnknownFileTypeException e) {
      fail("The catalog's mime type is not supported: " + e.getMessage());
    } catch (ParserConfigurationException e) {
      fail("Error creating a parser for the catalog: " + e.getMessage());
    } catch (TransformerException e) {
      fail("Error saving the catalog: " + e.getMessage());
    }

    // Re-read the saved catalog and test for its content
    DublinCoreCatalog dcNewFromDisk = parse(dcNew.getFile());
    assertEquals(dcSample.getFirst(PROPERTY_IDENTIFIER), dcNewFromDisk.getFirst(PROPERTY_IDENTIFIER));
    assertEquals(dcSample.getFirst(PROPERTY_TITLE, "en"), dcNewFromDisk.getFirst(PROPERTY_TITLE, "en"));
    assertEquals(dcSample.getFirst(PROPERTY_PUBLISHER), dcNewFromDisk.getFirst(PROPERTY_PUBLISHER));
  }

  /**
   * Test method for {@link org.opencastproject.media.mediapackage.dublincore.DublinCoreCatalogImpl#save()} .
   */
  @Test(expected = IllegalStateException.class)
  public void testRequiredFields() {
    // Read the sample catalog
    DublinCoreCatalog dcSample = parse(catalogFile);

    // Create a new catalog and fill it with a few fields
    DublinCoreCatalog dcNew = null;
    try {
      dcNew = DublinCoreCatalogImpl.newInstance();

      // Add the required fields but the title
      dcNew.set(PROPERTY_IDENTIFIER, dcSample.getFirst(PROPERTY_IDENTIFIER));

      // Add an additional field
      dcNew.set(PROPERTY_PUBLISHER, dcSample.getFirst(PROPERTY_PUBLISHER));

      // Store the document
      (dcNew).save();
      fail("Required field was missing but not reported!");
    } catch (NoSuchAlgorithmException e) {
      fail("Error verifying the catalog checksum: " + e.getMessage());
    } catch (IOException e) {
      fail("Error creating the catalog: " + e.getMessage());
    } catch (UnknownFileTypeException e) {
      fail("The catalog's mime type is not supported: " + e.getMessage());
    } catch (ParserConfigurationException e) {
      fail("Error creating a parser for the catalog: " + e.getMessage());
    } catch (TransformerException e) {
      fail("Error saving the catalog: " + e.getMessage());
    }
  }

  /**
   * Tests overwriting of values.
   */
  @Test
  public void testOverwriting() {
    // Create a new catalog and fill it with a few fields
    DublinCoreCatalog dcNew = null;
    try {
      dcNew = DublinCoreCatalogImpl.newInstance();
      dcNew.set(PROPERTY_TITLE, "Title 1");
      assertEquals("Title 1", dcNew.getFirst(PROPERTY_TITLE));

      dcNew.set(PROPERTY_TITLE, "Title 2");
      assertEquals("Title 2", dcNew.getFirst(PROPERTY_TITLE));

      dcNew.set(PROPERTY_TITLE, "Title 3", "de");
      assertEquals("Title 2", dcNew.getFirst(PROPERTY_TITLE));
      assertEquals("Title 3", dcNew.getFirst(PROPERTY_TITLE, "de"));
      dcNew = null;
    } catch (NoSuchAlgorithmException e) {
      fail("Error verifying the catalog checksum: " + e.getMessage());
    } catch (IOException e) {
      fail("Error creating the catalog: " + e.getMessage());
    } catch (UnknownFileTypeException e) {
      fail("The catalog's mime type is not supported: " + e.getMessage());
    }
  }

  @Test
  public void testVarious() throws NoSuchAlgorithmException, IOException, UnknownFileTypeException {
    DublinCoreCatalog dc = DublinCoreCatalogImpl.newInstance();
    // Add a title
    dc.add(PROPERTY_TITLE, "Der alte Mann und das Meer");
    assertEquals("Der alte Mann und das Meer", dc.getFirst(PROPERTY_TITLE));
    assertEquals(1, dc.get(PROPERTY_TITLE, LANGUAGE_UNDEFINED).size());
    assertEquals(1, dc.get(PROPERTY_TITLE).size());
    // Overwrite the title
    dc.set(PROPERTY_TITLE, "La Peste");
    assertEquals("La Peste", dc.getFirst(PROPERTY_TITLE));
    assertEquals(1, dc.get(PROPERTY_TITLE).size());

    dc.set(PROPERTY_TITLE, "Die Pest", "de");
    assertEquals(2, dc.get(PROPERTY_TITLE).size());
    assertEquals(1, dc.get(PROPERTY_TITLE, LANGUAGE_UNDEFINED).size());

    // Remove the title without language code
    dc.remove(PROPERTY_TITLE, LANGUAGE_UNDEFINED);
    // The german title is now the only remaining title so we should get it here
    assertEquals("Die Pest", dc.getFirst(PROPERTY_TITLE));
    assertNotNull(dc.getFirst(PROPERTY_TITLE, "de"));
    assertNull(dc.getFirst(PROPERTY_TITLE, "fr"));
    assertEquals(1, dc.get(PROPERTY_TITLE).size());
    assertTrue(dc.hasValue(PROPERTY_TITLE));
    assertFalse(dc.hasMultipleValues(PROPERTY_TITLE));

    // Add a german title (does not make sense...)
    dc.add(PROPERTY_TITLE, "nonsense", "de");
    assertEquals(2, dc.get(PROPERTY_TITLE, "de").size());
    assertEquals(2, dc.get(PROPERTY_TITLE).size());

    // Now restore the orginal french title
    dc.set(PROPERTY_TITLE, "La Peste");
    assertEquals(3, dc.get(PROPERTY_TITLE).size());

    // And get rid of the german ones
    dc.remove(PROPERTY_TITLE, "de");
    assertEquals(0, dc.get(PROPERTY_TITLE, "de").size());
    assertNull(dc.getFirst(PROPERTY_TITLE, "de"));
    assertEquals(1, dc.get(PROPERTY_TITLE).size());

    // No contributor is set so expect null
    assertNull(dc.getFirst(PROPERTY_CONTRIBUTOR));
    // ... but expect an empty list here
    assertNotNull(dc.get(PROPERTY_CONTRIBUTOR));
  }

  @Test
  public void testVarious2() throws NoSuchAlgorithmException, IOException, UnknownFileTypeException {
    DublinCoreCatalog dc = DublinCoreCatalogImpl.newInstance();
    dc.add(PROPERTY_TITLE, "The Lord of the Rings");
    dc.add(PROPERTY_TITLE, "Der Herr der Ringe", "de");
    assertEquals(2, dc.getLanguages(PROPERTY_TITLE).size());

    assertEquals("The Lord of the Rings; Der Herr der Ringe", dc.getAsText(PROPERTY_TITLE, LANGUAGE_ANY, "; "));
    assertNull(dc.getAsText(PROPERTY_CONTRIBUTOR, LANGUAGE_ANY, "; "));

    dc.remove(PROPERTY_TITLE, "de");
    assertEquals(1, dc.getLanguages(PROPERTY_TITLE).size());

    dc.remove(PROPERTY_TITLE);

    assertNull(dc.getAsText(PROPERTY_TITLE, LANGUAGE_ANY, "; "));
  }

  @Test
  public void testVarious3() throws NoSuchAlgorithmException, IOException, UnknownFileTypeException {
    DublinCoreCatalog dc = DublinCoreCatalogImpl.newInstance();
    dc.add(PROPERTY_CONTRIBUTOR, "Heinz Strunk");
    dc.add(PROPERTY_CONTRIBUTOR, "Rocko Schamoni");
    dc.add(PROPERTY_CONTRIBUTOR, "Jacques Palminger");
    assertTrue(dc.hasValue(PROPERTY_CONTRIBUTOR));
    assertTrue(dc.hasValue(PROPERTY_CONTRIBUTOR, LANGUAGE_UNDEFINED));
    // assertTrue(dc.hasMultipleValues(PROPERTY_TITLE));
    assertEquals(3, dc.get(PROPERTY_CONTRIBUTOR).size());

    dc.add(PROPERTY_CONTRIBUTOR, "Klaus Allofs", "de");
    dc.add(PROPERTY_CONTRIBUTOR, "Karl-Heinz Rummenigge", "de");
    assertTrue(dc.hasValue(PROPERTY_CONTRIBUTOR, "de"));
    assertTrue(dc.hasMultipleValues(PROPERTY_CONTRIBUTOR, "de"));
    assertEquals(2, dc.get(PROPERTY_CONTRIBUTOR, "de").size());
    assertEquals(5, dc.get(PROPERTY_CONTRIBUTOR).size());

    dc.set(PROPERTY_CONTRIBUTOR, "Hans Manzke");
    assertEquals(1, dc.get(PROPERTY_CONTRIBUTOR, LANGUAGE_UNDEFINED).size());
    assertEquals(3, dc.get(PROPERTY_CONTRIBUTOR).size());
  }

  @Test
  public void testVarious4() throws NoSuchAlgorithmException, IOException, UnknownFileTypeException {
    DublinCoreCatalog dc = DublinCoreCatalogImpl.newInstance();
    dc.add(PROPERTY_TITLE, "deutsch", "de");
    dc.add(PROPERTY_TITLE, "english", "en");
    assertNull(dc.getFirst(PROPERTY_TITLE, LANGUAGE_UNDEFINED));
    assertNotNull(dc.getFirst(PROPERTY_TITLE, LANGUAGE_ANY));
    assertNotNull(dc.getFirst(PROPERTY_TITLE));

    dc.add(PROPERTY_TITLE, "undefined");
    assertEquals("undefined", dc.getFirst(PROPERTY_TITLE, LANGUAGE_UNDEFINED));
    assertEquals("undefined", dc.getFirst(PROPERTY_TITLE));
    assertEquals("deutsch", dc.getFirst(PROPERTY_TITLE, "de"));
  }

  /**
   * Parses the test catalog.
   * 
   * @param file
   *          the file containing the catalog
   * @return the dublin core object representation
   */
  private DublinCoreCatalog parse(File file) {
    DublinCoreCatalog dc;
    try {
      dc = DublinCoreCatalogImpl.fromFile(file);
      return dc;
    } catch (NoSuchAlgorithmException e) {
      fail("Error verifying the catalog checksum: " + e.getMessage());
    } catch (IOException e) {
      fail("Error accessing the catalog: " + e.getMessage());
    } catch (UnknownFileTypeException e) {
      fail("The catalog's mime type is not supported: " + e.getMessage());
    } catch (ParserConfigurationException e) {
      fail("Error creating a parser for the catalog: " + e.getMessage());
    } catch (SAXException e) {
      fail("Error parsing the catalog: " + e.getMessage());
    }
    throw new IllegalStateException("Shouldn't get here!");
  }
}