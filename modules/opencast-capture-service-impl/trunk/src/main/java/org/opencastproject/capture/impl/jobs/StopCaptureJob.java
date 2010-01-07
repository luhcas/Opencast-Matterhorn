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
import org.opencastproject.capture.impl.SchedulerImpl;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * The class responsible for stopping a capture.
 */
public class StopCaptureJob implements Job {
  
  private static final Logger logger = LoggerFactory.getLogger(StopCaptureJob.class);
  
  // TODO: Move these constants into some common interface such as 'JobParameters'
  /** Constant used to define the key for the recording ID which is pulled out of the execution context */
  public static final String RECORDING_ID = "recording_id";
  
  /**
   * Stops the capture.
   * {@inheritDoc}
   * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
   */
  public void execute(JobExecutionContext ctx) throws JobExecutionException {
    
    logger.info ("Initiating stopCaptureJob");
    
    try {
      // Extract the Capture Agent to stop the capture ASAP
      CaptureAgentImpl ca = (CaptureAgentImpl)ctx.getMergedJobDataMap().get(SchedulerImpl.CAPTURE_AGENT);
      ca.stopCapture();
      
      // Extract the recording ID
      String recordingID = ctx.getMergedJobDataMap().getString(RECORDING_ID);
      
      // Create job and trigger
      JobDetail job = new JobDetail("SerializeJob", Scheduler.DEFAULT_GROUP, SerializeJob.class);
      // TODO: Should we need a cron trigger in case the serialization fails? 
      // Or do we assume that is an unrecoverable error?
      SimpleTrigger trigger = new SimpleTrigger("SerializeJobTrigger", Scheduler.DEFAULT_GROUP, new Date());
      trigger.getJobDataMap().put(RECORDING_ID, recordingID);
      trigger.getJobDataMap().put(SchedulerImpl.CAPTURE_AGENT, ca);

      //Schedule the serializeJob
      ctx.getScheduler().scheduleJob(job, trigger);
      
      logger.info("stopCaptureJob complete");
      
    } catch (SchedulerException e) {
      logger.error("Couldn't schedule task: {}", e.getMessage());
      e.printStackTrace();
    } catch (Exception e) {
      logger.error("Unexpected exception: {}", e.getMessage());
      e.printStackTrace();
    }
  }

}
