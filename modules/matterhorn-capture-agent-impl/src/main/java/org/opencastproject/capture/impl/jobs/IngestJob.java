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

import org.opencastproject.capture.api.CaptureParameters;
import org.opencastproject.capture.impl.CaptureAgentImpl;

import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.quartz.StatefulJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class to create the manifest, then attempt to ingest the media to the remote server.
 */
public class IngestJob implements StatefulJob {

  private static final Logger logger = LoggerFactory.getLogger(IngestJob.class);
  
  /**
   * Attempts to ingest the job to the central core.
   * {@inheritDoc}
   * @see org.quartz.Job#execute(JobExecutionContext)
   * @throws JobExecutionException
   */
  public void execute(JobExecutionContext ctx) throws JobExecutionException {

    logger.debug("Initiating ingestJob");
    
    // Obtains the recordingID
    String recordingID = ctx.getMergedJobDataMap().getString(CaptureParameters.RECORDING_ID);
    
    // Obtains the CaptureAgentImpl from the context
    CaptureAgentImpl ca = (CaptureAgentImpl)ctx.getMergedJobDataMap().get(JobParameters.CAPTURE_AGENT);

    logger.info("Proceeding to try ingest");
    // Tries ingest
    int result = ca.ingest(recordingID);
    
    if (result != 200) {
      logger.error("Ingest failed with a value of {}", result);
    } else { 
      logger.info("Ingest finished");

      //Remove this job from the system
      JobDetail mine = ctx.getJobDetail();
      try {
        ctx.getScheduler().deleteJob(mine.getName(), mine.getGroup());
      } catch (SchedulerException e) {
        logger.warn("Unable to delete ingest job {}!", mine.getName());
        e.printStackTrace();
      }
    }
  }
}
