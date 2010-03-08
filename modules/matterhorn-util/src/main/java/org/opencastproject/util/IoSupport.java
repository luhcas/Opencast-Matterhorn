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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Contains operations concerning IO.
 */
public class IoSupport {

  /** the logging facility provided by log4j */
  private static Logger log_ = LoggerFactory.getLogger(IoSupport.class
      .getName());

  public static String getSystemTmpDir() {
    String tmpdir = System.getProperty("java.io.tmpdir");
    if (tmpdir != null) {
      if (! tmpdir.endsWith(File.separator)) {
        tmpdir += File.separator;
      }
    } else {
      tmpdir = File.separator + "tmp";
    }
    return tmpdir;
  }

  private IoSupport() {
  }

  /**
   * Closes a <code>Closable</code> quietly so that no exceptions are thrown.
   * 
   * @param s
   *          maybe null
   */
  public static boolean closeQuietly(final Closeable s) {
    if (s == null) {
      return false;
    }
    try {
      s.close();
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Closes a <code>StreamHelper</code> quietly so that no exceptions are
   * thrown.
   * 
   * @param s
   *          maybe null
   */
  public static boolean closeQuietly(final StreamHelper s) {
    if (s == null) {
      return false;
    }
    s.stopReading();
    return true;
  }

  /**
   * Closes the processes input, output and error streams.
   * 
   * @param process
   *          the process
   * @return <code>true</code> if the streams were closed
   */
  public static boolean closeQuietly(final Process process) {
    if (process != null) {
      try {
        if (process.getErrorStream() != null)
          process.getErrorStream().close();
        if (process.getInputStream() != null)
          process.getInputStream().close();
        if (process.getOutputStream() != null)
          process.getOutputStream().close();
        return true;
      } catch (Throwable t) {
        log_.trace("Error closing process streams: " + t.getMessage());
      }
    }
    return false;
  }

  /**
   * Extracts the content from the given input stream. This method is intended
   * to faciliate handling of processes that have error, input and output
   * streams.
   * 
   * @param is
   *          the input stream
   * @return the stream content
   */
  public static String getOutput(InputStream is) {
    InputStreamReader bis = new InputStreamReader(is);
    StringBuffer outputMsg = new StringBuffer();
    char[] chars = new char[1024];
    try {
      int len = 0;
      try {
        while ((len = bis.read(chars)) > 0) {
          outputMsg.append(chars, 0, len);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    } finally {
      if (bis != null)
        try {
          bis.close();
        } catch (IOException e) {
        }
    }
    return outputMsg.toString();
  }

}
