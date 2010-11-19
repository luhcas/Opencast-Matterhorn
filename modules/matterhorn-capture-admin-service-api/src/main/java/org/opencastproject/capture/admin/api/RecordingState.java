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
package org.opencastproject.capture.admin.api;

import java.util.Arrays;
import java.util.List;

/**
 * A representation of a recording's current state (MH-1475).
 */
public interface RecordingState {
  /** Constant <code>UNKNOWN="unknown"</code> */
  public static final String UNKNOWN = "unknown";
  /** Constant <code>CAPTURING="capturing"</code> */
  public static final String CAPTURING = "capturing";
  /** Constant <code>CAPTURE_FINISHED="capture_finished"</code> */
  public static final String CAPTURE_FINISHED = "capture_finished";
  /** Constant <code>CAPTURE_ERROR="capture_error"</code> */
  public static final String CAPTURE_ERROR = "capture_error";
  /** Constant <code>MANIFEST="manifest"</code> */
  public static final String MANIFEST = "manifest";
  /** Constant <code>MANIFEST_ERROR="manifest_error"</code> */
  public static final String MANIFEST_ERROR = "manifest_error";
  /** Constant <code>MANIFEST_FINISHED="manifest_finished"</code> */
  public static final String MANIFEST_FINISHED = "manifest_finished";
  /** Constant <code>COMPRESSING="compressing"</code> */
  public static final String COMPRESSING = "compressing";
  /** Constant <code>COMPRESSING_ERROR="compressing_error"</code> */
  public static final String COMPRESSING_ERROR = "compressing_error";
  /** Constant <code>UPLOADING="uploading"</code> */
  public static final String UPLOADING = "uploading";
  /** Constant <code>UPLOAD_FINISHED="upload_finished"</code> */
  public static final String UPLOAD_FINISHED = "upload_finished";
  /** Constant <code>UPLOAD_ERROR="upload_error"</code> */
  public static final String UPLOAD_ERROR = "upload_error";

  /** The collection of all known states. TODO: Remove this when the states are replaced with enums */
  public static final List<String> KNOWN_STATES = Arrays.asList(new String[] { UNKNOWN, CAPTURING, CAPTURE_FINISHED,
          CAPTURE_ERROR, MANIFEST, MANIFEST_ERROR, MANIFEST_FINISHED, COMPRESSING, COMPRESSING_ERROR, UPLOADING,
          UPLOAD_FINISHED, UPLOAD_ERROR });

}
