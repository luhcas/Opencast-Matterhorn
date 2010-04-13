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
package org.opencastproject.util;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

public class ZipUtilTest {

  static final String NESTED_DIR_NAME = "nested";
  File baseDir;
  File srcDir;
  File nestedSrcDir;
  File src1;
  File src2;
  File destDir;
  
  /**
   * Added as part of the fix for MH-1809
   * WARNING: Changes in the files to zip would change the resulting zip size.
   * If such changes are made, change also this constant accordingly
   * MH-2455: If files used in zip are checked out with native line endings,
   * windows file size will differ.
   */
  private static final long UNIX_ZIP_SIZE = 870531;
  private static final long WINDOWS_ZIP_SIZE = 870533;

  @Before
  public void setup() throws Exception {
    // Set up the source and destination directories
    baseDir = new File("target/zip_test_tmp");
    Assert.assertTrue(baseDir.mkdirs());
    srcDir = new File(baseDir, "src");
    Assert.assertTrue(srcDir.mkdir());
    nestedSrcDir = new File(srcDir, NESTED_DIR_NAME);
    Assert.assertTrue(nestedSrcDir.mkdir());
    destDir = new File(baseDir, "dest");
    Assert.assertTrue(destDir.mkdir());
    
    // Copy the source files from the classpath to the source dir
    src1 = new File(srcDir, "av.mov");
    src2 = new File(nestedSrcDir, "manifest.xml");
    FileUtils.copyURLToFile(this.getClass().getResource("/av.mov"), src1);
    FileUtils.copyURLToFile(this.getClass().getResource("/manifest.xml"), src2);
  }
  
  @After
  public void teardown() throws Exception {
    FileUtils.forceDelete(baseDir);
  }
  
  @Test
  public void testZipFiles() throws Exception {
    File zip = ZipUtil.zip(new File[] {src1, src2}, destDir + File.separator + "testingZip.zip");
    Assert.assertTrue(zip.exists());
    // Added as part of MH-2455
    String OSName = System.getProperty("os.name").toUpperCase();
    if(OSName.contains("WINDOWS")){
      Assert.assertEquals(WINDOWS_ZIP_SIZE, zip.length());
    } else {
      // Testing issue MH-1809
      Assert.assertEquals(UNIX_ZIP_SIZE, zip.length());
    }
    // java 5 incompatible
    //Assert.assertTrue(zip.getTotalSpace() > 0);
  }

  @Test
  public void testZipDir() throws Exception {
    File zip = ZipUtil.zip(srcDir.listFiles(), destDir + File.separator + "testingZip.zip", true);
    Assert.assertTrue(zip.exists());
    // todo have a size test like in testZipFiles
  }

  /**
   * Test zipping and unzipping of several files.
   */
  @Test
  public void testUnzipFiles() throws Exception {
    File zip = ZipUtil.zip(new File[] {src1, src2}, destDir + File.separator + "testingUnzip.zip");
    File unzipDir = new File(zip.getParent(), "unzipdir");
    ZipUtil.unzip(zip, unzipDir);
    Assert.assertTrue(new File(unzipDir, "av.mov").exists());
    Assert.assertTrue(new File(unzipDir, "manifest.xml").exists());
  }

  /**
   * Test zipping and unzipping a directory's content recursively.
   */
  @Test
  public void testUnzipDirRec() throws Exception {
    File zip = ZipUtil.zip(srcDir.listFiles(), destDir + File.separator + "testingZip.zip", true);
    File unzipDir = new File(zip.getParent(), "unzipdir");
    ZipUtil.unzip(zip, unzipDir);
    Assert.assertTrue(new File(unzipDir, "av.mov").exists());
    Assert.assertTrue(new File(unzipDir, NESTED_DIR_NAME + File.separator + "manifest.xml").exists());
  }

  /**
   * Test zipping and unzipping a directory recursively.
   */
  @Test
  public void testUnzipDirRecInclBaseDir() throws Exception {
    File zip = ZipUtil.zip(new File[] {srcDir}, destDir + File.separator + "testingZip.zip", true);
    File unzipDir = new File(zip.getParent(), "unzipdir");
    ZipUtil.unzip(zip, unzipDir);
    Assert.assertTrue(new File(unzipDir, srcDir.getName() + File.separator + "av.mov").exists());
    Assert.assertTrue(new File(unzipDir, srcDir.getName() + File.separator + NESTED_DIR_NAME + File.separator + "manifest.xml").exists());
  }

  /**
   * Test zipping a directory without recursion.
   */
  @Test
  public void testUnzipDir() throws Exception {
    File zip = ZipUtil.zip(new File[]{srcDir}, destDir + File.separator + "testingZip.zip");
    File unzipDir = new File(zip.getParent(), "unzipdir");
    ZipUtil.unzip(zip, unzipDir);
    System.out.println(unzipDir.listFiles().length);
    Assert.assertTrue(unzipDir.listFiles().length == 0);
  }
}
