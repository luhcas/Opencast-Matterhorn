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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements two very simple methods to compress and uncompress zip files. Please note that NO SUPPORT FOR COMPRESSING
 * DIRECTORIES has been tested, and probably does not work.
 */
public class Compressor {

  // Indicates buffer size for copying data between streams
  private int BUFFER_SIZE = 0xFFFF;
  private static final Logger logger = LoggerFactory.getLogger(Compressor.class);

  /**
   * Constructor with standard parameters
   */
  public Compressor() {

  }

  /**
   * Constructor specifying the buffer size
   */
  public Compressor(int buf_size) {
    BUFFER_SIZE = buf_size;
  }

  /**
   * BUFFER_SIZE specifies the size of the buffer used to move data from input to output streams.
   * 
   * @return The current buffer size
   */
  public int getBufferSize() {
    return BUFFER_SIZE;
  }

  /**
   * BUFFER_SIZE specifies the size of the buffer used to move data from input to output streams. PLEASE NOTE no
   * checking is made to the value specified to the function.
   * 
   * @param size
   *          : The new size for the buffer
   */
  public void setBufferSize(int size) {
    BUFFER_SIZE = size;
  }

  /**
   * Copies data from an InputStream to an OutputStream in blocks of BUFFER_SIZE bytes
   * 
   * @param in
   * @param out
   */
  private void copy(InputStream in, OutputStream out) throws IOException {
    byte[] buffer = new byte[BUFFER_SIZE];
    int bytesRead = 0, total = 0;

    while ((bytesRead = in.read(buffer, 0, BUFFER_SIZE)) != -1) {
      out.write(buffer, 0, bytesRead);
      total += bytesRead;
    }
  }

  /**
   * Calculates the CRC32 of a file
   * 
   * @param fileName
   *          : The name of the file whose CRC32 is to be computed
   * @return The CRC32 calculated, or -1 if an error happened
   */
  private long checksum(File fileDesc) {
    CheckedInputStream fileStream;

    try {
      fileStream = new CheckedInputStream(new FileInputStream(fileDesc), new CRC32());

      fileStream.skip(fileDesc.length());
    } catch (Exception e) {
      logger.error("COMPRESSOR: Failed to calculate CRC32 entry for file: " + fileDesc.getName() + ".\n"
              + e.getLocalizedMessage());
      return -1;
    }

    return fileStream.getChecksum().getValue();
  }

  /**
   * "ZIPs" the files whose names are passed in the array files[], to the file indicated by ZipName.
   * 
   * @param files
   *          : An array with the paths of the files to be compressed
   * @param ZipName
   *          : The name of the .zip file created
   * @return A boolean indicating the state of the operation
   */
  public File zip(String files[], String ZipName) {
    File myZipFile = null, inputFile = null;
    ZipOutputStream myZipStream;
    ZipEntry anEntry;
    FileInputStream inputStream = null;

    // Creates the ZIP file on disk
    try {
      myZipFile = new File(ZipName);
      myZipStream = new ZipOutputStream(new FileOutputStream(myZipFile));

      // Iterates the array with file names to be compressed
      for (String fileName : files) {
        // Open the file
        inputFile = new File(fileName);

        // Check file is not the output file
        if (inputFile.equals(myZipFile))
          continue;

        // Check file exists
        if (!inputFile.exists()) {
          System.out.println(inputFile.getName() + " doesn't exist");
          continue;
        }

        // Check file is not a directory
        if (inputFile.isDirectory()) {
          System.out.println(inputFile.getName() + " is a directory");
          continue;
        }

        // Get an InputStream from the file
        inputStream = new FileInputStream(inputFile);

        // Create a new entry for the file
        anEntry = new ZipEntry(inputFile.getName());

        // Force no compression
        // Note: Forcing no compression needs including a CRC32; otherwise this can be skipped
        anEntry.setMethod(ZipEntry.STORED);
        anEntry.setSize(inputStream.available());

        // Creates a CRC32 hash
        // Note: this *MUST* be included in the entry BEFORE it is inserted
        anEntry.setCrc(checksum(inputFile));

        // Insert the entry in the ZIP file
        myZipStream.putNextEntry(anEntry);

        copy(inputStream, myZipStream);

        inputStream.close();

      }

      myZipStream.close();

    } catch (FileNotFoundException e) {
      logger.error("COMPRESSOR: File doesn't exist. " + e.getLocalizedMessage());
      if (myZipFile.exists()) {
        logger.info("COMPRESSOR: Deleting .zip file...");
        myZipFile.delete();
      }
    } catch (IOException e) {
      logger.error("COMPRESSOR: I/O exception occurred. " + e.getLocalizedMessage());
      if (myZipFile.exists()) {
        logger.info("COMPRESSOR: Deleting .zip file...");
        myZipFile.delete();
      }
    }

    return ((myZipFile != null) && (myZipFile.exists()) ? myZipFile : null);
  }

  /**
   * Unzips the file indicated by its fileName
   * 
   * @param fileName
   *          : The location of the file
   * @return A boolean indicating the state of the operation
   */
  public boolean unzip(String fileName) {
    return unzip(fileName, "");
  }

  /**
   * Unzips the file indicated by its fileName
   * 
   * @param fileName
   *          : The location of the file
   * @param dir
   *          : A directory in which the files will be uncompressed
   * @return A boolean indicating the state of the operation
   */
  public boolean unzip(String fileName, String dir) {

    File outDirect;
    String outName;

    try {
      if (dir == null)
        dir = new String("");

      outDirect = new File(dir);
      outDirect.mkdirs();

      ZipFile zip = new ZipFile(fileName);
      Enumeration<? extends ZipEntry> entries = zip.entries();

      if (!entries.hasMoreElements())
        return false;

      while (entries.hasMoreElements()) {
        ZipEntry entry = (ZipEntry) entries.nextElement();
        outName = outDirect.getPath() + File.separator + entry.getName();

        if (entry.isDirectory()) {
          (new File(outName)).mkdir();
          continue;
        }

        FileOutputStream fileOut = new FileOutputStream(outName);

        copy(zip.getInputStream(entry), fileOut);

        fileOut.close();
      }
    } catch (Exception e) {
      logger.error("COMPRESSOR: Failed to unzip the file " + fileName + ". " + e.getLocalizedMessage());
      logger.info("Some files may have been extracted from " + fileName);
      return false;
    }

    return true;
  }

}
