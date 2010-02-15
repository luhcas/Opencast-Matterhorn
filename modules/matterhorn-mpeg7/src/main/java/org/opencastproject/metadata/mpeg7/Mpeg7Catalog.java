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

package org.opencastproject.metadata.mpeg7;

import org.opencastproject.media.mediapackage.Catalog;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;

import org.w3c.dom.Document;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * The <code>MPEG7</code> catalog encapsulates MPEG-7 metadata.
 */
public interface Mpeg7Catalog extends Mpeg7, Catalog {

  /** Element type definition */
  // TODO Remove the generic mpeg7 flavor
  MediaPackageElementFlavor FLAVOR = new MediaPackageElementFlavor("metadata", "mpeg-7", "MPEG-7 slides catalog");
  MediaPackageElementFlavor SLIDES_FLAVOR = new MediaPackageElementFlavor("segments", "slides", "MPEG-7 slides catalog");
  MediaPackageElementFlavor SPEECH_FLAVOR = new MediaPackageElementFlavor("segments", "speech", "MPEG-7 speech catalog");
  MediaPackageElementFlavor CHAPTER_FLAVOR = new MediaPackageElementFlavor("segments", "chapter", "MPEG-7 chapters catalog");

  /**
   * Saves the catalog to disk.
   * 
   * todo think about hiding technical exceptions
   * 
   * @throws ParserConfigurationException
   *           if the xml parser environment is not correctly configured
   * @throws TransformerException
   *           if serialization of the metadata document fails
   * @throws IOException
   *           if an error with catalog file handling occurs
   */
  Document toXml() throws ParserConfigurationException, TransformerException, IOException;

}
