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

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class responsible for starting a capture.
 */
public class CaptureJob implements Job {
  
  private static final Logger logger = LoggerFactory.getLogger(CaptureJob.class);

  /**
   * Starts the capture itself.
   * {@inheritDoc}
   * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
   */
  public void execute(JobExecutionContext ctx) throws JobExecutionException {
    logger.warn("Capture job fired but nothing will be sent because I don't know what needs sending!");

    ConfigurationManager config = ConfigurationManager.getInstance();

    //Figure out where we're sending the data
    //TODO:  Should this be hardcoded, or grabbed from some config?
    HttpPost remoteServer = new HttpPost("http://localhost:8080/capture/rest/StartCapture");
    List<NameValuePair> formParams = new ArrayList<NameValuePair>();

    //TODO:  Fill in the data the capture start function needs  (see MH-1184)
    //TODO:  Make this into a properties object for the REST endpoint
    //CaptureParameters.CAPTURE_DEVICE_NAMES = CSV of friendly device name
    //CaptureParameters.CAPTURE_DEVICE_PREFIX+".FRIENDLYNAME.src"="/dev/video0"
    //CaptureParameters.CAPTURE_DEVICE_PREFIX+".FRIENDLYNAME.outputfile"="presenter.mpg"
    
    //Send the data
    try {
      remoteServer.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));
      HttpClient client = new DefaultHttpClient();
      client.execute(remoteServer);
    } catch (Exception e) {
      logger.error("Unable to start capture: {}.", e.getMessage());
    }
  }

}
