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

import org.opencastproject.media.mediapackage.jaxb.MediapackageType;
import org.opencastproject.media.mediapackage.jaxb.TrackType;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

/**
 * A web service endpoint delegating logic to the {@link MediaInspectionService}
 */
@WebService(name="ComposerService")
public interface ComposerWebService {
  /**
   * Encode a track from a media package using the specified encoding profile ID.
   * 
   * @param mediaPackage The media package that contains the track to encode
   * @param sourceTrackId The track to encode.  TODO: This may require a collection, since we may want to use multiple source tracks.
   * @param profileId The profile to use in the encoding.
   * @return The track describing the output of the encoding procedure.
   * @throws Exception
   */
  @WebMethod
  TrackType encode(
    @WebParam(name="mediapackage") MediapackageType mediaPackage,
    @WebParam(name="sourceTrackId") String sourceTrackId,
    @WebParam(name="targetTrackId") String targetTrackId,
    @WebParam(name="profileId") String profileId) throws Exception;

  /**
   * Lists the available encoding profiles
   * @return The list of all available EncodingProfiles
   */
  @WebMethod
  public EncodingProfileList listProfiles();
}
