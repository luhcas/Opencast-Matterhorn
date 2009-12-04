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

/** 
 * Contains properties that the ConfigurationManager refer. These properties
 * should exist in the configuration file on the local machine as well as the
 * centralised server. 
 */
public interface CaptureParameters {

  /** Location of the centralised configuration file */
  public static final String CAPTURE_CONFIG_URL = "capture.config.url";
  
  /** The time to wait between updating the local copy of the configuration */
  public static final String CAPTURE_CONFIG_POLLING_INTERVAL = "capture.config.polling.interval";

  /** The URL to store the cached config file in */
  public static final String CAPTURE_CONFIG_CACHE_URL = "capture.config.cache.url";
  
  /** The URL to store the main config file */
  public static final String CAPTURE_CONFIG_FILESYSTEM_URL = "capture.config.filesystem.url";

  /** The URL of the config directory under the root directory */
  public static final String CAPTURE_FILESYSTEM_CONFIG_URL = "capture.filesystem.config.url";

  /** The URL of the capture directory under the root directory */
  public static final String CAPTURE_FILESYSTEM_CAPTURE_URL = "capture.filesystem.capture.url";

  /** The URL of the caching directory under the root directory */
  public static final String CAPTURE_FILESYSTEM_CACHE_URL = "capture.filesystem.cache.url";

  /** The URL of the volatile directory under the root directory */
  public static final String CAPTURE_FILESYSTEM_VOLATILE_URL = "capture.filesystem.volatile.url";

  /** The root URL where the captures should be stored prior to ingest */
  public static final String CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL = "capture.filesystem.cache.capture.url";
  
  /** The remote URL where the capture schedule should be retrieved */
  public static final String CAPTURE_SCHEDULE_URL = "capture.schedule.url";
  
  /** The time between attempts to fetch updated calendar data */
  public static final String CAPTURE_SCHEDULE_POLLING_INTERVAL = "capture.schedule.polling.interval";

  /** The local URL of the cached copy of the capture schedule */
  public static final String CAPTURE_SCHEDULE_CACHE_URL = "capture.schedule.cache.url";

  /** The name of the agent */ 
  public static final String AGENT_NAME = "capture.agent.name";
  
  /** The URL of the remote status service */
  public static final String AGENT_STATUS_ENDPOINT_URL = "capture.agent.status.endpoint.url";

  /** The time between attempts to push the agent's status to the status service */
  public static final String AGENT_STATUS_POLLING_INTERVAL = "capture.agent.status.polling.interval";

  /** The URL of the remote recording status service */
  public static final String RECORDING_STATUS_ENDPOINT_URL = "capture.recording.status.endpoint.url";

  /** The ID of a capture */
  public static final String RECORDING_ID = "capture.recording.id";

  /** A directory which contains a capture */
  public static final String RECORDING_ROOT_URL = "capture.recording.root.url";

  /** Duration to specify for the capture client */
  public static final String RECORDING_END = "capture.recording.end";

  /** The URL to send the capture data to during ingest */
  public static final String INGEST_ENDPOINT_URL = "capture.ingest.endpoint.url";

  /** The retry interval for attempting ingest */
  public static final String INGEST_RETRY_INTERVAL = "capture.ingest.retry.interval";

  /** A comma delimited list of the friendly names for capturing devices */
  public static final String CAPTURE_DEVICE_NAMES = "capture.device.names";
  
  /* Specification for configuration files are discussed in MH-1184. Properties for capture devices
   * are specified by CAPTURE_DEVICE_PREFIX + "$DEVICENAME" + CAPTURE_DEVICE_* where DEVICENAME is one of
   * the devices specified in CAPTURE_DEVICE_NAMES. For instance, the source of a capture device for
   * a device named SCREEN is CAPTURE_DEVICE_PREFIX + SCREEN + CAPTURE_DEVICE_SOURCE
   */
  
  /** String prefix used when specify capture device properties */
  public static final String CAPTURE_DEVICE_PREFIX = "capture.device.";
  
  /** Property specifying the source location of the device e.g., /dev/video0 */
  public static final String CAPTURE_DEVICE_SOURCE = ".src";
  
  /** Property specifying the name of the file to output */
  public static final String CAPTURE_DEVICE_DEST = ".outputfile";
  
  /** Property specifying a codec for the device */
  public static final String CAPTURE_DEVICE_CODEC = ".codec";
  
  /** Property appended to CAPTURE_DEVICE_CODEC to specify that codec's bitrate */
  public static final String CAPTURE_DEVICE_BITRATE = ".properties.bitrate";

  /** Threshold used for determining when deleting archived captures needs to happen */
  public static final String CAPTURE_CLEANER_MIN_DISK_SPACE = "capture.cleaner.mindiskspace";
  
  /** Maximum number of days to archive a capture after its been ingested before cleaning up */
  public static final String CAPTURE_CLEANER_MAX_ARCHIVAL_DAYS = "capture.cleaner.maxarchivaldays";
}
