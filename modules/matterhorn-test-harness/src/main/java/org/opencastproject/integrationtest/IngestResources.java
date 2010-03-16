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

import java.io.InputStream;

import javax.ws.rs.core.MultivaluedMap;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * Ingest REST resources
 * @author jamiehodge
 *
 */

public class IngestResources {
	public static Client c = Client.create();
	public static WebResource r = c.resource(IntegrationTests.BASE_URL + "/ingest/rest/");
	
	public static ClientResponse createMediaPackage() throws Exception {
		return r.path("createMediaPackage").get(ClientResponse.class);
	}
	
	/**
	 * 
	 * @param type Type of media to add: Track, Catalog, Attachment
	 * 
	 */
	public static ClientResponse add(String type, String url,
			String flavor, String mediaPackage) throws Exception {
		MultivaluedMap<String, String> params = new MultivaluedMapImpl();
		params.add("url", url);
		params.add("flavor", flavor);
		params.add("mediaPackage", mediaPackage);
		return r.path("add" + type).post(ClientResponse.class, params);
	}
	
	/**
	 * 
	 * @param type Type of media to add: Track, Catalog, Attachment
	 *
	 */
	public static ClientResponse addTrack(String type, InputStream media,
			String flavor, String mediaPackage) throws Exception {
		MultivaluedMap<String, String> params = new MultivaluedMapImpl();
		params.add("flavor", flavor);
		params.add("mediaPackage", mediaPackage);
		return r.path("add" + type).entity(media).post(ClientResponse.class, params);
	}
	
	// TODO addMediaPackage
	
	public static ClientResponse addZippedMediaPackage(InputStream mediaPackage) throws Exception {
		return r.path("addZippedMediaPackage").entity(mediaPackage).post(ClientResponse.class);
	}
	
	public static ClientResponse ingest(String mediaPackageId) throws Exception {
		MultivaluedMap<String, String> params = new MultivaluedMapImpl();
		params.add("mediaPackage", mediaPackageId);
		return r.path("ingest").post(ClientResponse.class, params);
	}

}
