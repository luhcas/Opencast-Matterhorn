/**
 *  Copyright 2009 Opencast Project (http://www.opencastproject.org)
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

import org.opencastproject.media.bundle.handle.Handle;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.MimeTypes;

import java.io.File;

/**
 * Object containing information about the contents of the associated bundle.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public interface Manifest {

  /** The manifest file name */
  String FILENAME = "index.xml";

  /** Bundle element type */
  BundleElement.Type TYPE = BundleElement.Type.Manifest;

  /** The manifest element type */
  BundleElementFlavor ELEMENT_TYPE = new BundleElementFlavor("metadata",
      "manifest");

  /** The manifest element type */
  MimeType MIME_TYPE = MimeTypes.XML;

  /**
   * Returns the bundle identifier as indicated in the manifest's head section.
   * 
   * @return the bundle identifier
   */
  Handle getIdentifier();

  /**
   * Returns a list of files listed by this manifest as being bundle members.
   * 
   * @return the list of files
   */
  BundleElement[] getEntries();

  /**
   * Returns a reference to the element's file object.
   * 
   * @return the file reference
   */
  File getFile();

  /**
   * Returns the bundle start time.
   * 
   * @return the start time
   */
  long getStartDate();

  /**
   * Returns the bundle duration in milliseconds.
   * 
   * @return the duration
   */
  long getDuration();

}