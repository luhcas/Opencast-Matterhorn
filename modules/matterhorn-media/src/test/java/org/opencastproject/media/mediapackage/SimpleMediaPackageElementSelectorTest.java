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

import org.opencastproject.media.mediapackage.selector.SimpleMediaPackageElementSelector;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test case for {@link SimpleMediaPackageElementSelector}.
 */
public class SimpleMediaPackageElementSelectorTest extends AbstractMediaPackageTest {

  /** The selector to be tested */
  SimpleMediaPackageElementSelector<Track> selector = null;
  
  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    super.setUp();
    selector = new SimpleMediaPackageElementSelector<Track>();
  }

  /**
   * Test method for {@link org.opencastproject.media.mediapackage.selector.SimpleMediaPackageElementSelector#select(org.opencastproject.media.mediapackage.MediaPackage)}.
   */
  @Test @Ignore
  public void testSelect() {
    assertEquals(mediaPackage.getElements().length, selector.select(mediaPackage).size());
  }

  /**
   * Test method for {@link org.opencastproject.media.mediapackage.selector.SimpleMediaPackageElementSelector#setFlavors(java.util.List)}.
   */
  @Test @Ignore
  public void testSetFlavors() {
    fail("Not yet implemented"); // TODO
  }

  /**
   * Test method for {@link org.opencastproject.media.mediapackage.selector.SimpleMediaPackageElementSelector#addFlavor(org.opencastproject.media.mediapackage.MediaPackageElementFlavor)}.
   */
  @Test @Ignore
  public void testAddFlavorMediaPackageElementFlavor() {
    fail("Not yet implemented"); // TODO
  }

  /**
   * Test method for {@link org.opencastproject.media.mediapackage.selector.SimpleMediaPackageElementSelector#addFlavor(java.lang.String)}.
   */
  @Test @Ignore
  public void testAddFlavorString() {
    fail("Not yet implemented"); // TODO
  }

  /**
   * Test method for {@link org.opencastproject.media.mediapackage.selector.SimpleMediaPackageElementSelector#addFlavorAt(int, org.opencastproject.media.mediapackage.MediaPackageElementFlavor)}.
   */
  @Test @Ignore
  public void testAddFlavorAtIntMediaPackageElementFlavor() {
    fail("Not yet implemented"); // TODO
  }

  /**
   * Test method for {@link org.opencastproject.media.mediapackage.selector.SimpleMediaPackageElementSelector#addFlavorAt(int, java.lang.String)}.
   */
  @Test @Ignore
  public void testAddFlavorAtIntString() {
    fail("Not yet implemented"); // TODO
  }

  /**
   * Test method for {@link org.opencastproject.media.mediapackage.selector.SimpleMediaPackageElementSelector#removeFlavor(org.opencastproject.media.mediapackage.MediaPackageElementFlavor)}.
   */
  @Test @Ignore
  public void testRemoveFlavorMediaPackageElementFlavor() {
    fail("Not yet implemented"); // TODO
  }

  /**
   * Test method for {@link org.opencastproject.media.mediapackage.selector.SimpleMediaPackageElementSelector#removeFlavor(java.lang.String)}.
   */
  @Test @Ignore
  public void testRemoveFlavorString() {
    fail("Not yet implemented"); // TODO
  }

  /**
   * Test method for {@link org.opencastproject.media.mediapackage.selector.SimpleMediaPackageElementSelector#removeFlavorAt(int)}.
   */
  @Test @Ignore
  public void testRemoveFlavorAt() {
    fail("Not yet implemented"); // TODO
  }

  /**
   * Test method for {@link org.opencastproject.media.mediapackage.selector.SimpleMediaPackageElementSelector#includeTag(java.lang.String)}.
   */
  @Test @Ignore
  public void testIncludeTag() {
    fail("Not yet implemented"); // TODO
  }

  /**
   * Test method for {@link org.opencastproject.media.mediapackage.selector.SimpleMediaPackageElementSelector#excludeTag(java.lang.String)}.
   */
  @Test @Ignore
  public void testExcludeTag() {
    fail("Not yet implemented"); // TODO
  }

  /**
   * Test method for {@link org.opencastproject.media.mediapackage.selector.SimpleMediaPackageElementSelector#clearTags()}.
   */
  @Test @Ignore
  public void testClearTags() {
    fail("Not yet implemented"); // TODO
  }

}
