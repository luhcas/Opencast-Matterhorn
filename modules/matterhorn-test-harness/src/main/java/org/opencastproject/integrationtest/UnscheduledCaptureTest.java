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

import com.sun.jersey.api.client.ClientResponse;

import org.junit.Test;
import org.w3c.dom.Document;

import javax.xml.xpath.XPathConstants;

/**
 * Integration test for unscheduled capture
 * @author jamiehodge
 *
 */

public class UnscheduledCaptureTest {
  public static String recordingId;

  @Test
  public void testUnscheduledCapture() throws Exception {
    
    // Agent Registered (Capture Admin Agents)
    ClientResponse response = CaptureAdminResources.agents();
    assertEquals("Response code (agents):", 200, response.getStatus());
    Document xml = Utils.parseXml(response.getEntity(String.class));
    assertTrue("Agent included? (agents):", Utils.xPathExists(xml, "//ns1:agent-state-update[name=\'" + IntegrationTests.AGENT + "\']" ));
    
    // Agent Registered (Capture Admin Agent)
    response = CaptureAdminResources.agent(IntegrationTests.AGENT);
    assertEquals("Response code (agent):", 200, response.getStatus());
    xml = Utils.parseXml(response.getEntity(String.class));
    assertTrue("Agent included? (agent):", Utils.xPathExists(xml, "//ns2:agent-state-update[name=\'" + IntegrationTests.AGENT + "\']"));
    
    // Agent idle (State)
    response = StateResources.getState();
    assertEquals("Response code (getState):", 200, response.getStatus());
    assertEquals("Agent idle? (getState):", "idle", response.getEntity(String.class));
    
    // Start capture (Capture)
    response = CaptureResources.startCaptureGet();
    assertEquals("Response code (startCapture):", 200, response.getStatus());
    
    // Get capture ID (Capture)
    recordingId = CaptureResources.captureId(response);
    
    // Agent capturing (State)
    response = StateResources.getState();
    assertEquals("Response code (getState):", 200, response.getStatus());
    assertEquals("Agent recording? (getState):", "capturing", response.getEntity(String.class));
    
    // Stop capture (Capture)
    response = CaptureResources.stopCapturePost(recordingId);
    assertEquals("Response code (stopCapturePost):", 200, response.getStatus());
    
    // Agent idle (State)
    response = StateResources.getState();
    assertEquals("Response code (getState):", 200, response.getStatus());
    assertEquals("Agent idle? (getState):", "idle", response.getEntity(String.class));

    Thread.sleep(15000);

    // Agent idle (Capture Admin)
    response = CaptureAdminResources.agent(IntegrationTests.AGENT);
    assertEquals("Response code (agent):", 200, response.getStatus());
    xml = Utils.parseXml(response.getEntity(String.class));
    assertEquals("Agent idle? (agent):", "idle", Utils.xPath(xml, "//ns2:agent-state-update/state", XPathConstants.STRING));
    
    // Recording is finished (State)
    response = StateResources.recordings();
    assertEquals("Response code (recordings):", 200, response.getStatus());
    xml = Utils.parseXml(response.getEntity(String.class));
    assertTrue("Recording included? (recordings):", Utils.xPathExists(xml, "//ns1:recording-state-update[name=\'" + recordingId + "\']"));
    assertEquals("Recording finished (recordings):", "upload_finished", Utils.xPath(xml, "//ns1:recording-state-update[name=\'" + recordingId + "\']/state", XPathConstants.STRING));
    
    // Pause for Capture Admin to sync
    Thread.sleep(10000);
    
    // Recording is finished (Capture Admin)
    response = CaptureAdminResources.recording(recordingId);
    assertEquals("Response code (recording):", 200, response.getStatus());
    xml = Utils.parseXml(response.getEntity(String.class));
    assertTrue("Recording included? (recordings):", Utils.xPathExists(xml, "//ns2:recording-state-update[name=\'" + recordingId + "\']"));
    assertEquals("Recording finished (recordings):", "upload_finished", Utils.xPath(xml, "//ns2:recording-state-update[name=\'" + recordingId + "\']/state", XPathConstants.STRING));

  }
}
