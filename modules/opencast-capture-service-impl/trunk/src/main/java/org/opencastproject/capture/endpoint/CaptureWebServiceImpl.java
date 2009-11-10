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

import org.opencastproject.capture.api.CaptureAgent;
import org.opencastproject.media.mediapackage.MediaPackage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

/**
 * @see CaptureWebService
 */
@WebService()
public class CaptureWebServiceImpl implements CaptureWebService {
  private static final Logger logger = LoggerFactory.getLogger(CaptureWebServiceImpl.class);

  private CaptureAgent service;

  public void setService(CaptureAgent service) {
    this.service = service;
  }

  public void unsetService(CaptureAgent service) {
    this.service = null;
  }

  @WebMethod()
  @WebResult(name = "recorder-info")
  public String startCapture() {
    return service.startCapture();
  }

  @WebMethod()
  @WebResult(name = "recorder-info")
  public String startCapture(@WebParam(name = "media-package") MediaPackage mediaPackage) {
    return service.startCapture(mediaPackage);
  }

  @WebMethod()
  @WebResult(name = "recorder-info")
  public String startCapture(@WebParam(name = "configuration") Properties configuration) {
    return service.startCapture(configuration);
  }

  @WebMethod()
  @WebResult(name = "recorder-info")
  public String startCapture(@WebParam(name = "media-package") MediaPackage mediaPackage,
          @WebParam(name = "configuration") Properties configuration) {
    return service.startCapture(mediaPackage, configuration);
  }
  
  @WebMethod()
  @WebResult(name = "recorder-info")
  public String stopCapture() {
    return service.stopCapture();
  }

}
