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

package org.opencastproject.media.bundle;

import org.opencastproject.media.analysis.AudioStreamMetadata;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Takes
 * {@link ch.ethz.replay.core.api.common.media.analysis.AudioStreamMetadata} in
 * the Bundle context.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @author Christoph E. Driessen <ced@neopoly.de>
 * @version $Id$
 */
public interface AudioSettings extends Cloneable {

  /**
   * Returns the audio metadata.
   */
  AudioStreamMetadata getMetadata();

  /**
   * Normally the system determines the technical metadata for a track. But
   * there may be some situations where it comes handy to provide the metadata
   * from the outside, e.g. when they are edited manually.
   */
  void setMetadata(AudioStreamMetadata metadata);

  /**
   * Returns the xml representation of this settings object as found in the
   * bundle manifest.
   * 
   * @param document
   *          the manifest dom
   * @return the serialized settings object
   */
  Node toManifest(Document document);

  Object clone() throws CloneNotSupportedException;

}