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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.opencastproject.media.bundle.handle.Handle;
import org.opencastproject.media.bundle.handle.HandleBuilder;
import org.opencastproject.media.bundle.handle.HandleBuilderFactory;
import org.opencastproject.media.bundle.handle.HandleException;
import org.opencastproject.util.IdBuilderFactory;
import org.opencastproject.util.SerialIdBuilder;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Test case to make sure creation of handle is working as expected.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public class HandleBuilderTest {

  /** The handle builder */
  private HandleBuilder handleBuilder = null;

  /** The handle url */
  private URL url = null;

  /** The handle naming authority */
  private static final String namingAuthority = "00000";

  /** The handle local name */
  private static final String localName = "0001";

  /** The handle value */
  private static final String handleId = namingAuthority + "/" + localName;

  /** List of created handles */
  List<Handle> newHandles = new ArrayList<Handle>();

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    System.setProperty(IdBuilderFactory.PROPERTY_NAME, SerialIdBuilder.class
        .getName());
    handleBuilder = HandleBuilderFactory.newInstance().newHandleBuilder();
    assertNotNull(handleBuilder);
    url = new URL("http://www.opencastproject.org");
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
   * {@link org.opencastproject.media.bundle.handle.HandleBuilderImpl#createNew()}
   * .
   */
  @Test
  public void testCreateNew() {
    Handle handle = null;
    try {
      handle = handleBuilder.createNew();
      newHandles.add(handle);
      assertNotNull(handle);
      assertNotNull(handle.getNamingAuthority());
      assertNotNull(handle.getLocalName());
      assertNotNull(handle.resolve());
    } catch (HandleException e) {
      fail("Error creating handle: " + e.getMessage());
    }
  }

  /**
   * Test method for
   * {@link org.opencastproject.media.bundle.handle.HandleBuilderImpl#createNew(java.net.URL)}
   * .
   */
  @Test
  public void testCreateNewURL() {
    Handle handle = null;
    try {
      handle = handleBuilder.createNew(url);
      newHandles.add(handle);
      assertNotNull(handle);
      assertNotNull(handle.getNamingAuthority());
      assertNotNull(handle.getLocalName());
      assertNotNull(handle.resolve());
      assertEquals(url, handle.resolve());
    } catch (HandleException e) {
      fail("Error creating handle: " + e.getMessage());
    }
  }

  /**
   * Test method for
   * {@link org.opencastproject.media.bundle.handle.HandleBuilderImpl#fromValue(java.lang.String)}
   * .
   */
// FIXME @Test
  public void testFromValue() {
    Handle handle = null;
    String[] tests = new String[] { localName, handleId, "10." + handleId,
        "hdl://10." + handleId, };
    try {
      for (String t : tests) {
        handle = handleBuilder.fromValue(t);
        assertNotNull(handle);
        assertEquals(localName, handle.getLocalName());
      }

      // hdl://10127/abc (should fail)
      try {
        handle = handleBuilder.fromValue("hdl://" + handleId);
        fail("hdl://" + handleId + " should fail");
      } catch (HandleException e) {
        // is expected
      }

    } catch (HandleException e) {
      fail("Error creating handle: " + e.getMessage());
    }
  }

  /**
   * Test method for
   * {@link org.opencastproject.media.bundle.handle.HandleBuilderImpl#update(Handle, java.net.URL)}
   * .
   */
  @Test
  public void testUpdate() {
    try {
      Handle newHandle = handleBuilder.createNew(url);
      newHandles.add(newHandle);

      // Create new target and update
      URL newTarget = new URL("http://www.apple.com");
      boolean updated = handleBuilder.update(newHandle, newTarget);
      assertTrue(updated);

      // TODO: Our handle server at the moment does caching in the
      // webservice, so this test always fails, although resolving
      // is working properly
      // URL resolvedUrl = handleBuilder.resolve(newHandle);
      // assertEquals(newTarget, resolvedUrl);
    } catch (HandleException e) {
      fail("Error updating handle: " + e.getMessage());
    } catch (MalformedURLException e) {
      fail("Error creating new handle url: " + e.getMessage());
    }
  }

}