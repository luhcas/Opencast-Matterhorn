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

import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;

/**
 * The class to schedule the task of serializing the MediaPackage (this means: obtaining an XML
 * representation) and zipping it.
 *
 */
public class SerializeJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(SerializeJob.class);

  /**
   * Generates a manifest file then zips everything up so it can be ingested.  Also schedules a IngestJob
   * {@inheritDoc}
   * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
   */
  @Override
  public void execute(JobExecutionContext ctx) throws JobExecutionException {
    
    logger.debug("Initiating serializeJob");
    
    // Obtains the recordingID
    String recordingID = ctx.getMergedJobDataMap().getString(CaptureParameters.RECORDING_ID);
    
    // Obtains the CaptureAgentImpl from the context
    CaptureAgentImpl ca = (CaptureAgentImpl)ctx.getMergedJobDataMap().get(JobParameters.CAPTURE_AGENT);
    
    // Creates manifest
    boolean manifestCreated = ca.createManifest(recordingID);
    if (!manifestCreated) {
      throw new JobExecutionException("Unable to create manifest properly, serialization job failed but will retry.");
    }
    
    logger.info("Manifest created");
    
    // Zips files
    ca.zipFiles(recordingID);
    
    logger.info("Files zipped");

    String postfix = ctx.getMergedJobDataMap().getString(JobParameters.JOB_POSTFIX);

    // Schedules Ingestion
    JobDetail job = new JobDetail("IngestJob-" + postfix, JobParameters.OTHER_TYPE, IngestJob.class);
    CronTrigger trigger;
    try {
      trigger = new CronTrigger();
      trigger.setGroup(JobParameters.OTHER_TYPE);
      trigger.setName("IngestJobTrigger-" + postfix);
      //TODO:  Make this configurable.  Or at least slow it down a bit - hitting things every 20 seconds it too fast.
      trigger.setCronExpression("0/20 * * * * ?");
      trigger.getJobDataMap().put(JobParameters.CAPTURE_AGENT, ca);
      trigger.getJobDataMap().put(CaptureParameters.RECORDING_ID, recordingID);
      trigger.getJobDataMap().put(JobParameters.JOB_POSTFIX, postfix);

      //Schedule the update
      ctx.getScheduler().scheduleJob(job, trigger);

      //Remove this job from the system
      JobDetail mine = ctx.getJobDetail();
      try {
        ctx.getScheduler().deleteJob(mine.getName(), mine.getGroup());
      } catch (SchedulerException e) {
        logger.warn("Unable to delete serialize job {}!", mine.getName());
        e.printStackTrace();
      }

    } catch (ParseException e) {
      logger.error("Invalid argument for CronTrigger: {}", e.getMessage());
      e.printStackTrace();
    } catch (SchedulerException e) {
      logger.error("Couldn't schedule task: {}", e.getMessage());
      e.printStackTrace();
    }   


  }

}
