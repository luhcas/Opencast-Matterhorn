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
package org.opencastproject.composer.impl.endpoint;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.impl.EncodingProfileImpl;
import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.media.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.media.mediapackage.jaxb.MediapackageType;
import org.opencastproject.media.mediapackage.jaxb.TrackType;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.List;

import javax.jws.WebParam;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * A web service endpoint delegating logic to the {@link MediaInspectionService}
 */
public class ComposerWebServiceImpl implements ComposerWebService {
  private static final Logger logger = LoggerFactory.getLogger(ComposerRestService.class);
  
  protected ComposerService composerService;
  public void setComposerService(ComposerService composerService) {
    this.composerService = composerService;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.composer.impl.endpoint.ComposerWebService#encode(org.opencastproject.media.mediapackage.jaxb.MediapackageType, java.lang.String, java.lang.String)
   */
  public TrackType encode(
          @WebParam(name="mediapackage") MediapackageType mediaPackageType,
          @WebParam(name="sourceTrackId") String sourceTrackId,
          @WebParam(name="profileId") String profileId) throws Exception {
    if(mediaPackageType == null || sourceTrackId == null || profileId == null) {
      throw new IllegalArgumentException("mediapackage, sourceTrackId, and profileId must not be null");
    }
    logger.info("Encoding track " + sourceTrackId + " of mediapackage " + mediaPackageType.getId() + " using profile " + profileId);
    // Build a media package from the POSTed XML
    MediaPackage mediaPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().
        loadFromManifest(IOUtils.toInputStream(mediaPackageType.toXml()));
    
    // Encode the specified track
    Track track = composerService.encode(mediaPackage, sourceTrackId, profileId);
    
    // Return the JAXB version of the track
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    doc.appendChild(track.toManifest(doc, new DefaultMediaPackageSerializerImpl()));
    return TrackType.fromXml(doc);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.composer.impl.endpoint.ComposerWebService#listProfiles()
   */
  public EncodingProfileList listProfiles() {
    List<EncodingProfileImpl> list = new ArrayList<EncodingProfileImpl>();
    for(EncodingProfile p : composerService.listProfiles()) {
      list.add((EncodingProfileImpl) p);
    }
    return new EncodingProfileList(list);
  }
}
