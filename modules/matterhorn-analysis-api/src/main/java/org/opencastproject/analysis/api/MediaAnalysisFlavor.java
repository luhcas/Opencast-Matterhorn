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
package org.opencastproject.analysis.api;

import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;

/**
 * This interface defines common flavors as used by clients of media analysis.
 */
public interface MediaAnalysisFlavor {

  /** The flavor produced by video segmentation */
  MediaPackageElementFlavor SEGMENTS_FLAVOR = new MediaPackageElementFlavor("mpeg-7", "segments");

  /** The flavor produced by text extraction */
  MediaPackageElementFlavor TEXTS_FLAVOR = new MediaPackageElementFlavor("mpeg-7", "text");

  /** The flavor produced by speech recognition */
  MediaPackageElementFlavor SPEECH_FLAVOR = new MediaPackageElementFlavor("mpeg-7", "speech");

}
