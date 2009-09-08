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
import org.opencastproject.media.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.media.mediapackage.jaxb.TrackType;

import org.w3c.dom.Document;

import java.net.URL;

import javax.xml.parsers.DocumentBuilderFactory;

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
  public TrackType inspect(URL url) {
    Track track = service.inspect(url);
    Document doc;
    try {
      doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
      track.toManifest(doc, new DefaultMediaPackageSerializerImpl());
      return TrackType.fromXml(doc);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
