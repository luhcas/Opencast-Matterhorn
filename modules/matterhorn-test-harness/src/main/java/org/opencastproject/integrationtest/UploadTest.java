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
import static org.junit.Assert.fail;

import org.opencastproject.util.Checksum;
import org.opencastproject.util.ChecksumType;

import com.sun.jersey.api.client.ClientResponse;

import org.junit.Test;
import org.w3c.dom.Document;

import javax.xml.xpath.XPathConstants;

/**
 * Integration test for file upload using thin client
 * @author jamiehodge
 */

public class UploadTest {
	static String trackUrl = IntegrationTests.BASE_URL + "/workflow/samples/camera.mpg";
	static String catalogUrl = IntegrationTests.BASE_URL + "/workflow/samples/dc-1.xml";
	static String attachmentUrl = IntegrationTests.BASE_URL + "/workflow/samples/index.txt";
	static String mediaPackage;
	static String[] catalogKeys = {"format", "promoted", "description", "subject", "publisher", "identifier", "title"}; 
	
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
		String workflowId = (String) Utils.xPath(xml, 
				"//ns3:workflow/@id", XPathConstants.STRING);
		String mediaPackageId = (String) Utils.xPath(xml, 
				"//mediapackage/@id", XPathConstants.STRING);
		
		// Confirm ingest
		int retries = 0;
		int timeout = 20; // FIXME: This value will be different, depending on CPU speed, workflow definitions, etc
		while (retries < timeout) {
			Thread.sleep(1000);
			
			// Check workflow instance status
			response = WorkflowResources.instance(workflowId, "xml");
			assertEquals("Response code (workflow instance):", 200, response.getStatus());
			String workflowInstance = response.getEntity(String.class);
			xml = Utils.parseXml(workflowInstance);
			if (Utils.xPath(xml, "//ns3:workflow/@state", XPathConstants.STRING).equals("RUNNING")) { break; }
			
			retries++;
		}
		
		if (retries == timeout) {
			fail("Workflow instance failed to start.");
		}
		
		// Compare Track
		String ingestedTrackUrl = (String) Utils.xPath(xml, "//media/track[@type='presenter/source']/url", XPathConstants.STRING);
		assertEquals("Media Track Checksum:",
				Checksum.create(ChecksumType.DEFAULT_TYPE, Utils.getUrlAsFile(ingestedTrackUrl)),
				Checksum.create(ChecksumType.DEFAULT_TYPE, Utils.getUrlAsFile(trackUrl)));
		
		// Compare Catalog
		String ingestedCatalogUrl = (String) Utils.xPath(xml, "//metadata/catalog[@type='metadata/dublincore']/url", XPathConstants.STRING);
		Document ingestedCatalog = Utils.getUrlAsDocument(ingestedCatalogUrl);
		Document catalog = Utils.getUrlAsDocument(catalogUrl);
		for (String key : catalogKeys) {
			assertEquals("Catalog " + key + ":", 
				((String) Utils.xPath(ingestedCatalog, "//dcterms:" + key, XPathConstants.STRING)).trim(),
				((String) Utils.xPath(catalog, "//dcterms:" + key, XPathConstants.STRING)).trim());
		}
		
		// Compare Attachment
		String ingestedAttachmentUrl = (String) Utils.xPath(xml, "//attachments/attachment[@type='attachment/txt']/url", XPathConstants.STRING);
		assertEquals("Attachment Checksum:",
				Checksum.create(ChecksumType.DEFAULT_TYPE, Utils.getUrlAsFile(ingestedAttachmentUrl)),
				Checksum.create(ChecksumType.DEFAULT_TYPE, Utils.getUrlAsFile(attachmentUrl)));
		
		// Confirm search indexing
		retries = 0;
		while (retries < timeout) {
			Thread.sleep(1000);
			
			// Check workflow instance status
			response = SearchResources.episode(mediaPackageId);
			assertEquals("Response code (episode):", 200, response.getStatus());
			xml = Utils.parseXml(response.getEntity(String.class));
			if (Utils.xPathExists(xml, "//ns2:result[@id='" + mediaPackageId + "']/ns2:mediapackage").equals(true)) { break; }
			
			retries++;
		}
		
		if (retries == timeout) {
			fail("Search Service failed to index file.");
		}
		
	}

}
