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
package org.opencastproject.util;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

public class ZipUtilTest {
  File baseDir;
  File srcDir;
  File src1;
  File src2;
  File destDir;
  /**
   * Added as part of the fix for MH-1809
   * WARNING: Changes in the files to zip would change the resulting zip size.
   * If such changes are made, change also this constant accordingly
   */
  private static final long ZIPSIZE = 870532;
  
  @Before
  public void setup() throws Exception {
    // Set up the source and destination directories
    baseDir = new File("target/zip_test_tmp");
    Assert.assertTrue(baseDir.mkdirs());
    srcDir = new File(baseDir, "src");
    Assert.assertTrue(srcDir.mkdir());
    destDir = new File(baseDir, "dest");
    Assert.assertTrue(destDir.mkdir());
    
    // Copy the source files from the classpath to the source dir
    src1 = new File(srcDir, "av.mov");
    src2 = new File(srcDir, "manifest.xml");
    FileUtils.copyURLToFile(this.getClass().getResource("/av.mov"), src1);
    FileUtils.copyURLToFile(this.getClass().getResource("/manifest.xml"), src2);
  }
  
  @After
  public void teardown() throws Exception {
    FileUtils.forceDelete(baseDir);
  }
  
  @Test
  public void testZip() throws Exception {
    File zip = ZipUtil.zip(new File[] {src1, src2}, destDir + File.separator + "testingZip.zip");
    Assert.assertTrue(zip.exists());
    // Testing issue MH-1809
    Assert.assertEquals(ZIPSIZE, zip.length());
    // java 5 incompatible
    //Assert.assertTrue(zip.getTotalSpace() > 0);
  }
  
  @Test
  public void testUnzip() throws Exception {
    File zip = ZipUtil.zip(new File[] {src1, src2}, destDir + File.separator + "testingUnzip.zip");
    File unzipDir = new File(zip.getParent(), "unzipdir");
    ZipUtil.unzip(zip, unzipDir);
    Assert.assertTrue(new File(unzipDir, "av.mov").exists());
  }
}
