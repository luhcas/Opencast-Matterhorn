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

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.Dictionary;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.gstreamer.Bus;
import org.gstreamer.GstObject;
import org.gstreamer.Pipeline;
import org.gstreamer.State;
import org.gstreamer.event.EOSEvent;
import org.opencastproject.capture.admin.api.AgentState;
import org.opencastproject.capture.admin.api.RecordingState;
import org.opencastproject.capture.api.CaptureAgent;
import org.opencastproject.capture.api.StateService;
import org.opencastproject.capture.impl.jobs.IngestJob;
import org.opencastproject.capture.impl.jobs.StopCaptureJob;
import org.opencastproject.capture.pipeline.PipelineFactory;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.quartz.CronExpression;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of the Capture Agent: using gstreamer, generates several Pipelines
 * to store several tracks from a certain recording.
 */
public class CaptureAgentImpl implements CaptureAgent, ManagedService {
  private static final Logger logger = LoggerFactory.getLogger(CaptureAgentImpl.class);

  private static final long default_capture_length = 1 * 60 * 60 * 1000; //1 Hour

  private Pipeline pipe = null;
  private ConfigurationManager config = ConfigurationManager.getInstance();

  /** The agent's current state.  Used for logging */
  private String agent_state = null;
  /** A pointer to the current capture directory.  Note that this should be null except for when we are actually capturing */
  private File current_capture_dir = null;
  /** The properties object for the current capture.  NOTE THAT THIS WILL BE NULL IF THE AGENT IS NOT CURRENTLY CAPTURING. */
  private Properties current_capture_properties = null;
  /** A pointer to the state service.  This is where all of the recording state information should be kept. */
  private StateService state_service = null;
  /** The scheduler which is used to schedule stopCapture calls */
  private Scheduler captureScheduler = null;

  /**
   * Called when the bundle is activated.
   * @param cc The component context
   */
  public void activate(ComponentContext cc) {
    logger.info("Starting CaptureAgentImpl.");
    setAgentState(AgentState.IDLE);
    try {
      //Load the properties for this scheduler.  Each scheduler requires its own unique properties file.
      Properties captureProperties = new Properties();
      captureProperties.load(getClass().getClassLoader().getResourceAsStream("config/capture_scheduler.properties"));
      StdSchedulerFactory sched_fact = new StdSchedulerFactory(captureProperties);
      //Create and start the capture scheduler
      captureScheduler = sched_fact.getScheduler();
      if (!captureScheduler.isStarted()) {
        captureScheduler.start();
      }
    } catch (SchedulerException e) {
      throw new RuntimeException("Internal error in capture scheduler, unable to start.", e);
    } catch (IOException e) {
      throw new RuntimeException("IOException, unable to load Quartz properties file for capture scheduling.");
    }
  }

  /**
   * Gets the state service this capture agent is pushing its state to
   * @return The service this agent pushes its state to.
   */
  public StateService getStateService() {
    return state_service;
  }

  /**
   * Sets the state service this capture agent should push its state to.
   * @param service The service to push the state information to
   */
  public void setStateService(StateService service) {
    state_service = service;
    setAgentState(agent_state);
  }

  /**
   * Unsets the state service which this capture agent should push its state to.
   */
  public void unsetStateService() {
    state_service = null;
  }
  

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.recorder.api.CaptureAgent#startCapture()
   */
  public boolean startCapture() {

    logger.info("Starting capture using default values for MediaPackage and properties.");

    // Creates default MediaPackage
    MediaPackage pack;
    try {
      pack = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    } catch (org.opencastproject.util.ConfigurationException e) {
      logger.error("Wrong configuration for the default media package: {}.", e.getMessage());
      return false;
    } catch (MediaPackageException e) {
      logger.error("Media Package exception: {}.", e.getMessage());
      return false;
    }

    return startCapture(pack, null);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.recorder.api.CaptureAgent#startCapture(org.opencastproject.media.mediapackage.MediaPackage)
   */
  public boolean startCapture(MediaPackage mediaPackage) {

    logger.info("Starting capture using default values for the capture properties and a passed in media package.");
    
    return startCapture(mediaPackage, null);

  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.recorder.api.CaptureAgent#startCapture(java.util.HashMap)
   */
  public boolean startCapture(Properties properties) {
    logger.info("Starting capture using a passed in properties and default media package.");

    // Creates default MediaPackage
    MediaPackage pack;
    try {
      pack = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    } catch (org.opencastproject.util.ConfigurationException e) {
      logger.error("Wrong configuration for the default media package: {}.", e.getMessage());
      return false;
    } catch (MediaPackageException e) {
      logger.error("Media Package exception: {}.", e.getMessage());
      return false;
    }

    return startCapture(pack, properties);
  }

  /**
   * {@inheritDoc}
   * 
   * @see 
   *      org.opencastproject.recorder.api.CaptureAgent#startCapture(org.opencastproject.media.mediapackage.MediaPackage,
   *      HashMap properties)
   */
  public boolean startCapture(MediaPackage mediaPackage, Properties properties) {

    if (current_capture_dir != null || !agent_state.equals(AgentState.IDLE)) {
      logger.warn("Unable to start capture, a different capture is still in progress in {}.", current_capture_dir.getAbsolutePath());
      return false;
    } else {
      setAgentState(AgentState.CAPTURING);
    }

    logger.info("Initializing devices for capture.");

    // merges properties without overwriting the system's configuration
    Properties merged = config.merge(properties, false);

    String recordingID = null;

    //Figure out where captureDir lives
    if (merged.containsKey(CaptureParameters.RECORDING_ROOT_URL)) {
      current_capture_dir = new File(merged.getProperty(CaptureParameters.RECORDING_ROOT_URL));
      if (merged.containsKey(CaptureParameters.RECORDING_ID)) {
        recordingID = merged.getProperty(CaptureParameters.RECORDING_ID);
      } else {
        //In this case they've set the root URL, but not the recording ID.  Get the id from that url instead then.
        logger.warn("{} was set, but not {}.", CaptureParameters.RECORDING_ROOT_URL, CaptureParameters.RECORDING_ID);
        String[] pathAry = merged.getProperty(CaptureParameters.RECORDING_ROOT_URL).split(File.separator);
        recordingID = pathAry[pathAry.length-1];
        merged.put(CaptureParameters.RECORDING_ID, recordingID);
      }
    } else {
      //If there is a recording ID use it, otherwise it's unscheduled so just grab a timestamp
      if (merged.containsKey(CaptureParameters.RECORDING_ID)) {
        recordingID = merged.getProperty(CaptureParameters.RECORDING_ID);
        current_capture_dir = new File(merged.getProperty(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL), recordingID);
      } else {
        //Unscheduled capture, use a timestamp value instead
        recordingID = "Unscheduled-" + System.currentTimeMillis();
        merged.setProperty(CaptureParameters.RECORDING_ID, recordingID);
        current_capture_dir = new File(merged.getProperty(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL), recordingID);
      }
      merged.put(CaptureParameters.RECORDING_ROOT_URL, current_capture_dir.getAbsolutePath());
    }

    //Setup the root capture dir, also make sure that it exists.
    if (!current_capture_dir.exists()) {
      try {
        FileUtils.forceMkdir(current_capture_dir);
      } catch (IOException e) {
        logger.error("IOException creating required directory {}.", current_capture_dir.toString());
        setAgentState(AgentState.IDLE);
        setRecordingState(recordingID, RecordingState.CAPTURE_ERROR);
        return false;
      }
      //Should have been created.  Let's make sure of that.
      if (!current_capture_dir.exists()) {
        logger.error("Unable to start capture, could not create required directory {}.", current_capture_dir.toString());
        setAgentState(AgentState.IDLE);
        setRecordingState(recordingID, RecordingState.CAPTURE_ERROR);
        return false;
      }
    }

    pipe = PipelineFactory.create(merged);

    if (pipe == null) {
      logger.error("Capture could not start, pipeline was null!");
      setAgentState(AgentState.IDLE);
      return false;
    }

    Bus bus = pipe.getBus();
    bus.connect(new Bus.EOS() {
      /**
       * {@inheritDoc}
       * @see org.gstreamer.Bus.EOS#endOfStream(org.gstreamer.GstObject)
       */
      public void endOfStream(GstObject arg0) {
        logger.debug("Pipeline received EOS.");
      }
    });
    bus.connect(new Bus.ERROR() {
      /**
       * {@inheritDoc}
       * @see org.gstreamer.Bus.ERROR#errorMessage(org.gstreamer.GstObject, int, java.lang.String)
       */
      public void errorMessage(GstObject arg0, int arg1, String arg2) {
        logger.error(arg0.getName() + ": " + arg2);
        stopCapture();
      }
    });

    current_capture_properties = merged;

    CronExpression end = null;
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
    }
    
//    pipe.play();

    // It **SEEMS** the state changes are immediate (for the test i've done so far)
    while (pipe.getState() != State.PLAYING);
    logger.info("{} started.", pipe.getName());
    //Gst.main();
    //Gst.deinit();

    setRecordingState(recordingID, RecordingState.CAPTURING);
    setAgentState(AgentState.CAPTURING);
    return true;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.CaptureAgent#stopCapture()
   */
  public boolean stopCapture() {
    if (pipe == null) {
      logger.warn("Pipeline is null, unable to stop capture.");
      setAgentState(AgentState.IDLE);
      return false;
    }

    File stopFlag = new File(current_capture_dir, "capture.stopped");

    //Take the properties out of the class level variable so that we can start capturing again immediately without worrying about overwriting them.
    Properties cur = current_capture_properties;
    current_capture_properties = null;

    //Update the states of everything.
    String recordingID = cur.getProperty(CaptureParameters.RECORDING_ID);
    setRecordingState(recordingID, RecordingState.CAPTURE_FINISHED);
    setAgentState(AgentState.IDLE);
    current_capture_dir = null;

    try {
      // Sending End Of Stream event to the Pipeline so its components stop appropriately
      pipe.sendEvent(new EOSEvent());
      //while (pipe.getState() != State.NULL);
      pipe.setState(State.NULL);

     // Gst.deinit();

      stopFlag.createNewFile();
    } catch (IOException e) {
      setRecordingState(recordingID, RecordingState.UPLOAD_ERROR);
      logger.error("IOException: Could not create \"capture.stopped\" file: {}.", e.getMessage());
      //TODO:  Is this capture.stopped file required for ingest to work?  Then it should die here rather than trying to build the manifest.
    }

    try {
      IngestJob.scheduleJob(cur, state_service);
    } catch (IOException e) {
      logger.error("IOException while attempting to schedule ingest for recording {}.", recordingID);
      setRecordingState(recordingID, RecordingState.UPLOAD_ERROR);
      return false;
    } catch (SchedulerException e) {
      logger.error("SchedulerException while attempting to schedule ingest for recording {}: {}.", recordingID, e);
      setRecordingState(recordingID, RecordingState.UPLOAD_ERROR);
      return false;
    }

    return true;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.CaptureAgent#stopCapture()
   */
  public boolean stopCapture(String recordingID) {
    if (current_capture_properties != null) {
      String current_id = current_capture_properties.getProperty(CaptureParameters.RECORDING_ID); 
      if (recordingID.equals(current_id)) {
        return stopCapture();
      }
    }
    return false;
  }

  /**
   * Sets the machine's current encoding status
   * 
   * @param state The state for the agent.  Defined in AgentState.
   * @see org.opencastproject.capture.api.AgentState
   */
  private void setAgentState(String state) {
    agent_state = state;
    if (state_service != null) {
      state_service.setAgentState(agent_state);
    } else {
      logger.warn("State service for capture agent is null, unable to push updates to remote server!");
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.CaptureAgent#getAgentState()
   */
  public String getAgentState() {
    return agent_state;
  }

  /**
   * Convenience method which wraps calls to the state_service to make sure it's not going to null pointer on me.
   * @param recordingID The ID of the recording to update
   * @param state The state to update the recording to
   */
  private void setRecordingState(String recordingID, String state) {
    if (state_service != null) {
      state_service.setRecordingState(recordingID, state);
    } else {
      logger.warn("State service for capture agent is null, unable to push updates to remote server!");
    }
  }

  public void updated(Dictionary props) throws ConfigurationException {
    // Update any configuration properties here
  }
}
