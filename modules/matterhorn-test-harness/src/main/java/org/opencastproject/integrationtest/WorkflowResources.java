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
 * Workflow REST resources
 * @author jamiehodge
 *
 */

public class WorkflowResources {
	public static Client c = Client.create();
	public static WebResource r = c.resource(IntegrationTests.BASE_URL + "/workflow/rest/");

	/**
	 * 
	 * @param format Response format: xml or json
	 * 
	 */
	public static ClientResponse definitions(String format) throws Exception {
		return r.path("definitions." + format.toLowerCase()).get(ClientResponse.class);
	}
	
	/**
	 * 
	 * @param format Response format: xml or json
	 * 
	 */
	public static ClientResponse instances(String format) throws Exception {
		return r.path("instances," + format.toLowerCase()).get(ClientResponse.class);
	}
	
	public static ClientResponse start(String mediapackage, 
			String workflowDefinition, String properties) throws Exception {
		MultivaluedMap<String, String> params = new MultivaluedMapImpl();
		params.add("mediapackage", mediapackage);
		params.add("definition", workflowDefinition);
		params.add("properties", properties);
		return r.path("start").post(ClientResponse.class, params);
	}
	
	public static ClientResponse suspend(String id) throws Exception {
		return r.path("suspend/" + id).get(ClientResponse.class);
	}
	
	public static ClientResponse resume(String id) throws Exception {
		return r.path("resume/" + id).get(ClientResponse.class);
	}
	
	public static ClientResponse stop(String id) throws Exception {
		return r.path("stop/" + id).get(ClientResponse.class);
	}
}
