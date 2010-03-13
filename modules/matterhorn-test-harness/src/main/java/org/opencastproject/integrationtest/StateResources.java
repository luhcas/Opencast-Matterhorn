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
package org.opencastproject.integrationtest;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

/**
 * Run all integration tests
 * @author jamiehodge
 *
 */

public class StateResources {
	public static Client c = Client.create();
	public static WebResource r = c.resource(IntegrationTests.BASE_URL + "/state/rest/");
	
	public static ClientResponse getState() throws UniformInterfaceException {
		return r.path("GetState").get(ClientResponse.class);
	}
	
	public static ClientResponse recordings() throws UniformInterfaceException {
		return r.path("GetRecordings").get(ClientResponse.class);
	}
}
