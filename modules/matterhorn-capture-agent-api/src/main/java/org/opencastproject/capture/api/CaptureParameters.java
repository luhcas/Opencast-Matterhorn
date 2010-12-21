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
package org.opencastproject.capture.api;

/** 
 * Contains properties that the ConfigurationManager refer. These properties
 * should exist in the configuration file on the local machine as well as the
 * centralised server. 
 */
public interface CaptureParameters {

  /** The number of milliseconds in a second */
  static final long MILLISECONDS = 1000;
  /** The base unit of measurement, 1 second */ 
  static final long SECONDS = 1;
  /** The number of seconds in a minute */
  static final long MINUTES = 60 * SECONDS;
  /** The number of seconds in an hour */
  static final long HOURS = 60 * MINUTES;
  /** The default time to poll the server for calendar information **/
  static final long DEFAULT_STATE_PUSH_TIME = 10 * SECONDS * MILLISECONDS;
  
  /** Matterhorn Core URL */
  static final String CAPTURE_CORE_URL = "org.opencastproject.capture.core.url";

  /**
   * Settings which control the configuration service
   */

  /** Location of the centralised configuration file */
  static final String CAPTURE_CONFIG_REMOTE_ENDPOINT_URL = "capture.config.remote.endpoint.url";
  
  /** The time to wait between updating the local copy of the configuration */
  static final String CAPTURE_CONFIG_REMOTE_POLLING_INTERVAL = "capture.config.remote.polling.interval";

  /** The full path to the cached server config */
  static final String CAPTURE_CONFIG_CACHE_URL = "capture.config.cache.url";

  /**
   * Settings which control the filesystem
   */

  /** The URL of the caching directory under the root directory */
  static final String CAPTURE_FILESYSTEM_CACHE_URL = "capture.filesystem.cache.url";

  /** The URL of the volatile directory under the root directory */
  static final String CAPTURE_FILESYSTEM_VOLATILE_URL = "capture.filesystem.volatile.url";

  /** The root URL where the captures should be stored prior to ingest */
  static final String CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL = "capture.filesystem.cache.capture.url";

  /**
   * Settings which control the scheduler
   */

  /** The remote URL where the capture schedule should be retrieved */
  static final String CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL = "capture.schedule.remote.endpoint.url";
  
  /** The time between attempts to fetch updated calendar data */
  static final String CAPTURE_SCHEDULE_REMOTE_POLLING_INTERVAL = "capture.schedule.remote.polling.interval";

  /** The local URL of the cached copy of the capture schedule */
  static final String CAPTURE_SCHEDULE_CACHE_URL = "capture.schedule.cache.url";

  /**
   * Settings which control the agent state service
   */

  /** The name of the agent */ 
  static final String AGENT_NAME = "capture.agent.name";
  
  /** The URL of the remote state service */
  static final String AGENT_STATE_REMOTE_ENDPOINT_URL = "capture.agent.state.remote.endpoint.url";

  /** The time between attempts to push the agent's state to the state service */
  static final String AGENT_STATE_REMOTE_POLLING_INTERVAL = "capture.agent.state.remote.polling.interval";

  /** The time between attempts to push the agent's capabilities to the state service */                                                                            
  static final String AGENT_CAPABILITIES_REMOTE_POLLING_INTERVAL = "capture.agent.capabilities.remote.polling.interval";

  
  /**
   * Settings which control the recording state service
   */

  /** The URL of the remote recording state service */
  static final String RECORDING_STATE_REMOTE_ENDPOINT_URL = "capture.recording.state.remote.endpoint.url";

  /** The ID of a capture */
  static final String RECORDING_ID = "capture.recording.id";

  /** A directory which contains a capture */
  static final String RECORDING_ROOT_URL = "capture.recording.root.url";

  /** Time at which to stop the capture */
  static final String RECORDING_END = "capture.recording.end";

  /** The duration of the recording in seconds */
  static final String RECORDING_DURATION = "capture.recording.duration";

  /** The number of seconds to wait before force-killing the pipeline */
  static final String RECORDING_SHUTDOWN_TIMEOUT = "capture.recording.shutdown.timeout";

  /** The recording's properties */
  //TODO:  I shouldn't live here, but where should I live?
  static final String RECORDING_PROPERTIES = "org.opencastproject.capture.agent.properties";

  /**
   * Settings which control the ingest jobs
   */

  /** The URL to send the capture data to during ingest */
  static final String INGEST_ENDPOINT_URL = "capture.ingest.endpoint.url";

  /** Number of attempts the capture agent will attempt to ingest before waiting on the next attempt. **/
  static final String INGEST_RETRY_LIMIT = "capture.ingest.retry.limit";
  
  /** The length of time to wait between trying to retry to ingest. **/
  static final String INGEST_RETRY_INTERVAL = "capture.ingest.retry.interval";
  
  /** The length of time to wait until trying to ingest again after failing the number of times in INGEST_RETRY_LIMIT. **/
  static final String INGEST_PAUSE_TIME = "capture.ingest.pause.time";
    
  /** The key for the workflow definition, if any, in the capture properties attached to the iCal event */
  static final String INGEST_WORKFLOW_DEFINITION = "org.opencastproject.workflow.definition";

  /** The path to png file to show, while recording hardware is unplugged (e.g. Epiphan devices). */
  static final String FALLBACK_PNG = "capture.fallback.png";

  /**
   * Settings which control the capture hardware and outputs
   */

  /**
   * The maximum length, in seconds, which should be captured regardless of scheduled length.
   * This is to catch user input errors in the scheduler, and also to stop infinite captures from an unscheduled capture.  
   */
  static final String CAPTURE_MAX_LENGTH = "capture.max.length";
  
  /**
   * The maximum amount of time to wait for our gstreamer pipeline to start capturing
   * before we decide to kill the process and report a failure. Set in seconds.
   */
  static final String CAPTURE_START_WAIT = "capture.start.wait";

  /** A comma delimited list of the friendly names for capturing devices */
  static final String CAPTURE_DEVICE_NAMES = "capture.device.names";
  
  /* Specification for configuration files are discussed in MH-1184. Properties for capture devices
   * are specified by CAPTURE_DEVICE_PREFIX + "$DEVICENAME" + CAPTURE_DEVICE_* where DEVICENAME is one of
   * the devices specified in CAPTURE_DEVICE_NAMES. For instance, the source of a capture device for
   * a device named SCREEN is CAPTURE_DEVICE_PREFIX + SCREEN + CAPTURE_DEVICE_SOURCE
   */
  
  /** String prefix used when specify capture device properties */
  static final String CAPTURE_DEVICE_PREFIX = "capture.device.";
  
  /** Property specifying the type of the source for this device e.g. V4L2_Src **/
  static final String CAPTURE_DEVICE_TYPE = ".type";
  
  /** Property specifying the source location of the device e.g., /dev/video0 */
  static final String CAPTURE_DEVICE_SOURCE = ".src";

  /** Property specifying the flavor of the device */
  static final String CAPTURE_DEVICE_FLAVOR = ".flavor";
  
  /** Property specifying the GStreamer like syntax for a Custom Producer either Video or Audio**/
  static final String CAPTURE_DEVICE_CUSTOM_PRODUCER = ".customProducer";
  
  /** Property specifying the name of the file to output */
  static final String CAPTURE_DEVICE_DEST = ".outputfile";
  
  /** Property specifying a codec for the device */
  static final String CAPTURE_DEVICE_CODEC = ".codec";
  
  /** Property specifying the media container to use */
  static final String CAPTURE_DEVICE_CONTAINER = ".container";
  
  /** Property appended to CAPTURE_DEVICE_CODEC to specify that codec's bitrate */
  static final String CAPTURE_DEVICE_BITRATE = ".bitrate";
  
  /** Property appended to CAPTURE_DEVICE_CODEC to specify that codec's quantizer value (codec=x264enc only) */
  static final String CAPTURE_DEVICE_QUANTIZER = ".bitrate";
  
  /** The framerate in frames per second to force on the video */
  static final String CAPTURE_DEVICE_FRAMERATE = ".framerate";

  /** Property prefixing properties involving the capture buffers */
  static final String CAPTURE_DEVICE_BUFFER = ".buffer";

  /** Property appended to CAPTURE_DEVICE_BUFFER specifying the maximum number of buffers in the queue */
  static final String CAPTURE_DEVICE_BUFFER_MAX_BUFFERS = ".size";

  /** Property appended to CAPTURE_DEVICE_BUFFER specifying the maximum number of bytes in the queue */
  static final String CAPTURE_DEVICE_BUFFER_MAX_BYTES = ".bytes";

  /** Property appended to CAPTURE_DEVICE_BUFFER specifying the maximum length of time to store buffers in the queue */
  static final String CAPTURE_DEVICE_BUFFER_MAX_TIME = ".time";
  
  /** Time interval between confidence updates (in seconds) */
  static final String CAPTURE_DEVICE_CONFIDENCE_INTERVAL = ".confidence.interval";

  /** Time interval between executions of the capture cleaner */
  static final String CAPTURE_CLEANER_INTERVAL = "capture.cleaner.interval";
  
  /** Threshold used for determining when deleting archived captures needs to happen */
  static final String CAPTURE_CLEANER_MIN_DISK_SPACE = "capture.cleaner.mindiskspace";
  
  /** Maximum number of days to archive a capture after its been ingested before cleaning up */
  static final String CAPTURE_CLEANER_MAX_ARCHIVAL_DAYS = "capture.cleaner.maxarchivaldays";
  
  /** Name of the zip file ingested by the capture agent, containing all the relevant files for a recording **/
  //TODO:  I shouldn't live here, but where should I live?
  static final String ZIP_NAME = "media.zip";
  
  /** Default name for the manifest file */
  //TODO:  I shouldn't live here, but where should I live?
  static final String MANIFEST_NAME = "manifest.xml";
  
  /**
   * Settings that control the capture agent confidence monitoring
   */
  
  /** Flag to turn confidence monitoring on off (boolean) */
  static final String CAPTURE_CONFIDENCE_ENABLE = "capture.confidence.enable";
  
  /** Directory which contains confidence monitoring images */
  static final String CAPTURE_CONFIDENCE_VIDEO_LOCATION = "capture.confidence.video.location";
  
  /** Maximum number of seconds of audio monitoring data to store in memory */
  static final String CAPTURE_CONFIDENCE_AUDIO_LENGTH = "capture.confidence.audio.length";
  
  /** Flag to turn confidence monitoring debugging on or off (boolean) */
  static final String CAPTURE_CONFIDENCE_DEBUG = "capture.confidence.debug";

  /**
   * Settings which control the capture agent's handling of scheduling data.
   */

  /**
   * Controls the behaviour of the agent when two scheduled events overlap or are within X of one another.
   * Setting this value to true will cause the cronologically second event to be dropped from the schedule.
   * Any other setting will have the agent shorten the second event to fit.
   * Note that if the length drops below the minimum capture length then the capture will not be scheduled.
   */
  static final String CAPTURE_SCHEDULE_DROP_EVENT_IF_CONFLICT = "capture.schedule.event.drop";

  /** The length of time to require between capture events.  Specified in minutes.
    * Note that this is a limitation of your hardware:  It takes a certain length of time for the hardware
    * to stop and then be ready to capture again.  Setting this to less than 1 will *not* make this happen
    * any faster, and will in fact cause you more problems when the agent tries to start a second capture
    * while the first is still in progress.
    */
  static final String CAPTURE_SCHEDULE_INTEREVENT_BUFFERTIME = "capture.schedule.event.buffertime";
  
}
