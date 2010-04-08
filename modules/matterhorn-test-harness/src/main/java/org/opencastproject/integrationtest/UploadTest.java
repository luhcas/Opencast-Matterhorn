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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.opencastproject.util.Checksum;
import org.opencastproject.util.ChecksumType;

import javax.xml.xpath.XPathConstants;

import org.junit.Test;
import org.w3c.dom.Document;

import com.sun.jersey.api.client.ClientResponse;

/**
 * Integration test for file upload using thin client
 * @author jamiehodge
 *
 */

public class UploadTest {
	static String trackUrl = IntegrationTests.BASE_URL + "/workflow/samples/camera.mpg";
	static String catalogUrl = IntegrationTests.BASE_URL + "/workflow/samples/dc-1.xml";
	static String attachmentUrl = IntegrationTests.BASE_URL + "/workflow/samples/index.txt";
	static String mediaPackage;
	
	@Test
	public void testIngestThinClient() throws Exception {
		
		// Create Media Package
		ClientResponse response = IngestResources.createMediaPackage();
		assertEquals("Response code (createMediaPacakge):", 200, response.getStatus());
		mediaPackage = response.getEntity(String.class);
		// TODO validate Media Package
		
		// Add Track
		response = IngestResources.add("Track", trackUrl, "presenter/source", mediaPackage);
		assertEquals("Response code (addTrack):", 200, response.getStatus());
		mediaPackage = response.getEntity(String.class);
		// TODO validate Media Package
		
		// Add Catalog
		response = IngestResources.add("Catalog", catalogUrl, "metadata/dublincore", mediaPackage);
		assertEquals("Response code (addCatalog):", 200, response.getStatus());
		mediaPackage = response.getEntity(String.class);
		// TODO validate Media Package
		
		// Add Attachment
		response = IngestResources.add("Attachment", attachmentUrl, "attachment/txt", mediaPackage);
		assertEquals("Response code (addAttachment):", 200, response.getStatus());
		mediaPackage = response.getEntity(String.class);
		// TODO validate Media Package
		
		// Ingest
		response = IngestResources.ingest(mediaPackage);
		assertEquals("Response code (ingest):", 200, response.getStatus());
		mediaPackage = response.getEntity(String.class);
		Document xml = Utils.parseXml(mediaPackage);
		
		// Pause for ingest to complete
		Thread.sleep(2000);
		
		// Confirm Ingest
		String mediaPackageId = (String) Utils.xPath(xml, 
				"//mediapackage/@id", XPathConstants.STRING);
		String trackId = (String) Utils.xPath(xml,
				"//media/track/@id", XPathConstants.STRING);
		String catalogId = (String) Utils.xPath(xml, 
				"//metadata/catalog/@id", XPathConstants.STRING);
		String attachmentId = (String) Utils.xPath(xml,
				"//attachments/attachment/@id", XPathConstants.STRING);
		
		// Retrieve and compare mediapackage elements from files repository
		response = FilesResources.getFile(mediaPackageId, trackId);
		assertEquals("Response code (getFile Track):", 200, response.getStatus());
		assertEquals("Media Track Checksum:",
				Checksum.create(ChecksumType.DEFAULT_TYPE, response.getEntity(File.class)),
				Checksum.create(ChecksumType.DEFAULT_TYPE, Utils.getUrlAsFile(trackUrl)));
		response = FilesResources.getFile(mediaPackageId, catalogId);
		assertEquals("Response code (getFile Catalog):", 200, response.getStatus());
		// TODO compare files
		response = FilesResources.getFile(mediaPackageId, attachmentId);
		assertEquals("Response code (getFile Attachment):", 200, response.getStatus());
		// TODO compare files
		
		// Pause for indexing to complete
		Thread.sleep(2000);
		
		// Confirm search indexing 
		response = SearchResources.episode(mediaPackageId);
		assertEquals("Response code (episode):", 200, response.getStatus());
		assertTrue("Episode Registered:", 
				Utils.xPathExists(
						Utils.parseXml(response.getEntity(String.class)),
						"//ns2:result[@id='" + mediaPackageId + "']/ns2:mediapackage" ));
		
	}

}
