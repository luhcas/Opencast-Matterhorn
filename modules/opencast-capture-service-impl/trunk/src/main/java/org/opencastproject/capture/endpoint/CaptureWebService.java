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
package org.opencastproject.capture.endpoint;

import org.opencastproject.media.mediapackage.MediaPackage;

import java.util.HashMap;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

/**
 *
 */
@WebService()
public interface CaptureWebService {
  @WebMethod()
  @WebResult(name = "recorder-info")
  public String startCapture();

  @WebMethod()
  @WebResult(name = "recorder-info")
  public String startCapture(@WebParam(name = "media-package") MediaPackage mediaPackage);

  @WebMethod()
  @WebResult(name = "recorder-info")
  public String startCapture(@WebParam(name = "configuration") HashMap<String, String> configuration);

  @WebMethod()
  @WebResult(name = "recorder-info")
  public String startCapture(@WebParam(name = "media-package") MediaPackage mediaPackage,
          @WebParam(name = "configuration") HashMap<String, String> configuration);
}
