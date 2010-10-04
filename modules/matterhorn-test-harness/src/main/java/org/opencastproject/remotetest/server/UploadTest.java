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
package org.opencastproject.remotetest.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.opencastproject.remotetest.Main;
import org.opencastproject.remotetest.server.resource.IngestResources;
import org.opencastproject.remotetest.server.resource.SearchResources;
import org.opencastproject.remotetest.util.Utils;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.ChecksumType;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

import javax.xml.xpath.XPathConstants;

/**
 * Integration test for file upload using thin client
 * 
 * @author jamiehodge
 */
@Ignore // Until we can make the Jersey client work with digest auth, this must remain ignored
public class UploadTest {
  String trackUrl;
  String catalogUrl;
  String attachmentUrl;
  String mediaPackage;
  String[] catalogKeys;

  public void setUp() throws Exception {
    trackUrl = Main.getBaseUrl() + "/workflow/samples/camera.mpg";
    catalogUrl = Main.getBaseUrl() + "/workflow/samples/dc-1.xml";
    attachmentUrl = Main.getBaseUrl() + "/workflow/samples/index.txt";
    catalogKeys = new String[] { "format", "promoted", "description", "subject", "publisher", "identifier", "title" };
  }

  @Test
  public void testIngestThinClient() throws Exception {

    // Create Media Package
    HttpResponse response = IngestResources.createMediaPackage();
    assertEquals("Response code (createMediaPacakge):", 200, response.getStatusLine().getStatusCode());
    mediaPackage = EntityUtils.toString(response.getEntity(), "UTF-8");
    // TODO validate Media Package

    // Add Track
    response = IngestResources.add("Track", trackUrl, "presenter/source", mediaPackage);
    assertEquals("Response code (addTrack):", 200, response.getStatusLine().getStatusCode());
    mediaPackage = EntityUtils.toString(response.getEntity(), "UTF-8");
    // TODO validate Media Package

    // Add Catalog
    response = IngestResources.add("Catalog", catalogUrl, "dublincore/episode", mediaPackage);
    assertEquals("Response code (addCatalog):", 200, response.getStatusLine().getStatusCode());
    mediaPackage = EntityUtils.toString(response.getEntity(), "UTF-8");
    // TODO validate Media Package

    // Add Attachment
    response = IngestResources.add("Attachment", attachmentUrl, "attachment/txt", mediaPackage);
    assertEquals("Response code (addAttachment):", 200, response.getStatusLine().getStatusCode());
    mediaPackage = EntityUtils.toString(response.getEntity(), "UTF-8");
    // TODO validate Media Package

    // Ingest
    response = IngestResources.ingest(mediaPackage);
    assertEquals("Response code (ingest):", 200, response.getStatusLine().getStatusCode());
    mediaPackage = EntityUtils.toString(response.getEntity(), "UTF-8");
    Document xml = Utils.parseXml(IOUtils.toInputStream(mediaPackage, "UTF-8"));
    String workflowId = (String) Utils.xPath(xml, "//ns3:workflow/@id", XPathConstants.STRING);
    String mediaPackageId = (String) Utils.xPath(xml, "//mediapackage/@id", XPathConstants.STRING);

    // Confirm ingest
    response = IngestResources.getWorkflowInstance(workflowId);
    assertEquals("Response code (workflow instance):", 200, response.getStatusLine().getStatusCode());

    // Compare Track
    String ingestedTrackUrl = (String) Utils.xPath(xml, "//media/track[@type='presenter/source']/url",
            XPathConstants.STRING);
    assertEquals("Media Track Checksum:", Checksum.create(ChecksumType.DEFAULT_TYPE, Utils
            .getUrlAsFile(ingestedTrackUrl)), Checksum.create(ChecksumType.DEFAULT_TYPE, Utils.getUrlAsFile(trackUrl)));

    // Compare Catalog
    String ingestedCatalogUrl = (String) Utils.xPath(xml, "//metadata/catalog[@type='dublincore/episode']/url",
            XPathConstants.STRING);
    Document ingestedCatalog = Utils.getUrlAsDocument(ingestedCatalogUrl);
    Document catalog = Utils.getUrlAsDocument(catalogUrl);
    for (String key : catalogKeys) {
      assertEquals("Catalog " + key + ":", ((String) Utils.xPath(ingestedCatalog, "//dcterms:" + key,
              XPathConstants.STRING)).trim(),
              ((String) Utils.xPath(catalog, "//dcterms:" + key, XPathConstants.STRING)).trim());
    }

    // Compare Attachment
    String ingestedAttachmentUrl = (String) Utils.xPath(xml, "//attachments/attachment[@type='attachment/txt']/url",
            XPathConstants.STRING);
    assertEquals("Attachment Checksum:", Checksum.create(ChecksumType.DEFAULT_TYPE, Utils
            .getUrlAsFile(ingestedAttachmentUrl)), Checksum.create(ChecksumType.DEFAULT_TYPE, Utils
            .getUrlAsFile(attachmentUrl)));

    // Confirm search indexing
    int retries = 0;
    int timeout = 20;
    while (retries < timeout) {
      Thread.sleep(1000);

      // Check workflow instance status
      response = SearchResources.episode(mediaPackageId);
      assertEquals("Response code (episode):", 200, response.getStatusLine().getStatusCode());
      xml = Utils.parseXml(response.getEntity().getContent());
      if (Utils.xPathExists(xml, "//ns2:result[@id='" + mediaPackageId + "']/ns2:mediapackage").equals(true)) {
        break;
      }

      retries++;
    }

    if (retries == timeout) {
      fail("Search Service failed to index file.");
    }

  }

}
