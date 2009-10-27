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

import junit.framework.Assert;

import org.junit.Test;

import java.io.File;

public class WorkspaceImplTest {
  @Test
  public void testGetRemoteFile() {
    WorkspaceImpl workspace = new WorkspaceImpl("target/junit-workspace-rootdir");
    File f = workspace.get(getClass().getClassLoader().getResource("opencast_header.gif"));
    Assert.assertTrue(f.exists());
  }

  @Test
  public void testLocalFileUrls() throws Exception {
    WorkspaceImpl workspace = new WorkspaceImpl();
    File source1 = new File("target/test-classes/opencast_header.gif");
    File source2 = new File("target/test-classes/opencast_header2.gif");
    File fromWorkspace1 = workspace.get(source1.toURI().toURL());
    File fromWorkspace2 = workspace.get(source2.toURI().toURL());
    Assert.assertFalse(fromWorkspace1.getAbsolutePath().equals(fromWorkspace2.getAbsolutePath()));
  }
}
