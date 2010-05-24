/**
 *  Copyright 2009, 2010 The Regents of the University of California
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

import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;
import org.opencastproject.workspace.api.NotFoundException;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpUriRequest;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

public class WorkspaceImplTest {
  WorkspaceImpl workspace;
  
  private static final String workspaceRoot = "." + File.separator + "target" + File.separator + "junit-workspace-rootdir";
  private static final String repoRoot = "." + File.separator + "target" + File.separator + "junit-repo-rootdir";

  @Before
  public void setup() throws Exception {
    workspace = new WorkspaceImpl(workspaceRoot);    
    workspace.activate(null);
  }
  
  @After
  public void tearDown() throws Exception {
    workspace.deactivate();
    FileUtils.deleteDirectory(new File(workspaceRoot));
    FileUtils.deleteDirectory(new File(repoRoot));
  }

  @Test
  public void testGetRemoteFile() throws Exception {
    URL fileURL = getClass().getClassLoader().getResource("opencast_header.gif");
    File f = null;
    f = workspace.get(fileURL.toURI());
    Assert.assertTrue(f.exists());
  }

  @Test
  public void testLocalFileUrls() throws Exception {
    File source1 = new File("target/test-classes/opencast_header.gif");
    File source2 = new File("target/test-classes/opencast_header2.gif");
    File fromWorkspace1 = workspace.get(source1.toURI());
    File fromWorkspace2 = workspace.get(source2.toURI());
    Assert.assertFalse(fromWorkspace1.getAbsolutePath().equals(fromWorkspace2.getAbsolutePath()));
  }
  
  @Test
  public void testLongFilenames() throws Exception {
    File source = new File("target/test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/opencast_header.gif");
    URL urlToSource = source.toURI().toURL();
    Assert.assertTrue(urlToSource.toString().length() > 255);
    try {
      Assert.assertNotNull(workspace.get(urlToSource.toURI()));
    } catch (NotFoundException e) {
      //This happens on some machines, so we catch and handle it.
    }
  }
  
  // Calls to put() should put the file into the working file repository, but not in the local cache if there's a valid
  // filesystem mapping present
  @Test
  public void testPutCachingWithFilesystemMapping() throws Exception {
    // First, mock up the collaborating working file repository
    WorkingFileRepository repo = EasyMock.createMock(WorkingFileRepository.class);
    EasyMock.expect(repo.put((String)EasyMock.anyObject(), (String)EasyMock.anyObject(), (String)EasyMock.anyObject(), (InputStream)EasyMock.anyObject())).andReturn(new URI("http://localhost:8080/files/mp/foo/bar/header.gif"));
    EasyMock.replay(repo);

    workspace.setRepository(repo);
    workspace.filesystemMappings.clear();
    workspace.filesystemMappings.put("http://localhost:8080/files/mp", repoRoot);

    // Put a stream into the workspace (and hence, the repository)
    InputStream in = getClass().getResourceAsStream("/opencast_header.gif");
    Assert.assertNotNull(in);
    workspace.put("foo", "bar", "header.gif", in);
    IOUtils.closeQuietly(in);
    
    // Ensure that the file was put into the working file repository
    EasyMock.verify(repo);

    // Ensure that the file was not cached in the workspace (since there is a configured filesystem mapping)
    File file = new File(workspaceRoot, "httplocalhost8080filesfoobarheader.gif");
    Assert.assertFalse(file.exists());
  }

  // Calls to put() should put the file into the working file repository and the local cache if there is no valid
  // filesystem mapping present
  @Test
  public void testPutCachingWithoutFilesystemMapping() throws Exception {
    // First, mock up the collaborating working file repository
    WorkingFileRepository repo = EasyMock.createMock(WorkingFileRepository.class);
    EasyMock.expect(repo.put((String)EasyMock.anyObject(), (String)EasyMock.anyObject(), (String)EasyMock.anyObject(), (InputStream)EasyMock.anyObject())).andReturn(new URI("http://localhost:8080/files/mp/foo/bar/header.gif"));
    EasyMock.replay(repo);
    workspace.setRepository(repo);
    workspace.filesystemMappings.clear();

    // Put a stream into the workspace (and hence, the repository)
    InputStream in = getClass().getResourceAsStream("/opencast_header.gif");
    Assert.assertNotNull(in);
    workspace.put("foo", "bar", "header.gif", in);
    IOUtils.closeQuietly(in);
    
    // Ensure that the file was put into the working file repository
    EasyMock.verify(repo);

    // Ensure that the file was cached in the workspace (since there is no configured filesystem mapping)
    File file = new File(workspaceRoot, "httplocalhost8080filesmpfoobarheader.gif");
    Assert.assertTrue(file.exists());
  }
  
  @Test
  public void testGarbageCollection() throws Exception {
    // First, mock up the collaborating working file repository
    WorkingFileRepository repo = EasyMock.createMock(WorkingFileRepository.class);
    EasyMock.expect(repo.put((String)EasyMock.anyObject(), (String)EasyMock.anyObject(), (String)EasyMock.anyObject(),
            (InputStream)EasyMock.anyObject())).andReturn(
                    new URI("http://localhost:8080/files/mp/mediapackage/element/sample.txt"));
    EasyMock.replay(repo);
    workspace.setRepository(repo);
    TrustedHttpClient httpClient = EasyMock.createNiceMock(TrustedHttpClient.class);
    // Simulate not finding the file
    EasyMock.expect(httpClient.execute((HttpUriRequest)EasyMock.anyObject())).andThrow(new RuntimeException());
    EasyMock.replay(httpClient);
    workspace.trustedHttpClient = httpClient;
    workspace.filesystemMappings.clear();

    // Put a file in the workspace
    ByteArrayInputStream in = new ByteArrayInputStream("sample".getBytes());
    URI uri = workspace.put("mediapackage", "element", "sample.txt", in);
    
    EasyMock.verify(repo);
    
    File file = workspace.get(uri);
    Assert.assertNotNull(file);
    Assert.assertTrue(file.exists());

    // Activate garbage collection
    workspace.garbageCollectionPeriodInSeconds = 1;
    workspace.maxAgeInSeconds = 1;
    workspace.activateGarbageFileCollectionTimer();

    // Wait for the garbage collector to delete the file
    Thread.sleep(3000);
    
    // The file should have been deleted
    try {
      workspace.get(uri);
      Assert.fail("The file at " + uri + " should have been deleted");
    } catch(NotFoundException e) {}

    workspace.deactivateGarbageFileCollectionTimer();
    Thread.sleep((workspace.garbageCollectionPeriodInSeconds + 1) * 1000);
  }

}
