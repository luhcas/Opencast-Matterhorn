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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.schlichtherle.io.ArchiveException;
import de.schlichtherle.io.File;
import de.schlichtherle.io.FileInputStream;
import de.schlichtherle.io.FileOutputStream;

/**
 * Provides static methods for compressing and extracting zip files using zip64 extensions when necessary.
 */
public class ZipUtil {
  
  private static final Logger logger = LoggerFactory.getLogger(ZipUtil.class);

  /**
   * Compresses source files into a zip archive.  Does not support recursive addition of directories.
   * 
   * @param sourceFiles The files to include in the root of the archive
   * @param destination The path to put the resulting zip archive file.
   * @return the resulting zip archive file
   */
  public static java.io.File zip(java.io.File[] sourceFiles, String destination) {
    return zip(sourceFiles, destination, false);
  }

  /**
   * Compresses a files into a zip archive. May add files recursively.
   *
   * @param sourceFiles The files to include in the root of the archive
   * @param destination The path to put the resulting zip archive file.
   * @param recursively Set true to recursively add directories.
   * @return the resulting zip archive file
   */
  public static java.io.File zip(java.io.File[] sourceFiles, String destination, boolean recursively) {
    if (sourceFiles == null || sourceFiles.length <= 0) {
      throw new IllegalArgumentException("sourceFiles must include at least 1 file");
    }
    if (destination == null || "".equals(destination)) {
      throw new IllegalArgumentException("destination must be set");
    }
    // patch from ruben.perez to ensure only exposes java.io.File: http://issues.opencastproject.org/jira/browse/MH-1698 - AZ
    java.io.File normalFile = new java.io.File(destination);
    File zipFile = new File(normalFile);
    _zip(zipFile, sourceFiles, recursively, -1);

    // Solves issue MH-1809 (java.io.File.length() doesn't return actual zip file size) 
    try {
      File.umount();
    } catch (ArchiveException e) {
      zipFile.delete();
      throw new RuntimeException(e);
    }

    return normalFile;
  }

  private static void _zip(File zipFile, java.io.File[] files, boolean recursively, int basePathLen) {
    for (java.io.File f : files) {
      OutputStream out = null;
      InputStream in = null;
      int curBasePathLen = basePathLen < 0
          ? lenSkippingSlash(f.getParentFile().getAbsolutePath())
          : basePathLen;
      if (f.isDirectory()) {
        if (recursively)
          _zip(zipFile, f.listFiles(), recursively, curBasePathLen);
      } else {
        try {
          out = new FileOutputStream(zipFile.getAbsolutePath() + "/" + f.getAbsolutePath().substring(curBasePathLen));
          in = new FileInputStream(f);
          File.cat(in, out);
        } catch (Exception e) {
          zipFile.delete();
          throw new RuntimeException(e);
        } finally {
          if (in != null) {
            try {
              in.close();
            } catch (IOException e) {
              logger.error(e.getMessage());
            }
          }
          if (out != null) {
            try {
              out.close();
            } catch (IOException e) {
              logger.error(e.getMessage());
            }
          }
        }
      }
    }
  }

  private static int lenSkippingSlash(String path) {
    return path.endsWith(File.separator) ? path.length() : path.length() + 1;
  }

  /**
   * Extracts a zip file to a directory.
   * 
   * @param zipFile The source zip archive
   * @param destination the destination to extract the zip archive.  If this destination directory does not exist, it
   * will be created.
   */
  public static void unzip(java.io.File zipFile, java.io.File destination) {
    if (zipFile == null) {
      throw new IllegalArgumentException("zipFile must be set");
    }
    if (destination == null) {
      throw new IllegalArgumentException("destination must be set");
    }
    if (destination.exists() && destination.isFile()) {
      throw new IllegalArgumentException("destination file must be a directory");
    }
    if (!destination.exists())
      if (!destination.mkdirs())
        throw new RuntimeException("cannot create " + destination);

    File zip = new File(zipFile);
    if (zip.copyAllTo(destination)) {
      try {
        File.umount();
      } catch (ArchiveException e) {
        logger.error(e.getMessage());
      }
    }
  }
}
