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
package org.opencastproject.integrationtest;

import javax.ws.rs.core.MultivaluedMap;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * Composer REST resources
 * @author jamiehodge
 *
 */

public class ComposerResources {
  public static Client c = Client.create();
  public static WebResource r = c.resource(IntegrationTests.BASE_URL + "/composer/rest/");
  
  public static ClientResponse profiles() throws Exception {
    return r.path("profiles").get(ClientResponse.class);
  }
  
  public static ClientResponse encode(String mediapackage, 
      String audioSourceTrackId, String videoSourceTrackId, String profileId) throws Exception {
    MultivaluedMap<String, String> params = new MultivaluedMapImpl();
    params.add("mediapackage", mediapackage);
    params.add("audioSourceTrackId", audioSourceTrackId);
    params.add("videoSourceTrackId", videoSourceTrackId);
    params.add("profileId", profileId);
    return r.path("encode").post(ClientResponse.class, params);
  }
  
  public static ClientResponse receipt(String id) throws Exception {
    return r.path("receipt/" + id).get(ClientResponse.class);
  }
  
  public static ClientResponse image(String mediapackage, 
      String time, String sourceTrackId, String profileId) throws Exception {
    MultivaluedMap<String, String> params = new MultivaluedMapImpl();
    params.add("mediapackage", mediapackage);
    params.add("time", time);
    params.add("sourceTrackId", sourceTrackId);
    params.add("profileId", profileId);
    return r.path("image").post(ClientResponse.class, params);
  }
}
