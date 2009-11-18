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
import org.opencastproject.captions.api.CaptionshandlerService;
import org.opencastproject.media.mediapackage.Attachment;
import org.opencastproject.media.mediapackage.Catalog;
import org.opencastproject.media.mediapackage.DublinCoreCatalog;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.MediaPackageReferenceImpl;
import org.opencastproject.media.mediapackage.Track;

import java.net.URL;

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
  public URL getMediaURL() {
    // TODO make this handle the tracks better
    URL url = null;
    Track[] tracks = mediaPackage.getTracks();
    if (tracks != null && tracks.length > 0) {
      Track track = tracks[0];
      url = track.getURL();
    }
    return url;
  }
  public String getTitle() {
    // get the title of the media package
    String title = null;
    Catalog[] dcCatalogs = mediaPackage.getCatalogs(DublinCoreCatalog.FLAVOR, MediaPackageReferenceImpl.ANY_MEDIAPACKAGE);
    if (dcCatalogs != null && dcCatalogs.length > 0) {
      DublinCoreCatalog dc = (DublinCoreCatalog) dcCatalogs[0];
      title = dc.getFirst(DublinCoreCatalog.PROPERTY_TITLE);
    }
    return title;
  }
  public URL getCaptionsURL(String captionType) {
    URL url = null;
    Attachment[] attachments = mediaPackage.getAttachments(makeCaptionsFlavor(captionType));
    if (attachments != null && attachments.length > 0) {
      url = attachments[0].getURL();
    }
    return url;
  }

  public static MediaPackageElementFlavor makeCaptionsFlavor(String type) {
    if (type == null || "".equals(type)) {
      throw new IllegalArgumentException("type must be set");
    }
    return new MediaPackageElementFlavor(CaptionshandlerService.CAPTIONS_OPERATION_NAME, type.toLowerCase());
  }

}

