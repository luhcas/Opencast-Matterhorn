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

import org.opencastproject.media.mediapackage.MediaPackage;
import java.util.HashMap;

/**
 * OSGi service for starting capture (MH-730)
 */
public interface CaptureAgent {
  /**
   * Starting a simple capture.
   */
  String startCapture();

  /**
   * Starting a simple capture.
   * 
   * @param mediaPackage 
   */
  String startCapture(MediaPackage mediaPackage);

  /**
   * Starting a simple capture.
   * 
   * @param configuration HashMap<String, String> for properties.
   */
  String startCapture(HashMap<String, String> configuration);

  /**
   * Starting a simple capture.
   * 
   * @param mediaPackage 
   * @param configuration HashMap<String, String> for properties.
   */
  String startCapture(MediaPackage mediaPackage, HashMap<String, String> configuration);
}

