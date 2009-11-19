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
package org.opencastproject.capture.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
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

  /**
   * Pushes the agent's status to the remote status service
   * {@inheritDoc}
   * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
   */
  public void execute(JobExecutionContext ctx) throws JobExecutionException {
    ConfigurationManager config = ConfigurationManager.getInstance();
    //Figure out where we're sending the data
    String url = config.getItem(CaptureParameters.AGENT_STATUS_ENDPOINT_URL);
    if (url == null) {
      logger.warn("URL for " + CaptureParameters.AGENT_STATUS_ENDPOINT_URL + " is invalid, unable to push status to remote server");
      return;
    }
    HttpPost remoteServer = new HttpPost(url);
    List<NameValuePair> formParams = new ArrayList<NameValuePair>();

    //Build the list of data that gets sent
    StatusServiceImpl status = (StatusServiceImpl) ctx.getMergedJobDataMap().get(StatusServiceImpl.SERVICE);

    String agentName = config.getItem(CaptureParameters.AGENT_NAME);
    if (agentName == null) {
      try {
        agentName = InetAddress.getLocalHost().getHostName();
      } catch (UnknownHostException e) {
        logger.error("Unable to resolve localhost.  This is very bad...", e);
        return;
      }
      logger.warn("Invalid name set for " + CaptureParameters.AGENT_NAME + " falling back to " + agentName);
    }
    formParams.add(new BasicNameValuePair("agentName", agentName));
    formParams.add(new BasicNameValuePair("state", status.getState()));

    //Send the data
    try {
      remoteServer.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));
      HttpClient client = new DefaultHttpClient();
      client.execute(remoteServer);
    } catch (Exception e) {
      logger.error("Unable to push agent status to remote server", e);
    }
  }

}
