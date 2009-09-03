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
package org.opencastproject.inspection.impl.endpoints;

import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.media.mediapackage.jaxb.AttachmentType;
import org.opencastproject.media.mediapackage.jaxb.AttachmentsType;
import org.opencastproject.media.mediapackage.jaxb.ChecksumType;
import org.opencastproject.media.mediapackage.jaxb.MediapackageType;
import org.opencastproject.media.mediapackage.jaxb.ObjectFactory;

import java.net.URL;

/**
 * A web service endpoint delegating logic to the {@link MediaInspectionService}
 */
public class MediaInspectionWebServiceImpl implements MediaInspectionWebService {
  protected MediaInspectionService service;
  public void setService(MediaInspectionService service) {
    this.service = service;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.inspection.impl.endpoints.MediaInspectionWebService#inspect(java.net.URL)
   */
  public Track inspect(URL url) {
    return service.inspect(url);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.inspection.impl.endpoints.MediaInspectionWebService#getMediaPackage()
   */
  public MediapackageType getMediaPackage() {
    ObjectFactory of = new ObjectFactory();

    // Create the media package, and set some example values
    MediapackageType mp = of.createMediapackageType();
    mp.setId("handle/0000");
    mp.setDuration(1024);

    // Create an attachment
    AttachmentType attachment = of.createAttachmentType();
    ChecksumType attachment1Checksum = of.createChecksumType();
    attachment1Checksum.setType("md5");
    attachment1Checksum.setValue("abc123def");
    attachment.setId("attachment-1");
    attachment.setChecksum(attachment1Checksum);

    // Create the attachments container
    AttachmentsType attachments = of.createAttachmentsType();

    // Add the attachment to the container
    attachments.getAttachment().add(attachment);
    
    return mp;
  }
  
}
