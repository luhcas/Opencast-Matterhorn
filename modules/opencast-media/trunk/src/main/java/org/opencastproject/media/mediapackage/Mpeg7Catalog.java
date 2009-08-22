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

import org.opencastproject.media.mediapackage.mpeg7.Mpeg7;

import org.w3c.dom.Document;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * The <code>MPEG7</code> catalog encapsulates MPEG-7 metadata.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: Mpeg7Catalog.java 2905 2009-07-15 16:16:05Z ced $
 */
public interface Mpeg7Catalog extends XMLCatalog, Mpeg7 {

  /** Element type definition */
  MediaPackageElementFlavor FLAVOR = new MediaPackageElementFlavor("metadata", "mpeg-7", "MPEG-7 catalog");

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