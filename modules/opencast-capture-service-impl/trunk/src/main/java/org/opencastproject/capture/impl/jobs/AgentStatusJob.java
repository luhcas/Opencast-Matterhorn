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
package org.opencastproject.capture.impl.jobs;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.opencastproject.capture.api.StatusService;
import org.opencastproject.capture.impl.CaptureParameters;
import org.opencastproject.capture.impl.ConfigurationManager;
import org.opencastproject.capture.impl.StateSingleton;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for pushing the agent's status to the remote status service
 */
public class AgentStatusJob implements Job {

  private static final Logger logger = LoggerFactory.getLogger(AgentStatusJob.class);

  private ConfigurationManager config = ConfigurationManager.getInstance();
  private StatusService status = StateSingleton.getInstance();

  /**
   * Pushes the agent's status to the remote status service
   * {@inheritDoc}
   * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
   */
  public void execute(JobExecutionContext ctx) throws JobExecutionException {
    sendAgentState();
    sendRecordingState();
  }

  /**
   * Sends an agent state update to the capture-admin status service
   */
  private void sendAgentState() {

    //Figure out where we're sending the data
    String url = config.getItem(CaptureParameters.AGENT_STATUS_ENDPOINT_URL);
    if (url == null) {
      logger.warn("URL for {} is invalid, unable to push state to remote server.", CaptureParameters.AGENT_STATUS_ENDPOINT_URL);
      return;
    }

    List<NameValuePair> formParams = new ArrayList<NameValuePair>();

    String agentName = config.getItem(CaptureParameters.AGENT_NAME);
    if (agentName == null) {
      try {
        agentName = InetAddress.getLocalHost().getHostName();
      } catch (UnknownHostException e) {
        logger.error("Unable to resolve localhost.  This is very bad...", e);
        return;
      }
      logger.warn("Invalid name set for {} falling back to {}.", CaptureParameters.AGENT_NAME, agentName);
    }
    formParams.add(new BasicNameValuePair("agentName", agentName));
    formParams.add(new BasicNameValuePair("state", status.getAgentState()));

    send(formParams, url);
  }

  /**
   * Sends an update for each of the recordings currently being tracked in the system
   */
  private void sendRecordingState() {

    //Figure out where we're sending the data
    String url = config.getItem(CaptureParameters.RECORDING_STATUS_ENDPOINT_URL);
    if (url == null) {
      logger.warn("URL for {} is invalid, unable to push recording state to remote server.", CaptureParameters.RECORDING_STATUS_ENDPOINT_URL);
      return;
    }

    //For each recording being tracked by the system send an update
    Set<String> recordings = status.getRecordings();
    for (String id : recordings) {
      List<NameValuePair> formParams = new ArrayList<NameValuePair>();

      formParams.add(new BasicNameValuePair("id", id));
      formParams.add(new BasicNameValuePair("state", status.getRecordingState(id)));

      send(formParams, url);
    }
  }

  /**
   * Utility method to POST data to a URL.  This method encodes the data in UTF-8 as post data, rather than multipart MIME
   * @param formParams The data to send
   * @param url The URL to send the data to
   */
  private void send(List<NameValuePair> formParams, String url) {
    HttpPost remoteServer = new HttpPost(url);

    try {
      remoteServer.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));
      HttpClient client = new DefaultHttpClient();
      client.execute(remoteServer);
    } catch (Exception e) {
      logger.error("Unable to push update to remote server: {}.", e.getMessage());
    }
  }
}
