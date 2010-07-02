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
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;

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

  private static final String workspaceRoot = "." + File.separator + "target" + File.separator
          + "junit-workspace-rootdir";
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
  public void testLongFilenames() throws Exception {
    WorkingFileRepository repo = EasyMock.createNiceMock(WorkingFileRepository.class);
    EasyMock.expect(repo.getBaseUri()).andReturn(new URI("http://localhost:8080/files")).anyTimes();
    EasyMock.replay(repo);

    workspace.setRepository(repo);
    File source = new File(
            "target/test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/../test-classes/opencast_header.gif");
    URL urlToSource = source.toURI().toURL();
    Assert.assertTrue(urlToSource.toString().length() > 255);
    try {
      Assert.assertNotNull(workspace.get(urlToSource.toURI()));
    } catch (NotFoundException e) {
      // This happens on some machines, so we catch and handle it.
    }
  }

  // Calls to put() should put the file into the working file repository, but not in the local cache if there's a valid
  // filesystem mapping present
  @Test
  public void testPutCachingWithFilesystemMapping() throws Exception {
    WorkingFileRepository repo = EasyMock.createNiceMock(WorkingFileRepository.class);
    EasyMock.expect(
            repo.getURI((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject()))
            .andReturn(
                    new URI("http://localhost:8080/files" + WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX
                            + "foo/bar/header.gif"));
    EasyMock.expect(
            repo.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(
            new URI("http://localhost:8080/files" + WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX
                    + "foo/bar/header.gif"));
    EasyMock.expect(repo.getBaseUri()).andReturn(new URI("http://localhost:8080/files")).anyTimes();
    EasyMock.replay(repo);

    workspace.setRepository(repo);

    // Put a stream into the workspace (and hence, the repository)
    InputStream in = getClass().getResourceAsStream("/opencast_header.gif");
    Assert.assertNotNull(in);
    workspace.put("foo", "bar", "header.gif", in);
    IOUtils.closeQuietly(in);

    // Ensure that the file was put into the working file repository
    EasyMock.verify(repo);

    // Ensure that the file was not cached in the workspace (since there is a configured filesystem mapping)
    File file = new File(workspaceRoot, "http___localhost_8080_files_foo_bar_header.gif");
    Assert.assertFalse(file.exists());
  }

  // Calls to put() should put the file into the working file repository and the local cache if there is no valid
  // filesystem mapping present
  @Test
  public void testPutCachingWithoutFilesystemMapping() throws Exception {
    // First, mock up the collaborating working file repository
    WorkingFileRepository repo = EasyMock.createMock(WorkingFileRepository.class);
    EasyMock.expect(
            repo.getURI((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject()))
            .andReturn(
                    new URI(UrlSupport.concat(new String[] { "http://localhost:8080", WorkingFileRepository.URI_PREFIX,
                            WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX, "foo", "bar", "header.gif" })));
    EasyMock.expect(
            repo.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(
            new URI("http://localhost:8080/files" + WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX
                    + "foo/bar/header.gif"));
    EasyMock.expect(repo.getBaseUri()).andReturn(new URI("http://localhost:8080/files")).anyTimes();
    EasyMock.replay(repo);
    workspace.setRepository(repo);

    // Put a stream into the workspace (and hence, the repository)
    InputStream in = getClass().getResourceAsStream("/opencast_header.gif");
    Assert.assertNotNull(in);
    workspace.put("foo", "bar", "header.gif", in);
    IOUtils.closeQuietly(in);

    // Ensure that the file was put into the working file repository
    EasyMock.verify(repo);

    // Ensure that the file was cached in the workspace (since there is no configured filesystem mapping)
    File file = new File(PathSupport.concat(new String[] { workspaceRoot,
            WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX, "foo", "bar", "header.gif" }));
    Assert.assertTrue(file.exists());
  }

  @Test
  public void testGarbageCollection() throws Exception {
    // First, mock up the collaborating working file repository
    WorkingFileRepository repo = EasyMock.createMock(WorkingFileRepository.class);
    EasyMock
            .expect(
                    repo.getURI((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock
                            .anyObject()))
            .andReturn(
                    new URI(UrlSupport.concat(new String[] { "http://localhost:8080", WorkingFileRepository.URI_PREFIX,
                            WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX, "mediapackage", "element", "sample.txt" })));
    EasyMock.expect(
            repo.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(
            new URI(UrlSupport.concat(new String[] { "http://localhost:8080", WorkingFileRepository.URI_PREFIX,
                    WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX, "mediapackage", "element", "sample.txt" })));
    EasyMock.expect(repo.getBaseUri()).andReturn(new URI("http://localhost:8080/files")).anyTimes();
    EasyMock.replay(repo);
    workspace.setRepository(repo);
    TrustedHttpClient httpClient = EasyMock.createNiceMock(TrustedHttpClient.class);
    // Simulate not finding the file
    EasyMock.expect(httpClient.execute((HttpUriRequest) EasyMock.anyObject())).andThrow(new RuntimeException());
    EasyMock.replay(httpClient);
    workspace.trustedHttpClient = httpClient;

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
    } catch (NotFoundException e) {
    }

    workspace.deactivateGarbageFileCollectionTimer();
    Thread.sleep((workspace.garbageCollectionPeriodInSeconds + 1) * 1000);
  }

  @Test
  public void testGetWorkspaceFileWithOutPort() throws Exception {
    WorkingFileRepository repo = EasyMock.createNiceMock(WorkingFileRepository.class);
    EasyMock.expect(repo.getBaseUri()).andReturn(new URI("http://localhost/files")).anyTimes();
    EasyMock.replay(repo);
    workspace.setRepository(repo);

    File workspaceFile = workspace.getWorkspaceFile(new URI("http://foo.com/myaccount/videos/bar.mov"), true);
    File expected = new File(PathSupport.concat(new String[] { workspaceRoot, "http_foo.com", "myaccount", "videos",
            "bar.mov" }));
    Assert.assertEquals(expected.getAbsolutePath(), workspaceFile.getAbsolutePath());

    workspaceFile = workspace.getWorkspaceFile(new URI("http://foo.com:8080/myaccount/videos/bar.mov"), true);
    expected = new File(PathSupport.concat(new String[] { workspaceRoot, "http_foo.com_8080", "myaccount",
            "videos", "bar.mov" }));
    Assert.assertEquals(expected.getAbsolutePath(), workspaceFile.getAbsolutePath());

    workspaceFile = workspace.getWorkspaceFile(new URI("http://localhost/files/collection/c1/bar.mov"), true);
    expected = new File(PathSupport.concat(new String[] { workspaceRoot, "collection", "c1", "bar.mov" }));
    Assert.assertEquals(expected.getAbsolutePath(), workspaceFile.getAbsolutePath());
  
  }

  @Test
  public void testGetWorkspaceFileWithPort() throws Exception {
    WorkingFileRepository repo = EasyMock.createNiceMock(WorkingFileRepository.class);
    EasyMock.expect(repo.getBaseUri()).andReturn(new URI("http://localhost:8080/files")).anyTimes();
    EasyMock.replay(repo);
    workspace.setRepository(repo);

    File workspaceFile = workspace.getWorkspaceFile(new URI("http://foo.com/myaccount/videos/bar.mov"), true);
    File expected = new File(PathSupport.concat(new String[] { workspaceRoot, "http_foo.com", "myaccount", "videos",
            "bar.mov" }));
    Assert.assertEquals(expected.getAbsolutePath(), workspaceFile.getAbsolutePath());

    workspaceFile = workspace.getWorkspaceFile(new URI("http://foo.com:8080/myaccount/videos/bar.mov"), true);
    expected = new File(PathSupport.concat(new String[] { workspaceRoot, "http_foo.com_8080", "myaccount",
            "videos", "bar.mov" }));
    Assert.assertEquals(expected.getAbsolutePath(), workspaceFile.getAbsolutePath());

    workspaceFile = workspace.getWorkspaceFile(new URI("http://localhost:8080/files/collection/c1/bar.mov"), true);
    expected = new File(PathSupport.concat(new String[] { workspaceRoot, "collection", "c1", "bar.mov" }));
    Assert.assertEquals(expected.getAbsolutePath(), workspaceFile.getAbsolutePath());

  }

}
