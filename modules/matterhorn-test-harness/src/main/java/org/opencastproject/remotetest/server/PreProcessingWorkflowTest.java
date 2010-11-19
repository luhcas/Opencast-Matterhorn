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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.opencastproject.remotetest.Main.BASE_URL;

import org.opencastproject.remotetest.Main;
import org.opencastproject.remotetest.util.CaptureUtils;
import org.opencastproject.remotetest.util.TrustedHttpClient;
import org.opencastproject.remotetest.util.Utils;
import org.opencastproject.remotetest.util.WorkflowUtils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.xml.xpath.XPathConstants;

/**
 * This test case simulates adding a recording event, waiting for the capture agent to pick it up, do the recording and
 * then ingest it. At the same time, it monitors the associated workflow and makes sure the operation and state
 * transitions happen as expected.
 */
public class PreProcessingWorkflowTest {

  private static final Logger logger = LoggerFactory.getLogger(PreProcessingWorkflowTest.class);
  
  private TrustedHttpClient client;

  /** The workflow to append after preprocessing */
  private static final String WORKFLOW_DEFINITION_PATH = "/workflow-postprocessing.xml";

  /** The postprocessing workflow definition identifier */
  private static final String WORKFLOW_DEFINITION_ID = "postprocess_integrationtest";

  /** Id of the demo capture agent */
  private static final String CAPTURE_AGENT_ID = "demo_capture_agent";

  @BeforeClass
  public static void setupClass() throws Exception {
    logger.info("Running " + PreProcessingWorkflowTest.class.getName());
  }
  
  @Before
  public void setup() throws Exception {
    client = Main.getClient();
    WorkflowUtils.registerWorkflowDefinition(getSampleWorkflowDefinition());
  }

  @After
  public void teardown() throws Exception {
    Main.returnClient(client);
    WorkflowUtils.unregisterWorkflowDefinition(WORKFLOW_DEFINITION_ID);
  }

  @Test
  public void test() throws Exception {
    String workflowId = null;
    long waiting = 0;
    long TIMEOUT = 5000L;
    long GRACE_PERIOD = 30000L;

    // Make sure the demo capture agent is online
    if (!CaptureUtils.isOnline(CAPTURE_AGENT_ID))
      fail("Demo capture agent with id " + CAPTURE_AGENT_ID + " is not online");

    // Specify start end end time for capture
    Calendar c = Calendar.getInstance();
    c.roll(Calendar.MINUTE, 1);
    Date start = c.getTime();
    c.roll(Calendar.MINUTE, 1);
    Date end = c.getTime();

    // TODO: How do we submit the workflow definition?

    // Schedule and event and make sure the workflow is in "schedule" operation
    workflowId = scheduleEvent(start, end);
    Thread.sleep(1000);
    if (!WorkflowUtils.isWorkflowInOperation(workflowId, "schedule")
            || !WorkflowUtils.isWorkflowInState(workflowId, "PAUSED")) {
      fail("Workflow " + workflowId + " should be on hold in 'schedule'");
    }

    // Wait for the capture agent to start the recording and make sure the workflow enters the "capture" operation
    waiting = 60 * 1000L + GRACE_PERIOD; // 1 min +
    boolean agentIsCapturing = false;
    boolean inCaptureOperation = false;
    while (waiting > 0) {
      if (CaptureUtils.recordingExists(workflowId)) {
        agentIsCapturing |= CaptureUtils.isInState(workflowId, "capturing");
        inCaptureOperation |= WorkflowUtils.isWorkflowInOperation(workflowId, "capture");
        if (agentIsCapturing && inCaptureOperation)
          break;
      }
      waiting -= TIMEOUT;
      Thread.sleep(TIMEOUT);
    }

    // Are we already past the grace period?
    if (waiting <= 0) {
      if (!agentIsCapturing)
        fail("Agent '" + CAPTURE_AGENT_ID + "' did not start recording '" + workflowId + "'");
      else if (!inCaptureOperation)
        fail("Workflow '" + workflowId + "' never entered the 'capture' hold state");
    }

    // Wait for capture agent to stop capturing
    Thread.sleep(Math.max(end.getTime() - System.currentTimeMillis(), 0));

    // Make sure workflow advanced to "ingest" operation
    waiting = 60 * 1000L + GRACE_PERIOD; // 1 min +
    boolean agentIsIngesting = false;
    boolean inIngestOperation = false;
    while (waiting > 0) {
      agentIsIngesting |= CaptureUtils.isInState(workflowId, "uploading");
      inIngestOperation |= WorkflowUtils.isWorkflowInOperation(workflowId, "capture");
      if (agentIsIngesting && inIngestOperation)
        break;
      waiting -= TIMEOUT;
      Thread.sleep(TIMEOUT);
    }

    // Are we already past the grace period?
    if (waiting <= 0) {
      if (!agentIsIngesting)
        fail("Agent '" + CAPTURE_AGENT_ID + "' did not start ingesting '" + workflowId + "'");
      else if (!inIngestOperation)
        fail("Workflow '" + workflowId + "' never entered the 'ingest' hold state");
    }

    // Wait for ingest and make sure workflow executes "cleanup", then finishes successfully
    waiting = 60 * 1000L + GRACE_PERIOD; // 1 min +
    while (waiting > 0) {
      if (WorkflowUtils.isWorkflowInState(workflowId, "SUCCEEDED"))
        break;
      waiting -= TIMEOUT;
      Thread.sleep(TIMEOUT);
    }

    // Are we already past the grace period?
    if (waiting <= 0) {
      fail("Workflow '" + workflowId + "' did not finish");
    }

  }


  /**
   * Adds a new recording event to the scheduling service and returns the event id.
   * 
   * @param start
   *          start date
   * @param end
   *          end date
   * @return the event identifier
   */
  private String scheduleEvent(Date start, Date end) throws Exception {
    HttpPut request = new HttpPut(BASE_URL + "/scheduler/rest");

    // Create the request body
    Calendar c = Calendar.getInstance();
    c.roll(Calendar.MINUTE, 1);
    long startTime = c.getTimeInMillis();
    c.roll(Calendar.MINUTE, 1);
    long endTime = c.getTimeInMillis();
    String eventXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><event>"
            + "<contributor>demo contributor</contributor><creator>demo creator</creator>"
            + "<startDate>{start}</startDate><endDate>{stop}</endDate><device>{device}</device>"
            + "<language>en</language><license>creative commons</license><resources>vga, audio</resources>"
            + "<title>demo title</title><additionalMetadata><metadata id=\"0\"><key>location</key>"
            + "<value>demo location</value></metadata><metadata id=\"0\"><key>org.opencastproject.workflow.definition</key>"
            + "<value>" + WORKFLOW_DEFINITION_ID + "</value></metadata></additionalMetadata></event>";
    eventXml = eventXml.replace("{device}", CAPTURE_AGENT_ID).replace("{start}", Long.toString(startTime))
            .replace("{stop}", Long.toString(endTime));

    // Prepare the request
    List<NameValuePair> formParams = new ArrayList<NameValuePair>();
    formParams.add(new BasicNameValuePair("event", eventXml));
    request.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));

    // Submit and check the response
    HttpResponse response = client.execute(request);
    assertEquals(HttpStatus.SC_CREATED, response.getStatusLine().getStatusCode());
    String responseBody = StringUtils.trimToNull(EntityUtils.toString(response.getEntity()));
    assertNotNull(responseBody);
    String eventId = StringUtils.trimToNull((String) Utils.xpath(responseBody, "/event/@id", XPathConstants.STRING));
    assertNotNull("No event id found", eventId);
    return eventId;
  }

  private String getSampleWorkflowDefinition() throws Exception {
    InputStream in = null;
    try {
      in = PreProcessingWorkflowTest.class.getResourceAsStream(WORKFLOW_DEFINITION_PATH);
      return IOUtils.toString(in, "UTF-8");
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

}
