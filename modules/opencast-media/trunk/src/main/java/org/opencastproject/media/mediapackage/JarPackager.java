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
 * Reads media packages from and saves to jar streams.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: JarPackager.java 1655 2008-12-08 16:14:11Z wunden $
 */
public class JarPackager implements MediaPackagePackager {

  /** the logging facility provided by log4j */
  private final static Logger log_ = LoggerFactory.getLogger(JarPackager.class.getName());

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackagePackager#pack(org.opencastproject.media.mediapackage.MediaPackage,
   *      java.io.OutputStream)
   */
  public void pack(MediaPackage mediaPackage, OutputStream stream) throws MediaPackageException {
    try {
      JarOutputStream out = new JarOutputStream(stream, new Manifest());
      out.setComment("Media package " + mediaPackage);

      // Add all files contained in the media package root to the archive
      Stack<File> files = new Stack<File>();
      files.push(mediaPackage.getRoot());
      while (!files.empty()) {
        File file = files.pop();
        if (file.isFile()) {
          addJarEntry(mediaPackage, file, out);
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
      throw new MediaPackageException("Error creating jar file from media package " + mediaPackage + ": "
              + e.getMessage(), e);
    }
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackagePackager#unpack(java.io.InputStream)
   */
  public MediaPackage unpack(InputStream in) throws IOException, MediaPackageException {
    File workDir = FileSupport.getTempDirectory();
    File packageRoot = null;

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
      if (packageRoot == null) {
        File f = new File(workDir, jarEntry.getName());
        while (!workDir.equals(f.getParentFile()))
          f = f.getParentFile();
        packageRoot = f;
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

    // And finally, build the media package
    MediaPackageBuilderFactory builderFactory = MediaPackageBuilderFactory.newInstance();
    MediaPackageBuilder builder = builderFactory.newMediaPackageBuilder();
    return builder.loadFromDirectory(packageRoot);
  }

  /**
   * Adds the media package element to the jar archive.
   * 
   * @param mediaPackage
   *          the original media package
   * @param element
   *          the media package element to add
   * @param jar
   *          the archive
   * @throws IOException
   *           If writing to the archive fails
   */
  private void addJarEntry(MediaPackage mediaPackage, File file, JarOutputStream jar) throws IOException {
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