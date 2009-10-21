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

import org.w3c.dom.Node;

import java.io.IOException;
import java.net.URL;

/**
 * A media package element builder provides factory methods for the creation and loading of media package elements from
 * files.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: MediaPackageElementBuilder.java 678 2008-08-05 14:56:22Z wunden $
 */
public interface MediaPackageElementBuilder {

  /**
   * Creates a media package element from the given file that was previously accepted.
   * <p>
   * Since only the file is given, it is possible, that the best builder plugin cannot be uniquely identified and may
   * require additional contraints, e. g. a matching filename. Be sure to check the documentation of the corresponding
   * plugin for details.
   * </p>
   * 
   * @param url
   *          the element location
   * @return the new media package element
   * @throws MediaPackageException
   *           if creating the media package element fails
   */
  MediaPackageElement elementFromURL(URL url) throws MediaPackageException;

  /**
   * Creates a media package element from the given file that was previously accepted, while <code>type</code> and
   * <code>flavor</code> may be taken as strong hints and may both be <code>null</code>.
   * <p>
   * If only the file is given, it is possible, that the best suited builder plugin cannot be uniquely identified and
   * may require additional contraints, e. g. a matching filename. Be sure to check the documentation of the
   * corresponding builder plugin for details.
   * </p>
   * 
   * @param url
   *          the element location
   * @param type
   *          the element type
   * @param flavor
   *          the element flavor
   * @return the new media package element
   * @throws MediaPackageException
   *           if creating the media package element fails
   */
  MediaPackageElement elementFromURL(URL url, MediaPackageElement.Type type, MediaPackageElementFlavor flavor)
          throws MediaPackageException;

  /**
   * Creates a media package element from the DOM element.
   * 
   * @param elementNode
   *          the DOM node
   * @param serializer
   *          the media package serializer
   * @return the media package element
   * @throws MediaPackageException
   *           if reading the file from manifest fails
   */
  MediaPackageElement elementFromManifest(Node elementNode, MediaPackageSerializer serializer)
          throws MediaPackageException;

  /**
   * Creates a new media package elment of the specified type.
   * 
   * @param type
   *          the element type
   * @param flavor
   *          the element flavor
   * @return the new media package element
   * @throws IOException
   *           if the media package element's file cannot be created
   */
  MediaPackageElement newElement(MediaPackageElement.Type type, MediaPackageElementFlavor flavor) throws IOException;

}
