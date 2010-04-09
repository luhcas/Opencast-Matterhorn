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

import java.util.UUID;

import javax.xml.xpath.XPathConstants;

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
	public void testScheduledCapture() throws Exception {
		
		// Agent registered (Capture Admin Agents)
		ClientResponse response = CaptureAdminResources.agents();
		assertEquals("Response code (agents):", 200, response.getStatus());
		Document xml = Utils.parseXml(response.getEntity(String.class));
		assertTrue("Agent included? (agents):", Utils.xPathExists(xml, "//ns1:agent-state-update[name=\'" + IntegrationTests.AGENT + "\']" ));
		
		// Agent registered (Capture Admin Agent)
		response = CaptureAdminResources.agent(IntegrationTests.AGENT);
		assertEquals("Response code (agent):", 200, response.getStatus());
		xml = Utils.parseXml(response.getEntity(String.class));
		assertTrue("Agent included? (agent):", Utils.xPathExists(xml, "//ns2:agent-state-update[name=\'" + IntegrationTests.AGENT + "\']"));
		
		// Agent idle (State)
		response = StateResources.getState();
		assertEquals("Response code (getState):", 200, response.getStatus());
		assertEquals("Agent idle? (getState):", "idle", response.getEntity(String.class));
		
		// Agent idle (Capture Admin Agent)
		response = CaptureAdminResources.agent(IntegrationTests.AGENT);
		assertEquals("Response code (agent):", 200, response.getStatus());
		assertEquals("Agent idle? (agent):", "idle", Utils.xPath(xml, "//ns2:agent-state-update[name=\'" + IntegrationTests.AGENT + "\']/state", XPathConstants.STRING));

		// Generate unique title and create event XML
		String title = UUID.randomUUID().toString();
		String id = UUID.randomUUID().toString();
		String event = Utils.schedulerEvent(10000, title, id);
		
		// Add event (Scheduler)
		response = SchedulerResources.addEvent(event);
		assertEquals("Response code (addEvent):", 200, response.getStatus());
		
		// Event included? (Scheduler: events)
		response = SchedulerResources.getEvents();
		assertEquals("Response code (getEvents):", 200, response.getStatus());
		xml = Utils.parseXml(response.getEntity(String.class));
		assertTrue("Event included? (getEvents):", Utils.xPathExists(xml, "//ns1:SchedulerEvent[id=\'" + id + "\']"));
		
		// Event included? (Scheduler: upcoming events)
		response = SchedulerResources.getUpcomingEvents();
		assertEquals("Response code (getUpcomingEvents):", 200, response.getStatus());
		xml = Utils.parseXml(response.getEntity(String.class));
		assertTrue("Event included? (getUpcomingEvents):", Utils.xPathExists(xml, "//ns1:SchedulerEvent[id=\'" + id + "\']"));
		
		// Compare event (Scheduler: event)
		response = SchedulerResources.getEvent(id);
		assertEquals("Response code (getEvent):", 200, response.getStatus());
		xml = Utils.parseXml(response.getEntity(String.class));
		assertEquals("Event id (getEvent):", title, Utils.xPath(xml, "//item[@key='title']/value", XPathConstants.STRING));
		assertEquals("Event title (getEvent):", id, Utils.xPath(xml, "//id", XPathConstants.STRING));
		
		// Compare event DC metadata (Scheduler)
		response = SchedulerResources.getDublinCoreMetadata(id);
		assertEquals("Response code (getDublinCoreMetadata):", 200, response.getStatus());
		xml = Utils.parseXml(response.getEntity(String.class));
		assertEquals("Event id (getDublinCoreMetadata):", title, Utils.xPath(xml, "//dcterms:title", XPathConstants.STRING));
		assertEquals("Event title (getDublinCoreMetadata):", id, Utils.xPath(xml, "//dcterms:identifier", XPathConstants.STRING));
		
		// Pause for recording to start
		Thread.sleep(60000);
		
		// Agent capturing (Capture Admin Agents)
		// response = CaptureAdminResources.agents();
		// assertEquals("Response code (agents):", 200, response.getStatus());
		// xml = Utils.parseXml(response.getEntity(String.class));
		// assertEquals("Agent capturing? (agent):", "capturing", Utils.xPath(xml, "//ns1:agent-state-update[name=\'" + IntegrationTests.AGENT + "\']/state", XPathConstants.STRING));
		
		// Agent capturing (State)
		response = StateResources.getState();
		assertEquals("Response code (getState):", 200, response.getStatus());
		assertEquals("Agent capturing? (getState):", "capturing", response.getEntity(String.class));
		
		// Pause for recording to complete
		Thread.sleep(10000);
		
		// Agent idle (State)
		response = StateResources.getState();
		assertEquals("Response code (getState):", 200, response.getStatus());
		assertEquals("Agent idle? (getState):", "idle", response.getEntity(String.class));
		
		// Agent idle (Capture Admin Agents)
		// response = CaptureAdminResources.agents();
		// assertEquals("Response code (agents):", 200, response.getStatus());
		// xml = Utils.parseXml(response.getEntity(String.class));
		// assertEquals("Agent capturing? (agent):", "capturing", Utils.xPath(xml, "//ns1:agent-state-update[name=\'" + IntegrationTests.AGENT + "\']/state", XPathConstants.STRING));
		
		// Pause for workflow to complete
		Thread.sleep(15000);
		
		// Recording indexed (Search)
		response = SearchResources.all(title);
		assertEquals("Response code (search all):", 200, response.getStatus());
		xml = Utils.parseXml(response.getEntity(String.class));
	    assertTrue("Recording included? (search all):", Utils.xPathExists(xml, "//ns2:mediapackage[title=\'" + title + "\']"));
	}
}
