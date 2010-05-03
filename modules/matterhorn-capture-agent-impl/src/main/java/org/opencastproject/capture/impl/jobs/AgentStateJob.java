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
package org.opencastproject.capture.impl.jobs;

import org.opencastproject.capture.api.AgentRecording;
import org.opencastproject.capture.api.CaptureParameters;
import org.opencastproject.capture.api.StateService;
import org.opencastproject.capture.impl.ConfigurationManager;
import org.opencastproject.security.api.TrustedHttpClient;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This class is responsible for pushing the agent's state to the remote state service.
 */
public class AgentStateJob implements Job {

  private static final Logger logger = LoggerFactory.getLogger(AgentStateJob.class);

  private ConfigurationManager config = null;
  private StateService state = null;
  private TrustedHttpClient client = null;

  /**
   * Pushes the agent's state to the remote state service.
   * {@inheritDoc}
   * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
   */
  public void execute(JobExecutionContext ctx) throws JobExecutionException {
    config = (ConfigurationManager) ctx.getMergedJobDataMap().get(JobParameters.CONFIG_SERVICE);
    state = (StateService) ctx.getMergedJobDataMap().get(JobParameters.STATE_SERVICE);
    client = (TrustedHttpClient) ctx.getMergedJobDataMap().get(JobParameters.TRUSTED_CLIENT);
    sendAgentState();
    sendRecordingState();
  }

  /**
   * Sends an agent state update to the capture-admin state service.
   */
  private void sendAgentState() {

    logger.debug("Sending agent {}'s state", state.getAgentName());
    
    //Figure out where we're sending the data
    String url = config.getItem(CaptureParameters.AGENT_STATE_REMOTE_ENDPOINT_URL);
    if (url == null) {
      logger.warn("URL for {} is invalid, unable to push state to remote server.", CaptureParameters.AGENT_STATE_REMOTE_ENDPOINT_URL);
      return;
    }
    try {
      if (url.charAt(url.length()-1) == '/') {
        url += config.getItem(CaptureParameters.AGENT_NAME);
      } else {
        url += "/" + config.getItem(CaptureParameters.AGENT_NAME);
      }
    } catch (StringIndexOutOfBoundsException e) {
      logger.warn("Unable to build valid state endpoint for agents.");
      return;
    }

    List<NameValuePair> formParams = new ArrayList<NameValuePair>();

    //formParams.add(new BasicNameValuePair("agentName", a.getName()));
    formParams.add(new BasicNameValuePair("state", state.getAgentState()));

    send(formParams, url);
  }

  /**
   * Sends an update for each of the recordings currently being tracked in the system.
   */
  private void sendRecordingState() {

    //Figure out where we're sending the data
    String url = config.getItem(CaptureParameters.RECORDING_STATE_REMOTE_ENDPOINT_URL);
    if (url == null) {
      logger.warn("URL for {} is invalid, unable to push recording state to remote server.", CaptureParameters.RECORDING_STATE_REMOTE_ENDPOINT_URL);
      return;
    }
    try {
      if (url.charAt(url.length() - 1) != '/') {
        url += "/";
      }
    } catch (StringIndexOutOfBoundsException e) {
      logger.warn("Unable to build valid state endpoint for recordings.");
      return;
    }

    //For each recording being tracked by the system send an update
    Map<String, AgentRecording> recordings = state.getKnownRecordings();
    for (Entry<String, AgentRecording> e : recordings.entrySet()) {
      List<NameValuePair> formParams = new ArrayList<NameValuePair>();

      formParams.add(new BasicNameValuePair("id", e.getKey()));
      formParams.add(new BasicNameValuePair("state", e.getValue().getState()));

      String myURL = url + e.getKey();
      send(formParams, myURL);
    }
  }

  /**
   * Utility method to POST data to a URL.  This method encodes the data in UTF-8 as post data, rather than multipart MIME.
   * @param formParams The data to send.
   * @param url The URL to send the data to.
   */
  private void send(List<NameValuePair> formParams, String url) {
    HttpPost remoteServer = new HttpPost(url);

    try {
      remoteServer.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));
      client.execute(remoteServer);
    } catch (Exception e) {
      logger.error("Unable to push update to remote server: {}.", e.getMessage());
    }
  }
}
