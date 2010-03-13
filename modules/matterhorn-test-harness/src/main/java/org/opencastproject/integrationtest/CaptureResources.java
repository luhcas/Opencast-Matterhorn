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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.io.IOUtils;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * Capture REST resources
 * @author jamiehodge
 *
 */

public class CaptureResources {
	public static Client c = Client.create();
	public static WebResource r = c.resource(IntegrationTests.BASE_URL + "/capture/rest/");
	
	public static ClientResponse startCaptureGet() throws UniformInterfaceException {
		return r.path("startCapture").get(ClientResponse.class);
	}
	
	public static ClientResponse stopCapture() throws UniformInterfaceException {
		return r.path("stopCapture").get(ClientResponse.class);
	}
	
	public static ClientResponse startCapturePost() throws UniformInterfaceException, Exception {
		MultivaluedMap<String, String> params = new MultivaluedMapImpl();
		params.add("config", captureProperties());
		return r.path("startCapture").post(ClientResponse.class, params);
	}
	
	public static ClientResponse stopCapturePost(String id) throws UniformInterfaceException {
		MultivaluedMap<String, String> params = new MultivaluedMapImpl();
		params.add("recordingID", id);
		return r.path("stopCapture").post(ClientResponse.class, params);
	}
	
	protected static String captureProperties() throws Exception {
		return IOUtils.toString(CaptureResources.class.getClassLoader().getResourceAsStream("capture.properties"));
	}
	
	protected static String captureId(ClientResponse response) throws Exception {
		String pattern = "Unscheduled-\\d+";
		Matcher matcher = Pattern.compile(pattern).matcher(response.getEntity(String.class));
		matcher.find();
		return matcher.group();
	}
}
