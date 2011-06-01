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
package org.opencastproject.capture.pipeline.bins.consumers;

/**
 * The Consumers that are currently supported and tested with this code. They are all Bins that inherit from Partial Bin
 * and are an abstraction for GStreamer Sinks.
 */
public enum ConsumerType {
  AUDIO_FILE_SINK, // Places audio data into a file.
  CUSTOM_CONSUMER, // User defined Consumer with GStreamer CLI syntax
  VIDEO_FILE_SINK, //Places video data into a file.
  XVIMAGE_SINK // Only available on Linux. Shows Producer data in real time.
}
