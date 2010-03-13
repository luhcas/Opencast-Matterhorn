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

package org.opencastproject.media.mediapackage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Interface describing a media package packager, that will, for example, pack a media package structure into a jar
 * file.
 */
public interface MediaPackagePackager {

  /**
   * Reads a media package from the given input stream, unpacked into the <code>packageRoot</code> directory.
   * 
   * @param in
   *          the archive input stream
   * 
   * @return the media package
   * @throws IOException
   *           if accessing the package fails
   * @throws MediaPackageException
   *           if unpacking the jar file fails
   */
  MediaPackage unpack(InputStream in) throws IOException, MediaPackageException;

  /**
   * Saves a media package to the given output stream.
   * 
   * @param mediaPackage
   *          the media package
   * @param out
   *          the output stream
   * @throws IOException
   *           if accessing a media package file fails
   * @throws MediaPackageException
   *           if creating the jar file fails
   */
  void pack(MediaPackage mediaPackage, OutputStream out) throws IOException, MediaPackageException;

}
