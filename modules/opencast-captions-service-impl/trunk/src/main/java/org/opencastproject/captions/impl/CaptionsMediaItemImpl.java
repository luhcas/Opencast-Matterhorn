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

package org.opencastproject.captions.impl;

import org.opencastproject.captions.api.CaptionsMediaItem;
import org.opencastproject.captions.api.CaptionsService;
import org.opencastproject.media.mediapackage.Attachment;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.Track;

import org.apache.commons.lang.ArrayUtils;

import java.net.URI;

public class CaptionsMediaItemImpl implements CaptionsMediaItem {
  String workflowId;
  MediaPackage mediaPackage;
  public CaptionsMediaItemImpl(String workflowId, MediaPackage mediaPackage) {
    if (workflowId == null) {
      throw new IllegalArgumentException("workflowId must not be null");
    }
    if (mediaPackage == null) {
      throw new IllegalArgumentException("mediaPackage must not be null");
    }
    this.workflowId = workflowId;
    this.mediaPackage = mediaPackage;
  }
  public String getWorkflowId() {
    return workflowId;
  }
  public MediaPackage getMediaPackage() {
    return mediaPackage;
  }
  public String getMediaPackageId() {
    return mediaPackage.getIdentifier().compact();
  }
  public URI getMediaURI() {
    // TODO make this handle the tracks better
    URI url = null;
    Track[] tracks = mediaPackage.getTracks();
    if (tracks != null && tracks.length > 0) {
      // get the tagged track first if there is one
      Track current = null;
      for (Track track : tracks) {
        String[] tags = track.getTags();
        if (tags != null 
                && ArrayUtils.contains(tags, CaptionsService.CAPTIONS_MEDIA_TAG) ) {
          current = track;
          break;
        }
      }
      if (current == null) {
        current = tracks[0]; // default to the first one in the set otherwise
      }
      url = current.getURI();
    }
    return url;
  }
  public String getTitle() {
    return mediaPackage.getTitle();
  }
  public URI getCaptionsURI(String captionType) {
    URI url = null;
    Attachment[] attachments = mediaPackage.getAttachments(makeCaptionsFlavor(captionType));
    if (attachments != null && attachments.length > 0) {
      url = attachments[0].getURI();
    }
    return url;
  }

  public static MediaPackageElementFlavor makeCaptionsFlavor(String type) {
    if (type == null || "".equals(type)) {
      throw new IllegalArgumentException("type must be set");
    }
    return new MediaPackageElementFlavor(CaptionsService.CAPTIONS_OPERATION_NAME, type.toLowerCase());
  }

}

