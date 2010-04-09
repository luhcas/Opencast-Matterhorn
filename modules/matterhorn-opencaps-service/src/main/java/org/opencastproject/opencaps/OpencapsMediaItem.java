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

package org.opencastproject.opencaps;

import org.opencastproject.media.mediapackage.Catalog;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.Track;

import java.net.URI;

public class OpencapsMediaItem {

  String workflowId;
  MediaPackage mediaPackage;

  public OpencapsMediaItem(String workflowId, MediaPackage mediaPackage) {
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
    Track[] tracks = mediaPackage.getTracksByTag(OpencapsService.MEDIA_TAG);
    if (tracks.length == 0) {
      return null;
    } else {
      return tracks[0].getURI();
    }
  }

  public String getTitle() {
    return mediaPackage.getTitle();
  }

  /**
   * @param captionType
   *          the caption type string from {@link OpencapsService#CAPTIONS_TYPE_TIMETEXT}
   * @return the url OR null if none found
   */
  public URI getCaptionsURI(String captionType) {
    MediaPackageElementFlavor flavor = new MediaPackageElementFlavor(OpencapsService.FLAVOR_TYPE, captionType);
    Catalog[] captions = mediaPackage.getCatalogs(flavor);
    return captions.length > 0 ? captions[0].getURI() : null;
  }

}
