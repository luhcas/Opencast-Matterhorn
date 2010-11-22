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

import org.opencastproject.capture.api.CaptureAgent;
import org.opencastproject.capture.api.CaptureParameters;
import org.opencastproject.capture.impl.ConfigurationManager;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.TrustedHttpClientException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;

/**
 * This class is responsible for pushing the agent's state to the remote state service.
 */
public class AgentCapabilitiesJob implements Job {

  private static final Logger logger = LoggerFactory.getLogger(AgentStateJob.class);

  /**
   * Pushes the agent's capabilities to the remote state service.
   * {@inheritDoc}
   * @see org.quartz.Job#execute(JobExecutionContext)
   * @throws JobExecutionException
   */
  public void execute(JobExecutionContext ctx) throws JobExecutionException {
    ConfigurationManager config = (ConfigurationManager) ctx.getMergedJobDataMap().get(JobParameters.CONFIG_SERVICE);
    CaptureAgent agent = (CaptureAgent) ctx.getMergedJobDataMap().get(JobParameters.STATE_SERVICE);
    TrustedHttpClient client = (TrustedHttpClient) ctx.getMergedJobDataMap().get(JobParameters.TRUSTED_CLIENT);

    //Figure out where we're sending the data
    String url = config.getItem(CaptureParameters.AGENT_STATE_REMOTE_ENDPOINT_URL);
    if (url == null) {
      logger.warn("URL for {} is invalid, unable to push capabilities to remote server.",
                  CaptureParameters.AGENT_STATE_REMOTE_ENDPOINT_URL);
      return;
    }
    try {
      if (url.charAt(url.length()-1) == '/') {
        url += config.getItem(CaptureParameters.AGENT_NAME) + "/capabilities";
      } else {
        url += "/" + config.getItem(CaptureParameters.AGENT_NAME) + "/capabilities";
      }
    } catch (StringIndexOutOfBoundsException e) {
      logger.warn("Unable to build valid capabilities endpoint for agents.");
      return;
    }

    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    HttpResponse resp = null;
    try {
      agent.getAgentCapabilities().storeToXML(baos, "Capabilities for the agent " + agent.getAgentName());
    } catch (IOException e) {
      logger.warn("Unable to serialize agent capabilities!");
      return;
    }
    HttpPost remoteServer = new HttpPost(url);
    try {
      remoteServer.setEntity(new StringEntity(baos.toString()));
    } catch (UnsupportedEncodingException e) {
      logger.warn("Unable to send agent capapbillities because correct encoding scheme is not supported!");
      return;
    }
    try {
      resp = client.execute(remoteServer);
      if (resp.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
        logger.info("Capabilities push to {} failed with code {}.", url, resp.getStatusLine().getStatusCode());
      }
    } catch (TrustedHttpClientException e) {
      logger.warn("Unable to post capabilities to {}, message reads: {}.", url, e);
    } finally {
      if (resp != null) {
        client.close(resp);
      }
    }
  }
}
