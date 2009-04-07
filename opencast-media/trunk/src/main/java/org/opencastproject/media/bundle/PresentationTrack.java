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

/**
 * Interface description for presentation tracks. A presentation track mainly
 * contains presenter slides.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public interface PresentationTrack extends VideoTrack, AudioTrack {

  /** Bundle element type */
  BundleElement.Type TYPE = BundleElement.Type.Track;

  /** Element flavor definition */
  BundleElementFlavor FLAVOR = new BundleElementFlavor("track", "presentation");

  /** Presentation track flavor description */
  String FLAVOR_DESCRIPTION = "Presentation track";

  /** Prefix for presentation track filenames */
  String FILENAME_PREFIX = "presentation";

}