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

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.opencastproject.capture.impl.CaptureParameters;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class responsible for stopping a capture.
 */
public class StopCaptureJob implements Job {
  
  private static final Logger logger = LoggerFactory.getLogger(StopCaptureJob.class);

  /**
   * Stops the capture.
   * {@inheritDoc}
   * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
   */
  public void execute(JobExecutionContext ctx) throws JobExecutionException {

    logger.error("Stopcapturejob firing");
    //Figure out where we're sending the data
    //TODO:  Should this be hardcoded, or grabbed from some config?
    HttpPost remoteServer = new HttpPost("http://localhost:8080/capture/rest/stopCapture");
    List<NameValuePair> formParams = new ArrayList<NameValuePair>();

    String recordingID = ctx.getMergedJobDataMap().getString(CaptureParameters.RECORDING_ID);

    //Note that config must be the same as the name in the endpoint!
    formParams.add(new BasicNameValuePair("recordingID", recordingID));
    
    //Send the data
    try {
      remoteServer.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));
      HttpClient client = new DefaultHttpClient();
      client.execute(remoteServer);
    } catch (Exception e) {
      logger.error("Unable to stop capture: {}.", e.getMessage());
    }
  }

}
