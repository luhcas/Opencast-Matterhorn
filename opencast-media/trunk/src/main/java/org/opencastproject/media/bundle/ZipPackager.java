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

package org.opencastproject.media.bundle;

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
 * Reads bundles from and saves to zip streams.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public class ZipPackager implements BundlePackager {

  /**
   * @see org.opencastproject.media.bundle.BundlePackager#pack(org.opencastproject.media.bundle.Bundle,
   *      java.io.OutputStream)
   */
  public void pack(Bundle bundle, OutputStream out) throws IOException,
      BundleException {
    ZipOutputStream zipos = new ZipOutputStream(out);
    zipos.setMethod(ZipOutputStream.DEFLATED);

    // Add all files contained in the bundle root to the archive
    Stack<File> files = new Stack<File>();
    files.push(bundle.getRoot());
    while (!files.empty()) {
      File file = files.pop();
      if (file.isFile()) {
        addZipEntry(bundle, file, zipos);
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
   * @see org.opencastproject.media.bundle.BundlePackager#unpack(java.io.InputStream)
   */
  public Bundle unpack(InputStream in) throws IOException, BundleException {
    File workDir = FileSupport.getTempDirectory();
    File bundleRoot = null;

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
      if (bundleRoot == null) {
        File f = new File(workDir, zipentry.getName());
        while (!workDir.equals(f.getParentFile()))
          f = f.getParentFile();
        bundleRoot = f;
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

    // And finally, build the bundle
    BundleBuilderFactory builderFactory = BundleBuilderFactory.newInstance();
    BundleBuilder builder = builderFactory.newBundleBuilder();
    return builder.loadFromDirectory(bundleRoot);
  }

  /**
   * Adds the bundle element to the zip archive.
   * 
   * @param bundle
   *          the original bundle
   * @param element
   *          the bundle element to add
   * @param zip
   *          the zip archive
   * @throws IOException
   *           If writing to the archive fails
   */
  private void addZipEntry(Bundle bundle, File file, ZipOutputStream zip)
      throws IOException {
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
    String parentPath = bundle.getRoot().getParentFile().getAbsolutePath();
    String entryPath = file.getAbsolutePath()
        .substring(parentPath.length() + 1);
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