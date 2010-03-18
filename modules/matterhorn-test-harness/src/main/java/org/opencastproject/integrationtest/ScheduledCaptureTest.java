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

import org.junit.Test;
import org.w3c.dom.Document;

import com.sun.jersey.api.client.ClientResponse;

/**
 * Integration test for scheduled capture
 * @author jamiehodge
 *
 */

public class ScheduledCaptureTest {

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
	public void testScheduleEvent() throws Exception {
		
		String event = Utils.schedulerEvent(60000);
		
		ClientResponse response = SchedulerResources.addEvent(event);
		
		assertEquals("Response code (addEvent):", 200, response.getStatus());
	}
}
