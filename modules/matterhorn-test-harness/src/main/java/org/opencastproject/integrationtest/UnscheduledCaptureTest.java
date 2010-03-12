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

import static org.junit.Assert.*;
import org.junit.Test;
import org.w3c.dom.Document;

import com.sun.jersey.api.client.ClientResponse;

/**
 * Integration test for unscheduled capture
 * @author jamiehodge
 *
 */

public class UnscheduledCaptureTest {
	public static String recordingId;

	@Test
	public void testAgentRegisteredAndIdle() throws Exception {
		
		// Agent Registered
		ClientResponse response = CaptureAdminResources.agents();
		
		assertEquals("Response code (agents):", 200, response.getStatus());
		
		Document xml = Utils.parseXml(response.getEntity(String.class));
		
		assertTrue("Agent included? (agents):", Utils.xPathExists(xml, "//ns1:agent-state-update[name=\'" + IntegrationTests.AGENT + "\']" ));
		
		response = CaptureAdminResources.agent(IntegrationTests.AGENT);
		
		assertEquals("Response code (agent):", 200, response.getStatus());
		
		xml = Utils.parseXml(response.getEntity(String.class));
		
		assertTrue("Agent included? (agent):", Utils.xPathExists(xml, "//ns2:agent-state-update[name=\'" + IntegrationTests.AGENT + "\']"));
		
		// Agent State: idle
		response = StateResources.getState();
		
		assertEquals("Response code (getState):", 200, response.getStatus());
		assertEquals("Agent idle? (getState):", "idle", response.getEntity(String.class));
	}
	
	@Test
	public void testStartCapture() throws Exception {
		
		// Start capture
		ClientResponse response = CaptureResources.startCaptureGet();
		
		assertEquals("Response code (startCapture):", 200, response.getStatus());
		
		// Get capture ID
		recordingId = CaptureResources.captureId(response);
	}
	
	@Test
	public void testAgentCapturing() throws Exception {
		
		// Agent State: capturing
		ClientResponse response = StateResources.getState();
		
		assertEquals("Response code (getState):", 200, response.getStatus());
		assertEquals("Agent recording? (getState):", "capturing", response.getEntity(String.class));
	}
	
	@Test
	public void testStopCapture() throws Exception {
		
		// Stop capture
		ClientResponse response = CaptureResources.stopCapturePost(recordingId);
		
		assertEquals("Response code (stopCapturePost):", 200, response.getStatus());
	}
	
	@Test
	public void testAgentIdleAfterStopped() throws Exception {
		
		// Agent State: idle
		ClientResponse response = StateResources.getState();
		
		assertEquals("Response code (getState):", 200, response.getStatus());
		assertEquals("Agent idle? (getState):", "idle", response.getEntity(String.class));
	}
	
	
}
