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
package org.opencastproject.workspace.impl;

import static org.junit.Assert.fail;

import junit.framework.Assert;

import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

public class WorkspaceImplTest {
  @Test
  public void testGetRemoteFile() {
    WorkspaceImpl workspace = new WorkspaceImpl("target/junit-workspace-rootdir");
    URL fileURL = getClass().getClassLoader().getResource("opencast_header.gif");
    File f = null;
    try {
      f = workspace.get(fileURL.toURI());
      Assert.assertTrue(f.exists());
    } catch (URISyntaxException e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testLocalFileUrls() throws Exception {
    WorkspaceImpl workspace = new WorkspaceImpl("target/junit-workspace-rootdir");
    File source1 = new File("target/test-classes/opencast_header.gif");
    File source2 = new File("target/test-classes/opencast_header2.gif");
    File fromWorkspace1 = workspace.get(source1.toURI());
    File fromWorkspace2 = workspace.get(source2.toURI());
    Assert.assertFalse(fromWorkspace1.getAbsolutePath().equals(fromWorkspace2.getAbsolutePath()));
  }
  
  @Test
  public void testLongFilenames() throws Exception {
    WorkspaceImpl workspace = new WorkspaceImpl("target/junit-workspace-rootdir");
    File source = new File("target/test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/opencast_header.gif");
    URL urlToSource = source.toURI().toURL();
    Assert.assertTrue(urlToSource.toString().length() > 255);
    Assert.assertNotNull(workspace.get(urlToSource.toURI()));
  }
}
