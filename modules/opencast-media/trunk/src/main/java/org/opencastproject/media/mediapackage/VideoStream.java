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

package org.opencastproject.media.mediapackage;

import org.opencastproject.media.analysis.types.ScanOrder;
import org.opencastproject.media.analysis.types.ScanType;

/**
 * A stream containing video data.
 * 
 * @author Christoph E. Driessen <ced@neopoly.de>
 */
public interface VideoStream extends Stream {

  Float getBitRate();

  Float getFrameRate();

  Integer getFrameWidth();

  Integer getFrameHeight();

  ScanType getScanType();

  ScanOrder getScanOrder();

  String getCaptureDevice();

  String getCaptureDeviceVersion();

  String getCaptureDeviceVendor();

  String getFormat();

  String getFormatVersion();

  String getEncoderLibraryVendor();
}
