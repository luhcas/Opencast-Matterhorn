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

public class CaptionsMediaItem {
  String workflowId;
  MediaPackage mediaPackage;
  public CaptionsMediaItem(String workflowId, MediaPackage mediaPackage) {
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
    return mediaPackage.getIdentifier().getFullName();
  }
}

