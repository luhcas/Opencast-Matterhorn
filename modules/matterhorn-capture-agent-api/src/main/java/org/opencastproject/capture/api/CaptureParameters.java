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

  /**
   * Settings which control the configuration service
   */

  /** Location of the centralised configuration file */
  String CAPTURE_CONFIG_REMOTE_ENDPOINT_URL = "capture.config.remote.endpoint.url";
  
  /** The time to wait between updating the local copy of the configuration */
  String CAPTURE_CONFIG_REMOTE_POLLING_INTERVAL = "capture.config.remote.polling.interval";

  /** The full path to the cached server config */
  String CAPTURE_CONFIG_CACHE_URL = "capture.config.cache.url";

  /**
   * Settings which control the filesystem
   */

  /** The URL of the caching directory under the root directory */
  String CAPTURE_FILESYSTEM_CACHE_URL = "capture.filesystem.cache.url";

  /** The URL of the volatile directory under the root directory */
  String CAPTURE_FILESYSTEM_VOLATILE_URL = "capture.filesystem.volatile.url";

  /** The root URL where the captures should be stored prior to ingest */
  String CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL = "capture.filesystem.cache.capture.url";

  /**
   * Settings which control the scheduler
   */

  /** The remote URL where the capture schedule should be retrieved */
  String CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL = "capture.schedule.remote.endpoint.url";
  
  /** The time between attempts to fetch updated calendar data */
  String CAPTURE_SCHEDULE_REMOTE_POLLING_INTERVAL = "capture.schedule.remote.polling.interval";

  /** The local URL of the cached copy of the capture schedule */
  String CAPTURE_SCHEDULE_CACHE_URL = "capture.schedule.cache.url";

  /**
   * Settings which control the agent state service
   */

  /** The name of the agent */ 
  String AGENT_NAME = "capture.agent.name";
  
  /** The URL of the remote state service */
  String AGENT_STATE_REMOTE_ENDPOINT_URL = "capture.agent.state.remote.endpoint.url";

  /** The time between attempts to push the agent's state to the state service */
  String AGENT_STATE_REMOTE_POLLING_INTERVAL = "capture.agent.state.remote.polling.interval";

  /** The time between attempts to push the agent's capabilities to the state service */                                                                            
  String AGENT_CAPABILITIES_REMOTE_POLLING_INTERVAL = "capture.agent.capabilities.remote.polling.interval";

  
  /**
   * Settings which control the recording state service
   */

  /** The URL of the remote recording state service */
  String RECORDING_STATE_REMOTE_ENDPOINT_URL = "capture.recording.state.remote.endpoint.url";

  /** The ID of a capture */
  String RECORDING_ID = "capture.recording.id";

  /** A directory which contains a capture */
  String RECORDING_ROOT_URL = "capture.recording.root.url";

  /** Duration to specify for the capture client */
  String RECORDING_END = "capture.recording.end";

  /**
   * Settings which control the ingest jobs
   */

  /** The URL to send the capture data to during ingest */
  String INGEST_ENDPOINT_URL = "capture.ingest.endpoint.url";

  /** The retry interval for attempting ingest */
  String INGEST_RETRY_INTERVAL = "capture.ingest.retry.interval";

  /**
   * Settings which control the capture hardware and outputs
   */

  /**
   * The maximum length, in seconds, which should be captured regardless of scheduled length.
   * This is to catch user input errors in the scheduler, and also to stop infinite captures from an unscheduled capture.  
   */
  String CAPTURE_MAX_LENGTH = "capture.max.length";

  /** A comma delimited list of the friendly names for capturing devices */
  String CAPTURE_DEVICE_NAMES = "capture.device.names";
  
  /* Specification for configuration files are discussed in MH-1184. Properties for capture devices
   * are specified by CAPTURE_DEVICE_PREFIX + "$DEVICENAME" + CAPTURE_DEVICE_* where DEVICENAME is one of
   * the devices specified in CAPTURE_DEVICE_NAMES. For instance, the source of a capture device for
   * a device named SCREEN is CAPTURE_DEVICE_PREFIX + SCREEN + CAPTURE_DEVICE_SOURCE
   */
  
  /** String prefix used when specify capture device properties */
  String CAPTURE_DEVICE_PREFIX = "capture.device.";
  
  /** Property specifying the source location of the device e.g., /dev/video0 */
  String CAPTURE_DEVICE_SOURCE = ".src";

  /** Property specifying the flavor of the device */
  String CAPTURE_DEVICE_FLAVOR = ".flavor";
  
  /** Property specifying the name of the file to output */
  String CAPTURE_DEVICE_DEST = ".outputfile";
  
  /** Property specifying a codec for the device */
  String CAPTURE_DEVICE_CODEC = ".codec";
  
  /** Property appended to CAPTURE_DEVICE_CODEC to specify that codec's bitrate */
  String CAPTURE_DEVICE_BITRATE = ".properties.bitrate";
  
  /** Time interval between confidence updates (in seconds) */
  String CAPTURE_DEVICE_CONFIDENCE_INTERVAL = ".confidence.interval";

  /** Time interval between executions of the capture cleaner */
  String CAPTURE_CLEANER_INTERVAL = "capture.cleaner.interval";
  
  /** Threshold used for determining when deleting archived captures needs to happen */
  String CAPTURE_CLEANER_MIN_DISK_SPACE = "capture.cleaner.mindiskspace";
  
  /** Maximum number of days to archive a capture after its been ingested before cleaning up */
  String CAPTURE_CLEANER_MAX_ARCHIVAL_DAYS = "capture.cleaner.maxarchivaldays";
  
  /** Name of the file created after a recording has been successfuly stopped */
  String CAPTURE_STOPPED_FILE_NAME = "capture.stopped";
  
  /** Name of the file created after a recording has been successfuly ingested */
  String CAPTURE_INGESTED_FILE = "capture.ingested";
  
  /** Name of the zip file ingested by the capture agent, containing all the relevant files for a recording **/
  String ZIP_NAME = "media.zip";
  
  /** Default name for the manifest file */
  String MANIFEST_NAME = "manifest.xml";
  
  /**
   * Settings that control the capture agent confidence monitoring
   */
  
  /** Directory which contains confidence monitoring images */
  String CAPTURE_CONFIDENCE_VIDEO_LOCATION = "capture.confidence.video.location";
  
  /** Maximum number of seconds of audio monitoring data to store in memory */
  String CAPTURE_CONFIDENCE_AUDIO_LENGTH = "capture.confidence.audio.length";
  
}
