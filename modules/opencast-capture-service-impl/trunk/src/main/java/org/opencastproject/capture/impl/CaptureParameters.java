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

  /** Duration to specify for the capture client */
  public static final String CAPTURE_DURATION = "capture.duration";
  
  public static final String ATTACHMENT = "capture.attachment";
  
  /** Where the data should be sent to for ingestion */
  public static final String INGEST_URL = "admin.ingest.endpoint";
  
  /** Location of the centralised configuration file */
  public static final String CAPTURE_CONFIG_URL = "capture.config.url";
  
  /** The time to wait between updating the local copy of the configuration */
  public static final String CAPTURE_CONFIG_POLLING_INTERVAL = "capture.config.polling.interval";

  /** The URL to store the cached config file in */
  public static final String CAPTURE_CONFIG_CACHE_URL = "capture.config.cache.url";

  /** The URL of the config directory under the root directory */
  public static final String CAPTURE_FILESYSTEM_CONFIG_URL = "capture.filesystem.config.url";

  /** The URL of the capture directory under the root directory */
  public static final String CAPTURE_FILESYSTEM_CAPTURE_URL = "capture.filesystem.capture.url";

  /** The URL of the caching directory under the root directory */
  public static final String CAPTURE_FILESYSTEM_CACHE_URL = "capture.filesystem.cache.url";

  /** The URL of the volatile directory under the root directory */
  public static final String CAPTURE_FILESYSTEM_VOLATILE_URL = "capture.filesystem.volatile.url";

  /** The remote URL where the capture schedule should be retrieved */
  public static final String CAPTURE_SCHEDULE_URL = "capture.schedule.url";
  
  /** The time between attempts to fetch updated calendar data */
  public static final String CAPTURE_SCHEDULE_POLLING_INTERVAL = "capture.schedule.polling.interval";

  /** The local URL of the cached copy of the capture schedule */
  public static final String CAPTURE_SCHEDULE_CACHE_URL = "capture.schedule.cache.url";
  
  /** A comma delimited list of the friendly names for capturing devices */
  public static final String CAPTURE_DEVICE_NAMES = "capture.device.names";
  
  public static final String CAPTURE_DEVICE_PREFIX = "capture.device";
}
