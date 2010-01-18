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

import org.opencastproject.capture.impl.CaptureAgentImpl;
import org.opencastproject.capture.impl.CaptureParameters;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.StatefulJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates the manifest, then attempts to ingest the media to the remote server
 */
public class IngestJob implements StatefulJob {

  private static final Logger logger = LoggerFactory.getLogger(IngestJob.class);
  
  /**
   * {@inheritDoc}
   * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
   */
  public void execute(JobExecutionContext ctx) throws JobExecutionException {

    logger.info("Initiating ingestJob");
    
    // Obtains the recordingID
    String recordingID = ctx.getMergedJobDataMap().getString(CaptureParameters.RECORDING_ID);
    
    // Obtains the CaptureAgentImpl from the context
    CaptureAgentImpl ca = (CaptureAgentImpl)ctx.getMergedJobDataMap().get(JobParameters.CAPTURE_AGENT);

    logger.info("Proceeding to try ingestion");
    // Tries ingestion
    int result = ca.ingest(recordingID);
    
    if (result != 200) {
      logger.error("Ingestion failed with a value of {}", result);
    } else { 
      logger.info("Ingestion finished");
      try {
        ctx.getScheduler().unscheduleJob("IngestJobTrigger", Scheduler.DEFAULT_GROUP);
      } catch (SchedulerException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } 

    }
  }
}
