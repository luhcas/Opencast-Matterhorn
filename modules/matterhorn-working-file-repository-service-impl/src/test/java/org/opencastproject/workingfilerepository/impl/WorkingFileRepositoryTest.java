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

package org.opencastproject.workingfilerepository.impl;

import static org.junit.Assert.fail;

import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;

public class WorkingFileRepositoryTest {

  private static final Logger logger = LoggerFactory.getLogger(WorkingFileRepositoryTest.class);
  private String mediaPackageID = "working-file-test-media-package-1";
  private String mediaPackageElementID = "working-file-test-element-1";
  private String collectionId = "collection-1";
  private String filename = "file.gif";
  private WorkingFileRepositoryImpl repo = new WorkingFileRepositoryImpl();

  @Before
  public void setup() throws Exception {
    repo.rootDirectory = "target" + File.separator + "repotest";
    repo.serverUrl = UrlSupport.DEFAULT_BASE_URL;
    repo.serviceUrl = new URI(UrlSupport.concat(UrlSupport.DEFAULT_BASE_URL, WorkingFileRepositoryImpl.URI_PREFIX));
    repo.createRootDirectory();

    // Put an image file into the repository using the mediapackage / element storage
    InputStream in = null;
    try {
      in = getClass().getClassLoader().getResourceAsStream("opencast_header.gif");
      repo.put(mediaPackageID, mediaPackageElementID, "opencast_header.gif", in);
      try {
        in.close();
      } catch (IOException e) {
        logger.error(e.getMessage());
      }
    } finally {
      IOUtils.closeQuietly(in);
    }

    // Put an image file into the repository into a collection
    try {
      in = getClass().getClassLoader().getResourceAsStream("opencast_header.gif");
      repo.putInCollection(collectionId, filename, in);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  @After
  public void tearDown() throws Exception {
    FileUtils.forceDelete(new File(repo.rootDirectory));
  }

  @Test
  public void testPut() throws Exception {
    // Get the file back from the repository to check whether it's the same file that we put in.
    InputStream fromRepo = null;
    try {
      fromRepo = repo.get(mediaPackageID, mediaPackageElementID);
      byte[] bytesFromRepo = IOUtils.toByteArray(fromRepo);
      byte[] bytesFromClasspath = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream(
              "opencast_header.gif"));
      Assert.assertEquals(bytesFromClasspath.length, bytesFromRepo.length);
    } finally {
      IOUtils.closeQuietly(fromRepo);
    }
  }

  @Test
  public void testDelete() throws Exception {
    // Delete the file and ensure that we can no longer get() it
    repo.delete(mediaPackageID, mediaPackageElementID);
    try {
      Assert.assertTrue(repo.get(mediaPackageID, mediaPackageElementID) == null);
      fail("File " + mediaPackageID + "/" + mediaPackageElementID + " was not deleted");
    } catch (NotFoundException e) {
      // This is intended
    }
  }

  @Test
  public void testPutBadId() throws Exception {
    // Try adding a file with a bad ID
    String badId = "../etc";
    InputStream in = null;
    try {
      in = getClass().getClassLoader().getResourceAsStream("opencast_header.gif");
      repo.put(badId, mediaPackageElementID, "opencast_header.gif", in);
      Assert.fail();
    } catch (Exception e) {
      // This is intended
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  @Test
  public void testGetBadId() throws Exception {
    String badId = "../etc";
    try {
      repo.get(badId, mediaPackageElementID);
      Assert.fail();
    } catch (Exception e) {
    }
  }

  @Test
  public void testPutIntoCollection() throws Exception {
    // Get the file back from the repository to check whether it's the same file that we put in.
    InputStream fromRepo = null;
    try {
      fromRepo = repo.getFromCollection(collectionId, filename);
      byte[] bytesFromRepo = IOUtils.toByteArray(fromRepo);
      byte[] bytesFromClasspath = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream(
              "opencast_header.gif"));
      Assert.assertEquals(bytesFromClasspath.length, bytesFromRepo.length);
    } finally {
      IOUtils.closeQuietly(fromRepo);
    }
  }

  @Test
  public void testCollectionSize() throws Exception {
    Assert.assertEquals(1, repo.getCollectionSize(collectionId));
  }

  @Test
  public void testCopy() throws Exception {
    String newFileName = "newfile.gif";
    byte[] bytesFromCollection = IOUtils.toByteArray(repo.getFromCollection(collectionId, filename));
    repo.copyTo(collectionId, filename, "copied-mediapackage", "copied-element", newFileName);
    byte[] bytesFromCopy = IOUtils.toByteArray(repo.get("copied-mediapackage", "copied-element"));
    Assert.assertTrue(Arrays.equals(bytesFromCollection, bytesFromCopy));
  }

  @Test
  public void testMove() throws Exception {
    String newFileName = "newfile.gif";
    byte[] bytesFromCollection = IOUtils.toByteArray(repo.getFromCollection(collectionId, filename));
    repo.moveTo(collectionId, filename, "moved-mediapackage", "moved-element", newFileName);
    byte[] bytesFromMove = IOUtils.toByteArray(repo.get("moved-mediapackage", "moved-element"));
    Assert.assertTrue(Arrays.equals(bytesFromCollection, bytesFromMove));
  }
}
