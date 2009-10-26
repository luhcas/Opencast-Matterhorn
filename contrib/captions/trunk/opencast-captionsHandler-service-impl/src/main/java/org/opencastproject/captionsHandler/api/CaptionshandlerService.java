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

package org.opencastproject.captionsHandler.api;

import org.opencastproject.media.mediapackage.MediaPackage;
import java.io.InputStream;
import java.util.List;

/**
 * The API for the captions handler service
 */
public interface CaptionshandlerService {

  /**
   * Get the list of all captionable media packages,
   * currently this will simply retrieve media packages by date,
   * this will probably be overridden for local implementations
   * 
   * @param start the start item (for paging), <=0 for first item
   * @param max the maximum items to return (for paging), <=0 for up to 50 items
   * @param sort the sort order string (e.g. 'title asc')
   * @return the list of MediaPackage or empty if none
   */
  public List<MediaPackage> getCaptionableMedia(int start, int max, String sort);

  /**
   * Update a media package with some captions data
   * 
   * @param mediaId the media package id
   * @param captionType the type of caption data (could be {@value #CAPTIONS_TYPE_TIMETEXT} or {@value #CAPTIONS_TYPE_DESCAUDIO} for example)
   * @param data the captions data to store with the media package
   * @return the updated media package
   */
  public MediaPackage updateCaptions(String mediaId, String captionType, InputStream data);

  /**
   * Gets an entity by ID.  Replace this method with your service operations.
   */
  public CaptionshandlerEntity getCaptionshandlerEntity(String id);

  /**
   * Adds an entity for a given ID.
   * 
   * @param id The ID of the entity
   * @param entity The entity to save
   */
  public void saveCaptionshandlerEntity(CaptionshandlerEntity entity);
}

