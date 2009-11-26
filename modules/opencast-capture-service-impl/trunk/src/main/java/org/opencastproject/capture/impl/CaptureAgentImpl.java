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
import java.util.Dictionary;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.gstreamer.Bus;
import org.gstreamer.Gst;
import org.gstreamer.GstObject;
import org.gstreamer.Pipeline;
import org.gstreamer.State;
import org.gstreamer.event.EOSEvent;
import org.opencastproject.capture.api.AgentState;
import org.opencastproject.capture.api.CaptureAgent;
import org.opencastproject.capture.api.RecordingState;
import org.opencastproject.capture.pipeline.PipelineFactory;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


// TODO: Eliminate dependency with MediaInspector in pom.xml, if it is not finally used

/**
 * Implementation of the Capture Agent: using gstreamer, generates several Pipelines
 * to store several tracks from a certain recording.
 */
public class CaptureAgentImpl implements CaptureAgent, ManagedService {
  private static final Logger logger = LoggerFactory.getLogger(CaptureAgentImpl.class);

  private Pipeline pipe = null;
  private ConfigurationManager config = ConfigurationManager.getInstance();

  /** Used by the AgentStatusJob class to pull a pointer to the CaptureAgentImpl so it can poll for status updates */
  public static final String SERVICE = "status_service";
  /** The agent's current state.  Used for logging */
  private String agent_state = AgentState.IDLE;
  /** A pointer to the current capture directory.  Note that this should be null except for when we are actually capturing */
  private File current_capture_dir = null;
  /** The properties object for the current capture.  NOTE THAT THIS WILL BE NULL IF THE AGENT IS NOT CURRENTLY CAPTURING. */
  private Properties current_capture_properties = null;
  /** A pointer to the state singleton.  This is where all of the recording state information should be kept. */
  private StateSingleton status_service = null;

  /**
   * Called when the bundle is activated.
   * @param cc The component context
   */
  public void activate(ComponentContext cc) {
    logger.info("Starting CaptureAgentImpl.");
    status_service = StateSingleton.getInstance();
    status_service.setCaptureAgent(this);
    setAgentState(AgentState.IDLE);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.recorder.api.CaptureAgent#startCapture()
   */
  public String startCapture() {

    logger.info("Starting capture using default values for MediaPackage and properties.");

    // Creates default MediaPackage
    MediaPackage pack;
    try {
      pack = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    } catch (org.opencastproject.util.ConfigurationException e) {
      logger.error("Wrong configuration for the default media package: {}.", e.getMessage());
      return "Wrong MediaPackage configuration";
    } catch (MediaPackageException e) {
      logger.error("Media Package exception: {}.", e.getMessage());
      return "Media Package exception";
    }

    return startCapture(pack, null);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.recorder.api.CaptureAgent#startCapture(org.opencastproject.media.mediapackage.MediaPackage)
   */
  public String startCapture(MediaPackage mediaPackage) {

    logger.info("Starting capture using default values for the capture properties and a passed in media package.");
    
    return startCapture(mediaPackage, null);

  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.recorder.api.CaptureAgent#startCapture(java.util.HashMap)
   */
  public String startCapture(Properties properties) {
    logger.info("Starting capture using a passed in properties and default media package.");

    // Creates default MediaPackage
    MediaPackage pack;
    try {
      pack = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    } catch (org.opencastproject.util.ConfigurationException e) {
      logger.error("Wrong configuration for the default media package: {}.", e.getMessage());
      return "Wrong MediaPackage configuration";
    } catch (MediaPackageException e) {
      logger.error("Media Package exception: {}.", e.getMessage());
      return "Media Package exception";
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
  public String startCapture(MediaPackage mediaPackage, Properties properties) {

    if (current_capture_dir != null || !agent_state.equals(AgentState.IDLE)) {
      logger.warn("Unable to start capture, a different capture is still in progress in {}.", current_capture_dir.getAbsolutePath());
      return "Unable to start capture, a different capture is still in progress in " + current_capture_dir.getAbsolutePath() + ".";
    }

    logger.info("Initializing devices for capture.");

    // merges properties without overwriting the system's configuration
    Properties merged = config.merge(properties, false);

    //Get the recording id
    String recordingID = merged.getProperty(CaptureParameters.RECORDING_ID);

    //Figure out where captureDir lives
    if (merged.contains(CaptureParameters.RECORDING_ROOT_URL)) {
      current_capture_dir = new File(merged.getProperty(CaptureParameters.RECORDING_ROOT_URL));
    } else {
      //If there is a recording ID use it, otherwise it's unscheduled so just grab a timestamp
      if (merged.contains(CaptureParameters.RECORDING_ID)) {
        current_capture_dir = new File(config.getItem(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL), recordingID);
      } else {
        //Unscheduled capture, use a timestamp value instead
        recordingID = "Unscheduled-" + System.currentTimeMillis();
        merged.setProperty(CaptureParameters.RECORDING_ID, recordingID);
        current_capture_dir = new File(config.getItem(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL), recordingID);
      }
      merged.put(CaptureParameters.RECORDING_ROOT_URL, current_capture_dir.toString());
    }

    //Setup the root capture dir, also make sure that it exists.
    if (!current_capture_dir.exists()) {
      try {
        FileUtils.forceMkdir(current_capture_dir);
      } catch (IOException e) {
        logger.error("IOException creating required directory {}.", current_capture_dir.toString());
        status_service.setRecordingState(recordingID, RecordingState.CAPTURE_ERROR);
        return "IOException creating required directory " + current_capture_dir.toString();
      }
      //Should have been created.  Let's make sure of that.
      if (!current_capture_dir.exists()) {
        logger.error("Unable to start capture, could not create required directory {}.", current_capture_dir.toString());
        status_service.setRecordingState(recordingID, RecordingState.CAPTURE_ERROR);
        return "Unable to start capture, could not create required directory " + current_capture_dir.toString();
      }
    }

    pipe = PipelineFactory.create(merged);

    if (pipe == null) {
      logger.error("Capture could not start, pipeline was null!");
      return "Capture could not start, pipline was null!";
    }

    Bus bus = pipe.getBus();
    bus.connect(new Bus.EOS() {
      public void endOfStream(GstObject arg0) {
        logger.debug("Pipeline received EOS.");
      }
    });
    bus.connect(new Bus.ERROR() {
      public void errorMessage(GstObject arg0, int arg1, String arg2) {
        //TODO:  What does this mean?
        logger.error(arg0.getName() + ": " + arg2);
        stopCapture();
      }
    });

    current_capture_properties = merged;
    pipe.play();

    // It **SEEMS** the state changes are immediate (for the test i've done so far)
    while (pipe.getState() != State.PLAYING);
    logger.info("{} started.", pipe.getName());
    //Gst.main();
    //Gst.deinit();

    status_service.setRecordingState(recordingID, RecordingState.CAPTURING);
    setAgentState(AgentState.CAPTURING);
    return "Capture started";
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.api.CaptureAgent#stopCapture()
   */
  public String stopCapture() {
    if (pipe == null) {
      logger.warn("Pipeline is null, unable to stop capture.");
      return "Pipeline is null.";
    }
    String result = "Capture OK";
    File stopFlag = new File(current_capture_dir, "capture.stopped");
    
    try {
      // "READY" is the idle state for pipelines
      pipe.sendEvent(new EOSEvent());
      //while (pipe.getState() != State.NULL);
      pipe.setState(State.NULL);

      Gst.deinit();

      stopFlag.createNewFile();
    } catch (IOException e) {
      logger.error("IOException: Could not create \"capture.stopped\" file: {}.", e.getMessage());
      result = "\"capture.stopped\" could not be created.";
      //TODO:  Is this capture.stopped file required for ingest to work?  Then it should die here rather than trying to build the manifest.
    }

    //Take the properties out of the class level variable so that we can start capturing again immediately without worrying about overwriting them.
    Properties cur = current_capture_properties;
    current_capture_properties = null;

    //Update the states of everything.
    String recordingID = cur.getProperty(CaptureParameters.RECORDING_ID);
    status_service.setRecordingState(recordingID, RecordingState.CAPTURE_FINISHED);
    setAgentState(AgentState.IDLE);

    //TODO:  Schedule ingest job here

    return result;
  }

  /**
   * Sets the machine's current encoding status
   * 
   * @param state The state for the agent.  Defined in AgentState.
   * @see org.opencastproject.capture.api.AgentState
   */
  private synchronized void setAgentState(String state) {
    agent_state = state;
  }

  /**
   * Gets the machine's current state
   * @return A state (should be defined in AgentState)
   * @see org.opencastproject.capture.api.AgentState
   */
  public String getAgentState() {
    return agent_state;
  }

  public void updated(Dictionary props) throws ConfigurationException {
    // Update any configuration properties here
  }
}
