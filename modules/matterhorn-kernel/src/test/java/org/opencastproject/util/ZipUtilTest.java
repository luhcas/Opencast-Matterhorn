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
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/* WARNING
 * 
 * Instead of using a single static final file for testing the destination files, we are using
 * a file with a unique name for each test. The reason is that if the tests are run so quickly
 * the filesystem is unable to erase the files on time and a test can re-create the file before
 * it was physically deleted. If those files are suppossed to be different, a test may return 
 * a false positive or negative. If the pathnames are different for each test, there's no
 * possible mistake.
 * For uniformity, all the tests declare internally it's own destination file/filename, even 
 * though this may not be strictly necessary for all of them.
 * 
 * ruben.perez
 */


public class ZipUtilTest {

  private static final String baseDirName = "zip_test_tmp", 
  baseDirPath=System.getProperty("java.io.tmpdir") + File.separator + baseDirName, 
  srcDirName="src",
  srcDirPath=baseDirPath + File.separator + srcDirName,
  nestedSrcDirName="nested",
  nestedSrcDirPath=srcDirPath + File.separator + nestedSrcDirName,
  srcFileName="av.mov",
  srcFilePath=srcDirPath + File.separator + srcFileName, 
  nestedSrcFileName="manifest.xml",
  nestedSrcFilePath= nestedSrcDirPath + File.separator + nestedSrcFileName,
  destDirName="dest",
  destDirPath=baseDirPath + File.separator + destDirName,
  sampleZipName="sampleZip.zip";

  private static final File baseDir = new File(System.getProperty("java.io.tmpdir"), baseDirName),
  srcDir = new File(baseDir, srcDirName),
  nestedSrcDir = new File(srcDir, nestedSrcDirName),
  srcFile = new File(srcDir, srcFileName),
  nestedSrcFile = new File(nestedSrcDir, nestedSrcFileName),
  destDir = new File(baseDir, destDirName),
  sampleZip = new File(baseDir, sampleZipName);

  /**
   * Added as part of the fix for MH-1809
   * WARNING: Changes in the files to zip would change the resulting zip size.
   * If such changes are made, change also this constant accordingly
   * MH-2455: If files used in zip are checked out with native line endings,
   * windows file size will differ.
   */
  // Commented by ruben.perez -- not necessary
  //private static final long UNIX_ZIP_SIZE = 882172;

  private static final Logger logger = LoggerFactory.getLogger(ZipUtilTest.class);

  // Please adjust this value -cedriessen
  // Commented out by ruben.perez --not necessary
  //private static final long WINDOWS_ZIP_SIZE = 870533;

  @Before
  public void setup() throws Exception {
    // Set up the source and destination directories
    Assert.assertTrue(baseDir.isDirectory() || baseDir.mkdirs());

    Assert.assertTrue(srcDir.isDirectory() || srcDir.mkdir());
    
    Assert.assertTrue(srcDir.isDirectory() || nestedSrcDir.mkdir());

    Assert.assertTrue(destDir.isDirectory() || destDir.mkdir());
    
    // Copy the source files from the classpath to the source dir
    FileUtils.copyURLToFile(this.getClass().getResource("/av.mov"), srcFile);
    FileUtils.copyURLToFile(this.getClass().getResource("/manifest.xml"), nestedSrcFile);
  }

  @After
  public void teardown() throws Exception {
    FileUtils.forceDelete(baseDir);
  }

  /** Check the behavior with bad arguments for the zip signature String[], String */
  @Test
  public void badInputZipStrStr() throws Exception {

    String destFilePath = destDirPath + File.separator + "badInputStrStr.zip";
    String badDestFilePath = destDirPath + File.separator + "badInputStrStr";
    
    // Null String array, correct destination filename 
    try {
      ZipUtil.zip((String[])null, destFilePath, true, 0);
      logger.error("Zip should fail when input String array is null");
      Assert.fail("Zip should fail when input String array is null");
    } catch (NullPointerException e) {
      logger.info("Null input String array detection (String, String): OK");
    } catch (Exception e) {
      logger.error("Another exception was expected, but got {} instead: {}", e.getClass().getName(), e.getMessage());
      Assert.fail("Another exception was expected, but got " + e.getClass().getName() + "instead: " + e.getMessage());
    }

    // Correct input filenames, null destination filename 
    try {
      ZipUtil.zip(new String[]{srcFilePath, nestedSrcFilePath}, (String)null, true, 0);
      logger.error("Zip should fail when destination filename is null");
      Assert.fail("Zip should fail when destination filename is null");
    } catch (NullPointerException e) {
      logger.info("Null destination filename detection (String, String): OK");
    } catch (Exception e) {
      logger.error("Another exception was expected, but got {} instead: {}", e.getClass().getName(), e.getMessage());
      Assert.fail("Another exception was expected, but got " + e.getClass().getName() + "instead: " + e.getMessage());
    }

    // Correct input filenames, empty destination String
    try {
      ZipUtil.zip(new String[]{srcFilePath, nestedSrcFilePath}, "", true, 0);
      logger.error("Zip should fail when destination filename is empty");
      Assert.fail("Zip should fail when destination filename is empty");
    } catch (IllegalArgumentException e) {
      logger.info("Empty destination filename detection (String, String): OK");
    } catch (Exception e) {
      logger.error("Another exception was expected, but got {} instead: {}", e.getClass().getName(), e.getMessage());
      Assert.fail("Another exception was expected, but got " + e.getClass().getName() + "instead: " + e.getMessage());
    }
    
    // Invalid name for the zip file
    try {
      ZipUtil.zip(new String[]{srcFilePath, nestedSrcFilePath}, badDestFilePath, true, 0);
      logger.error("Zip should fail when the destination filename does not represent a zip file");
      Assert.fail("Zip should fail when the destination filename does not represent a zip file");
    } catch (IllegalArgumentException e) {
      logger.info("Destination filename not representing a valid zip file detection (String, String): OK");
    } catch (Exception e) {
      logger.error("Another exception was expected, but got {} instead: {}", e.getClass().getName(), e.getMessage());
      Assert.fail("Another exception was expected, but got " + e.getClass().getName() + "instead: " + e.getMessage());
    }
    
  }
  
  
  /** Check the behavior with bad arguments for the zip signature String[], File */
  @Test
  public void badInputZipStrFile() throws Exception {
   
    File destFile = new File(destDir,"badInputStrFile.zip");
    File badDestFile = new File(destDir, "badInputStrFile");
    
    // Null String array, correct destination File 
    try {
      ZipUtil.zip((String[])null, destFile, true, 0);
      logger.error("Zip should fail when input String array is null");
      Assert.fail("Zip should fail when input String array is null");
    } catch (NullPointerException e) {
      logger.info("Null input File array detection (String, File): OK");
    } catch (Exception e) {
      logger.error("Another exception was expected, but got {} instead: {}", e.getClass().getName(), e.getMessage());
      Assert.fail("Another exception was expected, but got " + e.getClass().getName() + "instead: " + e.getMessage());
    }

    // Correct input filenames, null destination File
    try {
      ZipUtil.zip(new String[]{srcFilePath, nestedSrcFilePath}, (File)null, true, 0);
      logger.error("Zip should fail when destination File is null");
      Assert.fail("Zip should fail when destination File is null");
    } catch (NullPointerException e) {
      logger.info("Null destination File detection (String, File): OK");
    } catch (Exception e) {
      logger.error("Another exception was expected, but got {} instead: {}", e.getClass().getName(), e.getMessage());
      Assert.fail("Another exception was expected, but got " + e.getClass().getName() + "instead: " + e.getMessage());
    }

    // To make zip fail
    Assert.assertTrue(destFile.exists() || destFile.createNewFile());
        
    // Correct input filenames, existing non-directory destination File
    try {
      ZipUtil.zip(new String[]{srcFilePath, nestedSrcFilePath}, destFile, true, 0);
      logger.error("Zip should fail when destination filename is empty");
      Assert.fail("Zip should fail when destination filename is empty");
    } catch (IllegalArgumentException e) {
      logger.info("Wrong destination File detection (String, File): OK");
    } catch (Exception e) {
      logger.error("Another exception was expected, but got {} instead: {}", e.getClass().getName(), e.getMessage());
      Assert.fail("Another exception was expected, but got " + e.getClass().getName() + "instead: " + e.getMessage());
    }
    
    // Invalid name for the zip file
    try {
      ZipUtil.zip(new String[]{srcFilePath, nestedSrcFilePath}, badDestFile, true, 0);
      logger.error("Zip should fail when the destination File does not represent a zip file");
      Assert.fail("Zip should fail when the destination File does not represent a zip file");
    } catch (IllegalArgumentException e) {
      logger.info("Destination File not representing a valid zip file detection (String, File): OK");
    } catch (Exception e) {
      logger.error("Another exception was expected, but got {} instead: {}", e.getClass().getName(), e.getMessage());
      Assert.fail("Another exception was expected, but got " + e.getClass().getName() + "instead: " + e.getMessage());
    }

  }
    
  
  /** Check the behavior before bad arguments for the zip signature File[], String */
  @Test
  public void badInputZipFileStr() throws Exception {
    
    String destFilePath = destDirPath + File.separator + "badInputFileStr.zip";
    String badDestFilePath = destDirPath + File.separator + "badInputFileStr";
    
    // Null File array, correct destination filename 
    try {
      ZipUtil.zip((File[])null, destFilePath, true, 0);
      logger.error("Zip should fail when input File array is null");
      Assert.fail("Zip should fail when input File array is null");
    } catch (NullPointerException e) {
      logger.info("Null input File array detection (File, String): OK");
    } catch (Exception e) {
      logger.error("Another exception was expected, but got {} instead: {}", e.getClass().getName(), e.getMessage());
      Assert.fail("Another exception was expected, but got " + e.getClass().getName() + "instead: " + e.getMessage());
    }

    // Correct input Files, null destination filename 
    try {
      ZipUtil.zip(new File[]{srcFile, nestedSrcFile}, (String)null, true, 0);
      logger.error("Zip should fail when destination filename is null");
      Assert.fail("Zip should fail when destination filename is null");
    } catch (NullPointerException e) {
      logger.info("Null destination filename detection (File, String): OK");
    } catch (Exception e) {
      logger.error("Another exception was expected, but got {} instead: {}", e.getClass().getName(), e.getMessage());
      Assert.fail("Another exception was expected, but got " + e.getClass().getName() + "instead: " + e.getMessage());
    }

    // Correct input Files, empty destination String
    try {
      ZipUtil.zip(new File[]{srcFile, nestedSrcFile}, "", true, 0);
      logger.error("Zip should fail when destination filename is empty");
      Assert.fail("Zip should fail when destination filename is empty");
    } catch (IllegalArgumentException e) {
      logger.info("Empty destination filename detection (File, String): OK");
    } catch (Exception e) {
      logger.error("Another exception was expected, but got {} instead: {}", e.getClass().getName(), e.getMessage());
      Assert.fail("Another exception was expected, but got " + e.getClass().getName() + "instead: " + e.getMessage());
    }
    
    // Invalid name for the zip file
    try {
      ZipUtil.zip(new String[]{srcFilePath, nestedSrcFilePath}, badDestFilePath, true, 0);
      logger.error("Zip should fail when the destination filename does not represent a zip file");
      Assert.fail("Zip should fail when the destination filename does not represent a zip file");
    } catch (IllegalArgumentException e) {
      logger.info("Destination filename not representing a valid zip file detection (File, String): OK");
    } catch (Exception e) {
      logger.error("Another exception was expected, but got {} instead: {}", e.getClass().getName(), e.getMessage());
      Assert.fail("Another exception was expected, but got " + e.getClass().getName() + "instead: " + e.getMessage());
    }

  }
  
  
  /** Check the behavior before bad arguments for the signature File[], File */
  @Test
  public void badInputZipFileFile() throws Exception {
    
    File destFile = new File(destDir,"badInputFileFile.zip");
    File badDestFile = new File(destDir,"badInputFileFile");
    
    // Null File array, correct destination File 
    try {
      ZipUtil.zip((File[])null, destFile, true, 0);
      logger.error("Zip should fail when input File array is null");
      Assert.fail("Zip should fail when input File array is null");
    } catch (NullPointerException e) {
      logger.info("Null input File array detection (File, File): OK");
    } catch (Exception e) {
      logger.error("Another exception was expected, but got {} instead: {}", e.getClass().getName(), e.getMessage());
      Assert.fail("Another exception was expected, but got " + e.getClass().getName() + "instead: " + e.getMessage());
    }

    // Correct input Files, null destination File
    try {
      ZipUtil.zip(new File[]{srcFile, nestedSrcFile}, (File)null, true, 0);
      logger.error("Zip should fail when destination File is null");
      Assert.fail("Zip should fail when destination File is null");
    } catch (NullPointerException e) {
      logger.info("Null destination File detection (File, File): OK");
    } catch (Exception e) {
      logger.error("Another exception was expected, but got {} instead: {}", e.getClass().getName(), e.getMessage());
      Assert.fail("Another exception was expected, but got " + e.getClass().getName() + "instead: " + e.getMessage());
    }

    // To make zip fail
    Assert.assertTrue(destFile.exists() || destFile.createNewFile());
        
    // Correct input Files, existing non-directory destination File
    try {
      ZipUtil.zip(new File[]{srcFile, nestedSrcFile}, destFile, true, 0);
      logger.error("Zip should fail when destination filename is empty");
      Assert.fail("Zip should fail when destination filename is empty");
    } catch (IllegalArgumentException e) {
      logger.info("Wrong destination File detection (String, File): OK");
    } catch (Exception e) {
      logger.error("Another exception was expected, but got {} instead: {}", e.getClass().getName(), e.getMessage());
      Assert.fail("Another exception was expected, but got " + e.getClass().getName() + "instead: " + e.getMessage());
    }

    // Invalid name for the zip file
    try {
      ZipUtil.zip(new String[]{srcFilePath, nestedSrcFilePath}, badDestFile, true, 0);
      logger.error("Zip should fail when the destination File does not represent a zip file");
      Assert.fail("Zip should fail when the destination File does not represent a zip file");
    } catch (IllegalArgumentException e) {
      logger.info("Destination File not representing a valid zip file detection (File, File): OK");
    } catch (Exception e) {
      logger.error("Another exception was expected, but got {} instead: {}", e.getClass().getName(), e.getMessage());
      Assert.fail("Another exception was expected, but got " + e.getClass().getName() + "instead: " + e.getMessage());
    }

  }

  @Test
  public void zipNoRecStrStr() throws Exception {

    String destFilePath = destDirPath + File.separator + "noRecStrStr.zip";

    Vector<String> names = new Vector<String>();
    names.add(srcFileName);
    names.add(nestedSrcFileName);
    
    File test = ZipUtil.zip(new String[]{srcFilePath, nestedSrcFilePath}, destFilePath);
    Assert.assertTrue(test.exists());
    ZipFile zip = new ZipFile(test);
    Assert.assertEquals(2,zip.size());
    Enumeration<? extends ZipEntry> entries = zip.entries();
    
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();
      Assert.assertTrue(names.contains(entry.getName()));
    }
  }

  @Test
  public void zipNoRecStrFile() throws Exception {
    
    File destFile = new File(destDir,"noRecStrFile.zip");
    
    Vector<String> names = new Vector<String>();
    names.add(srcFileName);
    names.add(nestedSrcFileName);
    
    File test = ZipUtil.zip(new String[] {srcFilePath, nestedSrcFilePath}, destFile);
    Assert.assertTrue(test.exists());
    ZipFile zip = new ZipFile(test);
    Assert.assertEquals(2,zip.size());
    Enumeration<? extends ZipEntry> entries = zip.entries();
    
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();
      Assert.assertTrue(names.contains(entry.getName()));
    }
  }
  
  @Test
  public void zipNoRecFileStr() throws Exception {
    
    String destFilePath = destDirPath + File.separator + "noRecFileStr.zip";
    
    Vector<String> names = new Vector<String>();
    names.add(srcFileName);
    names.add(nestedSrcFileName);
    
    File test = ZipUtil.zip(new File[] {srcFile, nestedSrcFile}, destFilePath);
    Assert.assertTrue(test.exists());
    ZipFile zip = new ZipFile(test);
    Assert.assertEquals(2,zip.size());
    Enumeration<? extends ZipEntry> entries = zip.entries();
    
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();
      Assert.assertTrue(names.contains(entry.getName()));
    }

  }
  
  @Test
  public void zipNoRecFileFile() throws Exception {
    
    File destFile = new File(destDir,"noRecFileFile.zip");
    
    Vector<String> names = new Vector<String>();
    names.add(srcFileName);
    names.add(nestedSrcFileName);
    
    File test = ZipUtil.zip(new File[] {srcFile, nestedSrcFile}, destFile);
    Assert.assertTrue(test.exists());
    ZipFile zip = new ZipFile(test);
    Assert.assertEquals(2,zip.size());
    Enumeration<? extends ZipEntry> entries = zip.entries();
    
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();
      Assert.assertTrue(names.contains(entry.getName()));
    }
  }
  
  @Test
  public void zipRecStrStr() throws Exception {

    String destFilePath = destDirPath + File.separator + "recStrStr.zip";
    
    Vector<String> names = new Vector<String>();
    names.add(srcFileName);
    names.add(nestedSrcDirName + File.separator);
    names.add(nestedSrcDirName + File.separator + nestedSrcFileName);
    
    String[] filenames = srcDir.list();
    for (int i = 0; i < filenames.length; i++)
      filenames[i] = srcDir.getAbsolutePath() + File.separator + filenames[i];
    
    File test = ZipUtil.zip(filenames, destFilePath, true);
    Assert.assertTrue(test.exists());
    ZipFile zip = new ZipFile(test);
    Assert.assertEquals(3,zip.size());
    Enumeration<? extends ZipEntry> entries = zip.entries();
    
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();
      Assert.assertTrue(names.contains(entry.getName()));
    }
  }

  @Test
  public void zipRecStrFile() throws Exception {
    
    File destFile = new File(destDir,"recStrFile.zip");
    
    Vector<String> names = new Vector<String>();
    names.add(srcFileName);
    names.add(nestedSrcDirName + File.separator);
    names.add(nestedSrcDirName + File.separator + nestedSrcFileName);
    
    String[] filenames = srcDir.list();
    for (int i = 0; i < filenames.length; i++)
      filenames[i] = srcDir.getAbsolutePath() + File.separator + filenames[i];
    
    File test = ZipUtil.zip(filenames, destFile, true);
    Assert.assertTrue(test.exists());
    ZipFile zip = new ZipFile(test);
    Assert.assertEquals(3,zip.size());
    Enumeration<? extends ZipEntry> entries = zip.entries();
    
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();
      Assert.assertTrue(names.contains(entry.getName()));
    }
  }

  @Test
  public void zipRecFileStr() throws Exception {

    String destFilePath = destDirPath + File.separator + "recFileStr.zip";
    
    Vector<String> names = new Vector<String>();
    names.add(srcFileName);
    names.add(nestedSrcDirName + File.separator);
    names.add(nestedSrcDirName + File.separator + nestedSrcFileName);
    
    File test = ZipUtil.zip(srcDir.listFiles(), destFilePath, true);
    Assert.assertTrue(test.exists());
    ZipFile zip = new ZipFile(test);
    Assert.assertEquals(3,zip.size());
    Enumeration<? extends ZipEntry> entries = zip.entries();
    
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();
      Assert.assertTrue(names.contains(entry.getName()));
    }
  }

  @Test
  public void zipRecFileFile() throws Exception {

    File destFile = new File(destDir,"recFileFile.zip");
    
    Vector<String> names = new Vector<String>();
    names.add(srcFileName);
    names.add(nestedSrcDirName + File.separator);
    names.add(nestedSrcDirName + File.separator + nestedSrcFileName);
    
    File test = ZipUtil.zip(srcDir.listFiles(), destFile, true);
    Assert.assertTrue(test.exists());
    ZipFile zip = new ZipFile(test);
    Assert.assertEquals(3,zip.size());
    Enumeration<? extends ZipEntry> entries = zip.entries();
    
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();
      Assert.assertTrue(names.contains(entry.getName()));
    }
  }

  /**
   * Test unzip syntax
   */
  @Test @Ignore
  public void unzipSyntax() throws Exception {
    // Null File array, correct destination File 
    try {
      //ZipUtil.unzip((File[])null, destFile, true, 0);
      logger.error("Zip should fail when input File array is null");
      Assert.fail("Zip should fail when input File array is null");
    } catch (NullPointerException e) {
      logger.info("Null input File array detection (File, File): OK");
    } catch (Exception e) {
      logger.error("Another exception was expected, but got {} instead: {}", e.getClass().getName(), e.getMessage());
      Assert.fail("Another exception was expected, but got " + e.getClass().getName() + "instead: " + e.getMessage());
    }
    
  }
   
  /**
   * Test unzipping 
   */
  @Test
  public void testUnzip() throws Exception {
    
    FileUtils.copyURLToFile(this.getClass().getResource("/sampleZip.zip"), sampleZip);
    
    ZipUtil.unzip(sampleZip, destDir);
    
    ZipFile test = new ZipFile(sampleZip);
    
    Enumeration<? extends ZipEntry> entries = test.entries();
    
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();
      Assert.assertTrue(new File(destDir,entry.getName()).exists());
    }
  }

  /**
   * Test zipping and unzipping a directory's content recursively.
   */
  @Test @Ignore
  public void testUnzipDirRec() throws Exception {
    
    String destFilePath = destDirPath + File.separator + "badInputStrFile.zip";
    
    File zip = ZipUtil.zip(srcDir.listFiles(), destDir + File.separator + "testingZip.zip", true);
    File unzipDir = new File(zip.getParent(), "unzipdir");
    ZipUtil.unzip(zip, unzipDir);
    Assert.assertTrue(new File(unzipDir, "av.mov").exists());
    Assert.assertTrue(new File(unzipDir, destFilePath).exists());
  }

  /**
   * Test zipping and unzipping a directory recursively.
   */
  @Test @Ignore
  public void testUnzipDirRecInclBaseDir() throws Exception {
    File zip = ZipUtil.zip(new File[] {srcDir}, destDir + File.separator + "testingZip.zip", true);
    File unzipDir = new File(zip.getParent(), "unzipdir");
    ZipUtil.unzip(zip, unzipDir);
    Assert.assertTrue(new File(unzipDir, srcDir.getName() + File.separator + "av.mov").exists());
    //Assert.assertTrue(new File(unzipDir, srcDir.getName() + File.separator + NESTED_DIR_NAME + File.separator + "manifest.xml").exists());
  }

  /**
   * Test zipping a directory without recursion.
   */
  @Test @Ignore
  public void testUnzipDir() throws Exception {
    File zip = ZipUtil.zip(new File[]{srcDir}, destDir + File.separator + "testingZip.zip");
    File unzipDir = new File(zip.getParent(), "unzipdir");
    ZipUtil.unzip(zip, unzipDir);
    Assert.assertTrue(unzipDir.listFiles().length == 0);
  }
}
