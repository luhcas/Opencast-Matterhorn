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

package org.opencastproject.videosegmenter.api;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.Track;

/**
 * Api for video segmentation implementations, aimed at detecting scenes in audiovisual tracks.
 */
public interface VideoSegmenterService extends JobProducer {

  /** Receipt type */
  public static final String JOB_TYPE = "org.opencastproject.videosegmenter";

  /**
   * Takes the given element and returns a receipt that can be used to get the resulting {@link MediaPackageElement}.
   * 
   * @param track
   *          element to segment
   * @param block
   *          whether to block the calling thread until the analysis is complete
   * @return the metadata
   */
  Job segment(Track track, boolean block) throws VideoSegmenterException;

}
