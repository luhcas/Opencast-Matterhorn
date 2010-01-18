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
import java.util.Properties;

import org.opencastproject.capture.impl.CaptureAgentImpl;
import org.opencastproject.capture.impl.CaptureParameters;
import org.opencastproject.capture.impl.SchedulerImpl;
import org.opencastproject.media.mediapackage.MediaPackage;
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
 * The class responsible for starting a capture.
 */
public class StartCaptureJob implements Job {

  private static final Logger logger = LoggerFactory.getLogger(StartCaptureJob.class);

  // TODO: Move these constants into some common interface such as 'JobParameters'  
  /** Constant used to define the key for the properties object which is pulled out of the execution context */
  public static final String CAPTURE_PROPS = "capture_props";

  /** Constant used to define the key for the media package object which is pulled out of the execution context */
  public static final String MEDIA_PACKAGE = "media_package";

  /** Constant used to define the key for the Dublic Core catalog which is pulled out of the execution context */
  // TODO: necessary????
  //public static final String DUBLIN_CORE = "dublin_core";

  /**
   * Starts the capture itself.
   * {@inheritDoc}
   * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
   */
  public void execute(JobExecutionContext ctx) throws JobExecutionException {

    CaptureAgentImpl ca = null;
    MediaPackage mp = null;
    Properties props = null;

    //// Extracts the necessary parameters for calling startCapture()
    // The capture agent
    ca = (CaptureAgentImpl)ctx.getMergedJobDataMap().get(SchedulerImpl.CAPTURE_AGENT);
    // The MediaPackage
    mp = (MediaPackage)ctx.getMergedJobDataMap().get(MEDIA_PACKAGE);
    // The capture Properties
    props = (Properties)ctx.getMergedJobDataMap().get(CAPTURE_PROPS);

    if (ca == null) {
      logger.error("No capture agent provided! Capture Interrupted");
      return;
    }

    // TODO: Considering ONLY the case where we have both parameters. Should we create default MPkg. and/or Properties otherwise?
    if (props == null || mp == null) {
      logger.error("Insufficient parameters provided. startCapture() needs Properties and a MediaPackage to proceed");
      return;
    }

    try {

      // Get the stopCaptureJob scheduling ready in case something happens, so we don't need to stop the capture afterwards
      String time2Stop = props.getProperty(CaptureParameters.RECORDING_END);

      JobDetail job = new JobDetail("StopCapture", Scheduler.DEFAULT_GROUP, StopCaptureJob.class);
      CronTrigger trigger = new CronTrigger("StopCaptureTrigger", Scheduler.DEFAULT_GROUP, time2Stop);

      trigger.getJobDataMap().put(SchedulerImpl.CAPTURE_AGENT, ca);

      // Actually does the service
      String recordingID = ca.startCapture(mp, props);

      // Stores the recordingID so that it can be passed from one job to the other
      trigger.getJobDataMap().put(StopCaptureJob.RECORDING_ID, recordingID);

      // Schedules the stop event
      ctx.getScheduler().scheduleJob(job, trigger);
      logger.info("stopCapture scheduled");

    } catch (SchedulerException e) {
      logger.error("Couldn't schedule task: {}", e.getMessage());
      //e.printStackTrace();
    } catch (ParseException e) {
      logger.error("Invalid time for stopping capture: {}. Aborting.\n{}", props.get(CaptureParameters.RECORDING_END), e.getMessage());
      //e.printStackTrace();
    } catch (Exception e) {
      logger.error("Unexpected exception: {}\nJob may have not been executed", e.getMessage());
      e.printStackTrace();
    }
  }

  //This code was taken out from CaptureAgentImpl and should belong here.
  //It does basically the same functionality than this job
  //TODO: Verify that everything which is done below is also implemented above and delete this comment
  
  /*    CronExpression end = null;
  if (current_capture_properties.containsKey(CaptureParameters.RECORDING_END)) {
    try {
      end = new CronExpression(current_capture_properties.getProperty(CaptureParameters.RECORDING_END));
    } catch (ParseException e) {
      logger.error("Invalid end time for capture {}, skipping startup!", recordingID);
      setRecordingState(recordingID, RecordingState.CAPTURE_ERROR);
      setAgentState(AgentState.IDLE);
      return false;
    }
  } else {
    try {
      Date date = new Date(System.currentTimeMillis() + default_capture_length);
      StringBuilder sb = new StringBuilder();
      //TODO:  Remove the deprecated calls here.
      sb.append(date.getSeconds() + " ");
      sb.append(date.getMinutes() + " ");
      sb.append(date.getHours() + " ");
      sb.append(date.getDate() + " ");
      sb.append(date.getMonth() + 1 + " "); //Note:  Java numbers months from 0-11, Quartz uses 1-12.  Sigh.
      sb.append("? ");
      end = new CronExpression(sb.toString());
    } catch (ParseException e) {
      logger.error("Parsing exception in default length fallback code.  This is very bad.");
      setRecordingState(recordingID, RecordingState.CAPTURE_ERROR);
      setAgentState(AgentState.IDLE);
      return false;
    }
  }
  JobDetail job = new JobDetail("STOP-" + recordingID, Scheduler.DEFAULT_GROUP, StopCaptureJob.class);
  job.getJobDataMap().put(CaptureParameters.RECORDING_ID, recordingID);

  CronTrigger trig = new CronTrigger();
  trig.setName(recordingID);
  trig.setCronExpression(end);

  try {
    captureScheduler.scheduleJob(job, trig);
  } catch (SchedulerException e) {
    logger.error("Unable to schedule stopCapture for {}, skipping capture: {}.", recordingID, e.toString());
    e.printStackTrace();
    setRecordingState(recordingID, RecordingState.CAPTURE_ERROR);
    setAgentState(AgentState.IDLE);
    return false;
  }*/

}
