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

import java.text.ParseException;

import org.opencastproject.capture.impl.CaptureAgentImpl;
import org.opencastproject.capture.impl.CaptureParameters;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to schedule the task of serializing the MediaPackage (this means: obtaining an XML
 * representation) and zipping it
 *
 */
public class SerializeJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(SerializeJob.class);

  /**
   * {@inheritDoc}
   * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
   */
  @Override
  public void execute(JobExecutionContext ctx) throws JobExecutionException {
    
    logger.info("Initiating serializeJob");
    
    // Obtains the recordingID
    String recordingID = ctx.getMergedJobDataMap().getString(CaptureParameters.RECORDING_ID);
    
    // Obtains the CaptureAgentImpl from the context
    CaptureAgentImpl ca = (CaptureAgentImpl)ctx.getMergedJobDataMap().get(JobParameters.CAPTURE_AGENT);
    
    // Creates manifest
    ca.createManifest(recordingID);
    
    logger.info("Manifest created");
    
    // Zips files
    ca.zipFiles(recordingID);
    
    logger.info("Files zipped");
    
    // Schedules Ingestion
    JobDetail job = new JobDetail("IngestJob", Scheduler.DEFAULT_GROUP, IngestJob.class);
    CronTrigger trigger;
    try {
      trigger = new CronTrigger("IngestJobTrigger", Scheduler.DEFAULT_GROUP, "IngestJob", Scheduler.DEFAULT_GROUP, "0/20 * * * * ?");
      trigger.getJobDataMap().put(JobParameters.CAPTURE_AGENT, ca);
      trigger.getJobDataMap().put(CaptureParameters.RECORDING_ID, recordingID);

      //Schedule the update
      ctx.getScheduler().scheduleJob(job, trigger);

    } catch (ParseException e) {
      logger.error("Invalid argument for CronTrigger: {}", e.getMessage());
      e.printStackTrace();
    } catch (SchedulerException e) {
      logger.error("Couldn't schedule task: {}", e.getMessage());
      e.printStackTrace();
    }   


  }

}
