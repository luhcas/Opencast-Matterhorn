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

package org.opencastproject.media.bundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.opencastproject.media.bundle.handle.Handle;
import org.opencastproject.media.bundle.handle.HandleBuilder;
import org.opencastproject.media.bundle.handle.HandleBuilderImpl;
import org.opencastproject.media.bundle.handle.HandleException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Test case for the handle class implementation.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public class HandleTest {

  /** The handle builder */
  private HandleBuilder handleBuilder = null;

  /** The handle */
  Handle handle = null;

  /** List of created handles */
  List<Handle> newHandles = new ArrayList<Handle>();

  /** The url */
  URL url = null;

  /** Handle local name */
  String defaultLocalName = "0001";

  /** The handle value */
  String handleValue = "http://www.replay.ethz.ch";

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    handleBuilder = new HandleBuilderImpl();
    url = new URL(handleValue);
    handle = handleBuilder.fromValue(defaultLocalName);
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
    for (Handle h : newHandles) {
      try {
        handleBuilder.delete(h);
      } catch (HandleException e) {
        fail("Error deleting handle " + h + ": " + e.getMessage());
      }
    }
  }

  /**
   * Test method for
   * {@link org.opencastproject.media.bundle.handle.HandleImpl#getLocalName()}.
   */
  @Test
  public void testGetLocalName() {
    assertNotNull(handle.getLocalName());
  }

  /**
   * Test method for
   * {@link org.opencastproject.media.bundle.handle.HandleImpl#getNamingAuthority()}
   * .
   */
  @Test
  public void testGetNamingAuthority() {
    assertNotNull(handle.getNamingAuthority());
  }

  /**
   * Test method for
   * {@link org.opencastproject.media.bundle.handle.HandleImpl#toString()}.
   */
  @Test
  public void testToString() {
    StringBuffer fullName = new StringBuffer();
    fullName.append(handle.getNamingAuthority());
    fullName.append("/");
    fullName.append(handle.getLocalName());
    assertEquals(fullName.toString(), handle.toString());
  }

  /**
   * Test method for
   * {@link org.opencastproject.media.bundle.handle.HandleImpl#resolve()}.
   */
  @Test
  public void testGetValue() {
    try {
      Handle newHandle = handleBuilder.createNew(url);
      newHandles.add(newHandle);
      URL resolvedUrl = newHandle.resolve();
      assertEquals(url, resolvedUrl);
    } catch (HandleException e) {
      fail("Error resolving handle: " + e.getMessage());
    }
  }

  /**
   * Test method for
   * {@link org.opencastproject.media.bundle.handle.HandleImpl#update(java.net.URL)}
   * .
   */
  @Test
  public void testUpdate() {
    try {
      URL newTarget = new URL("http://www.apple.com");
      Handle newHandle = handleBuilder.createNew(url);
      newHandles.add(newHandle);
      newHandle.update(newTarget);
      URL resolvedUrl = newHandle.resolve();
      assertEquals(newTarget, resolvedUrl);
    } catch (HandleException e) {
      fail("Error updating handle: " + e.getMessage());
    } catch (MalformedURLException e) {
      fail("Error creating new handle url: " + e.getMessage());
    }
  }

}