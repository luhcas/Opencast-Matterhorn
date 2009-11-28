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
package org.opencastproject.inspection.api;

import org.opencastproject.media.mediapackage.AbstractMediaPackageElement;
import org.opencastproject.media.mediapackage.Track;

import java.net.URI;

/**
 * Anayzes media to determine its technical metadata.
 */
public interface MediaInspectionService {
  Track inspect(URI uri);

  /**
   * Equip an existing track with automatically generated metadata
   * 
   * @param originalTrack
   *          The original track that will be inspected
   * @param override
   *          In case of conflict between existing and automatically obtained metadata this switch selects preference.
   *          False..The original metadata will be kept, True..The new metadata will be used.
   * @return
   */
  Track enrich(Track originalTrack, Boolean override);

  /**
   * Equip an existing media package element with automatically generated metadata
   * 
   * @param original
   *          The original media package element that will be inspected
   * @param override
   *          In case of conflict between existing and automatically obtained metadata this switch selects preference.
   *          False..The original metadata will be kept, True..The new metadata will be used.
   * @return
   */
  AbstractMediaPackageElement enrich(AbstractMediaPackageElement element, Boolean override);
}
