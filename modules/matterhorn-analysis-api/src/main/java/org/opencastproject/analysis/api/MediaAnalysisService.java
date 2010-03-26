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

import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.metadata.mpeg7.Mpeg7Catalog;

import java.net.URL;

/**
 * Api for media analysis implementations, aimed at extracting metadata information from audio and audiovisual tracks.
 */
public interface MediaAnalysisService {

  /**
   * Takes the given track and returns metadata in an mpeg-7 format.
   * 
   * @param mediaUrl
   *          url of the file location
   * @return the metadata
   */
  Mpeg7Catalog analyze(URL mediaUrl) throws MediaAnalysisException;

  /**
   * Gets the specified track from the media package and returns metadata in an mpeg-7 format.
   * 
   * @param mediaPackage
   *          the media package
   * @param trackId
   *          identifier of a track contained in the media package
   * @return the metadata
   */
  Mpeg7Catalog analyze(MediaPackage mediaPackage, String trackId) throws MediaAnalysisException;

  /**
   * Returns the flavor that this media analysis service implementation produces. The flavor will usually be of type
   * <code>mpeg-7</code> and contain a specific subtype such as <code>segments</code> or <code>text</code>.
   * 
   * @return the flavor that is produced by this analysis service
   */
  MediaPackageElementFlavor produces();

  /**
   * Returns a list of catalog flavors that need to be present inside a mediapackage in order to enable this analyzer to
   * successfully deliver results.
   * <p>
   * Media analysis implementations that don't have requirements will return an empty array.
   * 
   * @return the required catalog flavors
   */
  MediaPackageElementFlavor[] requires();

}
