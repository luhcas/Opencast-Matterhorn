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

package org.opencastproject.media.mediapackage.elementbuilder;

import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.MediaPackageException;

import org.w3c.dom.Node;

import java.io.File;
import java.io.IOException;

/**
 * An element builder plugin is an object that is able to recognize one ore more filetypes slated for ingest into
 * matterhorn.
 * <p/>
 * <strong>Implementation note:</strong> Builder plugins may be stateful. They are intended to be used as throw-away
 * objects.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: MediaPackageElementBuilderPlugin.java 2905 2009-07-15 16:16:05Z ced $
 */
public interface MediaPackageElementBuilderPlugin {

  /**
   * This method is called once in a plugin's life cycle. When this method is called, the plugin can make sure that
   * everything is in place for it to work properly. If this isn't the case, it should throw an exception so it will no
   * longer be bothered by the element builder.
   * 
   * @throws Exception
   *           if some unrecoverable state is reached
   */
  void setup() throws Exception;

  /**
   * This method is called before the plugin is abandoned by the element builder.
   */
  void cleanup();

  /**
   * This method is called if the media package builder tries to create a new media package element of type
   * <code>elementType</code>.
   * <p>
   * Every registered builder plugin will then be asked whether it is able to create a media package element from the
   * given element type. If this is the case for a plugin, it will then be asked to create such an element by a call to
   * {@link #newElement(org.opencastproject.media.mediapackage.MediaPackageElement.Type ,MediaPackageElementFlavor)}.
   * </p>
   * 
   * @param type
   *          the type
   * @param flavor
   *          the element flavor
   * @return <code>true</code> if the plugin is able to create such an element
   */
  boolean accept(MediaPackageElement.Type type, MediaPackageElementFlavor flavor);

  /**
   * This method is called on every registered media package builder plugin until one of these plugins returns
   * <code>true</code>. If no plugin recognises the file, it is rejected.
   * <p>
   * The parameters <code>type</code> and <code>flavor</code> may be taken as strong hints and may both be
   * <code>null</code>.
   * </p>
   * <p>
   * Implementers schould return the correct mime type for the given file if they are absolutely sure about the file.
   * Otherwise, <code>null</code> should be returned.
   * </p>
   * 
   * @param file
   *          the file to check
   * @param type
   *          the type
   * @param flavor
   *          the element flavor
   * @return the file's mime type or <code>null</code>
   * @throws IOException
   *           if the file cannot be accessed
   */
  boolean accept(File file, MediaPackageElement.Type type, MediaPackageElementFlavor flavor) throws IOException;

  /**
   * This method is called while the media package builder parses a media package manifest.
   * <p>
   * Every registered builder plugin will then be asked, whether it is able to create a media package element from the
   * given element definition.
   * </p>
   * <p>
   * The element must then be constructed and returned in the call to {@link #elementFromManifest(Node, File, boolean)}.
   * </p>
   * 
   * @param elementNode
   *          the node
   * @return <code>true</code> if the plugin is able to create such an element
   */
  boolean accept(Node elementNode);

  /**
   * Returns the priority of this builder. If more than one builder claim responsibility for a certain element, the one
   * with the highest priority is chosen.
   * <p/>
   * Implementations are free to return any integer value here, but it is a recommended best practice to regard -1 the
   * least priority, which should be the default.
   */
  int getPriority();

  /**
   * Sets the priority of this builder. If more than one builder claim responsibility for a certain element, the one
   * with the highest priority is chosen.
   * <p/>
   * Implementations are free to return any integer value here, but it is a recommended best practice to regard -1 the
   * least priority, which should be the default.
   */
  void setPriority(int priority);

  /**
   * Creates a media package element from the given file that was previously accepted.
   * 
   * @param file
   *          the file to ingest
   * @return the new media package element
   * @throws MediaPackageException
   *           if creating the media package element fails
   */
  MediaPackageElement elementFromFile(File file) throws MediaPackageException;

  /**
   * Creates a media package element from the DOM element.
   * 
   * @param elementNode
   *          the DOM node
   * @param packageRoot
   *          the media package root directory
   * @param verify
   *          <code>true</code> to verify the element's integrity
   * @return the media package element
   * @throws MediaPackageException
   */
  MediaPackageElement elementFromManifest(Node elementNode, File packageRoot, boolean verify)
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