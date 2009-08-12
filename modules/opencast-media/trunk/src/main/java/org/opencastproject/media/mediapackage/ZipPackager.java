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

package org.opencastproject.media.mediapackage;

import org.opencastproject.util.FileSupport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Stack;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Reads media packages from and saves to zip streams.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: ZipPackager.java 1655 2008-12-08 16:14:11Z wunden $
 */
public class ZipPackager implements MediaPackagePackager {

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackagePackager#pack(org.opencastproject.media.mediapackage.MediaPackage,
   *      java.io.OutputStream)
   */
  public void pack(MediaPackage mediaPackage, OutputStream out) throws IOException, MediaPackageException {
    ZipOutputStream zipos = new ZipOutputStream(out);
    zipos.setMethod(ZipOutputStream.DEFLATED);

    // Add all files contained in the media package root to the archive
    Stack<File> files = new Stack<File>();
    files.push(mediaPackage.getRoot());
    while (!files.empty()) {
      File file = files.pop();
      if (file.isFile()) {
        addZipEntry(mediaPackage, file, zipos);
      } else {
        for (File f : file.listFiles())
          files.push(f);
        continue;
      }
    }

    // Close the zip file
    zipos.close();
    out.close();
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackagePackager#unpack(java.io.InputStream)
   */
  public MediaPackage unpack(InputStream in) throws IOException, MediaPackageException {
    File workDir = FileSupport.getTempDirectory();
    File packageRoot = null;

    // Read and unzip the zip file
    ZipInputStream zipinputstream = new ZipInputStream(in);
    while (true) {
      // Get the next zip entry. Break out of the loop if there are
      // no more.
      ZipEntry zipentry = zipinputstream.getNextEntry();
      if (zipentry == null)
        break;

      // Is this a directory?
      if (zipentry.isDirectory()) {
        File dir = new File(workDir, zipentry.getName());
        dir.mkdirs();
        continue;
      }

      // By convention, all zip entries start with the directory name
      if (packageRoot == null) {
        File f = new File(workDir, zipentry.getName());
        while (!workDir.equals(f.getParentFile()))
          f = f.getParentFile();
        packageRoot = f;
      }

      // Read data from the zip entry. The read() method will return
      // -1 when there is no more data to read.
      byte[] rgb = new byte[1000];
      int n = 0;
      FileOutputStream fos = null;
      try {
        File f = new File(workDir, zipentry.getName());
        f.getParentFile().mkdirs();
        fos = new FileOutputStream(f);
        while ((n = zipinputstream.read(rgb)) > -1) {
          fos.write(rgb, 0, n);
        }
      } finally {
        if (fos != null)
          fos.close();
      }
      zipinputstream.closeEntry();
    }

    // Close the zip file
    zipinputstream.close();

    // And finally, build the media package
    MediaPackageBuilderFactory builderFactory = MediaPackageBuilderFactory.newInstance();
    MediaPackageBuilder builder = builderFactory.newMediaPackageBuilder();
    return builder.loadFromDirectory(packageRoot);
  }

  /**
   * Adds the media package element to the zip archive.
   * 
   * @param mediaPackage
   *          the original media package
   * @param element
   *          the media package element to add
   * @param zip
   *          the zip archive
   * @throws IOException
   *           If writing to the archive fails
   */
  private void addZipEntry(MediaPackage mediaPackage, File file, ZipOutputStream zip) throws IOException {
    FileInputStream fis = null;
    byte[] rgb = new byte[1000];
    int n = 0;

    // Calculate the CRC-32 value. This isn't strictly necessary
    // for deflated entries, but it doesn't hurt.

    CRC32 crc32 = new CRC32();
    try {
      fis = new FileInputStream(file);
      while ((n = fis.read(rgb)) > -1) {
        crc32.update(rgb, 0, n);
      }
    } finally {
      fis.close();
    }

    // Create a zip entry.
    String parentPath = mediaPackage.getRoot().getParentFile().getAbsolutePath();
    String entryPath = file.getAbsolutePath().substring(parentPath.length() + 1);
    ZipEntry zipentry = new ZipEntry(entryPath);
    zipentry.setSize(file.length());
    zipentry.setTime(file.lastModified());
    zipentry.setCrc(crc32.getValue());

    // Add the zip entry and associated data.
    zip.putNextEntry(zipentry);
    try {
      fis = new FileInputStream(file);
      while ((n = fis.read(rgb)) > -1) {
        zip.write(rgb, 0, n);
      }
    } finally {
      if (fis != null)
        fis.close();
      if (zip != null)
        zip.closeEntry();
    }
  }

}