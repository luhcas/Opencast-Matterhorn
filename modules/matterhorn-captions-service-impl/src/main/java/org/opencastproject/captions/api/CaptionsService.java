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

package org.opencastproject.captions.api;

import java.io.InputStream;
import java.util.List;

/**
 * The API for the captions handler service
 */
public interface CaptionsService {

  public static final String CAPTIONS_MEDIA_TAG = "captioning";
  public static final String CAPTIONS_OPERATION_NAME = "captions";
  public static final String CAPTIONS_ELEMENT = "captions-";
  public static final String CAPTIONS_TYPE_TIMETEXT = "TimeText";
  public static final String CAPTIONS_TYPE_DESCAUDIO = "DescriptiveAudio";

  /**
   * Get the list of all captionable media items,
   * this will probably be overridden for local implementations
   * 
   * @param start the start item (for paging), <=0 for first item
   * @param max the maximum items to return (for paging), <=0 for up to 50 items
   * @param sort the sort order string (e.g. 'title asc')
   * @return the results with a list of CaptionsMediaItem OR empty if none
   */
  public CaptionsResults getCaptionableMedia(int start, int max, String sort);

  /**
   * Update a media package with some captions data based on the workflow id
   * 
   * @param workflowId the workflow instance id
   * @param captionType the type of caption data (could be {@value #CAPTIONS_TYPE_TIMETEXT} or {@value #CAPTIONS_TYPE_DESCAUDIO} for example)
   * @param data the captions data to store with the media package
   * @return the updated media package
   */
  public CaptionsMediaItem updateCaptions(String workflowId, String captionType, InputStream data);

  /**
   * Retrieve a captions media item based on the workflow Id
   * @param workflowId the full identifier of a workflow
   * @return the item OR null if none found for this workflow
   */
  public CaptionsMediaItem getCaptionsMediaItem(String workflowId);

  /**
   * This will hold the results of a request for captionable media
   */
  public static class CaptionsResults {
    public int start;
    public int max;
    public int total;
    public List<CaptionsMediaItem> results;
    public CaptionsResults(List<CaptionsMediaItem> results, int start, int max, int total) {
      this.results = results;
      this.start = start;
      this.max = max;
      this.total = total;
    }
  }

}

