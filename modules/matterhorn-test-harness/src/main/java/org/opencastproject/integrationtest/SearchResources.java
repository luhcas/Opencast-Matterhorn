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
 * Search REST resources
 * @author jamiehodge
 *
 */

public class SearchResources {
  public static Client c = Client.create();
  public static WebResource r = c.resource(IntegrationTests.BASE_URL + "/search/rest/");
  
  public static ClientResponse add(String mediapackage) throws Exception {
    MultivaluedMap<String, String> params = new MultivaluedMapImpl();
    params.add("mediapackage", mediapackage);
    return r.path("add").post(ClientResponse.class, params);
  }
  
  // TODO add remaining query parameters (episode and series)
  public static ClientResponse episode(String id) throws Exception {
    MultivaluedMap<String, String> params = new MultivaluedMapImpl();
    params.add("id", id);
    return r.path("episode").queryParams(params).get(ClientResponse.class);
  }
  
  public static ClientResponse episodeQuery(String q) throws Exception {
	MultivaluedMap<String, String> params = new MultivaluedMapImpl();
	params.add("q", q);
	return r.path("episode").queryParams(params).get(ClientResponse.class);
  }
  
  public static ClientResponse series(String id) throws Exception {
    MultivaluedMap<String, String> params = new MultivaluedMapImpl();
    params.add("id", id);
    return r.path("series").queryParams(params).get(ClientResponse.class);
  }
}
