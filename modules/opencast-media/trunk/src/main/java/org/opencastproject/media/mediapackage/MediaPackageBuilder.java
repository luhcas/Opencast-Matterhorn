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

import java.io.IOException;
import java.io.InputStream;

import org.opencastproject.media.mediapackage.handle.Handle;

/**
 * A media package builder provides factory methods for the creation of media packages from manifest files, packages,
 * directories or from sratch.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: MediaPackageBuilder.java 2700 2009-03-18 15:49:02Z wunden $
 */
public interface MediaPackageBuilder {

  /**
   * Creates a new media package in the temporary directory defined by the java runtime property
   * <code>java.io.tmpdir</code>.
   * 
   * @return the new media package
   * @throws MediaPackageException
   *           if creation of the new media package fails
   */
  MediaPackage createNew() throws MediaPackageException;

  /**
   * Creates a new media package in the temporary directory defined by the java runtime property
   * <code>java.io.tmpdir</code>.
   * <p>
   * The name of the media package root folder will be equal to the handle value.
   * </p>
   * 
   * @param handle
   *          the media package identifier
   * @return the new media package
   * @throws MediaPackageException
   *           if creation of the new media package fails
   */
  MediaPackage createNew(Handle identifier) throws MediaPackageException;

  /**
   * Loads a media package from the manifest.
   * 
   * @param is
   *          the media package manifest input stream
   * @return the media package
   * @throws MediaPackageException
   *           if loading of the media package fails
   */
  MediaPackage loadFromManifest(InputStream is) throws MediaPackageException;

  /**
   * Loads a media package from the input stream, using the provided packager to decode the stream.
   * 
   * @param packager
   *          the packager
   * @param in
   *          the input stream
   * @return the media package
   * @throws MediaPackageException
   *           if loading of the media package fails
   */
  MediaPackage loadFromPackage(MediaPackagePackager packager, InputStream in) throws IOException, MediaPackageException;
  
  /**
   * Sets the media package serializer that is used to resolve urls and helps
   * in serialization and deserialization of media package elements.
   * 
   * @param serializer
   *          the serializer
   */
  void setSerializer(MediaPackageSerializer serializer);
 
  /**
   * Returns the currently active serializer. The serializer is used to resolve
   * urls and helps in serialization and deserialization of media package
   * elements.
   * 
   * @return the serializer
   * @see #setSerializer(MediaPackageSerializer)
   */
  MediaPackageSerializer getSerializer();

}