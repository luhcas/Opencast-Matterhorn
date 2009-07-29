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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Stack;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.CRC32;

/**
 * Reads bundles from and saves to jar streams.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public class JarPackager implements BundlePackager {

  /** the logging facility provided by log4j */
  private final static Logger log_ = LoggerFactory.getLogger(JarPackager.class);

  /**
   * @see org.opencastproject.media.bundle.BundlePackager#pack(org.opencastproject.media.bundle.Bundle,
   *      java.io.OutputStream)
   */
  public void pack(Bundle bundle, OutputStream stream) throws BundleException {
    try {
      JarOutputStream out = new JarOutputStream(stream, new Manifest());
      out.setComment("Bundle " + bundle);

      // Add all files contained in the bundle root to the archive
      Stack<File> files = new Stack<File>();
      files.push(bundle.getRoot());
      while (!files.empty()) {
        File file = files.pop();
        if (file.isFile()) {
          addJarEntry(bundle, file, out);
        } else {
          for (File f : file.listFiles()) {
            files.push(f);
          }
        }
        out.closeEntry();
      }
      out.close();
      stream.close();
      log_.debug("Jar file created");
    } catch (Exception e) {
      throw new BundleException("Error creating jar file from bundle " + bundle
          + ": " + e.getMessage(), e);
    }
  }

  /**
   * @see org.opencastproject.media.bundle.BundlePackager#unpack(java.io.InputStream)
   */
  public Bundle unpack(InputStream in) throws IOException, BundleException {
    File workDir = FileSupport.getTempDirectory();
    File bundleRoot = null;

    // Read and unzip the zip file
    JarInputStream jarInputStream = new JarInputStream(in);
    while (true) {
      // Get the next jar entry. Break out of the loop if there are
      // no more.
      JarEntry jarEntry = jarInputStream.getNextJarEntry();
      if (jarEntry == null)
        break;

      // Is this a directory?
      if (jarEntry.isDirectory()) {
        File dir = new File(workDir, jarEntry.getName());
        dir.mkdirs();
        continue;
      }

      // By convention, all jar entries start with the directory name
      if (bundleRoot == null) {
        File f = new File(workDir, jarEntry.getName());
        while (!workDir.equals(f.getParentFile()))
          f = f.getParentFile();
        bundleRoot = f;
      }

      // Read data from the jar entry. The read() method will return
      // -1 when there is no more data to read.
      byte[] rgb = new byte[1000];
      int n = 0;
      FileOutputStream fos = null;
      try {
        File f = new File(workDir, jarEntry.getName());
        f.getParentFile().mkdirs();
        fos = new FileOutputStream(f);
        while ((n = jarInputStream.read(rgb)) > -1) {
          fos.write(rgb, 0, n);
        }
      } finally {
        if (fos != null)
          fos.close();
      }
      jarInputStream.closeEntry();
    }

    // Close the jar file
    jarInputStream.close();

    // And finally, build the bundle
    BundleBuilderFactory builderFactory = BundleBuilderFactory.newInstance();
    BundleBuilder builder = builderFactory.newBundleBuilder();
    return builder.loadFromDirectory(bundleRoot);
  }

  /**
   * Adds the bundle element to the jar archive.
   * 
   * @param bundle
   *          the original bundle
   * @param element
   *          the bundle element to add
   * @param jar
   *          the archive
   * @throws IOException
   *           If writing to the archive fails
   */
  private void addJarEntry(Bundle bundle, File file, JarOutputStream jar)
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
    JarEntry jarentry = new JarEntry(entryPath);
    jarentry.setSize(file.length());
    jarentry.setTime(file.lastModified());
    jarentry.setCrc(crc32.getValue());

    // Add the zip entry and associated data.
    jar.putNextEntry(jarentry);
    try {
      fis = new FileInputStream(file);
      while ((n = fis.read(rgb)) > -1) {
        jar.write(rgb, 0, n);
      }
    } finally {
      if (fis != null)
        fis.close();
      if (jar != null)
        jar.closeEntry();
    }
  }

}