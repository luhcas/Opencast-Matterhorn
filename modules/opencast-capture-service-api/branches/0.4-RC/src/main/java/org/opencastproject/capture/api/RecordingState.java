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
package org.opencastproject.capture.api;

/**
 * A representation of a recording's current state (MH-1475)
 */
public interface RecordingState {
  public static final String UNKNOWN = "unknown";
  public static final String CAPTURING = "capturing";
  public static final String CAPTURE_FINISHED = "capture_finished";
  public static final String UPLOADING = "uploading";
  public static final String UPLOAD_FINISHED = "upload_finished";
  public static final String UPLOAD_ERROR = "upload_error";
}
