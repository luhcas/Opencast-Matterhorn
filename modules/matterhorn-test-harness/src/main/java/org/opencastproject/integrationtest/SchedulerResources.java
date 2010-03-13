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
 * Scheduler REST resources
 * @author jamiehodge
 *
 */

public class SchedulerResources {
	public static Client c = Client.create();
	public static WebResource r = c.resource(IntegrationTests.BASE_URL + "/scheduler/rest/");
	
	public static ClientResponse addEvent(String event) throws Exception {
		MultivaluedMap<String, String> params = new MultivaluedMapImpl();
		params.add("event", event);
		return r.path("addEvent").post(ClientResponse.class, params);
	}
	
	public static ClientResponse updateEvent(String event) throws Exception {
		MultivaluedMap<String, String> params = new MultivaluedMapImpl();
		params.add("event", event);
		return r.path("updateEvent").post(ClientResponse.class, params);
	}
	
	public static ClientResponse getEvent(String id) throws Exception {
		return r.path("getEvent/" + id).get(ClientResponse.class);
	}
	
	public static ClientResponse getDublinCoreMetadata(String id) throws Exception {
		return r.path("getDublinCoreMetadata/" + id).get(ClientResponse.class);
	}
	
	public static ClientResponse findConflictingEvents(String event) throws Exception {
		MultivaluedMap<String, String> params = new MultivaluedMapImpl();
		params.add("event", event);
		return r.path("findConflictingEvents").post(ClientResponse.class, params);
	}
	
	public static ClientResponse removeEvent(String id) throws Exception {
		return r.path("removeEvent/" + id).get(ClientResponse.class);
	}
	
	public static ClientResponse getEvents() throws Exception {
		return r.path("getEvents").get(ClientResponse.class);
	}

	public static ClientResponse getUpcomingEvents() throws Exception {
		return r.path("getUpcomingEvents").get(ClientResponse.class);
	}
	
	public static ClientResponse getCalendarForCaptureAgent(String id) throws Exception {
		return r.path("getCalendarForCaptureAgent/" + id).get(ClientResponse.class);
	}
	
	public static ClientResponse getCaptureAgentMetadata(String id) throws Exception {
		return r.path("getCaptureAgentMetadata/" + id).get(ClientResponse.class);
	}
}
